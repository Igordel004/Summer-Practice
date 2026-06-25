package ru.ritg.gsmessage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Точка входа.
 *
 * <p>Активирует:</p>
 * <ul>
 *   <li>Автоконфигурацию Spring Boot</li>
 *   <li>Планировщик задач (@Scheduled)</li>
 * </ul>
 */
@SpringBootApplication
@EnableScheduling
public class GsMessageApplication {

    /**
     * Главный метод запуска приложения.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        SpringApplication.run(GsMessageApplication.class, args);
    }
}
