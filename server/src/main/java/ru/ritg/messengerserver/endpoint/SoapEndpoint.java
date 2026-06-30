package ru.ritg.messengerserver.endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import ru.ritg.messengerserver.dto.soap.*;
import ru.ritg.messengerserver.exception.LimitExceededException;
import ru.ritg.messengerserver.exception.MessageNotFoundException;
import ru.ritg.messengerserver.exception.SoapFaultExceptionResolver;
import ru.ritg.messengerserver.exception.UnauthorizedException;
import ru.ritg.messengerserver.model.Contact;
import ru.ritg.messengerserver.model.User;
import ru.ritg.messengerserver.repository.ContactRepository;
import ru.ritg.messengerserver.repository.UserRepository;
import ru.ritg.messengerserver.service.AuthService;
import ru.ritg.messengerserver.service.MessageRoutingService;
import ru.ritg.messengerserver.websocket.SessionRegistry;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * SOAP Endpoint для обработки XML-запросов.
 *
 * <p>Обрабатывает запросы по пути SOAP / HTTP на порту 8080:</p>
 * <ul>
 *   <li>requestOtp — запрос OTP</li>
 *   <li>verifyOtp — верификация OTP и выдача JWT</li>
 *   <li>addContact — добавление контакта</li>
 *   <li>getHistory — получение истории переписки</li>
 * </ul>
 */
@Endpoint
public class SoapEndpoint {

    private static final Logger log = LoggerFactory.getLogger(SoapEndpoint.class);
    private static final String NAMESPACE = SoapConstants.NAMESPACE;
    private static final int MAX_CONNECTIONS = 500;

    private final AuthService authService;
    private final MessageRoutingService messageRoutingService;
    private final SoapFaultExceptionResolver soapFaultResolver;
    private final UserRepository userRepository;
    private final ContactRepository contactRepository;
    private final SessionRegistry sessionRegistry;

    public SoapEndpoint(AuthService authService,
                        MessageRoutingService messageRoutingService,
                        SoapFaultExceptionResolver soapFaultResolver,
                        UserRepository userRepository,
                        ContactRepository contactRepository,
                        SessionRegistry sessionRegistry) {
        this.authService = authService;
        this.messageRoutingService = messageRoutingService;
        this.soapFaultResolver = soapFaultResolver;
        this.userRepository = userRepository;
        this.contactRepository = contactRepository;
        this.sessionRegistry = sessionRegistry;
    }

    private void checkConnectionLimit() {
        if (sessionRegistry.getAllSessions().size() >= MAX_CONNECTIONS) {
            throw new LimitExceededException("Service Unavailable: connection limit exceeded");
        }
    }

    /**
     * Запросить отправку OTP-кода.
     *
     * <p>Входные данные: XML {@link RequestOtp}.</p>
     * <p>Параметры: {@code phone} — номер телефона, обязательный.</p>
     * <p>Ожидаемый результат: XML {@link ResponseOtp} — подтверждение.</p>
     * <p>Возможные ошибки: SOAP Fault InvalidPhone, TooManyRequests.</p>
     *
     * @param request запрос OTP
     * @return ответ с подтверждением
     */
    @PayloadRoot(namespace = NAMESPACE, localPart = "RequestOtp")
    @ResponsePayload
    public ResponseOtp requestOtp(@RequestPayload RequestOtp request) {
        try {
            checkConnectionLimit();
            return authService.generateOtp(request.getPhone());
        } catch (RuntimeException ex) {
            throw soapFaultResolver.resolveException(ex);
        }
    }

    /**
     * Верифицировать OTP-код и получить JWT-токен.
     *
     * <p>Входные данные: XML {@link VerifyOtp}.</p>
     * <p>Параметры: {@code phone}, {@code code}.</p>
     * <p>Ожидаемый результат: XML {@link AuthResponse} — JWT и срок действия.</p>
     * <p>Возможные ошибки: SOAP Fault InvalidOtp, OtpExpired.</p>
     *
     * @param request запрос верификации
     * @return ответ с JWT-токеном
     */
    @PayloadRoot(namespace = NAMESPACE, localPart = "VerifyOtp")
    @ResponsePayload
    public AuthResponse verifyOtp(@RequestPayload VerifyOtp request) {
        try {
            checkConnectionLimit();
            return authService.verifyOtp(request.getPhone(), request.getCode(), request.getNickname());
        } catch (RuntimeException ex) {
            throw soapFaultResolver.resolveException(ex);
        }
    }

