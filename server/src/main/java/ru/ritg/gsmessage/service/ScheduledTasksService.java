package ru.ritg.gsmessage.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Сервис фоновых задач.
 *
 * <p>Управляет Cron-задачами: очистка просроченных OTP и сессий.</p>
 */
@Service
public class ScheduledTasksService {

    /**
     * Удалить просроченные OTP-коды.
     *
     * <p>Запускается по расписанию (cron: 0 0 3 * * * — ежедневно в 03:00).</p>
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupExpiredVerificationCodes() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Удалить просроченные сессии.
     *
     * <p>Запускается по расписанию (cron: 0 0 4 * * * — ежедневно в 04:00).</p>
     */
    @Scheduled(cron = "0 0 4 * * *")
    public void cleanupExpiredSessions() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
