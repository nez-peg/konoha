package konoha.dynamic;

import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Type;

import konoha.script.Functor;
import konoha.script.Lang;
import konoha.script.TypeMatcher;
import konoha.script.TypeSystem;

public abstract class OperatorSite extends DynamicSite {

	protected OperatorSite(Lookup lookup, String targetName, MethodType methodType) {
		super(lookup, targetName, methodType, true);
	}

	protected OperatorSite(TypeSystem ts, String name, Type returnType, Type[] paramTypes) {
		super(ts, name, returnType, paramTypes, true);
	}

	@Override
	public Functor lookup(TypeMatcher m, String name, Type[] a) {
		precheckParamTypes(a);
		m.init(a[0]);
		return this.typeSystem.getMethod(m, a[0], name, a);
	}

	protected void precheckParamTypes(Type[] a) {
		Type t1 = Lang.toUnbox(a[0]);
		Type t2 = Lang.toUnbox(a[1]);
		if (t1 == t2) {
			unify(a, t1);
		} else if ((t1 == String.class || t2 == String.class)) {
			unify(a, String.class);
		} else if (t1 == Object.class || t2 == Object.class) {
			unify(a, Object.class);
		} else if (t1 instanceof Class<?> && t2 instanceof Class<?>) {
			Class<?> u = this.typeSystem.unify((Class<?>) t1, (Class<?>) t2);
			if (u != null) {
				unify(a, alias(u));
			}
		}
	}

	protected final void unify(Type[] a, Type t) {
		a[0] = t;
		a[1] = t;
	}

	protected final Class<?> alias(Class<?> t) {
		if (t == float.class) {
			return double.class;
		}
		if (t == short.class) {
			return int.class;
		}
		return t;
	}

}
