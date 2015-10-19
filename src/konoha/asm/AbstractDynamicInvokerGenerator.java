/**
 * Sample from http://niklasschlimm.blogspot.jp/2012/02/java-7-complete-invokedynamic-example.html
 */

package konoha.asm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;

import konoha.hack.Person;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public abstract class AbstractDynamicInvokerGenerator implements Opcodes {

	public byte[] dump(String dynamicInvokerClassName, String dynamicLinkageClassName, String bootstrapMethodName, String targetMethodDescriptor) throws Exception {

		ClassWriter cw = new ClassWriter(0);
		FieldVisitor fv;
		MethodVisitor mv;
		AnnotationVisitor av0;

		cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER, dynamicInvokerClassName, null, "java/lang/Object", null);

		{
			mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
			mv.visitInsn(RETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
			mv.visitCode();
			MethodType mt = MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class);
			Handle bootstrap = new Handle(Opcodes.H_INVOKESTATIC, dynamicLinkageClassName, bootstrapMethodName, mt.toMethodDescriptorString());
			int maxStackSize = addMethodParameters(mv);
			mv.visitInvokeDynamicInsn("runCalculation", targetMethodDescriptor, bootstrap);
			mv.visitInsn(RETURN);
			mv.visitMaxs(maxStackSize, 1);
			mv.visitEnd();
		}
		cw.visitEnd();

		return cw.toByteArray();
	}

	protected abstract int addMethodParameters(MethodVisitor mv);

}

class SimpleDynamicInvokerGenerator extends AbstractDynamicInvokerGenerator {

	@Override
	protected int addMethodParameters(MethodVisitor mv) {
		return 0;
	}

	public static void main(String[] args) throws IOException, Exception {
		String dynamicInvokerClassName = "com/schlimm/bytecode/SimpleDynamicInvoker";
		FileOutputStream fos = new FileOutputStream(new File("target/classes/" + dynamicInvokerClassName + ".class"));
		fos.write(new SimpleDynamicInvokerGenerator().dump(dynamicInvokerClassName, "com/schlimm/bytecode/invokedynamic/linkageclasses/SimpleDynamicLinkageExample", "bootstrapDynamic", "()V"));
	}

	void f() throws Throwable {
		Object x, y;
		String s;
		int i;
		MethodType mt;
		MethodHandle mh;
		MethodHandles.Lookup lookup = MethodHandles.lookup();
		// mt is (char,char)String
		mt = MethodType.methodType(String.class, char.class, char.class);
		mh = lookup.findVirtual(String.class, "replace", mt);
		s = (String) mh.invokeExact("daddy", 'd', 'n');
		// invokeExact(Ljava/lang/String;CC)Ljava/lang/String;
		// assertEquals(s, "nanny");
		// weakly typed invocation (using MHs.invoke)
		s = (String) mh.invokeWithArguments("sappy", 'p', 'v');
		// assertEquals(s, "savvy");
		// mt is (Object[])List
		mt = MethodType.methodType(java.util.List.class, Object[].class);
		mh = lookup.findStatic(java.util.Arrays.class, "asList", mt);
		// assert (mh.isVarargsCollector());
		x = mh.invoke("one", "two");
		// invoke(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;
		// assertEquals(x, java.util.Arrays.asList("one", "two"));
		// mt is (Object,Object,Object)Object
		mt = MethodType.genericMethodType(3);
		mh = mh.asType(mt);
		x = mh.invokeExact((Object) 1, (Object) 2, (Object) 3);
		// invokeExact(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
		// assertEquals(x, java.util.Arrays.asList(1, 2, 3));
		// mt is ()int
		mt = MethodType.methodType(int.class);
		mh = lookup.findVirtual(java.util.List.class, "size", mt);
		i = (int) mh.invokeExact(java.util.Arrays.asList(1, 2, 3));
		// invokeExact(Ljava/util/List;)I
		// assert (i == 3);
		mt = MethodType.methodType(void.class, String.class);
		mh = lookup.findVirtual(java.io.PrintStream.class, "println", mt);
		mh.invokeExact(System.out, "Hello, world.");
		// invokeExact(Ljava/io/PrintStream;Ljava/lang/String;)V

	}

	public final static Object testGetter(Object self, String name) {
		MethodHandles.Lookup lookup = MethodHandles.lookup();
		MethodHandle mh = null;
		try {
			Field f = self.getClass().getField(name);
			mh = lookup.unreflectGetter(f);
			System.out.println("mh=" + mh + "," + mh.asFixedArity());
			System.out.println("mh=" + mh.type() + ", " + mh.type().toMethodDescriptorString());
		} catch (NoSuchFieldException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		try {
			return mh.invoke((Person) self);
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Deprecated
	public final static int age(Person p) {
		return p.age;
	}

}

class SimpleDynamicLinkageExample {

	private static MethodHandle sayHello;

	private static void sayHello() {
		System.out.println("There we go!");
	}

	public static CallSite bootstrapDynamic(MethodHandles.Lookup caller, String name, MethodType type) throws NoSuchMethodException, IllegalAccessException {
		MethodHandles.Lookup lookup = MethodHandles.lookup();
		Class<?> thisClass = lookup.lookupClass(); // (who am I?)
		sayHello = lookup.findStatic(thisClass, "sayHello", MethodType.methodType(void.class));
		return new ConstantCallSite(sayHello.asType(type));
	}

}