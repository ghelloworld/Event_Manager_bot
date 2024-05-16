package org.embot;

import org.embot.bot.Bot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
	public static void main(String[] args) {
		try {
			var botsApi = new TelegramBotsApi(DefaultBotSession.class);
			Bot.get().init();
			botsApi.registerBot(Bot.get());
		} catch (TelegramApiException e) {
			System.out.println("In Main.main(): " + e);
		}
		System.out.println("----- started -----");
	}
}