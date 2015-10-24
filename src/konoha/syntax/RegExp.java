package konoha.syntax;

import java.lang.reflect.Type;

import konoha.script.ScriptContext;
import konoha.script.SyntaxTree;

public class RegExp extends SyntaxExtension {

	public static void hack(ScriptContext context) {
		context.addSyntaxExtension(new BigInteger(context));
	}

	public RegExp(ScriptContext context) {
		super(context);
	}

	@Override
	public String getName() {
		return "Integer";
	}

	@Override
	public Type acceptType(SyntaxTree node) {
		// TODO;;
		return null;
	}

}
