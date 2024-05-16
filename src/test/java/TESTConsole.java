import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.embot.data.Data;
import org.embot.messages.EntityRecord;
import org.embot.messages.TextRecord;
import org.embot.messages.UserRecord;
import org.embot.userinfo.UserInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Hashtable;
import java.util.List;

public class TESTConsole {
	public static void main(String... args) throws JsonProcessingException {
		var user = new UserRecord(213L,"First name", true);
		var entity = new EntityRecord("efe",11,32,"Url",user,"Lang","Emoji");
		var text = new TextRecord("This is text", List.of(entity));

		var a = new A() {
			@Override
			void a() {

			}
		};

		var mapper = new ObjectMapper();
		var s = mapper.writeValueAsString(text);
		System.out.println(s);

		var b = mapper.readValue(s, TextRecord.class);
		System.out.println(b);
	}
}

abstract class A {
	abstract void a();
}