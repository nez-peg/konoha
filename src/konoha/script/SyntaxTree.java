package konoha.script;

import java.lang.reflect.Type;

import nez.ast.Source;
import nez.ast.Symbol;
import nez.ast.Tree;
import nez.util.UList;

public class SyntaxTree extends Tree<SyntaxTree> {
	Type type;

	// Method resolvedMethod;

	SyntaxTree() {
		super();
	}

	public SyntaxTree(Symbol tag, Source source, long pos, int len, int size, Object value) {
		super(tag, source, pos, len, size > 0 ? new SyntaxTree[size] : null, value);
	}

	@Override
	protected SyntaxTree newInstance(Symbol tag, Source source, long pos, int len, int objectsize, Object value) {
		return new SyntaxTree(tag, source, pos, len, objectsize, value);
	}

	@Override
	protected void link(int n, Symbol label, Object child) {
		this.set(n, label, (SyntaxTree) child);
	}

	@Override
	public SyntaxTree newInstance(Symbol tag, int size, Object value) {
		return new SyntaxTree(tag, this.getSource(), this.getSourcePosition(), 0, size, value);
	}

	public SyntaxTree newInstance(Symbol tag, SyntaxTree... sub) {
		SyntaxTree t = new SyntaxTree(tag, this.getSource(), this.getSourcePosition(), 0, 0, null);
		t.makeFlattenedList(sub);
		return t;
	}

	public SyntaxTree newInstance(Symbol tag, Symbol l1, SyntaxTree t1) {
		SyntaxTree t = new SyntaxTree(tag, this.getSource(), this.getSourcePosition(), 0, 0, null);
		t.sub(l1, t1);
		return t;
	}

	public SyntaxTree newConst(Type type, Object value) {
		SyntaxTree t = new SyntaxTree(CommonSymbols._Const, this.getSource(), this.getSourcePosition(), 0, 0, value);
		t.setConst(type, value);
		return t;
	}

	@Override
	protected SyntaxTree dupImpl() {
		SyntaxTree t = new SyntaxTree(this.getTag(), this.getSource(), this.getSourcePosition(), this.getLength(), this.size(), getValue());
		t.type = this.type;
		return t;
	}

	// @Override
	// protected RuntimeException newNoSuchLabel(Symbol label) {
	// return new TypeCheckerException(this, Message.SyntaxError_Expected,
	// label);
	// }

	// public void changed(Symbol tag, int n, Object v) {
	// this.tag = tag;
	// this.subTree = new TypedTree[n];
	// this.value = v;
	// }

	public final Class<?> getClassType() {
		return Lang.toClassType(this.type);
	}

	public Type getType() {
		return this.type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public Type setConst(Type type, Object value) {
		this.setTag(CommonSymbols._Const);
		this.setValue(value);
		this.type = type;
		return this.type;
	}

	@Override
	protected void stringfy(String indent, Symbol label, StringBuilder sb) {
		super.stringfy(indent, label, sb);
		if (type != null) {
			sb.append(" :");
			sb.append(Lang.name(type));
		}
	}

	public void done() {
		this.setTag(CommonSymbols._Empty);
	}

	/* Tree Manipulation */

	public void makeFlattenedList(SyntaxTree... trees) {
		UList<SyntaxTree> l = new UList<SyntaxTree>(new SyntaxTree[4]);
		for (SyntaxTree t : trees) {
			if (t.is(CommonSymbols._List)) {
				for (SyntaxTree sub : t) {
					l.add(sub);
				}
			} else {
				l.add(t);
			}
		}
		this.subTree = l.compactArray();
		this.labels = new Symbol[l.size()];
	}

	public void add(Symbol l, SyntaxTree t) {
		SyntaxTree[] newTree = new SyntaxTree[labels.length + 1];
		Symbol[] newLabels = new Symbol[labels.length + 1];
		if (subTree != null) {
			System.arraycopy(subTree, 0, newTree, 0, labels.length);
		}
		System.arraycopy(labels, 0, newLabels, 0, labels.length);
		newLabels[labels.length] = l;
		newTree[labels.length] = t;
		this.subTree = newTree;
		this.labels = newLabels;
	}

	public void sub() {
		this.subTree = null;
		this.labels = EmptyLabels;
	}

	public void sub(Symbol l1, SyntaxTree t1) {
		this.subTree = new SyntaxTree[] { t1 };
		this.labels = new Symbol[] { l1 };
	}

	public void sub(Symbol l1, SyntaxTree t1, Symbol l2, SyntaxTree t2) {
		this.subTree = new SyntaxTree[] { t1, t2 };
		this.labels = new Symbol[] { l1, l2 };
	}

	public void sub(Symbol l1, SyntaxTree t1, Symbol l2, SyntaxTree t2, Symbol l3, SyntaxTree t3) {
		this.subTree = new SyntaxTree[] { t1, t2, t3 };
		this.labels = new Symbol[] { l1, l2, l3 };
	}

	public void setFunctor(Functor f) {
		this.value = f;
	}

	public Functor getFunctor() {
		return (Functor) this.value;
	}

}
