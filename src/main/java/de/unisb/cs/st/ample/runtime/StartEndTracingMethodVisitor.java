/*
 *
 */
package de.unisb.cs.st.ample.runtime;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;

public class StartEndTracingMethodVisitor extends LocalVariablesSorter {

	private Label methodStartLabel;

	private int methodIdentifier;

	private int classIdentifier;

	private int access;

	private String name;

	private String className;

	private boolean isConstructor = false;

	private boolean isConstructorStartInstrumented = false;

	private int newInstructionCounter = 0;

	public StartEndTracingMethodVisitor(MethodVisitor visitor, int access, String className,
	                                    String name, String desc, int methodIdentifier,
	                                    int classIdentifier) {
		super(Opcodes.ASM7,access, desc, visitor);

		this.methodIdentifier = methodIdentifier;
		this.classIdentifier = classIdentifier;
		this.access = access;
		this.name = name;
		this.className = className;
		isConstructor = "<init>".equals(name);
	}

	@Override
	public void visitMethodInsn(int opCode, String owner, String name, String desc, boolean isInterface) {
		super.visitMethodInsn(opCode, owner, name, desc, isInterface);
		if (isConstructor && !isConstructorStartInstrumented) {
			if (opCode == Opcodes.INVOKESPECIAL && "<init>".equals(name)) {
				if (newInstructionCounter == 0) {
					 addMethodStartCode();
					isConstructorStartInstrumented = true;
				} else {
					newInstructionCounter--;
				}
			}
		}
	}

//	@Override
//	public void visitInsn(int opcode) {
//		if ((opcode == ARETURN) || (opcode == IRETURN) || (opcode == DRETURN) || (opcode == LRETURN) || (opcode == FRETURN)
//				|| (opcode == RETURN)) {
//			addMethodEndCode();
//		}
//		super.visitInsn(opcode);
//	}

//	protected void addMethodEndCode() {
//		super.visitLdcInsn(methodIdentifier);
//		super.visitLdcInsn(classIdentifier);
//		if ((access & ACC_STATIC) == 0 ) {
//			super.visitVarInsn(ALOAD, 0);
//			super.visitFieldInsn(GETFIELD, className, IdentifierFieldSetConstructorVisitor.FIELD_OBJECTID, "I");
//			super.visitMethodInsn(INVOKESTATIC, Instrumenter.TRACECLASSNAME, "dynamicMethodEnded", "(III)V", false);
//		} else {
//			super.visitMethodInsn(INVOKESTATIC, Instrumenter.TRACECLASSNAME, "staticMethodEnded", "(II)V", false);
//		}
//	}

	@Override
	public void visitTypeInsn(int opcode, String desc) {
		if (isConstructor && !isConstructorStartInstrumented && (opcode == Opcodes.NEW)) {
			newInstructionCounter++;
		}
		super.visitTypeInsn(opcode, desc);
	}

	protected void addMethodStartCode() {
		methodStartLabel = new Label();
		super.visitLabel(methodStartLabel);
		// Push the parameters on to the stack
        super.visitLdcInsn(methodIdentifier);
        super.visitLdcInsn(classIdentifier);
		if ((access & Opcodes.ACC_STATIC) == 0) {
			super.visitVarInsn(Opcodes.ALOAD, 0);
			super.visitFieldInsn(Opcodes.GETFIELD, className, IdentifierFieldSetConstructorVisitor.FIELD_OBJECTID, "I");
			super.visitMethodInsn(Opcodes.INVOKESTATIC, Instrumenter.TRACECLASSNAME, "dynamicMethodStarted", "(III)V", false);
		} else {
			super.visitMethodInsn(Opcodes.INVOKESTATIC, Instrumenter.TRACECLASSNAME, "staticMethodStarted", "(II)V", false);
		}
	}

	@Override
	public void visitCode() {
		super.visitCode();
		if (!isConstructor) {
			addMethodStartCode();
		}
	}

//	@Override
//	public void visitEnd() {
//		int exceptionVariableIndex;
//		if (methodStartLabel != null) {
//			Label methodEndLabel = new Label();
//			Label exceptionHandlerStartLabel = new Label();
//			super.visitLabel(methodEndLabel);
//			super.visitLabel(exceptionHandlerStartLabel);
//			exceptionVariableIndex = newLocal(Type.BOOLEAN_TYPE);
//			super.visitVarInsn(ASTORE, exceptionVariableIndex);
//			addMethodEndCode();
//			super.visitVarInsn(ALOAD, exceptionVariableIndex);
//			super.visitInsn(ATHROW);
//			super.visitTryCatchBlock(methodStartLabel, methodEndLabel, exceptionHandlerStartLabel, null); // Add a "finally" block
//		} else {
//			throw new RuntimeException("Method start label is null: " + className + "." + name);
//		}
//        super.visitEnd();
//	}

}
