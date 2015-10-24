package konoha.syntax;

import java.lang.reflect.Type;

import konoha.message.Message;
import konoha.script.TypedTree;

public class BigInteger extends SyntaxExtension {

	@Override
	public String getName() {
		return "Integer";
	}

	@Override
	public Type acceptType(TypedTree node) {
		String n = node.toText().replace("_", "");
		int radix = 10;
		if (n.endsWith("L") || n.endsWith("l")) {
			n = n.substring(0, n.length() - 1);
		}
		if (n.startsWith("0b") || n.startsWith("0B")) {
			n = n.substring(2);
			radix = 2;
		} else if (n.startsWith("0x") || n.startsWith("0X")) {
			n = n.substring(2);
			radix = 16;
		}
		try {
			java.math.BigInteger big = new java.math.BigInteger(n, radix);
			return node.setConst(java.math.BigInteger.class, big);
		} catch (NumberFormatException e) {
			checker.reportWarning(node, Message.InvalidNumberFormat, node.toText());
		}
		return node.setConst(java.math.BigInteger.class, java.math.BigInteger.ZERO);
	}

}
