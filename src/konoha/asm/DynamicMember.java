package konoha.asm;

import java.lang.reflect.Type;
import java.util.Arrays;

import konoha.script.Functor;
import konoha.script.Syntax;

import org.objectweb.asm.Handle;
import org.objectweb.asm.commons.GeneratorAdapter;

/**
 * pseudo prototype for representing indy target
 * 
 * @author skgchxngsxyz-osx
 */
public class DynamicMember extends Prototype {
	private final Syntax syntax;

	private static Type[] newParamTypes(int paramSize) {
		Type[] types = new Type[paramSize];
		Arrays.fill(types, Object.class);
		return types;
	}

	/**
	 * 
	 * @param syntax
	 * @param name
	 * @param paramSize
	 *            not contains receiver
	 */
	DynamicMember(Syntax syntax, String name, int paramSize) {
		/**
		 * param types contains receiver
		 */
		super("dummy", Object.class, name, newParamTypes(paramSize + 1));
		this.syntax = syntax;
	}

	public static Functor newIndexGetter(String methodName) {
		Syntax s = Syntax.Indexer;
		return new Functor(s, new DynamicMember(s, methodName, 1));
	}

	public static Functor newIndexSetter(String methodName) {
		Syntax s = Syntax.SetIndexer;
		return new Functor(s, new DynamicMember(s, methodName, 2));
	}

	public static Functor newGetter(String fieldName) {
		Syntax s = Syntax.Getter;
		return new Functor(s, new DynamicMember(s, fieldName, 0));
	}

	public static Functor newSetter(String fieldName) {
		Syntax s = Syntax.Setter;
		return new Functor(s, new DynamicMember(s, fieldName, 1));
	}

	@Override
	public void push(GeneratorAdapter adapter) {
		Handle handle = null;
		if (this.syntax == Syntax.Indexer || this.syntax == Syntax.SetIndexer) {
			handle = DynamicCallSite.bsmInstanceMethodHandle;
		} else if (this.syntax == Syntax.Getter) {
			handle = DynamicCallSite.bsmGetterHandle;
		} else if (this.syntax == Syntax.Setter) {
			handle = DynamicCallSite.bsmSetterHandle;
		} else {
			throw new RuntimeException("unsupported dynamic invocation: " + this.syntax);
		}

		org.objectweb.asm.Type returnType = org.objectweb.asm.Type.getType(Object.class);
		int paramSize = this.paramTypes.length;
		org.objectweb.asm.Type[] paramTypes = new org.objectweb.asm.Type[paramSize];
		Arrays.fill(paramTypes, returnType);
		adapter.invokeDynamic(this.name, org.objectweb.asm.Type.getMethodType(returnType, paramTypes).getDescriptor(), handle);
	}
}
