package konoha.syntax;

import java.lang.reflect.Method;

import konoha.script.Reflector;
import konoha.script.ScriptEvaluator;
import konoha.script.TypeChecker;
import konoha.script.TypeSystem;
import konoha.script.TypedTree;
import nez.Parser;

public class ScriptContextHacks {

	protected Parser parser;
	protected TypeSystem typeSystem;
	protected TypeChecker checker;
	protected ScriptEvaluator eval;

	protected Parser getParser() {
		return parser;
	}

	protected void setParser(Parser parser) {
		this.parser = parser;
	}

	protected TypeSystem getTypeSystem() {
		return typeSystem;
	}

	protected TypeChecker getTypeChecker() {
		return checker;
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
		// if (isDefinedAssembler(s)) {
		// asm.add(s.getName(), s);
		// }
	}

	private static boolean isDefinedChecker(SyntaxExtension s) {
		Method m = Reflector.getMethod(s, "acceptType", TypedTree.class);
		if (m != null) {
			return (m.getDeclaringClass() != s.getClass());
		}
		return false;
	}

	private static boolean isDefinedDesugar(SyntaxExtension s) {
		Method m = Reflector.getMethod(s, "acceptDesugar", TypedTree.class);
		if (m != null) {
			return (m.getDeclaringClass() != s.getClass());
		}
		return false;
	}

	private static boolean isDefinedEvaluator(SyntaxExtension s) {
		Method m = Reflector.getMethod(s, "acceptEval", TypedTree.class);
		if (m != null) {
			return (m.getDeclaringClass() != s.getClass());
		}
		return false;
	}

	private static boolean isDefinedAssembler(SyntaxExtension s) {
		Method m = Reflector.getMethod(s, "acceptAsm", TypedTree.class);
		if (m != null) {
			return (m.getDeclaringClass() != s.getClass());
		}
		return false;
	}

}
