package konoha.hack;

import konoha.script.ScriptContextError;

public class ExpectedAssertionError {
	static {
		konoha.main.Main.expected = ScriptContextError.AssertonError;
	}
}
