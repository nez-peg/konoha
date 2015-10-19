package konoha.script;

import java.lang.reflect.Type;

public interface SyntaxTreeTypeChecker {
	public Type acceptType(TypedTree node);
}
