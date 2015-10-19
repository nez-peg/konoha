package konoha.asm;

import konoha.script.Java;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

public abstract class Prototype {
	String cname;
	java.lang.reflect.Type returnType;
	String name;
	java.lang.reflect.Type[] paramTypes;

	Type owner;
	Method desc;

	Prototype(String cname, java.lang.reflect.Type returnType, String name, java.lang.reflect.Type[] paramTypes) {
		this.cname = cname;
		this.returnType = returnType;
		this.name = name;
		this.paramTypes = paramTypes;
		//
		this.owner = Type.getType("L" + cname + ";");
		// System.out.println("owner" + owner + " / cname=" + cname);
		this.desc = method(returnType, name, paramTypes);
	}

	public String getClassName() {
		return cname;
	}

	public String getName() {
		return name;
	}

	public java.lang.reflect.Type getReturnType() {
		return this.returnType;
	}

	public java.lang.reflect.Type[] getParameterTypes() {
		return this.paramTypes;
	}

	public void prepare(GeneratorAdapter a) {
	}

	public abstract void push(GeneratorAdapter a);

	public static Method method(java.lang.reflect.Type returnType, String methodName, java.lang.reflect.Type[] paramTypes) {
		Type[] paramTypeDescs = new Type[paramTypes.length];
		for (int i = 0; i < paramTypes.length; i++) {
			paramTypeDescs[i] = Type.getType(Java.toClassType(paramTypes[i]));
		}
		return new Method(methodName, Type.getType(Java.toClassType(returnType)), paramTypeDescs);
	}

	@Override
	public String toString() {
		return owner + "/" + desc;
	}

}

class FunctionPrototype extends Prototype {

	public FunctionPrototype(String cname, java.lang.reflect.Type returnType, String name, java.lang.reflect.Type[] paramTypes) {
		super(cname, returnType, name, paramTypes);
	}

	@Override
	public void push(GeneratorAdapter a) {
		a.invokeStatic(owner, desc);
	}
}
