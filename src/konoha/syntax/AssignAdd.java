package konoha.syntax;

import java.lang.reflect.Type;

import konoha.script.SyntaxTree;

public class AssignAdd extends SelfAssign {

	@Override
	public Type acceptType(SyntaxTree node) {
		return this.typeSelfAssignment(node, _Add);
	}

}
