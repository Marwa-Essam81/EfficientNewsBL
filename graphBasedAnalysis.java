package codeForSharing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//import DocGraph.Edge;

public class graphBasedAnalysis {

	/**
	 * A method that generates a search query using the basic KCore method
	 * @param Content
	 * @param title
	 * @param window
	 * @param numkeywords
	 * @param percentage
	 * @return
	 */
	public static String basicKCoreExactlyK(String Content,String title,int window,int numkeywords,double percentage)
	{
		String searchQuery="";
		
		DocGraph dg=constructArticleGraph(Content,window);
	
		/** copy the graph before starting the decomposition process
		 * 
		 */
		DocGraph qDocGraph=dg.Copy();
		
		//Decompose the Graph into Cores and Find the Core of each node(Vertex)
		HashMap<String,Integer> NodesCores=new HashMap<String,Integer>();
		NodesCores=kCoreExtractionUpdated(dg);
		
		HashMap<String,Integer> nodes_CR=CRE_Core(NodesCores,qDocGraph);
    	
		ArrayList termBoosts_Sorted= (ArrayList) nodes_CR.entrySet().stream().sorted(HashMap.Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toList());
      int kcount=0;
  		searchQuery="";
          for (int i=0;i<termBoosts_Sorted.size();i++) {
  					
        	  searchQuery=searchQuery+termBoosts_Sorted.get(i).toString().replace("=","^")+" ";
  			kcount=kcount+1;
  			if(kcount==numkeywords)
  				break;
  		}
  		
     	
 	   return searchQuery.trim();
    	
		
	}
	
	public static DocGraph constructArticleGraph(String Content,int window)
	{

				// step 1- add all unique tokens to a graph structure
	      List<String> results = Arrays.asList(Content.split(" "));  

		DocGraph dg=new DocGraph();
				for (int i = 0; i < results.size(); i++) {
					dg.addNode(results.get(i));
				}
				//Step 2 - add all edges given the input window
				if(window<1)
					{
						System.out.println("window should at least be 1");
						return null;
					}
				//System.out.println("Step 1: tokens are added as nodes to the graph");

				for (int j = 0; j < results.size()-1; j++) {
					String sj=results.get(j);
						for(int k=j+1;k<=j+window;k++)
							{
							if(k<results.size())
								if(!sj.equals(results.get(k))) // to escape adding edges between difference occurrences of a term
								{
									dg.addEdge(results.get(j),results.get(k));
								}
							}
				}
				//System.out.println("Step 2: The following is the constructed graph");

				return dg;
	}
	public  static HashMap<String,Integer> kCoreExtractionUpdated(DocGraph dg)
	{
		
		/*we need to initialize a core structure for storing core numbers of nodes... 
		 * initially all nodes are in core 1 
		 */
		HashMap<String,Integer> nodesCores=new HashMap<String,Integer>();
		int core=0;
		do
		{
			core=core+1;	
	//		System.out.println("Peeling nodes at core:"+core);
			ArrayList<String> peeled=corePeelOff(dg,core);
			
			for(int i=0;i<peeled.size();i++)
			{	nodesCores.put(peeled.get(i),core);
			}
			
			
		} while(dg.Nodes.size()!=0); // stop when all nodes are peeled off
		
		//System.out.println("Final Core is "+ Integer.toString(core-1));
	
		return nodesCores;
		
	}
	public static ArrayList<String> 	corePeelOff(DocGraph dg,int k)
	{
		ArrayList<String>  overallpeeled=new ArrayList<String>();

		
		boolean resume;
		int lastPeeledindex=0;
		do {
			resume=false;
			ArrayList<String>  peeledinIteration=new ArrayList<String>();

			Iterator it = dg.Nodes.entrySet().iterator();
		
			while (it.hasNext()) {
				Map.Entry<String,Integer> mp =(Map.Entry<String,Integer>)it.next();
				String node=mp.getKey();
					if(dg.getDegree(node)<=k)
					{
				//		System.out.println("adding node:"+node);
						resume=true; // a node is peeled off- there is a possibility for other nodes to be removed later.
						peeledinIteration.add(node);
					}
			}
			overallpeeled.addAll(peeledinIteration);
	//		System.out.println("peeled nodes:"+peeledinIteration.size());
			for(int i=0;i<peeledinIteration.size();i++)
			{
	//			System.out.println("peeling off node:"+peeledinIteration.get(i));
				dg.removeAllEdges(peeledinIteration.get(i));
				dg.Nodes.remove(peeledinIteration.get(i));
			}
		
		}
		while(resume);
		
		return overallpeeled;
		
	}
	public static HashMap<String,Integer> CRE_Core(HashMap<String,Integer> NodesCores,DocGraph qDocGraph )
	{
		HashMap<String,Integer> nodesCRE_Core=new HashMap<String,Integer>();
		Integer[] CREValues=new Integer[NodesCores.size()];
		int index=0;
		String keywords="";
		//qDocGraph.printGraph();
		Iterator it =NodesCores.keySet().iterator();
		while (it.hasNext()) {
			String node=(String)it.next();
		//	System.out.println(node);
			// find the neighbors of the node from the graph. 
			ArrayList<String> neighbors=qDocGraph.neighborsOf(node);
		//	System.out.println("Neighbors---> ");

			// looping the neighbors to get the total weight of this node
			Iterator<String> itn =neighbors.iterator();
			int CRValue=0;
			while (itn.hasNext()) {
				String nb=itn.next();
			//	System.out.print(nb+" , ");
				if(NodesCores.get(nb)==null) System.out.println(nb);
				CRValue+=NodesCores.get(nb);//*qDocGraph.getEdgeWeight(node,nb);
			}
			nodesCRE_Core.put(node,CRValue);//*qDocGraph.getNodeFrequency(node));//,CRValue);	
		}
		return nodesCRE_Core;
	}
	
	
	
	
	
