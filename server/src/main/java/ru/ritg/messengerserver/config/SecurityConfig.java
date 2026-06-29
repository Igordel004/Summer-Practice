package ru.ritg.messengerserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

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
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/messenger/**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .anyRequest().permitAll()
            )
            .cors(cors -> cors.disable());
        return http.build();
    }
}
