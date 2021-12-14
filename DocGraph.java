package codeForSharing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** This class implements methods needed for graph manipulation 
 * i.e Adding and Removing Nodes and Edges */
public class DocGraph {
	public class Edge
	{
	    public final String LNode;
	    public final String RNode;

	    public Edge(String l, String r)
	    {
	    	//adjusting for undirected edges .. ex:(A,B) = (B,A)
	    	if(l.compareTo(r)<0)
	        {
	    		LNode   = l;
	    		RNode = r;
	        }
	    	else
	    	{
	    		LNode   = r;
	    		RNode = l;
	    	}
	        
	    }

	    public String key()   { return LNode; }
	    public String value() { return RNode; }
	    
	    @Override    
	    public boolean equals(Object o) { 
	        if (this == o) return true;        
	        if (o == null || getClass() != o.getClass()) 
	        	return false;        
	        Edge e = (Edge) o;  
	        if ((LNode .equals(e.LNode)) && (RNode.equals(e.RNode))) return true;
	        return false;
	        }
	    @Override
	    public int hashCode() {
	    	return Objects.hash(LNode,RNode);
	    }
	    @Override
	    public String toString() {
	    	return LNode+"---"+RNode;
	    }
	}

	public HashMap<String,Integer> Nodes;
	public HashMap<Edge,Integer> Edges;
	
	public DocGraph()
	{
		Nodes=new HashMap<String,Integer>();
		Edges=new HashMap<Edge,Integer>();
	}
	public void addNode(String term)
	{
		if(!Nodes.containsKey(term))
			Nodes.put(term,1);
		else
			Nodes.replace(term,Nodes.get(term)+1);
	}
	public void increaseTermOccurrence(String term)
	{
		Nodes.put(term,Nodes.get(term)+1);
	}
	public int getEdgeWeight(String term1,String term2)
	{
		Edge e=new Edge(term1,term2);
		if(Edges.containsKey(e))
			{
			return Edges.get(e);
			}
		else
			return -1;
	
	}
	public void addEdge(String term1,String term2)
	{
		Edge e=new Edge(term1,term2);
		if(!Edges.containsKey(e))
			{
			Edges.put(e,1);
			}
		else
			Edges.replace(e, Edges.get(e)+1);
	}
	public void increaseEdgeWeight(String term1,String term2)
	{
		Edge temp=new Edge(term1,term2);
		Edges.replace(temp,Edges.get(temp)+1);
	}
	public void updateEdgeWeight(String term1,String term2,int weight)
	{
		Edge temp=new Edge(term1,term2);
		Edges.replace(temp,Edges.get(temp)*weight);
	}
	public Integer getNodeFrequency(String nodeS)
	{
		return Nodes.get(nodeS);
	}
	
	public void printGraph()
	{
	    System.out.println(Nodes.toString() );
	   System.out.println(Edges.toString());
	        
	}
	
	public static void main(String[] args) {
		DocGraph dg=new DocGraph();
		//one pass to add nodes in the sentences "A B C A D A B"
		dg.addNode("A");
		dg.addNode("B");
		dg.addNode("C");
		dg.addNode("D");
		dg.addNode("E");
		dg.addNode("F");
		// then add the edges W=1
		dg.addEdge("A", "B");
		dg.addEdge("A", "B");
		dg.addEdge("B", "C");
		dg.addEdge("B", "C");
		dg.addEdge("B", "C");
		dg.addEdge("C", "A");
		dg.addEdge("C", "A");
		dg.addEdge("A", "D");
		dg.addEdge("D", "A");
		dg.addEdge("A", "E");
		dg.addEdge("F", "B");
		dg.addEdge("C", "E");
		dg.addEdge("D", "E");
		dg.addEdge("E", "F");

		dg.printGraph();
		System.out.println("-----------------");
		dg.mergeNodes("A","B");
		dg.printGraph();
	}
	
	public HashMap<String,Integer> getNeighbors(String n)
	{
		HashMap<String,Integer> results=new HashMap<String,Integer>();
		Iterator ite = Edges.entrySet().iterator();
	    while (ite.hasNext()) {
	    	Map.Entry mp =(Map.Entry)ite.next();
	    	Edge keye = (Edge)mp.getKey();
	    	if(keye.LNode.equals(n))
	    		results.put(keye.RNode, (Integer)mp.getValue());
	    	if(keye.RNode.equals(n))
	    		results.put(keye.RNode, (Integer)mp.getValue());
	    }
	    return results;
	}
	public int getDegree(String n)
	{
		int degree=0;
		Iterator ite = Edges.entrySet().iterator();
	    while (ite.hasNext()) {
	    	Map.Entry mp =(Map.Entry)ite.next();
	    	Edge keye = (Edge)mp.getKey();
	    	if(keye.LNode.equals(n)||keye.RNode.equals(n))
	    		degree+=(Integer)mp.getValue();
	    }
	    return degree;
	}
	public int getMaxCore()
	{
		int max_Core=0;
		Iterator it = Nodes.keySet().iterator();
		while (it.hasNext()) {
	        String key = (String)it.next();
	        int d=getDegree(key);
	        if(d>max_Core)
	        	max_Core=d;
		}
		return max_Core;
	}
	public int getMinDegree()
	{
		int min_Degree=100000; // setting it a very high number 
		Iterator it = Nodes.keySet().iterator();
		while (it.hasNext()) {
	        String key = (String)it.next();
	        int d=getDegree(key);
	        if(d<min_Degree)
	        	min_Degree=d;
		}
		return min_Degree;
	}
	
