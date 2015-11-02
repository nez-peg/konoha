package konoha.dynamic;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Type;

import konoha.script.Lang;
import konoha.script.TypeSystem;

public class ArithmeticSite extends OperatorSite {

	public static CallSite bootstrap(MethodHandles.Lookup lookup, String methodName, MethodType type) throws Throwable {
		return new ArithmeticSite(lookup, methodName, type);
	}

	private ArithmeticSite(Lookup lookup, String targetName, MethodType methodType) {
		super(lookup, targetName, methodType);
	}

	private ArithmeticSite(TypeSystem ts, String name, Type returnType, Type[] paramTypes) {
		super(ts, name, returnType, paramTypes);
	}

	public ArithmeticSite(TypeSystem ts, String name) {
		super(ts, name, Object.class, Lang.EmptyTypes);
	}

	@Override
	public DynamicSite op(String name, Type returnType, Type[] paramTypes) {
		return new ArithmeticSite(this.typeSystem, name, returnType, paramTypes);
	}

	@Override
	protected void precheckParamTypes(Type[] a) {
		Type t1 = Lang.toUnbox(a[0]);
		Type t2 = Lang.toUnbox(a[1]);
		if (t1 == t2) {
			unify(a, t1);
		} else if (t1 == Object.class || t2 == Object.class) {
			unify(a, Object.class);
		} else if (t1 instanceof Class<?> && t2 instanceof Class<?>) {
			Class<?> u = this.typeSystem.unify((Class<?>) t1, (Class<?>) t2);
			if (u != null) {
				unify(a, alias(u));
			}
		}
	}

}
