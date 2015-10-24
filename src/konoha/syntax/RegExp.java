package konoha.syntax;

import java.lang.reflect.Type;

import konoha.script.SyntaxTree;

public class RegExp extends SyntaxExtension {

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
