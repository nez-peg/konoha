package konoha.script;

import java.io.IOException;
import java.lang.reflect.Type;

import konoha.main.ConsoleUtils;
import konoha.message.Message;
import konoha.syntax.ExtensibleScriptContext;
import nez.Parser;
import nez.io.SourceContext;

public class ScriptContext extends ExtensibleScriptContext {

	public ScriptContext(Parser parser) {
		this.parser = parser;
		this.typeSystem = new TypeSystem(this);
		this.checker = new KonohaChecker(this, getTypeSystem());
		this.eval = new Evaluator(this, getTypeSystem());
		this.checker.init();
		this.set("__lookup__", getTypeSystem());
	}

	public void setShellMode(boolean b) {
		this.getTypeSystem().setShellMode(b);
	}

	public final void load(String path) throws IOException {
		eval(SourceContext.newFileContext(path));
	}

	public final Object eval(String uri, int linenum, String script) {
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
		SyntaxTree node = (SyntaxTree) this.getParser().parse(source, new SyntaxTree());
		if (node == null) {
			log(source.getErrorMessage("error", Message.SyntaxError.toString()));
			this.found = ScriptContextError.SyntaxError;
			return Evaluator.empty; // nothing
		}
		if (!node.is(CommonSymbols._Source)) {
			node = node.newInstance(CommonSymbols._Source, node);
		}
		return evalSource(node);
	}

	public boolean enableASTDump = false;

	private Object evalSource(SyntaxTree node) {
		Object result = Evaluator.empty;
		for (int i = 0; i < node.size(); i++) {
			SyntaxTree sub = node.get(i);
			if (enableASTDump) {
				ConsoleUtils.println("[Parsed]");
				ConsoleUtils.println("    ", sub);
			}
			SyntaxTree typed = checker.checkAtTopLevel(sub);
			if (typed != sub) {
				node.set(i, typed);
			}
			if (enableASTDump) {
				ConsoleUtils.println("[Typed]");
				ConsoleUtils.println("    ", sub);
			}
			if (found == ScriptContextError.NoError) {
				result = eval.visit(typed);
				if (typed.getType() == void.class) {
					result = Evaluator.empty;
				}
			}
		}
		return found == ScriptContextError.NoError ? result : Evaluator.empty;
	}

	public Object get(String name) {
		GlobalVariable gv = this.getTypeSystem().getGlobalVariable(name);
		if (gv != null) {
			return Reflector.getStatic(gv.getField());
		}
		return null;
	}

	public void set(String name, Object value) {
		GlobalVariable gv = this.getTypeSystem().getGlobalVariable(name);
		if (gv == null) {
			Type type = Reflector.infer(value);
			gv = this.getTypeSystem().newGlobalVariable(type, name);
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

	private boolean verboseMode = true;

	public void setVerboseMode(boolean b) {
		verboseMode = b;
	}

	public void verbose(String fmt, Object... args) {
		if (verboseMode) {
			ConsoleUtils.begin(37);
			ConsoleUtils.println(String.format(fmt, args));
			ConsoleUtils.end();
		}
	}

}