    /**
     * Добавить контакт в адресную книгу.
     *
     * <p>Входные данные: XML {@link AddContact}.</p>
     * <p>Параметры: {@code token}, {@code contactPhone}.</p>
     * <p>Ожидаемый результат: XML {@link ContactResponse}.</p>
     * <p>Возможные ошибки: SOAP Fault Unauthorized, ContactNotFound.</p>
     *
     * @param request запрос добавления контакта
     * @return ответ с результатом
     */
    @PayloadRoot(namespace = NAMESPACE, localPart = "AddContact")
    @ResponsePayload
    public ContactResponse addContact(@RequestPayload AddContact request) {
        try {
            checkConnectionLimit();
            Optional<User> currentUser = authService.getUserByToken(request.getToken());
            if (currentUser.isEmpty()) {
                throw new UnauthorizedException(request.getToken());
            }

            Optional<User> contactUser = userRepository.findByPhone(request.getContactPhone());
            if (contactUser.isEmpty()) {
                ContactResponse failResponse = new ContactResponse();
                failResponse.setSuccess(false);
                failResponse.setMessage("Пользователь не найден");
                return failResponse;
            }

            if (currentUser.get().getId().equals(contactUser.get().getId())) {
                ContactResponse selfResponse = new ContactResponse();
                selfResponse.setSuccess(false);
                selfResponse.setMessage("Нельзя добавить себя");
                return selfResponse;
            }

            Optional<Contact> existing = contactRepository.findByUserAndContactUser(
                    currentUser.get(), contactUser.get());
            if (existing.isPresent()) {
                ContactResponse alreadyExists = new ContactResponse();
                alreadyExists.setSuccess(false);
                alreadyExists.setContactId(contactUser.get().getId());
                alreadyExists.setMessage("Контакт уже добавлен");
                return alreadyExists;
            }

            Contact contact = Contact.builder()
                    .user(currentUser.get())
                    .contactUser(contactUser.get())
                    .build();
            contactRepository.save(contact);

            ContactResponse response = new ContactResponse();
            response.setSuccess(true);
            response.setContactId(contactUser.get().getId());
            response.setMessage("Контакт добавлен");
            return response;
        } catch (LimitExceededException | UnauthorizedException ex) {
            throw soapFaultResolver.resolveException(ex);
        }
    }

    /**
     * Удалить контакт из адресной книги.
     *
     * <p>Входные данные: XML {@link DeleteContact}.</p>
     * <p>Параметры: {@code token}, {@code contactPhone}.</p>
     * <p>Ожидаемый результат: XML {@link ContactResponse}.</p>
     * <p>Возможные ошибки: SOAP Fault Unauthorized, ContactNotFound.</p>
     *
     * @param request запрос удаления контакта
     * @return ответ с результатом
     */
    @PayloadRoot(namespace = NAMESPACE, localPart = "DeleteContact")
    @ResponsePayload
    public ContactResponse deleteContact(@RequestPayload DeleteContact request) {
        try {
            checkConnectionLimit();
            Optional<User> currentUser = authService.getUserByToken(request.getToken());
            if (currentUser.isEmpty()) {
                throw new UnauthorizedException(request.getToken());
            }

            Optional<User> contactUser = userRepository.findByPhone(request.getContactPhone());
            if (contactUser.isEmpty()) {
                ContactResponse failResponse = new ContactResponse();
                failResponse.setSuccess(false);
                failResponse.setMessage("Пользователь не найден");
                return failResponse;
            }

            Optional<Contact> existing = contactRepository.findByUserAndContactUser(
                    currentUser.get(), contactUser.get());
            if (existing.isEmpty()) {
                ContactResponse notFound = new ContactResponse();
                notFound.setSuccess(false);
                notFound.setMessage("Контакт не найден");
                return notFound;
            }

            contactRepository.delete(existing.get());

            ContactResponse response = new ContactResponse();
            response.setSuccess(true);
            response.setMessage("Контакт удалён");
            return response;
        } catch (LimitExceededException | UnauthorizedException ex) {
            throw soapFaultResolver.resolveException(ex);
        }
    }

    /**
     * Получить историю переписки с пагинацией.
     *
     * <p>Входные данные: XML {@link GetHistory}.</p>
     * <p>Параметры: {@code token}, {@code offset}, {@code limit}.</p>
     * <p>Ожидаемый результат: XML {@link HistoryResponse}.</p>
     * <p>Возможные ошибки: SOAP Fault Unauthorized, InvalidOffset.</p>
     *
     * @param request запрос истории
     * @return ответ со списком сообщений
     */
    @PayloadRoot(namespace = NAMESPACE, localPart = "GetHistory")
    @ResponsePayload
    public HistoryResponse getHistory(@RequestPayload GetHistory request) {
        try {
            checkConnectionLimit();
            Optional<User> currentUser = authService.getUserByToken(request.getToken());
            if (currentUser.isEmpty()) {
                throw new UnauthorizedException(request.getToken());
            }

            var messages = messageRoutingService.getHistory(
                    currentUser.get().getId(), request.getContactId(),
                    request.getOffset(), request.getLimit());

            List<MessageDto> dtos = messages.stream().map(msg -> {
                MessageDto dto = new MessageDto();
                dto.setId(msg.getId());
                dto.setSenderId(msg.getSender().getId());
                dto.setRecipientId(msg.getRecipient().getId());
                dto.setPayload(msg.getPayload());
                dto.setStatus(msg.getStatus());
                dto.setCreatedAt(msg.getCreatedAt() != null
                        ? msg.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        : "");
                return dto;
            }).toList();

            HistoryResponse response = new HistoryResponse();
            response.setMessages(dtos);
            long totalCount = messageRoutingService.countHistory(
                    currentUser.get().getId(), request.getContactId());
            response.setTotal((int) totalCount);
            log.info("GetHistory: {}-{} of {}", request.getOffset(), request.getOffset() + dtos.size(), totalCount);
            return response;
        } catch (LimitExceededException | UnauthorizedException ex) {
            throw soapFaultResolver.resolveException(ex);
        }
    }

