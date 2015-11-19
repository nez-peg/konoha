package konoha.syntax;

import java.lang.reflect.Type;

import konoha.asm.ScriptCompilerAsm;
import konoha.asm.TreeAsm;
import konoha.script.CommonSymbols;
import konoha.script.Evaluator;
import konoha.script.ScriptContext;
import konoha.script.SyntaxTree;
import konoha.script.TreeChecker;
import konoha.script.TreeDesugar;
import konoha.script.TreeEvaluator;
import konoha.script.TypeChecker;
import konoha.script.TypeSystem;

public abstract class SyntaxExtension implements TreeChecker, TreeDesugar, TreeEvaluator, TreeAsm, CommonSymbols {

	protected ScriptContext context;
	protected TypeSystem typeSystem;
	protected TypeChecker checker;
	protected Evaluator eval;
	protected ScriptCompilerAsm asm;

	public SyntaxExtension(ScriptContext context) {
		this.context = context;
		this.typeSystem = context.getTypeSystem();
		this.checker = context.getTypeChecker();
		this.eval = context.getEvaluator();
		this.asm = context.getScriptCompilerAsm();
	}

	public abstract String getName();

	@Override
	public abstract Type acceptType(SyntaxTree node);

	@Override
	public SyntaxTree acceptDesugar(SyntaxTree node) {
		return node;
	}

	@Override
	public boolean isConst(SyntaxTree node) {
		return false;
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
