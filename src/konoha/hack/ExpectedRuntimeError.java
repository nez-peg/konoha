package konoha.hack;

import konoha.script.ScriptContextError;

public class ExpectedRuntimeError {
	static {
		konoha.main.Main.expected = ScriptContextError.RuntimeError;
	}
}