	/**
	 * Implementing K-Truss Methods
	 */
	public static String basicKTrussExactlyK(String Content,String title,int window,int numkeywords,double percentage)
	{
		DocGraph dg=constructArticleGraph(Content,window);
	//	DocGraph qDocGraph=dg.Copy();

		//All Nodes truss number are initially equal to 2 (assuming a node is only a member of one edge -- no other common neighbors between nodes pairs)
		
		HashMap<String,Integer> NodesTruss=new HashMap<String,Integer>();
		
		Iterator it = dg.Nodes.keySet().iterator();
		while (it.hasNext()) {
			String nextNode =(String)it.next();
			NodesTruss.put(nextNode,2);
		}
		// Now -- start the decomposition process
		
		int k=3; // start from k=3 since this is the minimum truss number in which edges can be pruned from the main graph
		
		DocGraph trussGraph=dg.Copy();
		while(true)
		{
			DocGraph tempGraph=trussGraph.Copy();
			// find first the set of edges to be removed
			ArrayList<DocGraph.Edge> edgesToRemove =new ArrayList<DocGraph.Edge>();
			it =tempGraph.Edges.keySet().iterator();
			while (it.hasNext()) {
				DocGraph.Edge nextEdge =(DocGraph.Edge)it.next();
				if(!edgePartofTruss(tempGraph, nextEdge, k))
					edgesToRemove.add(tempGraph.new Edge(nextEdge.LNode,nextEdge.RNode));
			}
			// remove the edges from the graph
			for(int i=0;i<edgesToRemove.size();i++)
				tempGraph.removeEdge(edgesToRemove.get(i).LNode,edgesToRemove.get(i).RNode);
			//clean the nodes that do not have a supported edge anymore
				tempGraph.clearNotSupportedNodes();
			
			// Increasing the truss number of remaining nodes
				it = tempGraph.Nodes.keySet().iterator();
				while (it.hasNext()) {
					String nextNode =(String)it.next();
					NodesTruss.put(nextNode,k);
				}
			
				//preparing for the next iteration
				k=k+1;
				trussGraph=tempGraph;
		
			if(trussGraph.Edges.size()<=3)
				break;
			
		}
		// System.out.println("Body size: "+NodesTruss.size());

		HashMap<String,Integer> nodes_CR=CRE_Truss(NodesTruss,dg);
    	//if(test==1) break;
    	int bl=Content.split(" ").length;
    	
    	ArrayList termBoosts_Sorted= (ArrayList) nodes_CR.entrySet().stream().sorted(HashMap.Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toList());
        //  System.out.println(termBoosts_Sorted);
  		int kcount=0;
  		String searchQuery="";
          for (int i=0;i<termBoosts_Sorted.size();i++) {
  					
        	  searchQuery=searchQuery+termBoosts_Sorted.get(i).toString().replace("=","^")+" ";
  			kcount=kcount+1;
  			if(kcount==numkeywords)
  				break;
  		}
  		//System.out.println(sq);
  		
     	
 	   return searchQuery.trim();
	}
	
	public  static boolean  edgePartofTruss(DocGraph dg, DocGraph.Edge testEdge,int ktruss)
	{
		List<String> RNeighbors =new ArrayList<String>();
		List<String> LNeighbors =new ArrayList<String>();

		Iterator it =dg.Edges.keySet().iterator();
		while (it.hasNext()) {
			DocGraph.Edge nextEdge =(DocGraph.Edge)it.next();
			if(!nextEdge.equals(testEdge))
			{
				// check if this edge contains a  neighbor to the right node of the test edge
				if(nextEdge.LNode.equals(testEdge.RNode))
					RNeighbors.add(nextEdge.RNode);
				if(nextEdge.RNode.equals(testEdge.RNode))
					RNeighbors.add(nextEdge.LNode);
				// check if this edge contains a  neighbor to the left node of the test edge
				if(nextEdge.LNode.equals(testEdge.LNode))
					LNeighbors.add(nextEdge.RNode);
				if(nextEdge.RNode.equals(testEdge.LNode))
					LNeighbors.add(nextEdge.LNode);
				
			}
		}
		//intersecting both neighbors lists to find commons
		int triangles =RNeighbors.stream().distinct().filter(LNeighbors::contains).collect(Collectors.toList()).size();
		
		// intersect neighbors lists
		if(triangles>=ktruss-2)
		return true;
		else
		return false;
	}
	public static HashMap<String,Integer> CRE_Truss(HashMap<String,Integer> NodesTruss,DocGraph qDocGraph)
	{

		HashMap<String,Integer> nodesCRE_Core=new HashMap<String,Integer>();
		Iterator it =NodesTruss.keySet().iterator();
		while (it.hasNext()) {
			String node=(String)it.next();
			ArrayList<String> neighbors=qDocGraph.neighborsOf(node);
			// looping the neighbors to get the total weight of this node
			Iterator itn =neighbors.iterator();
			int CRValue=0;
			while (itn.hasNext()) {
				CRValue+=NodesTruss.get(itn.next());
			}
			nodesCRE_Core.put(node,CRValue);	
		}

		return nodesCRE_Core;
	}
}
