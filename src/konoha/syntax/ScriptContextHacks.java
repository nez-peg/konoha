package konoha.syntax;

import konoha.script.ScriptEvaluator;
import konoha.script.TypeChecker;
import konoha.script.TypeSystem;
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

}
