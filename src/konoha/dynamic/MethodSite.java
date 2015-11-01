package konoha.dynamic;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Type;

import konoha.script.Functor;
import konoha.script.TypeMatcher;
import konoha.script.TypeSystem;

public class MethodSite extends DynamicSite {

	public static CallSite bootstrap(MethodHandles.Lookup lookup, String methodName, MethodType type) throws Throwable {
		return new MethodSite(lookup, methodName, type);
	}

	private MethodSite(Lookup lookup, String targetName, MethodType methodType) {
		super(lookup, targetName, methodType, true);
	}

	public MethodSite(TypeSystem ts, String name, Type returnType, Type[] paramTypes) {
		super(ts, name, returnType, paramTypes, true);
	}

	@Override
	public DynamicSite op(String name, Type returnType, Type[] paramTypes) {
		return new MethodSite(this.typeSystem, name, returnType, paramTypes);
	}

	@Override
	public Functor lookup(TypeMatcher m, String name, Type[] a) {
		m.init(a[0]);
		return this.typeSystem.getMethod(m, a[0], name, a);
	}

}
