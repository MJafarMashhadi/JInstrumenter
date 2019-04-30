package ca.ucalgary.sealab.jinstrumenter.runtime;

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
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Instrumenter implements ClassFileTransformer {
	protected static Logger logger = Logger.getLogger("JInstrumenter");

	public static final String TRACECLASSNAME = "ca/ucalgary/sealab/jinstrumenter/runtime/CallSequenceSetRecorder";

	private IdentifierMap methodIdentifierMap = new IdentifierMap();
	
	private ClassIdentifierMap classIdentifierMap = new ClassIdentifierMap();
	
	private static Instrumenter singletonInstance = new Instrumenter();

	private HashSet<String> preloadedClasses = new HashSet<String>();

	private enum FunctionFilter {
		WHITE_LIST {
			@Override
			public boolean shouldSkip(String className) {
				return !fixedNames.contains(className) &&
						(namePatterns == null || !namePatterns.matcher(className).matches());
			}

			@Override
			public Collection<String> getSkippedNames() {
				return new HashSet<>();
			}
		},
		BLACK_LIST {
			@Override
			public boolean shouldSkip(String className) {
				return fixedNames.contains(className) ||
						(namePatterns != null && namePatterns.matcher(className).matches());
			}

			@Override
			public Collection<String> getSkippedNames() {
				return fixedNames;
			}
		};
		protected static Pattern namePatterns;
		protected static Set<String> fixedNames = new HashSet<>();

		public abstract Collection<String> getSkippedNames();
		public abstract boolean shouldSkip(String className);
	}
	private FunctionFilter functionFilter;

	private HashSet<String> applicationClasses = new HashSet<String>();
	
	private Instrumenter() {
		readFilterFile();
		logger.log(Level.INFO, "Initialized instrumenter");
	}

	private String getFilterFileName() {
		String fileName = System.getProperty("jinst.filterfile");
		if (fileName == null) {
			fileName = "jifilter.txt";
		}
		logger.log(Level.INFO, "Filter file name " + fileName);
		return fileName;
	}

	private void readFilterFile() {
		try {
			ArrayList<String> regexes = new ArrayList<>();
			try (Stream<String> stream = Files.lines(Paths.get(getFilterFileName()))) {
				stream.forEach((line) -> {
					line = line.trim();
					if (line.startsWith("#") || line.length() == 0) {
						// Do nothing
					} else if (line.matches("\\[\\w+\\]")) {
						// Directive
						switch (line.toLowerCase().substring(1, line.length()-1)) {
							case "white":
							case "include":
							case "whitelist":
								functionFilter = FunctionFilter.WHITE_LIST;
								break;
							case "black":
							case "exclude":
							case "blacklist":
								functionFilter = FunctionFilter.BLACK_LIST;
								break;
						}
					} else {
						if (line.contains("*")) {
							regexes.add(line.replaceAll("\\*", ".+"));
						} else {
							FunctionFilter.fixedNames.add(line);
						}
					}
				});
			}
			logger.log(Level.INFO,
					"Loaded filter file. Type=" + functionFilter.toString() +
							" fixed#=" + FunctionFilter.fixedNames.size() +
							" pattern#=" + regexes.size());
			FunctionFilter.namePatterns = Pattern.compile("(" + String.join(")|(", regexes) + ")");
		} catch (IOException e) {
			logger.log(Level.WARNING, "Couldn't read filter file");
		}
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
		instance.preloadedClasses.addAll(instance.functionFilter.getSkippedNames());
	}

	public byte[] transform(ClassLoader loader, String className, 
	                        Class<?> classBeingRedefined, ProtectionDomain protectionDomain, 
	                        byte[] classBytes)
	throws IllegalClassFormatException {
		if (functionFilter.shouldSkip(className)) {
			// Skip
			return classBytes;
		}

		try {
			if (loader != null) {
				applicationClasses.add(className);
			}

			return transform(classBytes, className);
		} catch (Throwable oops) {
			logger.log(Level.SEVERE, "Error in transforming class " + className, oops);
			return classBytes;
		}
	}

	protected byte[] transform(byte[] classBytes, String className) {
		logger.log(Level.INFO, "Transforming " + className);
		ClassReader classReader = new ClassReader(classBytes);
		ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		CallSequenceClassVisitor visitor = new CallSequenceClassVisitor(
				classWriter,
				methodIdentifierMap,
				classIdentifierMap.getIdentifier(className),
				preloadedClasses
		);
		classReader.accept(visitor, ClassReader.SKIP_FRAMES);
		return classWriter.toByteArray();
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
