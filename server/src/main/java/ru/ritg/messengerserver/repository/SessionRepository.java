package ru.ritg.messengerserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ritg.messengerserver.model.Session;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Репозиторий для управления сущностями {@link Session}.
 *
 * <p>Хранит JWT-токены сессий пользователей.
 * Поддерживает массовое удаление просроченных сессий.</p>
 */
@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {

    /**
     * Удалить все сессии, срок действия которых истёк до указанного времени.
     *
     * @param now текущее время
     */
    void deleteByExpiresAtBefore(LocalDateTime now);
}
