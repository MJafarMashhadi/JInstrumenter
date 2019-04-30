package ca.ucalgary.sealab.jinstrumenter.test;

import org.apache.commons.io.FileUtils;
import org.softevo.util.datastructures.Graph;
import org.softevo.util.datastructures.GraphEdge;
import org.softevo.util.datastructures.GraphNode;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Vector;

public class ClassesGenerator {

	private Graph<MethodInformation, CallInformation> callGraph = null;
	
	private Hashtable<String, ClassInformation> classNameToInformationMap;

	private File targetDirectory;

	private String packageName = "generated";
	
	public ClassesGenerator(Graph<MethodInformation, CallInformation> callGraph, 
	                            Hashtable<String, ClassInformation> classNameToInformationMap,
	                            File targetDirectory) {
		this.callGraph = callGraph;
		this.classNameToInformationMap = classNameToInformationMap;
		this.targetDirectory = targetDirectory;
	}
	
	public void generateClasses() throws IOException {
		System.out.println("Cleaning target directory " + targetDirectory);
		FileUtils.cleanDirectory(targetDirectory);
		File packageDirectory = new File(targetDirectory + File.separator + packageName);
		packageDirectory.mkdirs();
		for (String className : classNameToInformationMap.keySet()) {
			String javaClass;
			if (!className.equals("Main")) {
				javaClass = generateClass(classNameToInformationMap.get(className));
			} else {
				MethodInformation methodInformation = new MethodInformation("Main", null, "main");
				GraphNode<MethodInformation, CallInformation> graphNode = callGraph.getNodeForData(methodInformation);
				javaClass = generateMainClass(graphNode);
			}
			File targetFile = new File(packageDirectory + File.separator + className + ".java");
			FileUtils.writeStringToFile(targetFile, javaClass, "ISO-8859-1");
		}
	}
	
	public static String getCounterIdentifier(String methodIdentifier, String objectIdentifier, int position) {
		return "counter_" + methodIdentifier + "_" + objectIdentifier + "_" + position;
	}
	
	public Vector<GraphEdge<CallInformation>> sortEdges(Vector<GraphEdge<CallInformation>> edges) {
		Vector<GraphEdge<CallInformation>> result = new Vector<GraphEdge<CallInformation>>(edges);
		Collections.sort(result, new Comparator<GraphEdge<CallInformation>>() {

			public int compare(GraphEdge<CallInformation> first, GraphEdge<CallInformation> second) {
				return first.getData().compareTo(second.getData());
			}
			
		});
		return result;
	}
	
	protected String generateClass(ClassInformation classInformation) {
		StringBuffer result = new StringBuffer("package " + packageName + ";\n\n");
		result.append("public class " + classInformation.getClassName() + " {\n\n");
		for (String objectIdentifier : classInformation.getObjectIdentifiers()) {
			result.append("\tpublic static " + classInformation.getClassName() + " " + 
			              objectIdentifier + " = new " + classInformation.getClassName() + "(\"" +
			              objectIdentifier + "\");\n\n");
		}
		for (String objectIdentifier : classInformation.getObjectIdentifiers()) {
			for (String methodIdentifier : classInformation.getMethodIdentifiers()) {
				MethodInformation methodInformation = new MethodInformation(classInformation.getClassName(), objectIdentifier, methodIdentifier);
				GraphNode<MethodInformation, CallInformation> graphNode = callGraph.getNodeForData(methodInformation);
				for (GraphEdge<CallInformation> edge : graphNode.getOutgoingEdges()) {
					result.append("\tprivate static int " + getCounterIdentifier(methodIdentifier, objectIdentifier, edge.getData().getPosition()) + 
					              " = " + edge.getData().getNumberOfExecutions() + ";\n\n");
				}
			}
		}
		result.append("\tprivate String objectIdentifier = null;\n\n");
		result.append("\tpublic " + classInformation.getClassName() + "(String objectIdentifier) {\n");
		result.append("\t\tthis.objectIdentifier = objectIdentifier;\n");
		result.append("\t}\n\n");
		for (String methodIdentifier : classInformation.getMethodIdentifiers()) {
			result.append("\tpublic void " + methodIdentifier + "() {\n");
			for (String objectIdentifier : classInformation.getObjectIdentifiers()) {
				MethodInformation methodInformation = new MethodInformation(classInformation.getClassName(), objectIdentifier, methodIdentifier);
				GraphNode<MethodInformation, CallInformation> graphNode = callGraph.getNodeForData(methodInformation);
				result.append("\t\tif (objectIdentifier.equals(\"" + objectIdentifier + "\") {\n");
				Vector<GraphEdge<CallInformation>> sortedEdges = sortEdges(graphNode.getOutgoingEdges());
				for (GraphEdge<CallInformation> edge : sortedEdges) {
					GraphNode<MethodInformation, CallInformation> targetNode = edge.getEndNode();
					MethodInformation targetMethod = targetNode.getData();
					String counterIdentifier = getCounterIdentifier(methodIdentifier, objectIdentifier, edge.getData().getPosition());
					result.append("\t\t\tif (" + counterIdentifier + " > 0) {\n");
					result.append("\t\t\t\t" + counterIdentifier + "--;\n");
					result.append("\t\t\t\t" + targetMethod.getClassName() + "." + targetMethod.getObjectIdentifier() + "." + targetMethod.getMethodIdentifier() + "();\n");
					result.append("\t\t\t}\n");
				}
				result.append("\t\t}\n");
			}
			result.append("\t}\n\n");
		}
		result.append("}\n");
		return result.toString();
	}
	
	protected String generateMainClass(GraphNode<MethodInformation, CallInformation> mainNode) {
		StringBuffer result = new StringBuffer("package " + packageName + ";\n\n");
		result.append("public class Main {\n\n");
		result.append("\tpublic static void main(String[] args) {\n");
		Vector<GraphEdge<CallInformation>> sortedEdges = sortEdges(mainNode.getOutgoingEdges());
		for (GraphEdge<CallInformation> edge : sortedEdges) {
			MethodInformation targetInformation = (MethodInformation) edge.getEndNode().getData();
			result.append("\t\tfor (int counter = 0; counter < " + edge.getData().getNumberOfExecutions() + "; counter++) {\n");
			result.append("\t\t\t" + targetInformation.getClassName() + "." + targetInformation.getClassName() + "." + targetInformation.getMethodIdentifier() + "();\n");
			result.append("\t\t}\n");
		}
		result.append("\t}\n");
		result.append("}\n");
		return result.toString();
	}
}
