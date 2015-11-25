package konoha.syntax;

import java.util.ArrayList;

import konoha.script.Evaluator;
import konoha.script.FunctionBuilder;
import konoha.script.Functor;
import konoha.script.ScriptContext;
import konoha.script.SyntaxTree;
import konoha.script.TypeChecker;
import konoha.script.TypeCheckerException;
import nez.ast.Symbol;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

public abstract class ClassDeclaration extends SyntaxExtension {

	public static void hack(ScriptContext context) {
		context.addSyntaxExtension(new ClassDecl(context));
		context.addSyntaxExtension(new Constructor(context));
		context.addSyntaxExtension(new FieldDecl(context));
		context.addSyntaxExtension(new MethodDecl(context));
	}

	public ClassDeclaration(ScriptContext context) {
		super(context);
	}

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	protected static Class<?> superClass = Object.class;

	protected static ArrayList<SyntaxTree> fields = null;

	protected boolean has(Symbol tag, SyntaxTree node) {
		for (SyntaxTree sub : node) {
			if (sub.is(tag)) {
				return true;
			}
		}
		return false;
	}
}

class ClassDecl extends ClassDeclaration {
	ClassDecl(ScriptContext context) {
		super(context);
	}

	@Override
	public void acceptAsm(SyntaxTree node) {
		fields = new ArrayList<>();
		if (node.has(_super)) {
			superClass = node.get(_super).getClassType();
		}
		for (SyntaxTree n : node.get(_body)) {
			if (n.is(_FieldDecl)) {
				for (SyntaxTree field : n) {
					if (field.has(_expr)) {
						fields.add(field);
					}
				}
			}
		}

		SyntaxTree bodyNode = node.get(_body);
		if (!has(_Constructor, bodyNode)) {
			asm.mBuilder = asm.cBuilder.newConstructorBuilder(Opcodes.ACC_PUBLIC, new Class<?>[0]);
			asm.mBuilder.loadThis();
			asm.mBuilder.invokeConstructor(Type.getType(superClass), Method.getMethod("void <init> ()"));
			for (SyntaxTree field : fields) {
				asm.mBuilder.loadThis();
				asm.visit(field.get(_expr));
				asm.mBuilder.putField(asm.cBuilder.getTypeDesc(), field.getText(_name, null), Type.getType(field.get(_name).getClassType()));
			}
			asm.mBuilder.returnValue();
			asm.mBuilder.endMethod();
		}
		for (SyntaxTree n : node.get(_body)) {
			asm.visit(n);
		}
		superClass = Object.class;
		fields = null;
	}

	@Override
	public java.lang.reflect.Type acceptType(SyntaxTree node) {
		if (node.has(_super)) {
			SyntaxTree superNode = node.get(_super);
			java.lang.reflect.Type stype = checker.resolveType(superNode, Object.class);
			superNode.setType(stype);
		}
		if (node.has(_impl)) {
			SyntaxTree implNode = node.get(_impl);
			for (SyntaxTree i : implNode) {
				i.setType(checker.resolveType(i, Object.class));
			}
		}
		checker.visit(node.get(_body));
		return void.class;
	}

	@Override
	public Object acceptEval(SyntaxTree node) {
		eval.compiler.compileClassDecl(node);
		return Evaluator.empty;
	}
}

class Constructor extends ClassDeclaration {
	Constructor(ScriptContext context) {
		super(context);
	}

	@Override
	public void acceptAsm(SyntaxTree node) {
		// inConstructor = true;
		SyntaxTree args = node.get(_param);
		Class<?>[] paramClasses = new Class<?>[args.size()];
		for (int i = 0; i < args.size(); i++) {
			paramClasses[i] = args.get(i).getClassType();
		}
		asm.mBuilder = asm.cBuilder.newConstructorBuilder(Opcodes.ACC_PUBLIC, paramClasses);
		asm.mBuilder.enterScope();
		for (SyntaxTree arg : args) {
			asm.mBuilder.defineArgument(arg.getText(_name, null), arg.getClassType());
		}
		asm.mBuilder.loadThis();
		asm.mBuilder.invokeConstructor(Type.getType(superClass), Method.getMethod("void <init> ()"));
		for (SyntaxTree field : fields) {
			asm.mBuilder.loadThis();
			asm.visit(field.get(_expr));
			asm.mBuilder.putField(asm.cBuilder.getTypeDesc(), field.getText(_name, null), Type.getType(field.get(_name).getClassType()));
		}
		asm.visit(node.get(_body));
		asm.mBuilder.exitScope();
		asm.mBuilder.returnValue();
		asm.mBuilder.endMethod();
	}

