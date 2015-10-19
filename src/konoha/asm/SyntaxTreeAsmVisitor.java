package konoha.asm;

import konoha.script.TypedTree;

public interface SyntaxTreeAsmVisitor {
	public void acceptAsm(TypedTree node);

}
