package konoha.script;

import konoha.Array;
import konoha.ArrayInt;
import konoha.Function;
import konoha.asm.ScriptCompiler;
import nez.ast.TreeVisitor2;

public class Evaluator extends TreeVisitor2<TreeEvaluator> implements CommonSymbols {
	ScriptContext context;
	TypeSystem typeSystem;
	private ScriptCompiler compiler;

	public Evaluator(ScriptContext sc, TypeSystem ts) {
		super();
		init(new Undefined());
		this.context = sc;
		this.typeSystem = ts;
		this.compiler = new ScriptCompiler(this.typeSystem);
	}

	public static EmptyResult empty = new EmptyResult();

	public Object visit(SyntaxTree node) {
		return find(node).acceptEval(node);
	}

	public Object nullEval(SyntaxTree node) {
		return (node == null) ? null : visit(node);
	}

	public class Undefined implements TreeEvaluator {
		@Override
		public Object acceptEval(SyntaxTree node) {
			context.log("[TODO]: Interperter " + node);
			return null;
		}

		@Override
		public boolean isConst(SyntaxTree node) {
			return false;
		}
	}

	public class _Functor extends Undefined {
		@Override
		public Object acceptEval(SyntaxTree node) {
			Object[] args = evalApplyArgument(node);
			return node.getFunctor().eval(node, args);
		}
	}

	// private Object evalFunctorWithArguments(Functor f, TypedTree node) {
	// Object[] args = evalApplyArgument(node);
	// return f.eval(node, args);
	// }

	private Object evalFunctorWithSingleArgument(SyntaxTree node, Functor f, SyntaxTree arg) {
		Object val = visit(arg);
		return f.eval(node, val);
	}

	public class Const extends Undefined {
		@Override
		public Object acceptEval(SyntaxTree node) {
			return node.getValue();
		}
	}

	public class Source extends Undefined {
		@Override
		public Object acceptEval(SyntaxTree node) {
			boolean foundError = false;
			Object result = empty;
			for (SyntaxTree sub : node) {
				if (sub.is(_Error)) {
					context.log(sub.getText(_msg, ""));
					foundError = true;
				}
				if (!foundError) {
					result = visit(sub);
				}
			}
			return foundError ? empty : result;
		}
	}

	/* TopLevel */

	public class FuncDecl extends Undefined {
		@Override
		public Object acceptEval(SyntaxTree node) {
			Functor f = node.getFunctor();
			compiler.compileFuncDecl(node, f);
			return empty;
		}
	}

	public class Lambda extends Undefined {
		@Override
		public Object acceptEval(SyntaxTree node) {
			Function lambda = compiler.compileLambda(node);
			return lambda;
		}
	}

	public class VarDecl extends Undefined {
		@Override
		public Object acceptEval(SyntaxTree node) {
			evalFunctorWithSingleArgument(node, node.getFunctor(), node.get(_expr));
			return empty;
		}
	}

	public class MultiVarDecl extends Undefined {
		@Override
		public Object acceptEval(SyntaxTree node) {
			for (SyntaxTree sub : node.get(_list)) {
				evalFunctorWithSingleArgument(sub, sub.getFunctor(), sub.get(_expr));
			}
			return empty;
		}
	}

	public class ClassDecl extends Undefined {
		@Override
		public Object acceptEval(SyntaxTree node) {
			compiler.compileClassDecl(node);
			return empty;
		}
	}

	/* boolean */

	public class Block extends Undefined {
		@Override
		public Object acceptEval(SyntaxTree node) {
			for (SyntaxTree child : node) {
				visit(child);
			}
			return empty;
		}
	}

	public class BlockExpression extends Undefined {
		@Override
		public Object acceptEval(SyntaxTree node) {
			Object retVal = null;
			for (SyntaxTree child : node) {
				retVal = visit(child);
			}
			return retVal;
		}
	}

	public class If extends Undefined {
		@Override
		public Object acceptEval(SyntaxTree node) {
			boolean cond = (Boolean) visit(node.get(_cond));
			if (cond) {
				return visit(node.get(_then));
			} else {
				SyntaxTree elseNode = node.get(_else, null);
				if (elseNode != null) {
					return visit(elseNode);
				}
			}
			return empty;
		}
	}

	/* Expression Statement */
	public class Expression extends Undefined {
		@Override
		public Object acceptEval(SyntaxTree node) {
			if (node.getType() == void.class) {
				visit(node.get(0));
				return empty;
			}
			return visit(node.get(0));
		}
	}

