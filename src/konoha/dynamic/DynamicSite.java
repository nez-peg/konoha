package konoha.dynamic;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.reflect.Type;
import java.util.Arrays;

import konoha.asm.ConstPools;
import konoha.script.Functor;
import konoha.script.Lang;
import konoha.script.TypeMatcher;
import konoha.script.TypeSystem;

public abstract class DynamicSite extends MutableCallSite {

	public DynamicSite(TypeSystem ts, String name, Type returnType, Type[] paramTypes, boolean collectAsArray) {
		this(MethodHandles.lookup(), encode(ts, name, paramTypes), toMethodType(returnType, paramTypes), collectAsArray);
	}

	public final static MethodType toMethodType(Type returnType, Type[] paramTypes) {
		Class<?>[] p = new Class<?>[paramTypes.length];
		for (int i = 0; i < paramTypes.length; i++) {
			p[i] = Lang.toClassType(paramTypes[i]);
			if (p[i].isPrimitive()) {
				p[i] = Object.class;
			}
		}
		return MethodType.methodType(Lang.toClassType(returnType), p);
	}

	final static String encode(TypeSystem ts, String name, Type[] paramTypes) {
		int id = ConstPools.registTypeSystem(ts);
		int paramid = ts.getIndyParameterTypes(paramTypes);
		return name + "," + id + "," + paramid;
	}

	public final String getTargetName() {
		return this.targetName;
	}

	public final String encodeName() {
		return encode(this.typeSystem, this.targetName, this.paramTypes);
	}

	protected static TypeMatcher threadUnsafeMatcher = new TypeMatcher();
	protected TypeSystem typeSystem;
	protected final MethodHandles.Lookup lookup;
	protected final String targetName;
	protected final MethodHandle fallback;

	public DynamicSite(MethodHandles.Lookup lookup, String targetName, MethodType methodType, boolean collectAsArray) {
		super(methodType);
		String[] s = targetName.split(",");
		this.targetName = s[0];
		this.typeSystem = ConstPools.typeSystem(Integer.parseInt(s[1]));
		this.paramTypes = this.typeSystem.getIndyParameterTypes(Integer.parseInt(s[2]));
		this.lookup = lookup;
		MethodHandle fallbackHandle = this.initFallbackHandle(methodType.returnType(), collectAsArray ? -1 : methodType.parameterCount());
		fallbackHandle = fallbackHandle.bindTo(this);
		if (collectAsArray) {
			fallbackHandle = fallbackHandle.asCollector(Object[].class, methodType.parameterCount());
		}
		this.fallback = fallbackHandle.asType(methodType);
		this.setTarget(this.fallback);
		this.recheckTypes = methodType.parameterCount() == 0 ? Lang.EmptyClasses : new Class<?>[methodType.parameterCount()];
	}

	public abstract Functor lookup(TypeMatcher m, String name, Type[] a);

	public abstract DynamicSite op(String name, Type returnType, Type[] paramTypes);

