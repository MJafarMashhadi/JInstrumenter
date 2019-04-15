package de.unisb.cs.st.ample.runtime;

import org.softevo.util.asm.ClassIdentifierMap;
import org.softevo.util.asm.IdentifierMap;

import java.io.*;
import java.util.*;

public class CallSequenceSetRecorder {

	private static final String REVISION = "2";

	public static final int METHODID_UNDEF = -1;

	public static final int OBJECTID_STATIC = -2;

	private static HashMap<Integer, Stack<StackEntry>> threadIdToCallStackMap = new HashMap<Integer, Stack<StackEntry>>();

	private static HashMap<Integer, HashMap<Integer,LinkedList<Integer>>> classIdToObjectWindowsMapMap = new HashMap<Integer, HashMap<Integer,LinkedList<Integer>>>();

	private static HashMap<Integer, HashMap<Pattern, Pattern>> classIdToPatternSetMap = new HashMap<Integer, HashMap<Pattern, Pattern>>();

	private static Object lock = new Object();

	private static int idCounter = 0;

	private static int windowSize = 5;

	public static IdentifierMap methodIdentifierMap = null;

	public static ClassIdentifierMap classIdentifierMap = null;

	public static HashSet<String> applicationClasses = null;

	private static boolean ignoreInterClassCalls = true;

	private static boolean filterBootstrapLoaderClasses = true;

