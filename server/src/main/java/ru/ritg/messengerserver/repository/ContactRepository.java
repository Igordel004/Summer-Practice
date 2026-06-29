package ru.ritg.messengerserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ritg.messengerserver.model.Contact;
import ru.ritg.messengerserver.model.ContactId;
import ru.ritg.messengerserver.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для управления сущностями {@link Contact}.
 *
 * <p>Хранит связи пользователь-контакт (адресная книга).
 * Составной ключ: (user_id, contact_user_id).</p>
 */
@Repository
public interface ContactRepository extends JpaRepository<Contact, ContactId> {

    /**
     * Найти все контакты пользователя.
     *
     * @param user владелец адресной книги
     * @return список контактов
     */
    List<Contact> findByUser(User user);

    /**
     * Найти конкретную связь пользователь-контакт.
     *
     * @param user владелец
     * @param contactUser целевой контакт
     * @return {@link Optional} с записью связи
     */
    Optional<Contact> findByUserAndContactUser(User user, User contactUser);
}
