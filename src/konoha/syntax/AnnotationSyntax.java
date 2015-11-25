package konoha.syntax;

import java.lang.reflect.Type;

import konoha.script.ScriptContext;
import konoha.script.SyntaxTree;
import nez.ast.Symbol;

public abstract class AnnotationSyntax extends SyntaxExtension {

	public static void hack(ScriptContext context) {
		context.addSyntaxExtension(new AnnotationDecl(context));
	}

	public AnnotationSyntax(ScriptContext context) {
		super(context);
	}

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	protected boolean has(Symbol tag, SyntaxTree node) {
		for (SyntaxTree sub : node) {
			if (sub.is(tag)) {
				return true;
			}
		}
		return false;
	}
}

class AnnotationDecl extends AnnotationSyntax {

	public AnnotationDecl(ScriptContext context) {
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
		eval.compiler.compileAnnotation(node);
		return null;
	}

	@Override
	public void acceptAsm(SyntaxTree node) {
		for (SyntaxTree n : node.get(_body)) {
			asm.visit(n);
		}
	}
}