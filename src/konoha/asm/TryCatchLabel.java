package konoha.asm;

import konoha.script.SyntaxTree;

import org.objectweb.asm.Label;

public class TryCatchLabel {
	private final Label startLabel;
	private final Label endLabel;
	private final Label finallyLabel;

	VarEntry retAddrEntry;
	SyntaxTree finallyNode;

	/**
	 * 
	 * @param startLabel
	 * @param endLabel
	 * @param finallyLabel
	 *            if not found finally, null
	 */
	TryCatchLabel(Label startLabel, Label endLabel, Label finallyLabel) {
		this.startLabel = startLabel;
		this.endLabel = endLabel;
		this.finallyLabel = finallyLabel;
	}

	public Label getStartLabel() {
		return this.startLabel;
	}

	public Label getEndLabel() {
		return this.endLabel;
	}

	/**
	 * 
	 * @return may be null
	 */
	public Label getFinallyLabel() {
		return this.finallyLabel;
	}

	public SyntaxTree getFinallyNode() {
		return this.finallyNode;
	}

	public void setFinallyNode(SyntaxTree finallyNode) {
		this.finallyNode = finallyNode;
	}
}