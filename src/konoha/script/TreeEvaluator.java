package konoha.script;

public interface TreeEvaluator {
	public Object acceptEval(SyntaxTree node);

	public boolean isConst(SyntaxTree node);
}
