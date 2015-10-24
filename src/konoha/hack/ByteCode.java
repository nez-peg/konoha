package konoha.hack;

import konoha.script.ScriptContext;

public class ByteCode {
	public final static void hack(ScriptContext context) {
		konoha.asm.ScriptClassLoader.enabledDump = !konoha.asm.ScriptClassLoader.enabledDump;
	}
}
