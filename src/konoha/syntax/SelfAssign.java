package konoha.syntax;

import java.lang.reflect.Type;

import konoha.script.ScriptContext;
import konoha.script.SyntaxTree;
import nez.ast.Symbol;

public abstract class SelfAssign extends SyntaxExtension {

	public static void hack(ScriptContext context) {
		context.addSyntaxExtension(new AssignAdd(context));
		context.addSyntaxExtension(new AssignSub(context));
		context.addSyntaxExtension(new AssignMul(context));
		context.addSyntaxExtension(new AssignDiv(context));
		context.addSyntaxExtension(new AssignMod(context));

		context.addSyntaxExtension(new AssignLeftShift(context));
		context.addSyntaxExtension(new AssignRightShift(context));
		context.addSyntaxExtension(new AssignLogicalRightShift(context));

		context.addSyntaxExtension(new AssignBitwiseOr(context));
		context.addSyntaxExtension(new AssignBitwiseAnd(context));
		context.addSyntaxExtension(new AssignBitwiseXOr(context));
	}

	SelfAssign(ScriptContext context) {
		super(context);
	}

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	protected Type typeSelfAssignment(SyntaxTree node, Symbol optag) {
		SyntaxTree op = node.newInstance(optag, 0, null);
		op.sub(_left, node.get(_left).dup(), _right, node.get(_right));
		node.set(_right, op);
		node.setTag(_Assign);
		return checker.typeAssign(node);
	}

}

class AssignAdd extends SelfAssign {
	AssignAdd(ScriptContext context) {
		super(context);
	}

	@Override
	public Type acceptType(SyntaxTree node) {
		return this.typeSelfAssignment(node, _Add);
	}

}

class AssignSub extends SelfAssign {
	AssignSub(ScriptContext context) {
		super(context);
	}

	@Override
	public Type acceptType(SyntaxTree node) {
		return typeSelfAssignment(node, _Sub);
	}
}

class AssignMul extends SelfAssign {
	AssignMul(ScriptContext context) {
		super(context);
	}

	@Override
	public Type acceptType(SyntaxTree node) {
		return typeSelfAssignment(node, _Mul);
	}
}

class AssignDiv extends SelfAssign {
	AssignDiv(ScriptContext context) {
		super(context);
	}

	@Override
	public Type acceptType(SyntaxTree node) {
		return typeSelfAssignment(node, _Div);
	}
}

class AssignMod extends SelfAssign {
	AssignMod(ScriptContext context) {
		super(context);
	}

	@Override
	public Type acceptType(SyntaxTree node) {
		return typeSelfAssignment(node, _Mod);
	}
}

class AssignLeftShift extends SelfAssign {
	AssignLeftShift(ScriptContext context) {
		super(context);
	}

	@Override
	public Type acceptType(SyntaxTree node) {
		return typeSelfAssignment(node, _LeftShift);
	}
}

class AssignRightShift extends SelfAssign {
	AssignRightShift(ScriptContext context) {
		super(context);
	}

	@Override
	public Type acceptType(SyntaxTree node) {
		return typeSelfAssignment(node, _RightShift);
	}
}

class AssignLogicalRightShift extends SelfAssign {
	AssignLogicalRightShift(ScriptContext context) {
		super(context);
	}

	@Override
	public Type acceptType(SyntaxTree node) {
		return typeSelfAssignment(node, _LogicalRightShift);
	}
}

class AssignBitwiseAnd extends SelfAssign {
	AssignBitwiseAnd(ScriptContext context) {
		super(context);
	}

	@Override
	public Type acceptType(SyntaxTree node) {
		return typeSelfAssignment(node, _BitwiseAnd);
	}
}

class AssignBitwiseXOr extends SelfAssign {
	AssignBitwiseXOr(ScriptContext context) {
		super(context);
	}

	@Override
	public Type acceptType(SyntaxTree node) {
		return typeSelfAssignment(node, _BitwiseXor);
	}
}

class AssignBitwiseOr extends SelfAssign {
	AssignBitwiseOr(ScriptContext context) {
		super(context);
	}

	@Override
	public Type acceptType(SyntaxTree node) {
		return typeSelfAssignment(node, _BitwiseOr);
	}
}
