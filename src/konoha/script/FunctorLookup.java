package konoha.script;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;

import nez.util.UList;

public class FunctorLookup {

	private final static Functor[] empty = new Functor[0];

	/* key */

	private String keyCast(Class<?> f, Class<?> t) {
		return f.getName() + "&" + t.getName();
	}

	private String keyConv(Class<?> f, Class<?> t) {
		return f.getName() + "!" + t.getName();
	}

	private String keyFunc(String name, Type[] a) {
		StringBuilder sb = new StringBuilder();
		sb.append(a.length);
		sb.append(name);
		for (Type t : a) {
			sb.append("#");
			sb.append(Java.name(t));
		}
		return sb.toString();
	}

	private String keyMethod(Class<?> c, String name, Type[] a) {
		StringBuilder sb = new StringBuilder();
		sb.append(c.getName());
		sb.append("::");
		sb.append(a.length);
		sb.append(name);
		for (Type t : a) {
			sb.append("#");
			sb.append(Java.name(t));
		}
		return sb.toString();
	}

	/* coercion */

	private HashMap<String, Functor> castMethodMap = new HashMap<String, Functor>();

	public final Functor getCast(Class<?> f, Class<?> t) {
		return this.castMethodMap.get(keyCast(f, t));
	}

	public final Functor getCast(Type f, Type t) {
		return this.getCast(Java.toClassType(f), Java.toClassType(t));
	}

	protected void addCastMethod(Method m) {
		Class<?> f = m.getParameterTypes()[0];
		Class<?> t = m.getReturnType();
		this.castMethodMap.put(keyCast(f, t), new Functor(Syntax.Cast, m));
	}

	protected void addConvertMethod(Method m) {
		Class<?> f = m.getParameterTypes()[0];
		Class<?> t = m.getReturnType();
		this.castMethodMap.put(keyConv(f, t), new Functor(Syntax.Conv, m));
	}

	public final Functor getConv(Class<?> f, Class<?> t) {
		return this.castMethodMap.get(keyConv(f, t));
	}

	public final Functor getConv(Type f, Type t) {
		return this.getConv(Java.toClassType(f), Java.toClassType(t));
	}

	/* class */

	HashMap<Class<?>, Functor[]> methodClassMap = new HashMap<>();

	public Functor[] getClassMethods(Class<?> c) {
		Functor[] m = methodClassMap.get(c);
		return m == null ? empty : m;
	}

	private void addClassMethod(Method method) {
		Class<?> c = method.getParameterTypes()[0];
		Functor[] list = methodClassMap.get(c);
		Functor f = new Functor(Syntax.Method, method);
		if (list != null) {
			Functor[] newlist = new Functor[list.length + 1];
			System.arraycopy(list, 0, newlist, 0, list.length);
			newlist[list.length] = f;
			methodClassMap.put(c, newlist);
		} else {
			Functor[] newlist = { f };
			methodClassMap.put(c, newlist);
		}
	}

	/* symbol */

	UList<Functor> symbolList = new UList<>(new Functor[128]);

	public final void addSymbol(Class<?> importedClass) {
		for (Method m : importedClass.getDeclaredMethods()) {
			if (Java.isPublicStatic(m)) {
				if (Java.isCastMethod(m)) {
					addCastMethod(m);
					continue;
				}
				if (Java.isConvMethod(m)) {
					addConvertMethod(m);
					continue;
				}
				if (Java.isExtraMethod(m)) {
					addClassMethod(m);
					continue;
				}
				addStaticMethod(m);
			}
		}
	}

	private void addStaticMethod(Method m) {
		updateSymbolCache(m.getName());
		this.symbolList.add(new Functor(Syntax.Function, m));
	}

	protected void addSymbolFunctor(Functor f) {
		updateSymbolCache(f.getName());
		this.symbolList.add(f);
	}

	public void addGlobalFunction(String name, Type funcType) {
		updateSymbolCache(name);
	}

	/* cache */

	HashMap<String, Functor[]> symbolCacheMap = new HashMap<>();
	HashMap<String, Functor> symbolCacheMap1 = new HashMap<>();

	// private Functor putSymbolCache(String name, Functor f) {
	// symbolCacheMap1.put(name, f);
	// return f;
	// }

	private void updateSymbolCache(String name) {
		if (symbolCacheMap.size() > 0) {
			symbolCacheMap.clear();
		}
		if (symbolCacheMap1.size() > 0) {
			symbolCacheMap1.clear();
		}
	}

