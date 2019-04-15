package de.unisb.cs.st.ample.runtime;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.TraceClassVisitor;
import org.softevo.util.asm.ClassIdentifierMap;
import org.softevo.util.asm.IdentifierMap;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Instrumenter implements ClassFileTransformer {

	public static final String TRACECLASSNAME = "de/unisb/cs/st/ample/runtime/CallSequenceSetRecorder";
	public static final String PROPERTIES_FILE_NAME = "ignore_list.txt";

	private IdentifierMap methodIdentifierMap = new IdentifierMap();
	
	private ClassIdentifierMap classIdentifierMap = new ClassIdentifierMap();
	
	private static Instrumenter singletonInstance = new Instrumenter();

	private HashSet<String> preloadedClasses = new HashSet<String>();
	
	private Pattern forbiddenPatterns;
	private HashSet<String> forbiddenFixedPatterns = new HashSet<String>();

	private HashSet<String> applicationClasses = new HashSet<String>();
	
	private Instrumenter() {
		createForbiddenClasses();
	}

	private void createForbiddenClasses() {
		try {
			ArrayList<String> regexes = new ArrayList<>();
			try (Stream<String> stream = Files.lines(Paths.get(PROPERTIES_FILE_NAME))) {
				stream.forEach((line) -> {
					line = line.trim();
					if (line.contains("*")) {
						regexes.add(line.replaceAll("\\*", "([^/]+)"));
					} else {
						forbiddenFixedPatterns.add(line);
					}
				});
			}
			forbiddenPatterns = Pattern.compile("(" + String.join(")|(", regexes) + ")");
		} catch (IOException e) {
			System.err.println("Couldn't read ignore list file");
		}
//		forbiddenPatterns.add("java/util/LinkedList");
//		forbiddenPatterns.add("java/util/Queue");
//		forbiddenPatterns.add("java/util/AbstractSequentialList");
//		forbiddenPatterns.add("java/util/LinkedList$Entry");
//		forbiddenPatterns.add("java/util/HashMap$KeySet");
//		forbiddenPatterns.add("java/util/HashMap$KeyIterator");
//		forbiddenPatterns.add("java/util/HashMap$HashIterator");
//		forbiddenPatterns.add("java/io/DataOutputStream");
//		forbiddenPatterns.add("java/io/DataOutput");
        System.err.println("....Initialized Instrumenter....");
	}
	
	public static void premain(String agentArgs, Instrumentation inst) {
		Instrumenter instance = getInstrumenter();

		CallSequenceSetRecorder.classIdentifierMap = instance.classIdentifierMap;
		CallSequenceSetRecorder.methodIdentifierMap = instance.methodIdentifierMap;
		CallSequenceSetRecorder.applicationClasses = instance.applicationClasses;

		inst.addTransformer(instance);

		Class[] loadedClasses = inst.getAllLoadedClasses();
		for (int counter = 0; counter < loadedClasses.length; counter++) {
			String className = loadedClasses[counter].getName().replace('.', '/');
			instance.preloadedClasses.add(className);
		}
		instance.preloadedClasses.addAll(instance.forbiddenFixedPatterns);
	}

	public byte[] transform(ClassLoader loader, String className, 
	                        Class<?> classBeingRedefined, ProtectionDomain protectionDomain, 
	                        byte[] classBytes)
	throws IllegalClassFormatException {
		try {
		if (shouldSkip(className)) {
		//if (!forbiddenPatterns.contains(className) && className.startsWith("ca")) {
//		if (!forbiddenPatterns.contains(className)) {
			if (loader != null) {
				applicationClasses.add(className);
			}
			return transform(classBytes, className);
		} else {
			return classBytes;
		}
		} catch (Throwable oops) {
			oops.printStackTrace(System.out);
			return classBytes;
		}
	}

	private boolean shouldSkip(String className) {
		return !forbiddenFixedPatterns.contains(className) &&
				!forbiddenPatterns.matcher(className).matches();

//		return !className.startsWith("de/unisb") && !className.startsWith("java") &&
//				!className.startsWith("org/apache/tools/ant") && !className.startsWith("jdk") &&
//				!className.startsWith("org/junit") && !className.startsWith("com/sun") &&
//				!className.startsWith("sun") && !forbiddenFixedPatterns.contains(className);
	}

	private PrintWriter createScratchFile(String name) throws IOException {
	    name = name.replace('/', '.');
        return new PrintWriter("/Users/mjafar/Library/Preferences/IntelliJIdea2018.2/scratches/"+name+".java");
    }

	protected byte[] transform(byte[] classBytes, String className) {
        // try {
            System.err.println("Transforming " + className);
            // displayClass(classBytes, createScratchFile(className + "-before"));
            ClassReader classReader = new ClassReader(classBytes);
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            CallSequenceClassVisitor visitor = new CallSequenceClassVisitor(
                    classWriter,
                    methodIdentifierMap,
                    classIdentifierMap.getIdentifier(className),
                    preloadedClasses
            );
            classReader.accept(visitor, ClassReader.SKIP_FRAMES);
            // displayClass(classWriter.toByteArray(),  createScratchFile(className + "-after"));
            return classWriter.toByteArray();
        // } catch (IOException ignored) {
        //     ignored.printStackTrace();
        // }
        // return null;
	}
	
	public static Instrumenter getInstrumenter() {
		return singletonInstance;
	}

	private static void displayClass(byte[] classBytes, PrintWriter w) {
		ClassReader classReader;
		TraceClassVisitor traceVisitor;

		classReader = new ClassReader(classBytes);
		traceVisitor = new TraceClassVisitor(w);
		classReader.accept(traceVisitor, 0);
	}
}
