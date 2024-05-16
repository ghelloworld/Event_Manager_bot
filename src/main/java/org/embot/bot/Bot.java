package org.embot.bot;

import org.embot.BotToken;
import org.embot.data.Data;
import org.embot.messages.MessageRecord;
import org.embot.messages.TextRecord;
import org.embot.requests.DM;
import org.embot.requests.IR;
import org.embot.userinfo.UserInfo;
import org.embot.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Hashtable;
import java.util.List;

public class Bot extends TelegramLongPollingBot {
	private Bot(DefaultBotOptions options, String botToken) {
		super(options, botToken);
	}

	private static Bot createThis() {
		var options = new DefaultBotOptions();
		options.setAllowedUpdates(List.of("message", "edited_message", "channel_post", "edited_channel_post", "callback_query"));
		var botToken = BotToken.TEST_BOT;
		return new Bot(options, botToken);
	}

	private static final Bot bot = createThis();

	public static Bot get() {
		return bot;
	}

	@Override
	public String getBotUsername() {
		return "UserChat bot";
	}

	@Override
	public void onUpdateReceived(Update update) {

		/* TODO исправить threadId */
		try {
			if (update.hasCallbackQuery()) {
				var callbackQuery = update.getCallbackQuery();
				if (Data.optUserInfo(callbackQuery.getFrom()).isBanned()) return;
				if (callbackQuery.getMessage().isUserMessage())
					onCallbackQuery(update.getCallbackQuery());
			} else if (update.hasMessage()) {
				var message = update.getMessage();
				if (Data.optUserInfo(message.getFrom()).isBanned()) return;
				if (message.isCommand())
					onCommand(message);
				else
					onMessage(message);
			}
		} catch (Exception e) {
			System.out.println(e.toString());
			e.printStackTrace(System.out);
		}
	}

	//////////////////////// BUTTONS ////////////////////////
	private interface ButtonHandler { @Nullable String toHandle(@NotNull CallbackQuery callbackQuery, @Nullable String parameter); }
	private record ButtonListener(@NotNull ButtonHandler handler, int required, @Nullable Long owner, boolean isGroupAllowed) {}
	private final Hashtable<String, ButtonListener> buttonMap = new Hashtable<>();

	private void onCallbackQuery(@NotNull CallbackQuery callbackQuery) {
		String data = callbackQuery.getData();
		/* TODO переделать */
		if (data == null || data.isBlank()) return;
		String answerText = null;
		if (!IR.isCallbackProcessed(callbackQuery) && !DM.isCallbackProcessed(callbackQuery)) {
			int p = data.indexOf(' ');
			String command = (p != -1) ? data.substring(0,p) : data;
			String parameter = (p != -1) ? data.substring(p+1) : "";
			if (command.isEmpty()) return;
			if (parameter.isEmpty()) parameter = null;


			var listener = buttonMap.get(command);
			if (listener != null) {
				if (!callbackQuery.getMessage().isUserMessage() && !listener.isGroupAllowed) return;
				if (isAllowed(callbackQuery.getFrom(), listener.required, listener.owner))
					answerText = listener.handler.toHandle(callbackQuery, parameter);
			}
		}
		System.out.println(answerText);
		var answer = new AnswerCallbackQuery(callbackQuery.getId(), answerText, false, null, 0);
		try { execute(answer); } catch (TelegramApiException ignored) { System.out.println(ignored); }
	}

	//////////////////////// COMMANDS ////////////////////////
	private interface CommandHandler { @Nullable MessageRecord toHandle(@NotNull Message message, @Nullable String parameter); }
	private record CommandListener(@NotNull CommandHandler handler, int required, @Nullable Long owner, boolean isGroupAllowed) {}
	private final Hashtable<String, CommandListener> commandMap = new Hashtable<>();

	private void onCommand(@NotNull Message message) {
		IR.toCancel(message.getFrom().getId());
		DM.toCancel(message.getFrom().getId());

		String text = message.getText();
		int p1 = text.indexOf('_');
		int p2 = text.indexOf(' ');
		int p = (p1 == -1) ? p2 : (p2 == -1) ? p1 : Math.min(p1, p2);
		String command = (p != -1) ? text.substring(1, p) : text.substring(1);
		String parameter = (p != -1) ? text.substring(p + 1) : "";
		if (command.isEmpty()) return;
		if (parameter.isEmpty()) parameter = null;

		var listener = commandMap.get(command);
		if (listener != null) {
			if (!message.isUserMessage() && !listener.isGroupAllowed) return;
			if (isAllowed(message.getFrom(), listener.required, listener.owner)) {
				try {
					var answer = listener.handler.toHandle(message, parameter);
					if (answer != null)
						Utils.sendMessage(answer,message.getChatId(),message.getMessageThreadId());
				} catch (IllegalArgumentException e) { System.out.println(e.getMessage());}
			}
		}
	}

