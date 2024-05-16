package org.embot.requests;

import org.embot.utils.Utils;
import org.embot.messages.MessageRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class IR {

	public static final int ACCEPT_REPLY_ONLY = 1;
	public static final int LOCK_OTHER_BUTTONS = 2;
	public static final int DELETE_REQUEST_MESSAGE = 4;
	public static final int DELETE_ANSWER_MESSAGE = 8;
	public static final int DEFAULT_OPTIONS = LOCK_OTHER_BUTTONS | DELETE_ANSWER_MESSAGE | DELETE_REQUEST_MESSAGE;
	public static final String CANCEL_COMMAND = "ir_cancel";

	//////////////////////// TYPES ////////////////////////

	public interface OnAnswer { @Nullable MessageRecord onAnswer(@NotNull Message message); }

	private record Request(long chatId,
						   @Nullable Integer threadId,
						   int requestMessageId,
						   int flags,
						   @NotNull IR.OnAnswer observer) {}

	/*TODO Cancel all before bot stopped*/
	private static final Hashtable<Long,Request> requestMap = new Hashtable<>();

	//////////////////////// METHODS ////////////////////////

	public static void toMakeRequest(long userId,
									 long chatId,
									 @Nullable Integer threadId,
									 @NotNull MessageRecord requestMessage,
									 @NotNull IR.OnAnswer observer,
									 int flags) {
		toCancel(userId);
		DM.toCancel(userId);

		var button = InlineKeyboardButton.builder().text("Отмена").callbackData(CANCEL_COMMAND).build();
		var buttons = new InlineKeyboardMarkup(Collections.singletonList(Collections.singletonList(button)));

		var sent = Utils.sendMessage(requestMessage.setKeyboard(buttons),chatId,threadId);
		if (sent != null) {
			int requestMessageId = sent.getMessageId();
			requestMap.put(userId,new Request(chatId,threadId,requestMessageId,flags,observer));
		}
	}

	public static void toCancel(long userId) {
		var current = requestMap.get(userId);
		if (current != null) {
			Utils.deleteMessage(current.chatId,current.requestMessageId);
			requestMap.remove(userId);
		}
	}

	public static boolean isProcessed(@NotNull Message message) {

		long userId = message.getFrom().getId();
		var current = requestMap.get(userId);
		if (current == null || current.chatId() != message.getChatId()) return false;
		/*Дальше мы точно знаем что наш юзер и наш чат*/

		if ((current.flags & ACCEPT_REPLY_ONLY)!=0) {
			if (message.isReply() && (current.requestMessageId == message.getReplyToMessage().getMessageId())) {
				toProcessAnswer(userId, current, message);
				return true;
			} else return false;
		} else {
			if (Objects.equals(current.threadId,message.getMessageThreadId())) {
				toProcessAnswer(userId, current, message);
				return true;
			} else return false;
		}
	}

	private static void toProcessAnswer(long userId, @NotNull Request current, @NotNull Message message) {
		var answer = current.observer.onAnswer(message);
		if (answer != null) {
			var resultMessage = Utils.sendMessage(answer, current.chatId, current.threadId);
			if (resultMessage != null ){
				ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
				service.schedule(() -> {
					Utils.deleteMessage(resultMessage.getChatId(), resultMessage.getMessageId());
				}, 2, TimeUnit.SECONDS);
			}
		}

		int i = 0;
		int[] toDelete = new int[2];
		if ((current.flags & DELETE_REQUEST_MESSAGE)!=0) toDelete[i++] = current.requestMessageId;
		if ((current.flags & DELETE_ANSWER_MESSAGE)!=0) toDelete[i++] = message.getMessageId();
		if (i != 0) {
			if (i == 1) Utils.deleteMessage(current.chatId,toDelete[0]);
			else Utils.deleteMessages(current.chatId,toDelete[0],toDelete[1]);
		}

		requestMap.remove(userId);
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

		return false;
	}
}