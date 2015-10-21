package konoha.script;

import java.lang.reflect.Type;

import nez.ast.Source;
import nez.ast.Symbol;
import nez.ast.Tree;
import nez.util.UList;

public class TypedTree extends Tree<TypedTree> {
	Type type;

	// Method resolvedMethod;

	TypedTree() {
		super();
	}

	public TypedTree(Symbol tag, Source source, long pos, int len, int size, Object value) {
		super(tag, source, pos, len, size > 0 ? new TypedTree[size] : null, value);
	}

	@Override
	protected TypedTree newInstance(Symbol tag, Source source, long pos, int len, int objectsize, Object value) {
		return new TypedTree(tag, source, pos, len, objectsize, value);
	}

	@Override
	protected void link(int n, Symbol label, Object child) {
		this.set(n, label, (TypedTree) child);
	}

	@Override
	public TypedTree newInstance(Symbol tag, int size, Object value) {
		return new TypedTree(tag, this.getSource(), this.getSourcePosition(), 0, size, value);
	}

	public TypedTree newConst(Type type, Object value) {
		TypedTree t = new TypedTree(CommonSymbols._Const, this.getSource(), this.getSourcePosition(), 0, 0, value);
		t.setConst(type, value);
		return t;
	}

	@Override
	protected TypedTree dupImpl() {
		TypedTree t = new TypedTree(this.getTag(), this.getSource(), this.getSourcePosition(), this.getLength(), this.size(), getValue());
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

	public void makeFlattenedList(TypedTree... trees) {
		UList<TypedTree> l = new UList<TypedTree>(new TypedTree[4]);
		for (TypedTree t : trees) {
			if (t.is(CommonSymbols._List)) {
				for (TypedTree sub : t) {
					l.add(sub);
				}
			} else {
				l.add(t);
			}
		}
		this.subTree = l.compactArray();
		this.labels = new Symbol[l.size()];
	}

	public void removeSubtree() {
		this.subTree = null;
		this.labels = EmptyLabels;
	}

	public void make(Symbol l1, TypedTree t1) {
		this.subTree = new TypedTree[] { t1 };
		this.labels = new Symbol[] { l1 };
	}

	public void make(Symbol l1, TypedTree t1, Symbol l2, TypedTree t2) {
		this.subTree = new TypedTree[] { t1, t2 };
		this.labels = new Symbol[] { l1, l2 };
	}

	public void make(Symbol l1, TypedTree t1, Symbol l2, TypedTree t2, Symbol l3, TypedTree t3) {
		this.subTree = new TypedTree[] { t1, t2, t3 };
		this.labels = new Symbol[] { l1, l2, l3 };
	}

	public void setFunctor(Functor f) {
		this.value = f;
	}

	public Functor getFunctor() {
		return (Functor) this.value;
	}

}
