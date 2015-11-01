package konoha.dynamic;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Type;

import konoha.script.Lang;
import konoha.script.TypeSystem;

public class ComparatorSite extends OperatorSite {

	public static CallSite bootstrap(MethodHandles.Lookup lookup, String methodName, MethodType type) throws Throwable {
		return new ComparatorSite(lookup, methodName, type);
	}

	private ComparatorSite(Lookup lookup, String targetName, MethodType methodType) {
		super(lookup, targetName, methodType);
	}

	private ComparatorSite(TypeSystem ts, String name, Type returnType, Type[] paramTypes) {
		super(ts, name, returnType, paramTypes);
	}

	public ComparatorSite(TypeSystem ts, String name) {
		super(ts, name, boolean.class, Lang.EmptyTypes);
	}

	@Override
	public DynamicSite op(String name, Type returnType, Type[] paramTypes) {
		return new ComparatorSite(this.typeSystem, name, returnType, paramTypes);
	}

	@Override
	protected void precheckParamTypes(Type[] a) {
		Type t1 = Lang.toUnbox(a[0]);
		Type t2 = Lang.toUnbox(a[1]);
		if (t1 == Object.class || t2 == Object.class) {
			unify(a, Object.class);
		} else if (t1 instanceof Class<?> && t2 instanceof Class<?>) {
			Class<?> u = this.typeSystem.unify((Class<?>) t1, (Class<?>) t2);
			if (u != null) {
				unify(a, alias(u));
			}
		}
	}

}
