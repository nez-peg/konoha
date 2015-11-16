package konoha.asm;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

import konoha.Function;
import konoha.dynamic.DynamicSite;
import konoha.main.ConsoleUtils;
import konoha.script.CommonSymbols;
import konoha.script.Debug;
import konoha.script.Functor;
import konoha.script.Lang;
import konoha.script.Syntax;
import konoha.script.SyntaxTree;
import konoha.script.TypeSystem;
import nez.ast.Symbol;
import nez.ast.TreeVisitor2;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

public class ScriptCompilerAsm extends TreeVisitor2<TreeAsm> implements CommonSymbols {
	// private TypeSystem typeSystem;
	private ScriptClassLoader cLoader;
	private ClassBuilder cBuilder;
	private MethodBuilder mBuilder;
	private String classPath = "konoha/runtime/";
	private Stack<ClassBuilder> cBuilders;
	private Stack<MethodBuilder> mBuilders;
	private int lambdaIdentifier = 0;

	public ScriptCompilerAsm(TypeSystem typeSystem, ScriptClassLoader cLoader) {
		// super(TypedTree.class);
		// this.typeSystem = typeSystem;
		this.cLoader = cLoader;
		mBuilders = new Stack<>();
		cBuilders = new Stack<>();
		init(new Undefined());
	}

	private void visit(SyntaxTree node) {
		this.find(node).acceptAsm(node);
	}

	public class Undefined implements TreeAsm {
		@Override
		public void acceptAsm(SyntaxTree node) {
			ConsoleUtils.println(node.formatSourceMessage("error", "unsupproted in ScriptCompiler #" + node));
			visitDefaultValue(node);
		}
	}

