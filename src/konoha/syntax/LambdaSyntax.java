package konoha.syntax;

import java.lang.reflect.Type;

import konoha.script.KonohaFunctor;
import konoha.script.ScriptContext;
import konoha.script.SyntaxTree;

public abstract class LambdaSyntax extends SyntaxExtension {

	public static void hack(ScriptContext context) {
		context.addSyntaxExtension(new LambdaApply(context));
	}

	public LambdaSyntax(ScriptContext context) {
		super(context);
	}

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

}

class LambdaApply extends LambdaSyntax {

	public LambdaApply(ScriptContext context) {
		super(context);
	}

	@Override
	public Type acceptType(SyntaxTree node) {
		SyntaxTree lambda = node.get(_recv);
		checker.visit(lambda);
		if (checker.inFunction()) {
			SyntaxTree params = node.get(_param);
			for (int i = 0; i < params.size(); i++) {
				checker.enforceType(Object.class, params, i);
			}
			params.setTag(_Array);
			params.setType(Object[].class);
			node.sub(_name, lambda, _param, params);
			// if (params.size() > 0) {
			// node.sub(_name, node.get(_name), _param, params);
			// } else {
			// node.sub(_name, node.get(_name));
			// }
			checker.setFunctor(node, KonohaFunctor.getInvokeFunc());
			return lambda.get(_param).getType();
		} else {
			SyntaxTree params = node.get(_param);
			node.sub(_name, lambda);
			for (int i = 0; i < params.size(); i++) {
				checker.enforceType(Object.class, params, i);
				node.add(_expr, params.get(i));
			}
			checker.setFunctor(node, KonohaFunctor.getInvokeFunc());
			return lambda.get(_param).getType();
		}
	}

}
