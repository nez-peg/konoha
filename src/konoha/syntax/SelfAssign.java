package konoha.syntax;

import java.lang.reflect.Type;

import konoha.script.TypedTree;
import nez.ast.Symbol;

public abstract class SelfAssign extends SyntaxExtension {

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	protected Type typeSelfAssignment(TypedTree node, Symbol optag) {
		TypedTree op = node.newInstance(optag, 0, null);
		op.sub(_left, node.get(_left).dup(), _right, node.get(_right));
		node.set(_right, op);
		node.setTag(_Assign);
		return checker.typeAssign(node);
	}
}