	public DocGraph Copy()
	{
		DocGraph dg=new DocGraph();
		//copying nodes
		Iterator it = this.Nodes.entrySet().iterator();
		while (it.hasNext()) 
		{
			Map.Entry mp =(Map.Entry)it.next();
			dg.Nodes.put((String)mp.getKey(),(Integer)mp.getValue());
		}
		Iterator ite = Edges.entrySet().iterator();
		//copying edges
	    while (ite.hasNext()) {
	    	Map.Entry mp =(Map.Entry)ite.next();
	    	Edge keye = (Edge)mp.getKey();
	    	dg.Edges.put(new Edge(keye.LNode, keye.RNode),(Integer)mp.getValue());
	    }
		return dg;
	}
	public void removeEdge(String L,String R)
	{
		Edges.remove(new Edge(L,R));
	}
	
	//This following method removes all edges from the graph that has node in one of its sides
	public void removeAllEdges(String node)
	{
		HashMap<Edge,Integer> nEdges=new HashMap<Edge,Integer>();
		Iterator ite = Edges.entrySet().iterator();
		//copying edges
	    while (ite.hasNext()) {
	    	Map.Entry<Edge,Integer> mp =(Map.Entry<Edge,Integer>)ite.next();
	    	//Edge keye = (Edge)mp.getKey();
	    	if(!(mp.getKey().LNode.equals(node)||mp.getKey().RNode.equals(node)))
	    		nEdges.put(mp.getKey(),mp.getValue());
	    }
	    Edges=nEdges;
	}
	
	public void clearNotSupportedNodes()
	{
		HashMap<String,Integer> cNodes=(HashMap<String,Integer>)Nodes.clone();
		// loop over the nodes --> if not supported by an edge (as a result of truss decomposing) , remove it
		
		// first get all the nodes in the remaining edges
		ArrayList<String> nodesOnEdges=new ArrayList<String>();
		Iterator it =Edges.keySet().iterator();
		while (it.hasNext()) {
			Edge nextEdge =(Edge)it.next();
			nodesOnEdges.add(nextEdge.LNode);
			nodesOnEdges.add(nextEdge.RNode);
		}
		List<String> sNodes=nodesOnEdges.stream().distinct().collect(Collectors.toList());
		it =cNodes.keySet().iterator();
		while (it.hasNext()) {
			String nextNode =(String)it.next();
			if(!sNodes.contains(nextNode))
				Nodes.remove(nextNode);
		
		}
	}
	public ArrayList<String> neighborsOf(String node)
	{
		ArrayList<String> str=new ArrayList<String>();
		Iterator it =Edges.keySet().iterator();
		int index=0;
		while (it.hasNext()) {
			Edge e=(Edge)it.next();
			if(e.LNode.equals(node)) str.add(e.RNode);
			if(e.RNode.equals(node)) str.add(e.LNode);
		}
		return str;
	}
	public void mergeNodes(String node1,String node2)
	{
		String MNodeLabel="";
		if(node1.compareTo(node2)<0)
        {
			MNodeLabel=node1+" "+node2;
        }
    	else
    	{
    		MNodeLabel=node2+" "+node1;
    	}
		Edge temp=new Edge(node1,node2);
		HashMap<Edge,Integer> MergedEdges=new HashMap<Edge,Integer>();
		
		Iterator ite = Edges.entrySet().iterator();
		//copying edges
		HashMap<Edge,Integer> cEdges=new HashMap<Edge,Integer>(Edges);
		
	    while (ite.hasNext()) {
	    	
	    	Map.Entry<Edge,Integer> mp =(Map.Entry<Edge,Integer>)ite.next();
	    	Edge entryedge=mp.getKey();
	    	//Edge keye = (Edge)mp.getKey();
	    	if(!entryedge.equals(temp))// this way i will remove the edge between the two nodes
	    	{
	    		if(entryedge.LNode.equals(node1))
	    		{
	    			addMergedEdge(MergedEdges,new Edge(MNodeLabel,entryedge.RNode),mp.getValue());
	    			
	    		}
	    		else
	    			if(entryedge.RNode.equals(node1))
	    			{
	    					addMergedEdge(MergedEdges,new Edge(MNodeLabel,entryedge.LNode),mp.getValue());
	    			}
	    			else
	    				if(entryedge.LNode.equals(node2))
	    					addMergedEdge(MergedEdges,new Edge(MNodeLabel,entryedge.RNode),mp.getValue());
	    	    		else
	    	    			if(entryedge.RNode.equals(node2))
	    	    				addMergedEdge(MergedEdges,new Edge(MNodeLabel,entryedge.LNode),mp.getValue());	
	    	}
	    	else
	    		cEdges.remove(entryedge);
	    }
	  //now i need to remove previous occurrence of the merged edge of node 1 (the substring of the NE)
		
	    ite = Edges.entrySet().iterator();
	    while (ite.hasNext()) {
	    	Map.Entry<Edge,Integer> mp =(Map.Entry<Edge,Integer>)ite.next();
	    	Edge entryedge=mp.getKey();
	    	if(node1.contains(" "))
	    		if(entryedge.LNode.equals(node1)||entryedge.RNode.equals(node1))
	    			cEdges.remove(entryedge);
	    }
		Edges=cEdges;
		Edges.putAll(MergedEdges);
		    
		//System.out.println("remaining edges:"+Edges.size());
		
	}
	public void addMergedEdge(HashMap<Edge,Integer> emap,Edge e,int v)
	{
		if(emap.containsKey(e))
			emap.replace(e,emap.get(e)+v);
		else
			emap.put(e,v);
		
	}

}

