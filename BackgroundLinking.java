package codeForSharing;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Entities.EscapeMode;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Whitelist;
import org.tartarus.snowball.ext.EnglishStemmer;

import backgroundLinking.entrance;
import backgroundLinking.luceneIndex;



public class BackgroundLinking {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	/**
	 * The following method creates a sql database that stores each article as a set of paragraph records after removing the html tags
	 */
	public static void createWBDatabase()
	{
		int fileno=14;
		try{
			Class.forName("com.mysql.jdbc.Driver");  
			
			
			
	    int count=0;
	    int indexed=0;
	    while(fileno<15)
	    {
	    	
	    	FileReader fileReader = new FileReader("V3CollectionFiles/TREC_WashingtonFile_"+fileno);
			

			BufferedReader bufferedReader = new BufferedReader(fileReader);
	        
		    String line=bufferedReader.readLine();
		    Connection con=DriverManager.getConnection(  
					"jdbc:mysql://localhost:3306/WpostDB","root","123456789");  
	    
		while(line!=null)
	    {
			
	    	String docid="";
	    	String pStmt="";
	    	try{
	    	count=count+1;
	    	if(count%1000==0) System.out.println(count);
	    	
	    	/***
	    	 * First Step Extract the kicker type
	    	 */
	    	String type="Normal";
	    	/**
	    	 * Extract the other data
	    	 */
	    	
	    	JSONObject	obj = (JSONObject) new JSONParser().parse(line);
	    	
	    	docid=(String)obj.get("id");
	    	
        	String title="T";
			try{ title=(String)obj.get("title");} catch(Exception te)  { }//te.printStackTrace();}
			if(title==null) { title="T";}
			String url="U";
			try{  url=(String)obj.get("article_url");} catch(Exception te)  { }//te.printStackTrace();}
			if(url==null) url="U";
			String author="A";
			try{  author=(String)obj.get("author");} catch(Exception te)  { }//te.printStackTrace();}
			if(author==null) author="A";
			long date=0;
			try{date=(Long)obj.get("published_date");} catch(Exception te) { }//te.printStackTrace();}
			
	        JSONArray contents=(JSONArray) obj.get("contents");
          
            /**
             * Round 1 ... get the title and date correctly
             */
            for(int i=0;i<contents.size();i++)
        	{
            	try {
            		JSONObject jobj=(JSONObject)contents.get(i);
            		String ctype="";
            		try{	ctype=(String)jobj.get("type");}catch(Exception te)  {}
    		
            				if(ctype.equals("title"))
            					if(title==null)
            					{
            						try{title=(String)jobj.get("content");
            						System.out.println(title);} catch(Exception te)  {}
            					}
            		else
            		if(date==0)
            			if(ctype.equals("date")) try{ date=(Long)obj.get("content");} catch(Exception te)  {}
            		else
                		if(ctype.equals("kicker"))
                		{
                			try{
                				type=(String)jobj.get("content");
                			}
                			catch(Exception te)  {}
                		}
    				}
            	
            	
            	/*
            	 * else if(ctype.equals("sanitized_html"))
            			contentStr=contentStr+" "+cStr;
            	 */
            	catch(Exception e)
            	{
            		System.out.print("error within document loop");
            		e.printStackTrace();
            		//not a type we are interested in
            	}
        	}
            /**
             * Saving the record of the document
             */
            pStmt="insert into Documents values ('"
            		+docid+
            		"','"
            		+ title.replace("'","\\'")
            		+ "','"+
            		type.replace("'","\\'")
            		+"','"+
            		url.replace("'","\\'")
            		+"','"+
            		author.replace("'","\\'")
            		+"',"+
            		Long.toString(date)+")";
            PreparedStatement preparedStmt = con.prepareStatement(pStmt);

		      preparedStmt.execute();
		      
		    /**
		     * Round 2: Saving the contents of paragraphs
		     */
		      int paragraphPos=1;
		      for(int i=0;i<contents.size();i++)
	        	{
	            	try {
	            		JSONObject jobj=(JSONObject)contents.get(i);
	            		String ctype=(String)jobj.get("type");
	            		if(ctype.equals("sanitized_html"))
	            		{
	            			String subtype=ctype=(String)jobj.get("subtype");
	            			
	            			if(subtype!=null&& subtype.equals("paragraph"))	            				
	            			{
	            				String content=(String)jobj.get("content");
	            				OutputSettings settings = new OutputSettings();
	            				settings.escapeMode(EscapeMode.base);
	            				String cleanHtml = Jsoup.clean(content, " ", Whitelist.none(), settings);
	            				cleanHtml=Parser.unescapeEntities(cleanHtml, false).replace("\\","\\\\").replace("'","\\'").replace("_","\\_").replace("%","\\%").replace("\"","\\'");
	            				if(!cleanHtml.replace(" ","").contentEquals(""))
	            				{
	            					String pid=""+docid+"_"+paragraphPos;
	            				
	            					preparedStmt = con.prepareStatement("insert into Contents values ('"
	            							+pid+
	            							"','"
	            							+docid+
	            		            		"','"
	            		            		+ cleanHtml
	            		            		+"',"+
	            		            		Integer.toString(paragraphPos)+")");
	            					paragraphPos=paragraphPos+1;
	            					preparedStmt.execute();
	            				} 
	            			}
	            		}
	            	}
	            	catch(NullPointerException e)
	            	{
	            	
	            	}
	            	catch(Exception e)
	            	{
	            		System.out.println("error in processing paragraphs of document:"+docid);
	            		System.out.println(preparedStmt);
	            		//e.printStackTrace
	            	}
	        	}  
	    	}
			catch(Exception e)
			{
				System.out.println("error adding document:"+docid);
				//System.out.println("-------:"+pStmt);
				e.printStackTrace();
			} 
	    	
	    	line=bufferedReader.readLine(); 
	    }
	    bufferedReader.close();
	    con.close();
	    fileno=fileno+1;
	    }
		}
		catch(Exception e)
		{
    		System.out.print("error ");
			e.printStackTrace();
		}
		
	}

	/**
	 * A method that takes query1 and query2 and outputs the query 1 reduced after removing uncommon terms between it and query 2.
	 * @param query1
	 * @param query2
	 * @return
	 */
	public static String furtherReduction(String query1,String query2)
	{
		HashMap<String,Double> q1Terms=new HashMap<String,Double>();
    	HashMap<String,Double> q2Terms=new HashMap<String,Double>();
    	
    	/**
    	 * Getting the first vector
    	 */
    	String[] qData=query1.split(" ");
    	for(int i=0;i<qData.length;i++)
    	{
    		String[] td=qData[i].split("\\^");
    		//System.out.println(t);
    		q1Terms.put(td[0],Double.parseDouble(td[1]));
    	}
    	

    	/**Getting the second vector
    	 */
    	
    	qData=query2.split(" ");
    	for(int i=0;i<qData.length;i++)
    	{
    		String[] td=qData[i].split("\\^");
    		//if(td.length>2)
    		q2Terms.put(td[0],Double.parseDouble(td[1]));
    		
    	}
    
    	/**
    	 * Getting common terms and Computing dot Product
    	 */
    	
    	String adjustedQuery="";
    	for(String term1:q1Terms.keySet())
    		if(q2Terms.containsKey(term1))
    			adjustedQuery=adjustedQuery+term1+"^"+q1Terms.get(term1)+" ";
    	return adjustedQuery.strip();
    	
	}

