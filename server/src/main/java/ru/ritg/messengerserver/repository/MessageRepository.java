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
     * Найти диалог между двумя пользователями.
     */
    @Query("SELECT m FROM Message m WHERE " +
            "(m.sender = :user1 AND m.recipient = :user2) OR " +
            "(m.sender = :user2 AND m.recipient = :user1) " +
            "ORDER BY m.createdAt ASC")
    List<Message> findConversation(@Param("user1") User user1, @Param("user2") User user2);

    @Query("SELECT m FROM Message m WHERE " +
            "(m.sender = :user1 AND m.recipient = :user2) OR " +
            "(m.sender = :user2 AND m.recipient = :user1) " +
            "ORDER BY m.createdAt ASC")
    List<Message> findConversationPaginated(@Param("user1") User user1, @Param("user2") User user2,
                                            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT DISTINCT m.recipient FROM Message m WHERE m.sender = :user")
    List<User> findRecipientsBySender(@Param("user") User user);

    @Query("SELECT DISTINCT m.sender FROM Message m WHERE m.recipient = :user")
    List<User> findSendersByRecipient(@Param("user") User user);
}
