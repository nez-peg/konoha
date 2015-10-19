package konoha.script;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;

public class Java {

	public final static Class<?> toClassType(Type type) {
		if (type instanceof Class<?>) {
			return (Class<?>) type;
		}
		return ((GenericType) type).base;
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

	public final static boolean isExtraMethod(Method m) {
		Annotation a = m.getAnnotation(konoha.Method.class);
		return a != null;
	}

	public final static boolean isGetterMethod(Method m) {
		return false;
	}

	public final static boolean isSetterMethod(Method m) {
		return false;
	}

	public static final String name(Type t) {
		if (t == null) {
			return "untyped";
		}
		if (t instanceof Class<?>) {
			String n = ((Class<?>) t).getName();
			if (n.startsWith("java.lang.") || n.startsWith("java.util.")) {
				return n.substring(10);
			}
			return n;
		}
		return t.toString();
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

	public final static Type unifyAdd(Type t, Type t2) {
		if (t == t2) {
			return t;
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
		if (t == long.class || t2 == long.class) {
			return long.class;
		}
		if (t == int.class || t2 == int.class || t == byte.class || t2 == byte.class) {
			return int.class;
		}
		if (t == String.class || t2 == String.class) {
			return String.class;
		}
		if (t == Object.class || t2 == Object.class) {
			return Object.class;
		}
		return t;
	}

	public final static Type unifyEq(Type t, Type t2) {
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
		if (t == long.class || t2 == long.class) {
			return long.class;
		}
		if (t == int.class || t2 == int.class) {
			return int.class;
		}
		return t;
	}

	public final static Type unifyCmp(Type t, Type t2) {
		if (t == t2) {
			return t;
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
		if (t == long.class || t2 == long.class) {
			return long.class;
		}
		if (t == int.class || t2 == int.class) {
			return int.class;
		}
		return t;
	}

	public final static Type unifyBit(Type t, Type t2) {
		if (t == Object.class || t2 == Object.class) {
			return Object.class;
		}
		if (t == BigInteger.class || t2 == BigInteger.class) {
			return BigInteger.class;
		}
		if (t == long.class || t2 == long.class) {
			return long.class;
		}
		if (t == int.class || t2 == int.class) {
			return int.class;
		}
		return t;
	}

}
