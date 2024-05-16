package org.embot.messages;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.telegram.telegrambots.meta.api.objects.User;

/**
 * Представляет класс для хранения поля @Nullable user в классе EntityRecord. Защищено от изменений.
 * Гарантирует ненулевое значение firstName. Завершено.
 */
public record UserRecord(@JsonProperty("user_id") long userId,
                         @JsonProperty("first_name") @NotNull String firstName,
                         @JsonProperty("is_bot") boolean isBot) {

    /** Создает экземпляр класса из телеграмовского User.
     * Возвращает null если параметр user = null*/
    @JsonIgnore
    public static @Nullable UserRecord fromTelegramUser(@Nullable User user) {
        if (user == null) return null;
        return new UserRecord(user.getId(), user.getFirstName(), user.getIsBot());
    }

    /** Возвращает ограниченный телеграмовский User. Возвращает null если параметр user = null*/
    @JsonIgnore
    public static @Nullable User getTelegramUser(@Nullable UserRecord user) {
        if (user == null) return null;
        return new User(user.userId, user.firstName, user.isBot);
    }
}