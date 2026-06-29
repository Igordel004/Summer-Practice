package ru.ritg.messengerserver.model;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

/**
 * Составной первичный ключ для сущности {@link Contact}.
 *
 * <p>Обязан реализовывать {@link Serializable} для JPA.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ContactId implements Serializable {

    private UUID user;
    private UUID contactUser;
}
