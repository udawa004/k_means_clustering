import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.*;

public class kcluster {
	static String inputFileName;
	static String criterionFunction;
	static String classFileName;
	static int nclusters;
	static int ntrials;
	static String outputFileName;
	static Map<Integer, Map<Integer,Double>> fileList= new HashMap<>();
	static int maxDimension = 0;
	static int classLabelSize=0;
	static Map<Integer,Map<Integer,Double>> centroids = new HashMap<>();
	
	static Map<Integer, List<Integer>> clusters = new HashMap<>();
	static int[] seedList = {1,3,5,7,9,11,13,15,17,19,21,23,25,27,29,31,33,35,37,39};
	static double bestScore = 0;
	static Map<Integer, List<Integer>> bestClusters = new HashMap<>();
	
	static LinkedHashMap<String, Integer> topicList = new LinkedHashMap<String, Integer>();
	static HashMap<Integer, Integer> articleLabelMap = new HashMap<Integer, Integer>();
	static int[][] outputMatrix;
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		inputFileName = args[0];
		criterionFunction = args[1];
		classFileName = args[2];
		nclusters = Integer.parseInt(args[3]);
		ntrials = Integer.parseInt(args[4]);
		outputFileName = args[5];
		
		//Read input file
		FileReader fr = new FileReader(new File(inputFileName).getAbsoluteFile());
		BufferedReader bufferedReader = new BufferedReader(fr);
		String line = new String();
		while((line = bufferedReader.readLine()) != null){		
			String[] inData = line.split(",");
			int newid = Integer.parseInt(inData[0]);
			int dimensionNum = Integer.parseInt(inData[1]);
			
			if(dimensionNum>maxDimension){
				maxDimension = dimensionNum;
			}
			
			double freq = Double.parseDouble(inData[2]);
			
			if(fileList.containsKey(newid)){	
				Map<Integer,Double> currMap = fileList.get(newid); 
				currMap.put(dimensionNum,freq);
				fileList.put(newid, currMap);
			}else{
				Map<Integer,Double> freqMap = new HashMap<>();
				freqMap.put(dimensionNum, freq);
				fileList.put(newid, freqMap);
			}
		}
		bufferedReader.close();
		fr.close();
		
		for(int i = 0; i<ntrials; i++){
			generateCentroids(seedList[i]);
			
			for(int j=0;j<20;j++){
				int pointsMoved = assignPoints();
				computeClusterCentroids();
				if(pointsMoved < 100){
					break;
				}
				
				double currScore = calculateObjectiveValue();
				if(currScore > bestScore){
					bestScore = currScore;
					bestClusters = clusters;
				}				
			}
		}
		
		generateOutputFile();
		
