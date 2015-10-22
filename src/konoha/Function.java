package konoha;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

import konoha.script.Functor;
import konoha.script.Reflector;

public abstract class Function {
	public final Method f;
	public final MethodHandle mh;

	protected Function() {
		f = Reflector.findInvokeMethod(this);
		mh = Functor.toMethodHandler(f);
	}
}
