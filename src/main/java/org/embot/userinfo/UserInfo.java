package org.embot.userinfo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public class UserInfo {

	private static final String JSON_TG_FIRSTNAME = "telegram_first_name";
	private static final String JSON_PSN_NICKNAME = "psn_nickname";
	private static final String JSON_GT7_NICKNAME = "gt7_nickname";
	private static final String JSON_FIRST_NAME = "first_name";
	private static final String JSON_LAST_NAME = "last_name";
	private static final String JSON_ACCESS_LEVEL = "access_level";
	private static final String JSON_IS_BANNED = "is_banned";

	public static final int BOT_ADMIN = 1;
	public static final int GROUP_ADMIN = 2;
	public static final int EVENT_EDITOR = 4;
	public static final int EVENT_PRODUCER = 8;
	public static final int IGNORE_OWNER = 16;
	public static final int SUPER = BOT_ADMIN | GROUP_ADMIN | EVENT_PRODUCER | EVENT_EDITOR | IGNORE_OWNER;

	@JsonIgnore
	public UserInfo(@NotNull String telegramFirstName) {
		this.telegramFirstName = telegramFirstName;
		this.accessLevel = 0;
		this.banned = false;
	}

	@JsonCreator
	public UserInfo(@JsonSetter(JSON_TG_FIRSTNAME) @NotNull String telegramFirstName,
					@JsonSetter(JSON_PSN_NICKNAME) String psnNickname,
					@JsonSetter(JSON_GT7_NICKNAME) String gt7Nickname,
					@JsonSetter(JSON_FIRST_NAME) String firstName,
					@JsonSetter(JSON_LAST_NAME) String lastName,
					@JsonSetter(JSON_ACCESS_LEVEL) int accessLevel,
					@JsonSetter(JSON_IS_BANNED) boolean banned) {
		this.telegramFirstName = telegramFirstName;
		this.psnNickname = psnNickname;
		this.gt7Nickname = gt7Nickname;
		this.firstName = firstName;
		this.lastName = lastName;
		this.accessLevel = accessLevel;
		this.banned = banned;
	}

	private String telegramFirstName;
	private String psnNickname;
	private String gt7Nickname;
	private String firstName;
	private String lastName;
	private int accessLevel;
	private boolean banned;

	@JsonGetter(JSON_TG_FIRSTNAME) public @NotNull String getTelegramFirstName() { return telegramFirstName; }
	@JsonGetter(JSON_PSN_NICKNAME) public @Nullable String getPsnNickname() { return psnNickname; }
	@JsonGetter(JSON_GT7_NICKNAME) public @Nullable String getGt7Nickname() { return gt7Nickname; }
	@JsonGetter(JSON_FIRST_NAME) public @Nullable String getFirstName() { return firstName; }
	@JsonGetter(JSON_LAST_NAME) public @Nullable String getLastName() { return lastName; }
	@JsonGetter(JSON_ACCESS_LEVEL) public int getAccessLevel() { return accessLevel; }
	@JsonGetter(JSON_IS_BANNED) public boolean isBanned() { return banned; }

	public boolean hasPermission(int required) { return (this.accessLevel & required) == required; }

	public void setTelegramFirstName(@NotNull String telegramFirstName) {
		this.telegramFirstName = telegramFirstName;
	}

	public void setAccessLevel(int accessLevel) { this.accessLevel = accessLevel; }

	public void upAccessLevel(int accessLevel) { this.accessLevel = this.accessLevel | accessLevel; }

	public void setBanned(boolean banned) { this.banned = banned; }

	public boolean setPsnNickname(@NotNull String value) {
		if (value.length() > 30 || !checkPsnNickname(value)) return false;
		this.psnNickname = value;
		return true;
	}

	public boolean setGt7Nickname(@NotNull String value) {
		if (value.length() > 30 || value.isBlank()) return false;
		this.gt7Nickname = value;
		return true;
	}

	public boolean setFirstName(@NotNull String value) {
		if (value.length() > 30) return false;
		var result = checkCyrillicName(value);
		if (result == null) return false;
		this.firstName = result;
		return true;
	}

	public boolean setLastName(@NotNull String value) {
		if (value.length() > 30) return false;
		var result = checkCyrillicName(value);
		if (result == null) return false;
		this.lastName = result;
		return true;
	}

	private static final Pattern psnPattern = Pattern.compile("\\s*[a-zA-Z0-9,_,-]{1,30}\\s*");

	private static boolean checkPsnNickname(@NotNull String psn) {
		var matcher = psnPattern.matcher(psn);
		return matcher.matches();
	}

	private static @Nullable String checkCyrillicName(@NotNull String name) {
		StringBuilder sb = new StringBuilder(name.length() + 1);
		boolean isNextPart = true;
		boolean isOk = true;
		for (var c : name.toCharArray()) {
			if (Character.UnicodeBlock.of(c).equals(Character.UnicodeBlock.CYRILLIC)) {
				if (isNextPart) {
					sb.append(' ');
					sb.append(Character.toUpperCase(c));
					isNextPart = false;
				} else sb.append(Character.toLowerCase(c));
			} else if (Character.isWhitespace(c)) {
				isNextPart = true;
			} else {
				isOk = false;
				break;
			}
		}
		if (!isOk || sb.isEmpty()) return null;
		return sb.substring(1);
	}
}