		readClassFile();
		outputMatrix = new int[nclusters][classLabelSize]; 
		generateOutputMatrix();
		calculateEntropyAndPurity();
	}
	
	public static void calculateEntropyAndPurity()
	{
		Double totalEntropy=0.0;
		Double totalPurity=0.0;
		for(int i=0;i<bestClusters.size();i++)
		{
			double entropy =0.0;
			double clusterSize = bestClusters.get(i).size();
			double maxPWithinCluster =0.0;
			for(int j=0;j<classLabelSize;j++)
			{
				double thisEntropy =0.0;
				if(clusterSize!=0)
				{
					double Pij = (outputMatrix[i][j])/((double)clusterSize);
					if(Pij!=0)
					{
						thisEntropy = (Pij*(Math.log(Pij)/Math.log(2)));
					}
					entropy+=thisEntropy;
					if(Pij>maxPWithinCluster)
						maxPWithinCluster = Pij;
				}
			}
			totalEntropy+=((clusterSize/fileList.size())*entropy);;
			totalPurity+=((clusterSize/fileList.size())*maxPWithinCluster);
		}
		System.out.println();
		System.out.println("\nMax Objective Value:"+bestScore);
		System.out.println("Entropy:"+(-totalEntropy));
		System.out.println("Purity:"+totalPurity);
	}
	
	private static void readClassFile() throws IOException 
	{
		File inFile = new File(classFileName);
		FileReader fr = new FileReader(inFile);
		BufferedReader br=new BufferedReader(fr);
		String line = new String();
		int topicIndex=0;
		while ( (line = br.readLine()) != null)
		{
			
			String[] label = line.split(",");
			Integer newID = label[0]!=null?Integer.parseInt(label[0]):null;
			String topic = label[1]!=null?label[1]:null;
			int index =0;
			if(topicList.containsKey(topic))
			{
				index=topicList.get(topic);
			}
			else
			{
				topicList.put(topic, topicIndex);
				index=topicIndex;
				topicIndex++;
			}
			articleLabelMap.put(newID, index);
		}
		br.close();
		fr.close();
		classLabelSize = topicList.size();
	}
	
	public static void generateOutputMatrix() 
	{
		Iterator<Entry<Integer,List<Integer>>> clusterIter = bestClusters.entrySet().iterator();

		while(clusterIter.hasNext()){
			Entry<Integer,List<Integer>> cluster = (Entry<Integer,List<Integer>>)clusterIter.next();
			int clusterId = cluster.getKey();
			List<Integer> clusterFiles = cluster.getValue();
			for(Integer newid:clusterFiles){
				int labelIndex =articleLabelMap.get(newid);
				outputMatrix[clusterId][labelIndex]+=1;
			}
			
		}
		
		System.out.println("Output Matrix:");
		System.out.println("");
		String[] topics = topicList.keySet().toArray(new String[0]);
		System.out.print(String.format("%15s","Labels->"));
		for(int i=0;i<topics.length;i++)
			System.out.print(String.format("%15s",topics[i]));
		for(int i =0;i<outputMatrix.length;i++)
		{
			System.out.println();
			System.out.print(String.format("%15s","Cluster "+i+":"));
			for(int j=0;j<outputMatrix[0].length;j++)
				System.out.print(String.format("%15s",outputMatrix[i][j]));
		}
	}
	
	public static void generateOutputFile() throws IOException
	{
		File outFile = new File(outputFileName);
		if (!outFile.exists()) {
			outFile.createNewFile();
		}
		FileWriter fw = new FileWriter(outFile.getAbsoluteFile()); 
		BufferedWriter bw = new BufferedWriter(fw);
		try {
			Iterator<Entry<Integer,List<Integer>>> clusterIter = bestClusters.entrySet().iterator();

			while(clusterIter.hasNext()){
				Entry<Integer,List<Integer>> cluster = (Entry<Integer,List<Integer>>)clusterIter.next();
				int clusterId = cluster.getKey();
				List<Integer> clusterFiles = cluster.getValue();
				for(Integer newid:clusterFiles){
					bw.write(newid+","+clusterId+"\n");
				}
			}
		}catch(IOException E){
			E.printStackTrace();
		}
		bw.close();
		fw.close();
	}
	
	public static void generateCentroids(int seed)
	{
		Random randomGenerator = new Random(seed);
		for(int i=0;i<nclusters;i++)
		{
			int selectedPoint = randomGenerator.nextInt(fileList.size()-1) +1;
			List<Integer> fileIds = new ArrayList<Integer>(fileList.keySet());
			centroids.put(i, fileList.get(fileIds.get(selectedPoint)));			
		}
	}
	
	public static double getMagnitude(Map<Integer,Double> point){
		double mag = 0;
		for(Double v : point.values()){
			mag = mag + (v*v);
		}
		return mag;
	}
	
	
	
	public static double getSimilarity(Map<Integer,Double> pointX, Map<Integer,Double> pointY){
		double similarity = 0;
		if(criterionFunction.equals("SSE")){
			Iterator<Entry<Integer, Double>> iter = pointX.entrySet().iterator();
			while(iter.hasNext())
			{
				Entry<Integer,Double> pointXPair = (Entry<Integer,Double>)iter.next();
				int dimNo = pointXPair.getKey();
				Double pointXDimFreq = pointXPair.getValue();
				if(pointY.containsKey(dimNo))
				{
					Double pointYDimFreq = pointY.get(dimNo);
					double diff = pointXDimFreq-pointYDimFreq;
					similarity = similarity + (diff*diff);
				}
			}
		}else if(criterionFunction.equals("I2")){
			Iterator<Entry<Integer, Double>> iter = pointX.entrySet().iterator();
			double dotSum=0;
			while(iter.hasNext())
			{
				Entry<Integer,Double> pointXPair = (Entry<Integer,Double>)iter.next();
				int dimNo = pointXPair.getKey();
				Double pointXDimFreq = pointXPair.getValue();
				if(pointY.containsKey(dimNo))
				{
					Double pointYDimFreq = pointY.get(dimNo);
					dotSum+= pointXDimFreq*pointYDimFreq;
				}
			}
			similarity = dotSum/(Math.sqrt(getMagnitude(pointX))*Math.sqrt(getMagnitude(pointY)));
		}
		return similarity;
	}
	
	public static int assignPoints(){
		int pointsMoved = 0;
		Map<Integer, List<Integer>> newClusters = new HashMap<>();
		Iterator<Entry<Integer,Map<Integer,Double>>> fileIter = fileList.entrySet().iterator();
		while(fileIter.hasNext()){
			double bestSimilarity = 0;
			int bestCluster = 0;
			Entry<Integer,Map<Integer,Double>> file = (Entry<Integer,Map<Integer,Double>>)fileIter.next();
			
			Iterator<Entry<Integer,Map<Integer,Double>>> centroidIter = centroids.entrySet().iterator();
			while(centroidIter.hasNext()){
				Entry<Integer,Map<Integer,Double>> centroid = (Entry<Integer,Map<Integer,Double>>)centroidIter.next();
				double currSimilarity = getSimilarity(file.getValue(),centroid.getValue());
				if(currSimilarity > bestSimilarity){
					bestSimilarity = currSimilarity;
					bestCluster = centroid.getKey();
				}
			}
			if(clusters.get(bestCluster)==null || clusters.get(bestCluster).contains(file.getKey()))
				pointsMoved++;
			
			if(newClusters.containsKey(bestCluster)){				
				newClusters.get(bestCluster).add(file.getKey());
			}else{
				List<Integer> fileList = new ArrayList<>();
				fileList.add(file.getKey());	
				newClusters.put(bestCluster, fileList);
			}				
		} 
		clusters = newClusters;
		return pointsMoved;
	}
	
	public static double calculateObjectiveValue(){
		Iterator<Entry<Integer,List<Integer>>> clusterIter = clusters.entrySet().iterator();
		double objectiveValue = 0;
		while(clusterIter.hasNext()){
			Entry<Integer,List<Integer>> cluster = (Entry<Integer,List<Integer>>)clusterIter.next();
			int clusterId = cluster.getKey();
			List<Integer> clusterFiles = cluster.getValue();
			Map<Integer,Double> centroidFreqMap =  centroids.get(clusterId);
			for(Integer newid:clusterFiles){
				Map<Integer,Double> fileFreqMap = fileList.get(newid);
				objectiveValue += getSimilarity(fileFreqMap, centroidFreqMap);
			}
		}
		
		return objectiveValue;
	}
		
	public static void computeClusterCentroids(){
		Iterator<Entry<Integer,List<Integer>>> clusterIter = clusters.entrySet().iterator();
					
		while(clusterIter.hasNext()){
		
			Map<Integer,Double> newCentroid = new HashMap<>();
			Entry<Integer,List<Integer>> cluster = (Entry<Integer,List<Integer>>)clusterIter.next();
			
			List<Integer> clusterFiles = cluster.getValue();
			
			for(Integer newid:clusterFiles){
				Map<Integer,Double> freqMap = fileList.get(newid);
				
				Iterator<Entry<Integer,Double>> clusterFileIter = freqMap.entrySet().iterator();
				
				while(clusterFileIter.hasNext()){
					Entry<Integer,Double> dimfreq = (Entry<Integer,Double>)clusterFileIter.next();
					int dimNo = dimfreq.getKey(); 
					double freq = dimfreq.getValue(); 
					if (newCentroid.containsKey(dimNo)){
						newCentroid.put(dimNo, newCentroid.get(dimNo)+freq);
					}
					else {
						newCentroid.put(dimNo, freq);
					}
				}
			}
			
			for (Entry<Integer, Double> e : newCentroid.entrySet()) {
			    e.setValue(e.getValue()/cluster.getValue().size());
			}
		}
	}
}