	public class Empty extends Undefined {
		@Override
		public Object acceptEval(SyntaxTree node) {
			return empty;
		}
	}

	/* Expression */

	public class Cast extends Undefined {
		@Override
		public Object acceptEval(SyntaxTree node) {
			Object v = visit(node.get(_expr));
			Class<?> c = node.getClassType();
			return c.cast(v);
		}
	}

	public class Conditional extends Undefined {
		@Override
		public Object acceptEval(SyntaxTree node) {
			Boolean v = (Boolean) visit(node.get(_cond));
			if (v) {
				return visit(node.get(_then));
			} else {
				return visit(node.get(_else));
			}

		}
	}

	public class InstanceOf extends Undefined {
		@Override
		public Object acceptEval(SyntaxTree node) {
			Object v = visit(node.get(_left));
			if (v == null) {
				return false;
			}
			return ((Class<?>) node.getValue()).isAssignableFrom(v.getClass());
		}
	}

	public class Inc extends Undefined {
		@Override
		public Object acceptEval(SyntaxTree node) {
			Object v = visit(node.get(_expr));
			visit(node.get(_body));
			return v;
		}
	}

	public class Dec extends Undefined {
		@Override
		public Object acceptEval(SyntaxTree node) {
			Object v = visit(node.get(_expr));
			visit(node.get(_body));
			return v;
		}
	}

	private Object[] evalApplyArgument(SyntaxTree node) {
		Object[] args = new Object[node.size()];
		for (int i = 0; i < node.size(); i++) {
			args[i] = visit(node.get(i));
		}
		return args;
	}

	public class And extends Undefined {
		@Override
		public Object acceptEval(SyntaxTree node) {
			Boolean b = (Boolean) visit(node.get(_left));
			if (b) {
				return visit(node.get(_right));
			}
			return false;
		}
	}

	public class Or extends Undefined {
		@Override
		public Object acceptEval(SyntaxTree node) {
			Boolean b = (Boolean) visit(node.get(_left));
			if (!b) {
				return visit(node.get(_right));
			}
			return true;
		}
	}

	public class Not extends Undefined {
		@Override
		public Object acceptEval(SyntaxTree node) {
			return !((Boolean) visit(node.get(_expr)));
		}
	}

	public class Null extends Undefined {
		@Override
		public Object acceptEval(SyntaxTree node) {
			return null;
		}
	}

	public class NullCheck extends Undefined {
		@Override
		public Object acceptEval(SyntaxTree node) {
			if (node.getValue() != null) {
				return node.getValue();
			}
			return visit(node.get(_expr)) == null;
		}
	}

	public class NonNullCheck extends Undefined {
		@Override
		public Object acceptEval(SyntaxTree node) {
			if (node.getValue() != null) {
				return node.getValue();
			}
			return visit(node.get(_expr)) != null;
		}
	}

	public class _Array extends Undefined {
		@Override
		public Object acceptEval(SyntaxTree node) {
			Object a = evalArray(node);
			if (Lang.isNativeArray(node.getType())) {
				return a;
			}
			Class<?> atype = Lang.getArrayElementClass(node.getType());
			return newArray(atype, a);
		}
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

	public class NewArray extends Undefined {
		@Override
		public Object acceptEval(SyntaxTree node) {
			int size = (Integer) visit(node.get(_size));
			Class<?> atype = Lang.getArrayElementClass(node.getType());
			Object a = java.lang.reflect.Array.newInstance(atype, size);
			return newArray(atype, a);
		}
	}

	public class NewArray2 extends Undefined {
		@Override
		public Object acceptEval(SyntaxTree node) {
			int sizex = (Integer) visit(node.get(0));
			int sizey = (Integer) visit(node.get(1));
			Class<?> type = node.getClassType();
			Class<?> atype = Lang.getArrayElementClass(node.getType());
			Class<?> aatype = Lang.getArrayElementClass(Lang.getArrayElementType(node.getType()));
			Object aa = java.lang.reflect.Array.newInstance(aatype, sizey);
			Object x = newArray(aatype, aa);
			Object a = java.lang.reflect.Array.newInstance(aatype, sizey);
			Object y = newArray(aatype, a);
			Object[] val = { x, y };
			return newArray(atype, val);
		}
	}

	private Object newArray(Class<?> atype, Object a) {
		if (atype == int.class) {
			return new ArrayInt((int[]) a);
		}
		return new Array<Object>((Object[]) a);
	}
}