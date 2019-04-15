package de.unisb.cs.st.ample.test;

public class CallInformation implements Comparable {

	private int numberOfExecutions;
	
	private int position;

	public CallInformation(int position, int numberOfExecutions) {
		this.position = position;
		this.numberOfExecutions = numberOfExecutions;
	}

	public int getNumberOfExecutions() {
		return numberOfExecutions;
	}

	public int getPosition() {
		return position;
	}

	public int compareTo(Object object) {
		return ((CallInformation) object).position - position;
	}
	
}
