package konoha.syntax;

import java.lang.reflect.Type;

import konoha.asm.TreeAsm;
import konoha.hack.Hacker;
import konoha.script.ScriptContext;
import konoha.script.TreeChecker;
import konoha.script.TreeDesugar;
import konoha.script.TreeEvaluator;
import konoha.script.TypeChecker;
import konoha.script.TypeSystem;
import konoha.script.TypedTree;

public class SyntaxExtension extends Hacker implements TreeChecker, TreeDesugar, TreeEvaluator, TreeAsm {

	protected ScriptContext context;
	protected TypeSystem typeSystem;
	protected TypeChecker checker;

	public SyntaxExtension(ScriptContext context) {
		this.perform(context, context.getTypeSystem());
	}

	@Override
	public void perform(ScriptContext context, TypeSystem typeSystem) {
		this.context = context;
		this.typeSystem = typeSystem;
		this.checker = context.getTypeChecker();
	}

	@Override
	public Type acceptType(TypedTree node) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TypedTree acceptDesugar(TypedTree node) {
		return node;
	}

	@Override
	public Object acceptEval(TypedTree node) {
		// Default: should be desugared to Konoha CommonTags
		return null;
	}

	@Override
	public void acceptAsm(TypedTree node) {
		// Default: should be desugared to Konoha CommonTags
	}

}
