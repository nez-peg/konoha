package konoha.script;

import java.lang.reflect.Field;
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

	public void setFunction(Function f) {
		Reflector.setStatic(this.field, f);
	}

}
