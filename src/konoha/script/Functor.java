package konoha.script;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import konoha.asm.Prototype;
import nez.util.ConsoleUtils;

public class Functor {
	private static final MethodHandles.Lookup lookup = MethodHandles.lookup();

	public Syntax syntax;
	public Object ref;
	private Type[] paramTypes = null; // cache

	public Functor(Syntax syntax, Method ref) {
		this.syntax = syntax;
		this.ref = ref;
	}

	public Functor(Syntax syntax, Field f) {
		this.syntax = syntax;
		this.ref = f;
	}

	public Functor(Syntax syntax, Constructor<?> c) {
		this.syntax = syntax;
		this.ref = c;
	}

	public Functor(Syntax syntax, Prototype proto) {
		this.syntax = syntax;
		this.ref = proto;
		this.paramTypes = proto.getParameterTypes();
	}

	public void update(Method method) {
		this.ref = method;
	}

	public final String getClassName() {
		if (ref instanceof Prototype) {
			return ((Prototype) ref).getClassName();
		}
		if (ref instanceof Method) {
			return ((Method) ref).getDeclaringClass().getName();
		}
		if (ref instanceof Field) {
			return ((Field) ref).getDeclaringClass().getName();
		}
		if (ref instanceof Constructor<?>) {
			return ((Constructor<?>) ref).getDeclaringClass().getName();
		}
		return null;
	}

	public final String getName() {
		if (ref instanceof Prototype) {
			return ((Prototype) ref).getName();
		}
		if (ref instanceof Method) {
			return ((Method) ref).getName();
		}
		if (ref instanceof Field) {
			return ((Field) ref).getName();
		}
		if (ref instanceof Constructor<?>) {
			return "<init>";
		}
		return null;
	}

	public final Type getReturnType() {
		if (ref instanceof Prototype) {
			return ((Prototype) ref).getReturnType();
		}
		if (ref instanceof Method) {
			return ((Method) ref).getGenericReturnType();
		}
		if (ref instanceof Field) {
			return syntax == Syntax.Getter ? ((Field) ref).getGenericType() : void.class;
		}
		if (ref instanceof Constructor<?>) {
			return ((Constructor<?>) ref).getDeclaringClass();
		}
		return Object.class;
	}

	public final int size() {
		if (ref instanceof Method) {
			Method m = (Method) ref;
			if (paramTypes == null) { // Getter
				paramTypes = m.getGenericParameterTypes();
			}
			return Lang.isStatic(m) ? paramTypes.length : paramTypes.length + 1;
		}
		if (ref instanceof Prototype) {
			return this.paramTypes.length;
		}
		if (ref instanceof Field) {
			Field f = (Field) ref;
			if (syntax == Syntax.Getter) { // Getter
				return Lang.isStatic(f) ? 0 : 1;
			}
			return Lang.isStatic(f) ? 1 : 2;
		}
		if (ref instanceof Constructor<?>) {
			Constructor<?> c = (Constructor<?>) ref;
			if (paramTypes == null) {
				paramTypes = c.getGenericParameterTypes();
			}
			return paramTypes.length;
		}
		return 0;
	}

	public final Type get(int index) {
		if (ref instanceof Method) {
			Method m = (Method) ref;
			if (paramTypes == null) {
				paramTypes = m.getGenericParameterTypes();
			}
			if (Lang.isStatic(m)) {
				return paramTypes[index];
			}
			return (index == 0) ? m.getDeclaringClass() : paramTypes[index - 1];
		}
		if (ref instanceof Prototype) {
			return this.paramTypes[index];
		}
		if (ref instanceof Field) {
			Field f = (Field) ref;
			if (syntax == Syntax.Getter) { // Getter
				return f.getDeclaringClass();
			}
			if (!Lang.isStatic(f)) {
				return index == 0 ? f.getDeclaringClass() : f.getGenericType();
			}
			return f.getGenericType();
		}
		if (ref instanceof Constructor<?>) {
			if (paramTypes == null) {
				Constructor<?> c = (Constructor<?>) ref;
				paramTypes = c.getGenericParameterTypes();
			}
			return paramTypes[index];
		}
		return Object.class;
	}

	public final boolean match(Type[] paramTypes) {
		if (this.size() == paramTypes.length) {
			for (int i = 0; i < paramTypes.length; i++) {
				if (paramTypes[i] != this.get(i)) {
					return true;
				}
			}
		}
		return false;
	}

	private Object evalIndy(Object... args) throws Throwable {
		MethodHandle mh = null;
		if (ref instanceof Method) {
			mh = lookup.unreflect((Method) ref);
			return mh.invokeWithArguments(args);
		}
		if (ref instanceof Field) {
			mh = syntax == Syntax.Getter ? lookup.unreflectGetter((Field) ref) : lookup.unreflectSetter((Field) ref);
			return mh.invokeWithArguments(args);
		}
		if (ref instanceof Constructor<?>) {
			mh = lookup.unreflectConstructor((Constructor<?>) ref);
		}
		if (mh != null) {
			return mh.invokeWithArguments(args);
		}
		return null;
	}

	public final Object eval(TypedTree node, Object... args) {
		try {
			Object v = evalIndy(args);
			return this.getReturnType() == void.class ? Interpreter.empty : v;
		} catch (Throwable e) {
			if (e instanceof Error) {
				throw (Error) e;
			}
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			e.printStackTrace();
			ConsoleUtils.println("[FIXME] " + node);
		}
		return null;
	}

	@Override
	public String toString() {
		return ref.toString();
	}

	public static Functor newPrototypeStaticMethod(String cname, Type returnType, String name, Type[] paramTypes) {
		// TODO Auto-generated method stub
		return null;
	}

}