	private static String patternFileName = "patterns.ser";

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownThread()));
		if (System.getProperty("ample.windowsize") != null) {
			windowSize = Integer.parseInt(System.getProperty("ample.windowsize"));
		}
		patternFileName = System.getProperty("ample.patternfile", "patterns.ser");
	}

	public static int getCurrentThreadId() {
		return System.identityHashCode(Thread.currentThread());
	}

	private static String getClassName(int classId) {
		return classIdentifierMap.getClassName(classId);
	}

	private static String getMethodName(int methodId) {
		if (methodId != METHODID_UNDEF) {
			return methodIdentifierMap.getMethodIdentifier(methodId).toString();
		} else {
			return "UNDEFINED";
		}
	}

	public static void staticMethodEnded(int methodId, int classId) {
		synchronized (lock) {
			Stack<StackEntry> callStack = getOrCreateCallStackOfCurrentThread();
			if (callStack.size() > 0) {
				StackEntry entry = callStack.pop();
				if (!entry.isStatic() || (entry.methodId != methodId)) {
					throw new RuntimeException("Expected end of static method " + getMethodName(entry.methodId) + 
					                           " but found end of " + entry.objectId + "." + 
					                           getMethodName(methodId));
				}
			} else {
				throw new RuntimeException("Empty stack at end of method " + getMethodName(methodId));
			}
		}
	}   

	public static void staticMethodStarted(int methodId, int classId) {
		synchronized (lock) {
            System.out.println("Dynamic method started m=" + getMethodName(methodId) + " c=" + getClassName(classId));
//			getOrCreateCallStackOfCurrentThread().push(new StackEntry(methodId, classId, OBJECTID_STATIC));
		}
	}

	public static void dynamicMethodEnded(int methodId, int classId, int objectId) {
		synchronized (lock) {
			Stack<StackEntry> callStack = getOrCreateCallStackOfCurrentThread();
			if (callStack.size() > 0) {
				StackEntry entry = callStack.pop();
				if ((entry.objectId != objectId) || (entry.methodId != methodId)) {
					throw new RuntimeException("CallStack mismatch: Expected end of dynamic method " + 
					                           entry.objectId + "." + getMethodName(entry.methodId) + 
					                           " but got " + objectId + "." + getMethodName(methodId));
				}
			} else {
				throw new RuntimeException("Empty call stack at end of method " + getMethodName(methodId));
			}
		}

	}    

	public static void dynamicMethodStarted(int methodId, int classId, int objectId) {
		synchronized (lock) {
			System.out.println("Dynamic method started m=" + getMethodName(methodId) + " c=" + getClassName(classId) + " o=" + objectId);
//			Stack<StackEntry> callStack = getOrCreateCallStackOfCurrentThread();
//			if (callStack.size() > 0) {
//				StackEntry entry = callStack.peek();
//				if (!entry.isStatic()) {
//					if (!ignoreInterClassCalls || (entry.objectId != objectId)) {
//						updateCallWindow(getOrCreateCallWindow(entry.classId, entry.objectId), entry.classId, methodId);
//
//					}
//				}
//			}
//			callStack.push(new StackEntry(methodId, classId, objectId));
		}
	}

	protected static void updateCallWindow(LinkedList<Integer> callWindow, int classId, int calledMethodId) {
		synchronized (lock) {
			if (callWindow.size() < (windowSize - 1)) {
				callWindow.add(calledMethodId);
			} else {
				if (callWindow.size() == (windowSize)) {
					callWindow.removeFirst();
				}
				callWindow.add(calledMethodId);
				Integer[] calledMethods = callWindow.toArray(new Integer[callWindow.size()]);
				int[] primitiveCalledMethods = new int[calledMethods.length];
				for (int counter = 0; counter < primitiveCalledMethods.length; counter++) {
					primitiveCalledMethods[counter] = calledMethods[counter];
				}
				Pattern pattern = new Pattern(primitiveCalledMethods);
				HashMap<Pattern, Pattern> patternSet = getOrCreatePatternSetForClass(classId);
				Pattern storedPattern = patternSet.get(pattern);
				if (storedPattern != null) {
					storedPattern.increaseNumberOfOccurences();
				} else {
					patternSet.put(pattern, pattern);
				}
			}
		}
	}

	protected static HashMap<Pattern, Pattern> getOrCreatePatternSetForClass(int classId) {
		HashMap<Pattern, Pattern> result;

		synchronized (lock) {
			result = classIdToPatternSetMap.get(classId);
			if (result == null) {
				result = new HashMap<Pattern, Pattern>();
				classIdToPatternSetMap.put(classId, result);
			}
			return result;
		}
	}

	protected static LinkedList<Integer> getOrCreateCallWindow(int classId, int objectId) {
		synchronized (lock) {
			HashMap<Integer, LinkedList<Integer>> objectIdToCallWindowMap = classIdToObjectWindowsMapMap.get(classId);
			if (objectIdToCallWindowMap == null) {
				objectIdToCallWindowMap = new HashMap<Integer, LinkedList<Integer>>();
				classIdToObjectWindowsMapMap.put(classId, objectIdToCallWindowMap);
			}
			LinkedList<Integer> result = objectIdToCallWindowMap.get(objectId);
			if (result == null) {
				result = new LinkedList<Integer>();
				objectIdToCallWindowMap.put(objectId, result);
			}
			return result;
		}
	}

	protected static Stack<StackEntry> getOrCreateCallStackOfCurrentThread() {
		synchronized (lock) {
			int threadId = getCurrentThreadId();
			Stack<StackEntry> result = threadIdToCallStackMap.get(threadId);
			if (result == null) {
				result = new Stack<StackEntry>();
				threadIdToCallStackMap.put(threadId, result);
			}
			return result;
		}
	}
	public static int getNextId() {
		synchronized(lock) {
			int result = idCounter;
			idCounter++;
			return result;
		}
	}

	private static class Pattern {

		private int[] calledMethods = null;

		private int numberOfOccurences = 1;

		private int hashValue = 0;

		private boolean isHashCodeUpToDate = false;

		public Pattern(int[] calledMethods) {
			this.calledMethods = calledMethods;
		}

		private void updateHash(int value) {
			int highorder = hashValue & 0xf8000000;
			hashValue = hashValue << 5;                    
			hashValue = hashValue ^ (highorder >> 27);     
			hashValue = hashValue ^ value;                       
		}

		public int hashCode() {
			if (!isHashCodeUpToDate) {
				for (int counter = 0; counter < calledMethods.length; counter++) {
					updateHash(calledMethods[counter]);
				}
				isHashCodeUpToDate = true;
			}
			return hashValue;
		}

		public boolean equals(Object object) {
			Pattern peer = (Pattern) object;
			if (peer.calledMethods.length != calledMethods.length) {
				return false;
			} else {
				for (int counter = 0; counter < calledMethods.length; counter++) {
					if (calledMethods[counter] != peer.calledMethods[counter]) {
						return false;
					}
				}
				return true;
			}
		}

		public void increaseNumberOfOccurences() {
			numberOfOccurences++;
		}

		public int getNumberOfOccurences() {
			return numberOfOccurences;
		}

		public String toString() {
			StringBuffer result = new StringBuffer("#" + numberOfOccurences + " ");
			for (int counter = 0; counter < calledMethods.length; counter++) {
				result.append(calledMethods[counter]);
				if (counter != calledMethods.length - 1) {
					result.append(',');
				}
			}
			return result.toString();
		}
	}

	private static void writePatternSetMap(File file) throws IOException {
		FileOutputStream fileOutputStream = null;

		try {
			fileOutputStream = new FileOutputStream(file);
			BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream, 32768);
			DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);
			dataOutputStream.writeInt(windowSize);
			Iterator<Integer> classIdIterator = classIdToPatternSetMap.keySet().iterator();
			int numberOfApplicationClassesWithPatterns = 0;
			while (classIdIterator.hasNext()) {
				int classId = classIdIterator.next();
				String className = getClassName(classId);
				System.out.println(filterBootstrapLoaderClasses);
				System.out.println(applicationClasses);
				if (!filterBootstrapLoaderClasses || applicationClasses.contains(className)) {
					numberOfApplicationClassesWithPatterns++;
				}
			}
			dataOutputStream.writeInt(numberOfApplicationClassesWithPatterns);
			classIdIterator = classIdToPatternSetMap.keySet().iterator();
			while (classIdIterator.hasNext()) {
				int classId = classIdIterator.next();
				String className = getClassName(classId);
				if (!filterBootstrapLoaderClasses || applicationClasses.contains(className)) {
					dataOutputStream.writeUTF(getClassName(classId));
					HashMap<Pattern, Pattern> patterns = classIdToPatternSetMap.get(classId);
					dataOutputStream.writeInt(patterns.size());
					Iterator<Pattern> patternIterator = patterns.keySet().iterator();
					while (patternIterator.hasNext())	{
						Pattern pattern = patternIterator.next();
						dataOutputStream.writeInt(pattern.numberOfOccurences);
						for (int counter = 0; counter < pattern.calledMethods.length; counter++) {
							dataOutputStream.writeUTF(getMethodName(pattern.calledMethods[counter]));
						}
					}
				}
			}
			dataOutputStream.flush();
			dataOutputStream.close();
		} finally {
			if (fileOutputStream != null) {
				try {
					fileOutputStream.close();
				} catch (IOException finalizeException) {
					//ignore exception
				}
			}
		}
	}

	private static class StackEntry {

		private int methodId;

		private int classId;

		private int objectId;

		public StackEntry( int methodId, int classId, int objectId) {
			this.classId = classId;
			this.methodId = methodId;
			this.objectId = objectId;
		}

		private boolean isStatic() {
			return objectId == OBJECTID_STATIC;
		}

		public String toString() {
			StringBuffer result = new StringBuffer();
			result.append("Object #" + objectId + " " + getMethodName(methodId));
			return result.toString();
		}
	}

	public static class ShutdownThread implements Runnable {


		public void run() {
			try {
				synchronized(lock) {
					Iterator<Integer> classIterator = classIdToObjectWindowsMapMap.keySet().iterator();
					while (classIterator.hasNext()) {
						int classId = classIterator.next();
						HashMap<Integer, LinkedList<Integer>> objectWindowsMap = classIdToObjectWindowsMapMap.get(classId);
						Iterator<Integer> objectIdIterator = objectWindowsMap.keySet().iterator();
						while (objectIdIterator.hasNext()) {
							int objectId = objectIdIterator.next();
							LinkedList<Integer> callSequence = objectWindowsMap.get(objectId);
							if (callSequence.size() < windowSize) {
								int numberOfElementsToAdd = windowSize - callSequence.size();
								for (int counter = 0; counter < numberOfElementsToAdd; counter++) {
									updateCallWindow(callSequence, classId, METHODID_UNDEF);
								}
							}
						}
					}
					File file = new File(patternFileName);
					writePatternSetMap(file);
				}
			} catch (Exception serializeException) {
				System.out.println("An exception occured serializing:");
				serializeException.printStackTrace(System.out);
			}
		}

	}

}
