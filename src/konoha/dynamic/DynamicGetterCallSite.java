package konoha.dynamic;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

public class DynamicGetterCallSite extends DynamicCallSite {

	public static CallSite bootstrap(MethodHandles.Lookup lookup, String fieldName, MethodType type) throws Throwable {
		return new DynamicGetterCallSite(lookup, fieldName, type);
	}

	public DynamicGetterCallSite(Lookup lookup, String targetName, MethodType methodType) {
		super(lookup, targetName, methodType, false);
	}

	public Object fallback(Object arg) throws Throwable {
		Class<?> recvClass = arg.getClass();
		MethodHandle targetHandle = null;
		synchronized (threadUnsafeMatcher) {
			threadUnsafeMatcher.init(recvClass);
			targetHandle = toMethodHandle(recvClass, typeSystem.getGetter(threadUnsafeMatcher, recvClass, this.targetName));
		}
		return guard(targetHandle, new Class<?>[] { recvClass }, arg);
	}

	@Override
	public Object eval(Object... args) throws Throwable {
		Class<?> recvClass = args[0].getClass();
		MethodHandle targetHandle = null;
		synchronized (threadUnsafeMatcher) {
			threadUnsafeMatcher.init(recvClass);
			targetHandle = toMethodHandle(recvClass, typeSystem.getGetter(threadUnsafeMatcher, recvClass, this.targetName));
		}
		return targetHandle.invokeWithArguments(args);
	}

}
