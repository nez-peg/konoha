package konoha.script;

import nez.ast.TreeVisitor;

public class ScriptGenerator extends TreeVisitor implements CommonSymbols {

	private ScriptBuilder builder;

	public ScriptGenerator() {
		super(SyntaxTree.class);
		this.builder = new ScriptBuilder();
		// this.context = context;
	}

	public void generate(SyntaxTree node) {

	}

	public void visit(SyntaxTree node) {
		visit("visit", node);
	}

	/* TopLevel */

	public void visitSource(SyntaxTree node) {
		for (SyntaxTree sub : node) {
			visit(sub);
		}
	}

	public void visitImport(SyntaxTree node) {

	}

	private void join(StringBuilder sb, SyntaxTree node) {
		SyntaxTree prefix = node.get(_prefix);
		if (prefix.size() == 2) {
			join(sb, prefix);
		} else {
			sb.append(prefix.toText());
		}
		sb.append(".").append(node.getText(_name, null));
	}

	/* FuncDecl */

	public void visitFuncDecl(SyntaxTree node) {
		// String name = node.getText(_name, null);
		// TypedTree bodyNode = node.get(_body, null);
	}

	public void visitReturn(SyntaxTree node) {
		this.builder.push("return");
		this.visit(node.get(_expr));
	}

	/* Statement */

	public void visitBlock(SyntaxTree node) {
		this.builder.openBlock("{");
		this.visitStatementList(node);
		this.builder.closeBlock("}");
	}

	public void visitStatementList(SyntaxTree node) {
		for (SyntaxTree sub : node) {
			this.visit(sub);
		}
	}

	public void visitStatement(SyntaxTree node) {
		if (node.is(_Block)) {
			visitBlock(node);
		} else {
			this.builder.openBlock("{");
			visit(node);
			this.builder.closeBlock("}");
		}
	}

	public void visitIf(SyntaxTree node) {
		this.builder.beginStatement("if");
		this.builder.push("(");
		this.visit(node.get(_cond));
		this.builder.push(")");
		this.visitStatement(node.get(_then));
		this.builder.endStatement("");
		if (node.has(_else)) {
			this.builder.beginStatement("else");
			this.visitStatement(node.get(_else));
			this.builder.endStatement("");
		}
	}

	public void visitConditional(SyntaxTree node) {
		this.visit(node.get(_cond));
		this.builder.push("?");
		this.visit(node.get(_then));
		this.builder.push(":");
		this.visit(node.get(_else));
	}

	public void visitWhile(SyntaxTree node) {
		this.builder.beginStatement("while");
		this.builder.push("(");
		this.visit(node.get(_cond));
		this.builder.push(")");
		this.visitStatement(node.get(_then));
	}

	public void visitContinue(SyntaxTree node) {
		this.builder.beginStatement("continue");
		this.builder.endStatement(";");
	}

	public void visitBreak(SyntaxTree node) {
		this.builder.beginStatement("break");
		this.builder.endStatement(";");
	}

	public void visitFor(SyntaxTree node) {
		// if (inFunction()) {
		// this.function.beginLocalVarScope();
		// }
		// if (node.has(_init)) {
		// type(node.get(_init));
		// }
		// if (node.has(_cond)) {
		// this.enforceType(boolean.class, node, _cond);
		// }
		// if (node.has(_iter)) {
		// type(node.get(_iter));
		// }
		// type(node.get(_body));
		// if (inFunction()) {
		// this.function.endLocalVarScope();
		// }
		// return void.class;
	}

	public void visitForEach(SyntaxTree node) {
		// Type req_t = null;
		// if (node.has(_type)) {
		// req_t = this.typeSystem.resolveType(node.get(_type), null);
		// }
		// String name = node.getText(_name, "");
		// req_t = typeIterator(req_t, node.get(_iter));
		// if (inFunction()) {
		// this.function.beginLocalVarScope();
		// }
		// this.function.setVarType(name, req_t);
		// type(node.get(_body));
		// if (inFunction()) {
		// this.function.endLocalVarScope();
		// }
		// return void.class;
	}

	public void visitVarDecl(SyntaxTree node) {
	}

	/* StatementExpression */
	public void visitExpression(SyntaxTree node) {
		this.builder.beginStatement("");
		this.visit(node.get(_expr));
		this.builder.beginStatement("");
	}

	/* Expression */

	public void visitName(SyntaxTree node) {
		String name = node.toText();
		this.builder.push(name);
	}

	public void visitAssign(SyntaxTree node) {
		this.visit(node.get(_left));
		this.builder.push("=");
		this.visit(node.get(_right));
	}

	/* Expression */

	public void visitCast(SyntaxTree node) {
		this.visit(node.get(_expr));
	}

	public void visitField(SyntaxTree node) {
		this.visit(node.get(_left));
		this.builder.write(",");
		this.visit(node.get(_right));
	}

	public void visitIndexer(SyntaxTree node) {

	}

	public void visitApply(SyntaxTree node) {
	}

