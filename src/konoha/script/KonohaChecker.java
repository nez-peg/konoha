package konoha.script;

import java.lang.reflect.Type;
import java.util.ArrayList;

import konoha.Function;
import konoha.api.BooleanOp;
import konoha.api.DoubleOp;
import konoha.api.IntOp;
import konoha.api.LongOp;
import konoha.api.ObjectOp;
import konoha.api.StringOp;
import konoha.dynamic.ArithmeticSite;
import konoha.dynamic.ComparatorSite;
import konoha.dynamic.DynamicSite;
import konoha.dynamic.UnarySite;
import konoha.message.Message;
import nez.ast.Symbol;
import nez.util.StringUtils;

public class KonohaChecker extends TypeChecker implements CommonSymbols {

	public KonohaChecker(ScriptContext context, TypeSystem typeSystem) {
		super(context, typeSystem);
		this.init(this.getClass(), new Undefined());
	}

	@Override
	public void init() {

		typeSystem.setType("void", void.class);
		typeSystem.setType("boolean", boolean.class);
		typeSystem.setType("byte", byte.class);
		typeSystem.setType("char", char.class);
		typeSystem.setType("short", int.class);
		typeSystem.setType("int", int.class);
		typeSystem.setType("long", long.class);
		typeSystem.setType("float", double.class);
		typeSystem.setType("double", double.class);
		typeSystem.setType("String", String.class);
		typeSystem.setType("Array", konoha.Array.class);
		typeSystem.setType("Dict", konoha.Dict.class);
		typeSystem.setType("Func", Function.class);
		typeSystem.setType("RuntimeException", RuntimeException.class);
		typeSystem.setType("NullPointerException", NullPointerException.class);
		typeSystem.setType("ArithmeticException", ArithmeticException.class);

		typeSystem.addUnifyNumber(double.class, float.class);
		typeSystem.addUnifyNumber(double.class, long.class);
		typeSystem.addUnifyNumber(double.class, int.class);
		typeSystem.addUnifyNumber(double.class, short.class);
		typeSystem.addUnifyNumber(double.class, byte.class);
		typeSystem.addUnifyNumber(float.class, long.class);
		typeSystem.addUnifyNumber(float.class, int.class);
		typeSystem.addUnifyNumber(float.class, short.class);
		typeSystem.addUnifyNumber(float.class, byte.class);
		typeSystem.addUnifyNumber(long.class, int.class);
		typeSystem.addUnifyNumber(long.class, short.class);
		typeSystem.addUnifyNumber(long.class, byte.class);
		typeSystem.addUnifyNumber(int.class, short.class);
		typeSystem.addUnifyNumber(int.class, byte.class);
		typeSystem.addUnifyNumber(short.class, byte.class);

		/* Operator */

		typeSystem.loadStaticFunctionClass(ObjectOp.class, false);
		typeSystem.loadStaticFunctionClass(BooleanOp.class, false);
		typeSystem.loadStaticFunctionClass(IntOp.class, false);
		typeSystem.loadStaticFunctionClass(LongOp.class, false);
		typeSystem.loadStaticFunctionClass(DoubleOp.class, false);
		typeSystem.loadStaticFunctionClass(StringOp.class, false);
		typeSystem.loadStaticFunctionClass(konoha.libc.class, false);

		/* Additional syntax */
		typeSystem.loadSyntaxClass(konoha.syntax.SelfAssign.class);

		add(_Add, new ArithmeticSite(typeSystem, "add"));
		add(_Sub, new ArithmeticSite(typeSystem, "subtract"));
		add(_Mul, new ArithmeticSite(typeSystem, "multiply"));
		add(_Div, new ArithmeticSite(typeSystem, "divide"));
		add(_Mod, new ArithmeticSite(typeSystem, "mod"));

		/* null check */
		// add(_Equals, new ComparatorSite(typeSystem, "eq"));
		// /add(_NotEquals, new ComparatorSite(typeSystem, "ne"));
		add(_LessThan, new ComparatorSite(typeSystem, "lt"));
		add(_LessThanEquals, new ComparatorSite(typeSystem, "lte"));
		add(_GreaterThan, new ComparatorSite(typeSystem, "gt"));
		add(_GreaterThanEquals, new ComparatorSite(typeSystem, "gte"));

		add(_LeftShift, new ArithmeticSite(typeSystem, "shiftLeft"));
		add(_RightShift, new ArithmeticSite(typeSystem, "shiftRight"));
		add(_BitwiseAnd, new ArithmeticSite(typeSystem, "and"));
		add(_BitwiseOr, new ArithmeticSite(typeSystem, "or"));
		add(_BitwiseXor, new ArithmeticSite(typeSystem, "xor"));

		add(_Minus, new UnarySite(typeSystem, "negate"));
		add(_Compl, new UnarySite(typeSystem, "not"));

	}

