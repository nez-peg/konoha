package konoha.script;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;

import konoha.Function;
import konoha.dynamic.ComparatorSite;
import konoha.dynamic.DynamicSite;
import konoha.dynamic.GetterSite;
import konoha.dynamic.MethodSite;
import konoha.dynamic.SetterSite;
import konoha.dynamic.UnarySite;
import konoha.message.Message;
import nez.ast.Symbol;
import nez.util.VisitorMap;

public abstract class TypeChecker extends VisitorMap<TreeChecker> implements CommonSymbols {
	protected final ScriptContext context;
	protected final TypeSystem typeSystem;

	public TypeChecker(ScriptContext context, TypeSystem typeSystem) {
		this.context = context;
		this.typeSystem = typeSystem;
	}

	public abstract void init();

	/* Visitor */

	public Type visit(SyntaxTree node) {
		Type c = node.getType();
		if (c == null) {
			if (this.isOperator(node)) {
				c = this.typeOperator(node);
			} else {
				c = find(node.getTag().toString()).acceptType(node);
			}
			if (c == null) {
				Debug.FIXME("UNTYPED NODE %s", node);
			}
			node.setType(c);
		}
		return c;
	}

	/* Operator */

	protected HashMap<String, DynamicSite> siteMap = new HashMap<>();

	public final void add(String s, DynamicSite site) {
		siteMap.put(s, site);
	}

	public final void add(Symbol s, DynamicSite site) {
		siteMap.put(s.toString(), site);
	}

	public final Type typeBinary(SyntaxTree node, DynamicSite site) {
		Type[] a = { visit(node.get(_left)), visit(node.get(_right)) };
		Functor f = site.lookup(methodMatcher, a);
		if (f != null) {
			if (site instanceof ComparatorSite && f.get(0) == Object.class) {
				/* Object comparators need to recheck at runtime */
				a[0] = Object.class;
				f = null;
			}
			if (f != null) {
				return found(node, f, methodMatcher);
			}
		}
		if (typeSystem.isDynamic(a[0])) {
			f = new Functor(Syntax.Operator, site.op(site.getTargetName(), Object.class, a));
			return setDynamicFunctor(node, f, node.get(_left), node.get(_right));
		}
		Functor[] fs = this.typeSystem.getMethods(a[0], site.getTargetName());
		return unfound(node, fs, Message.Binary___, name(node.get(_left).getType()), OperatorNames.name(site.getTargetName()), name(node.get(_right).getType()));
	}

	public final Type typeUnary(SyntaxTree node, DynamicSite site) {
		Type[] a = { visit(node.get(_expr)) };
		Functor f = site.lookup(methodMatcher, a);
		if (f != null) {
			return found(node, f, methodMatcher);
		}
		if (typeSystem.isDynamic(a[0])) {
			f = new Functor(Syntax.Operator, site.op(site.getTargetName(), Object.class, a));
			return setDynamicFunctor(node, f, node.get(_expr));
		}
		Functor[] fs = this.typeSystem.getMethods(a[0], site.getTargetName());
		return unfound(node, fs, Message.Unary__, OperatorNames.name(site.getTargetName()), name(a[0]));
	}

	public final boolean isOperator(SyntaxTree node) {
		DynamicSite site = this.siteMap.get(node.getTag().toString());
		if (site != null) {
			return true;
		}
		return false;
	}

	public final Type typeOperator(SyntaxTree node) {
		DynamicSite site = this.siteMap.get(node.getTag().toString());
		if (site instanceof UnarySite) {
			return typeUnary(node, site);
		}
		return typeBinary(node, site);
	}

	FunctionBuilder function = null;

	public final FunctionBuilder enterFunction(String name) {
		this.function = new FunctionBuilder(this.function, name);
		return this.function;
	}

	public final FunctionBuilder enterLambda() {
		this.function = new FunctionBuilder(this.function);
		return this.function;
	}

	public final void exitFunction() {
		this.function = this.function.pop();
	}

	public final boolean inFunction() {
		return this.function != null;
	}

