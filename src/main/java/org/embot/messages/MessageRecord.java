package org.embot.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.LinkPreviewOptions;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

/* Не для JSON */
@JsonIgnoreType
public record MessageRecord(@Nullable MediaRecord media,
                            @Nullable TextRecord text,
                            @Nullable InlineKeyboardMarkup replyMarkup,
                            @Nullable LinkPreviewOptions linkPreview) {

    private static final MessageRecord empty = new MessageRecord(null,null,null,null);

    public @NotNull MessageRecord setMedia(@Nullable MediaRecord media) {
        return new MessageRecord(media, text, replyMarkup, linkPreview);
    }

    public @NotNull MessageRecord setText(@Nullable TextRecord text) {
        return new MessageRecord(media, text, replyMarkup, linkPreview);
    }

    public @NotNull MessageRecord setKeyboard(@Nullable InlineKeyboardMarkup replyMarkup) {
        return new MessageRecord(media, text, replyMarkup, linkPreview);
    }

    public @NotNull MessageRecord setLinkOptions(@Nullable LinkPreviewOptions linkPreview) {
        return new MessageRecord(media, text, replyMarkup, linkPreview);
    }

    public static @NotNull MessageRecord fromMessage(@NotNull Message message) {
        if (message.hasText()) {
            var text = TextRecord.fromMessageText(message);
            return new MessageRecord(null,text,message.getReplyMarkup(),message.getLinkPreviewOptions());
        } else if (message.hasPhoto()) {
            var text = message.getCaption() != null ? TextRecord.fromMessageCaption(message) : null;
            var media = new MediaRecord(MediaRecord.PHOTO,message.getPhoto().get(message.getPhoto().size()-1).getFileId());
            return new MessageRecord(media,text,message.getReplyMarkup(),null);
        } else if (message.hasVideo()) {
            var text = message.getCaption() != null ?  TextRecord.fromMessageCaption(message) : null;
            var media = new MediaRecord(MediaRecord.VIDEO,message.getVideo().getFileId());
            return new MessageRecord(media,text,message.getReplyMarkup(),null);
        } else return empty;
    }
}