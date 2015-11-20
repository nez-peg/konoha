package konoha.syntax;

import java.lang.reflect.Type;

import konoha.script.ScriptContext;
import konoha.script.SyntaxTree;

import org.objectweb.asm.Opcodes;

public abstract class InterfaceDeclaration extends SyntaxExtension {

	public static void hack(ScriptContext context) {
		context.addSyntaxExtension(new InterfaceDecl(context));
		context.addSyntaxExtension(new InterfaceMethodDecl(context));
		context.addSyntaxExtension(new InterfaceFieldDecl(context));
	}

	public InterfaceDeclaration(ScriptContext context) {
		super(context);
	}

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}
}

class InterfaceDecl extends InterfaceDeclaration {

	public InterfaceDecl(ScriptContext context) {
		super(context);
	}

	@Override
	public Type acceptType(SyntaxTree node) {
		for (SyntaxTree n : node.get(_body)) {
			checker.visit(n);
		}
		return void.class;
	}

	@Override
	public Object acceptEval(SyntaxTree node) {
		eval.compiler.compileInterface(node);
		return null;
	}

	@Override
	public void acceptAsm(SyntaxTree node) {
		for (SyntaxTree n : node.get(_body)) {
			asm.visit(n);
		}
	}
}

class InterfaceMethodDecl extends InterfaceDeclaration {

	public InterfaceMethodDecl(ScriptContext context) {
		super(context);
	}

	@Override
	public Type acceptType(SyntaxTree node) {
		Type t = checker.resolveType(node.get(_type), Object.class);
		node.get(_name).setType(t);
		SyntaxTree params = node.get(_param);
		for (SyntaxTree param : params) {
			param.setType(checker.resolveType(param.get(_type), Object.class));
		}
		return void.class;
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
		asm.mBuilder = asm.cBuilder.newMethodBuilder(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, returnType, name, paramTypes);
		asm.mBuilder.endMethod();
	}
}

class InterfaceFieldDecl extends InterfaceDeclaration {

	public InterfaceFieldDecl(ScriptContext context) {
		super(context);
	}

	@Override
	public Type acceptType(SyntaxTree node) {
		return checker.typeVarDecl(node);
	}

	@Override
	public void acceptAsm(SyntaxTree node) {
		SyntaxTree nameNode = node.get(_name);
		Object value = eval.visit(node.get(_expr));
		asm.cBuilder.addField(Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, nameNode.toText(), nameNode.getClassType(), value);
	}

}