	/**
	 * A method that applies background linking using the common terms extracted between Yake and TF-IDF
	 * @param lindex
	 * @param QueriesFile
	 * @param QueriesDir
	 * @param YakeQueriesDir -- The place where the yake queries (output from the python file) are stored
	 * @param outputDir
	 * @param outputfile
	 * @param loggerfile
	 * @param keywords
	 * @param winSize
	 */
	public static void processQueriesYake_ReducedTFIDF(String lindex,String QueriesFile,String QueriesDir,String YakeQueriesDir,String outputDir,String outputfile,String loggerfile,int keywords,int winSize)
	{
		int Sumkeywords=0;
		
		try
		{
			
			int errorInQueries=0;
			luceneIndex index=new luceneIndex(lindex);
			PrintWriter writer = new PrintWriter(outputDir+"/"+outputfile, "UTF-8");
			PrintWriter writerLogger = new PrintWriter(outputDir+"/"+loggerfile, "UTF-8");
			PrintWriter writerQuery = new PrintWriter(outputDir+"/query_"+outputfile, "UTF-8");

			/** loading stop words list 
			 */
			ArrayList<String> stopwords=new ArrayList<String>();
			 FileReader fileReader = new FileReader("StopWordsSEO.txt");

		     BufferedReader bufferedReader = new BufferedReader(fileReader);
		        
		        String lineInput=bufferedReader.readLine();
		        while(lineInput!=null)
		        {
		        	stopwords.add(lineInput.trim());
		        	lineInput=bufferedReader.readLine();
		        }
		        bufferedReader.close();
		        
			/**
	    	 * loading ids of documents
	    	 */
			HashMap <String,Long> idDates =new HashMap <String,Long>();
			try
	    	{
			 fileReader = new FileReader("DocIdsDates.txt");

			 bufferedReader = new BufferedReader(fileReader);
	        
		    String line=bufferedReader.readLine();
		   
		  
		    while(line!=null)
		    {
		    	String[] str=line.split(",");
		    	idDates.put(str[0],Long.parseLong(str[1]));
		    	line=bufferedReader.readLine();
		    }

	    	}
	    	catch(Exception te) {te.printStackTrace();}
			
			
			 fileReader = new FileReader(QueriesFile);
			 bufferedReader = new BufferedReader(fileReader);
	        
		    String line=bufferedReader.readLine();
		   
		    Long TotalPT=0l;
		    while(line!=null)
		    {
		    	String[] str=line.split("#");
		    	String topicNo=str[0];
		    	//System.out.println("Processing topic:"+topicNo);
		    	Long qDate=Long.parseLong(str[1]);
		    	String qTitle=str[2];
		    	String searchQuery=getQueryBody(QueriesDir+"/"+topicNo+".txt");
		    	searchQuery=preProcessStringStopWordsRemoved(searchQuery, true, true, stopwords, false,2);
		    	String TFIDFQuery=getTermsTFIDF(searchQuery,true,keywords,lindex);
				searchQuery=getYakeQuery(YakeQueriesDir+topicNo+".txt",keywords);
		    	
		    	
		    	searchQuery=furtherReduction(searchQuery,TFIDFQuery);
		   
		    	Sumkeywords=Sumkeywords+searchQuery.split(" ").length;
		    	
		    	qTitle=preProcessStringStopWordsRemoved(qTitle, true, false, stopwords, false,0);
		    	
		    	
		    	//
		    	ArrayList<String> DocSignatures=new ArrayList<String>();
		    	DocSignatures.add(qTitle);
		    	writerQuery.println(searchQuery);
		    	Long starttime=System.currentTimeMillis();

		    	ScoreDoc[] hits=index.searchBody(searchQuery.trim());
		    	Long endtime=System.currentTimeMillis();
		    	Long Result=(endtime-starttime);
		    	TotalPT=TotalPT+Result;
		    	writerLogger.println(topicNo+" "+(keywords-searchQuery.split(" ").length));
		    	
		    	IndexReader reader = DirectoryReader.open(index.iIndex);
		    	IndexSearcher searcher = new IndexSearcher(reader);
		    	

		    	int count=0;
		    	int countA=0;
		    	// Now checking every hit to see if it can be added to our result set:
		    	for(int i=0;i<hits.length;i++) {
		
		    	    int docId = hits[i].doc;
		    	    Document d = searcher.doc(docId);
		    	    String docid=d.get("docID");
		    	    Long docDate=(long)0;
		    	    try{docDate=idDates.get(docid);} catch(Exception e) {e.printStackTrace();}
		    	   // System.out.println(docDate);
		    	    String field=d.get("field");
		    	    String type=d.get("type");
		    	    long datedifference=0;
		    	    if(docDate!=null)
		    	  	   	datedifference=qDate-docDate;
		    		 
		    	    if(field.equals("Opinion")||field.equals("Editor")||field.equals("PostView")) {countA=countA+1;continue;}
		    	    if(type.equals("Opinion")||type.equals("Editor")||type.equals("PostView")) {countA=countA+1;continue;}
		    		String signature=preProcessStringStopWordsRemoved(d.get("title"), true, false, stopwords, false,0);
		    				
		    	    if(!DocSignatures.contains(signature))  //only if this document was not added before
		    	    if(datedifference>0&&(docDate!=null)&&(docDate!=0)) // only if the retrieved article is published before the current article, it can be added to the result
		    	       	{
		    	    		count=count+1;
		    	    	
		    	    	writer.println(topicNo+" Q0 "+docid+" 0 "+hits[i].score+" QU_KTR");
		    	    	if(count==100)
		    	    		break;
		    	       	}
		    	    else
		    	    	countA=countA+1;
		    	    
		    	    
		    	    
		    	}
		    	
		    	if(count<100)
		    	{
		    		//System.out.println("error in topic"+topicNo);
		    		errorInQueries=errorInQueries+1;
		    	}
		    		
		    	line=bufferedReader.readLine();
		    }
		   
		   // System.out.println("error in query "+errorInQueries);	
		    writer.close();
		    writerLogger.close();
		    writerQuery.close();
		 System.out.println("Total Processing Time:"+TotalPT);  
		}
		catch(Exception te) {te.printStackTrace();}
		 System.out.println(Sumkeywords);
	    	
	}
	/**
	 * 	A method that applies preprocessing to the text (i.e stop words removal, lower casing,...)
	 * @param input
	 * @param lowercase
	 * @param stopWordsRemoval
	 * @param stopWords
	 * @param stem
	 * @param minTokenLength
	 * @return
	 */
	public static String preProcessStringStopWordsRemoved(String input,boolean lowercase,boolean stopWordsRemoval,ArrayList<String> stopWords, boolean stem,int minTokenLength)
	{
	// First Step is to filter the text to remove any remaining HTML content
	OutputSettings settings = new OutputSettings();
	settings.escapeMode(EscapeMode.base);
	String cleanHtml = Jsoup.clean(input, " ", Whitelist.none(), settings);
	cleanHtml=Parser.unescapeEntities(cleanHtml, false); // rempoving the &nbsp; resulted from parsing the html  

	if(lowercase) cleanHtml=cleanHtml.toLowerCase();
	String finaltxt="";
	if(stopWordsRemoval)
	{
		String[] substrings=cleanHtml.split(" ");

	for(int i=0;i<substrings.length;i++)
		if(!stopWords.contains(substrings[i].toLowerCase()))
				finaltxt=finaltxt+substrings[i]+" ";
	}
	else
		finaltxt=cleanHtml;
	// Now remove all non alphapetical characters and all extra spaces.
	finaltxt=finaltxt.trim().replaceAll("[^A-Za-z ]"," ").replaceAll("( )+"," ");
	//Make sure that no stop words are there after special character removal
	String finaltxt1="";
	if(stopWordsRemoval)
	{
		String[] substrings=finaltxt.split(" ");

	for(int i=0;i<substrings.length;i++)
		if(!stopWords.contains(substrings[i].toLowerCase()))
				finaltxt1=finaltxt1+substrings[i]+" ";
	}
	else
		finaltxt1=finaltxt;

	//Now removing all token less than min in length
	cleanHtml="";
	if(minTokenLength>0)
	{
		String[] substrings=finaltxt1.split(" ");
		for(int i=0;i<substrings.length;i++)
	    	if(substrings[i].length()>=minTokenLength)
	    		cleanHtml=cleanHtml+substrings[i]+" ";
		finaltxt1=cleanHtml;
	}

	// we need to apply stemming here if requested

	String output="";
	if(stem) {
		
		
		EnglishStemmer english = new EnglishStemmer();
	    String[] words = finaltxt1.split(" ");
	    for(int i = 0; i < words.length; i++){
	            english.setCurrent(words[i]);
	            english.stem();
	            output=output+english.getCurrent()+" ";
	    }
	}
	else
		output=finaltxt1;
	return output.strip();	

	}
	/**
	 * A method that applies background linking using the terms extracted from TF or TF-IDF
	 * @param lindex
	 * @param queryInfo
	 * @param queryDir
	 * @param Outdirectory
	 * @param outputfile
	 * @param loggerfile
	 * @param TFWords
	 * @param idf -- wether or not to include idf
	 */
	public static void processQueriesTFIDF(String lindex,String queryInfo,String queryDir,String Outdirectory,String outputfile,String loggerfile,int TFWords,boolean idf)
	{
		try
		{
			int errorInQueries=0;
			luceneIndex index=new luceneIndex(lindex);
			PrintWriter writer = new PrintWriter(Outdirectory+"/"+outputfile, "UTF-8");
			PrintWriter writerQuery = new PrintWriter(Outdirectory+"/query_"+outputfile, "UTF-8");
			
			PrintWriter writerLogger = new PrintWriter(Outdirectory+"/"+loggerfile, "UTF-8");
			
			/** loading stop words list 
			 */
			ArrayList<String> stopwords=new ArrayList<String>();
			 FileReader fileReader = new FileReader("StopWordsSEO.txt");

		     BufferedReader bufferedReader = new BufferedReader(fileReader);
		        
		        String lineInput=bufferedReader.readLine();
		        while(lineInput!=null)
		        {
		        	stopwords.add(lineInput.trim());
		        	lineInput=bufferedReader.readLine();
		        }
		        bufferedReader.close();
		        
			/**
	    	 * loading ids of documents to correct missing dates in collection file
	    	 */
			HashMap <String,Long> idDates =new HashMap <String,Long>();
			try
	    	{
			 fileReader = new FileReader("DocIdsDates.txt");

			 bufferedReader = new BufferedReader(fileReader);
	        
		    String line=bufferedReader.readLine();
		   
		  
		    while(line!=null)
		    {
		    	String[] str=line.split(",");
		    	idDates.put(str[0],Long.parseLong(str[1]));
		    	line=bufferedReader.readLine();
		    }

	    	}
	    	catch(Exception te) {te.printStackTrace();}
			
			
			 fileReader = new FileReader(queryInfo);
			 bufferedReader = new BufferedReader(fileReader);
	        
		    String line=bufferedReader.readLine();
		   
		    Long TotalTime=0l;
		    int qcount=0;
		    long totalextractiontime=0l;
		    while(line!=null)
		    {
		    	String[] str=line.split("#");
		    	String topicNo=str[0];
		    	Long qDate=Long.parseLong(str[1]);
		    	String qTitle=str[2];
		    	String searchQuery=getQueryBody(queryDir+"/"+topicNo+".txt");
		    
		    	searchQuery=preProcessStringStopWordsRemoved(searchQuery, true, true, stopwords, false,2);
		    	long starttimeE=System.currentTimeMillis();
		    	searchQuery=getTermsTFIDF(searchQuery,idf,TFWords,lindex);
		    	totalextractiontime=totalextractiontime+(System.currentTimeMillis()-starttimeE);
		    	
		    		qTitle=preProcessStringStopWordsRemoved(qTitle, true, false, stopwords, false,0);
		    	
		    	
		    	//
		    	ArrayList<String> DocSignatures=new ArrayList<String>();
		    	DocSignatures.add(qTitle);

		    	writerQuery.println(searchQuery);
		    	qcount=qcount+1;
		    	long starttime=System.currentTimeMillis();

		    	ScoreDoc[] hits=index.searchBody(searchQuery.trim());
		    	
		    	Long endtime=System.currentTimeMillis();
		    	Long Result=(endtime-starttime);
		    	TotalTime=TotalTime+Result;
		    	writerLogger.println(topicNo+" "+Result);
		    	IndexReader reader = DirectoryReader.open(index.iIndex);
		    	IndexSearcher searcher = new IndexSearcher(reader);
		    	

		    	int count=0;
		    	// Now checking every hit to see if it can be added to our result set:
		    	for(int i=0;i<hits.length;i++) {
		    	    int docId = hits[i].doc;
		    	    Document d = searcher.doc(docId);
		    	    String docid=d.get("docID");
		    	    Long docDate=(long)0;
		    	    try{docDate=idDates.get(docid);} catch(Exception e) {e.printStackTrace();}
		    	   // System.out.println(docDate);
		    	    String field=d.get("field");
		    	    String type=d.get("type");
		    	    long datedifference=0;
		    	    if(docDate!=null)
		    	  	   	datedifference=qDate-docDate;
		    		 
		    	    if(field.equals("Opinion")||field.equals("Editor")||field.equals("PostView")) continue;
		    	    if(type.equals("Opinion")||type.equals("Editor")||type.equals("PostView")) continue;
		    		String signature=preProcessStringStopWordsRemoved(d.get("title"), true, false, stopwords, false,0);
		    				
		    	    if(!DocSignatures.contains(signature))  //only if this document was not added before
		    	    if(datedifference>0&&(docDate!=null)&&(docDate!=0)) // only if the retrieved article is published before the current article, it can be added to the result
		    	       	{
		    	    	//System.out.println("Document before: "+docid);
		    	    	count=count+1;
		    	    	
		    	    	writer.println(topicNo+" Q0 "+docid+" 0 "+hits[i].score+" QU_KTR");
		    	    	if(count==100)
		    	    		break;
		    	       	}
		    	    
		    	}
		    	if(count<100)
		    	{
		    		//System.out.println("error in Topic"+topicNo);
		    		errorInQueries=errorInQueries+1;
		    	}
		    		
		    	line=bufferedReader.readLine();
		    }
		   // System.out.print(TotalTime+" ");	
		   System.out.println("TF Extraction Time "+totalextractiontime+" "+idf);	

		    
		  //  System.out.println(TotalTime);	
		  // System.out.println("error in query "+errorInQueries);	
		    writer.close();
		    writerLogger.close();
		    writerQuery.close();
		}
		catch(Exception te) {te.printStackTrace();}
	}
	/**
	 * A method that applies background linking using the terms extracted using the k-Truss method
	 * @param lindex
	 * @param QueriesFile
	 * @param QueriesDir
	 * @param outputDir
	 * @param outputfile
	 * @param loggerfile
	 * @param keywords
	 * @param winSize
	 */
	public static void processQueriesKTruss(String lindex,String QueriesFile,String QueriesDir,String outputDir,String outputfile,String loggerfile,int keywords,int winSize)
	{
		try
		{
			
			int errorInQueries=0;
			luceneIndex index=new luceneIndex(lindex);
			PrintWriter writer = new PrintWriter(outputDir+"/"+outputfile, "UTF-8");
			PrintWriter writerLogger = new PrintWriter(outputDir+"/"+loggerfile, "UTF-8");
			PrintWriter writerQuery = new PrintWriter(outputDir+"/query_"+outputfile, "UTF-8");

			/** loading stop words list 
			 */
			ArrayList<String> stopwords=new ArrayList<String>();
			 FileReader fileReader = new FileReader("StopWordsSEO.txt");

		     BufferedReader bufferedReader = new BufferedReader(fileReader);
		        
		        String lineInput=bufferedReader.readLine();
		        while(lineInput!=null)
		        {
		        	stopwords.add(lineInput.trim());
		        	lineInput=bufferedReader.readLine();
		        }
		        bufferedReader.close();
		        
			/**
	    	 * loading ids of documents
	    	 */
			HashMap <String,Long> idDates =new HashMap <String,Long>();
			try
	    	{
			 fileReader = new FileReader("DocIdsDates.txt");

			 bufferedReader = new BufferedReader(fileReader);
	        
		    String line=bufferedReader.readLine();
		   
		  
		    while(line!=null)
		    {
		    	String[] str=line.split(",");
		    	idDates.put(str[0],Long.parseLong(str[1]));
		    	line=bufferedReader.readLine();
		    }

	    	}
	    	catch(Exception te) {te.printStackTrace();}
			
			
			 fileReader = new FileReader(QueriesFile);
			 bufferedReader = new BufferedReader(fileReader);
	        
		    String line=bufferedReader.readLine();
		   
		    Long Totaltime=0l;
		    while(line!=null)
		    {
		    	String[] str=line.split("#");
		    	String topicNo=str[0];
		    	//if(!topicNo.equals("887")) {line=bufferedReader.readLine(); continue;}
		    	//System.out.println("Processing topic:"+topicNo);
		    	Long qDate=Long.parseLong(str[1]);
		    	String qTitle=str[2];
		    	String searchQuery=getQueryBody(QueriesDir+"/"+topicNo+".txt");
		    	searchQuery=preProcessStringStopWordsRemoved(searchQuery, true, true, stopwords, false,2);
		    	Long starttime=System.currentTimeMillis();

		    	searchQuery=graphBasedAnalysis.basicKTrussExactlyK(searchQuery,"",winSize, keywords, 0);
		    	Long endtime=System.currentTimeMillis();
		    	Totaltime=Totaltime+(endtime-starttime);
		    	//System.out.println(searchQuery.split(" ").length);
		    	//if(true)
		    	//	break;
		    	
		    	//System.out.println(searchQuery);
		    	qTitle=preProcessStringStopWordsRemoved(qTitle, true, false, stopwords, false,0);
		    	
		    	
		    	//
		    	ArrayList<String> DocSignatures=new ArrayList<String>();
		    	DocSignatures.add(qTitle);
		    	
		    	writerQuery.println(searchQuery);

		    	ScoreDoc[] hits=index.searchBody(searchQuery.trim());
		    	
		    	
		    	writerLogger.println(topicNo+" "+Totaltime);
		    	
		    	IndexReader reader = DirectoryReader.open(index.iIndex);
		    	IndexSearcher searcher = new IndexSearcher(reader);
		    	

		    	int count=0;
		    	// Now checking every hit to see if it can be added to our result set:
		    	for(int i=0;i<hits.length;i++) {
		    	    int docId = hits[i].doc;
		    	    Document d = searcher.doc(docId);
		    	    String docid=d.get("docID");
		    	    Long docDate=(long)0;
		    	    try{docDate=idDates.get(docid);} catch(Exception e) {e.printStackTrace();}
		    	   // System.out.println(docDate);
		    	    String field=d.get("field");
		    	    String type=d.get("type");
		    	    long datedifference=0;
		    	    if(docDate!=null)
		    	  	   	datedifference=qDate-docDate;
		    		 
		    	    if(field.equals("Opinion")||field.equals("Editor")||field.equals("PostView")) continue;
		    	    if(type.equals("Opinion")||type.equals("Editor")||type.equals("PostView")) continue;
		    		String signature=preProcessStringStopWordsRemoved(d.get("title"), true, false, stopwords, false,0);
		    				
		    	    if(!DocSignatures.contains(signature))  //only if this document was not added before
		    	    if(datedifference>0&&(docDate!=null)&&(docDate!=0)) // only if the retrieved article is published before the current article, it can be added to the result
		    	       	{
		    	    	//System.out.println("Document before: "+docid);
		    	    	count=count+1;
		    	    	
		    	    	writer.println(topicNo+" Q0 "+docid+" 0 "+hits[i].score+" QU_KTR");
		    	    	if(count==100)
		    	    		break;
		    	       	}
		    	    
		    	}
		    	if(count<100)
		    	{
		    	//	System.out.println("error in topic"+topicNo);
		    		errorInQueries=errorInQueries+1;
		    	}
		    		
		    	line=bufferedReader.readLine();
		    }
		    	
		  //  System.out.println("error in query "+errorInQueries);	
		    writer.close();
		    writerLogger.close();
		    writerQuery.close();
		   System.out.println("Total Execution time for KTruss"+Totaltime);
		}
		catch(Exception te) {te.printStackTrace();}
	}
	
/**
 * 	A method that applies background linking using the terms extracted using the k-Core method
 * @param lindex
 * @param QueriesFile
 * @param QueriesDir
 * @param outputDir
 * @param outputfile
 * @param loggerfile
 * @param keywords
 * @param winSize
 */
	public static void processQueriesKCore(String lindex,String QueriesFile,String QueriesDir,String outputDir,String outputfile,String loggerfile,int keywords,int winSize)
	{
		try
		{
			
			int errorInQueries=0;
			luceneIndex index=new luceneIndex(lindex);
			PrintWriter writer = new PrintWriter(outputDir+"/"+outputfile, "UTF-8");
			PrintWriter writerLogger = new PrintWriter(outputDir+"/"+loggerfile, "UTF-8");
			PrintWriter writerQuery = new PrintWriter(outputDir+"/query_"+outputfile, "UTF-8");

			
			/** loading stop words list 
			 */
			ArrayList<String> stopwords=new ArrayList<String>();
			 FileReader fileReader = new FileReader("StopWordsSEO.txt");

		     BufferedReader bufferedReader = new BufferedReader(fileReader);
		        
		        String lineInput=bufferedReader.readLine();
		        while(lineInput!=null)
		        {
		        	stopwords.add(lineInput.trim());
		        	lineInput=bufferedReader.readLine();
		        }
		        bufferedReader.close();
		        
			/**
	    	 * loading ids of documents
	    	 */
			HashMap <String,Long> idDates =new HashMap <String,Long>();
			try
	    	{
			 fileReader = new FileReader("DocIdsDates.txt");

			 bufferedReader = new BufferedReader(fileReader);
	        
		    String line=bufferedReader.readLine();
		   
		  
		    while(line!=null)
		    {
		    	String[] str=line.split(",");
		    	idDates.put(str[0],Long.parseLong(str[1]));
		    	line=bufferedReader.readLine();
		    }

	    	}
	    	catch(Exception te) {te.printStackTrace();}
			
			
			 fileReader = new FileReader(QueriesFile);
			 bufferedReader = new BufferedReader(fileReader);
	        
		    String line=bufferedReader.readLine();
		   
		  Long Totaltime=0l;
		    while(line!=null)
		    {
		    	String[] str=line.split("#");
		    	String topicNo=str[0];
		    	//System.out.println("Processing topic:"+topicNo);
		    	Long qDate=Long.parseLong(str[1]);
		    	String qTitle=str[2];
		    	String searchQuery=getQueryBody(QueriesDir+"/"+topicNo+".txt");
		    	searchQuery=preProcessStringStopWordsRemoved(searchQuery, true, true, stopwords, false,2);
		    	Long starttime=System.currentTimeMillis();
		    	
		    	searchQuery=graphBasedAnalysis.basicKCoreExactlyK(searchQuery,"",winSize, keywords, 0);
		    	Long endtime=System.currentTimeMillis();
		    	Long Result=(endtime-starttime);
		    	Totaltime=Totaltime+Result;
		    	qTitle=preProcessStringStopWordsRemoved(qTitle, true, false, stopwords, false,0);
		    	
		    	
		    	//
		    	ArrayList<String> DocSignatures=new ArrayList<String>();
		    	DocSignatures.add(qTitle);
		    	
		    	writerQuery.println(searchQuery);
		    	
		    	ScoreDoc[] hits=index.searchBody(searchQuery.trim());
		    	
		    	writerLogger.println(topicNo+" "+Result);
		    	
		    	IndexReader reader = DirectoryReader.open(index.iIndex);
		    	IndexSearcher searcher = new IndexSearcher(reader);
		    	

		    	int count=0;
		    	// Now checking every hit to see if it can be added to our result set:
		    	for(int i=0;i<hits.length;i++) {
		    	    int docId = hits[i].doc;
		    	    Document d = searcher.doc(docId);
		    	    String docid=d.get("docID");
		    	    Long docDate=(long)0;
		    	    try{docDate=idDates.get(docid);} catch(Exception e) {e.printStackTrace();}
		    	   // System.out.println(docDate);
		    	    String field=d.get("field");
		    	    String type=d.get("type");
		    	    long datedifference=0;
		    	    if(docDate!=null)
		    	  	   	datedifference=qDate-docDate;
		    		 
		    	    if(field.equals("Opinion")||field.equals("Editor")||field.equals("PostView")) continue;
		    	    if(type.equals("Opinion")||type.equals("Editor")||type.equals("PostView")) continue;
		    		String signature=preProcessStringStopWordsRemoved(d.get("title"), true, false, stopwords, false,0);
		    				
		    	    if(!DocSignatures.contains(signature))  //only if this document was not added before
		    	    if(datedifference>0&&(docDate!=null)&&(docDate!=0)) // only if the retrieved article is published before the current article, it can be added to the result
		    	       	{
		    	    	//System.out.println("Document before: "+docid);
		    	    	count=count+1;
		    	    	
		    	    	writer.println(topicNo+" Q0 "+docid+" 0 "+hits[i].score+" QU_KTR");
		    	    	if(count==100)
		    	    		break;
		    	       	}
		    	    
		    	}
		    	if(count<100)
		    	{
		    		errorInQueries=errorInQueries+1;
		    	}
		    		
		    	line=bufferedReader.readLine();
		    }
			   System.out.println("Total Execution time for KCore"+Totaltime);
	
		   // System.out.println("error in query "+errorInQueries);	
		    writer.close();
		    writerLogger.close();
		    writerQuery.close();
		   
		}
		catch(Exception te) {te.printStackTrace();}
	}
/**
 * A method that applies background linking using the terms extracted using the PositionRank method
 * @param QueriesFile
 * @param QueriesDir
 * @param outputDir
 * @param outputfile
 * @param loggerfile
 * @param keywords
 * @param winSize
 */
	public static void processQueriesPositionRank(String QueriesFile,String QueriesDir,String outputDir,String outputfile,String loggerfile,int keywords,int winSize)
	{
		try
		{
			
			int errorInQueries=0;
			luceneIndex index=new luceneIndex("WPostVsNoStem.index");
			PrintWriter writer = new PrintWriter(outputDir+"/"+outputfile, "UTF-8");
			PrintWriter writerlogger = new PrintWriter(outputDir+"/"+loggerfile, "UTF-8");
			PrintWriter writerQuery = new PrintWriter(outputDir+"/query_"+outputfile, "UTF-8");

			/** loading stop words list 
			 */
			ArrayList<String> stopwords=new ArrayList<String>();
			 FileReader fileReader = new FileReader("StopWordsSEO.txt");

		     BufferedReader bufferedReader = new BufferedReader(fileReader);
		        
		        String lineInput=bufferedReader.readLine();
		        while(lineInput!=null)
		        {
		        	stopwords.add(lineInput.trim());
		        	lineInput=bufferedReader.readLine();
		        }
		        bufferedReader.close();
		        
			/**
	    	 * loading ids of documents
	    	 */
			HashMap <String,Long> idDates =new HashMap <String,Long>();
			try
	    	{
			 fileReader = new FileReader("DocIdsDates.txt");

			 bufferedReader = new BufferedReader(fileReader);
	        
		    String line=bufferedReader.readLine();
		   
		  
		    while(line!=null)
		    {
		    	String[] str=line.split(",");
		    	idDates.put(str[0],Long.parseLong(str[1]));
		    	line=bufferedReader.readLine();
		    }

	    	}
	    	catch(Exception te) {te.printStackTrace();}
			
			
			 fileReader = new FileReader(QueriesFile);
			 bufferedReader = new BufferedReader(fileReader);
	        
		    String line=bufferedReader.readLine();
		   
		    Long TotalTime=0l;
		  
		    while(line!=null)
		    {
		    	String[] str=line.split("#");
		    	String topicNo=str[0];
		    	//if(!topicNo.equals("887")) {line=bufferedReader.readLine(); continue;}
		    	//System.out.println("Processing topic:"+topicNo);
		    	Long qDate=Long.parseLong(str[1]);
		    	String qTitle=str[2];
		    	String searchQuery=getQueryBody(QueriesDir+"/"+topicNo+".txt");
		    	
		    	searchQuery=preparePositionRankQuery(searchQuery, keywords, stopwords);
		    			
		    	//System.out.println(searchQuery);
		    	qTitle=preProcessStringStopWordsRemoved(qTitle, true, false, stopwords, false,0);
		    	
		    	//
		    	ArrayList<String> DocSignatures=new ArrayList<String>();
		    	DocSignatures.add(qTitle);
		    	writerQuery.println(searchQuery);
		    	Long starttime=System.currentTimeMillis();

		    	ScoreDoc[] hits=index.searchBody(searchQuery.trim());
		    	Long endtime=System.currentTimeMillis();
		    	Long Result=(endtime-starttime);
		    	TotalTime=TotalTime+Result;
		    	writerlogger.println(topicNo+"#"+Result);
		    	IndexReader reader = DirectoryReader.open(index.iIndex);
		    	IndexSearcher searcher = new IndexSearcher(reader);
		    	

		    	int count=0;
		    	// Now checking every hit to see if it can be added to our result set:
		    	for(int i=0;i<hits.length;i++) {
		    	    int docId = hits[i].doc;
		    	    Document d = searcher.doc(docId);
		    	    String docid=d.get("docID");
		    	    Long docDate=(long)0;
		    	    try{docDate=idDates.get(docid);} catch(Exception e) {e.printStackTrace();}
		    	   // System.out.println(docDate);
		    	    String field=d.get("field");
		    	    String type=d.get("type");
		    	    long datedifference=0;
		    	    if(docDate!=null)
		    	  	   	datedifference=qDate-docDate;
		    		 
		    	    if(field.equals("Opinion")||field.equals("Editor")||field.equals("PostView")) continue;
		    	    if(type.equals("Opinion")||type.equals("Editor")||type.equals("PostView")) continue;
		    		String signature=preProcessStringStopWordsRemoved(d.get("title"), true, false, stopwords, false,0);
		    				
		    	    if(!DocSignatures.contains(signature))  //only if this document was not added before
		    	    if(datedifference>0&&(docDate!=null)&&(docDate!=0)) // only if the retrieved article is published before the current article, it can be added to the result
		    	       	{
		    	    	//System.out.println("Document before: "+docid);
		    	    	count=count+1;
		    	    	
		    	    	writer.println(topicNo+" Q0 "+docid+" 0 "+hits[i].score+" QU_KTR");
		    	    	if(count==100)
		    	    		break;
		    	       	}
		    	    
		    	}
		    	if(count<100)
		    	{
		    		//System.out.println("error"+topicNo);
		    		errorInQueries=errorInQueries+1;
		    	}
		    		
		    	line=bufferedReader.readLine();
		    }
		    	
		    //System.out.println("error in query "+errorInQueries);	
		    System.out.println("Total time for position rank is:"+TotalTime);
		    writer.close();
		    writerlogger.close();
		    writerQuery.close();
		   
		}
		catch(Exception te) {te.printStackTrace();}
	}
	