	private void updateSymbolCache(Class<?> c) {
		if (symbolCacheMap.size() > 0) {
			symbolCacheMap.clear();
		}
		if (symbolCacheMap1.size() > 0) {
			symbolCacheMap1.clear();
		}
	}

	/** Function */

	public final Functor[] getFunction(String name) {
		Functor[] list = symbolCacheMap.get(name);
		if (list != null) {
			return list;
		}
		UList<Functor> l = new UList<Functor>(new Functor[32]);
		for (int i = symbolList.size() - 1; i >= 0; i--) {
			Functor f = symbolList.ArrayValues[i];
			if (f.getName().equals(name)) {
				l.add(f);
			}
		}
		list = l.compactArray();
		symbolCacheMap.put(name, list);
		return list;
	}

	public final Functor getFunction(TypeMatcher matcher, String name, Type[] a) {
		matcher.init(null);
		String key = keyFunc(name, a);
		if (this.symbolCacheMap1.containsKey(key)) {
			return this.symbolCacheMap1.get(key);
		}
		Functor[] list = getFunction(name);
		Functor matched = null;
		for (int i = list.length - 1; i >= 0; i--) {
			Functor f = list[i];
			matcher.reset();
			if (accept(matcher, f, a)) {
				this.symbolCacheMap1.put(key, f);
				return f;
			}
			if (matched == null && match(matcher, f, a)) {
				matched = f;
			}
		}
		if (matched != null) {
			this.symbolCacheMap1.put(key, matched);
			return matched;
		}
		symbolCacheMap1.put(key, null);
		return null;
	}

	public final Functor[] getConstructors(Type newType) {
		Class<?> c = Java.toClassType(newType);
		String key = c.getName() + ":<>";
		Functor[] list = symbolCacheMap.get(key);
		if (list != null) {
			return list;
		}
		Constructor<?>[] cList = c.getConstructors();
		list = new Functor[cList.length];
		for (int i = 0; i < cList.length; i++) {
			list[i] = new Functor(Syntax.Constructor, cList[i]);
		}
		symbolCacheMap.put(key, list);
		return list;
	}

	public final Functor getConstructor(TypeMatcher matcher, Type newType, Type[] a) {
		Class<?> c = Java.toClassType(newType);
		matcher.init(newType);
		String key = keyMethod(c, "<>", a);
		if (this.symbolCacheMap1.containsKey(key)) {
			return this.symbolCacheMap1.get(key);
		}
		Functor[] list = getConstructors(newType);
		Functor matched = null;
		for (int i = list.length - 1; i >= 0; i--) {
			Functor f = list[i];
			matcher.reset();
			if (accept(matcher, f, a)) {
				this.symbolCacheMap1.put(key, f);
				return f;
			}
			if (matched == null && match(matcher, f, a)) {
				matched = f;
			}
		}
		if (matched != null) {
			this.symbolCacheMap1.put(key, matched);
			return matched;
		}
		symbolCacheMap1.put(key, null);
		return null;
	}

	/** Field */

	public final Functor getStaticGetter(TypeMatcher matcher, Class<?> c, String name) {
		String key = c.getName() + ":*" + name;
		matcher.init(null);
		if (this.symbolCacheMap1.containsKey(key)) {
			return this.symbolCacheMap1.get(key);
		}
		try {
			Field f = c.getField(name);
			if (Java.isStatic(f)) {
				Functor ff = new Functor(Syntax.Getter, f);
				this.symbolCacheMap1.put(key, ff);
				return ff;
			}
		} catch (NoSuchFieldException | SecurityException e) {
		}
		this.symbolCacheMap1.put(key, null);
		return null;
	}

	public final Functor getGetter(TypeMatcher matcher, Type recvType, String name) {
		Class<?> c = Java.toClassType(recvType);
		String key = c.getName() + ":@" + name;
		matcher.init(recvType);
		if (this.symbolCacheMap1.containsKey(key)) {
			return this.symbolCacheMap1.get(key);
		}
		while (c != null) {
			Functor f = getGetterImpl(c, name);
			if (f != null) {
				this.symbolCacheMap1.put(key, f);
				return f;
			}
			c = c.getSuperclass();
		}
		this.symbolCacheMap1.put(key, null);
		return null;
	}

	private Functor getGetterImpl(Class<?> c, String name) {
		Functor[] l = getClassMethods(c);
		for (Functor f : l) {
			if (name.equals(f.getName()) && isGetterFunctor(f)) {
				return f;
			}
		}
		Functor ff = null;
		try {
			ff = new Functor(Syntax.Getter, c.getDeclaredField(name));
		} catch (NoSuchFieldException | SecurityException e) {
		}
		return ff;
	}

