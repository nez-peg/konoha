package konoha.asm;

import java.lang.annotation.Annotation;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.Stack;

import konoha.Function;
import konoha.dynamic.DynamicSite;
import konoha.script.CommonSymbols;
import konoha.script.Debug;
import konoha.script.Functor;
import konoha.script.Lang;
import konoha.script.Syntax;
import konoha.script.SyntaxTree;
import konoha.script.TypeSystem;
import nez.ast.Symbol;
import nez.util.VisitorMap;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

public abstract class ScriptCompilerAsm extends VisitorMap<TreeAsm> implements CommonSymbols {
	// private TypeSystem typeSystem;
	protected ScriptClassLoader cLoader;
	public ClassBuilder cBuilder;
	public MethodBuilder mBuilder;
	private String classPath = "konoha/runtime/";
	protected Stack<ClassBuilder> cBuilders;
	protected Stack<MethodBuilder> mBuilders;
	protected int lambdaIdentifier = 0;

	public ScriptCompilerAsm(TypeSystem typeSystem, ScriptClassLoader cLoader) {
		// super(TypedTree.class);
		// this.typeSystem = typeSystem;
		this.cLoader = cLoader;
		mBuilders = new Stack<>();
		cBuilders = new Stack<>();
	}

	public abstract void init();

	public void visit(SyntaxTree node) {
		this.find(node.getTag().toString()).acceptAsm(node);
	}

	void prepare(Functor f) {
		if (f.ref instanceof java.lang.reflect.Constructor) {
			java.lang.reflect.Constructor<?> c = (java.lang.reflect.Constructor<?>) f.ref;
			this.mBuilder.newInstance(Type.getType(c.getDeclaringClass()));
			this.mBuilder.dup();
			return;
		} else if (f.ref instanceof Prototype) {
			((Prototype) f.ref).prepare(this.mBuilder);
		}
	}

	void push(Functor f, SyntaxTree node) {
		if (f.ref instanceof java.lang.reflect.Method) {
			java.lang.reflect.Method m = (java.lang.reflect.Method) f.ref;
			if (Lang.isStatic(m)) {
				this.mBuilder.invokeStatic(Type.getType(m.getDeclaringClass()), Method.getMethod(m));
			} else if (Lang.isInterface(m)) {
				this.mBuilder.invokeInterface(Type.getType(m.getDeclaringClass()), Method.getMethod(m));
			} else {
				this.mBuilder.invokeVirtual(Type.getType(m.getDeclaringClass()), Method.getMethod(m));
			}
			if (m.getReturnType() == Object.class && node.getClassType() != Object.class) {
				this.mBuilder.checkCast(Type.getType(node.getClassType()));
			}
		} else if (f.ref instanceof java.lang.reflect.Field) {
			Field fld = (Field) f.ref;
			Type owner = Type.getType(fld.getDeclaringClass());
			String name = fld.getName();
			Type fieldType = Type.getType(fld.getType());
			if (f.syntax == Syntax.Getter) {
				if (Lang.isStatic(fld)) {
					this.mBuilder.getStatic(owner, name, fieldType);
				} else {
					this.mBuilder.getField(owner, name, fieldType);
				}
			} else {
				java.lang.reflect.Type ret = f.getReturnType();
				if (Lang.isStatic(fld)) {
					if (ret == long.class || ret == double.class) {
						this.mBuilder.dup2();
					} else if (ret != void.class) {
						this.mBuilder.dup();
					}
					this.mBuilder.putStatic(owner, name, fieldType);
				} else {
					if (ret == long.class || ret == double.class) {
						this.mBuilder.dup2X1();
					} else if (ret != void.class) {
						this.mBuilder.dupX1();
					}
					this.mBuilder.putField(owner, name, fieldType);
				}
			}
		} else if (f.ref instanceof java.lang.reflect.Constructor) {
			java.lang.reflect.Constructor<?> c = (java.lang.reflect.Constructor<?>) f.ref;
			this.mBuilder.invokeConstructor(Type.getType(c.getDeclaringClass()), Method.getMethod(c));
		} else if (f.ref instanceof Prototype) {
			((Prototype) f.ref).push(this.mBuilder);
		} else if (f.ref instanceof DynamicSite) {
			if (f.syntax == Syntax.Setter) {
				this.mBuilder.dupX1();
			} else if (f.syntax == Syntax.SetIndexer) {
				this.mBuilder.dupX2();
			}
			DynamicSite site = (DynamicSite) f.ref;
			String desc = site.type().toMethodDescriptorString();
			// System.out.println("InvokeDynamic: " + desc);
			Type[] paramTypes = { Type.getType(MethodHandles.Lookup.class), Type.getType(String.class), Type.getType(MethodType.class) };
			Method methodDesc = new Method("bootstrap", Type.getType(CallSite.class), paramTypes);
			Handle handle = new Handle(Opcodes.H_INVOKESTATIC, Type.getType(site.getClass()).getInternalName(), "bootstrap", methodDesc.getDescriptor());
			this.mBuilder.invokeDynamic(site.encodeName(), desc, handle);
		}
	}