	public static String preparePositionRankQuery(String text,int numKeywords,ArrayList<String> stopwords)
	{
		// A method that makes sure that only unigrams are returned from position rank and that these unigrams are not stop words
		String searchQuery="";
		String[] tokens=text.split("#");
		int n=0;
		for(int j=0;j<tokens.length;j++)
	        {
	        	String[] data=tokens[j].split(",");
	        	String term=data[0].replace("\"", "");
	        	term=preProcessStringStopWordsRemoved(term, true, true, stopwords, false, 2);
	        	double boost=Double.parseDouble(data[1]);
	        	if(!term.equals(""))
	        		{
	        			searchQuery=searchQuery+term+"^"+boost+" ";	
	        			n=n+1;
	        			
	        			if(n==numKeywords)
	        				break;
	        		}
	        }	
		return searchQuery;
	}
/**
 * A method that applies background linking using the terms extracted using Yake method
 * @param QueryInfo
 * @param QueriesDir
 * @param Direct
 * @param outputfile
 * @param window
 * @param keywords
 */
	public static void processQueriesUsingYake(String QueryInfo, String QueriesDir,String Direct, String outputfile,int window, int keywords)
	{
		try
		{
			int errorInQueries=0;
			luceneIndex index=new luceneIndex("WPostVsNoStem.index");
			PrintWriter writer = new PrintWriter(Direct+"/"+outputfile, "UTF-8");
			//PrintWriter writerLogger = new PrintWriter(Direct+"/Log_Ouput_Win"+window+"_KW"+keywords+".txt", "UTF-8");
			PrintWriter writerQuery = new PrintWriter(Direct+"/query_"+outputfile, "UTF-8");

			/** loading stop words list 
			 */
			ArrayList<String> stopwords=new ArrayList<String>();
			 FileReader fileReader = new FileReader("StopWordsSEO.txt");

		     BufferedReader bufferedReader = new BufferedReader(fileReader);
		        
		        String lineInput=bufferedReader.readLine();
		        while(lineInput!=null)
		        {
		        	stopwords.add(lineInput.trim());
		        	lineInput=bufferedReader.readLine();
		        }
		        bufferedReader.close();
		        
			/**
	    	 * loading ids of documents
	    	 */
			HashMap <String,Long> idDates =new HashMap <String,Long>();
			try
	    	{
			 fileReader = new FileReader("DocIdsDates.txt");

			 bufferedReader = new BufferedReader(fileReader);
	        
		    String line=bufferedReader.readLine();
		   
		  
		    while(line!=null)
		    {
		    	String[] str=line.split(",");
		    	idDates.put(str[0],Long.parseLong(str[1]));
		    	line=bufferedReader.readLine();
		    }

	    	}
	    	catch(Exception te) {te.printStackTrace();}
			
			
			 fileReader = new FileReader(QueryInfo);
			 bufferedReader = new BufferedReader(fileReader);
	        
		    String line=bufferedReader.readLine();
		   
		    Long TotalTime=0l;
		  
		    while(line!=null)
		    {
		    	String[] str=line.split("#");
		    	String topicNo=str[0];
		    	//System.out.println("Processing topic:"+topicNo);
		    	Long qDate=Long.parseLong(str[1]);
		    	String qTitle=str[2];
		    	//System.out.println("Query: "+qTitle);
		    	String searchQuery=getYakeQuery(QueriesDir+topicNo+".txt",keywords);
		    	
		    	qTitle=preProcessStringStopWordsRemoved(qTitle, true, false, stopwords, false,0);
		    	
		    	//writerLogger.println(topicNo+" "+Result);
		    	//
		    	ArrayList<String> DocSignatures=new ArrayList<String>();
		    	DocSignatures.add(qTitle);
		    	writerQuery.println(searchQuery);
		    	Long starttime=System.currentTimeMillis();
		    	
		    	
		    	ScoreDoc[] hits=index.searchBody(searchQuery.trim());
		    	Long endtime=System.currentTimeMillis();
		    	Long Result=(endtime-starttime);
		    	TotalTime=TotalTime+Result;
		    	IndexReader reader = DirectoryReader.open(index.iIndex);
		    	IndexSearcher searcher = new IndexSearcher(reader);
		    	
		    	int count=0;
		    	// Now checking every hit to see if it can be added to our result set:
		    	for(int i=0;i<hits.length;i++) {
		    	    int docId = hits[i].doc;
		    	    Document d = searcher.doc(docId);
		    	    String docid=d.get("docID");
		    	    Long docDate=(long)0;
		    	    try{docDate=idDates.get(docid);} catch(Exception e) {e.printStackTrace();}
		    	   // System.out.println(docDate);
		    	    String field=d.get("field");
		    	    String type=d.get("type");
		    	    long datedifference=0;
		    	    if(docDate!=null)
		    	  	   	datedifference=qDate-docDate;
		    	    else
		    	    	System.out.println("Document without date"+docid);
		    	    if(field.equals("Opinion")||field.equals("Editor")||field.equals("PostView")) continue;
		    	    if(type.equals("Opinion")||type.equals("Editor")||type.equals("PostView")) continue;
		    		String signature=preProcessStringStopWordsRemoved(d.get("title"), true, false, stopwords, false,0);
		    				
		    	    if(!DocSignatures.contains(signature))  //only if this document was not added before
		    	    if(datedifference>0&&(docDate!=null)&&(docDate!=0)) // only if the retrieved article is published before the current article, it can be added to the result
		    	       	{
		    	    	count=count+1;
		    	    	//System.out.println(docid+"  ,  "+d.get("title"));
		    	    	writer.println(topicNo+" Q0 "+docid+" 0 "+hits[i].score+" QU_KTR");
		    	    	if(count==100)
		    	    		break;
		    	       	}
		    	}
		    	if(count<100)
		    		errorInQueries=errorInQueries+1;
		    	line=bufferedReader.readLine();
		    }
		    	
		   // System.out.println("error in query "+errorInQueries);	
		    System.out.println("Total Time For YAKE:"+TotalTime);
		    writer.close();
		    writerQuery.close();
		   // writerLogger.close();
		}
		catch(Exception te) {te.printStackTrace();}
	}
	
