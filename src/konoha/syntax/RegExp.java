package konoha.syntax;

import java.lang.reflect.Type;

import konoha.script.TypedTree;

public class RegExp extends SyntaxExtension {

	@Override
	public String getName() {
		return "Integer";
	}

	@Override
	public Type acceptType(TypedTree node) {
		// TODO;;
		return null;
	}

}
