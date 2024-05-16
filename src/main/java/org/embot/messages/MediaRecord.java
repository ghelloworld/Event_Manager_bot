package org.embot.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

/**
 * Представляет класс для хранения поля @Nullable media в классе EntityRecord. Защищено от изменений.
 * Гарантирует ненулевое значение mediaId и значение MediaType = MEDIATYPE_PHOTO или MEDIATYPE_VIDEO. Завершено.
 */
public record MediaRecord(@JsonProperty("media_type") int mediaType,
                          @JsonProperty("media_id") @NotNull String mediaId) {

    public static final int PHOTO = 1;
    public static final int VIDEO = 2;

    public MediaRecord {
        if (mediaType != PHOTO && mediaType != VIDEO) throw new IllegalArgumentException();
    }

    /* Возвращает экземпляр из ресурсов
     * @throws IllegalArgumentException если такого ресурса нет*/
    /*@JsonIgnore
    public static @NotNull MediaRecord fromResource(@NotNull String resourceId) {
        if (resourceId == null) throw new NullPointerException();
        var result = Data.media.get(resourceId);
        if (result == null) throw new IllegalArgumentException();
        return result;
    }*/
}