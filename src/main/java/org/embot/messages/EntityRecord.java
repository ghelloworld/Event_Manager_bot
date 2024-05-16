package org.embot.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;

/** Представляет класс для хранения одного Entity. При изменении создается новый экземпляр.
 * Гарантирует ненулевое значение type. Гарантирует неотрицательные значения offset и length. Завершено.*/
public record EntityRecord(@JsonProperty("type") @NotNull String type,
                           @JsonProperty("offset") int offset,
                           @JsonProperty("length") int length,
                           @JsonProperty("url") @Nullable String url,
                           @JsonProperty("user") @Nullable UserRecord user,
                           @JsonProperty("language") @Nullable String language,
                           @JsonProperty("custom_emoji_id") @Nullable String customEmojiId) {
    /** Создает экземпляр новый экземпляр класса.
     * Гарантирует ненулевое значение type. Гарантирует неотрицательные значения offset и length.
     * @throws NullPointerException если type = null
     * @throws IllegalArgumentException если offset или length = < 0*/
    @JsonCreator
    public EntityRecord {
        if (offset < 0 || length < 0) throw new IllegalArgumentException();
    }

    @JsonIgnore
    public @NotNull EntityRecord setType(@NotNull String type) {
        return new EntityRecord(type, offset, length, url, user, language, customEmojiId);
    }

    @JsonIgnore
    public @NotNull EntityRecord setOffset(int offset) {
        return new EntityRecord(type, offset, length, url, user, language, customEmojiId);
    }

    @JsonIgnore
    public @NotNull EntityRecord setLength(int length) {
        return new EntityRecord(type, offset, length, url, user, language, customEmojiId);
    }

    @JsonIgnore
    public @NotNull EntityRecord setUrl(@Nullable String url) {
        return new EntityRecord(type, offset, length, url, user, language, customEmojiId);
    }

    @JsonIgnore
    public @NotNull EntityRecord setUser(@Nullable UserRecord user) {
        return new EntityRecord(type, offset, length, url, user, language, customEmojiId);
    }

    @JsonIgnore
    public @NotNull EntityRecord setLanguage(@Nullable String language) {
        return new EntityRecord(type, offset, length, url, user, language, customEmojiId);
    }

    @JsonIgnore
    public @NotNull EntityRecord setCustomEmojiId(@Nullable String customEmojiId) {
        return new EntityRecord(type, offset, length, url, user, language, customEmojiId);
    }

    /** Создает экземпляр класса EntityRecord из телеграмовского Entity.
     * @throws NullPointerException если параметр е = null*/
    @JsonIgnore
    public static @NotNull EntityRecord fromMessageEntity(@NotNull MessageEntity e) {
        @Nullable var resultUser = UserRecord.fromTelegramUser(e.getUser());
        return new EntityRecord(e.getType(), e.getOffset(), e.getLength(), e.getUrl(), resultUser, e.getLanguage(), e.getCustomEmojiId());
    }

    /** Создает телеграмовский Entity из этого экземпляра*/
    @JsonIgnore
    public @NotNull MessageEntity getMessageEntity() {
        @Nullable var resultUser = UserRecord.getTelegramUser(user);
        return new MessageEntity(type, offset, length, url, resultUser, language, customEmojiId, null);
    }
}
