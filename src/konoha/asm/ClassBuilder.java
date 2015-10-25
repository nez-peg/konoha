package konoha.asm;

import nez.util.UList;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.FieldNode;

/**
 * wrapper class of ClassWriter
 * 
 * @author skgchxngsxyz-osx
 *
 */
public class ClassBuilder extends ClassWriter implements Opcodes {
	private final String qualifiedClassName;

	public ClassBuilder(int accessFlag, String fullyQualifiedClassName, String sourceName, Class<?> superClass, Class<?>[] interfaces) {
		super(ClassWriter.COMPUTE_FRAMES);
		this.qualifiedClassName = fullyQualifiedClassName;
		String[] interfaceNames = null;

		if (superClass == null) {
			superClass = Object.class;
		}
		if (interfaces != null) {
			final int size = interfaces.length;
			interfaceNames = new String[size];
			for (int i = 0; i < size; i++) {
				interfaceNames[i] = Type.getInternalName(interfaces[i]);
			}
		}
		this.visit(V1_7, accessFlag, this.qualifiedClassName, null, Type.getInternalName(superClass), interfaceNames);
		this.visitSource(sourceName, null);
	}

	public ClassBuilder(String fullyQualifiedClassName, String sourceName, Class<?> superClass, Class<?>[] interfaces) {
		this(ACC_PUBLIC | ACC_FINAL, fullyQualifiedClassName, sourceName, superClass, interfaces);
	}

	public String getQualifiedClassName() {
		return this.qualifiedClassName;
	}

	public Type getTypeDesc() {
		return Type.getType("L" + this.qualifiedClassName + ";");
	}

	public void addField(int acc, String name, Class<?> fieldClass, Object value) {
		FieldNode fn = new FieldNode(acc, name, Type.getDescriptor(fieldClass), null, value);
		fn.accept(this);
	}

	private UList<Object> constList = null;

	public final int poolConst(Object value) {
		if (constList == null) {
			constList = new UList<Object>(new Object[2]);
		}
		for (int i = 0; i < constList.size(); i++) {
			if (constList.ArrayValues[i] == value) {
				return i;
			}
		}
		int id = constList.size();
		constList.add(value);
		return id;
	}

	String constName(int id) {
		return "__const" + id + "__";
	}

	public void buildConstPool() {
		if (constList == null) {
			return;
		}
		for (int i = 0; i < constList.size(); i++) {
			addField(ACC_STATIC, constName(i), constList.ArrayValues[i].getClass(), null);
		}
		int poolId = ConstPools.registConstPools(constList.compactArray());
		MethodBuilder m = this.newMethodBuilder(ACC_PUBLIC | ACC_STATIC, Method.getMethod("void <clinit> ()"));
		for (int i = 0; i < constList.size(); i++) {
			m.push(poolId);
			m.push(i);
			m.invokeStatic(Type.getType(ConstPools.class), Method.getMethod("java.lang.Object get(int,int)"));
			Type fieldType = Type.getType(constList.ArrayValues[i].getClass());
			m.checkCast(fieldType);
			m.putStatic(this.getTypeDesc(), constName(i), fieldType);
		}
		m.returnValue();
		constList = null;
	}

	/**
	 * 
	 * @param accessFlag
	 *            represent for java access flag
	 * @param returnClass
	 * @param methodName
	 * @param paramClasses
	 * @return
	 */

	public MethodBuilder newMethodBuilder(int acc, Method method) {
		return new MethodBuilder(acc, method, this);
	}

	public MethodBuilder newMethodBuilder(int accessFlag, Class<?> returnClass, String methodName, Class<?>... paramClasses) {
		final int size = paramClasses.length;
		Type[] paramTypeDescs = new Type[paramClasses.length];
		for (int i = 0; i < size; i++) {
			paramTypeDescs[i] = Type.getType(paramClasses[i]);
		}
		Method method = new Method(methodName, Type.getType(returnClass), paramTypeDescs);
		return new MethodBuilder(accessFlag, method, this);
	}

	public MethodBuilder newConstructorBuilder(int accessFlag, Class<?>... paramClasses) {
		Method method = Methods.constructor(paramClasses);
		return new MethodBuilder(accessFlag, method, this);
	}

	@Override
	public String toString() {
		return this.getQualifiedClassName();
	}

}
