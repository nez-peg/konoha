package konoha.dynamic;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Type;

import konoha.script.Functor;
import konoha.script.Lang;
import konoha.script.TypeMatcher;
import konoha.script.TypeSystem;

public class UnarySite extends DynamicSite {
	public static CallSite bootstrap(MethodHandles.Lookup lookup, String methodName, MethodType type) throws Throwable {
		return new UnarySite(lookup, methodName, type);
	}

	private UnarySite(Lookup lookup, String targetName, MethodType methodType) {
		super(lookup, targetName, methodType, true);
	}

	private UnarySite(TypeSystem ts, String name, Type returnType, Type[] paramTypes) {
		super(ts, name, returnType, paramTypes, true);
	}

	public UnarySite(TypeSystem ts, String name) {
		super(ts, name, Object.class, Lang.EmptyTypes, true);
	}

	@Override
	public DynamicSite op(String name, Type returnType, Type[] paramTypes) {
		return new UnarySite(this.typeSystem, name, returnType, paramTypes);
	}

	@Override
	public Functor lookup(TypeMatcher m, String name, Type[] a) {
		m.init(a[0]);
		return this.typeSystem.getMethod(m, a[0], name, a);
	}

}
