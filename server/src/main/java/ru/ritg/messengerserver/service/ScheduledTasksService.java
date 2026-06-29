package ru.ritg.messengerserver.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.ritg.messengerserver.repository.SessionRepository;
import ru.ritg.messengerserver.repository.VerificationCodeRepository;

import java.time.LocalDateTime;

/**
 * Сервис фоновых задач.
 *
 * <p>Управляет Cron-задачами: очистка просроченных OTP и сессий.</p>
 */
@Service
public class ScheduledTasksService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasksService.class);

    private final VerificationCodeRepository verificationCodeRepository;
    private final SessionRepository sessionRepository;

    public ScheduledTasksService(VerificationCodeRepository verificationCodeRepository,
                                 SessionRepository sessionRepository) {
        this.verificationCodeRepository = verificationCodeRepository;
        this.sessionRepository = sessionRepository;
    }

    /**
     * Удалить просроченные OTP-коды.
     *
     * <p>Запускается по расписанию (cron: 0 0 3 * * * — ежедневно в 03:00).</p>
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupExpiredVerificationCodes() {
        verificationCodeRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.info("Cleaned up expired verification codes");
    }

    /**
     * Удалить просроченные сессии.
     *
     * <p>Запускается по расписанию (cron: 0 0 4 * * * — ежедневно в 04:00).</p>
     */
    @Scheduled(cron = "0 0 4 * * *")
    public void cleanupExpiredSessions() {
        sessionRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.info("Cleaned up expired sessions");
    }
}
