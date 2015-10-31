//package konoha.asm;
//
//import java.lang.reflect.Type;
//import java.util.Arrays;
//
//import konoha.script.Functor;
//import konoha.script.Syntax;
//
//import org.objectweb.asm.Handle;
//import org.objectweb.asm.commons.GeneratorAdapter;
//
///**
// * pseudo prototype for representing indy target
// * 
// * @author skgchxngsxyz-osx
// */
//public class DynamicMember extends Prototype {
//	private Handle handle;
//
//	private static Type[] newParamTypes(int paramSize) {
//		Type[] types = new Type[paramSize];
//		Arrays.fill(types, Object.class);
//		return types;
//	}
//
//	/**
//	 * 
//	 * @param syntax
//	 * @param name
//	 * @param paramSize
//	 *            not contains receiver
//	 */
//	DynamicMember(Handle handle, String name, int paramSize) {
//		super("dummy", Object.class, name, newParamTypes(paramSize + 1));
//		this.handle = handle;
//	}
//
//	public static Functor newIndexGetter(String methodName) {
//		Syntax s = Syntax.Indexer;
//		return new Functor(s, new DynamicMember(DynamicCallSite0.bsmInstanceMethodHandle, methodName, 1));
//	}
//
//	public static Functor newIndexSetter(String methodName) {
//		Syntax s = Syntax.SetIndexer;
//		return new Functor(s, new DynamicMember(DynamicCallSite0.bsmInstanceMethodHandle, methodName, 2));
//	}
//
//	public static Functor newGetter(String fieldName) {
//		Syntax s = Syntax.Getter;
//		return new Functor(s, new DynamicMember(DynamicCallSite0.bsmGetterHandle, fieldName, 0));
//	}
//
//	public static Functor newSetter(String fieldName) {
//		Syntax s = Syntax.Setter;
//		return new Functor(s, new DynamicMember(DynamicCallSite0.bsmSetterHandle, fieldName, 1));
//	}
//
//	public static Functor newMethod(String name, int paramSize) {
//		Syntax s = Syntax.Method;
//		return new Functor(s, new DynamicMember(DynamicCallSite0.bsmInstanceMethodHandle, name, paramSize));
//	}
//
//	@Override
//	public void push(GeneratorAdapter adapter) {
//		org.objectweb.asm.Type returnType = org.objectweb.asm.Type.getType(Object.class);
//		int paramSize = this.paramTypes.length;
//		org.objectweb.asm.Type[] paramTypes = new org.objectweb.asm.Type[paramSize];
//		Arrays.fill(paramTypes, returnType);
//		adapter.invokeDynamic(this.name, org.objectweb.asm.Type.getMethodType(returnType, paramTypes).getDescriptor(), handle);
//	}
// }
