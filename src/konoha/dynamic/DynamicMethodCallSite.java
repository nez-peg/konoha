package konoha.dynamic;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Type;

import konoha.script.TypeSystem;

public class DynamicMethodCallSite extends DynamicCallSite {

	public static CallSite bootstrap(MethodHandles.Lookup lookup, String methodName, MethodType type) throws Throwable {
		return new DynamicMethodCallSite(lookup, methodName, type);
	}

	public DynamicMethodCallSite(Lookup lookup, String targetName, MethodType methodType) {
		super(lookup, targetName, methodType, true);
	}

	public DynamicMethodCallSite(TypeSystem ts, String name, Type returnType, Type[] paramTypes) {
		super(ts, name, returnType, paramTypes, true);
	}

	public Object fallback(Object[] args) throws Throwable {
		MethodHandle targetHandle = lookupMethod(args);
		return guard(targetHandle, args);
	}

	@Override
	public final Object eval(Object... args) throws Throwable {
		MethodHandle mh = this.lookupMethod(args);
		return mh.invokeWithArguments(args);
	}

}
