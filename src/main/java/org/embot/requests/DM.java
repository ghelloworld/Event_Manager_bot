package org.embot.requests;

import org.embot.utils.Utils;
import org.embot.messages.MessageRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;

public class DM {

	public static final int LOCK_OTHER_BUTTONS = 2;
	public static final int DELETE_REQUEST_MESSAGE = 4;
	public static final String CANCEL_COMMAND = "dm_cancel";

	//////////////////////// TYPES ////////////////////////
	public interface OnAnswer { void onAnswer(@NotNull CallbackQuery callbackQuery); }

	/*TODO threadId не нужен?*/
	private record Request(long chatId, int requestMessageId, int flags, @NotNull OnAnswer observer) {}

	private static final Hashtable<Long, Request> requestMap = new Hashtable<>();

	//////////////////////// METHODS ////////////////////////

	public static void toMakeRequest(long userId,
	                          long chatId,
	                          @Nullable Integer threadId,
	                          @NotNull MessageRecord requestMessage,
	                          @NotNull ArrayList<List<InlineKeyboardButton>> buttons,
	                          @NotNull OnAnswer observer,
	                          int flags) {

		toCancel(userId);
		IR.toCancel(userId);

		buttons.add(Collections.singletonList(InlineKeyboardButton.builder().text("Отмена").callbackData(CANCEL_COMMAND).build()));

		var sent = Utils.sendMessage(requestMessage.setKeyboard(new InlineKeyboardMarkup(buttons)),chatId,threadId);
		if (sent != null) {
			int requestMessageId = sent.getMessageId();
			requestMap.put(userId,new DM.Request(chatId,requestMessageId,flags,observer));
		}
	}

	public static void toCancel(long userId) {
		var current = requestMap.get(userId);
		if (current != null) {
			Utils.deleteMessage(current.chatId,current.requestMessageId);
			requestMap.remove(userId);
		}
	}

	public static boolean isCallbackProcessed(@NotNull CallbackQuery callbackQuery) {
		var message = callbackQuery.getMessage();
		if (message == null) throw new NullPointerException("callbackQuery.getMessage() = null");

		long userId = callbackQuery.getFrom().getId();
		var current = requestMap.get(userId);
		if (current == null || current.chatId != message.getChatId()) return false;
		/*Дальше мы точно знаем что наш юзер и наш чат*/
		if (current.requestMessageId != message.getMessageId()) return false; /*TODO LOCK_OTHER_BUTTONS*/
		/*Нажатие на кнопку на нашем сообщении*/
		if (callbackQuery.getData().equals(CANCEL_COMMAND)) { toCancel(userId); return true; }
		/*Не отмена*/

		toProcessAnswer(userId,current,callbackQuery);
		return true;
	}

	private static void toProcessAnswer(long userId, @NotNull Request current, @NotNull CallbackQuery callbackQuery) {
		current.observer.onAnswer(callbackQuery);

		if ((current.flags & DELETE_REQUEST_MESSAGE)!=0) Utils.deleteMessage(current.chatId,current.requestMessageId);

		requestMap.remove(userId);
	}
}
