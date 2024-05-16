package org.embot.data;

import org.embot.userinfo.UserInfo;
import org.jetbrains.annotations.NotNull;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.Hashtable;

public class Data {
    static {
        Hashtable<Long, UserInfo> _userMap = new Hashtable<>();
        var me = new UserInfo("Ivan V.");
        me.upAccessLevel(UserInfo.SUPER);
        me.setFirstName("иван");
        me.setLastName("чиллов");
        me.setPsnNickname("chappie4573");

        var test = new UserInfo("Test user");
        test.setBanned(true);

        _userMap.put(1251052878L,me);
        _userMap.put(1253434478L,test);
        userMap = _userMap;
    }
    public static final Hashtable<Long, UserInfo> userMap;

    public static @NotNull UserInfo optUserInfo(@NotNull User telegramUser) {
        UserInfo userInfo = userMap.get(telegramUser.getId());
        if (userInfo == null) {
            var newUser = new UserInfo(telegramUser.getFirstName());
            /* TODO игнорировать анонимов в группах */
            if (telegramUser.getIsBot()) newUser.setBanned(true);
            userMap.put(telegramUser.getId(), newUser);
            return newUser;
        }
        return userInfo;
    }
}
