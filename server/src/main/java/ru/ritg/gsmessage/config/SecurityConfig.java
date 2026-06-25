package ru.ritg.gsmessage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Конфигурация Spring Security.
 *
 * <p>Настраивает фильтры безопасности, CORS и публичные эндпоинты.</p>
 */
@Configuration
public class SecurityConfig {

    /**
     * Создать цепочку фильтров безопасности.
     *
     * @param http конфигуратор HTTP-безопасности
     * @return {@link SecurityFilterChain}
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Создать кодировщик паролей BCrypt.
     *
     * @return {@link PasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
