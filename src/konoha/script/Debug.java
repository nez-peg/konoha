package konoha.script;

import nez.util.ConsoleUtils;

public class Debug {
	public static void TRACE(String fmt, Object... args) {
		ConsoleUtils.println("TRACE: " + String.format(fmt, args));
	}

	public static void TODO(String fmt, Object... args) {
		ConsoleUtils.println("TODO: " + String.format(fmt, args));
	}

	public static void DEBUG(String fmt, Object... args) {
		ConsoleUtils.println("DEBUG: " + String.format(fmt, args));
	}

}