	protected void asmConst(SyntaxTree node) {
		// assert (node.hint() == Hint.Constant);
		Object v = node.getValue();
		if (v instanceof String) {
			this.mBuilder.push((String) v);
		} else if (v instanceof Integer || v instanceof Character || v instanceof Byte) {
			this.mBuilder.push(((Number) v).intValue());
		} else if (v instanceof Double) {
			this.mBuilder.push(((Double) v).doubleValue());
		} else if (v instanceof Boolean) {
			this.mBuilder.push(((Boolean) v).booleanValue());
		} else if (v instanceof Long) {
			this.mBuilder.push(((Long) v).longValue());
		} else if (v instanceof Class<?>) {
			this.mBuilder.push(Type.getType((Class<?>) v));
		} else {
			if (v != null) {
				int id = this.cBuilder.poolConst(v);
				String name = this.cBuilder.constName(id);
				this.mBuilder.getStatic(cBuilder.getTypeDesc(), name, Type.getType(v.getClass()));
			} else {
				this.mBuilder.pushNull();
			}
		}
	}

	/* typechecker hints */

	public void visitDefaultValue(SyntaxTree node) {
		Class<?> t = node.getClassType();
		if (t != void.class) {
			if (t == int.class || t == short.class || t == char.class || t == byte.class) {
				this.mBuilder.push(0);
				return;
			}
			if (t == boolean.class) {
				this.mBuilder.push(false);
				return;
			}
			if (t == double.class) {
				this.mBuilder.push(0.0);
				return;
			}
			if (t == long.class) {
				this.mBuilder.push(0L);
				return;
			}
			if (t == float.class) {
				this.mBuilder.push(0.0f);
				return;
			}
			this.mBuilder.pushNull();
			return;
		}
	}

	private void visitDownCastHint(SyntaxTree node) {
		visit(node.get(_expr));
		this.mBuilder.checkCast(Type.getType(node.getClassType()));
	}

	private void unbox(java.lang.reflect.Type type, Class<?> clazz) {
		// TODO
	}

	/* class */

	public void openClass(String name) {
		if (this.cBuilder != null) {
			cBuilders.push(this.cBuilder);
		}
		this.cBuilder = new ClassBuilder(name, null, null, null);
	}

	public void openClass(String name, Class<?> superClass, Class<?>... interfaces) {
		if (this.cBuilder != null) {
			cBuilders.push(this.cBuilder);
		}
		this.cBuilder = new ClassBuilder(name, null, superClass, interfaces);
	}

	public void openClass(int acc, String name, Class<?> superClass, Class<?>... interfaces) {
		if (this.cBuilder != null) {
			cBuilders.push(this.cBuilder);
		}
		this.cBuilder = new ClassBuilder(acc, name, null, superClass, interfaces);
	}

	public Class<?> closeClass() {
		this.cBuilder.buildConstPool();
		Class<?> c = cLoader.definedAndLoadClass(this.cBuilder.getQualifiedClassName(), cBuilder.toByteArray());
		if (cBuilders.isEmpty()) {
			this.cBuilder = null;
		} else {
			this.cBuilder = cBuilders.pop();
		}
		return c;
	}

	public void openInterface(String name, Class<?>[] superInterfaces) {
		if (this.cBuilder != null) {
			cBuilders.push(this.cBuilder);
		}
		this.cBuilder = new ClassBuilder(Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT, name, null, null, superInterfaces);
	}

	public Class<?> closeInterface() {
		return this.closeClass();
	}

	/* global variable */