	//////////////////////// MESSAGES ////////////////////////
	private void onMessage(Message message) {
		if (!IR.isProcessed(message)){
			System.out.println("unhandled message");
		}
	}

	//////////////////////// HELPERS ////////////////////////
	private boolean isAllowed(@NotNull User user, int required, @Nullable Long owner) {
		var userInfo = Data.optUserInfo(user);
		var cond1 = userInfo.hasPermission(required);
		var cond2 = owner == null || owner.equals(user.getId());
		var cond3 = userInfo.hasPermission(UserInfo.IGNORE_OWNER);
		return cond1 && (cond2 || cond3);
	}

	//////////////////////// INIT ////////////////////////
	public void init() {
		commandMap.put("userlist", new CommandListener(new CommandHandler() {
			@Override
			public MessageRecord toHandle(@NotNull Message message, @Nullable String parameter) {
				var list = Utils._user_list();
				return new MessageRecord(null,list,null,null);
			}
		}, UserInfo.BOT_ADMIN, null, false));

		commandMap.put("useroptions",new CommandListener(new CommandHandler() {
			@Override
			public MessageRecord toHandle(@NotNull Message message, @Nullable String parameter) {
				var user = parameterToUserIdInfo(parameter, "Existing user id required");
				var userId = user.userId;
				var userInfo = user.userInfo;

				var text = Utils._user_details(userId, userInfo);
				text = text.append(new TextRecord("\nБан: /ban_"+userId));
				text = text.append(new TextRecord("\nИзменить имя: /setfirstname_"+userId));
				var button = InlineKeyboardButton.builder()
						.text((userInfo.isBanned() ? "разблокировать " : "заблокировать ") + userInfo.getTelegramFirstName())
						.callbackData("switchban " + userId)
						.build();
				var markup = new InlineKeyboardMarkup(List.of(List.of(button)));
				return new MessageRecord(null,text,markup,null);
			}
		}, UserInfo.BOT_ADMIN, null, false));

		commandMap.put("ban", new CommandListener(new CommandHandler() {
			@Override
			public MessageRecord toHandle(@NotNull Message message, @Nullable String parameter) {
				var userInfo = parameterToUserIdInfo(parameter, "Existing user id required").userInfo;
				userInfo.setBanned(!userInfo.isBanned());
				return new MessageRecord(null,new TextRecord("Принято!"),null,null);
			}
		}, UserInfo.BOT_ADMIN, null, false));

		commandMap.put("setfirstname", new CommandListener(new CommandHandler() {
			@Override
			public MessageRecord toHandle(@NotNull Message message, @Nullable String parameter) {
				var userInfo = parameterToUserIdInfo(parameter, "Existing user id required").userInfo;
				var request = new MessageRecord(null,new TextRecord("Введите имя:"),null,null);
				IR.toMakeRequest(message.getFrom().getId(), message.getChatId(), message.getMessageThreadId(), request, new IR.OnAnswer() {
					@Override
					public MessageRecord onAnswer(@NotNull Message message) {
						String answer;
						if (userInfo.setFirstName(message.getText())) answer = "Принято!";
						else answer = "Ошибка!";
						return new MessageRecord(null,new TextRecord(answer),null,null);
					}
				}, IR.DEFAULT_OPTIONS);
				return null;
			}
		}, UserInfo.BOT_ADMIN, null, false));

		buttonMap.put("switchban", new ButtonListener(new ButtonHandler() {
			@Override
			public String toHandle(@NotNull CallbackQuery callbackQuery, @Nullable String parameter) {
				var userInfo = parameterToUserIdInfo(parameter, "Existing user id required").userInfo;
				userInfo.setBanned(!userInfo.isBanned());
				return userInfo.getTelegramFirstName() + (userInfo.isBanned() ? " заблокирован" : " разблокирован");
			}
		}, UserInfo.BOT_ADMIN, null, false));
	}

	private record UserIdInfo(long userId, @NotNull UserInfo userInfo) {}

	private @NotNull UserIdInfo parameterToUserIdInfo(@Nullable String parameter, @NotNull String reportMessage) {
		long userId = parameterToLong(parameter,reportMessage);
		var userInfo = Data.userMap.get(userId);
		if (userInfo == null) throw new IllegalArgumentException(reportMessage);
		return new UserIdInfo(userId, userInfo);
	}

	private long parameterToLong(@Nullable String parameter, @NotNull String reportMessage) {
		if (parameter == null) throw new IllegalArgumentException(reportMessage);
		long value;
		try { value = Long.parseLong(parameter); } catch (NumberFormatException e) { throw new IllegalArgumentException(reportMessage); }
		return value;
	}
}
