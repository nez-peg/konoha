package konoha.syntax;

import java.lang.reflect.Type;
import java.util.regex.Pattern;

import konoha.script.ScriptContext;
import konoha.script.SyntaxTree;

public class RegExp extends SyntaxExtension {

	public static void hack(ScriptContext context) {
		context.addSyntaxExtension(new RegExp(context));
	}

	public RegExp(ScriptContext context) {
		super(context);
	}

	@Override
	public String getName() {
		return "RegExp";
	}

	@Override
	public Type acceptType(SyntaxTree node) {
		String regExp = node.toText();
		regExp = regExp.substring(1, regExp.length() - 1);
		Pattern p = Pattern.compile(regExp);
		return node.setConst(Pattern.class, p);
	}

	@Override
	public Object acceptEval(SyntaxTree node) {
		return node.getValue();
	}

}
