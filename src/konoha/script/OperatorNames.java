package konoha.script;

import java.util.HashMap;

public class OperatorNames {
	static HashMap<String, String> names = new HashMap<>();

	static void s(String n, String m) {
		names.put(n, m);
		names.put(m, n);
	}

	static {
		s("+", "add");
		s("-", "subtract");
		s("*", "multiply");
		s("/", "divide");
		s("%", "mod");
		s("==", "eq");
		s("!=", "ne");
		s("<=", "lte");
		s("<", "lt");
		s(">", "gt");
		s(">=", "gte");
		s("&", "and");
		s("|", "or");
		s("^", "xor");
		s("~", "not");
		s("-", "negate");
	}

	public final static String name(String n) {
		return names.get(n);
	}
}
