package konoha.hack;

import konoha.script.ScriptContextError;

public class ExpectedTypeError {
	static {
		konoha.main.Main.expected = ScriptContextError.TypeError;
	}
}