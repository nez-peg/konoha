package konoha.script;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigInteger;

import konoha.Function;
import konoha.asm.DynamicMember;
import konoha.message.Message;
import nez.ast.Symbol;
import nez.ast.TreeVisitor2;
import nez.util.StringUtils;

public class TypeChecker extends TreeVisitor2<TreeChecker> implements CommonSymbols {
	ScriptContext context;
	TypeSystem typeSystem;

	public TypeChecker(ScriptContext context, TypeSystem typeSystem) {
		this.context = context;
		this.typeSystem = typeSystem;
		init(new Undefined());
	}

	public class Undefined implements TreeChecker {
		@Override
		public Type acceptType(SyntaxTree node) {
			node.formatSourceMessage("error", "unsupproted type rule " + node);
			Debug.TODO("TypeChecker for %s", node);
			return void.class;
		}
	}

	public class Error {
		public Type type(SyntaxTree t) {
			context.log(t.getText(_msg, ""));
			return void.class;
		}
	}

	FunctionBuilder function = null;

	public final FunctionBuilder enterFunction(String name) {
		this.function = new FunctionBuilder(this.function, name);
		return this.function;
	}

	public final void exitFunction() {
		this.function = this.function.pop();
	}

	public final boolean inFunction() {
		return this.function != null;
	}

	public SyntaxTree check(SyntaxTree node) {
		try {
			visit(node);
			return node;
		} catch (TypeCheckerException e) {
			context.found(ScriptContextError.TypeError);
			return e.getErrorTree();
		}
	}

	public SyntaxTree checkAtTopLevel(SyntaxTree node) {
		try {
			visit(node);
			return node;
		} catch (TypeCheckerException e) {
			context.found(ScriptContextError.TypeError);
			context.log(e.getMessage());
			return node;
		}
	}

	public Type visit(SyntaxTree node) {
		Type c = node.getType();
		if (c == null) {
			c = find(node).acceptType(node);
			if (c != null) {
				node.setType(c);
			}
		}
		return c;
	}

	private String name(Type t) {
		return Lang.name(t);
	}

