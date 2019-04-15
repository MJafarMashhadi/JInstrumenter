package de.unisb.cs.st.ample.test;

public class MethodInformation {

	private String className;
	
	private String objectIdentifier;
	
	private String methodIdentifier;

	public MethodInformation(String className, String objectIdentifier, String methodIdentifier) {
		super();
		this.className = className;
		this.objectIdentifier = objectIdentifier;
		this.methodIdentifier = methodIdentifier;
	}

	public String getClassName() {
		return className;
	}

	public String getMethodIdentifier() {
		return methodIdentifier;
	}

	public String getObjectIdentifier() {
		return objectIdentifier;
	}
	
	public int hashCode() {
		return (className + methodIdentifier + objectIdentifier).hashCode();
	}
	
	public boolean equals(Object object) {
		return ((MethodInformation) object).toString().equals(toString());
	}
	
	public String toString() {
		return  objectIdentifier + "#" + className + "." + methodIdentifier;
	}
	
}
