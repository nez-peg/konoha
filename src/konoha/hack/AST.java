package konoha.hack;

import konoha.script.ScriptContext;

public class AST {

	public final static void hack(ScriptContext context) {
		context.enableASTDump = !context.enableASTDump;
	}
}
