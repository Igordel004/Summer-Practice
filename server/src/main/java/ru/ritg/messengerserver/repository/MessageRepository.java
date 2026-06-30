package ru.ritg.messengerserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.ritg.messengerserver.model.Message;
import ru.ritg.messengerserver.model.MessageStatus;
import ru.ritg.messengerserver.model.User;

import java.util.List;
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
     * Найти сообщения получателя с указанным статусом.
     *
     * @param recipient получатель
     * @param status статус доставки
     * @return список сообщений (например, PENDING для офлайн-пользователя)
     */
    List<Message> findByRecipientAndStatus(User recipient, MessageStatus status);

    /**
     * Найти все сообщения диалога между двумя пользователями (без пагинации).
     *
     * <p>Возвращает сообщения, где user1 — отправитель, user2 — получатель,
     * и наоборот. Сортировка от новых к старым.</p>
     *
     * @param user1 первый участник диалога
     * @param user2 второй участник диалога
     * @return список сообщений диалога, отсортированных по убыванию даты
     */
    @Query("SELECT m FROM Message m WHERE " +
            "(m.sender = :user1 AND m.recipient = :user2) OR " +
            "(m.sender = :user2 AND m.recipient = :user1) " +
            "ORDER BY m.createdAt DESC")
    List<Message> findConversation(@Param("user1") User user1, @Param("user2") User user2);

    /**
     * Найти сообщения диалога с пагинацией.
     *
     * <p>Используется для загрузки истории переписки порциями.
     * Сортировка от новых к старым — на клиенте результат разворачивается
     * в хронологический порядок.</p>
     *
     * @param user1   первый участник диалога
     * @param user2   второй участник диалога
     * @param pageable параметры пагинации (номер страницы, размер)
     * @return страница сообщений диалога
     */
    @Query("SELECT m FROM Message m WHERE " +
            "(m.sender = :user1 AND m.recipient = :user2) OR " +
            "(m.sender = :user2 AND m.recipient = :user1) " +
            "ORDER BY m.createdAt DESC")
    List<Message> findConversationPaginated(@Param("user1") User user1, @Param("user2") User user2,
                                            org.springframework.data.domain.Pageable pageable);

    /**
     * Найти всех получателей сообщений отправителя.
     *
     * <p>Используется для построения списка чатов — все пользователи,
     * которым текущий пользователь когда-либо отправлял сообщения.</p>
     *
     * @param user отправитель
     * @return список получателей (уникальные пользователи)
     */
    @Query("SELECT DISTINCT m.recipient FROM Message m WHERE m.sender = :user")
    List<User> findRecipientsBySender(@Param("user") User user);

    /**
     * Найти всех отправителей сообщений получателю.
     *
     * <p>Используется для построения списка чатов — все пользователи,
     * которые когда-либо отправляли сообщения текущему пользователю.</p>
     *
     * @param user получатель
     * @return список отправителей (уникальные пользователи)
     */
    @Query("SELECT DISTINCT m.sender FROM Message m WHERE m.recipient = :user")
    List<User> findSendersByRecipient(@Param("user") User user);

    /**
     * Подсчитать общее количество сообщений в диалоге.
     *
     * <p>Используется для пагинации — клиент получает общее количество
     * и может определить, есть ли ещё сообщения для загрузки.</p>
     *
     * @param user1 первый участник диалога
     * @param user2 второй участник диалога
     * @return количество сообщений в диалоге
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE " +
            "(m.sender = :user1 AND m.recipient = :user2) OR " +
            "(m.sender = :user2 AND m.recipient = :user1)")
    long countConversation(@Param("user1") User user1, @Param("user2") User user2);

    /**
     * Найти сообщения отправителя с указанными статусами.
     *
     * <p>Используется для уведомления отправителя об изменении статуса
     * его сообщений (DELIVERED / READ) при подключении через WebSocket.</p>
     *
     * @param sender   отправитель
     * @param statuses список статусов для фильтрации
     * @param pageable параметры пагинации (ограничение количества результатов)
     * @return список сообщений, отсортированных по убыванию даты создания
     */
    @Query("SELECT m FROM Message m WHERE m.sender = :sender AND m.status IN :statuses ORDER BY m.createdAt DESC")
    List<Message> findBySenderAndStatusIn(@Param("sender") User sender, @Param("statuses") List<MessageStatus> statuses,
                                          org.springframework.data.domain.Pageable pageable);
}
