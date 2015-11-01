package konoha.dynamic;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Type;

import konoha.script.Functor;
import konoha.script.TypeMatcher;
import konoha.script.TypeSystem;

public class SetterSite extends DynamicSite {

	public static CallSite bootstrap(MethodHandles.Lookup lookup, String fieldName, MethodType type) throws Throwable {
		return new SetterSite(lookup, fieldName, type);
	}

	private SetterSite(Lookup lookup, String targetName, MethodType methodType) {
		super(lookup, targetName, methodType, true);
	}

	public SetterSite(TypeSystem ts, String name, Type returnType, Type[] paramTypes) {
		super(ts, name, returnType, paramTypes, true);
	}

	@Override
	public DynamicSite op(String name, Type returnType, Type[] paramTypes) {
		return new SetterSite(this.typeSystem, name, returnType, paramTypes);
	}

	@Override
	public Functor lookup(TypeMatcher m, String name, Type[] a) {
		m.init(a[0]);
		return typeSystem.getSetter(m, a[0], this.targetName);
	}

	@Override
	public Object eval(Object... args) throws Throwable {
		MethodHandle targetHandle = lookup(args);
		targetHandle.invokeWithArguments(args);
		return args[1];
	}

}
