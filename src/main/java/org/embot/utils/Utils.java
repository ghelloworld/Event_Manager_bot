package org.embot.utils;

import org.embot.bot.Bot;
import org.embot.data.Data;
import org.embot.messages.MessageRecord;
import org.embot.messages.UserRecord;
import org.embot.messages.TextRecord;
import org.embot.messages.MediaRecord;
import org.embot.messages.EntityRecord;
import org.embot.userinfo.UserInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessages;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaVideo;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

public class Utils {

    static private final Bot bot = Bot.get();

    /* TODO Перенести функционал проверки содержания MessageRecord в MessageRecord*/
    public static @Nullable Message sendMessage(@NotNull MessageRecord record, long chatId, @Nullable Integer threadId) {
        if (bot == null) throw new NullPointerException();
        try {
            @Nullable var text = record.text();
            @Nullable var media = record.media();

            if (text != null && media == null) {
                var send = new SendMessage();
                send.setText(text.text());
                if (text.entities() != null) send.setEntities(text.getMessageEntities());
                if (record.replyMarkup() != null) send.setReplyMarkup(record.replyMarkup());
                if (record.linkPreview() != null) send.setLinkPreviewOptions(record.linkPreview());
                send.setChatId(chatId);
                if (threadId != null) send.setMessageThreadId(threadId);
                return bot.execute(send);
            } else if (media != null && media.mediaType() == MediaRecord.PHOTO) {
                var send = new SendPhoto();
                send.setPhoto(new InputFile(media.mediaId()));
                if (text != null) {
                    send.setCaption(text.text());
                    if (text.entities() != null) send.setCaptionEntities(text.getMessageEntities());
                }
                if (record.replyMarkup() != null) send.setReplyMarkup(record.replyMarkup());
                send.setChatId(chatId);
                if (threadId != null) send.setMessageThreadId(threadId);
                return bot.execute(send);
            } else if (media != null && media.mediaType() == MediaRecord.VIDEO) {
                var send = new SendVideo();
                send.setVideo(new InputFile(media.mediaId()));
                if (text != null) {
                    send.setCaption(text.text());
                    if (text.entities() != null) send.setCaptionEntities(text.getMessageEntities());
                }
                if (record.replyMarkup() != null) send.setReplyMarkup(record.replyMarkup());
                send.setChatId(chatId);
                if (threadId != null) send.setMessageThreadId(threadId);
                return bot.execute(send);
            } else throw new IllegalArgumentException();
        } catch (TelegramApiException e) {
            /* TODO доделать */
            System.out.println(e);
            throw new RuntimeException(e);
        }
    }

    public static void editMessage(@NotNull MessageRecord record, long chatId, int messageId, boolean isTextMessage) {
        if (bot == null) throw new NullPointerException();
        try {
            @Nullable var text = record.text();
            @Nullable var media = record.media();
            // TODO проверить, обнуляется ли entities при изменении на null в целом как себя ведет метод
            if (isTextMessage) {
                if (media != null) throw new IllegalArgumentException("in editMessage");
                var edit = new EditMessageText();
                if (text != null) {
                    edit.setText(text.text());
                    if (text.entities() != null) edit.setEntities(text.getMessageEntities());
                }
                if (record.replyMarkup() != null) edit.setReplyMarkup(record.replyMarkup());
                if (record.linkPreview() != null) edit.setLinkPreviewOptions(record.linkPreview());
                edit.setMessageId(messageId);
                edit.setChatId(chatId);
                bot.execute(edit);
            } else {
                if (text != null) {
                    var edit = new EditMessageCaption();
                    edit.setCaption(text.text());
                    if (text.entities() != null) edit.setCaptionEntities(text.getMessageEntities());
                    if (record.replyMarkup() != null) edit.setReplyMarkup(record.replyMarkup());
                    edit.setMessageId(messageId);
                    edit.setChatId(chatId);
                    try { bot.execute(edit); } catch (TelegramApiException e) { System.out.println(e); }
                }
                if (media != null) {
                    var edit = new EditMessageMedia();
                    if (media.mediaType() == MediaRecord.PHOTO)
                        edit.setMedia(new InputMediaPhoto(media.mediaId()));
                    else if (media.mediaType() == MediaRecord.VIDEO)
                        edit.setMedia(new InputMediaVideo(media.mediaId()));
                    if (text == null && record.replyMarkup() != null)
                        edit.setReplyMarkup(record.replyMarkup());
                    edit.setMessageId(messageId);
                    edit.setChatId(chatId);
                    bot.execute(edit);
                }
            }
        } catch (TelegramApiException e) { System.out.println(e); }
    }

    public static void deleteMessages(long chatId, int... messages) {
        if (bot == null || messages == null) throw new NullPointerException();
        if (messages.length == 0) throw new IllegalArgumentException();
        List<Integer> list = new ArrayList<>();
        for (var m: messages) list.add(m);
        var delete = DeleteMessages.builder()
                .chatId(chatId)
                .messageIds(list)
                .build();
        try { bot.execute(delete); } catch (TelegramApiException e) { System.out.println(e); }
    }

    public static void deleteMessage(long chatId, int message) {
        deleteMessages(chatId,message);
    }

    public static TextRecord getUserTag(User user) {
        if(user == null) throw new NullPointerException();
        var resultText = user.getFirstName();
        List<EntityRecord> resultEntities = new ArrayList<>();
        resultEntities.add(new EntityRecord("text_mention",0,resultText.length(),null, UserRecord.fromTelegramUser(user),null,null));
        return new TextRecord(resultText, resultEntities);
    }

    public static @NotNull TextRecord _user_list() {
        var text = TextRecord.empty;
        for (var user : Data.userMap.entrySet()) {
            var details = _user_details(user.getKey(), user.getValue());
            text = text.append(details);
            text = text.append(new TextRecord("\n"+"/useroptions_"+Long.toString(user.getKey())+"\n\n"));
        }
        return text;
    }

    public static @NotNull TextRecord _user_details(long userId, @NotNull UserInfo userInfo){
        var sb = new StringBuilder(100);
        sb.append("#");
        sb.append(userId);
        sb.append(": %first_name% ");

        if (userInfo.isBanned()) sb.append("\uD83D\uDED1 ");
        if (userInfo.hasPermission(UserInfo.BOT_ADMIN)) sb.append("\uD83D\uDC68\u200D\uD83D\uDD27 ");
        if (userInfo.hasPermission(UserInfo.GROUP_ADMIN)) sb.append("\uD83D\uDC69\u200D⚖\uFE0F ");
        if (userInfo.hasPermission(UserInfo.EVENT_PRODUCER)) sb.append("\uD83D\uDC68\u200D\uD83C\uDFA8 ");
        if (userInfo.hasPermission(UserInfo.EVENT_EDITOR)) sb.append("\uD83D\uDC6E\u200D♂\uFE0F ");
        if (userInfo.hasPermission(UserInfo.IGNORE_OWNER)) sb.append("\uD83E\uDD77 ");
        sb.append('\n');

        sb.append("{");
        sb.append(userInfo.getFirstName());
        sb.append("|");
        sb.append(userInfo.getLastName());
        sb.append("|");
        sb.append(userInfo.getPsnNickname());
        sb.append("|");
        sb.append(userInfo.getGt7Nickname());
        sb.append("}");

        var userMention = getUserTag(new User(userId, userInfo.getTelegramFirstName(), false));
        return new TextRecord(sb.toString()).layoutBuilder("%first_name%",userMention);
    }
}