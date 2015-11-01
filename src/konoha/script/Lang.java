package konoha.script;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import konoha.ArrayInt;
import konoha.KonohaArray;

public class Lang {

	public final static Type[] EmptyTypes = new Type[0];
	public final static Class<?>[] EmptyClasses = new Class<?>[0];

	public static final String name(Type t) {
		if (t == null) {
			return "untyped";
		}
		if (isKonohaArray(t) || isNativeArray(t)) {
			return name(getArrayElementType(t)) + "[]";
		}
		if (t instanceof Class<?>) {
			if (t == Object.class) {
				return "?";
			}
			String n = ((Class<?>) t).getName();
			if (n.startsWith("java.lang.")) {
				return n.substring(10);
			}
			return n;
		}
		return t.toString();
	}

	/* Type */

	public final static Class<?> toClassType(Type type) {
		if (type instanceof Class<?>) {
			return (Class<?>) type;
		}
		return ((GenericType) type).base;
	}

	private final static Map<Class<?>, Class<?>> unboxMap = new HashMap<>();
	static {
		unboxMap.put(Boolean.class, boolean.class);
		unboxMap.put(Byte.class, byte.class);
		unboxMap.put(Character.class, char.class);
		unboxMap.put(Short.class, short.class);
		unboxMap.put(Integer.class, int.class);
		unboxMap.put(Float.class, float.class);
		unboxMap.put(Long.class, long.class);
		unboxMap.put(Double.class, double.class);
	}

	public static Class<?> toUnbox(Class<?> c) {
		return unboxMap.getOrDefault(c, c);
	}

	public static Type toUnbox(Type t) {
		if (t instanceof Class<?>) {
			return toUnbox((Class<?>) t);
		}
		return t;
	}

	private final static Map<Class<?>, Class<?>> boxMap = new HashMap<>();
	static {
		boxMap.put(boolean.class, Boolean.class);
		boxMap.put(byte.class, Byte.class);
		boxMap.put(char.class, Character.class);
		boxMap.put(short.class, Short.class);
		boxMap.put(int.class, Integer.class);
		boxMap.put(float.class, Float.class);
		boxMap.put(long.class, Long.class);
		boxMap.put(double.class, Double.class);
	}

	public static Class<?> toBox(Class<?> c) {
		return boxMap.getOrDefault(c, c);
	}

	public static Type toBox(Type t) {
		if (t instanceof Class<?>) {
			return toBox((Class<?>) t);
		}
		return t;
	}

	/* Array */

	public final static boolean isKonohaArray(Type atype) {
		Class<?> c = toClassType(atype);
		return KonohaArray.class.isAssignableFrom(c);
	}

	public final static boolean isNativeArray(Type atype) {
		if (atype instanceof Class<?>) {
			return ((Class<?>) atype).isArray();
		}
		return false;
	}

	public final static Type getArrayElementType(Type atype) {
		if (isNativeArray(atype)) {
			return ((Class<?>) atype).getComponentType();
		}
		if (atype instanceof GenericType) {
			return ((GenericType) atype).getParameterTypes()[0];
		}
		if (isKonohaArray(atype)) {
			if (atype == ArrayInt.class) {
				return int.class;
			}
		}
		return Object.class;
	}

	public final static Class<?> getArrayElementClass(Type atype) {
		return toClassType(getArrayElementType(atype));
	}

	public final static Type toPrimitiveType(Type t) {
		if (t == Double.class || t == Float.class || t == float.class) {
			return double.class;
		}
		if (t == Long.class) {
			return long.class;
		}
		if (t == Integer.class || t == Short.class || t == short.class) {
			return int.class;
		}
		if (t == Character.class) {
			return char.class;
		}
		if (t == Boolean.class) {
			return boolean.class;
		}
		if (t == Byte.class) {
			return byte.class;
		}
		return t;
	}

	public final static boolean isPublic(Method m) {
		return Modifier.isPublic(m.getModifiers());
	}

	public final static boolean isStatic(Method m) {
		return Modifier.isStatic(m.getModifiers());
	}

	public final static boolean isPublicStatic(Method m) {
		int mod = m.getModifiers();
		return Modifier.isStatic(mod) && Modifier.isPublic(mod);
	}

	public final static boolean isInterface(Method m) {
		return Modifier.isInterface(m.getModifiers());
	}

	public final static boolean isPublic(Field m) {
		return Modifier.isPublic(m.getModifiers());
	}

	public final static boolean isStatic(Field m) {
		return Modifier.isStatic(m.getModifiers());
	}

	public final static boolean isPublicStatic(Field m) {
		int mod = m.getModifiers();
		return Modifier.isStatic(mod) && Modifier.isPublic(mod);
	}

	public static boolean isFinal(Field f) {
		return Modifier.isFinal(f.getModifiers());
	}

	public final static boolean isCastMethod(Method m) {
		Class<?>[] p = m.getParameterTypes();
		if (p.length != 1) {
			return false;
		}
		Annotation a = m.getAnnotation(konoha.Coercion.class);
		if (a != null) {
			return true;
		}
		String name = m.getName();
		if (name.startsWith("to_")) {
			return isPublicStatic(m);
		}
		return false;
	}

	public final static boolean isConvMethod(Method m) {
		Class<?>[] p = m.getParameterTypes();
		if (p.length != 1) {
			return false;
		}
		Annotation a = m.getAnnotation(konoha.Conversion.class);
		if (a != null) {
			return true;
		}
		String name = m.getName();
		if (name.startsWith("to") && !name.startsWith("to_")) {
			return isPublicStatic(m);
		}
		return false;
	}

	public final static boolean isExtraMethod2(Method m) {
		Annotation a = m.getAnnotation(konoha.Method.class);
		return a != null;
	}

	public final static boolean isOperator(Method m) {
		Annotation a = m.getAnnotation(konoha.Operator.class);
		return a != null;
	}

	public final static boolean isConst(Method m) {
		Annotation a = m.getAnnotation(konoha.Const.class);
		return a != null;
	}

	public final static boolean isGetterMethod(Method m) {
		return false;
	}

	public final static boolean isSetterMethod(Method m) {
		return false;
	}

	public final static Type unifyAdd(Type t, Type t2) {
		if (t == t2) {
			return t;
		}
		if (t == String.class || t2 == String.class) {
			return String.class;
		}
		return unifyNum(t, t2);
	}

	public final static Type unifyNum(Type t, Type t2) {
		if (t == t2) {
			return t;
		}
		if (t == Object.class || t2 == Object.class) {
			return Object.class;
		}
		if (t == BigDecimal.class || t2 == BigDecimal.class) {
			return BigDecimal.class;
		}
		if (t == BigInteger.class || t2 == BigInteger.class) {
			return BigInteger.class;
		}
		if (t == double.class || t2 == double.class) {
			return double.class;
		}
		if (t == float.class || t2 == float.class) {
			return float.class;
		}
		if (t == long.class || t2 == long.class) {
			return long.class;
		}
		if (t == int.class || t2 == int.class) {
			return int.class;
		}
		if (t == char.class || t2 == char.class) {
			return char.class;
		}
		if (t == short.class || t2 == short.class) {
			return int.class;
		}
		if (t == byte.class || t2 == byte.class) {
			return int.class;
		}
		return t;
	}

}
