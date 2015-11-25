package konoha.script;

import konoha.Array;
import konoha.ArrayInt;
import konoha.asm.ScriptCompiler;
import nez.util.VisitorMap;

public abstract class Evaluator extends VisitorMap<TreeEvaluator> implements CommonSymbols {
	ScriptContext context;
	TypeSystem typeSystem;
	public ScriptCompiler compiler;

	public Evaluator(ScriptContext sc, TypeSystem ts) {
		super();
		this.context = sc;
		this.typeSystem = ts;
		this.compiler = new ScriptCompiler(this.typeSystem);
	}

	public static EmptyResult empty = new EmptyResult();

	public Object visit(SyntaxTree node) {
		return this.find(node.getTag().toString()).acceptEval(node);
	}

	public Object nullEval(SyntaxTree node) {
		return (node == null) ? null : visit(node);
	}

	// private Object evalFunctorWithArguments(Functor f, TypedTree node) {
	// Object[] args = evalApplyArgument(node);
	// return f.eval(node, args);
	// }

	protected Object evalFunctorWithSingleArgument(SyntaxTree node, Functor f, SyntaxTree arg) {
		Object val = visit(arg);
		return f.eval(node, val);
	}

	Object[] evalApplyArgument(SyntaxTree node) {
		Object[] args = new Object[node.size()];
		for (int i = 0; i < node.size(); i++) {
			args[i] = visit(node.get(i));
		}
		return args;
	}

	public Object evalArray(SyntaxTree node) {
		Object[] args = evalApplyArgument(node);
		Class<?> atype = Lang.getArrayElementClass(node.getType());
		Object a = java.lang.reflect.Array.newInstance(atype, args.length);
		for (int i = 0; i < args.length; i++) {
			java.lang.reflect.Array.set(a, i, args[i]);
		}
		return a;
	}

	Object newArray(Class<?> atype, Object a) {
		if (atype == int.class) {
			return new ArrayInt((int[]) a);
		}
		return new Array<Object>((Object[]) a);
	}
}