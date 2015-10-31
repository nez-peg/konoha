//package konoha.dynamic;
//
//import java.lang.invoke.CallSite;
//import java.lang.invoke.MethodHandle;
//import java.lang.invoke.MethodHandles;
//import java.lang.invoke.MethodHandles.Lookup;
//import java.lang.invoke.MethodType;
//
//public class DynamicSetterCallSite extends DynamicCallSite {
//
//	public static CallSite bootstrap(MethodHandles.Lookup lookup, String fieldName, MethodType type) throws Throwable {
//		return new DynamicGetterCallSite(lookup, fieldName, type);
//	}
//
//	public DynamicSetterCallSite(Lookup lookup, String targetName, MethodType methodType) {
//		super(lookup, targetName, methodType, false);
//	}
//
//	// for getter
//
//	public Object fallback(Object recv, Object val) throws Throwable {
//		Class<?> recvClass = recv.getClass();
//		MethodHandle targetHandle = null;
//		synchronized (threadUnsafeMatcher) {
//			threadUnsafeMatcher.init(recvClass);
//			targetHandle = toMethodHandle(recvClass, typeSystem.getGetter(threadUnsafeMatcher, recvClass, this.targetName));
//		}
//
//	}
//
// }
