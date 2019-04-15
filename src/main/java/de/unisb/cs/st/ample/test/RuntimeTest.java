package de.unisb.cs.st.ample.test;

import de.unisb.cs.st.ample.runtime.CallSequenceSetRecorder;
import org.softevo.util.asm.ClassIdentifierMap;
import org.softevo.util.asm.IdentifierMap;

public class RuntimeTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		CallSequenceSetRecorder.classIdentifierMap = new ClassIdentifierMap();
		CallSequenceSetRecorder.classIdentifierMap.getIdentifier("RuntimeTest");
		CallSequenceSetRecorder.classIdentifierMap.getIdentifier("RuntimeTest2");
		CallSequenceSetRecorder.classIdentifierMap.getIdentifier("RuntimeTest3");
		CallSequenceSetRecorder.methodIdentifierMap = new IdentifierMap();
		CallSequenceSetRecorder.methodIdentifierMap.getIdentifier("RuntimeTest", "static1", "([Ljava/langString;)V", 0)  ;
		CallSequenceSetRecorder.methodIdentifierMap.getIdentifier("RuntimeTest", "static2", "([Ljava/langString;)V", 0)  ;
		CallSequenceSetRecorder.methodIdentifierMap.getIdentifier("RuntimeTest", "dynamic1", "([Ljava/langString;)V", 0)  ;
		CallSequenceSetRecorder.methodIdentifierMap.getIdentifier("RuntimeTest", "dynamic2", "([Ljava/langString;)V", 0)  ;
		CallSequenceSetRecorder.staticMethodStarted(0, 0);
		CallSequenceSetRecorder.dynamicMethodStarted(2, 1, 1);
		CallSequenceSetRecorder.dynamicMethodStarted(3, 2, 2);
		
		CallSequenceSetRecorder.dynamicMethodStarted(2, 1, 1);
		CallSequenceSetRecorder.dynamicMethodEnded(2, 1, 1);
		CallSequenceSetRecorder.dynamicMethodStarted(2, 1, 1);
		CallSequenceSetRecorder.dynamicMethodEnded(2, 1, 1);
		CallSequenceSetRecorder.dynamicMethodStarted(2, 1, 1);
		CallSequenceSetRecorder.dynamicMethodEnded(2, 1, 1);
		CallSequenceSetRecorder.dynamicMethodStarted(2, 1, 1);
		CallSequenceSetRecorder.dynamicMethodEnded(2, 1, 1);
		
		CallSequenceSetRecorder.dynamicMethodEnded(3, 2, 2);
		CallSequenceSetRecorder.dynamicMethodEnded(2, 1, 1);
		CallSequenceSetRecorder.staticMethodEnded(0, 0);
		
		/*for (int counter = 0; counter < 10; counter++) {
			CallSequenceSetRecorder.dynamicMethodStarted(2, 1, 1);
			for (int counter2 = 0; counter2 < 10; counter2++) {
				CallSequenceSetRecorder.dynamicMethodStarted(3, 1, 1);
				CallSequenceSetRecorder.dynamicMethodEnded(3, 1, 1);
			}
			CallSequenceSetRecorder.dynamicMethodEnded(2, 1, 1);
		}
		CallSequenceSetRecorder.staticMethodEnded(0, 0);*/
		new Thread(new CallSequenceSetRecorder.ShutdownThread()).start();
	}

}