	public static String getYakeQuery(String queryPath,int numKeywords)
	{
		// A method that makes sure that only unigrams (not terms separated by a dash or a hyphen) are returned from YAKE! and that these unigrams are not stop words

		
		      try
		      {
		    	 String sq="";
		    	 FileReader fileReader = new FileReader(queryPath);
		      
			     BufferedReader bufferedReader = new BufferedReader(fileReader);
			        
			     String query=bufferedReader.readLine();
			     
			     HashMap<String,Double> termBoosts=new HashMap<String,Double>();
					String[] data=query.split(" ");
					for(int i=0;i<data.length;i++)
					{
						
						int j=data[i].indexOf("^");
						String term=data[i].substring(0,j);
						double boost=Double.parseDouble(data[i].substring(j+1));

						String pterm=preProcessStringStopWordsRemoved(term, true, false, null, false, 2);
						if(!pterm.contentEquals(""))
			        	{	
			        		String[] tokens=pterm.split(" ");
				        	for(String s :tokens)
				        	{
				        		if(termBoosts.containsKey(s))
				        		{
				        			double v=termBoosts.get(s);
				        			termBoosts.put(s, v+boost);
				        		}
				        		else
				        			termBoosts.put(s, boost);
				        			
				        	}
			        	}
						
					}
					
					ArrayList termBoosts_Sorted= (ArrayList) termBoosts.entrySet().stream().sorted(HashMap.Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toList());
						int kcount=0;
				        for (int i=0;i<termBoosts_Sorted.size();i++) {
							if(i==numKeywords)
								break;
							sq=sq+termBoosts_Sorted.get(i).toString().replace("=","^")+" ";
				        }
					
			    return sq.trim();	 
		      }
		      catch(Exception e) {e.printStackTrace();}
	return null;	      
	}
	
