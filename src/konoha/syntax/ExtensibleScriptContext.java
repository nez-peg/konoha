package konoha.syntax;

import java.lang.reflect.Method;

import konoha.asm.ScriptCompiler;
import konoha.asm.ScriptCompilerAsm;
import konoha.script.Evaluator;
import konoha.script.Reflector;
import konoha.script.SyntaxTree;
import konoha.script.TypeChecker;
import konoha.script.TypeSystem;
import nez.Parser;

public class ExtensibleScriptContext {

	protected Parser parser;
	protected TypeSystem typeSystem;
	protected TypeChecker checker;
	protected Evaluator eval;
	protected ScriptCompiler compiler;
	protected ScriptCompilerAsm asm;

	protected Parser getParser() {
		return parser;
	}

	protected TypeSystem getTypeSystem() {
		return typeSystem;
	}

	protected TypeChecker getTypeChecker() {
		return checker;
	}

	protected Evaluator getEvaluator() {
		return eval;
	}

	protected ScriptCompilerAsm getScriptCompilerAsm() {
		return asm;
	}

	public void addSyntaxExtension(SyntaxExtension s) {
		if (isDefinedChecker(s)) {
			checker.add(s.getName(), s);
		}
		if (isDefinedDesugar(s)) {
			// checker.add(s.getName(), s);
		}
		if (isDefinedEvaluator(s)) {
			eval.add(s.getName(), s);
		}
		if (isDefinedAssembler(s)) {
			asm.add(s.getName(), s);
		}
	}

	private static boolean isDefinedChecker(SyntaxExtension s) {
		Method m = Reflector.getMethod(s, "acceptType", SyntaxTree.class);
		if (m != null) {
			return (m.getDeclaringClass() != SyntaxExtension.class);
		}
		return false;
	}

	private static boolean isDefinedDesugar(SyntaxExtension s) {
		Method m = Reflector.getMethod(s, "acceptDesugar", SyntaxTree.class);
		if (m != null) {
			return (m.getDeclaringClass() != SyntaxExtension.class);
		}
		return false;
	}

	private static boolean isDefinedEvaluator(SyntaxExtension s) {
		Method m = Reflector.getMethod(s, "acceptEval", SyntaxTree.class);
		if (m != null) {
			return (m.getDeclaringClass() != SyntaxExtension.class);
		}
		return false;
	}

	private static boolean isDefinedAssembler(SyntaxExtension s) {
		Method m = Reflector.getMethod(s, "acceptAsm", SyntaxTree.class);
		if (m != null) {
			return (m.getDeclaringClass() != SyntaxExtension.class);
		}
		return false;
	}

}
