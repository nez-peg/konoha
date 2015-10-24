package konoha.script;

import java.lang.reflect.Type;

public interface TreeChecker {
	public Type acceptType(SyntaxTree node);
}