	private boolean isGetterFunctor(Functor f) {
		if (f.ref instanceof Method) {
			return Java.isGetterMethod(((Method) f.ref));
		}
		return false;
	}

	public final Functor getSetter(TypeMatcher matcher, Type recvType, String name) {
		matcher.init(recvType);
		Class<?> c = Java.toClassType(recvType);
		String key = c.getName() + ":^" + name;
		if (this.symbolCacheMap1.containsKey(key)) {
			return this.symbolCacheMap1.get(key);
		}
		while (c != null) {
			Functor f = getSetterImpl(c, name);
			if (f != null) {
				this.symbolCacheMap1.put(key, f);
				return f;
			}
			c = c.getSuperclass();
		}
		this.symbolCacheMap1.put(key, null);
		return null;
	}

	private Functor getSetterImpl(Class<?> c, String name) {
		Functor[] l = getClassMethods(c);
		for (Functor f : l) {
			if (name.equals(f.getName()) && isSetterFunctor(f)) {
				return f;
			}
		}
		Functor ff = null;
		try {
			ff = new Functor(Syntax.Setter, c.getDeclaredField(name));
		} catch (NoSuchFieldException | SecurityException e) {
		}
		return ff;
	}

	private boolean isSetterFunctor(Functor f) {
		if (f.ref instanceof Method) {
			return Java.isSetterMethod(((Method) f.ref));
		}
		return false;
	}

	//

	public Functor[] getMethods(Class<?> c, String name) {
		String key = c.getName() + "::" + name;
		Functor[] list = symbolCacheMap.get(key);
		if (list != null) {
			return list;
		}
		UList<Functor> l = new UList<>(new Functor[8]);
		findMethod(c, name, l);
		list = l.compactArray();
		symbolCacheMap.put(key, list);
		return list;
	}

	public Functor[] getMethods(Type c, String name) {
		return getMethods(Java.toClassType(c), name);
	}

	private void findMethod(Class<?> c, String name, UList<Functor> l) {
		Class<?> cur = c;
		while (cur != null) {
			Functor[] fs = getClassMethods(cur);
			for (int i = fs.length - 1; i >= 0; i--) {
				Functor f = fs[i];
				if (name.equals(f.getName())) {
					l.add(f);
				}
			}
			cur = cur.getSuperclass();
		}
		for (Method m : c.getDeclaredMethods()) {
			if (Java.isPublic(m) && name.equals(m.getName())) {
				l.add(new Functor(Syntax.Method, m));
			}
		}
	}

	public final Functor getMethod(TypeMatcher matcher, Type recvType, String name, Type[] a) {
		assert (a[0] == recvType);
		Class<?> c = Java.toClassType(recvType);
		matcher.init(recvType);
		String key = keyMethod(c, name, a);
		if (this.symbolCacheMap1.containsKey(key)) {
			return this.symbolCacheMap1.get(key);
		}
		Functor[] list = this.getMethods(c, name);
		Functor matched = null;
		for (Functor f : list) {
			matcher.reset();
			if (accept(matcher, f, a)) {
				symbolCacheMap1.put(key, f);
				return f;
			}
			if (matched == null && match(matcher, f, a)) {
				matched = f;
			}
		}
		symbolCacheMap1.put(key, matched);
		return matched;
	}

	/** accept, match */

	public final static boolean accept(TypeMatcher matcher, Functor f, Type[] a) {
		if (f.size() != a.length) {
			return false;
		}
		for (int j = 0; j < a.length; j++) {
			if (!accept(matcher, f.get(j), a[j])) {
				return false;
			}
		}
		return false;
	}

	public final static boolean accept(TypeMatcher matcher, Type p, Type a) {
		if (p == a) {
			return true;
		}
		if (p instanceof Class<?> || matcher == null) {
			if (((Class<?>) p).isAssignableFrom(Java.toClassType(a))) {
				return true;
			}
			return false;
		}
		return matcher.match(p, a);
	}

	public final boolean match(TypeMatcher matcher, Functor f, Type[] a) {
		if (f.size() != a.length) {
			return false;
		}
		for (int j = 0; j < a.length; j++) {
			if (!match(matcher, f.get(j), a[j])) {
				return false;
			}
		}
		return true;
	}

	public final boolean match(TypeMatcher matcher, Type p, Type a) {
		if (accept(matcher, p, a)) {
			return true;
		}
		Functor f = this.getCast(a, p);
		return (f != null || a == Object.class);
	}

}