	// private Type[] visitArguments(TypedTree args) {
	// Type[] types = new Type[args.size()];
	// for (int i = 0; i < args.size(); i++) {
	// types[i] = type(args.get(i));
	// }
	// return types;
	// }

	public void visitMethodApply(SyntaxTree node) {

	}

	private void visitUnary(SyntaxTree node, String name) {
		this.builder.write(name);
		this.visit(node.get(_expr));
	}

	private void visitBinary(SyntaxTree node, String name) {
		this.visit(node.get(_left));
		this.builder.push(name);
		this.visit(node.get(_right));
	}

	public void visitAnd(SyntaxTree node) {
		this.visitBinary(node, "and");
	}

	public void visitOr(SyntaxTree node) {
		this.visitBinary(node, "or");
	}

	public void visitNot(SyntaxTree node) {
		this.visitUnary(node, "not ");
	}

	public void visitAdd(SyntaxTree node) {
		this.visitBinary(node, "+");
	}

	public void visitSub(SyntaxTree node) {
		this.visitBinary(node, "-");
	}

	public void visitMul(SyntaxTree node) {
		this.visitBinary(node, "*");
	}

	public void visitDiv(SyntaxTree node) {
		this.visitBinary(node, "/");
	}

	public void visitPlus(SyntaxTree node) {
		this.visitUnary(node, "+");
	}

	public void visitMinus(SyntaxTree node) {
		this.visitUnary(node, "-");
	}

	public void visitEquals(SyntaxTree node) {
		this.visitBinary(node, "==");
	}

	public void visitNotEquals(SyntaxTree node) {
		this.visitBinary(node, "!=");
	}

	public void visitLessThan(SyntaxTree node) {
		this.visitBinary(node, "<");
	}

	public void visitLessThanEquals(SyntaxTree node) {
		this.visitBinary(node, "<=");
	}

	public void visitGreaterThan(SyntaxTree node) {
		this.visitBinary(node, ">");
	}

	public void visitGreaterThanEquals(SyntaxTree node) {
		this.visitBinary(node, ">=");
	}

	public void visitLeftShift(SyntaxTree node) {
		this.visitBinary(node, "<<");
	}

	public void visitRightShift(SyntaxTree node) {
		this.visitBinary(node, ">>");
	}

	public void visitLogicalRightShift(SyntaxTree node) {
		this.visitBinary(node, ">>>");
	}

	public void visitBitwiseAnd(SyntaxTree node) {
		this.visitBinary(node, "&");
	}

	public void visitBitwiseOr(SyntaxTree node) {
		this.visitBinary(node, "|");
	}

	public void visitBitwiseXor(SyntaxTree node) {
		this.visitBinary(node, "^");
	}

	public void visitCompl(SyntaxTree node) {
		this.visitUnary(node, "~");
	}

	public void visitNull(SyntaxTree node) {
		this.builder.push("None");
	}

	public void visitTrue(SyntaxTree node) {
		this.builder.push("True");
	}

	public void visitFalse(SyntaxTree node) {
		this.builder.push("False");
	}

	public void visitShort(SyntaxTree node) {
		// return typeInteger(node);
	}

	public void visitInteger(SyntaxTree node) {
		// return node.setConst(int.class, 0);
	}

	public void visitLong(SyntaxTree node) {

	}

	public void visitFloat(SyntaxTree node) {

	}

	public void visitDouble(SyntaxTree node) {

	}

	public void visitText(SyntaxTree node) {
		// return node.setConst(String.class, node.toText());
	}

	public void visitString(SyntaxTree node) {
		// String t = node.toText();
		// return node.setConst(String.class, StringUtils.unquoteString(t));
	}

	public void visitCharacter(SyntaxTree node) {

	}

	public void visitInterpolation(SyntaxTree node) {

	}

	/* array */

	public void visitArray(SyntaxTree node) {
	}

	// Syntax Sugar

	public void visitAssignAdd(SyntaxTree node) {
		this.visitBinary(node, "+=");
	}

	public void visitAssignSub(SyntaxTree node) {
		this.visitBinary(node, "-=");
	}

	public void visitAssignMul(SyntaxTree node) {
		this.visitBinary(node, "*=");
	}

	public void visitAssignDiv(SyntaxTree node) {
		this.visitBinary(node, "/=");
	}

	public void visitAssignMod(SyntaxTree node) {
		this.visitBinary(node, "%=");
	}

	public void visitAssignLeftShift(SyntaxTree node) {
		this.visitBinary(node, "<<=");
	}

	public void visitAssignRightShift(SyntaxTree node) {
		this.visitBinary(node, ">>=");
	}

	public void visitAssignLogicalRightShift(SyntaxTree node) {
		this.visitBinary(node, ">>>=");
	}

	public void visitAssignBitwiseAnd(SyntaxTree node) {
		this.visitBinary(node, "&=");
	}

	public void visitAssignBitwiseXOr(SyntaxTree node) {
		this.visitBinary(node, "^=");
	}

	public void visitAssignBitwiseOr(SyntaxTree node) {
		this.visitBinary(node, "|=");
	}

}
