package konoha.hack;

import konoha.main.ConsoleUtils;
import konoha.message.Message;
import konoha.script.ScriptContext;
import konoha.script.TypeSystem;

public class Messages extends Hacker {

	@Override
	public void perform(ScriptContext context, TypeSystem typeSystem) {
		for (Message m : Message.values()) {
			ConsoleUtils.println(m.name() + ": " + m.toString());
		}
	}
}
