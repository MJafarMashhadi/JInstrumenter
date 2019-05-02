package ca.ucalgary.sealab.jinstrumenter.test;

import java.util.HashSet;

public class ClassInformation {

	private HashSet<String> objectIdentifiers;
	
	private HashSet<String> methodIdentifiers;

	private String className;
	
	public ClassInformation(String className) {
		super();
		this.objectIdentifiers = new HashSet<String>();
		this.methodIdentifiers = new HashSet<String>();
		this.className = className;
	}

	public HashSet<String> getMethodIdentifiers() {
		return methodIdentifiers;
	}

	public HashSet<String> getObjectIdentifiers() {
		return objectIdentifiers;
	}
	
	public void addMethodIdentifier(String methodIdentifier) {
		this.methodIdentifiers.add(methodIdentifier);
	}
	
	public void addObjectIdentifier(String objectIdentifier) {
		this.objectIdentifiers.add(objectIdentifier);
	}
	
	public String getClassName() {
		return className;
	}
	
	public int hashCode() {
		return className.hashCode();
	}
	
	public boolean equals(Object object) {
		return ((ClassInformation) object).className.equals(className);
	}
	
}
