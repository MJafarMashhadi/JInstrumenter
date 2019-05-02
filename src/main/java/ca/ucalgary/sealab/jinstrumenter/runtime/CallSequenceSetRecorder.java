package ca.ucalgary.sealab.jinstrumenter.runtime;

import org.softevo.util.asm.ClassIdentifierMap;
import org.softevo.util.asm.IdentifierMap;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CallSequenceSetRecorder {
	protected static Logger logger = Logger.getLogger("JInstrumenter");

	private static final String REVISION = "2";

	public static final int METHODID_UNDEF = -1;

	public static final int OBJECTID_STATIC = -2;

	private static final HashMap<Integer, OutputStream> threadTraceMap = new HashMap<Integer, OutputStream>();
	private static Object lock = new Object();

	private static int idCounter = 0;

	public static IdentifierMap methodIdentifierMap = null;
	public static HashSet<String> applicationClasses = null;
	public static ClassIdentifierMap classIdentifierMap = null;

	private static String startTime = new SimpleDateFormat("yy-MM-dd HHmm").format(new Date()).toString();

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownThread()));
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
		writeInThreadOutput("Static method ended m=" + getMethodName(methodId) + " c=" + getClassName(classId));
	}

	public static void staticMethodStarted(int methodId, int classId) {
		writeInThreadOutput("Static method started m=" + getMethodName(methodId) + " c=" + getClassName(classId));
	}

	public static void dynamicMethodEnded(int methodId, int classId, int objectId) {
		writeInThreadOutput("Dynamic method ended m=" + getMethodName(methodId) + " c=" + getClassName(classId) + " o=" + objectId);
	}

	public static void dynamicMethodStarted(int methodId, int classId, int objectId) {
		writeInThreadOutput("Dynamic method started m=" + getMethodName(methodId) + " c=" + getClassName(classId) + " o=" + objectId);
	}

	private static OutputStream getThreadFile() {
		String pattern = System.getProperty("jinst.tracefile", "-");

		if ("-".equals(pattern)) {
			return System.out;
		} else {
			int threadId = getCurrentThreadId();
			String name = pattern
					.replace("%tid%", String.valueOf(threadId))
					.replace("%time%", startTime);
			File file = new File(name);
			file.getParentFile().mkdirs();
			try {
				return new FileOutputStream(file);
			} catch (FileNotFoundException e) {
				logger.log(Level.SEVERE, "Cannot create thread file with name="+name, e);
				return null;
			}
		}
	}

	private static OutputStream getThreadOutput() {
		synchronized (lock) {
			int threadId = getCurrentThreadId();

			OutputStream result = threadTraceMap.get(threadId);
			if (result == null) {
				result = getThreadFile();
				threadTraceMap.put(threadId, result);
			}
			return result;
		}
	}

	protected static boolean writeInThreadOutput(String s) {
		synchronized (lock) {
			try {
				getThreadOutput().write((s + "\r\n").getBytes());
				return true;
			} catch (IOException e) {
				logger.log(Level.SEVERE, "Couldn't write to thread output file", e);
				return false;
			}
		}
	}

	public static int getNextId() {
		synchronized(lock) {
			int result = idCounter;
			idCounter++;
			return result;
		}
	}

	public static class ShutdownThread implements Runnable {
		public void run() {
			synchronized(lock) {
				for (OutputStream writer : threadTraceMap.values()) {
					try {
						writer.flush();
						writer.close();
					} catch (IOException serializeException) {
						logger.log(Level.SEVERE, "Exception occurred in closing logger file", serializeException);
					}
				}
			}
		}
	}
}
