package konoha.syntax;

import java.lang.reflect.Type;

import konoha.asm.TreeAsm;
import konoha.hack.Hacker;
import konoha.script.CommonSymbols;
import konoha.script.ScriptContext;
import konoha.script.TreeChecker;
import konoha.script.TreeDesugar;
import konoha.script.TreeEvaluator;
import konoha.script.TypeChecker;
import konoha.script.TypeSystem;
import konoha.script.SyntaxTree;

public abstract class SyntaxExtension extends Hacker implements TreeChecker, TreeDesugar, TreeEvaluator, TreeAsm, CommonSymbols {

	protected ScriptContext context;
	protected TypeSystem typeSystem;
	protected TypeChecker checker;

	@Override
	public void perform(ScriptContext context, TypeSystem typeSystem) {
		this.context = context;
		this.typeSystem = typeSystem;
		this.checker = context.getTypeChecker();
	}

	public abstract String getName();

	@Override
	public abstract Type acceptType(SyntaxTree node);

	@Override
	public SyntaxTree acceptDesugar(SyntaxTree node) {
		return node;
	}

	@Override
	public Object acceptEval(SyntaxTree node) {
		// Default: should be desugared to Konoha CommonTags
		return null;
	}

	@Override
	public void acceptAsm(SyntaxTree node) {
		// Default: should be desugared to Konoha CommonTags
	}

}
