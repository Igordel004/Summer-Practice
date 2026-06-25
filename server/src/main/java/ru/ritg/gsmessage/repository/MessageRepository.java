package ru.ritg.gsmessage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ritg.gsmessage.model.Message;
import ru.ritg.gsmessage.model.MessageStatus;
import ru.ritg.gsmessage.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для управления сущностями {@link Message}.
 *
 * <p>Поддерживает поиск по отправителю, получателю и статусу доставки.
 * Используется для маршрутизации сообщений и буферизации офлайн.</p>
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    /**
     * Найти все сообщения, адресованные указанному получателю.
     *
     * @param recipient получатель
     * @return список сообщений, отсортированный по времени создания
     */
    List<Message> findByRecipient(User recipient);

    /**
     * Найти сообщения в диалоге между двумя пользователями.
     *
     * @param sender отправитель
     * @param recipient получатель
     * @return список сообщений диалога
     */
    List<Message> findBySenderAndRecipient(User sender, User recipient);

    /**
     * Найти сообщения получателя с указанным статусом.
     *
     * @param recipient получатель
     * @param status статус доставки
     * @return список сообщений (например, PENDING для офлайн-пользователя)
     */
    List<Message> findByRecipientAndStatus(User recipient, MessageStatus status);

    /**
     * Найти сообщение по идентификатору.
     *
     * @param id UUID сообщения
     * @return {@link Optional} с сообщением
     */
    Optional<Message> findById(UUID id);
}