	/**
	 * The following same method is used to process queries output from TopicRank and MPR.. just passing the correct queries directory
	 * @param QueriesFile
	 * @param QueriesDir
	 * @param outputDir
	 * @param outputFile
	 * @param keywords
	 */
	public static void processQueriesTopicRank(String QueriesFile,String QueriesDir,String outputDir,String outputFile,int keywords)
	{
		try
		{
			
			int errorInQueries=0;
			luceneIndex index=new luceneIndex("WPostVsNoStem.index");
			PrintWriter writer = new PrintWriter(outputDir+"/"+outputFile, "UTF-8");
			PrintWriter writerQuery = new PrintWriter(outputDir+"/query_"+outputFile, "UTF-8");

			/** loading stop words list 
			 */
			ArrayList<String> stopwords=new ArrayList<String>();
			 FileReader fileReader = new FileReader("StopWordsSEO.txt");

		     BufferedReader bufferedReader = new BufferedReader(fileReader);
		        
		        String lineInput=bufferedReader.readLine();
		        while(lineInput!=null)
		        {
		        	stopwords.add(lineInput.trim());
		        	lineInput=bufferedReader.readLine();
		        }
		        bufferedReader.close();
		        
			/**
	    	 * loading ids of documents
	    	 */
			HashMap <String,Long> idDates =new HashMap <String,Long>();
			try
	    	{
			 fileReader = new FileReader("DocIdsDates.txt");

			 bufferedReader = new BufferedReader(fileReader);
	        
		    String line=bufferedReader.readLine();
		   
		  
		    while(line!=null)
		    {
		    	String[] str=line.split(",");
		    	idDates.put(str[0],Long.parseLong(str[1]));
		    	line=bufferedReader.readLine();
		    }

	    	}
	    	catch(Exception te) {te.printStackTrace();}
			
			
			 fileReader = new FileReader(QueriesFile);
			 bufferedReader = new BufferedReader(fileReader);
	        
		    String line=bufferedReader.readLine();
		   
		    Long TotalTime=0l;
		  
		    while(line!=null)
		    {
		    	String[] str=line.split("#");
		    	String topicNo=str[0];
		    	//if(!topicNo.equals("887")) {line=bufferedReader.readLine(); continue;}
		    	//System.out.println("Processing topic:"+topicNo);
		    	Long qDate=Long.parseLong(str[1]);
		    	String qTitle=str[2];
		    	String searchQuery=getQueryBody(QueriesDir+"/"+topicNo+".txt");
		    	
		    	searchQuery=prepareTopicRankQuery(searchQuery, keywords, stopwords);
		    	//if(true)
		    	//	break;
		    	//System.out.println(searchQuery);
		    	qTitle=preProcessStringStopWordsRemoved(qTitle, true, false, stopwords, false,0);
		    	
		    	//
		    	ArrayList<String> DocSignatures=new ArrayList<String>();
		    	DocSignatures.add(qTitle);
		    	writerQuery.println(searchQuery);
		    	Long starttime=System.currentTimeMillis();
		    	
		    	ScoreDoc[] hits=index.searchBody(searchQuery.trim());
		    	Long endtime=System.currentTimeMillis();
		    	Long Result=(endtime-starttime);
		    	TotalTime=TotalTime+Result;
		    	IndexReader reader = DirectoryReader.open(index.iIndex);
		    	IndexSearcher searcher = new IndexSearcher(reader);
		    	

		    	int count=0;
		    	// Now checking every hit to see if it can be added to our result set:
		    	for(int i=0;i<hits.length;i++) {
		    	    int docId = hits[i].doc;
		    	    Document d = searcher.doc(docId);
		    	    String docid=d.get("docID");
		    	    Long docDate=(long)0;
		    	    try{docDate=idDates.get(docid);} catch(Exception e) {e.printStackTrace();}
		    	   // System.out.println(docDate);
		    	    String field=d.get("field");
		    	    String type=d.get("type");
		    	    long datedifference=0;
		    	    if(docDate!=null)
		    	  	   	datedifference=qDate-docDate;
		    		 
		    	    if(field.equals("Opinion")||field.equals("Editor")||field.equals("PostView")) continue;
		    	    if(type.equals("Opinion")||type.equals("Editor")||type.equals("PostView")) continue;
		    		String signature=preProcessStringStopWordsRemoved(d.get("title"), true, false, stopwords, false,0);
		    				
		    	    if(!DocSignatures.contains(signature))  //only if this document was not added before
		    	    if(datedifference>0&&(docDate!=null)&&(docDate!=0)) // only if the retrieved article is published before the current article, it can be added to the result
		    	       	{
		    	    	//System.out.println("Document before: "+docid);
		    	    	count=count+1;
		    	    	
		    	    	//writer.println(topicNo+" Q0 "+docid+" 0 "+hits[i].score+" QU_KTR");
		    	    	if(count==100)
		    	    		break;
		    	       	}
		    	    
		    	}
		    	if(count<100)
		    	{
		    		//System.out.println("error"+topicNo);
		    		errorInQueries=errorInQueries+1;
		    	}
		    		
		    	line=bufferedReader.readLine();
		    }
		    System.out.println("Total Time for TRank: "+TotalTime);	

		   // System.out.println("error in query "+errorInQueries);	
		    writer.close();
		    writerQuery.close();
		   
		}
		catch(Exception te) {te.printStackTrace();}
	}
	public static String prepareTopicRankQuery(String text,int numKeywords,ArrayList<String> stopwords)
	{
		// Splitting the bigrams and trigrams output by TopicRank , if any, into unigrams and aggregating the scores of unigrams
		String searchQuery="";
		String[] keyphrases=text.split("@@");
		int n=0;
		HashMap<String,Double> tokenBoosts=new HashMap<String,Double> ();
		for(int j=0;j<keyphrases.length;j++)
	        {
	        	
	        	String[] data=keyphrases[j].split("##");
	        	String term=data[0];//.replace("\"", "");
	        	term=preProcessStringStopWordsRemoved(term, true, true, stopwords, false, 2);
	        	double boost=Double.parseDouble(data[1]);
	        	/**
	        	 * Now adding and aggregating scores for tokens
	        	 */
	        	if(!term.contentEquals(""))
	        	{	
	        		String[] tokens=term.split(" ");
		        	for(String s :tokens)
		        	{
		        		if(tokenBoosts.containsKey(s))
		        		{
		        			double v=tokenBoosts.get(s);
		        			tokenBoosts.put(s, v+boost);
		        		}
		        		else
		        			tokenBoosts.put(s, boost);
		        			
		        	}
	        	}
	        }	
		 ArrayList termBoosts_Sorted= (ArrayList) tokenBoosts.entrySet().stream().sorted(HashMap.Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toList());
	      //  System.out.println(termBoosts_Sorted);
			int kcount=0;
	        for (int i=0;i<termBoosts_Sorted.size();i++) {
						
	        	searchQuery=searchQuery+termBoosts_Sorted.get(i).toString().replace("=","^")+" ";
				kcount=kcount+1;
				if(kcount==numKeywords)
					break;
			}
			
		return searchQuery;
	}
/**
 * A method that applies background linking using the terms extracted using the sCake method
 * @param QueryInfo
 * @param Direct
 * @param outDir
 * @param outfile
 * @param keywords
 */
	public static void processQueriesUsing_sCake(String QueryInfo, String Direct,String outDir,String outfile, int keywords)
	{
		try
		{
			int errorInQueries=0;
			luceneIndex index=new luceneIndex("V3Index_Stemmed.index");
			PrintWriter writer = new PrintWriter(outDir+"/"+outfile, "UTF-8");
			//PrintWriter writerLogger = new PrintWriter(Direct+"/Log_Ouput_Win"+window+"_KW"+keywords+".txt", "UTF-8");
			PrintWriter writerQuery = new PrintWriter(Direct+"/query_"+outfile, "UTF-8");

			/** loading stop words list 
			 */
			ArrayList<String> stopwords=new ArrayList<String>();
			 FileReader fileReader = new FileReader("StopWordsSEO.txt");

		     BufferedReader bufferedReader = new BufferedReader(fileReader);
		        
		        String lineInput=bufferedReader.readLine();
		        while(lineInput!=null)
		        {
		        	stopwords.add(lineInput.trim());
		        	lineInput=bufferedReader.readLine();
		        }
		        bufferedReader.close();
		        
			/**
	    	 * loading ids of documents
	    	 */
			HashMap <String,Long> idDates =new HashMap <String,Long>();
			try
	    	{
			 fileReader = new FileReader("DocIdsDates.txt");

			 bufferedReader = new BufferedReader(fileReader);
	        
		    String line=bufferedReader.readLine();
		   
		  
		    while(line!=null)
		    {
		    	String[] str=line.split(",");
		    	idDates.put(str[0],Long.parseLong(str[1]));
		    	line=bufferedReader.readLine();
		    }

	    	}
	    	catch(Exception te) {te.printStackTrace();}
			
			
			 fileReader = new FileReader(QueryInfo);
			 bufferedReader = new BufferedReader(fileReader);
	        
		    String line=bufferedReader.readLine();
		   
		    Long TotalTime=0l;
		  
		    while(line!=null)
		    {
		    	String[] str=line.split("#");
		    	String topicNo=str[0];
		    	//System.out.println("Processing topic:"+topicNo);
		    	Long qDate=Long.parseLong(str[1]);
		    	String qTitle=str[2];
		    	//System.out.println("Query: "+qTitle);
		    	String searchQuery=getsCakeQuery(Direct+"/SCScore/"+topicNo+"ranked_list.csv",keywords);
		    	
		    	
		    	
		    	qTitle=preProcessStringStopWordsRemoved(qTitle, true, false, stopwords, false,0);
		    	
		    	//writerLogger.println(topicNo+" "+Result);
		    	//
		    	ArrayList<String> DocSignatures=new ArrayList<String>();
		    	DocSignatures.add(qTitle);
		    	
		    	//System.out.println(topicNo+"  "+searchQuery);
		    	writerQuery.println(searchQuery);
		    	Long starttime=System.currentTimeMillis();
		    	
		    	ScoreDoc[] hits=index.searchBody(searchQuery.trim());
		    	Long endtime=System.currentTimeMillis();
		    	Long Result=(endtime-starttime);
		    	TotalTime=TotalTime+Result;
		    	IndexReader reader = DirectoryReader.open(index.iIndex);
		    	IndexSearcher searcher = new IndexSearcher(reader);
		    	
		    	int count=0;
		    	// Now checking every hit to see if it can be added to our result set:
		    	for(int i=0;i<hits.length;i++) {
		    	    int docId = hits[i].doc;
		    	    Document d = searcher.doc(docId);
		    	    String docid=d.get("docID");
		    	    Long docDate=(long)0;
		    	    try{docDate=idDates.get(docid);} catch(Exception e) {e.printStackTrace();}
		    	   // System.out.println(docDate);
		    	    String field=d.get("field");
		    	    String type=d.get("type");
		    	    long datedifference=0;
		    	    if(docDate!=null)
		    	  	   	datedifference=qDate-docDate;
		    	    else
		    	    	System.out.println("Document without date"+docid);
		    	    if(field.equals("Opinion")||field.equals("Editor")||field.equals("PostView")) continue;
		    	    if(type.equals("Opinion")||type.equals("Editor")||type.equals("PostView")) continue;
		    		String signature=preProcessStringStopWordsRemoved(d.get("title"), true, false, stopwords, false,0);
		    				
		    	    if(!DocSignatures.contains(signature))  //only if this document was not added before
		    	    if(datedifference>0&&(docDate!=null)&&(docDate!=0)) // only if the retrieved article is published before the current article, it can be added to the result
		    	       	{
		    	    	count=count+1;
		    	    	//System.out.println(docid+"  ,  "+d.get("title"));
		    	    	writer.println(topicNo+" Q0 "+docid+" 0 "+hits[i].score+" QU_KTR");
		    	    	if(count==100)
		    	    		break;
		    	       	}
		    	}
		    	if(count<100)
		    		errorInQueries=errorInQueries+1;
		    	//System.out.println("Do you want to continue");
		    	//int inR=System.in.read();
		    	//if(inR==0)
		    	//	break;
		    	line=bufferedReader.readLine();
		    }
		    	
		    //System.out.println("error in query "+errorInQueries);	
		    System.out.println("Total Time for sCake:"+TotalTime);
		    writer.close();
		    writerQuery.close();
		   // writerLogger.close();
		}
		catch(Exception te) {te.printStackTrace();}
	}
	