	public class Undefined implements TreeChecker {
		@Override
		public Type acceptType(SyntaxTree node) {
			node.formatSourceMessage("error", "unsupproted type rule " + node);
			Debug.TODO("TypeChecker for %s", node);
			return void.class;
		}
	}

	public class Error extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			context.log(node.getText(_msg, ""));
			return void.class;
		}
	}

	/* TopLevel */

	public class Source extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			Type t = null;
			for (int i = 0; i < node.size(); i++) {
				SyntaxTree sub = node.get(i);
				try {
					t = visit(sub);
				} catch (TypeCheckerException e) {
					sub = e.errorTree;
					node.set(i, sub);
					t = sub.getType();
				}
			}
			return t;
		}
	}

	public class Import extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeImport(node);
		}
	}

	// public class ClassDecl extends Undefined {
	// @Override
	// public Type acceptType(SyntaxTree node) {
	// return typeClassDecl(node);
	// }
	// }
	//
	// public class Constructor extends Undefined {
	// @Override
	// public Type acceptType(SyntaxTree node) {
	// return typeConstructor(node);
	// }
	// }
	//
	// public class FieldDecl extends Undefined {
	// @Override
	// public Type acceptType(SyntaxTree node) {
	// return typeFieldDecl(node);
	// }
	// }
	//
	// public class MethodDecl extends Undefined {
	// @Override
	// public Type acceptType(SyntaxTree node) {
	// return typeMethodDecl(node);
	// }
	// }

	public class FuncDecl extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeFuncDecl(node);
		}
	}

	public class Lambda extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeLambda(node);
		}
	}

	/* Statement */
	public class Block extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			if (inFunction()) {
				function.beginLocalVarScope();
			}
			typeStatementList(node);
			if (inFunction()) {
				function.endLocalVarScope();
			}
			return void.class;
		}
	}

	public class BlockExpression extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			if (inFunction()) {
				function.beginLocalVarScope();
			}
			typeStatementList(node);
			if (inFunction()) {
				function.endLocalVarScope();
			}
			return node.get(node.size() - 1).getType();
		}
	}

	public class StatementList extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeStatementList(node);
		}
	}

	public class Assert extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			checkCondition(node, _cond);
			if (node.has(_msg)) {
				enforceType(String.class, node, _msg);
			} else {
				String msg = node.get(_cond).formatSourceMessage("assert", "failed");
				node.sub(_cond, node.get(_cond), _msg, node.newConst(String.class, msg));
			}
			return setFunctor(node, KonohaFunctor.getAssertFunctor());
		}
	}

	public class If extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			checkCondition(node, _cond);
			visit(node.get(_then));
			if (node.has(_else)) {
				visit(node.get(_else));
			}
			return void.class;
		}
	}

	public class Conditional extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			checkCondition(node, _cond);
			Type then_t = visit(node.get(_then));
			Type else_t = visit(node.get(_else));
			if (then_t != else_t) {
				enforceType(then_t, node, _else);
			}
			return then_t;
		}
	}

	public class While extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			checkInFunction(node);
			checkCondition(node, _cond);
			visit(node.get(_body));
			return void.class;
		}
	}

	public class DoWhile extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			checkInFunction(node);
			checkCondition(node, _cond);
			visit(node.get(_body));
			return void.class;
		}
	}

	public class Continue extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			checkInFunction(node);
			return void.class;
		}
	}

	public class Break extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			checkInFunction(node);
			return void.class;
		}
	}

	public class For extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			checkInFunction(node);
			function.beginLocalVarScope();
			if (node.has(_init)) {
				visit(node.get(_init));
			}
			if (node.has(_cond)) {
				checkCondition(node, _cond);
			}
			if (node.has(_iter)) {
				visit(node.get(_iter));
			}
			visit(node.get(_body));
			function.endLocalVarScope();
			return void.class;
		}
	}

	// public class ForEach extends Undefined {
	// @Override
	// public Type accept(TypedTree node) {
	// return typeForEach(node);
	// }
	// }
	//
	// public Type typeForEach(TypedTree node) {
	// Type req_t = null;
	// if (node.has(_type)) {
	// req_t = this.typeSystem.resolveType(node.get(_type), null);
	// }
	// String name = node.getText(_name, "");
	// req_t = typeIterator(req_t, node.get(_iter));
	// if (inFunction()) {
	// this.function.beginLocalVarScope();
	// }
	// this.function.setVarType(name, req_t);
	// visit(node.get(_body));
	// if (inFunction()) {
	// this.function.endLocalVarScope();
	// }
	// return void.class;
	// }

	public class Switch extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			Type condType = visit(node.get(_cond));
			typed(node.get(_cond), condType);
			for (SyntaxTree sub : node.get(_body)) {
				if (sub.has(_cond)) {
					Type caseCondType = visit(sub.get(_cond));
					typed(sub.get(_cond), caseCondType);
				}
				visit(sub.get(_body));
				typed(sub, void.class);
			}
			return void.class;
		}
	}

	public class Try extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			checkInFunction(node);
			// try block
			visit(node.get(_try));

			// catch block
			for (SyntaxTree sub : node.get(_catch)) {
				function.beginLocalVarScope();
				String name = sub.getText(_name, null);
				Type paramType = resolveType(sub.get(_type, null), Exception.class);
				function.setVarType(name, paramType);
				typed(sub.get(_name), paramType);
				Type type = visit(sub.get(_body));
				typed(sub, type);
				function.endLocalVarScope();
			}
			typed(node.get(_catch), void.class);

			// finally block
			if (node.has(_finally)) {
				visit(node.get(_finally));
				// typed(node.get(_finally), type);
			}
			return void.class;
		}
	}

	// private Type typeIterator(Type req_t, TypedTree node) {
	// Type iter_t = visit(node.get(_iter));
	// Method m = typeSystem.resolveObjectMethod(req_t, this.bufferMatcher,
	// "iterator", EmptyArgument, null, null);
	// if (m != null) {
	// TypedTree iter = node.newInstance(_MethodApply, 0, null);
	// iter.make(_recv, node.get(_iter), _param, node.newInstance(_List, 0,
	// null));
	// iter_t = iter.setMethod(Hint.MethodApply, m, this.bufferMatcher);
	// // TODO
	// // if(req_t != null) {
	// // }
	// // node.set(index, node)
	// }
	// throw error(node.get(_iter), "unsupported iterator for %s",
	// name(iter_t));
	// }

	public class Empty extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return void.class;
		}
	}

	public class Return extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeReturn(node);
		}
	}

	public class VarDecl extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeVarDecl(node);
		}
	}

	public class MultiVarDecl extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			Type type = resolveType(node.get(_type), null);
			for (SyntaxTree sub : node.get(_list)) {
				typeVarDecl(type, sub);
			}
			return void.class;
		}
	}

	/* Expression */

	public class Expression extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return visit(node.get(0));
		}
	}

	public class Name extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			Type t = tryCheckNameType(node, true);
			if (t == null) {
				String name = node.toText();
				throw error(node, Message.UndefinedName_, name);
			}
			return t;
		}
	}

	public class Assign extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeAssign(node);
		}
	}

	public class And extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			enforceType(boolean.class, node, _left);
			enforceType(boolean.class, node, _right);
			return boolean.class;
		}
	}

	public class Or extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			enforceType(boolean.class, node, _left);
			enforceType(boolean.class, node, _right);
			return boolean.class;
		}
	}

	public class Not extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			enforceType(boolean.class, node, _expr);
			return boolean.class;
		}
	}

	public class Equals extends Undefined {
		private final DynamicSite site = new ComparatorSite(typeSystem, "eq");

		@Override
		public Type acceptType(SyntaxTree node) {
			if (node.get(_right).is(_Null)) {
				return typeNullCheck(_NullCheck, node, _left);
			}
			if (node.get(_left).is(_Null)) {
				return typeNullCheck(_NullCheck, node, _right);
			}
			return typeBinary(node, site);
		}
	}

	public class NotEquals extends Undefined {
		private final DynamicSite site = new ComparatorSite(typeSystem, "ne");

		@Override
		public Type acceptType(SyntaxTree node) {
			if (node.get(_right).is(_Null)) {
				return typeNullCheck(_NonNullCheck, node, _left);
			}
			if (node.get(_left).is(_Null)) {
				return typeNullCheck(_NonNullCheck, node, _right);
			}
			return typeBinary(node, site);
		}
	}

	public class Apply extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeApply(node);
		}
	}

	public class Cast extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeCast(node);
		}
	}

	public class TypeOf extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			try {
				Type t = visit(node.get(_expr));
				node.setConst(String.class, name(t));
			} catch (TypeCheckerException e) {
				context.log(e.getMessage());
				node.setConst(String.class, null);
			}
			return String.class;
		}
	}

	public class Instanceof extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			Class<?> c = Lang.toClassType(visit(node.get(_left)));
			Class<?> t = resolveClass(node.get(_right), null);
			if (!t.isAssignableFrom(c)) {
				reportWarning(node, "incompatible instanceof operation: %s", name(t));
				node.setConst(boolean.class, false);
			}
			node.setValue(t);
			return boolean.class;
		}
	}

	public class Indexer extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeIndexer(node);
		}
	}

	/* this must be moved to Syntax Extension */
	private SyntaxTree desugarInc(SyntaxTree expr, Symbol optag) {
		SyntaxTree op = expr.newInstance(optag, 0, null);
		op.sub(_left, expr.dup(), _right, expr.newConst(int.class, 1));
		return op;
	}

	private SyntaxTree desugarAssign(SyntaxTree node, SyntaxTree expr, Symbol optag) {
		SyntaxTree op = expr.newInstance(optag, 0, null);
		op.sub(_left, expr.dup(), _right, expr.newConst(int.class, 1));
		node.sub(_left, expr, _right, desugarInc(expr, _Add));
		node.setTag(_Assign);
		return node;
	}

	public class PreInc extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			SyntaxTree expr = node.get(_expr);
			return visit(desugarAssign(node, expr, _Add));
		}
	}

	public class PreDec extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			SyntaxTree expr = node.get(_expr);
			return visit(desugarAssign(node, expr, _Sub));
		}
	}

	public class Inc extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			SyntaxTree expr = node.get(_recv);
			SyntaxTree assign = desugarAssign(node.newInstance(_Assign, 2, null), expr.dup(), _Add);
			node.sub(_expr, expr, _body, assign);
			Type t = visit(expr);
			visit(assign);
			return t;
		}
	}

	public class Dec extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			SyntaxTree expr = node.get(_recv);
			SyntaxTree assign = desugarAssign(node.newInstance(_Assign, 2, null), expr.dup(), _Sub);
			node.sub(_expr, expr, _body, assign);
			Type t = visit(expr);
			visit(assign);
			return t;
		}
	}

	/* Object */

	public class New extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			Type newType = resolveType(node.get(_type), null);
			Type[] a = typeArguments(node.get(_param));
			Functor f = typeSystem.getConstructor(methodMatcher, newType, a);
			if (f != null) {
				return found(node, f, methodMatcher, node.get(_param));
			}
			Functor[] unmatched = typeSystem.getConstructors(newType);
			return unfound(node, unmatched, Message.Constructor_, name(newType));
		}
	}

	public class _Field extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeField(node);
		}
	}

	public class This extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return Object.class;
		}
	}

	public class MethodApply extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeMethodApply(node);
		}
	}

	/* Literal */

	public class Null extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return Object.class;
		}
	}

	public class True extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return node.setConst(boolean.class, true);
		}
	}

	public class False extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return node.setConst(boolean.class, false);
		}
	}

	public class _Integer extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return parseIntegerAs(node, int.class);
		}
	}

	public class _Long extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return parseIntegerAs(node, long.class);
		}
	}

	public class _Float extends _Double {
	}

	public class _Double extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			try {
				String n = node.toText().replace("_", "");
				if (n.endsWith("D") || n.endsWith("d") || n.endsWith("F") || n.endsWith("f")) {
					n = n.substring(0, n.length() - 1);
				}
				return node.setConst(double.class, Double.parseDouble(n));
			} catch (NumberFormatException e) {
				reportWarning(node, Message.InvalidNumberFormat);
			}
			return node.setConst(double.class, 0.0);
		}
	}

	public class Text extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return node.setConst(String.class, node.toText());
		}
	}

	public class _String extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			String t = node.toText();
			return node.setConst(String.class, StringUtils.unquoteString(t));
		}
	}

	public class Character extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			String t = StringUtils.unquoteString(node.toText());
			if (t.length() == 1) {
				return node.setConst(char.class, t.charAt(0));
			}
			return node.setConst(String.class, t);
		}
	}

	/* Object Literal */

	public class NewArray extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			// FIXME only supported for single dimensinal array
			// #NewArray[$size: #Integer]
			Type t = resolveType(node.get(_type), null);

			ArrayList<SyntaxTree> sizeList = new ArrayList<>();
			SyntaxTree base = node.get(_type);
			while (base != null && base.has(_size)) {
				sizeList.add(base.get(_size));
				base = base.get(_base);
			}

			node.sub();
			int i = 0;
			for (SyntaxTree size : sizeList) {
				node.add(_size, size);
				enforceType(int.class, node, i);
				i++;
			}
			if (i == 2) {
				node.setTag(_NewArray2);
			} else if (i > 2) {
				throw error(node, "Unsupported " + i + " dimension array", node.getType());
			}
			// #NewArray[$size: #Integer]
			return t;
		}
	}

	public class Array extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			Type elementType = typeArrayElements(node, 1);
			Type arrayType = typeSystem.newArrayType(elementType);
			return arrayType;
		}
	}

	public class Set extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			Type elementType = typeArrayElements(node, 1);
			Type arrayType = typeSystem.newArrayType(elementType);
			node.setTag(_Array);
			return arrayType;
		}
	}

	public class Dict extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			Type elementType = typeArrayElements(node, 1);
			Type arrayType = typeSystem.newArrayType(elementType);
			return arrayType;
		}
	}

	public class Interpolation extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			for (int i = 0; i < node.size(); i++) {
				SyntaxTree sub = node.get(i);
				visit(sub);
				if (sub.getType() != Object.class) {
					enforceType(Object.class, node, i);
				}
			}
			SyntaxTree t = node.dup();
			t.setTag(_Array);
			t.setType(Object[].class);
			node.sub(_expr, t);
			return setFunctor(node, KonohaFunctor.getInterpolationFunctor());
		}
	}

}
