package ca.ucalgary.sealab.jinstrumenter.test;

import org.softevo.util.datastructures.Graph;
import org.softevo.util.datastructures.GraphNode;

import java.io.File;
import java.util.HashSet;
import java.util.Hashtable;

public class RandomCallGraphGenerator {

	private double methodCallDensity;
	
	private int numberOfObjectsPerClass;
	
	private int numberOfMethodsPerClass;
	
	private int numberOfClasses;

	private int meanNumberOfCallExecutions;
	
	private Hashtable<String, ClassInformation> classNameToInformationMap;
	
	private Graph<MethodInformation, CallInformation> callGraph;
	
	public RandomCallGraphGenerator(int numberOfClasses, int numberOfMethodsPerClass,
	                                int numberOfObjectsPerClass, double methodCallDensity,
	                                int meanNumberOfCallExecutions) {
		this.numberOfClasses = numberOfClasses;
		this.numberOfMethodsPerClass = numberOfMethodsPerClass;
		this.numberOfObjectsPerClass = numberOfObjectsPerClass;
		this.methodCallDensity = methodCallDensity;
		this.meanNumberOfCallExecutions = meanNumberOfCallExecutions;
	}
	
	private int getRandomValue(int baseValue) {
		return ((int) ((double) baseValue * (Math.random() * 0.4d + 0.8d)));
	}
	
	private Hashtable<String, ClassInformation> generateClassNameToInformationMap() {
		Hashtable<String, ClassInformation> result = new Hashtable<String, ClassInformation>();
		int numberOfClasses = getRandomValue(this.numberOfClasses);
		for (int counter = 0; counter < numberOfClasses; counter++) {
			String className = "GeneratedClass" + counter;
			int numberOfObjects = getRandomValue(this.numberOfObjectsPerClass);
			ClassInformation classInformation = new ClassInformation(className);
			for (int counter2 = 0; counter2 < numberOfObjects; counter2++) {
				classInformation.addObjectIdentifier("object" + counter2);
			}
			int numberOfMethods = getRandomValue(this.numberOfMethodsPerClass);
			for (int counter2 = 0; counter2 < numberOfMethods; counter2++) {
				classInformation.addMethodIdentifier("method" + counter2);
			}
			result.put(className, classInformation);
		}
		return result;
	}
	
	private Graph<MethodInformation, CallInformation> generateCallGraph(Hashtable<String, ClassInformation> classNameToInformationMap) {
		Graph<MethodInformation, CallInformation> result = new Graph<MethodInformation, CallInformation>();
		for (String className : classNameToInformationMap.keySet()) {
			ClassInformation classInformation = classNameToInformationMap.get(className);
			for (String objectIdentifier : classInformation.getObjectIdentifiers()) {
				for (String methodIdentifier : classInformation.getMethodIdentifiers()) {
					result.createNode(new MethodInformation(className, objectIdentifier, methodIdentifier));
				}
			}
		}
		for (String className : classNameToInformationMap.keySet()) {
			ClassInformation classInformation = classNameToInformationMap.get(className);
			for (String objectIdentifier : classInformation.getObjectIdentifiers()) {
				for (String methodIdentifier : classInformation.getMethodIdentifiers()) {
					MethodInformation methodInformation = new MethodInformation(className, objectIdentifier, methodIdentifier);
					GraphNode<MethodInformation, CallInformation> startNode = result.getNodeForData(methodInformation);
					int counter = 0;
					for (GraphNode<MethodInformation, CallInformation> graphNode : result.getNodes()) {
						if (Math.random() < methodCallDensity) {
							int numberOfCallExecutions = getRandomValue(meanNumberOfCallExecutions);
							result.createEdge(new CallInformation(counter, numberOfCallExecutions), startNode, graphNode);
							counter++;
						}
					}
				}
			}
		}
		MethodInformation mainMethodInformation = new MethodInformation("Main", null, "main");
		GraphNode<MethodInformation, CallInformation> mainNode = result.createNode(mainMethodInformation);
		HashSet<String> visitedClassesList = new HashSet<String>();
		int counter = 0;
		for (GraphNode<MethodInformation, CallInformation> graphNode : result.getNodes()) {
			if (!visitedClassesList.contains(graphNode.getData().getClassName())) {
				visitedClassesList.add(graphNode.getData().getClassName());
				int numberOfExecutions = getRandomValue(meanNumberOfCallExecutions);
				result.createEdge(new CallInformation(counter, numberOfExecutions), mainNode, graphNode);
				counter++;
			}
		}
		ClassInformation mainClassInformation = new ClassInformation("Main");
		classNameToInformationMap.put("Main", mainClassInformation);
		return result;
	}
	
	public void generateCallGraph() {
		classNameToInformationMap = generateClassNameToInformationMap();
		callGraph = generateCallGraph(classNameToInformationMap);
	}
	
	public Graph<MethodInformation, CallInformation> getCallGraph() {
		return callGraph;
	}

	public Hashtable<String, ClassInformation> getClassNameToInformationMap() {
		return classNameToInformationMap;
	}
	
	public static void main(String[] args) throws Exception {
		RandomCallGraphGenerator generator = new RandomCallGraphGenerator(5, 2, 3, 0.5, 5);
		generator.generateCallGraph();
		ClassesGenerator classGenerator = new ClassesGenerator(generator.getCallGraph(), generator.getClassNameToInformationMap(),
		                                                               new File("/tmp/testclasses"));
		classGenerator.generateClasses();
	}

}