	public static String getsCakeQuery(String queryfile,int keywords)

	{
		HashMap<String,Double> termBoosts=new HashMap<String,Double>();
		//System.out.println(queryfile);
		
		String searchQuery="";
		int processedKeywords=0;
		try
		{
		 FileReader fileReader = new FileReader(queryfile);

	     BufferedReader bufferedReader = new BufferedReader(fileReader);
	        
	        String lineInput=bufferedReader.readLine();
	        lineInput=bufferedReader.readLine(); //The first line contains the csv header, so skip it
	        while(lineInput!=null)
	        {
	        	String[] data=lineInput.split(",");
	        	String term=data[0].replace("\"", "");
	        	/**
	        	 * processing terms to remove malfunction ones
	        	 */
	        	String pterm=preProcessStringStopWordsRemoved(term, true, false, null, false, 2);
				double boost=Double.parseDouble(data[1]);
				if(!pterm.contentEquals(""))
	        	{	
	        		String[] tokens=pterm.split("\\s+");
		        	for(String s :tokens)
		        	{
		        		if(termBoosts.containsKey(s))
		        		{
		        			double v=termBoosts.get(s);
		        			termBoosts.put(s, v+boost);
		        		}
		        		else
		        			termBoosts.put(s, boost);
		        			
		        	}
	        	}
	        	lineInput=bufferedReader.readLine();
	        }
	        ArrayList termBoosts_Sorted= (ArrayList) termBoosts.entrySet().stream().sorted(HashMap.Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toList());
	        for (int i=0;i<termBoosts_Sorted.size();i++) {
				
				searchQuery=searchQuery+termBoosts_Sorted.get(i).toString().replace("=","^")+" ";
		        processedKeywords=processedKeywords+1;
		        if(processedKeywords==keywords)
		        	break;
		        }
	       
		}
		catch(Exception e)
		{
			System.out.println("error processing file of query:  "+queryfile);
			e.printStackTrace();
		}
		return searchQuery.strip();
	}