	public final boolean inLambda() {
		if (inFunction()) {
			return this.function.getName().equals("");
		}
		return false;
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

	protected final String name(Type t) {
		return Lang.name(t);
	}

	public void typed(SyntaxTree node, Type c) {
		node.setType(c);
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

	/* ClassDecl */
	public Type typeClassDecl(SyntaxTree node) {
		visit(node.get(_body));
		return void.class;
	}

	public Type typeConstructor(SyntaxTree node) {
		String name = "<init>";
		SyntaxTree bodyNode = node.get(_body, null);
		Type returnType = void.class;
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
		typed(node.get(_param), f.getReturnType());
		return void.class;
	}

	public Type typeFieldDecl(SyntaxTree node) {
		Type type = resolveType(node.get(_type), null);
		SyntaxTree list = node.get(_list);
		for (SyntaxTree field : list) {
			Type exprType = Object.class;
			if (field.has(_expr)) {
				exprType = visit(field.get(_expr));
			}
			if (type == null) {
				type = exprType;
			}
			field.get(_name).setType(type);
		}
		node.sub(_list, list);
		node.makeFlattenedList(list);
		return void.class;
	}

	public Type typeMethodDecl(SyntaxTree node) {
		typeFuncDecl(node);
		return void.class;
	}

	/* FuncDecl */
	private static Type[] EmptyTypes = new Type[0];

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

	public Type typeLambda(SyntaxTree node) {
		String name = "Lambda";
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
		FunctionBuilder f = this.enterLambda();
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
		typed(node.get(_param), f.getReturnType());

		// set free variables
		String[] freeVarNames = f.getFreeVarNames();
		SyntaxTree freeVarList = node.newInstance(_List, freeVarNames.length, null);
		for (String key : freeVarNames) {
			SyntaxTree freeVar = freeVarList.newInstance(_Name, 0, key);
			freeVar.setType(f.getFreeVarType(key));
			freeVarList.sub(_name, freeVar);
		}
		node.add(_list, freeVarList);

		return Function.class;
	}

	public void checkInFunction(SyntaxTree node) {
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

	public SyntaxTree newDefault(SyntaxTree node, Type type) {
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

	public Type typeVarDecl(SyntaxTree node) {
		boolean isArrayName = false;
		String name = node.getText(_name, null);
		ArrayList<SyntaxTree> arraySizeList = null;
		if (node.get(_name).is(_ArrayName)) {
			name = node.get(_name).getText(_name, null);
			arraySizeList = new ArrayList<>();
			SyntaxTree arrayName = node.get(_name);
			while (arrayName != null && arrayName.is(_ArrayName)) {
				arraySizeList.add(arrayName.get(_param, null));
				arrayName = arrayName.get(_name, null);
			}
			isArrayName = true;
		}
		Type type = resolveType(node.get(_type, null), null);
		if (type != null) {
			if (isArrayName) {
				if (!node.has(_expr) && arraySizeList != null) {
					this.reportWarning(node.get(_name), Message.CStyleArray);
					SyntaxTree expr = node.newInstance(_NewArray);
					SyntaxTree arrayName = node.get(_name);
					while (arrayName.has(_name)) {
						arrayName = arrayName.get(_name);
					}
					int i = 0;
					for (SyntaxTree arraySize : arraySizeList) {
						type = typeSystem.newArrayType(type);
						expr.add(_size, arraySize);
						enforceType(int.class, expr, i);
						i++;
					}
					expr.setType(type);
					if (i > 1) {
						expr.setTag(_NewArray2);
					}
					// node.add(_expr, expr);
					node.sub(_type, node.get(_type), _name, arrayName, _expr, expr);
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

	public void typeVarDecl(Type type, SyntaxTree node) {
		String name = node.getText(_name, null);
		if (node.get(_name).is(_ArrayName)) {
			name = node.get(_name).getText(_name, null);
			type = typeSystem.newArrayType(type);
			ArrayList<SyntaxTree> arraySizeList = new ArrayList<>();
			SyntaxTree arrayName = node.get(_name);
			while (arrayName != null && arrayName.is(_ArrayName)) {
				arraySizeList.add(arrayName.get(_param, null));
				arrayName = arrayName.get(_name, null);
			}
			if (!node.has(_expr) && arraySizeList != null) {
				this.reportWarning(node.get(_name), Message.CStyleArray);
				SyntaxTree expr = node.newInstance(_NewArray);
				int i = 0;
				for (SyntaxTree arraySize : arraySizeList) {
					expr.add(_size, arraySize);
					enforceType(int.class, expr, i);
					i++;
				}
				if (i > 0) {
					expr.setTag(_NewArray2);
				}
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

	public Type tryCheckNameType(SyntaxTree node, boolean rewrite) {
		String name = node.toText();
		if (this.inLambda()) {
			if (this.function.containsVariable(name)) {
				return this.function.getVarType(name);
			} else if (this.function.parent != null && this.function.parent.containsVariable(name)) {
				Type varType = this.function.parent.getVarType(name);
				if (rewrite) {
					node.setTag(_GetFreeVar);
					node.setType(varType);
					this.function.addFreeVariable(name, varType);
				}
				return varType;
			}
		}
		if (this.inFunction()) {
			if (this.function.containsVariable(name)) {
				return this.function.getVarType(name);
			}
		}
		if (this.typeSystem.hasGlobalVariable(name)) {
			GlobalVariable gv = this.typeSystem.getGlobalVariable(name);
			if (rewrite) {
				node.sub();
				setFunctor(node, gv.getGetter());
			}
			return gv.getType();
		}
		return null;
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
			Type t = typeSetIndexer(node, node.get(_left), node.get(_right));
			if (node.has(_left)) {
				SyntaxTree setNode = node.get(_left);
				node.sub(null, setNode);
				node.setTag(_Block);
			}
			return t;
		}
		if (leftnode.is(_Field)) {
			return typeSetField(node, node.get(_left));
		}
		assert (leftnode.is(_Name));
		String name = node.getText(_left, "");
		Type rightType = visit(node.get(_right));
		if (this.inLambda()) {
			if (this.function.containsVariable(name)) {
				Type t = this.function.getVarType(name);
				node.get(_left).setType(t);
				enforceType(t, node, _right);
				return t;
			} else if (this.function.parent != null && this.function.parent.containsVariable(name)) {
				Type t = this.function.parent.getVarType(name);
				node.setTag(_SetFreeVar);
				leftnode.setType(t);
				enforceType(t, node, _right);
				return t;
			}
		}
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
			setFunctor(node, f);
			return t;
		}
		throw error(node.get(_left), Message.UndefinedName_, name);
	}

	/* Expression */

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
			return setFunctor(node, f);
		}
		if (exp.isAssignableFrom(req)) { // downcast
			node.setTag(_DownCast);
			return t;
		}
		throw error(node.get(_type), Message.UndefinedCast__, name(inner), name(t));
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
		String name = node.getText(_name, "");
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
				return setFunctor(node.get(_name), gv.getGetter());
			}
		}
		return null;
	}

	private Type typeFuncApply(SyntaxTree node, Type funcType) {
		if (typeSystem.isStaticFuncType(funcType)) {
			Functor f = new Functor(Syntax.FuncObject, funcType);
			methodMatcher.init(null);
			return found(node, f, methodMatcher, node.get(_name), node.get(_param));
		} else if (inFunction()) {
			SyntaxTree params = node.get(_param);
			for (int i = 0; i < params.size(); i++) {
				enforceType(Object.class, params, i);
			}
			params.setTag(_Array);
			params.setType(Object[].class);
			if (params.size() > 0) {
				node.sub(_name, node.get(_name), _param, params);
			} else {
				node.sub(_name, node.get(_name));
			}
			return setFunctor(node, KonohaFunctor.getInvokeFunc());
		} else {
			SyntaxTree params = node.get(_param);
			node.sub(_name, node.get(_name));
			for (int i = 0; i < params.size(); i++) {
				enforceType(Object.class, params, i);
				node.add(_expr, params.get(i));
			}
			return setFunctor(node, KonohaFunctor.getInvokeFunc());
		}
	}

	public Type typeNullCheck(Symbol tag, SyntaxTree node, Symbol expr) {
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

	/* object oriented */

	public Type typeField(SyntaxTree node) {
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
			f = new Functor(Syntax.Getter, new GetterSite(typeSystem, name, Object.class, new Type[] { recvType }));
			return setDynamicFunctor(node, f, node.get(_recv));
		}
		throw error(node.get(_name), Message.UndefinedField__, name(recvType), name);

	}

	public Type typeSetField(SyntaxTree node, SyntaxTree field) {
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
			Type exprType = visit(node.get(_right));
			f = new Functor(Syntax.Setter, new SetterSite(typeSystem, name, void.class, new Type[] { recvType, exprType }));
			return setDynamicFunctor(node, f, field.get(_recv), node.get(_right));
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

	/* MethodApply */

	public Type typeMethodApply(SyntaxTree node) {
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
			f = new Functor(Syntax.Method, new MethodSite(typeSystem, name, Object.class, a));
			return setDynamicFunctor(node, f, node.get(_recv));
		}
		Functor[] unmatched = typeSystem.getMethods(recvType, name);
		return unfound(node, unmatched, Message.Method__, name(recvType), name);
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

	/* Indexer */

	public Type typeIndexer(SyntaxTree node) {
		Type recvType = visit(node.get(_recv));
		Type[] a = typeArguments(recvType, node.get(_param));
		Functor f = typeSystem.getMethod(methodMatcher, recvType, "get", a);
		if (f != null) {
			return found(node, f, methodMatcher, node.get(_recv), node.get(_param));
		}
		if (typeSystem.isDynamic(recvType)) {
			f = new Functor(Syntax.Indexer, new MethodSite(typeSystem, "get", Object.class, a));
			return setDynamicFunctor(node, f, node.get(_recv), node.get(_param));
		}
		Functor[] unmatched = typeSystem.getMethods(recvType, "get");
		return unfound(node, unmatched, Message.Indexer_, name(recvType));
	}

	public Type typeSetIndexer(SyntaxTree node, SyntaxTree indexer, SyntaxTree expr) {
		Type recvType = visit(indexer.get(_recv));
		Type exprType = visit(expr);
		Type[] a = typeArguments(recvType, indexer.get(_param), exprType);
		Functor f = typeSystem.getMethod(methodMatcher, recvType, "set", a);
		if (f != null) {
			return found(node, f, methodMatcher, indexer.get(_recv), indexer.get(_param), expr);
		}
		if (typeSystem.isDynamic(recvType)) {
			f = new Functor(Syntax.SetIndexer, new MethodSite(typeSystem, "set", void.class, a));
			return setDynamicFunctor(node, f, indexer.get(_recv), indexer.get(_param), expr);
		}
		Functor[] unmatched = typeSystem.getMethods(recvType, "set");
		return unfound(indexer, unmatched, Message.Indexer_, name(recvType));
	}

	/* array */

	public Type typeArrayElements(SyntaxTree node, int step) {
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

	// new interface

	protected TypeMatcher methodMatcher = new TypeMatcher();
	protected TypeMatcher castMatcher = new TypeMatcher();

	/* type */

	public final Type[] typeArguments(SyntaxTree params) {
		if (params.size() == 0) {
			return Lang.EmptyTypes;
		}
		Type[] types = new Type[params.size()];
		for (int i = 0; i < params.size(); i++) {
			types[i] = visit(params.get(i));
		}
		return types;
	}

	public final Type[] typeArguments(Type recvType, SyntaxTree params) {
		Type[] types = new Type[params.size() + 1];
		types[0] = recvType;
		for (int i = 0; i < params.size(); i++) {
			types[i + 1] = visit(params.get(i));
		}
		return types;
	}

	public final Type[] typeArguments(Type recvType, SyntaxTree params, Type exprType) {
		Type[] types = new Type[params.size() + 2];
		types[0] = recvType;
		for (int i = 0; i < params.size(); i++) {
			types[i + 1] = visit(params.get(i));
		}
		types[types.length - 1] = exprType;
		return types;
	}

	public final Type found(SyntaxTree node, Functor f, TypeMatcher matcher, SyntaxTree... trees) {
		node.makeFlattenedList(trees);
		return found(node, f, matcher);
	}

	public final Type found(SyntaxTree node, Functor f, TypeMatcher matcher) {
		Type returnType = matcher.resolve(f.getReturnType(), Object.class);
		for (int i = 0; i < f.size(); i++) {
			enforceType(matcher, f.get(i), node, i);
		}
		node.setTag(_Functor);
		node.setValue(f);
		return returnType;
	}

	public Type setDynamicFunctor(SyntaxTree node, Functor f, SyntaxTree... trees) {
		node.makeFlattenedList(trees);
		for (int i = 0; i < node.size(); i++) {
			enforceType(Object.class, node, i);
		}
		node.setTag(_Functor);
		node.setValue(f);
		return Object.class;
	}

	public final Type setFunctor(SyntaxTree node, Functor f) {
		node.setTag(_Functor);
		node.setFunctor(f);
		node.setType(f.getReturnType());
		return f.getReturnType();
	}

	public final Type unfound(SyntaxTree node, Functor[] unmatched, Message fmt, Object... args) {
		String methods = unfoundMessage(unmatched);
		String msg = String.format(fmt.toString(), args);
		if (methods == null) {
			msg = String.format(Message.UndefinedFunctor_.toString(), msg);
		} else {
			msg = String.format(Message.MismatchedFunctor__.toString(), msg, methods);
		}
		throw error(node, msg);
	}

	public final String unfoundMessage(Functor[] unmatched) {
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

	public void enforceType(TypeMatcher matcher, Type req, SyntaxTree node, int index) {
		SyntaxTree unode = node.get(index);
		visit(unode);
		node.set(index, this.enforceType(matcher, req, unode));
	}

	public void enforceType(TypeMatcher matcher, Type req, SyntaxTree node, Symbol label) {
		SyntaxTree unode = node.get(label);
		visit(unode);
		node.set(label, this.enforceType(matcher, req, unode));
	}

	public void enforceType(Type req, SyntaxTree node, int index) {
		enforceType(null, req, node, index);
	}

	public void enforceType(Type req, SyntaxTree node, Symbol label) {
		enforceType(null, req, node, label);
	}

	public final SyntaxTree enforceType(TypeMatcher matcher, Type reqt, SyntaxTree node) {
		if (FunctorLookup.accept(matcher, reqt, node.getType())) {
			return node;
		}
		Type reqt_resolved = matcher != null ? matcher.resolve(reqt, null) : reqt;
		SyntaxTree converted = tryCoersion(reqt_resolved, node);
		if (converted == null) {
			throw error(node, Message.TypeError__, name(reqt), name(node.getType()));
		}
		return converted;
	}

	private final SyntaxTree tryCoersion(Type reqt, SyntaxTree node) {
		Type expt = node.getType();
		Functor f = typeSystem.getCast(expt, reqt);
		if (f != null) {
			SyntaxTree newnode = node.newInstance(_Functor, 1, f);
			newnode.set(0, _expr, node);
			setFunctor(newnode, f);
			return newnode;
		}
		if (expt == Object.class) { // Auto Downcast
			SyntaxTree newnode = node.newInstance(_Cast, 1, null);
			newnode.set(0, _expr, node);
			newnode.setType(reqt);
			return newnode;
		}
		return null;
	}

	public TypeCheckerException error(SyntaxTree node, Message fmt, Object... args) {
		return new TypeCheckerException(node, fmt.toString(), args);
	}

	public TypeCheckerException error(SyntaxTree node, String fmt, Object... args) {
		return new TypeCheckerException(node, fmt, args);
	}

	public boolean verboseTypeInference = true;

	public void reportInferredType(SyntaxTree node, Message fmt, Object... args) {
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