	@Override
	public java.lang.reflect.Type acceptType(SyntaxTree node) {
		String name = "<init>";
		SyntaxTree bodyNode = node.get(_body, null);
		java.lang.reflect.Type returnType = void.class;
		java.lang.reflect.Type[] paramTypes = TypeChecker.EmptyTypes;
		SyntaxTree params = node.get(_param, null);
		if (node.has(_param)) {
			int c = 0;
			paramTypes = new java.lang.reflect.Type[params.size()];
			for (SyntaxTree p : params) {
				paramTypes[c] = checker.resolveType(p.get(_type, null), Object.class);
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
		FunctionBuilder f = checker.enterFunction(name);
		if (returnType != null) {
			f.setReturnType(returnType);
			checker.typed(node.get(_type), returnType);
		}
		if (node.has(_param)) {
			int c = 0;
			for (SyntaxTree sub : params) {
				String pname = sub.getText(_name, null);
				f.setVarType(pname, paramTypes[c]);
				checker.typed(sub, paramTypes[c]);
				c++;
			}
		}
		f.setParameterTypes(paramTypes);
		try {
			checker.visit(bodyNode);
		} catch (TypeCheckerException e) {
			node.set(_body, e.errorTree);
		}
		checker.exitFunction();
		if (f.getReturnType() == null) {
			f.setReturnType(void.class);
		}
		checker.typed(node.get(_param), f.getReturnType());
		return void.class;
	}
}

class FieldDecl extends ClassDeclaration {
	FieldDecl(ScriptContext context) {
		super(context);
	}

	@Override
	public void acceptAsm(SyntaxTree node) {
		for (SyntaxTree field : node) {
			asm.cBuilder.addField(Opcodes.ACC_PUBLIC, field.getText(_name, null), field.get(_name).getClassType(), null);
		}
	}

	@Override
	public java.lang.reflect.Type acceptType(SyntaxTree node) {
		java.lang.reflect.Type type = checker.resolveType(node.get(_type), null);
		SyntaxTree list = node.get(_list);
		for (SyntaxTree field : list) {
			java.lang.reflect.Type exprType = Object.class;
			if (field.has(_expr)) {
				exprType = checker.visit(field.get(_expr));
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
}

class MethodDecl extends ClassDeclaration {
	MethodDecl(ScriptContext context) {
		super(context);
	}

	@Override
	public void acceptAsm(SyntaxTree node) {
		SyntaxTree nameNode = node.get(_name);
		SyntaxTree args = node.get(_param);
		String name = nameNode.toText();
		Class<?> returnType = nameNode.getClassType();
		Class<?>[] paramTypes = new Class<?>[args.size()];
		for (int i = 0; i < paramTypes.length; i++) {
			paramTypes[i] = args.get(i).getClassType();
		}
		asm.mBuilder = asm.cBuilder.newMethodBuilder(Opcodes.ACC_PUBLIC, returnType, name, paramTypes);
		asm.mBuilder.enterScope();
		for (SyntaxTree arg : args) {
			asm.mBuilder.defineArgument(arg.getText(_name, null), arg.getClassType());
		}
		asm.visit(node.get(_body));
		asm.mBuilder.exitScope();
		if (returnType != void.class) {
			asm.visitDefaultValue(nameNode);
		}
		asm.mBuilder.returnValue();
		asm.mBuilder.endMethod();
	}

	@Override
	public java.lang.reflect.Type acceptType(SyntaxTree node) {
		return checker.typeFuncDecl(node);
	}
}