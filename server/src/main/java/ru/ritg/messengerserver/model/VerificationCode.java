package ru.ritg.messengerserver.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Сущность одноразового кода верификации (OTP).
 *
 * <p>Генерируется при запросе регистрации/авторизации. Содержит 4-значный
 * числовой код, срок действия и флаг использования. Хранится в БД до
 * истечения срока или явной очистки.</p>
 */
@Entity
@Table(name = "verification_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "code", nullable = false, length = 4)
    private String code;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_used", nullable = false)
    private boolean isUsed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Проверяет, истёк ли срок действия кода.
     *
     * @return {@code true}, если текущее время превышает {@code expiresAt}
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Помечает код как использованный.
     */
    public void markUsed() {
        this.isUsed = true;
    }
}
