package konoha.dynamic;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Type;

import konoha.script.Functor;
import konoha.script.TypeMatcher;
import konoha.script.TypeSystem;

public class GetterSite extends DynamicSite {

	public static CallSite bootstrap(MethodHandles.Lookup lookup, String fieldName, MethodType type) throws Throwable {
		return new GetterSite(lookup, fieldName, type);
	}

	private GetterSite(Lookup lookup, String targetName, MethodType methodType) {
		super(lookup, targetName, methodType, true);
	}

	public GetterSite(TypeSystem ts, String name, Type returnType, Type[] paramTypes) {
		super(ts, name, returnType, paramTypes, true);
	}

	@Override
	public DynamicSite op(String name, Type returnType, Type[] paramTypes) {
		return new GetterSite(this.typeSystem, name, returnType, paramTypes);
	}

	@Override
	public Functor lookup(TypeMatcher m, String name, Type[] a) {
		m.init(a[0]);
		return typeSystem.getGetter(m, a[0], name);
	}

}
