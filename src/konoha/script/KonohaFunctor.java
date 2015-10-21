package konoha.script;

import java.lang.reflect.Type;

import konoha.Function;

public abstract class KonohaFunctor {
	static private Functor assert_ = null;

	static Functor getAssertFunctor() {
		if (assert_ == null) {
			assert_ = load(Syntax.Function, "assert_", boolean.class, String.class);
		}
		return assert_;
	}

	public final static void assert_(boolean cond, String msg) {
		assert (cond) : msg;
	}

	private static Functor ThrowError = null;

	static Functor getThrowErrorFunctor() {
		if (ThrowError == null) {
			ThrowError = load(Syntax.Error, "ThrowError", String.class);
		}
		return ThrowError;
	}

	public final static void ThrowError(String msg) {
		throw new ScriptRuntimeException(msg);
	}

	private static Functor StringInterpolation = null;

	static Functor getInterpolationFunctor() {
		if (StringInterpolation == null) {
			StringInterpolation = load(Syntax.Function, "join", Object[].class);
		}
		return StringInterpolation;
	}

	public final static String join(Object[] args) {
		StringBuilder sb = new StringBuilder();
		for (Object a : args) {
			if (!(a instanceof Object[])) {
				sb.append(a);
			}
		}
		return sb.toString();
	}

	private static Functor indyMethod = null;

	static Functor getIndyMethodFunctor() {
		if (indyMethod == null) {
			indyMethod = load(Syntax.Indy, "indyMethod", TypeSystem.class, String.class, int.class, Object[].class);
		}
		return indyMethod;
	}

	private static TypeMatcher threadUnsafeMatcher = new TypeMatcher();

	public final static Object indyMethod(TypeSystem ts, String name, int paramId, Object... args) throws NoSuchMethodException {
		Type[] a = ts.getIndyParameterTypes(paramId);
		a = updateTypes(a, args);
		Functor f = null;
		synchronized (threadUnsafeMatcher) {
			threadUnsafeMatcher.init(a[0]);
			f = ts.getMethod(threadUnsafeMatcher, a[0], name, a);
		}
		if (f != null) {
			// need type conversion
			// for(int i = 0; i < a.length; i++) {
			// Class<?> c = Lang.toClassType(f.get(i));
			// args[i] = ts.cast(c, args[i]);
			// }
			return f.eval(null, args);
		}
		throw new NoSuchMethodException(String.format("%s::%s", Lang.name(a[0]), name));

	}

	private static Type[] updateTypes(Type[] a, Object[] args) {
		int start = args.length;
		for (int i = 0; i < args.length; i++) {
			if (args[i] == null) {
				continue;
			}
			if (a[i] instanceof Class<?>) {
				Class<?> t = (Class<?>) a[i];
				Class<?> c = args[i].getClass();
				if (t.isAssignableFrom(c)) {
					start = i; // update
					break;
				}
			}
		}
		if (start < args.length) {
			a = a.clone();
			for (int i = start; i < args.length; i++) {
				if (args[i] == null) {
					continue;
				}
				if (a[i] instanceof Class<?>) {
					Class<?> t = (Class<?>) a[i];
					Class<?> c = args[i].getClass();
					if (t.isAssignableFrom(c)) {
						a[i] = c;
					}
				}
			}
		}
		return a;
	}

	private static Functor Object_getField = null;

	static Functor Object_getField() {
		if (Object_getField == null) {
			Object_getField = new Functor(Syntax.Function, Reflector.load(KonohaFunctor.class, "Object_getField", Object.class, String.class));
		}
		return Object_getField;
	}

	public final static Object Object_getField(Object self, String name) {
		return Reflector.getField(self, name);
	}

	private static Functor Object_setField = null;

	static Functor Object_setField() {
		if (Object_setField == null) {
			Object_setField = new Functor(Syntax.Function, Reflector.load(KonohaFunctor.class, "Object_setField", Object.class, String.class, Object.class));
		}
		return Object_setField;
	}

	public final static Object Object_setField(Object self, String name, Object val) {
		Reflector.setField(self, name, val);
		return val;
	}

	private static Functor Object_invokeFunction = null;

	static Functor Object_invokeFunction() {
		if (Object_invokeFunction == null) {
			Object_invokeFunction = load(Syntax.Function, "Object_invokeFunction", Function.class, Object[].class);
		}
		return Object_invokeFunction;
	}

	public final static Object invokeFunc(Function self, Object... a) {
		return Reflector.invokeMethod(self, self.f, a);
	}

	private static Functor load(Syntax syntax, String name, Class<?>... args) {
		return new Functor(syntax, Reflector.load(KonohaFunctor.class, name, args));
	}
}
