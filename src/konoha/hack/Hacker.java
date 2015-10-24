package konoha.hack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import konoha.script.Debug;
import konoha.script.ScriptContext;

public abstract class Hacker {

	public final static boolean isHackerClass(Class<?> c) {
		try {
			c.getMethod("hack", ScriptContext.class);
			return true;
		} catch (NoSuchMethodException | SecurityException e) {
		}
		return false;
	}

	public final static void hack(Class<?> c, ScriptContext context) {
		try {
			Method m = c.getMethod("hack", ScriptContext.class);
			try {
				m.invoke(null, context);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				Debug.traceException(e);
			}
		} catch (NoSuchMethodException | SecurityException e) {
		}
	}
}
