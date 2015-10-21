package konoha.asm;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class DynamicCallSite extends MutableCallSite {
	public final static Handle bsmInstanceMethodHandle = initBsmHandle("InstanceMethod");
	private final static MethodHandle fbInstanceMethodHandle = initFallbackHandleWithVarg("InstanceMethod");
	private final static MethodHandle testInstanceMethodHandle = initTestHandleWithVarg("InstanceMethod");

	public final static Handle bsmGetterHandle = initBsmHandle("Getter");
	private final static MethodHandle fbGetterHandle = initFallbackHandle("Getter", 1);
	private final static MethodHandle testGetterHandle = initTestHandle("Getter", 1);

	public final static Handle bsmSetterHandle = initBsmHandle("Setter");
	private final static MethodHandle fbSetterHandle = initFallbackHandle("Setter", 2);
	private final static MethodHandle testSetterHandle = initTestHandle("Setter", 2);

	/**
	 * for wrapper class to primitive class conversion key is wrapper class,
	 * value is corresponding primitive type. not contains Void type.
	 */
	private final static Map<Class<?>, Class<?>> primitiveMap = new HashMap<>();
	static {
		primitiveMap.put(Boolean.class, boolean.class);
		primitiveMap.put(Byte.class, byte.class);
		primitiveMap.put(Character.class, char.class);
		primitiveMap.put(Short.class, short.class);
		primitiveMap.put(Integer.class, int.class);
		primitiveMap.put(Float.class, float.class);
		primitiveMap.put(Long.class, long.class);
		primitiveMap.put(Double.class, double.class);
	}

	private final MethodHandles.Lookup lookup;

	/**
	 * representing method or field name.
	 */
	private final String targetName;

	private final MethodHandle fallbackHandle;

	// helper utils
	private static MethodHandle initFallbackHandle(String name, int paramSize) {
		Class<?>[] paramClasses = new Class<?>[paramSize];
		Arrays.fill(paramClasses, Object.class);
		MethodType type = MethodType.methodType(Object.class, paramClasses);
		try {
			return MethodHandles.lookup().findVirtual(DynamicCallSite.class, "fallbackFor" + name, type);
		} catch (Throwable e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

	private static MethodHandle initFallbackHandleWithVarg(String name) {
		MethodType type = MethodType.methodType(Object.class, new Class<?>[] { Object[].class });
		try {
			return MethodHandles.lookup().findVirtual(DynamicCallSite.class, "fallbackFor" + name, type);
		} catch (Throwable e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

	private static MethodHandle initTestHandle(String name, int paramSize) {
		Class<?>[] paramClasses = new Class<?>[paramSize * 2];
		for (int i = 0; i < paramSize; i++) {
			paramClasses[i] = Class.class;
			paramClasses[i + paramSize] = Object.class;
		}
		MethodType type = MethodType.methodType(boolean.class, paramClasses);
		try {
			return MethodHandles.lookup().findStatic(DynamicCallSite.class, "testFor" + name, type);
		} catch (Throwable e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

	private static MethodHandle initTestHandleWithVarg(String name) {
		MethodType type = MethodType.methodType(boolean.class, new Class<?>[] { Class[].class, Object[].class });
		try {
			return MethodHandles.lookup().findStatic(DynamicCallSite.class, "testFor" + name, type);
		} catch (Throwable e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

	private static Handle initBsmHandle(String name) {
		String bsmName = "bsm" + name;
		Type[] paramTypes = { Type.getType(MethodHandles.Lookup.class), Type.getType(String.class), Type.getType(MethodType.class) };
		org.objectweb.asm.commons.Method methodDesc = new org.objectweb.asm.commons.Method(bsmName, Type.getType(CallSite.class), paramTypes);
		return new Handle(Opcodes.H_INVOKESTATIC, Type.getType(DynamicCallSite.class).getInternalName(), bsmName, methodDesc.getDescriptor());
	}

	/**
	 * 
	 * @param clazz
	 * @return if not found unwrapped class(corresponding primitive class),
	 *         return null.
	 */
	private static Class<?> getUnwrappedClass(Class<?> clazz) {
		return primitiveMap.get(Objects.requireNonNull(clazz));
	}

	public DynamicCallSite(MethodHandles.Lookup lookup, String targetName, MethodHandle fallbackHandle, MethodType methodType) {
		this(lookup, targetName, fallbackHandle, methodType, true);
	}

	public DynamicCallSite(MethodHandles.Lookup lookup, String targetName, MethodHandle fallbackHandle, MethodType methodType, boolean collectAsArray) {
		super(methodType);
		this.targetName = Objects.requireNonNull(targetName);
		this.lookup = lookup;

		fallbackHandle = fallbackHandle.bindTo(this);
		if (collectAsArray) {
			fallbackHandle = fallbackHandle.asCollector(Object[].class, methodType.parameterCount());
		}
		this.fallbackHandle = fallbackHandle.asType(methodType);
		this.setTarget(this.fallbackHandle);
	}

	// for method
	public static CallSite bsmInstanceMethod(MethodHandles.Lookup lookup, String methodName, MethodType type) throws Throwable {
		return new DynamicCallSite(lookup, methodName, fbInstanceMethodHandle, type);
	}

	public Object fallbackForInstanceMethod(Object[] args) throws Throwable {
		Class<?> recvClass = args[0].getClass();

		// lookup targetHandle
		MethodHandle targetHandle = null;
		for (Method method : recvClass.getMethods()) {
			final int argSize = args.length - 1;
			Class<?>[] paramClasses = method.getParameterTypes();
			if (argSize == paramClasses.length && this.targetName.equals(method.getName())) {
				int i = 0;
				for (; i < argSize; i++) {
					Class<?> paramClass = paramClasses[i];
					Object arg = args[i + 1];
					if (!paramClass.isInstance(arg) && !paramClass.equals(getUnwrappedClass(arg.getClass()))) {
						break;
					}
				}
				if (i == argSize) {
					targetHandle = this.lookup.unreflect(method).asType(this.type());
					break;
				}
			}
		}
		if (targetHandle == null) {
			throw new NoSuchMethodException(this.targetName);
		}

		// create test handle
		Class<?>[] argClasses = new Class<?>[args.length];
		for (int i = 0; i < args.length; i++) {
			argClasses[i] = args[i].getClass();
		}
		MethodHandle testHandle = testInstanceMethodHandle.bindTo(argClasses).asCollector(Object[].class, args.length);

		// create guard handle
		MethodHandle guard = MethodHandles.guardWithTest(testHandle, targetHandle, this.fallbackHandle);
		this.setTarget(guard);

		return targetHandle.invokeWithArguments(args);
	}

	/**
	 * 
	 * @param paramClasses
	 *            contains receiver class
	 * @param params
	 *            contains receiver object
	 * @return
	 */
	public static boolean testForInstanceMethod(Class<?>[] paramClasses, Object[] params) {
		if (paramClasses.length != params.length) {
			return false;
		}
		if (!paramClasses[0].isInstance(params[0])) {
			return false;
		}
		for (int i = 1; i < paramClasses.length; i++) {
			Class<?> paramClass = paramClasses[i];
			Object param = params[i];
			if (!paramClass.isInstance(param) && !paramClass.equals(getUnwrappedClass(param.getClass()))) {
				return false;
			}
		}
		return true;
	}

	// for getter
	public static CallSite bsmGetter(MethodHandles.Lookup lookup, String fieldName, MethodType type) throws Throwable {
		return new DynamicCallSite(lookup, fieldName, fbGetterHandle, type, false);
	}

	public Object fallbackForGetter(Object arg) throws Throwable {
		Class<?> recvClass = arg.getClass();

		// lookup field
		MethodHandle targetHandlee = this.lookup.unreflectGetter(recvClass.getField(this.targetName)).asType(this.type());

		// create test handle
		MethodHandle testHandle = testGetterHandle.bindTo(recvClass);

		// create guard method handle
		MethodHandle guard = MethodHandles.guardWithTest(testHandle, targetHandlee, this.fallbackHandle);
		this.setTarget(guard);

		return targetHandlee.invokeWithArguments(arg);
	}

	public static boolean testForGetter(Class<?> recvClass, Object receiver) {
		return recvClass.isInstance(receiver);
	}

	// for setter
	public static CallSite bsmSetter(MethodHandles.Lookup lookup, String fieldName, MethodType type) throws Throwable {
		return new DynamicCallSite(lookup, fieldName, fbSetterHandle, type, false);
	}

	public Object fallbackForSetter(Object receiver, Object value) throws Throwable {
		Class<?> recvClass = receiver.getClass();

		// lookup targetHandle
		MethodHandle targetHandle = this.lookup.unreflectSetter(recvClass.getField(this.targetName)).asType(this.type());

		// create test handle
		MethodHandle testHandle = testSetterHandle.bindTo(recvClass).bindTo(value.getClass());

		// crate guard
		MethodHandle guard = MethodHandles.guardWithTest(testHandle, targetHandle, this.fallbackHandle);
		this.setTarget(guard);

		return targetHandle.invokeWithArguments(receiver, value);
	}

	public static boolean testForSetter(Class<?> recvClass, Class<?> valueClass, Object receiver, Object value) {
		return recvClass.isInstance(receiver) && (valueClass.isInstance(value) || valueClass.equals(getUnwrappedClass(value.getClass())));
	}
}