	public class _Functor extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			prepare(node.getFunctor());
			for (SyntaxTree sub : node) {
				visit(sub);
			}
			push(node.getFunctor(), node);
		}
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
			if (f.syntax == Syntax.Setter || f.syntax == Syntax.SetIndexer) {
				this.mBuilder.dupX1();
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

	public class Const extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			asmConst(node);
		}
	}

	private void asmConst(SyntaxTree node) {
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

	private void visitDefaultValue(SyntaxTree node) {
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

	public class FuncDecl extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			SyntaxTree nameNode = node.get(_name);
			SyntaxTree args = node.get(_param);
			String name = nameNode.toText();
			Class<?> returnType = nameNode.getClassType();
			Class<?>[] paramTypes = new Class<?>[args.size()];
			for (int i = 0; i < paramTypes.length; i++) {
				paramTypes[i] = args.get(i).getClassType();
			}
			mBuilder = cBuilder.newMethodBuilder(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, returnType, name, paramTypes);
			mBuilder.enterScope();
			for (SyntaxTree arg : args) {
				mBuilder.defineArgument(arg.getText(_name, null), arg.getClassType());
			}
			visit(node.get(_body));
			mBuilder.exitScope();
			if (returnType != void.class) {
				visitDefaultValue(nameNode);
			}
			mBuilder.returnValue();
			mBuilder.endMethod();
		}
	}

	private Class<?>[] setFreeVariables(SyntaxTree node) {
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

	public class Lambda extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			Class<?> lambdaClass = compileLambda(node);
			mBuilder.newInstance(Type.getType(lambdaClass));
			mBuilder.dup();
			SyntaxTree fvList = node.get(_list);
			String argDesc = "";
			for (SyntaxTree fv : fvList) {
				VarEntry entry = mBuilder.getVar(fv.toText());
				mBuilder.loadFromVar(entry);
				argDesc += Type.getType(fv.getClassType()).getClassName();
				if (fv != fvList.get(fvList.size() - 1)) {
					argDesc += ",";
				}
			}
			mBuilder.invokeConstructor(Type.getType(lambdaClass), Method.getMethod("void <init> (" + argDesc + ")"));
		}
	}

	/* class */
	// public Class<?> compileClass(SyntaxTree node) {
	// String name = node.getText(_name, null);
	// SyntaxTree implNode = node.get(_impl, null);
	// SyntaxTree bodyNode = node.get(_body, null);
	// Class<?> superClass = null;
	// if (node.has(_super)) {
	// superClass = typeSystem.getType(classPath + node.getText(_super,
	// null)).getClass();
	// }
	// Class<?>[] implClasses = null;
	// if (implNode != null) {
	// implClasses = new Class<?>[implNode.size()];
	// for (int i = 0; i < implNode.size(); i++) {
	// implClasses[i] = typeSystem.getType(classPath + implNode.getText(i,
	// null)).getClass();
	// }
	// }
	// openClass(name, superClass, implClasses);
	// this.cBuilder.visitSource(node.getSource().getResourceName(), null);
	// for (SyntaxTree n : bodyNode) {
	// visit(n);
	// }
	// return closeClass();
	// }
	//
	// public class ClassDecl extends Undefined {
	// @Override
	// public void acceptAsm(SyntaxTree node) {
	// String name = node.getText(_name, null);
	// SyntaxTree implNode = node.get(_impl, null);
	// SyntaxTree bodyNode = node.get(_body, null);
	// Class<?> superClass = null;
	// if (node.has(_super)) {
	// superClass = typeSystem.getType(classPath + node.getText(_super,
	// null)).getClass();
	// }
	// Class<?>[] implClasses = null;
	// if (implNode != null) {
	// implClasses = new Class<?>[implNode.size()];
	// for (int i = 0; i < implNode.size(); i++) {
	// implClasses[i] = typeSystem.getType(classPath + implNode.getText(i,
	// null)).getClass();
	// }
	// }
	// openClass(name, superClass, implClasses);
	// cBuilder.visitSource(node.getSource().getResourceName(), null);
	// for (SyntaxTree n : bodyNode) {
	// visit(n);
	// }
	// closeClass();
	// }
	// }
	//
	// public class Constructor extends Undefined {
	// @Override
	// public void acceptAsm(SyntaxTree node) {
	// SyntaxTree args = node.get(_param);
	// Class<?>[] paramClasses = new Class<?>[args.size()];
	// for (int i = 0; i < args.size(); i++) {
	// paramClasses[i] = args.get(i).getClassType();
	// }
	// mBuilder = cBuilder.newConstructorBuilder(Opcodes.ACC_PUBLIC,
	// paramClasses);
	// mBuilder.enterScope();
	// for (SyntaxTree arg : args) {
	// mBuilder.defineArgument(arg.getText(_name, null), arg.getClassType());
	// }
	// visit(node.get(_body));
	// mBuilder.exitScope();
	// mBuilder.loadThis();
	// mBuilder.returnValue();
	// mBuilder.endMethod();
	// }
	// }
	//
	// public class FieldDecl extends Undefined {
	// @Override
	// public void acceptAsm(SyntaxTree node) {
	// // TODO
	// // TypedTree list = node.get(_list);
	// // for (TypedTree field : list) {
	// // cBuilder.addField(Opcodes.ACC_PUBLIC, field.getText(_name, null),
	// // this.typeof(field), );
	// // }
	// }
	// }
	//
	// public class MethodDecl extends Undefined {
	// @Override
	// public void acceptAsm(SyntaxTree node) {
	// // TODO
	// }
	// }

	private void visitStatementAsBlock(SyntaxTree node) {
		if (!node.is(_Block)) {
			visit(node);
			if (node.getType() != void.class) {
				mBuilder.pop(node.getClassType());
			}
		} else {
			visitBlock(node);
		}
	}

	private void visitBlock(SyntaxTree node) {
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

	public class Block extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			visitBlock(node);
		}
	}

	public class BlockExpression extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			int size = node.size();
			if (size == 0) {
				visitDefaultValue(node);
			} else {
				mBuilder.enterScope();
				for (int i = 0; i < size; i++) {
					SyntaxTree stmt = node.get(i);
					mBuilder.setLineNum(node.getLineNum()); // FIXME
					visit(stmt);
					assert (stmt.getType() != null);
					if (stmt.getType() != void.class && i < size - 1) {
						mBuilder.pop(stmt.getClassType());
					}
				}
				mBuilder.exitScope();
			}
		}
	}

	public class If extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			visit(node.get(_cond));
			mBuilder.push(true);

			Label elseLabel = mBuilder.newLabel();
			Label mergeLabel = mBuilder.newLabel();

			mBuilder.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.NE, elseLabel);

			// then
			visitStatementAsBlock(node.get(_then));
			mBuilder.goTo(mergeLabel);

			// else
			mBuilder.mark(elseLabel);
			if (node.size() > 2) {
				visitStatementAsBlock(node.get(_else));
			}

			// merge
			mBuilder.mark(mergeLabel);
		}
	}

	public class Conditional extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			visit(node.get(_cond));
			mBuilder.push(true);

			Label elseLabel = mBuilder.newLabel();
			Label mergeLabel = mBuilder.newLabel();

			mBuilder.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.NE, elseLabel);

			// then
			visit(node.get(_then));
			mBuilder.goTo(mergeLabel);

			// else
			mBuilder.mark(elseLabel);
			if (node.size() > 2) {
				visit(node.get(_else));
			}

			// merge
			mBuilder.mark(mergeLabel);
		}
	}

	public class While extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			Label beginLabel = mBuilder.newLabel();
			Label condLabel = mBuilder.newLabel();
			Label breakLabel = mBuilder.newLabel();
			mBuilder.getLoopLabels().push(new Pair<Label, Label>(breakLabel, condLabel));

			mBuilder.goTo(condLabel);

			// Block
			mBuilder.mark(beginLabel);
			visitStatementAsBlock(node.get(_body));

			// Condition
			mBuilder.mark(condLabel);
			visit(node.get(_cond));
			mBuilder.push(true);

			mBuilder.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.EQ, beginLabel);
			mBuilder.mark(breakLabel);
			mBuilder.getLoopLabels().pop();
		}
	}

	public class DoWhile extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			Label beginLabel = mBuilder.newLabel();
			Label continueLabel = mBuilder.newLabel();
			Label breakLabel = mBuilder.newLabel();
			mBuilder.getLoopLabels().push(new Pair<Label, Label>(breakLabel, continueLabel));

			// Do
			mBuilder.mark(beginLabel);
			visitStatementAsBlock(node.get(_body));

			// Condition
			mBuilder.mark(continueLabel);
			visit(node.get(_cond));
			mBuilder.push(true);

			mBuilder.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.EQ, beginLabel);
			mBuilder.mark(breakLabel);
			mBuilder.getLoopLabels().pop();
		}
	}

	public class For extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			Label beginLabel = mBuilder.newLabel();
			Label condLabel = mBuilder.newLabel();
			Label breakLabel = mBuilder.newLabel();
			Label continueLabel = mBuilder.newLabel();
			mBuilder.getLoopLabels().push(new Pair<Label, Label>(breakLabel, continueLabel));

			// Initialize
			if (node.has(_init)) {
				visitStatementAsBlock(node.get(_init));
			}

			mBuilder.goTo(condLabel);

			// Block
			mBuilder.mark(beginLabel);
			if (node.has(_body)) {
				visitStatementAsBlock(node.get(_body));
			}
			mBuilder.mark(continueLabel);
			if (node.has(_iter)) {
				visitStatementAsBlock(node.get(_iter));
			}
			// if (node.get(_iter).getType() != Type.VOID_TYPE) {
			// mBuilder.pop();
			// }

			// Condition
			mBuilder.mark(condLabel);
			visit(node.get(_cond));
			mBuilder.push(true);
			mBuilder.ifCmp(Type.BOOLEAN_TYPE, MethodBuilder.EQ, beginLabel);
			mBuilder.mark(breakLabel);
			mBuilder.getLoopLabels().pop();
		}
	}

	private boolean has(Symbol tag, SyntaxTree node) {
		for (SyntaxTree sub : node) {
			if (sub.is(tag)) {
				return true;
			}
		}
		return false;
	}

	private int switchUnique = 0;

	public class Switch extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			Label condLabel = mBuilder.newLabel();
			Label breakLabel = mBuilder.newLabel();
			mBuilder.getLoopLabels().push(new Pair<Label, Label>(breakLabel, null));
			SyntaxTree body = node.get(_body);
			int size;
			if (has(_SwitchDefault, body)) {
				size = body.size() - 1;
			} else {
				size = body.size();
			}

			Label dfltLabel = mBuilder.newLabel();
			Label[] labels = new Label[size];
			for (int i = 0; i < size; i++) {
				labels[i] = mBuilder.newLabel();
			}

			// keys must be ascending order
			Case cases[] = getSwitchKeys(node);
			Arrays.sort(cases);
			int[] indexes = new int[size];
			for (int i = 0; i < size; i++) {
				indexes[i] = cases[i].getIndex();
				if (i > 0 && cases[i].getKey() == cases[i - 1].getKey()) {
					cases[i].setChecklabel(cases[i - 1].getChecklabel());
				} else {
					cases[i].setChecklabel(mBuilder.newLabel());
				}
				cases[i].setEvalLabel(mBuilder.newLabel());
			}
			ArrayList<Integer> keyList = new ArrayList<>();
			ArrayList<Label> checkLabels = new ArrayList<>();
			for (int i = 0; i < size; i++) {
				if (i == 0 || cases[i].getKey() != cases[i - 1].getKey()) {
					checkLabels.add(cases[i].getChecklabel());
					keyList.add(cases[i].getKey());
				}
			}
			int[] keys = new int[keyList.size()];
			for (int i = 0; i < keyList.size(); i++) {
				keys[i] = keyList.get(i);
			}

			// Condition
			Class<?> condType = node.get(_cond).getClassType();
			if (condType == int.class) {
				acceptIntSwitch(node, condLabel, dfltLabel, cases, keys);
			} else if (condType == String.class) {
				acceptStringSwitch(node, condLabel, dfltLabel, checkLabels, keys, size, cases);
			}

			int i = 0;
			// Case and Default Block
			for (SyntaxTree sub : body) {
				if (sub.is(_SwitchCase)) {
					mBuilder.mark(cases[indexOf(indexes, i)].getEvalLabel());
					i++;
				} else if (sub.is(_SwitchDefault)) {
					mBuilder.mark(dfltLabel);
				}
				visit(sub.get(_body));
			}
			if (!has(_SwitchDefault, body)) {
				mBuilder.mark(dfltLabel);
			}
			mBuilder.mark(breakLabel);
		}

		class Case implements Comparable<Case> {

			private int index;
			private int key;
			private Object value;
			private Label evalLabel;
			private Label checklabel; // for StringSwitch

			public Case(int index, int key) {
				this.index = index;
				this.key = key;
				this.value = null;
			}

			public Case(int index, int key, Object value) {
				this.index = index;
				this.key = key;
				this.value = value;
			}

			public int getIndex() {
				return index;
			}

			public int getKey() {
				return key;
			}

			public Object getValue() {
				return value;
			}

			public Label getEvalLabel() {
				return evalLabel;
			}

			public void setEvalLabel(Label evalLabel) {
				this.evalLabel = evalLabel;
			}

			public Label getChecklabel() {
				return checklabel;
			}

			public void setChecklabel(Label checklabel) {
				this.checklabel = checklabel;
			}

			@Override
			public int compareTo(Case target) {
				int tkey = target.getKey();
				int res = ((Integer) this.key).compareTo(tkey);
				if (res == 0) {
					return ((Integer) this.index).compareTo(target.getIndex());
				}
				return res;
			}

			@Override
			public String toString() {
				String out = "";
				out += "Key: " + this.key + "\n";
				out += "Index: " + this.index + "\n";
				out += "Value: " + this.value + "\n";
				return out;
			}

		}

		// private boolean isTableSwitch(int[] keys) {
		// int min = keys[0];
		// int max = keys[0];
		// for (int n : keys) {
		// if (min > n) {
		// min = n;
		// } else if (max < n) {
		// max = n;
		// }
		// }
		// if (max - min == keys.length - 1) {
		// return true;
		// }
		// return false;
		// }

		private int evalKey(SyntaxTree node) {
			int result = 0;
			if (node.is(_Add)) {
				int left = evalKey(node.get(_left));
				int right = evalKey(node.get(_right));
				result = left + right;
			} else if (node.is(_Sub)) {
				int left = evalKey(node.get(_left));
				int right = evalKey(node.get(_right));
				result = left - right;
			} else if (node.is(_Mul)) {
				int left = evalKey(node.get(_left));
				int right = evalKey(node.get(_right));
				result = left * right;
			} else if (node.is(_Div)) {
				int left = evalKey(node.get(_left));
				int right = evalKey(node.get(_right));
				result = left / right;
			} else if (node.is(_Const) && node.getClassType() == int.class) {
				result = (int) node.getValue();
			} else if (node.is(_Const) && node.getClassType() == String.class) {
				result = ((String) node.getValue()).hashCode();
			}
			return result;
		}

		private Case[] getSwitchKeys(SyntaxTree node) {
			SyntaxTree body = node.get(_body);
			int size;
			if (has(_SwitchDefault, body)) {
				size = body.size() - 1;
			} else {
				size = body.size();
			}
			Case keys[] = new Case[size];
			int j = 0;
			for (SyntaxTree caseNode : body) {
				if (caseNode.is(_SwitchCase)) {
					keys[j] = new Case(j, evalKey(caseNode.get(_cond)), caseNode.get(_cond).getValue());
					j++;
				}
			}
			return keys;
		}

		private void acceptIntSwitch(SyntaxTree node, Label condLabel, Label dfltLabel, Case[] cases, int[] keys) {
			// Condition
			mBuilder.mark(condLabel);
			visit(node.get(_cond));
			Label[] evalLabels = new Label[cases.length];
			for (int i = 0; i < cases.length; i++) {
				evalLabels[i] = cases[i].getEvalLabel();
			}
			mBuilder.visitLookupSwitchInsn(dfltLabel, keys, evalLabels);
			// if (condType == int.class) {
			// if (isTableSwitch(keys)) {
			// mBuilder.visitTableSwitchInsn(arg0, arg1, dfltLabel, labels);
			// } else {
			// mBuilder.visitLookupSwitchInsn(dfltLabel, keys, labels);
			// }
			// } else if (condType == String.class) {
			// }
		}

		private void acceptStringSwitch(SyntaxTree node, Label condLabel, Label dfltLabel, ArrayList<Label> checkLabels, int[] keys, int size, Case[] cases) {
			// Condition
			mBuilder.mark(condLabel);
			visit(node.get(_cond));
			VarEntry condVar = mBuilder.createNewVarAndStore("#SWITCH_KEY" + switchUnique, String.class);
			switchUnique++;
			visit(node.get(_cond));
			mBuilder.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I", false);
			Label[] labels = new Label[checkLabels.size()];
			mBuilder.visitLookupSwitchInsn(dfltLabel, keys, checkLabels.toArray(labels));

			for (int i = 0; i < size; i++) {
				while (i > 0 && i < size && cases[i].getKey() == cases[i - 1].getKey()) {
					mBuilder.loadFromVar(condVar);
					mBuilder.visitLdcInsn(cases[i].getValue());
					mBuilder.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
					mBuilder.visitJumpInsn(Opcodes.IFNE, cases[i].getEvalLabel());
					i++;
				}
				if (i < size) {
					mBuilder.mark(cases[i].getChecklabel());
					mBuilder.loadFromVar(condVar);
					mBuilder.visitLdcInsn(cases[i].getValue());
					mBuilder.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
					mBuilder.visitJumpInsn(Opcodes.IFNE, cases[i].getEvalLabel());
				}
			}
		}

		private int indexOf(int[] array, int value) {
			for (int i = 0; i < array.length; i++) {
				if (array[i] == value) {
					return i;
				}
			}
			return -1;
		}
	}

	public class Try extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			SyntaxTree finallyNode = node.get(_finally, null);
			TryCatchLabel labels = mBuilder.createNewTryLabel(finallyNode != null);
			mBuilder.getTryLabels().push(labels);
			Label mergeLabel = mBuilder.newLabel();

			labels.setFinallyNode(finallyNode);

			// try block
			mBuilder.mark(labels.getStartLabel());
			visit(node.get(_try));
			mBuilder.mark(labels.getEndLabel());

			if (finallyNode != null) {
				// mBuilder.jumpToFinally();
				visit(mBuilder.getFinallyNode());
			}
			mBuilder.goTo(mergeLabel);

			// catch blocks
			for (SyntaxTree catchNode : node.get(_catch)) {
				Class<?> exceptionType = catchNode.get(_name).getClassType();
				mBuilder.catchException(labels.getStartLabel(), labels.getEndLabel(), Type.getType(exceptionType));
				mBuilder.enterScope();
				mBuilder.createNewVarAndStore(catchNode.getText(_name, null), exceptionType);
				visit(catchNode.get(_body));
				if (finallyNode != null) {
					for (SyntaxTree fnode : mBuilder.getMultipleFinallyNode()) {
						visit(fnode);
					}
				}
				mBuilder.exitScope();
				mBuilder.goTo(mergeLabel);
			}
			if (node.get(_catch).size() == 0 && finallyNode != null) {
				mBuilder.catchException(labels.getStartLabel(), labels.getEndLabel(), Type.getType(RuntimeException.class));
				for (SyntaxTree fnode : mBuilder.getMultipleFinallyNode()) {
					visit(fnode);
				}
				mBuilder.throwException();
			}

			// finally block
			// if (finallyNode != null) {
			// mBuilder.mark(labels.getFinallyLabel());
			// mBuilder.enterScope();
			// mBuilder.storeReturnAddr();
			// visit(finallyNode);
			// mBuilder.returnFromFinally();
			// mBuilder.exitScope();
			// }

			mBuilder.getTryLabels().pop();

			mBuilder.mark(mergeLabel);
		}
	}

	public class VarDecl extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			SyntaxTree varNode = node.get(_name);
			if (mBuilder.getVar(node.getText(_name, null)) == null) {
				VarEntry var = mBuilder.createNewVar(varNode.toText(), varNode.getClassType());
				if (node.has(_expr)) {
					visit(node.get(_expr));
					mBuilder.storeToVar(var);
				}
			}
		}
	}

	public class MultiVarDecl extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			for (SyntaxTree sub : node) {
				SyntaxTree varNode = sub.get(_name);
				VarEntry var = mBuilder.createNewVar(varNode.toText(), varNode.getClassType());
				if (sub.has(_expr)) {
					visit(sub.get(_expr));
					mBuilder.storeToVar(var);
				}
			}
		}
	}

	public class Assign extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			String name = node.getText(_left, null);
			SyntaxTree valueNode = node.get(_right);
			VarEntry var = mBuilder.getVar(name);
			visit(valueNode);
			mBuilder.dup(valueNode.getClassType()); // to return value form
													// assignment
			mBuilder.storeToVar(var);
		}
	}

	public class Assert extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			Label label = mBuilder.newLabel();
			visit(node.get(_cond));
			mBuilder.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.EQ, label);
			// TODO: Error handling
			mBuilder.throwException(Type.getType(java.lang.AssertionError.class), node.getText(_msg, null));
			mBuilder.mark(label);
		}
	}

	public class Return extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			if (node.has(_expr)) {
				visit(node.get(_expr));
			}
			// mBuilder.jumpToMultipleFinally();
			for (SyntaxTree finallyNode : mBuilder.getMultipleFinallyNode()) {
				visit(finallyNode);
			}
			mBuilder.returnValue();
		}
	}

	public class Break extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			Label breakLabel = mBuilder.getLoopLabels().peek().getLeft();
			// mBuilder.jumpToMultipleFinally();
			for (SyntaxTree finallyNode : mBuilder.getMultipleFinallyNode()) {
				visit(finallyNode);
			}
			mBuilder.goTo(breakLabel);
		}
	}

	public class Continue extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			Label continueLabel = mBuilder.getLoopLabels().peek().getRight();
			// mBuilder.jumpToMultipleFinally();
			for (SyntaxTree finallyNode : mBuilder.getMultipleFinallyNode()) {
				visit(finallyNode);
			}
			mBuilder.goTo(continueLabel);
		}
	}

	public class Throw extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			if (node.has(_expr)) {
				visit(node.get(_expr));
			}
			mBuilder.returnValue();
		}
	}

	public class Expression extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			visit(node.get(0));
		}
	}

	public class Name extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			VarEntry var = mBuilder.getVar(node.toText());
			mBuilder.loadFromVar(var);
		}
	}

	public class GetFreeVar extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			mBuilder.loadThis();
			mBuilder.getField(cBuilder.getTypeDesc(), "fv$" + node.toText(), Type.getType(node.getClassType()));
		}
	}

	public class SetFreeVar extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			SyntaxTree left = node.get(_left);
			SyntaxTree right = node.get(_right);
			mBuilder.loadThis();
			visit(right);
			mBuilder.putField(cBuilder.getTypeDesc(), "fv$" + left.toText(), Type.getType(right.getClassType()));
			mBuilder.loadThis();
			mBuilder.getField(cBuilder.getTypeDesc(), "fv$" + left.toText(), Type.getType(node.getClassType()));
		}
	}

	public class Cast extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			visit(node.get(_expr));
			mBuilder.checkCast(Type.getType(node.getClassType()));
		}
	}

	public class And extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			Label elseLabel = new Label();
			Label mergeLabel = new Label();
			visit(node.get(_left));
			mBuilder.visitJumpInsn(Opcodes.IFEQ, elseLabel);

			visit(node.get(_right));
			mBuilder.visitJumpInsn(Opcodes.IFEQ, elseLabel);

			mBuilder.visitLdcInsn(true);
			mBuilder.visitJumpInsn(Opcodes.GOTO, mergeLabel);

			mBuilder.visitLabel(elseLabel);
			mBuilder.visitLdcInsn(false);
			mBuilder.visitJumpInsn(Opcodes.GOTO, mergeLabel);

			mBuilder.visitLabel(mergeLabel);

		}
	}

	public class Or extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			Label thenLabel = new Label();
			Label mergeLabel = new Label();
			visit(node.get(_left));
			mBuilder.visitJumpInsn(Opcodes.IFNE, thenLabel);

			visit(node.get(_right));
			mBuilder.visitJumpInsn(Opcodes.IFNE, thenLabel);

			mBuilder.visitLdcInsn(false);
			mBuilder.visitJumpInsn(Opcodes.GOTO, mergeLabel);

			mBuilder.visitLabel(thenLabel);
			mBuilder.visitLdcInsn(true);
			mBuilder.visitJumpInsn(Opcodes.GOTO, mergeLabel);

			mBuilder.visitLabel(mergeLabel);
		}
	}

	public class Not extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			visit(node.get(_expr));
			mBuilder.not();
		}
	}

	public class Inc extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			// evalSuffixInc(node, 1);
			visit(node.get(_expr));
			visitStatementAsBlock(node.get(_body));
		}
	}

	public class Dec extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			// evalSuffixInc(node, -1);
			visit(node.get(_expr));
			visitStatementAsBlock(node.get(_body));
		}
	}

	// public class PrefixInc extends Undefined {
	// @Override
	// public void acceptAsm(TypedTree node) {
	// evalPrefixInc(node, 1);
	// }
	// }
	//
	// public class PrefixDec extends Undefined {
	// @Override
	// public void acceptAsm(TypedTree node) {
	// evalPrefixInc(node, -1);
	// }
	// }

	public class Array extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			Class<?> c = node.getClassType();
			Class<?> elemClass = Lang.getArrayElementClass(c);
			if (Lang.isNativeArray(c)) {
				pushArray(elemClass, node);
			} else {
				mBuilder.newInstance(Type.getType(c));
				mBuilder.dup();
				pushArray(elemClass, node);
				mBuilder.invokeConstructor(Type.getType(node.getClassType()), Method.getMethod("void <init> (" + elemClass.getName() + "[])"));
			}
		}
	}

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

	public class NewArray extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			Class<?> c = node.getClassType();
			Class<?> elemClass = Lang.getArrayElementClass(c);
			mBuilder.newInstance(Type.getType(c));
			mBuilder.dup();
			visit(node.get(_size));
			mBuilder.newArray(Type.getType(elemClass));
			mBuilder.invokeConstructor(Type.getType(node.getClassType()), Method.getMethod("void <init> (" + elemClass.getName() + "[])"));
		}
	}

	public class NewArray2 extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			Class<?> c = node.getClassType();
			Class<?> subc = Lang.getArrayElementClass(node.getType());
			Class<?> elemClass = Lang.getArrayElementClass(subc);
			mBuilder.newInstance(Type.getType(c));
			mBuilder.dup();
			mBuilder.push(node.size());
			mBuilder.newArray(Type.getType(subc));
			int i = 0;
			for (SyntaxTree size : node) {
				mBuilder.dup();
				mBuilder.push(i);
				mBuilder.newInstance(Type.getType(subc));
				mBuilder.dup();
				visit(size);
				mBuilder.newArray(Type.getType(elemClass));
				mBuilder.invokeConstructor(Type.getType(subc), Method.getMethod("void <init> (" + elemClass.getName() + "[])"));
				mBuilder.arrayStore(Type.getType(subc));
				i++;
			}
			mBuilder.invokeConstructor(Type.getType(node.getClassType()), Method.getMethod("void <init> (Object[])"));
		}
	}

	public class NullCheck extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			if (node.getValue() != null && node.getValue().getClass() == Boolean.class) {
				mBuilder.push((boolean) node.getValue());
			} else {
				Label trueLabel = mBuilder.newLabel();
				Label mergeLabel = mBuilder.newLabel();
				visit(node.get(_expr));
				mBuilder.ifNull(trueLabel);
				mBuilder.push(false);
				mBuilder.goTo(mergeLabel);
				mBuilder.mark(trueLabel);
				mBuilder.push(true);
				mBuilder.mark(mergeLabel);
			}
		}
	}

	public class NonNullCheck extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			if (node.getValue() != null && node.getValue().getClass() == Boolean.class) {
				mBuilder.push((boolean) node.getValue());
			} else {
				Label trueLabel = mBuilder.newLabel();
				Label mergeLabel = mBuilder.newLabel();
				visit(node.get(_expr));
				mBuilder.ifNonNull(trueLabel);
				mBuilder.push(false);
				mBuilder.goTo(mergeLabel);
				mBuilder.mark(trueLabel);
				mBuilder.push(true);
				mBuilder.mark(mergeLabel);
			}
		}
	}

	public class Null extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			mBuilder.pushNull();
		}
	}

	public class Empty extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			// empty
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