    /**
     * Обновить никнейм пользователя.
     *
     * <p>Входные данные: XML {@link UpdateNickname}.</p>
     * <p>Параметры: {@code token}, {@code nickname}.</p>
     * <p>Ожидаемый результат: XML {@link UpdateNicknameResponse}.</p>
     * <p>Возможные ошибки: SOAP Fault Unauthorized.</p>
     *
     * @param request запрос смены никнейма
     * @return ответ с результатом
     */
    @PayloadRoot(namespace = NAMESPACE, localPart = "UpdateNickname")
    @ResponsePayload
    public UpdateNicknameResponse updateNickname(@RequestPayload UpdateNickname request) {
        try {
            checkConnectionLimit();
            Optional<User> currentUser = authService.getUserByToken(request.getToken());
            if (currentUser.isEmpty()) {
                throw new UnauthorizedException(request.getToken());
            }

            String newNickname = authService.updateNickname(currentUser.get(), request.getNickname());

            UpdateNicknameResponse response = new UpdateNicknameResponse();
            response.setSuccess(true);
            response.setNickname(newNickname);
            return response;
        } catch (LimitExceededException | UnauthorizedException ex) {
            throw soapFaultResolver.resolveException(ex);
        }
    }

    /**
     * Получить список контактов пользователя.
     *
     * <p>Входные данные: XML {@link GetContacts}.</p>
     * <p>Параметры: {@code token}.</p>
     * <p>Ожидаемый результат: XML {@link GetContactsResponse}.</p>
     * <p>Возможные ошибки: SOAP Fault Unauthorized.</p>
     *
     * @param request запрос списка контактов
     * @return ответ со списком контактов
     */
    @PayloadRoot(namespace = NAMESPACE, localPart = "GetContacts")
    @ResponsePayload
    public GetContactsResponse getContacts(@RequestPayload GetContacts request) {
        try {
            checkConnectionLimit();
            Optional<User> currentUser = authService.getUserByToken(request.getToken());
            if (currentUser.isEmpty()) {
                throw new UnauthorizedException(request.getToken());
            }

            List<Contact> contacts = contactRepository.findByUser(currentUser.get());
            List<ContactDto> dtos = contacts.stream().map(c -> {
                ContactDto dto = new ContactDto();
                dto.setContactId(c.getContactUser().getId());
                dto.setPhone(c.getContactUser().getPhone());
                dto.setNickname(c.getContactUser().getNickname());
                return dto;
            }).toList();

            GetContactsResponse response = new GetContactsResponse();
            response.setContacts(dtos);
            return response;
        } catch (LimitExceededException | UnauthorizedException ex) {
            throw soapFaultResolver.resolveException(ex);
        }
    }

    /**
     * Получить список чатов (собеседников) пользователя.
     *
     * <p>Входные данные: XML {@link GetChats}.</p>
     * <p>Параметры: {@code token}.</p>
     * <p>Ожидаемый результат: XML {@link GetChatsResponse}.</p>
     * <p>Возможные ошибки: SOAP Fault Unauthorized.</p>
     *
     * @param request запрос списка чатов
     * @return ответ со списком собеседников
     */
    @PayloadRoot(namespace = NAMESPACE, localPart = "GetChats")
    @ResponsePayload
    public GetChatsResponse getChats(@RequestPayload GetChats request) {
        try {
            checkConnectionLimit();
            Optional<User> currentUser = authService.getUserByToken(request.getToken());
            if (currentUser.isEmpty()) {
                throw new UnauthorizedException(request.getToken());
            }

            List<User> partners = messageRoutingService.getChats(currentUser.get().getId());
            List<ContactDto> dtos = partners.stream().map(u -> {
                ContactDto dto = new ContactDto();
                dto.setContactId(u.getId());
                dto.setPhone(u.getPhone());
                dto.setNickname(u.getNickname());
                return dto;
            }).toList();

            GetChatsResponse response = new GetChatsResponse();
            response.setContacts(dtos);
            return response;
        } catch (LimitExceededException | UnauthorizedException ex) {
            throw soapFaultResolver.resolveException(ex);
        }
    }
}
