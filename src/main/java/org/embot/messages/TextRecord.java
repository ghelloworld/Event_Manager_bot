package org.embot.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/** Инкапсулирует в себе текст сообщения и привязанные к нему Entities. Защищено от изменений.
 * Гарантирует ненулевое значение text. Гарантирует ненулевые значения в списке entities посредством List.copyOf().
 * */
public record TextRecord(@JsonProperty("text") @NotNull String text,
                         @JsonProperty("entities") @Nullable List<EntityRecord> entities) {
    /** Создает экземпляр класса TextRecord. Гарантирует ненулевое значение text.
     * Гарантирует ненулевые значения в списке entities и неизменяемость entities
     * посредством List.copyOf(). Гарантирует что entities.offset и entities.length
     * не выходят за границы текста.
     * @throws NullPointerException если text = null */
    @JsonCreator
    public TextRecord(@NotNull String text, @Nullable List<EntityRecord> entities) {
        this.text = text;
        int textLength = text.length();

        if (entities == null || entities.size() == 0 || textLength == 0)
            this.entities = null;
        else {
            ArrayList<EntityRecord> resultEntities = new ArrayList<>(entities.size());
            for (var e : entities)
                if (e != null && e.offset() < textLength && e.length() != 0)
                    resultEntities.add(e.setLength(Math.min(e.offset() + e.length(), textLength) - e.offset()));
            this.entities = resultEntities.isEmpty() ? null : List.copyOf(resultEntities);
        }
    }

    /** Создает экземпляр класса из строки*/
    @JsonIgnore
    public TextRecord(@NotNull String text) {
        this(text,null);
    }

    /** Представляет пустой текст*/
    public static final TextRecord empty = new TextRecord("");

    /** Возвращает длину текста*/
    @JsonIgnore
    public int length() { return text.length(); }

    /* OK */
    @JsonIgnore
    public @NotNull TextRecord subText(int subBegin, int subEnd) {
        ArrayList<EntityRecord> resultEntities;
        var resultText = text.substring(subBegin, subEnd);
        // substring гарантирует допустимость subBegin и subEnd
        if (subBegin == subEnd) return TextRecord.empty;
        if (entities == null) resultEntities = null;
        else {
            resultEntities = new ArrayList<>(entities.size());
            for (var e : entities) {
                int eBegin = e.offset();
                int eEnd = e.offset() + e.length();
                if ((subBegin >= eEnd) || (subEnd <= eBegin)) continue;
                int resultBegin = Math.max(eBegin, subBegin) - subBegin;
                int resultEnd = Math.min(eEnd, subEnd) - subBegin;
                int resultLength = resultEnd - resultBegin;
                resultEntities.add(e.setOffset(resultBegin).setLength(resultLength));
            }
        }
        return new TextRecord(resultText, resultEntities);
    }

    /* OK */
    @JsonIgnore
    public static @NotNull TextRecord add(@NotNull TextRecord a, @NotNull TextRecord b) {
        if (a.length() == 0) return b; else if (b.length() == 0) return a;
        var resultText = a.text() + b.text();

        if (a.entities == null && b.entities == null) return new TextRecord(resultText);
        if (b.entities == null) return new TextRecord(resultText, a.entities);
        ArrayList<EntityRecord> resultEntities = a.entities != null ? new ArrayList<>(a.entities) : new ArrayList<>(b.entities.size());
        for (var e : b.entities)
            resultEntities.add(e.setOffset(a.length() + e.offset()));
        return new TextRecord(resultText, resultEntities);
    }

    /* OK */
    @JsonIgnore
    public @NotNull TextRecord append(@NotNull TextRecord append) {
        return add(this,append);
    }

    /* OK */
    @JsonIgnore
    public @NotNull TextRecord layoutBuilder(@NotNull Map<String, TextRecord> map) {
        if (map.size() == 0) return this;

        int currentIndex = 0;
        int closestKey;

        Entry<String, TextRecord> entry = null;
        TextRecord result = TextRecord.empty;
        do {
            closestKey = -1;
            for (var e : map.entrySet()) {
                int indexOf = text.indexOf(e.getKey(), currentIndex);
                if ((indexOf != -1) && (closestKey == -1 ^ indexOf < closestKey)) {
                    closestKey = indexOf;
                    entry = e;
                }
            }
            if (closestKey == -1) {
                if (currentIndex >= text.length()) break;
                result = TextRecord.add(result, this.subText(currentIndex, this.length()));
            } else {
                if (closestKey != currentIndex)
                    result = TextRecord.add(result, this.subText(currentIndex, closestKey));
                result = TextRecord.add(result, entry.getValue());
                currentIndex = closestKey + entry.getKey().length();
            }
        } while (closestKey != -1);
        return result;
    }

    /* OK */
    @JsonIgnore
    public @NotNull TextRecord layoutBuilder(@NotNull String key, TextRecord value) {
        return layoutBuilder(Map.of(key,value));
    }

    /** Возвращает телеграмовские entities, или null если они остутствуют*/
    @JsonIgnore
    public @Nullable List<MessageEntity> getMessageEntities() {
        if (entities == null) return null;
        else {
            List<MessageEntity> resultEntities = new ArrayList<>(entities.size());
            for (var e : entities)
                resultEntities.add(e.getMessageEntity());
            return resultEntities;
        }
    }

    /** Возвращает ArrayList список EntityRecord из MessageEntity или null
     * если entities = null или entities.size() = 0*/
    @JsonIgnore
    public static @Nullable ArrayList<EntityRecord> fromMessageEntities(@Nullable List<MessageEntity> entities) {
        if (entities == null || entities.size() == 0) return null;
        else {
            ArrayList<EntityRecord> resultEntities = new ArrayList<>(entities.size());
            for (var e : entities) {
                resultEntities.add(EntityRecord.fromMessageEntity(e));
            }
            return resultEntities;
        }
    }

    /** Возвращает новый экземпляр TextRecord из Message*/
    @JsonIgnore
    public static @NotNull TextRecord fromMessageText(@NotNull Message message) {
        if (message.getText() == null) throw new IllegalArgumentException();
        var text = message.getText();
        var entities = fromMessageEntities(message.getEntities());
        return new TextRecord(text,entities);
    }

    /** Возвращает новый экземпляр TextRecord из Message*/
    @JsonIgnore
    public static @NotNull TextRecord fromMessageCaption(@NotNull Message message) {
        if (message.getCaption() == null) throw new IllegalArgumentException();
        var text = message.getCaption();
        var entities = fromMessageEntities(message.getCaptionEntities());
        return new TextRecord(text,entities);
    }


    /* Возвращает экземпляр из ресурсов
     * @throws IllegalArgumentException если такого ресурса нет*/
    /*@JsonIgnore
    public static @NotNull TextRecord fromResource(@NotNull String resourceId) {
        if (resourceId == null) throw new NullPointerException();
        var result = Data.text.get(resourceId);
        if (result == null) throw new IllegalArgumentException();
        return result;
    }*/
}

