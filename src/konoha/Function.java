package konoha;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import konoha.script.Debug;
import konoha.script.Reflector;

public abstract class Function {
	public final Method f;
	public final MethodHandle mh;

	protected Function() {
		f = Reflector.findInvokeMethod(this);
		mh = toMethodHandle(f);
	}

	private static MethodHandle toMethodHandle(Method f) {
		try {
			return MethodHandles.lookup().unreflect(f);
		} catch (IllegalAccessException e) {
			Debug.traceException(e);
		}
		return null;
	}

}