	public static String getQueryBody(String filename)
	{
		// We prepared a text file for each query containing its title and body contents after removing all html tags.
		String body="";
		try
    	{
		FileReader fileReader = new FileReader(filename);

		BufferedReader bufferedReader = new BufferedReader(fileReader);
        while(true)
	    {
	    	String line=bufferedReader.readLine();
	    	if(line==null)
	    		break;
	    	body=body+" "+line;	
	    }
    	}
		
		catch(Exception te) {}
		return body;
	}

	public static String getTermsTFIDF(String text,boolean idf,int numTerms,String lindex)
	{
		HashMap<String,Double> terms=new HashMap<String,Double> ();
		String searchQuery="";
		String[] tokens=text.split("\\s+");
		for(int i=0;i<tokens.length;i++)
		{
			if(terms.containsKey(tokens[i]))
			{
				double count=terms.get(tokens[i]);
				terms.put(tokens[i],count+1);
			}
			else
				terms.put(tokens[i],1.0);
		}
		
		if(idf)
		{
			try {
			luceneIndex index=new luceneIndex(lindex);
			IndexReader reader = DirectoryReader.open(index.iIndex);
			int totalNumDocs=reader.numDocs();
			
			HashMap<String,Double> termsIdf=new HashMap<String,Double> ();
			for (Map.Entry me : terms.entrySet()) {
				String term=(String)me.getKey();
				double v=(Double)me.getValue();
				int df=reader.docFreq(new Term("body", term));
				termsIdf.put(term, v*Math.log(totalNumDocs/df));
		        }
			terms=termsIdf;
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			
		}
		ArrayList termBoosts_Sorted= (ArrayList) terms.entrySet().stream().sorted(HashMap.Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toList());
		int processedKeywords=0;
		for (int i=0;i<termBoosts_Sorted.size();i++) {
			
			searchQuery=searchQuery+termBoosts_Sorted.get(i).toString().replace("=","^")+" ";
	        processedKeywords=processedKeywords+1;
	        if(numTerms!=-1)
	        	if(processedKeywords==numTerms)
	        		break;
	        }
		return searchQuery;
	}

	/**
	 * A method that gets the paragraphs of the artice from a sql database of articles stored on desk
	 * @param docId
	 * @param numOfParagraphs
	 * @return
	 */
	public static String getParagraphs(String docId,int numOfParagraphs)
	{
		String paragraphQuery="";
		try{  
			 
			Connection con=DriverManager.getConnection(  
					"jdbc:mysql://localhost:3306/WpostDB","root","123456789"); 
				  
			//trying retrieve statement 
			Statement stmt=con.createStatement();  
			ResultSet rs=stmt.executeQuery("SELECT data FROM contents where did='" + docId + "' order by pos");//insert into Documents values ('mmf57310e5c8ec7833d6756ba637332e','titlebla','kickerbla','urlblabla','authorbla',1361143680000)");  
			
			int scannedP=0;
			while(rs.next())  
			{
				String pText=rs.getString(1);
				if(pText.split(" ").length<20)
				{
					paragraphQuery=paragraphQuery+" "+pText;
				}
				else
				{
					paragraphQuery=paragraphQuery+" "+pText;
					scannedP=scannedP+1;
				}
				if(numOfParagraphs!=-1)
					if(scannedP==numOfParagraphs)
						break;
				
			}
			//System.out.println(scannedP);
			con.close();  
			}catch(Exception e){ System.out.println(e);}  
		return paragraphQuery;
	}
	
	/**
	 * A method that uses the leadining paragraphs as a search query to retrieve the background links
	 * @param docId
	 * @param numOfParagraphs
	 * @return
	 */

	public static void processQueries_Paragraphs(String QueriesFile,String outputDir,int paragraph)
	{
		PrintWriter writer = new PrintWriter(outputDir+"/Para_"+paragraph, "UTF-8");
		
		try
		{
			
			int errorInQueries=0;
			luceneIndex index=new luceneIndex("V3Index_Stemmed.index");
			
			
			/** loading stop words list 
			 */
			ArrayList<String> stopwords=new ArrayList<String>();
			 FileReader fileReader = new FileReader("StopWordsLists/StopWordsSEO.txt");

		     BufferedReader bufferedReader = new BufferedReader(fileReader);
		        
		        String lineInput=bufferedReader.readLine();
		        while(lineInput!=null)
		        {
		        	stopwords.add(lineInput.trim());
		        	lineInput=bufferedReader.readLine();
		        }
		        bufferedReader.close();
		        
			/**
	    	 * loading ids of documents
	    	 */
			HashMap <String,Long> idDates =new HashMap <String,Long>();
			try
	    	{
			 fileReader = new FileReader("DocIdsDates.txt");

			 bufferedReader = new BufferedReader(fileReader);
	        
		    String line=bufferedReader.readLine();
		   
		  
		    while(line!=null)
		    {
		    	String[] str=line.split(",");
		    	idDates.put(str[0],Long.parseLong(str[1]));
		    	line=bufferedReader.readLine();
		    }

	    	}
	    	catch(Exception te) {te.printStackTrace();}
			
			
			 fileReader = new FileReader("AlgoProjectData/"+QueriesFile);
			 bufferedReader = new BufferedReader(fileReader);
	        
		    String line=bufferedReader.readLine();
		   
		    Class.forName("com.mysql.jdbc.Driver");  
			long sumtime=0;
		    while(line!=null)
		    {
		    	String[] str=line.split(",");
		    	String topicNo=str[1];
		    	String qID=str[0];
		    	Long qDate=Long.parseLong(str[2]);
		    	String qTitle=str[4];
		    	bufferedReader.readLine();//Skipping the query body part
		    	String searchQuery=getParagraphs(qID, paragraph);
		    	searchQuery=preProcessStringStopWordsRemoved(searchQuery, true, true, stopwords, false,2);
		    	
		    	searchQuery=preProcessStringStopWordsRemoved(qTitle, true, true, stopwords, false,2)+" "+searchQuery;
		    	if(searchQuery.contentEquals(""))
			    	{
		    			System.out.println("error in topic:"+topicNo);
		    			break;
			    	}
		    	
		    	ArrayList<String> DocSignatures=new ArrayList<String>();
		    	DocSignatures.add(qTitle);
		    	Long starttime=System.currentTimeMillis();
		    	ScoreDoc[] hits=index.searchBody(searchQuery.trim());
		    	Long endtime=System.currentTimeMillis();
		    	Long Result=(endtime-starttime);
		    	sumtime=sumtime+Result;

		    	IndexReader reader = DirectoryReader.open(index.iIndex);
		    	//logger.println(topicNo+" "+searchQuery.split(" ").length+" "+getSumOfTermFrequencyDF(searchQuery,reader)+" "+Result);
		    	//logger.println(topicNo+" "+searchQuery.split(" ").length+" "+getIntersectionPostingList(searchQuery,reader)+" "+Result);
		    	
		    	IndexSearcher searcher = new IndexSearcher(reader);
		    	
		    //	System.out.println("-------");
		    	
		    	int count=0;
		    	// Now checking every hit to see if it can be added to our result set:
		    	for(int i=0;i<hits.length;i++) {
		    	    int docId = hits[i].doc;
		    	    Document d = searcher.doc(docId);
		    	    String docid=d.get("docID");
		    	    Long docDate=(long)0;
		    	    try{docDate=idDates.get(docid);} catch(Exception e) {e.printStackTrace();}
		    	   // System.out.println(docDate);
		    	    String field=d.get("field");
		    	    String type=d.get("type");
		    	    long datedifference=0;
		    	    if(docDate!=null)
		    	  	   	datedifference=qDate-docDate;
		    		 
		    	    if(field.equals("Opinion")||field.equals("Editor")||field.equals("PostView")) continue;
		    	    if(type.equals("Opinion")||type.equals("Editor")||type.equals("PostView")) continue;
		    		String signature=preProcessStringStopWordsRemoved(d.get("title"), true, false, stopwords, false,0);
		    			
		    	    if(!DocSignatures.contains(signature))  //only if this document was not added before
		    	    if(datedifference>0&&(docDate!=null)&&(docDate!=0)) // only if the retrieved article is published before the current article, it can be added to the result
		    	       	{

		    	    	count=count+1;
		    	    	
		    	    	writer.println(topicNo+" Q0 "+docid+" 0 "+hits[i].score+" QU_KTR");
		    	    	if(count==5)
		    	    		break;
		    	       	}
		    	    
		    	}
		    	if(count<100)
		    	{
		    		System.out.println("error "+topicNo);
		    		errorInQueries=errorInQueries+1;
		    	}
		    		
		    	line=bufferedReader.readLine();
		    }
		    System.out.println(sumtime);	
		    writer.close();
		   
		}
		catch(Exception te) {te.printStackTrace();}
	}
	
	/**
	 * A method that reranks a run to generate an oracle run out of it
	 * @param qrel
	 * @param runfile
	 * @param outFile
	 * @param noDocs
	 */
	public static void reRankRun(String qrel,String runfile,String outFile,int noDocs)
	{
		HashMap<String,HashMap<String,Integer>> qreldata= new HashMap<String,HashMap<String,Integer>>();
		
		
		try
		{
			PrintWriter writer = new PrintWriter(outFile, "UTF-8");
			//Reading the qrel file
			 FileReader fileReader = new FileReader(qrel);
			 BufferedReader bufferedReader = new BufferedReader(fileReader);
		        
			    String lineInput=bufferedReader.readLine();
			        while(lineInput!=null)
			        {
			        	String[] data=lineInput.split(" ");
			        	int score=Integer.parseInt(data[3]);
						
		        		switch (score)
			        	{
			        	case 16:
			        		score=4;
			        		break;
			        	case 8:
			        		score=3;
			        		break;
			        	case 4:
			        		score=2;
			        		break;
			        	case 2:
			        		score=1;
			        		break;
			        	case 0:
			        		break;
			        	}
		        		
			        	if(qreldata.containsKey(data[0]))
			        	{	qreldata.get(data[0]).put(data[2],score);	
			        	}
			        	else
			        	{
			        		HashMap<String,Integer> qdata=new HashMap<String,Integer>();
			        		qdata.put(data[2],score);	
			        		qreldata.put(data[0], qdata);
			     
			        	}
			        	
			        	lineInput=bufferedReader.readLine();
			        }
			        bufferedReader.close();   
			
			        //Reading run file
			        
			        fileReader = new FileReader(runfile);
					bufferedReader = new BufferedReader(fileReader);
					HashMap<String,Integer> rundata= new HashMap<String,Integer>();
					    
					lineInput=bufferedReader.readLine();
					boolean firstrow=true;
					String qno="";
					String Newqno="";
					int docsCounttoRank=0;
					        while(lineInput!=null)
					        {
					        	String[] data=lineInput.split(" ");
					        	Newqno=data[0];
					        	String doc=data[2];
					        	Integer judgement=0;
					        	try
					        	{
					        		judgement=qreldata.get(Newqno).get(doc);
					        	
					        	}
					        	catch(Exception e) {}
					        	if(judgement==null)
					        		judgement=0;
					        	
					        	if(firstrow) // Reading the first row of the ranking file
					        	{
					        		firstrow=false;
					        		
					        	}
					        	else 
					        	{
					        		if(!Newqno.equals(qno)) // a new query is found in the file, now sort and write the data for the previous query
					        		{
					        			ArrayList docs_Sorted= (ArrayList) rundata.entrySet().stream().sorted(HashMap.Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toList());
									      //  System.out.println(termBoosts_Sorted);
					        			int jscore=rundata.size()+1;
					        			//System.out.println("Queries for qno"+qno+" is: "+rundata.size());
											int kcount=0;
									        for (int i=0;i<docs_Sorted.size();i++) {
												if(i==noDocs)
													break;
												String docNo=docs_Sorted.get(i).toString();
												String fdocNo=docNo.substring(0,docNo.indexOf("="));
												
									        	writer.println(qno+" Q0 "+fdocNo+" 0 "+jscore+" QU_KTR "+jscore);
									        	jscore=jscore-1;

									        }
									        rundata.clear();
									        docsCounttoRank=0;
									        qno=Newqno;
									       // System.out.println("-----------------------------------");
									        
					        		}
					        		
					        		
					        	}
					        	qno=Newqno;
			        			if(docsCounttoRank<noDocs)
			        				{
			        					docsCounttoRank=docsCounttoRank+1;
			        					rundata.put(doc,judgement);
			        				}
					        	lineInput=bufferedReader.readLine();
					        	
					        }
					        // Now writing the data for the last query
					        
					        int jscore=rundata.size()+1;

					        ArrayList docs_Sorted= (ArrayList) rundata.entrySet().stream().sorted(HashMap.Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toList());
							   
					        for (int i=0;i<docs_Sorted.size();i++) {
								if(i==noDocs)
									break;
								String docNo=docs_Sorted.get(i).toString();
								String fdocNo=docNo.substring(0,docNo.indexOf("="));
					        	writer.println(qno+" Q0 "+fdocNo+" 0 "+jscore+" QU_KTR "+jscore);
					        	jscore=jscore-1;

					        }
					        
					        writer.close();			      
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
	}

}