	public void typed(SyntaxTree node, Type c) {
		node.setType(c);
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

	public Type typeImport(SyntaxTree node) {
		String path = join(node.get(_name));
		try {
			typeSystem.importStaticClass(path);
		} catch (ClassNotFoundException e) {
			throw error(node.get(_name), Message.UndefinedPackage_, path);
		}
		node.done();
		return void.class;
	}

	private String join(SyntaxTree node) {
		if (node.size() == 0) {
			return node.toText();
		}
		StringBuilder sb = new StringBuilder();
		join(sb, node);
		return sb.toString();
	}

	private void join(StringBuilder sb, SyntaxTree node) {
		SyntaxTree prefix = node.get(_prefix);
		if (prefix.size() == 2) {
			join(sb, prefix);
		} else {
			sb.append(prefix.toText());
		}
		sb.append(".").append(node.getText(_name, null));
	}

	/* FuncDecl */
	private static Type[] EmptyTypes = new Type[0];

	public class FuncDecl extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeFuncDecl(node);
		}
	}

	public Type typeFuncDecl(SyntaxTree node) {
		String name = node.getText(_name, null);
		SyntaxTree bodyNode = node.get(_body, null);
		Type returnType = resolveType(node.get(_type, null), null);
		Type[] paramTypes = EmptyTypes;
		SyntaxTree params = node.get(_param, null);
		if (node.has(_param)) {
			int c = 0;
			paramTypes = new Type[params.size()];
			for (SyntaxTree p : params) {
				paramTypes[c] = resolveType(p.get(_type, null), Object.class);
				c++;
			}
		}
		if (bodyNode == null) {
			if (returnType != null) {
				typeSystem.newPrototype(node, returnType, name, paramTypes);
			}
			node.done();
			return void.class;
		}
		Functor prototype = typeSystem.getPrototype(returnType, name, paramTypes);
		if (prototype == null && returnType != null) {
			prototype = typeSystem.newPrototype(node, returnType, name, paramTypes);
		}
		node.setFunctor(prototype);
		FunctionBuilder f = this.enterFunction(name);
		if (returnType != null) {
			f.setReturnType(returnType);
			typed(node.get(_type), returnType);
		}
		if (node.has(_param)) {
			int c = 0;
			for (SyntaxTree sub : params) {
				String pname = sub.getText(_name, null);
				f.setVarType(pname, paramTypes[c]);
				typed(sub, paramTypes[c]);
				c++;
			}
		}
		f.setParameterTypes(paramTypes);
		try {
			visit(bodyNode);
		} catch (TypeCheckerException e) {
			node.set(_body, e.errorTree);
		}
		this.exitFunction();
		if (f.getReturnType() == null) {
			f.setReturnType(void.class);
		}
		typed(node.get(_name), f.getReturnType());
		return void.class;
	}

	public class Return extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeReturn(node);
		}
	}

	private void checkInFunction(SyntaxTree node) {
		if (!inFunction()) {
			throw error(node, Message.MustBeInFunction);
		}
	}

	public Type typeReturn(SyntaxTree node) {
		checkInFunction(node);
		Type t = this.function.getReturnType();
		if (t == null) { // type inference
			Type inferred = node.has(_expr) ? visit(node.get(_expr)) : void.class;
			reportInferredType(node, Message.InferredReturn_, name(inferred));
			this.function.setReturnType(inferred);
			return void.class;
		}
		if (t == void.class) {
			if (node.size() > 0) {
				node.sub();
			}
		} else {
			if (!node.has(_expr)) {
				node.sub(_expr, newDefault(node, t));
			}
			this.enforceType(t, node, _expr);
		}
		return void.class;
	}

	private SyntaxTree newDefault(SyntaxTree node, Type type) {
		if (type == int.class) {
			return node.newConst(int.class, 0);
		}
		if (type == double.class || type == float.class) {
			return node.newConst(double.class, 0.0);
		}
		if (type == long.class) {
			return node.newConst(boolean.class, false);
		}
		if (type == long.class) {
			return node.newConst(long.class, 0L);
		}
		return node.newConst(type, null);
	}

	/* Statement */

	public class Empty extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return void.class;
		}
	}

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

	public class StatementList extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeStatementList(node);
		}
	}

	public Type typeStatementList(SyntaxTree node) {
		for (int i = 0; i < node.size(); i++) {
			SyntaxTree sub = node.get(i);
			try {
				visit(sub);
			} catch (TypeCheckerException e) {
				sub = e.errorTree;
				node.set(i, sub);
			}
		}
		return void.class;
	}

	public void checkCondition(SyntaxTree node, Symbol condLabel) {
		if (node.get(condLabel).is(_Assign)) {
			this.reportWarning(node.get(condLabel), Message.AssignInCondition);
			node.setTag(_Equals);
		}
		enforceType(boolean.class, node, condLabel);
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
			return functor(node, KonohaFunctor.getAssertFunctor());
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

	public class VarDecl extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeVarDecl(node);
		}
	}

	public class NewArray extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			// FIXME only supported for single dimensinal array
			// #NewArray[$size: #Integer]
			Type t = resolveType(node.get(_type), null);
			SyntaxTree size = node.get(_type).get(_size);
			node.sub(_size, size);
			enforceType(int.class, node, _size);
			// #NewArray[$size: #Integer]
			return t;
		}
	}

	public Type typeVarDecl(SyntaxTree node) {
		boolean isArrayName = false;
		String name = node.getText(_name, null);
		SyntaxTree arraySize = null;
		if (node.get(_name).is(_ArrayName)) {
			name = node.get(_name).getText(_name, null);
			arraySize = node.get(_name).get(_param, null);
			isArrayName = true;
		}
		Type type = resolveType(node.get(_type, null), null);
		if (type != null) {
			if (isArrayName) {
				type = typeSystem.newArrayType(type);
				if (!node.has(_expr) && arraySize != null) {
					this.reportWarning(node.get(_name), Message.CStyleArray);
					SyntaxTree expr = node.newInstance(_NewArray, _size, arraySize);
					enforceType(int.class, expr, _size);
					expr.setType(type);
					node.add(_expr, expr);
				}
			}
			if (node.has(_expr)) {
				enforceType(type, node, _expr);
			}
		} else { /* type inference from the expression */
			if (!node.has(_expr)) { // untyped
				// this.typeSystem.reportWarning(node.get(_name),
				// "type is ungiven");
				type = Object.class;
			} else {
				type = visit(node.get(_expr));
			}
			this.reportInferredType(node.get(_name), Message.InferredVariable__, name, name(type));
		}
		defineVariable(node, type, name);
		return void.class;
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

	public void typeVarDecl(Type type, SyntaxTree node) {
		String name = node.getText(_name, null);
		if (node.get(_name).is(_ArrayName)) {
			name = node.get(_name).getText(_name, null);
			type = typeSystem.newArrayType(type);
			SyntaxTree arraySize = node.get(_name).get(_param, null);
			if (!node.has(_expr) && arraySize != null) {
				this.reportWarning(node.get(_name), Message.CStyleArray);
				SyntaxTree expr = node.newInstance(_NewArray, _size, arraySize);
				enforceType(int.class, expr, _size);
				expr.setType(type);
				node.add(_expr, expr);
			}
		}
		if (node.has(_expr)) {
			enforceType(type, node, _expr);
		}
		defineVariable(node, type, name);
	}

	private void defineVariable(SyntaxTree node, Type type, String name) {
		SyntaxTree nm = node.get(_name);
		typed(nm, type); // name is typed
		if (!node.has(_expr)) {
			reportWarning(nm, Message.NoInitialValue);
			SyntaxTree expr = newDefault(node, type);
			node.sub(_name, nm, _expr, expr);
		}
		if (this.inFunction()) {
			this.function.setVarType(name, type);
			typed(node, void.class);
			return;
		}
		GlobalVariable gv = typeSystem.getGlobalVariable(name);
		if (gv != null) {
			if (gv.getType() != type) {
				throw error(node.get(_name), Message.AlreadyDefinedName);
			}
		} else {
			gv = typeSystem.newGlobalVariable(type, name);
		}
		node.setFunctor(gv.setter);
		typed(node, void.class);
	}

	/* StatementExpression */

	public class Expression extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return visit(node.get(0));
		}
	}

	/* Expression */

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

	private Type tryCheckNameType(SyntaxTree node, boolean rewrite) {
		String name = node.toText();
		if (this.inFunction()) {
			if (this.function.containsVariable(name)) {
				return this.function.getVarType(name);
			}
		}
		if (this.typeSystem.hasGlobalVariable(name)) {
			GlobalVariable gv = this.typeSystem.getGlobalVariable(name);
			if (rewrite) {
				node.sub();
				return functor(node, gv.getGetter());
			}
			return gv.getType();
		}
		return null;
	}

	public class Assign extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeAssign(node);
		}
	}

	public void checkAssignable(SyntaxTree node) {
		if (node.is(_Name) || node.is(_Field) || node.is(_Indexer)) {
			return;
		}
		throw error(node, Message.LeftHandAssignment);
	}

	public Type typeAssign(SyntaxTree node) {
		SyntaxTree leftnode = node.get(_left);
		checkAssignable(leftnode);
		if (leftnode.is(_Indexer)) {
			return typeSetIndexer(node, node.get(_left), node.get(_right));
		}
		if (leftnode.is(_Field)) {
			return typeSetField(node, node.get(_left));
		}
		assert (leftnode.is(_Name));
		String name = node.getText(_left, "");
		Type rightType = visit(node.get(_right));
		if (this.inFunction()) {
			if (this.function.containsVariable(name)) {
				Type t = this.function.getVarType(name);
				node.get(_left).setType(t);
				enforceType(t, node, _right);
				return t;
			}
		}
		if (!this.typeSystem.hasGlobalVariable(name)) {
			if (!this.inFunction()) {
				if (!typeSystem.shellMode) {
					this.reportWarning(node.get(_left), Message.ImplicitVariable);
				}
				this.typeSystem.newGlobalVariable(Object.class, name);
			} else {
				this.reportWarning(node.get(_left), Message.ImplicitVariable);
				this.reportInferredType(node.get(_left), Message.InferredVariable__, name, name(rightType));
				this.function.setVarType(name, rightType);
				node.setTag(_VarDecl);
				node.sub(_name, node.get(_left), _expr, node.get(_right));
				node.get(_name).setType(rightType);
				return void.class;
			}
		}
		if (this.typeSystem.hasGlobalVariable(name)) {
			GlobalVariable gv = this.typeSystem.getGlobalVariable(name);
			Functor f = gv.getSetter();
			if (f == null) {
				throw error(node.get(_left), Message.ReadOnly);
			}
			Type t = gv.getType();
			enforceType(t, node, _right);
			node.sub(_right, node.get(_right));
			functor(node, f);
			return t;
		}
		throw error(node.get(_left), Message.UndefinedName_, name);
	}

	/* Expression */

	public class Cast extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeCast(node);
		}
	}

	public Type typeCast(SyntaxTree node) {
		Type inner = visit(node.get(_expr));
		Type t = this.resolveType(node.get(_type), null);
		if (t == null) {
			throw error(node.get(_type), Message.UndefinedType_, node.getText(_type, ""));
		}
		Class<?> req = Lang.toClassType(t);
		Class<?> exp = Lang.toClassType(inner);
		if (req.isAssignableFrom(exp)) { // upcast
			node.setTag(_UpCast);
			return t;
		}
		Functor f = typeSystem.getCast(exp, req);
		if (f == null) {
			f = typeSystem.getConv(exp, req);
		}
		if (f != null) {
			node.makeFlattenedList(node.get(_expr));
			return functor(node, f);
		}
		if (exp.isAssignableFrom(req)) { // downcast
			node.setTag(_DownCast);
			return t;
		}
		throw error(node.get(_type), Message.UndefinedCast__, name(inner), name(t));
	}

	public class Apply extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeApply(node);
		}
	}

	public Type typeApply(SyntaxTree node) {
		String name = node.getText(_name, "");
		Type[] a = typeArguments(node.get(_param));
		Type funcType = this.findFuncVariable(node);
		if (funcType != null) {
			return typeFuncApply(node, funcType);
		}
		Functor f = this.typeSystem.getFunction(methodMatcher, name, a);
		if (f != null) {
			return found(node, f, methodMatcher, node.get(_param));
		}
		Functor[] unmatched = this.typeSystem.getFunction(name);
		return unfound(node, unmatched, Message.Function_, name);
	}

	private Type findFuncVariable(SyntaxTree node) {
		String name = node.toText();
		if (this.inFunction()) {
			if (this.function.containsVariable(name)) {
				Type t = this.function.getVarType(name);
				if (this.typeSystem.isFuncType(t)) {
					node.setType(t);
					return t;
				}
			}
		}
		if (this.typeSystem.hasGlobalVariable(name)) {
			GlobalVariable gv = this.typeSystem.getGlobalVariable(name);
			Type t = gv.getType();
			if (this.typeSystem.isFuncType(t)) {
				node.sub();
				return functor(node, gv.getGetter());
			}
		}
		return null;
	}

	private Type typeFuncApply(SyntaxTree node, Type funcType) {
		if (typeSystem.isStaticFuncType(funcType)) {
			Functor f = new Functor(Syntax.FuncObject, funcType);
			methodMatcher.init(null);
			return found(node, f, methodMatcher, node.get(_name), node.get(_param));
		} else {
			SyntaxTree params = node.get(_param);
			for (int i = 0; i < params.size(); i++) {
				enforceType(Object.class, params, i);
			}
			params.setTag(_Array);
			params.setType(Object[].class);
			node.sub(_name, node.get(_name), _param, params);
			return functor(node, KonohaFunctor.getInvokeFunc());
		}
	}

	private static final Type[] emptyTypes = new Type[0];

	private Type typeUnary(Type ret, SyntaxTree node, String name) {
		Type left = visit(node.get(_expr));
		Type common = Lang.toPrimitiveType(left);
		if (left != common) {
			left = this.tryCastBeforeMatching(common, node, _expr);
		}
		Type[] a = { left };
		Functor f = this.typeSystem.getMethod(methodMatcher, left, name, a);
		if (f != null) {
			return found(node, f, methodMatcher);
		}
		if (typeSystem.isDynamic(left)) {
			if (ret == null) {
				ret = left;
			}
			return makeIndy(node, KonohaFunctor.getIndyMethodFunctor(), name, a, node.get(_expr));
		}
		Functor[] fs = this.typeSystem.getMethods(left, name);
		return unfound(node, fs, Message.Unary__, OperatorNames.name(name), name(left));
	}

	public Type typeBinary(Type ret, SyntaxTree node, String name) {
		Type left = visit(node.get(_left));
		Type right = visit(node.get(_right));
		Type[] a = { left, right };
		Functor f = this.typeSystem.getMethod(methodMatcher, left, name, a);
		if (f != null) {
			return found(node, f, methodMatcher);
		}
		if (typeSystem.isDynamic(left)) {
			if (ret == null) {
				ret = left;
			}
			return makeIndy(node, KonohaFunctor.getIndyMethodFunctor(), name, a, node.get(_left), node.get(_right));
		}
		Functor[] fs = this.typeSystem.getMethods(left, name);
		return unfound(node, fs, Message.Binary___, name(left), OperatorNames.name(name), name(right));
	}

	public void unifyBinaryAdd(SyntaxTree node) {
		Type left = visit(node.get(_left));
		Type right = visit(node.get(_right));
		Type common = Lang.unifyAdd(Lang.toPrimitiveType(left), Lang.toPrimitiveType(right));
		if (left != common) {
			this.tryCastBeforeMatching(common, node, _left);
		}
		// if (right != common) {
		// this.tryCastBeforeMatching(common, node, _right);
		// }
	}

	public void unifyBinaryNum(SyntaxTree node) {
		Type left = visit(node.get(_left));
		Type right = visit(node.get(_right));
		Type common = Lang.unifyNum(Lang.toPrimitiveType(left), Lang.toPrimitiveType(right));
		if (left != common) {
			this.tryCastBeforeMatching(common, node, _left);
		}
		// if (right != common) {
		// this.tryCastBeforeMatching(common, node, _right);
		// }
	}

	public class Add extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			unifyBinaryAdd(node);
			return typeBinary(null, node, "add");
		}
	}

	public class Sub extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			unifyBinaryNum(node);
			return typeBinary(null, node, "subtract");
		}
	}

	public class Mul extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			unifyBinaryNum(node);
			return typeBinary(null, node, "multiply");
		}
	}

	public class Div extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			unifyBinaryNum(node);
			return typeBinary(null, node, "divide");
		}
	}

	public class Mod extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			unifyBinaryNum(node);
			return typeBinary(null, node, "mod");
		}
	}

	public class Plus extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return visit(node.get(_expr));
		}
	}

	public class Minus extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeUnary(null, node, "negate");
		}
	}

	public class Equals extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			if (node.get(_right).is(_Null)) {
				return typeNullCheck(_NullCheck, node, _left);
			}
			if (node.get(_left).is(_Null)) {
				return typeNullCheck(_NullCheck, node, _right);
			}
			unifyBinaryNum(node);
			return typeBinary(boolean.class, node, "eq");
		}
	}

	public class NotEquals extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			if (node.get(_right).is(_Null)) {
				return typeNullCheck(_NonNullCheck, node, _left);
			}
			if (node.get(_left).is(_Null)) {
				return typeNullCheck(_NonNullCheck, node, _right);
			}
			unifyBinaryNum(node);
			return typeBinary(boolean.class, node, "ne");
		}
	}

	Type typeNullCheck(Symbol tag, SyntaxTree node, Symbol expr) {
		visit(node.get(expr));
		Class<?> c = node.get(expr).getClassType();
		if (c.isPrimitive()) {
			node.setConst(boolean.class, tag == _NullCheck ? false : true);
		} else {
			node.setTag(tag);
			node.sub(_expr, node.get(expr));
		}
		return boolean.class;
	}

	public class LessThan extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			unifyBinaryNum(node);
			return typeBinary(boolean.class, node, "lt");
		}
	}

	public class LessThanEquals extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeBinary(boolean.class, node, "lte");
		}
	}

	public class GreaterThan extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeBinary(boolean.class, node, "gt");
		}
	}

	public class GreaterThanEquals extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeBinary(boolean.class, node, "gte");
		}
	}

	public class LeftShift extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeBinary(null, node, "shiftLeft");
		}
	}

	public class RightShift extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeBinary(null, node, "shiftRight");
		}
	}

	// public class LogicalRightShift extends Undefined {
	// @Override
	// public Type acceptType(TypedTree node) {
	// return typeBinary(node, "opLogicalRightShift");
	// }
	// }

	public class BitwiseAnd extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeBinary(null, node, "and");
		}
	}

	public class BitwiseOr extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeBinary(null, node, "or");
		}
	}

	public class BitwiseXor extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeBinary(null, node, "xor");
		}
	}

	public class Compl extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeUnary(null, node, "not");
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
			return typeUnary(boolean.class, node, "not");
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

	public Type parseIntegerAs(SyntaxTree node, Class<?> base) {
		String n = node.toText().replace("_", "");
		int radix = 10;
		if (n.endsWith("L") || n.endsWith("l")) {
			n = n.substring(0, n.length() - 1);
			base = long.class;
		}
		if (n.startsWith("0b") || n.startsWith("0B")) {
			n = n.substring(2);
			radix = 2;
		} else if (n.startsWith("0x") || n.startsWith("0X")) {
			n = n.substring(2);
			radix = 16;
		}
		try {
			if (base == int.class) {
				return node.setConst(int.class, Integer.parseInt(n, radix));
			}
			return node.setConst(long.class, Long.parseLong(n, radix));
		} catch (NumberFormatException e) {
		}
		try {
			BigInteger big = new BigInteger(n, radix);
			reportWarning(node, Message.TooBigInteger_, base == int.class ? 32 : 64);
			return node.setConst(long.class, big.longValue());
		} catch (NumberFormatException e) {
			reportWarning(node, Message.InvalidNumberFormat, node.toText());
		}
		return node.setConst(base, base == int.class ? 0 : 0L);
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

	/* object oriented */

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

	private Type typeField(SyntaxTree node) {
		if (isStaticClassName(node)) {
			return typeStaticField(node);
		}
		Type recvType = visit(node.get(_recv));
		String name = node.getText(_name, "");
		Functor f = typeSystem.getGetter(methodMatcher, recvType, name);
		if (f != null) {
			if (isStaticField(f)) {
				node.sub();
				return found(node, f, methodMatcher);
			}
			return found(node, f, methodMatcher, node.get(_recv));
		}
		if (typeSystem.isDynamic(recvType)) {
			return found(node, DynamicMember.newGetter(name), methodMatcher, node.get(_recv));
		}
		throw error(node.get(_name), Message.UndefinedField__, name(recvType), name);
	}

	private Type typeSetField(SyntaxTree node, SyntaxTree field) {
		if (isStaticClassName(field)) {
			return typeSetStaticField(node, field);
		}
		Type recvType = visit(field.get(_recv));
		String name = field.getText(_name, "");
		Functor f = typeSystem.getSetter(methodMatcher, recvType, name);
		if (f != null) {
			if (isReadOnlyField(f)) {
				throw error(field.get(_name), Message.ReadOnly);
			}
			if (isStaticField(f)) {
				enforceType(f.get(0), node, _right);
				return found(node, f, methodMatcher, node.get(_right));
			}
			enforceType(f.get(1), node, _right);
			return found(node, f, methodMatcher, field.get(_recv), node.get(_right));
		}
		if (typeSystem.isDynamic(recvType)) {
			return found(node, DynamicMember.newSetter(name), methodMatcher, field.get(_recv), node.get(_right));
		}
		throw error(field.get(_name), Message.UndefinedField__, name(recvType), name);
	}

	private boolean isStaticField(Functor f) {
		if (f.ref instanceof Field) {
			return Lang.isStatic((Field) f.ref);
		}
		return false;
	}

	private boolean isReadOnlyField(Functor f) {
		if (f.ref instanceof Field) {
			return Lang.isFinal((Field) f.ref);
		}
		return false;
	}

	private Type typeStaticField(SyntaxTree node) {
		Class<?> recvClass = this.resolveClass(node.get(_recv), null);
		String name = node.getText(_name, "");
		Functor f = typeSystem.getStaticGetter(methodMatcher, recvClass, name);
		// System.out.println("get f" + f);
		if (f != null) {
			node.sub();
			return found(node, f, methodMatcher);
		}
		throw error(node.get(_name), Message.UndefinedField__, name(recvClass), name);
	}

	private Type typeSetStaticField(SyntaxTree node, SyntaxTree field) {
		Class<?> recvClass = this.resolveClass(field.get(_recv), null);
		String name = field.getText(_name, "");
		Functor f = typeSystem.getStaticSetter(methodMatcher, recvClass, name);
		// System.out.println("set f" + f);
		if (f != null) {
			if (isReadOnlyField(f)) {
				throw error(field.get(_name), Message.ReadOnly);
			}
			enforceType(f.get(0), node, _right);
			return found(node, f, methodMatcher, node.get(_right));
		}
		throw error(field.get(_name), Message.UndefinedField__, name(recvClass), name);
	}

	public class MethodApply extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			if (isStaticClassName(node)) {
				return typeStaticMethodApply(node);
			}
			Type recvType = visit(node.get(_recv));
			String name = node.getText(_name, "");
			Type[] a = typeArguments(recvType, node.get(_param));
			Functor f = typeSystem.getMethod(methodMatcher, recvType, name, a);
			if (f != null) {
				if (isStaticClassMethod(f, a.length)) {
					return found(node, f, methodMatcher, node.get(_param));
				}
				return found(node, f, methodMatcher, node.get(_recv), node.get(_param));
			}
			if (typeSystem.isDynamic(recvType)) {
				return makeIndy(node, KonohaFunctor.getIndyMethodFunctor(), name, a, node.get(_recv), node.get(_param));
			}
			Functor[] unmatched = typeSystem.getMethods(recvType, name);
			return unfound(node, unmatched, Message.Method__, name(recvType), name);
		}
	}

	private boolean isStaticMethod(Functor f) {
		if (f.ref instanceof Method) {
			Method m = (Method) f.ref;
			if (Lang.isStatic(m)) {
				return true;
			}
		}
		return false;
	}

	private boolean isStaticClassMethod(Functor f, int paramSize) {
		if (f.ref instanceof Method) {
			Method m = (Method) f.ref;
			if (Lang.isStatic(m) && m.getParameterTypes().length != paramSize) {
				return true;
			}
		}
		return false;
	}

	private boolean isStaticClassName(SyntaxTree node) {
		if (node.get(_recv).is(_Name)) {
			Type t = this.typeSystem.getType(node.get(_recv).toText());
			return t != null;
		}
		return false;
	}

	private Type typeStaticMethodApply(SyntaxTree node) {
		Class<?> staticClass = this.resolveClass(node.get(_recv), null);
		String name = node.getText(_name, "");
		// Type[] a = this.typeArguments(staticClass, node.get(_param));
		Type[] a = this.typeArguments(node.get(_param));
		Functor f = typeSystem.getMethod(methodMatcher, staticClass, name, a);
		if (f != null) {
			if (!isStaticMethod(f)) {
				Functor[] unmatched = { f };
				return unfound(node, unmatched, "not static method(%s::%s)", name(staticClass), name);
			}
			return found(node, f, methodMatcher, node.get(_param));
		}
		Functor[] unmatched = typeSystem.getMethods(staticClass, name);
		return unfound(node, unmatched, Message.Method__, name(staticClass), name);
	}

	public class Indexer extends Undefined {
		@Override
		public Type acceptType(SyntaxTree indexer) {
			Type recvType = visit(indexer.get(_recv));
			Type[] a = typeArguments(recvType, indexer.get(_param));
			Functor f = typeSystem.getMethod(methodMatcher, recvType, "get", a);
			if (f != null) {
				return found(indexer, f, methodMatcher, indexer.get(_recv), indexer.get(_param));
			}
			if (typeSystem.isDynamic(recvType)) {
				return found(indexer, DynamicMember.newIndexGetter("get"), methodMatcher, indexer.get(_recv), indexer.get(_param));
			}
			Functor[] unmatched = typeSystem.getMethods(recvType, "get");
			return unfound(indexer, unmatched, Message.Indexer_, name(recvType));
		}
	}

	private Type typeSetIndexer(SyntaxTree node, SyntaxTree indexer, SyntaxTree expr) {
		Type recvType = visit(indexer.get(_recv));
		Type exprType = visit(indexer.get(_expr));
		Type[] a = typeArguments(recvType, indexer.get(_param), exprType);
		Functor f = typeSystem.getMethod(methodMatcher, recvType, "set", a);
		if (f != null) {
			return found(node, f, methodMatcher, indexer.get(_recv), indexer.get(_param), expr);
		}
		if (typeSystem.isDynamic(recvType)) {
			return found(indexer, DynamicMember.newIndexSetter("set"), methodMatcher, indexer.get(_recv), indexer.get(_param));
		}
		Functor[] unmatched = typeSystem.getMethods(recvType, "set");
		return unfound(indexer, unmatched, Message.Indexer_, name(recvType));
	}

	/* array */

	private Type typeCollectionElement(SyntaxTree node, int step) {
		if (node.size() == 0) {
			return Object.class;
		}
		boolean mixed = false;
		Type elementType = null;
		int shift = step == 2 ? 1 : 0;
		for (int i = 0; i < node.size(); i += step) {
			SyntaxTree sub = node.get(i + shift);
			Type t = visit(sub);
			if (t == elementType) {
				continue;
			}
			if (elementType == null) {
				elementType = t;
			} else {
				mixed = true;
				elementType = Object.class;
			}
		}
		if (mixed) {
			for (int i = 0; i < node.size(); i += step) {
				SyntaxTree sub = node.get(i + shift);
				if (sub.getType() != Object.class) {
					enforceType(Object.class, node, i + shift);
				}
			}
		}
		return elementType;
	}

	public class Array extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			Type elementType = typeCollectionElement(node, 1);
			Type arrayType = typeSystem.newArrayType(elementType);
			return arrayType;
		}
	}

	public class Set extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			Type elementType = typeCollectionElement(node, 1);
			Type arrayType = typeSystem.newArrayType(elementType);
			node.setTag(_Array);
			return arrayType;
		}
	}

	public class Dict extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			Type elementType = typeCollectionElement(node, 1);
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
			return functor(node, KonohaFunctor.getInterpolationFunctor());
		}
	}

	// Syntax Sugar

	private Type typeSelfAssignment(SyntaxTree node, Symbol optag) {
		SyntaxTree op = node.newInstance(optag, 0, null);
		op.sub(_left, node.get(_left).dup(), _right, node.get(_right));
		node.set(_right, op);
		node.setTag(_Assign);
		return typeAssign(node);
	}

	public class AssignAdd extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeSelfAssignment(node, _Add);
		}
	}

	public class AssignSub extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeSelfAssignment(node, _Sub);
		}
	}

	public class AssignMul extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeSelfAssignment(node, _Mul);
		}
	}

	public class AssignDiv extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeSelfAssignment(node, _Div);
		}
	}

	public class AssignMod extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeSelfAssignment(node, _Mod);
		}
	}

	public class AssignLeftShift extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeSelfAssignment(node, _LeftShift);
		}
	}

	public class AssignRightShift extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeSelfAssignment(node, _RightShift);
		}
	}

	public class AssignLogicalRightShift extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeSelfAssignment(node, _LogicalRightShift);
		}
	}

	public class AssignBitwiseAnd extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeSelfAssignment(node, _BitwiseAnd);
		}
	}

	public class AssignBitwiseXOr extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeSelfAssignment(node, _BitwiseXor);
		}
	}

	public class AssignBitwiseOr extends Undefined {
		@Override
		public Type acceptType(SyntaxTree node) {
			return typeSelfAssignment(node, _BitwiseOr);
		}
	}

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

	// new interface

	private TypeMatcher methodMatcher = new TypeMatcher();
	private TypeMatcher castMatcher = new TypeMatcher();

	/* type */

	private Type[] typeArguments(SyntaxTree params) {
		if (params.size() == 0) {
			return emptyTypes;
		}
		Type[] types = new Type[params.size()];
		for (int i = 0; i < params.size(); i++) {
			types[i] = visit(params.get(i));
		}
		return types;
	}

	private Type[] typeArguments(Type recvType, SyntaxTree params) {
		Type[] types = new Type[params.size() + 1];
		types[0] = recvType;
		for (int i = 0; i < params.size(); i++) {
			types[i + 1] = visit(params.get(i));
		}
		return types;
	}

	private Type[] typeArguments(Type recvType, SyntaxTree params, Type exprType) {
		Type[] types = new Type[params.size() + 2];
		types[0] = recvType;
		for (int i = 0; i < params.size(); i++) {
			types[i + 1] = visit(params.get(i));
		}
		types[types.length - 1] = exprType;
		return types;
	}

	private Type found(SyntaxTree node, Functor f, TypeMatcher matcher, SyntaxTree... trees) {
		node.makeFlattenedList(trees);
		return found(node, f, matcher);
	}

	private Type found(SyntaxTree node, Functor f, TypeMatcher matcher) {
		Type returnType = matcher.resolve(f.getReturnType(), Object.class);
		for (int i = 0; i < f.size(); i++) {
			enforceType(matcher, f.get(i), node, i);
		}
		node.setTag(_Functor);
		node.setValue(f);
		return returnType;
	}

	private Type makeIndy(SyntaxTree node, Functor f, String name, Type[] a, SyntaxTree... trees) {
		SyntaxTree tlookup = node.newInstance(_Name, 0, "__lookup__");
		visit(tlookup); // typed
		SyntaxTree tname = node.newConst(String.class, name);
		SyntaxTree tparam = node.newConst(int.class, typeSystem.getIndyParameterTypes(a));
		SyntaxTree targs = node.newInstance(_Array, 0, null);
		targs.makeFlattenedList(trees);
		for (int i = 0; i < targs.size(); i++) {
			enforceType(Object.class, targs, i);
		}
		targs.setType(Object[].class);
		node.setTag(_Functor);
		node.makeFlattenedList(tlookup, tname, tparam, targs);
		node.setValue(f);
		return Object.class;
	}

	private Type functor(SyntaxTree node, Functor f) {
		node.setTag(_Functor);
		node.setFunctor(f);
		node.setType(f.getReturnType());
		return f.getReturnType();
	}

	private Type unfound(SyntaxTree node, Functor[] unmatched, Message fmt, Object... args) {
		String methods = unfoundMessage(unmatched);
		String msg = String.format(fmt.toString(), args);
		if (methods == null) {
			msg = String.format(Message.UndefinedFunctor_.toString(), msg);
		} else {
			msg = String.format(Message.MismatchedFunctor__.toString(), msg, methods);
		}
		throw error(node, msg);
	}

	private final String unfoundMessage(Functor[] unmatched) {
		String mismatched = null;
		if (unmatched != null && unmatched.length > 0) {
			StringBuilder sb = new StringBuilder();
			for (Functor f : unmatched) {
				sb.append(" ");
				sb.append(f.toBetterString());
			}
			mismatched = sb.toString();
		}
		return mismatched;
	}

	@Deprecated
	private Type unfound(SyntaxTree node, Functor[] unmatched, String fmt, Object... args) {
		String methods = unfoundMessage(unmatched);
		String msg = String.format(fmt, args);
		if (methods == null) {
			msg = String.format(Message.UndefinedFunctor_.toString(), msg);
		} else {
			msg = String.format(Message.MismatchedFunctor__.toString(), msg, methods);
		}
		throw error(node, msg);
	}

	// resolve

	public final Type resolveType(SyntaxTree node, Type deftype) {
		if (node == null) {
			return deftype;
		}
		if (node.size() == 0) {
			Type t = this.typeSystem.getType(node.toText());
			if (t == null) {
				throw this.error(node, Message.UndefinedType_, node.toText());
			}
			return t == null ? deftype : t;
		}
		if (node.is(_TypeOf)) {
			try {
				return visit(node.get(_expr));
			} catch (TypeCheckerException e) {
				context.log(e.getMessage());
			}
			return Object.class;
		}
		if (node.is(_ArrayType)) {
			return GenericType.newType(konoha.Array.class, resolveType(node.get(_base), Object.class));
		}
		if (node.is(_GenericType)) {
			Class<?> base = this.resolveClass(node.get(_base), null);
			if (base == null) {
				return deftype;
			}
			SyntaxTree params = node.get(_param);
			if (base == Function.class) {
				Class<?>[] p = new Class<?>[params.size() - 1];
				for (int i = 0; i < p.length; i++) {
					p[0] = resolveClass(params.get(i + 1), Object.class);
				}
				return this.typeSystem.getFuncType(resolveClass(params.get(0), void.class), p);
			} else {
				int paramSize = base.getTypeParameters().length;
				if (node.get(_param).size() != paramSize) {
					throw this.error(node, "mismatched parameter number %s", base);
				}
				Type[] p = new Type[paramSize];
				int c = 0;
				for (SyntaxTree sub : node.get(_param)) {
					p[c] = resolveType(sub, Object.class);
					c++;
				}
				return GenericType.newType(base, p);
			}
		}
		return deftype;
	}

	public final Class<?> resolveClass(SyntaxTree node, Class<?> deftype) {
		Type t = this.resolveType(node, deftype);
		if (t == null) {
			return deftype;
		}
		return Lang.toClassType(t);
	}

	// matching library

	private void enforceType(TypeMatcher matcher, Type req, SyntaxTree node, int index) {
		SyntaxTree unode = node.get(index);
		visit(unode);
		node.set(index, this.enforceType(matcher, req, unode));
	}

	private void enforceType(TypeMatcher matcher, Type req, SyntaxTree node, Symbol label) {
		SyntaxTree unode = node.get(label);
		visit(unode);
		node.set(label, this.enforceType(matcher, req, unode));
	}

	private void enforceType(Type req, SyntaxTree node, int index) {
		enforceType(null, req, node, index);
	}

	private void enforceType(Type req, SyntaxTree node, Symbol label) {
		enforceType(null, req, node, label);
	}

	private final SyntaxTree enforceType(TypeMatcher matcher, Type reqt, SyntaxTree node) {
		if (FunctorLookup.accept(matcher, reqt, node.getType())) {
			return node;
		}
		Type resolved = matcher != null ? matcher.resolve(reqt, null) : reqt;
		SyntaxTree converted = tryCoersion(resolved, node);
		if (converted == null) {
			throw error(node, Message.TypeError__, name(reqt), name(node.getType()));
		}
		return converted;
	}

	private Type tryCastBeforeMatching(Type req, SyntaxTree node, Symbol label) {
		SyntaxTree child = node.get(label);
		Functor f = typeSystem.getCast(child.getType(), req);
		if (f != null) {
			SyntaxTree newnode = node.newInstance(_Functor, 1, f);
			newnode.set(0, _expr, child);
			newnode.setFunctor(f);
			newnode.setType(req);
			node.set(label, newnode);
			return req;
		}
		return node.getType();
	}

	private final SyntaxTree tryCoersion(Type reqt, SyntaxTree node) {
		Type expt = node.getType();
		Functor f = typeSystem.getCast(expt, reqt);
		if (f != null) {
			SyntaxTree newnode = node.newInstance(_Functor, 1, f);
			newnode.set(0, _expr, node);
			functor(newnode, f);
			return newnode;
		}
		if (expt == Object.class) { // auto downcast
			SyntaxTree newnode = node.newInstance(_Cast, 1, null);
			newnode.set(0, _expr, node);
			newnode.setType(reqt);
			return newnode;
		}
		return null;
	}

	private TypeCheckerException error(SyntaxTree node, Message fmt, Object... args) {
		return new TypeCheckerException(node, fmt.toString(), args);
	}

	private TypeCheckerException error(SyntaxTree node, String fmt, Object... args) {
		return new TypeCheckerException(node, fmt, args);
	}

	public boolean verboseTypeInference = true;

	private void reportInferredType(SyntaxTree node, Message fmt, Object... args) {
		if (verboseTypeInference) {
			this.reportWarning(node, fmt, args);
		}
	}

	public void reportWarning(SyntaxTree node, String fmt, Object... args) {
		String msg = node.formatSourceMessage("warning", String.format(fmt, args));
		context.log(msg);
	}

	public void reportWarning(SyntaxTree node, Message fmt, Object... args) {
		String msg = node.formatSourceMessage("warning", String.format(fmt.toString(), args));
		context.log(msg);
	}

}
