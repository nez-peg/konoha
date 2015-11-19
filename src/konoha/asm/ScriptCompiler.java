package konoha.asm;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import konoha.Function;
import konoha.script.CommonSymbols;
import konoha.script.Functor;
import konoha.script.GenericType;
import konoha.script.Reflector;
import konoha.script.Syntax;
import konoha.script.SyntaxTree;
import konoha.script.TypeSystem;
import nez.ast.Tree;

public class ScriptCompiler {
	TypeSystem typeSystem;
	final ScriptClassLoader cLoader;
	public ScriptCompilerAsm asm;

	public ScriptCompiler(TypeSystem typeSystem) {
		this.typeSystem = typeSystem;
		this.cLoader = new ScriptClassLoader(typeSystem);
		this.asm = new KonohaCompilerAsm(this.typeSystem, this.cLoader);
		this.typeSystem.init(this);
	}

	public Class<?> compileGlobalVariable(Class<?> type, String name) {
		return this.asm.compileGlobalVariableClass(type, name);
	}

	public Class<?> compileFuncType(String name, Class<?> returnType, Class<?>... paramTypes) {
		return this.asm.compileFuncType(name, returnType, paramTypes);
	}

	public static String nameFuncType(Class<?> returnType, Class<?>... paramTypes) {
		StringBuilder sb = new StringBuilder();
		sb.append("Func");
		sb.append(paramTypes.length);
		sb.append('$');
		sb.append(returnType.getSimpleName());
		for (Class<?> p : paramTypes) {
			sb.append('$');
			sb.append(p.getSimpleName());
		}
		sb.append('_');
		return sb.toString();
	}

	public static String nameFuncType(Type returnType, Type... paramTypes) {
		StringBuilder sb = new StringBuilder();
		sb.append("Func");
		sb.append(paramTypes.length);
		sb.append('$');
		sb.append(name(returnType));
		for (Type p : paramTypes) {
			sb.append('$');
			sb.append(name(p));
		}
		sb.append('_');
		return sb.toString();
	}

	private static String name(Type t) {
		if (t instanceof GenericType) {
			return ((GenericType) t).getRawType().getSimpleName();
		}
		if (t instanceof Class<?>) {
			return ((Class<?>) t).getSimpleName();
		}
		return "Object";
	}

	public Function compileStaticFunctionObject(Method m) {
		Class<?> functype = this.typeSystem.getFuncType(m.getReturnType(), m.getParameterTypes());
		Class<?> c = this.asm.compileFunctionWrapperClass(functype, m);
		return (Function) Reflector.newInstance(c);
	}

	public Function compileFunction(Tree<?> node) {
		return null;
	}

	public void compileClassDecl(Tree<?> node) {
		Class<?> clazz = this.asm.compileClass((SyntaxTree) node);
		typeSystem.loadDefinedClass(clazz);
	}

	public Function compileLambda(SyntaxTree node) {
		Class<?> lambdaClass = asm.compileLambda(node);
		return (Function) Reflector.newInstance(lambdaClass);
	}

	public Functor newPrototypeFunction(SyntaxTree node, java.lang.reflect.Type returnType, String name, java.lang.reflect.Type[] paramTypes) {
		String cname = this.asm.nameFunctionClass(node, name);
		return new Functor(Syntax.Function, new FunctionPrototype(cname, returnType, name, paramTypes));
	}

	public Class<?> compileFuncDecl(SyntaxTree node, Functor symbolFunctor) {
		String name = node.getText(CommonSymbols._name, null);
		String cname = null;
		if (symbolFunctor != null) {
			cname = symbolFunctor.getClassName();
		} else {
			cname = this.asm.nameFunctionClass(node, name);
		}
		Class<?> function = this.asm.compileStaticFuncDecl(cname, node);
		if (symbolFunctor != null) {
			symbolFunctor.update(function.getDeclaredMethods()[0]);
		} else {
			typeSystem.loadStaticFunctionClass(function, true);
		}
		return function;
	}

}
