package konoha.dynamic;

import java.lang.invoke.CallSite;
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

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.GeneratorAdapter;

public abstract class DynamicCallSite extends MutableCallSite {

	public DynamicCallSite(TypeSystem ts, String name, Type returnType, Type[] paramTypes, boolean collectAsArray) {
		this(MethodHandles.lookup(), encode(ts, name, paramTypes), toMethodType(returnType, paramTypes), collectAsArray);
	}

	public final static MethodType toMethodType(Type returnType, Type[] paramTypes) {
		Class<?>[] p = new Class<?>[paramTypes.length];
		for (int i = 0; i < paramTypes.length; i++) {
			p[i] = Lang.toBox(Lang.toClassType(paramTypes[i]));
		}
		return MethodType.methodType(Lang.toClassType(returnType), p);
	}

	final static String encode(TypeSystem ts, String name, Type[] paramTypes) {
		int id = ConstPools.registTypeSystem(ts);
		int paramid = ts.getIndyParameterTypes(paramTypes);
		return name + "," + id + "," + paramid;
	}

	public final String encodeName() {
		return encode(this.typeSystem, this.targetName, this.paramTypes);
	}

	protected static TypeMatcher threadUnsafeMatcher = new TypeMatcher();
	protected TypeSystem typeSystem;
	protected final MethodHandles.Lookup lookup;
	protected final String targetName;
	protected final MethodHandle fallback;

	public DynamicCallSite(MethodHandles.Lookup lookup, String targetName, MethodType methodType, boolean collectAsArray) {
		super(methodType);
		String[] s = targetName.split(",");
		this.targetName = s[0];
		this.typeSystem = ConstPools.typeSystem(Integer.parseInt(s[1]));
		this.paramTypes = this.typeSystem.getIndyParameterTypes(Integer.parseInt(s[2]));
		this.lookup = lookup;
		MethodHandle fallbackHandle = this.initHandle("fallback", methodType.returnType(), collectAsArray ? -1 : methodType.parameterCount());
		fallbackHandle = fallbackHandle.bindTo(this);
		if (collectAsArray) {
			fallbackHandle = fallbackHandle.asCollector(Object[].class, methodType.parameterCount());
		}
		this.fallback = fallbackHandle.asType(methodType);
		this.setTarget(this.fallback);
		this.recheckTypes = new Class<?>[methodType.parameterCount()];
	}

	public final String getTargetName() {
		return this.targetName;
	}

	protected final MethodHandle initHandle(String name, Class<?> returnType, int paramSize) {
		MethodType type = MethodType.methodType(returnType, makeParams(paramSize));
		try {
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

	protected final Type[] paramTypes;

	final protected MethodHandle lookupMethod(Object[] args) throws Throwable {
		Type[] a = updateTypes(paramTypes, args);
		MethodHandle targetHandle = null;
		synchronized (threadUnsafeMatcher) {
			threadUnsafeMatcher.init(a[0]);
			targetHandle = toMethodHandle(a[0], typeSystem.getMethod(threadUnsafeMatcher, a[0], this.targetName, a));
		}
		MethodType mt = targetHandle.type();
		System.out.println("checking method filter" + targetHandle.type());
		for (int i = 0; i < args.length; i++) {
			Class<?> pc = mt.parameterType(i);
			Class<?> ac = argumentType(i, a, args);
			if (pc.isAssignableFrom(ac)) {
				this.recheckTypes[i] = pc;
				continue;
			}
			Functor f = typeSystem.getCast(ac, pc);
			System.out.printf("Converting a[%d] %s != %s f=%s\n", i, pc, ac, f);
			MethodHandle conv = f.toMethodHandler(lookup);
			MethodHandles.filterArguments(targetHandle, i, conv);
			this.recheckTypes[i] = ac;
		}
		return targetHandle;
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
		Class<?> ac = a[i].getClass();
		if (pc.isAssignableFrom(ac)) {
			return pc;
		}
		return ac;
	}

	final protected MethodHandle toMethodHandle(Type recvType, Functor f) throws Throwable {
		MethodHandle mh = null;
		if (f != null) {
			mh = f.toMethodHandler(this.lookup);
		}
		if (mh == null) {
			unfound(recvType);
		} else {
			mh = mh.asType(this.type());
		}
		return mh;
	}

	protected void unfound(Type recvType) throws Throwable {
		throw new NoSuchMethodException(String.format("%s::%s", Lang.name(recvType), targetName));
	}

	// asm

	public void asm(GeneratorAdapter adapter) {
		String desc = this.type().toMethodDescriptorString();
		adapter.invokeDynamic(this.targetName, desc, handle());
	}

	private Handle handle() {
		org.objectweb.asm.Type[] paramTypes = { org.objectweb.asm.Type.getType(MethodHandles.Lookup.class), org.objectweb.asm.Type.getType(String.class), org.objectweb.asm.Type.getType(MethodType.class) };
		org.objectweb.asm.commons.Method methodDesc = new org.objectweb.asm.commons.Method("bootstrap", org.objectweb.asm.Type.getType(CallSite.class), paramTypes);
		return new Handle(Opcodes.H_INVOKESTATIC, org.objectweb.asm.Type.getType(this.getClass()).getInternalName(), "bootstrap", methodDesc.getDescriptor());
	}

	protected final static MethodHandle typecheck = initTypeCheckHandle();

	private final static MethodHandle initTypeCheckHandle() {
		try {
			MethodType type = MethodType.methodType(boolean.class, new Class<?>[] { Object[].class });
			return MethodHandles.lookup().findVirtual(DynamicCallSite.class, "typecheck", type);
		} catch (Throwable e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

	public abstract Object eval(Object... args) throws Throwable;

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

}
