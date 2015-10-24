package konoha.syntax;

import java.lang.reflect.Type;

import konoha.script.TypedTree;

public class AssignAdd extends SelfAssign {

	@Override
	public Type acceptType(TypedTree node) {
		return this.typeSelfAssignment(node, _Add);
	}

}
