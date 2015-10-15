package konoha.hack;

import konoha.main.ConsoleUtils;
import konoha.script.ScriptContext;
import konoha.script.TypeSystem;

public class AST extends Hacker {

	@Override
	public void perform(ScriptContext context, TypeSystem typeSystem) {
		context.enableASTDump = !context.enableASTDump;
		ConsoleUtils.println("turning AST dump: " + context.enableASTDump);
	}
}
