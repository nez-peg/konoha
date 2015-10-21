package konoha.hack;

import konoha.script.ScriptContextError;

public class ExpectedSyntaxError {
	static {
		konoha.main.Main.expected = ScriptContextError.SyntaxError;
	}
}