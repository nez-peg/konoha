package konoha.script;

import konoha.message.Message;

public class TypeCheckerException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	public SyntaxTree errorTree;

	TypeCheckerException(SyntaxTree node, Message fmt, Object... args) {
		this.errorTree = newErrorTree(node, fmt.toString(), args);
	}

	TypeCheckerException(SyntaxTree node, String fmt, Object... args) {
		this.errorTree = newErrorTree(node, fmt, args);
	}

	private SyntaxTree newErrorTree(SyntaxTree node, String fmt, Object... args) {
		SyntaxTree newnode = node.newInstance(CommonSymbols._Functor, 1, null);
		String msg = node.formatSourceMessage("error", String.format(fmt, args));
		newnode.set(0, CommonSymbols._msg, node.newConst(String.class, msg));
		newnode.setFunctor(KonohaFunctor.getThrowErrorFunctor());
		return newnode;
	}

	public final SyntaxTree getErrorTree() {
		return this.errorTree;
	}

	@Override
	public final String getMessage() {
		return this.errorTree.getText(CommonSymbols._msg, "error");
	}

}
