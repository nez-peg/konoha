package konoha.script;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;

import konoha.Function;
import konoha.api.BooleanOp;
import konoha.api.DoubleOp;
import konoha.api.IntOp;
import konoha.api.LongOp;
import konoha.api.ObjectOp;
import konoha.api.StringOp;
import konoha.asm.ScriptCompiler;
import konoha.hack.Hacker;
import nez.util.UList;

public class TypeSystem extends FunctorLookup implements CommonSymbols {
	ScriptContext context;
	ScriptCompiler compl;

	public TypeSystem(ScriptContext context) {
		this.context = context;
	}

	void init() {
		loadStaticFunctionClass(ObjectOp.class, false);
		loadStaticFunctionClass(BooleanOp.class, false);
		loadStaticFunctionClass(IntOp.class, false);
		loadStaticFunctionClass(LongOp.class, false);
		loadStaticFunctionClass(DoubleOp.class, false);
		loadStaticFunctionClass(StringOp.class, false);
		loadStaticFunctionClass(konoha.libc.class, false);
		this.setType("void", void.class);
		this.setType("boolean", boolean.class);
		this.setType("byte", byte.class);
		this.setType("char", char.class);
		this.setType("short", int.class);
		this.setType("int", int.class);
		this.setType("long", long.class);
		this.setType("float", double.class);
		this.setType("double", double.class);
		this.setType("String", String.class);
		this.setType("Array", konoha.Array.class);
		this.setType("Dict", konoha.Dict.class);
		this.setType("Func", Function.class);
		this.setType("RuntimeException", RuntimeException.class);
		this.setType("NullPointerException", NullPointerException.class);
		this.setType("ArithmeticException", ArithmeticException.class);
		this.loadSyntaxClass(konoha.syntax.SelfAssign.class);
	}

	public void init(ScriptCompiler compl) {
		this.compl = compl; // this is called when the complier is instatiated
	}

	// void initDebug() {
	// this.setType("Math", Math.class);
	// this.setType("System", System.class);
	// }

	/* mode */
	protected boolean shellMode = false;

	public void setShellMode(boolean b) {
		this.shellMode = b;
	}

	/* Types */

	HashMap<String, Type> TypeNames = new HashMap<>();

	public void setType(String name, Type type) {
		this.TypeNames.put(name, type);
	}

	public final Type getType(String name) {
		return this.TypeNames.get(name);
	}

	/* ArrayType */

	public Type newArrayType(Type elementType) {
		return GenericType.newType(konoha.Array.class, elementType);
	}

	/* FuncType */

	public Class<?> getFuncType(Class<?> returnType, Class<?>... paramTypes) {
		String name = ScriptCompiler.nameFuncType(returnType, paramTypes);
		Class<?> c = (Class<?>) this.TypeNames.get(name);
		if (c == null) {
			c = this.compl.compileFuncType(name, returnType, paramTypes);
			this.TypeNames.put(name, c);
		}
		return c;
	}

	public Class<?> getFuncType(Type returnType, Type... paramTypes) {
		String name = ScriptCompiler.nameFuncType(returnType, paramTypes);
		Class<?> c = (Class<?>) this.TypeNames.get(name);
		if (c == null) {
			Class<?>[] p = new Class<?>[paramTypes.length];
			for (int i = 0; i < p.length; i++) {
				p[i] = Lang.toClassType(paramTypes[i]);
			}
			c = this.compl.compileFuncType(name, Lang.toClassType(returnType), p);
			this.TypeNames.put(name, c);
		}
		return c;
	}

	public boolean isFuncType(Type f) {
		return this.isStaticFuncType(f) || this.isDynamicFuncType(f);
	}

	public boolean isDynamicFuncType(Type f) {
		if (f == konoha.Function.class) {
			return true;
		}
		return false;
	}

	public boolean isStaticFuncType(Type f) {
		if (f instanceof Class<?>) {
			return ((Class<?>) f).getSuperclass() == konoha.Function.class;
		}
		return false;
	}

	public final static Class<?> getFuncReturnType(Type f) {
		if (f == konoha.Function.class) {
			return Object.class;
		}
		Method m = Reflector.findInvokeMethod((Class<?>) f);
		return m.getReturnType();
	}

	public final static Class<?>[] getFuncParameterTypes(Type f) {
		Method m = Reflector.findInvokeMethod((Class<?>) f);
		return m.getParameterTypes();
	}

	/* Dynamic type */

	public boolean isDynamic(Type c) {
		return c == Object.class;
	}

	public Type dynamicType() {
		return Object.class;
	}

	private UList<Type[]> paramList = new UList<>(new Type[16][]);
	private HashMap<String, Integer> paramMap = new HashMap<>();

	public final int getIndyParameterTypes(Type[] a) {
		StringBuilder sb = new StringBuilder();
		sb.append(a.length);
		for (Type t : a) {
			sb.append(",");
			sb.append(Lang.name(t));
		}
		String key = sb.toString();
		Integer n = paramMap.get(key);
		if (n == null) {
			n = paramList.size();
			paramList.add(a);
			paramMap.put(key, n);
		}
		return n;
	}

	public final Type[] getIndyParameterTypes(int paramId) {
		return paramList.get(paramId);
	}

	/* GlobalVariables */

	HashMap<String, GlobalVariable> GlobalVariables = new HashMap<>();

	public boolean hasGlobalVariable(String name) {
		return this.GlobalVariables.containsKey(name);
	}

	public GlobalVariable getGlobalVariable(String name) {
		return this.GlobalVariables.get(name);
	}

	public GlobalVariable newGlobalVariable(Type type, String name) {
		Class<?> varClass = this.compl.compileGlobalVariable(Lang.toClassType(type), name);
		GlobalVariable gv = new GlobalVariable(type, varClass);
		this.GlobalVariables.put(name, gv);
		return gv;
	}

	// private GlobalVariable addDebugGlobalVariable(Type type, String name,
	// Class<?> varClass) {
	// GlobalVariable gv = new GlobalVariable(type, varClass);
	// this.GlobalVariables.put(name, gv);
	// return gv;
	// }

	public void loadStaticFunctionClass(Class<?> c, boolean isGenerated) {
		this.addSymbol(c);
	}

	public void loadSyntaxClass(Class<?> c) {
		Hacker.hack(c, context);
	}

	public void importStaticClass(String path) throws ClassNotFoundException {
		Class<?> c = Class.forName(path);
		if (Hacker.isHackerClass(c)) {
			Hacker.hack(c, context);
		} else {
			loadStaticFunctionClass(c, false);
			this.setType(c.getSimpleName(), c);
		}
	}

	public Functor newPrototype(SyntaxTree node, Type returnType, String name, Type[] paramTypes) {
		Functor f = this.compl.newPrototypeFunction(node, returnType, name, paramTypes);
		this.addSymbolFunctor(f);
		return f;
	}

	public Functor getPrototype(Type returnType, String name, Type[] paramTypes) {
		for (int i = symbolList.size() - 1; i >= 0; i--) {
			Functor f = symbolList.ArrayValues[i];
			if (f.getName().equals(name) && f.getReturnType() == returnType && f.match(paramTypes)) {
				return f;
			}
		}
		return null;
	}

}
