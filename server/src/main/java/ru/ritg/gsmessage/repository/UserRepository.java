package ru.ritg.gsmessage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ritg.gsmessage.model.User;

import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для управления сущностями {@link User}.
 *
 * <p>Стандартные CRUD-операции наследуются от {@link JpaRepository}.
 * Дополнительно предоставляет поиск по номеру телефона.</p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Найти пользователя по номеру телефона.
     *
     * @param phone номер телефона в формате E.164 (например, +79001234567)
     * @return {@link Optional} с пользователем, если найден
     */
    Optional<User> findByPhone(String phone);

    /**
     * Проверить существование пользователя с указанным номером.
     *
     * @param phone номер телефона
     * @return {@code true}, если пользователь существует
     */
    boolean existsByPhone(String phone);
}
