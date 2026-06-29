package ru.ritg.messengerserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ritg.messengerserver.model.VerificationCode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для управления сущностями {@link VerificationCode}.
 *
 * <p>Хранит OTP-коды. Поддерживает поиск по телефону и коду, а также
 * очистку просроченных записей.</p>
 */
@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, UUID> {

    /**
     * Найти последний код верификации по телефону и значению кода.
     *
     * @param phone номер телефона
     * @param code 4-значный OTP-код
     * @return {@link Optional} с записью кода
     */
    Optional<VerificationCode> findByPhoneAndCode(String phone, String code);

    /**
     * Найти все коды для указанного телефона.
     *
     * @param phone номер телефона
     * @return список кодов
     */
    List<VerificationCode> findByPhone(String phone);

    /**
     * Удалить все коды, срок действия которых истёк.
     *
     * @param now текущее время
     */
    void deleteByExpiresAtBefore(LocalDateTime now);
}
