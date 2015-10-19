package konoha.script;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import konoha.Function;

public class GlobalVariable {
	Type type;
	Class<?> varClass;
	Field field;
	Functor getter;
	Functor setter;

	GlobalVariable(Type type, Class<?> varClass) {
		this.type = type;
		this.varClass = varClass;
		try {
			this.field = varClass.getField("v");
			this.getter = new Functor(Syntax.Getter, this.field);
			this.setter = new Functor(Syntax.Setter, this.field);
		} catch (NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		}
	}

	public Type getType() {
		return this.type;
	}

	public Field getField() {
		return this.field;
	}

	public Functor getGetter() {
		return this.getter;
	}

	public Functor getSetter() {
		return this.setter;
	}

	public boolean matchFunction(TypeSystem ts, Method m) {
		if (ts.isStaticFuncType(this.type)) {
			if (m.getReturnType() != ts.getFuncReturnType(this.type))
				return false;
			Class<?>[] tp = ts.getFuncParameterTypes(this.type);
			Class<?>[] mp = m.getParameterTypes();
			if (tp.length != mp.length) {
				return false;
			}
			for (int i = 0; i < mp.length; i++) {
				if (tp[i] != mp[i]) {
					return false;
				}
			}
			return true;
		}
		return ts.isDynamicFuncType(this.type);
	}

	public void setFunction(Function f) {
		Reflector.setStatic(this.field, f);
	}

}
