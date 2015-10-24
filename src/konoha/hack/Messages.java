package konoha.hack;

import konoha.main.ConsoleUtils;
import konoha.message.Message;
import konoha.script.ScriptContext;

public class Messages {

	public final static void hack(ScriptContext context) {
		for (Message m : Message.values()) {
			ConsoleUtils.println(m.name() + ": " + m.toString());
		}
	}
}
