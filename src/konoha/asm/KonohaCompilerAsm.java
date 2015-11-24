package konoha.asm;

import java.util.ArrayList;
import java.util.Arrays;

import konoha.main.ConsoleUtils;
import konoha.script.CommonSymbols;
import konoha.script.Lang;
import konoha.script.SyntaxTree;
import konoha.script.TypeSystem;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

public class KonohaCompilerAsm extends ScriptCompilerAsm implements CommonSymbols {

	public KonohaCompilerAsm(TypeSystem typeSystem, ScriptClassLoader cLoader) {
		super(typeSystem, cLoader);
		this.init(this.getClass(), new Undefined());
	}

	@Override
	public void init() {

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

	public class Const extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			asmConst(node);
		}
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
				if (entry != null) {
					mBuilder.loadFromVar(entry);
				} else {
					mBuilder.loadThis();
					mBuilder.getField(cBuilder.getTypeDesc(), "fv$" + fv.toText(), Type.getType(fv.getClassType()));
				}
				argDesc += Type.getType(fv.getClassType()).getClassName();
				if (fv != fvList.get(fvList.size() - 1)) {
					argDesc += ",";
				}
			}
			mBuilder.invokeConstructor(Type.getType(lambdaClass), Method.getMethod("void <init> (" + argDesc + ")"));
		}
	}

	/* class */
	// public class ClassDecl extends Undefined {
	// @Override
	// public void acceptAsm(SyntaxTree node) {
	// compileClass(node);
	// }
	// }
	//
	// public class Constructor extends Undefined {
	// @Override
	// public void acceptAsm(SyntaxTree node) {
	// // inConstructor = true;
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
	// mBuilder.loadThis();
	// mBuilder.invokeConstructor(Type.getType(Object.class),
	// Method.getMethod("void <init> ()"));
	// visit(node.get(_body));
	// mBuilder.exitScope();
	// mBuilder.returnValue();
	// mBuilder.endMethod();
	// }
	// }
	//
	// public class FieldDecl extends Undefined {
	// @Override
	// public void acceptAsm(SyntaxTree node) {
	// for (SyntaxTree field : node) {
	// cBuilder.addField(Opcodes.ACC_PUBLIC, field.getText(_name, null),
	// field.get(_name).getClassType(), null);
	// }
	// }
	// }
	//
	// public class MethodDecl extends Undefined {
	// @Override
	// public void acceptAsm(SyntaxTree node) {
	// SyntaxTree nameNode = node.get(_name);
	// SyntaxTree args = node.get(_param);
	// String name = nameNode.toText();
	// Class<?> returnType = nameNode.getClassType();
	// Class<?>[] paramTypes = new Class<?>[args.size()];
	// for (int i = 0; i < paramTypes.length; i++) {
	// paramTypes[i] = args.get(i).getClassType();
	// }
	// mBuilder = cBuilder.newMethodBuilder(Opcodes.ACC_PUBLIC, returnType,
	// name, paramTypes);
	// mBuilder.enterScope();
	// for (SyntaxTree arg : args) {
	// mBuilder.defineArgument(arg.getText(_name, null), arg.getClassType());
	// }
	// visit(node.get(_body));
	// mBuilder.exitScope();
	// if (returnType != void.class) {
	// visitDefaultValue(nameNode);
	// }
	// mBuilder.returnValue();
	// mBuilder.endMethod();
	// }
	// }

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
			for (SyntaxTree sub : node.get(_list)) {
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

	public class This extends Undefined {
		@Override
		public void acceptAsm(SyntaxTree node) {
			mBuilder.loadThis();

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

}
