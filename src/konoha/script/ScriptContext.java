package konoha.script;

import java.io.IOException;
import java.lang.reflect.Type;

import konoha.main.ConsoleUtils;
import konoha.message.Message;
import nez.Parser;
import nez.io.SourceContext;

public class ScriptContext {
	private Parser parser;
	private TypeSystem typeSystem;
	private TypeChecker typechecker;
	private Interpreter interpreter;

	public ScriptContext(Parser parser) {
		this.parser = parser;
		this.typeSystem = new TypeSystem(this);
		this.typechecker = new TypeChecker(this, typeSystem);
		this.interpreter = new Interpreter(this, typeSystem);
		this.set("__lookup__", typeSystem);
		// new TypeChecker2();
	}

	public void setShellMode(boolean b) {
		this.typeSystem.setShellMode(b);
	}

	public void setVerboseMode(boolean b) {
		// this.typeSystem.setVerboseMode(b);
	}

	public final void load(String path) throws IOException {
		eval(SourceContext.newFileContext(path));
	}

	public final Object eval2(String uri, int linenum, String script) {
		return eval(SourceContext.newStringContext(uri, linenum, script));
	}

	ScriptContextError found = ScriptContextError.NoError;

	public final ScriptContextError getError() {
		return found;
	}

	public void found(ScriptContextError e) {
		found = e;
	}

	public final Object eval(SourceContext source) {
		this.found = ScriptContextError.NoError;
		TypedTree node = (TypedTree) this.parser.parse(source, new TypedTree());
		if (node == null) {
			log(source.getErrorMessage("error", Message.SyntaxError.toString()));
			this.found = ScriptContextError.SyntaxError;
			return Interpreter.empty; // nothing
		}
		if (!node.is(CommonSymbols._Source)) {
			node = node.newInstance(CommonSymbols._Source, node);
		}
		return evalSource(node);
	}

	public boolean enableASTDump = false;

	private Object evalSource(TypedTree node) {
		Object result = Interpreter.empty;
		for (int i = 0; i < node.size(); i++) {
			TypedTree sub = node.get(i);
			if (enableASTDump) {
				ConsoleUtils.println("[Parsed]");
				ConsoleUtils.println("    ", sub);
			}
			TypedTree typed = typechecker.checkAtTopLevel(sub);
			if (typed != sub) {
				node.set(i, typed);
			}
			if (enableASTDump) {
				ConsoleUtils.println("[Typed]");
				ConsoleUtils.println("    ", sub);
			}
			if (found == ScriptContextError.NoError) {
				result = interpreter.visit(typed);
				if (typed.getType() == void.class) {
					result = Interpreter.empty;
				}
			}
		}
		return found == ScriptContextError.NoError ? result : Interpreter.empty;
	}

	public Object get(String name) {
		GlobalVariable gv = this.typeSystem.getGlobalVariable(name);
		if (gv != null) {
			return Reflector.getStatic(gv.getField());
		}
		return null;
	}

	public void set(String name, Object value) {
		GlobalVariable gv = this.typeSystem.getGlobalVariable(name);
		if (gv == null) {
			Type type = Reflector.infer(value);
			gv = this.typeSystem.newGlobalVariable(type, name);
		}
		Reflector.setStatic(gv.getField(), value);
	}

	public final void println(Object o) {
		ConsoleUtils.println(o);
	}

	public void log(String msg) {
		int c = msg.indexOf("[error]") > 0 ? 31 : 35;
		ConsoleUtils.begin(c);
		ConsoleUtils.println(msg);
		ConsoleUtils.end();
	}

}
