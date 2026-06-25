package ru.ritg.gsmessage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ritg.gsmessage.model.Session;
import ru.ritg.gsmessage.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для управления сущностями {@link Session}.
 *
 * <p>Хранит JWT-токены сессий пользователей. Поддерживает поиск по токену
 * и пользователю, а также массовое удаление просроченных сессий.</p>
 */
@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {

    /**
     * Найти сессию по токену JWT.
     *
     * @param token строка JWT-токена
     * @return {@link Optional} с сессией
     */
    Optional<Session> findByToken(String token);

    /**
     * Найти все сессии пользователя.
     *
     * @param user пользователь
     * @return список сессий
     */
    List<Session> findByUser(User user);

    /**
     * Удалить все сессии, срок действия которых истёк до указанного времени.
     *
     * @param now текущее время
     */
    void deleteByExpiresAtBefore(LocalDateTime now);
}
