package konoha.script;

import konoha.Array;
import konoha.ArrayInt;
import konoha.asm.ScriptCompiler;
import nez.ast.TreeVisitor2;

public class Interpreter extends TreeVisitor2<SyntaxTreeInterpreter> implements CommonSymbols {
	ScriptContext context;
	TypeSystem typeSystem;
	private ScriptCompiler compiler;

	public Interpreter(ScriptContext sc, TypeSystem ts) {
		super();
		init(new Undefined());
		this.context = sc;
		this.typeSystem = ts;
		this.compiler = new ScriptCompiler(this.typeSystem);
	}

	static EmptyResult empty = new EmptyResult();

	public Object visit(TypedTree node) {
		return find(node).accept(node);
	}

	public Object nullEval(TypedTree node) {
		return (node == null) ? null : visit(node);
	}

	public class Undefined implements SyntaxTreeInterpreter {
		@Override
		public Object accept(TypedTree node) {
			context.log("[TODO]: Interperter " + node);
			return null;
		}
	}

	public class _Functor extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			Object[] args = evalApplyArgument(node);
			return node.getFunctor().eval(node, args);
		}
	}

	// private Object evalFunctorWithArguments(Functor f, TypedTree node) {
	// Object[] args = evalApplyArgument(node);
	// return f.eval(node, args);
	// }

	private Object evalFunctorWithSingleArgument(TypedTree node, Functor f, TypedTree arg) {
		Object val = visit(arg);
		return f.eval(node, val);
	}

	public class Const extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			return node.getValue();
		}
	}

	public class Source extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			boolean foundError = false;
			Object result = empty;
			for (TypedTree sub : node) {
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
		public Object accept(TypedTree node) {
			Functor f = node.getFunctor();
			compiler.compileFuncDecl(node, f);
			return empty;
		}
	}

	public class VarDecl extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			evalFunctorWithSingleArgument(node, node.getFunctor(), node.get(_expr));
			return empty;
		}
	}

	public class MultiVarDecl extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			for (TypedTree sub : node.get(_list)) {
				evalFunctorWithSingleArgument(sub, sub.getFunctor(), sub.get(_expr));
			}
			return empty;
		}
	}

	public class ClassDecl extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			compiler.compileClassDecl(node);
			return empty;
		}
	}

	/* boolean */

	public class Block extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			Object retVal = null;
			for (TypedTree child : node) {
				retVal = visit(child);
			}
			return retVal;
		}
	}

	public class If extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			boolean cond = (Boolean) visit(node.get(_cond));
			if (cond) {
				return visit(node.get(_then));
			} else {
				TypedTree elseNode = node.get(_else, null);
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
		public Object accept(TypedTree node) {
			if (node.getType() == void.class) {
				visit(node.get(0));
				return empty;
			}
			return visit(node.get(0));
		}
	}

	public class Empty extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			return empty;
		}
	}

	/* Expression */

	public class Cast extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			Object v = visit(node.get(_expr));
			Class<?> c = node.getClassType();
			return c.cast(v);
		}
	}

	public class Conditional extends Undefined {
		@Override
		public Object accept(TypedTree node) {
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
		public Object accept(TypedTree node) {
			Object v = visit(node.get(_left));
			if (v == null) {
				return false;
			}
			return ((Class<?>) node.getValue()).isAssignableFrom(v.getClass());
		}
	}

	public class Inc extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			Object v = visit(node.get(_expr));
			visit(node.get(_body));
			return v;
		}
	}

	public class Dec extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			Object v = visit(node.get(_expr));
			visit(node.get(_body));
			return v;
		}
	}

	private Object[] evalApplyArgument(TypedTree node) {
		Object[] args = new Object[node.size()];
		for (int i = 0; i < node.size(); i++) {
			args[i] = visit(node.get(i));
		}
		return args;
	}

	public class And extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			Boolean b = (Boolean) visit(node.get(_left));
			if (b) {
				return visit(node.get(_right));
			}
			return false;
		}
	}

	public class Or extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			Boolean b = (Boolean) visit(node.get(_left));
			if (!b) {
				return visit(node.get(_right));
			}
			return true;
		}
	}

	public class Null extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			return null;
		}
	}

	public class NullCheck extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			if (node.getValue() != null) {
				return node.getValue();
			}
			return visit(node.get(_expr)) == null;
		}
	}

	public class NonNullCheck extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			if (node.getValue() != null) {
				return node.getValue();
			}
			return visit(node.get(_expr)) != null;
		}
	}

	public class _Array extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			Object a = evalArray(node);
			if (Lang.isNativeArray(node.getType())) {
				return a;
			}
			Class<?> atype = Lang.getArrayElementClass(node.getType());
			if (atype == int.class) {
				return new ArrayInt((int[]) a);
			}
			return new Array<Object>((Object[]) a);
		}
	}

	public Object evalArray(TypedTree node) {
		Object[] args = evalApplyArgument(node);
		Class<?> atype = Lang.getArrayElementClass(node.getType());
		Object a = java.lang.reflect.Array.newInstance(atype, args.length);
		for (int i = 0; i < args.length; i++) {
			java.lang.reflect.Array.set(a, i, args[i]);
		}
		return a;
	}

}