	protected final MethodHandle initFallbackHandle(Class<?> returnType, int paramSize) {
		MethodType type = MethodType.methodType(returnType, makeParams(paramSize));
		try {
			String name = "fallback";
			if (returnType == void.class || returnType == boolean.class) {
				name = returnType.getSimpleName() + "Fallback";
			}
			return MethodHandles.lookup().findVirtual(this.getClass(), name, type);
		} catch (Throwable e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

	private Class<?>[] makeParams(int paramSize) {
		if (paramSize == -1) {
			return new Class<?>[] { Object[].class };
		}
		Class<?>[] paramClasses = new Class<?>[paramSize];
		Arrays.fill(paramClasses, Object.class);
		return paramClasses;
	}

	public Object fallback(Object[] args) throws Throwable {
		MethodHandle targetHandle = lookup(args);
		return guard(targetHandle, args);
	}

	public void voidFallback(Object[] args) throws Throwable {
		MethodHandle targetHandle = lookup(args);
		Class<?> returnType = targetHandle.type().returnType();
		if (returnType != void.class) {
			// FIXME:
			// MethodHandles.filterReturnValue(targetHandle, filter);
		}
		guard(targetHandle, args);
	}

	public boolean booleanFallback(Object[] args) throws Throwable {
		MethodHandle targetHandle = lookup(args);
		Class<?> returnType = targetHandle.type().returnType();
		if (returnType != boolean.class) {
			// FIXME:
			// MethodHandles.filterReturnValue(targetHandle, filter);
		}
		return (Boolean) guard(targetHandle, args);
	}

	protected final Type[] paramTypes;

	final protected MethodHandle lookup(Object[] args) throws Throwable {
		Type[] a = updateTypes(paramTypes, args);
		Functor f = null;
		synchronized (threadUnsafeMatcher) {
			threadUnsafeMatcher.init(a[0]);
			f = lookup(threadUnsafeMatcher, a);
		}
		MethodHandle mh = null;
		if (f != null) {
			mh = f.toMethodHandler(this.lookup);
		}
		if (mh == null) {
			unfound(Lang.toClassType(a[0]));
		}
		// System.out.println("BEFORE: " + f + "\n\t" + mh);
		MethodType mt = mh.type();
		for (int i = 0; i < args.length; i++) {
			Class<?> pc = mt.parameterType(i);
			Class<?> ac = argumentType(i, a, args);
			if (pc.isAssignableFrom(ac)) {
				this.recheckTypes[i] = Lang.toBox(pc);
				continue;
			}
			Functor cast = typeSystem.getCast(ac, pc);
			// Debug.TRACE("Converting a[%d] %s != %s cast=%s", i, pc, ac,
			// cast);
			if (cast != null) {
				MethodHandle conv = cast.toMethodHandler(lookup);
				MethodHandles.filterArguments(mh, i, conv);
			}
			this.recheckTypes[i] = Lang.toBox(ac);
		}
		// System.out.println("CONV: " + mh);
		mh = mh.asType(this.type());
		// System.out.println("AFTER: " + mh);
		return mh;
	}

	public final Functor lookup(TypeMatcher m, Type[] a) {
		return lookup(m, this.targetName, a);
	}

	private static Type[] updateTypes(Type[] a, Object[] args) {
		int start = args.length;
		for (int i = 0; i < args.length; i++) {
			// System.out.printf("a[%d] %s %s\n", i, a[i], args[i]);
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

	private final Class<?> argumentType(int i, Type[] a, Object[] args) {
		Class<?> pc = Lang.toClassType(a[i]);
		if (args[i] == null) {
			return pc;
		}
		Class<?> ac = Lang.toUnbox(args[i].getClass());
		if (pc.isAssignableFrom(ac)) {
			return pc;
		}
		return ac;
	}

	protected void unfound(Type recvType) throws Throwable {
		throw new NoSuchMethodException(String.format("%s::%s", Lang.name(recvType), targetName));
	}

	// typecheck

	protected final static MethodHandle typecheck = initTypeCheckHandle();

	private final static MethodHandle initTypeCheckHandle() {
		try {
			MethodType type = MethodType.methodType(boolean.class, new Class<?>[] { Object[].class });
			return MethodHandles.lookup().findVirtual(DynamicSite.class, "typecheck", type);
		} catch (Throwable e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

	protected final Class<?>[] recheckTypes;

	public final boolean typecheck(Object[] a) {
		if (recheckTypes.length != a.length) {
			return false;
		}
		if (!recheckTypes[0].isInstance(a[0])) {
			return false;
		}
		for (int i = 1; i < recheckTypes.length; i++) {
			Class<?> paramClass = recheckTypes[i];
			Object param = a[i];
			if (!paramClass.isInstance(param)) {
				return false;
			}
		}
		return true;
	}

	protected final Object guard(MethodHandle targetHandle, Object... args) throws Throwable {
		MethodHandle testHandle = typecheck.bindTo(this).asCollector(Object[].class, args.length);
		// create guard handle
		MethodHandle guard = MethodHandles.guardWithTest(testHandle, targetHandle, this.fallback);
		this.setTarget(guard);
		return targetHandle.invokeWithArguments(args);
	}

	public Object eval(Object... args) throws Throwable {
		MethodHandle targetHandle = lookup(args);
		return targetHandle.invokeWithArguments(args);
	}

}
