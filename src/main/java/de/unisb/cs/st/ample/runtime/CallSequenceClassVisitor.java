package de.unisb.cs.st.ample.runtime;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.softevo.util.asm.IdentifierMap;

import java.util.HashSet;

public class CallSequenceClassVisitor extends ClassVisitor implements Opcodes {

	private String className = null;

	private String superClassName = null;

	protected String test = null;

	private IdentifierMap methodIdentifierMap;

	private int classIdentifier;

	private boolean isTopLevelClass;

	private boolean identifierFieldAdded = false;

	private boolean isInterface = false;

	private HashSet<String> preloadedClasses;

	public CallSequenceClassVisitor(ClassVisitor classVisitor,
	                                IdentifierMap methodIdentifierMap,
									int classIdentifier,
									HashSet<String> preloadedClasses
    ) {
		super(ASM7, classVisitor);

		this.methodIdentifierMap = methodIdentifierMap;
		this.classIdentifier = classIdentifier;
		this.preloadedClasses = preloadedClasses;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.className = name;
		this.superClassName = superName;
		this.isTopLevelClass = superName.startsWith("java/lang") || superName.startsWith("sun") || preloadedClasses.contains(superName);
		this.isInterface = (access & ACC_INTERFACE) > 0;
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if (!isInterface && ((access & (ACC_NATIVE | ACC_ABSTRACT)) == 0)) {
			if (isTopLevelClass && !identifierFieldAdded) {
				super.visitField(
				        ACC_PROTECTED,
                        IdentifierFieldSetConstructorVisitor.FIELD_OBJECTID,
                        "I",
                        null,
                        null
                );
				identifierFieldAdded = true;
			}

			int methodIdentifier = methodIdentifierMap.getIdentifier(className, name, desc, access);
			MethodVisitor writeVisitor = super.visitMethod(access, name, desc, signature, exceptions);
			if (name.equals("<init>") && isTopLevelClass) {
				MethodVisitor fieldSetVisitor = new IdentifierFieldSetConstructorVisitor(writeVisitor, className, superClassName);
                return new StartEndTracingMethodVisitor(fieldSetVisitor, access, className, name, desc, methodIdentifier, classIdentifier);
			} else {
                return new StartEndTracingMethodVisitor(writeVisitor, access, className, name, desc, methodIdentifier, classIdentifier);
			}
		}

		return super.visitMethod(access, name, desc, signature, exceptions);
	}
}