	public Class<?> compileGlobalVariableClass(Class<?> t, String name) {
		this.openClass("G_" + name);
		this.cBuilder.addField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "v", t, null);
		return this.closeClass();
	}

	/* generate function class */
	public Class<?> compileFuncType(String cname, Class<?> returnType, Class<?>... paramTypes) {
		this.openClass(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, cname, konoha.Function.class);
		this.mBuilder = this.cBuilder.newMethodBuilder(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, returnType, "invoke", paramTypes);
		this.mBuilder.endMethod();
		Method desc = Method.getMethod("void <init> ()");
		this.mBuilder = this.cBuilder.newMethodBuilder(Opcodes.ACC_PUBLIC, desc);
		this.mBuilder.loadThis(); // InitMethod.visitVarInsn(ALOAD, 0);
		this.mBuilder.invokeConstructor(Type.getType(konoha.Function.class), desc);
		this.mBuilder.returnValue();
		this.mBuilder.endMethod();

		return this.closeClass();
	}

	/* generate function class */

	public Class<?> compileFunctionWrapperClass(Class<?> superClass, java.lang.reflect.Method staticMethod) {
		this.openClass("C" + staticMethod.getName() + "Wrapper" + unique, superClass);
		unique++;
		Class<?> returnType = staticMethod.getReturnType();
		Class<?>[] paramTypes = staticMethod.getParameterTypes();
		this.mBuilder = this.cBuilder.newMethodBuilder(Opcodes.ACC_PUBLIC, returnType, "invoke", paramTypes);
		int index = 1;
		for (int i = 0; i < paramTypes.length; i++) {
			Type aType = Type.getType(paramTypes[i]);
			this.mBuilder.visitVarInsn(aType.getOpcode(Opcodes.ILOAD), index);
			// this.mBuilder.loadLocal(index, aType); FIXME
			index += aType.getSize();
		}
		Type owner = Type.getType(staticMethod.getDeclaringClass());
		Method methodDesc = Method.getMethod(staticMethod);
		this.mBuilder.invokeStatic(owner, methodDesc);
		this.mBuilder.returnValue();
		this.mBuilder.endMethod();

		Method desc = Method.getMethod("void <init> ()");
		this.mBuilder = this.cBuilder.newMethodBuilder(Opcodes.ACC_PUBLIC, desc);
		this.mBuilder.loadThis(); // InitMethod.visitVarInsn(ALOAD, 0);
		this.mBuilder.invokeConstructor(Type.getType(superClass), desc);
		this.mBuilder.returnValue();
		this.mBuilder.endMethod();

		return this.closeClass();
	}

	/* static function */
	int unique = 0;

	public String nameFunctionClass(SyntaxTree node, String name) {
		String path = node.getSource().getResourceName();
		String cname = "F" + unique + "$" + name;
		unique++;
		return cname;
	}

	public Class<?> compileStaticFuncDecl(String className, SyntaxTree node) {
		this.openClass(className);
		this.cBuilder.visitSource(node.getSource().getResourceName(), null);
		unique++;
		visit(node);
		return this.closeClass();
	}

	protected Class<?>[] setFreeVariables(SyntaxTree node) {
		SyntaxTree list = node.get(_list);
		Class<?> types[] = new Class<?>[list.size()];
		int i = 0;
		for (SyntaxTree fv : list) {
			String name = fv.toText();
			cBuilder.addField(Opcodes.ACC_PUBLIC, "fv$" + name, fv.getClassType(), null);
			types[i] = fv.getClassType();
		}
		return types;
	}

	public Class<?> compileLambda(SyntaxTree node) {
		openClass("Lambda$" + lambdaIdentifier, Function.class);
		Class<?>[] freeVarTypes = setFreeVariables(node);
		SyntaxTree args = node.get(_param);
		String name = "f";
		lambdaIdentifier++;
		Class<?> returnType = args.getClassType();
		Class<?>[] paramTypes = new Class<?>[args.size()];
		for (int i = 0; i < paramTypes.length; i++) {
			paramTypes[i] = args.get(i).getClassType();
		}
		mBuilders.push(mBuilder);
		mBuilder = cBuilder.newMethodBuilder(Opcodes.ACC_PUBLIC, returnType, name, paramTypes);
		mBuilder.enterScope();
		for (SyntaxTree arg : args) {
			mBuilder.defineArgument(arg.getText(_name, null), arg.getClassType());
		}
		visit(node.get(_body));
		mBuilder.exitScope();
		if (returnType != void.class) {
			visitDefaultValue(args);
		}
		mBuilder.returnValue();
		mBuilder.endMethod();

		/* Constructor */
		mBuilder = cBuilder.newConstructorBuilder(Opcodes.ACC_PUBLIC, freeVarTypes);
		mBuilder.loadThis();
		mBuilder.invokeConstructor(Type.getType(Function.class), Method.getMethod("void <init> ()"));
		int i = 0;
		for (SyntaxTree fv : node.get(_list)) {
			mBuilder.loadThis();
			mBuilder.loadArg(i);
			mBuilder.putField(cBuilder.getTypeDesc(), "fv$" + fv.toText(), Type.getType(freeVarTypes[i]));
			i++;
		}
		mBuilder.returnValue();
		mBuilder.endMethod();

		mBuilder = mBuilders.pop();
		return closeClass();
	}

	/* class */
	public Class<?> compileClass(SyntaxTree node) {
		String name = node.getText(_name, null);
		SyntaxTree implNode = node.get(_impl, null);
		Class<?> superClass = null;
		if (node.has(_super)) {
			superClass = node.get(_super).getClassType();
		}
		Class<?>[] implClasses = null;
		if (implNode != null) {
			implClasses = new Class<?>[implNode.size()];
			for (int i = 0; i < implNode.size(); i++) {
				implClasses[i] = implNode.get(i).getClassType();
			}
		}
		openClass(Opcodes.ACC_PUBLIC, name, superClass, implClasses);
		cBuilder.visitSource(node.getSource().getResourceName(), null);
		visit(node);
		return closeClass();
	}

	/* interface */
	public Class<?> compileInterface(SyntaxTree node) {
		String name = node.getText(_name, null);
		SyntaxTree superNode = node.get(_super, null);
		Class<?>[] superInterfaces = null;
		if (superNode != null) {
			superInterfaces = new Class<?>[superNode.size()];
			int i = 0;
			for (SyntaxTree n : superNode) {
				superInterfaces[i] = n.getClassType();
				i++;
			}
		}
		openInterface(name, superInterfaces);
		cBuilder.visitSource(node.getSource().getResourceName(), null);
		visit(node);
		return closeInterface();
	}

	/* Annotation */
	public Class<?> compileAnnotation(SyntaxTree node) {
		String name = node.getText(_name, null);
		Class<?>[] superInterfaces = { Annotation.class };
		openInterface(name, superInterfaces);
		cBuilder.visitSource(node.getSource().getResourceName(), null);
		visit(node);
		return closeInterface();
	}

	protected boolean has(Symbol tag, SyntaxTree node) {
		for (SyntaxTree sub : node) {
			if (sub.is(tag)) {
				return true;
			}
		}
		return false;
	}

	void visitStatementAsBlock(SyntaxTree node) {
		if (!node.is(_Block)) {
			visit(node);
			if (node.getType() != void.class) {
				mBuilder.pop(node.getClassType());
			}
		} else {
			visitBlock(node);
		}
	}

	void visitBlock(SyntaxTree node) {
		mBuilder.enterScope();
		for (SyntaxTree stmt : node) {
			mBuilder.setLineNum(node.getLineNum()); // FIXME
			visit(stmt);
			if (stmt.getType() == null) {
				// TypeError
			} else if (stmt.getType() != void.class) {
				mBuilder.pop(stmt.getClassType());
			}
		}
		mBuilder.exitScope();
	}

	protected int switchUnique = 0;

	void pushArray(Class<?> elementType, SyntaxTree node) {
		this.mBuilder.push(node.size());
		this.mBuilder.newArray(Type.getType(elementType));
		int index = 0;
		for (SyntaxTree sub : node) {
			this.mBuilder.dup();
			this.mBuilder.push(index);
			visit(sub);
			this.mBuilder.arrayStore(Type.getType(elementType));
			index++;
		}
	}

	/* code copied from libzen */

	void TRACE(String fmt, Object... args) {
		Debug.TRACE(fmt, args);
	}

	void TODO(String fmt, Object... args) {
		Debug.TODO(fmt, args);
	}

	void DEBUG(String fmt, Object... args) {
		Debug.DEBUG(fmt, args);
	}

}
