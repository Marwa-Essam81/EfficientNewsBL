# EfficientNewsBL
This repository contains the source code for the experiments done for the efficient background linking of news articles. 

The files here are as follows:

1) keywordExtraction.py 
=====================
This is a pyhton file that contains an implementation of the keyword extraction and query generation process for the methods: PositionRank, TopicRank, MPR and Yake. In order to run methods in these files you need to:
	- setup of an environment with the following pythonn libraries: position_rank, pke and yake. All are available on github:
	
		https://github.com/ymym3412/position-rank
		
		https://github.com/LIAAD/yake
		
		https://github.com/boudinfl/pke
		

	- PositionRank also requires you the StanfordCoreNlp library. Make sure that it is running in your system.

	- A directory that has a text file for each article query that you want to process. This text file should contain a 	concatenation of the article's title and paragraphs after removing the html tags (without any preprocessing such as lower casing or stop words removal). These text files are not provided here for copyright issues of the Washignton Post Dataset.


2) Java files for creating the inverted index for the Washignton Post collection in Lucene:
indexer.java
luceneIndex.java
thIndexer.java

	- In order to create the index, you need to download first the Washignton Post collection file from https://trec.nist.gov/
	- the indexer class will first split this file into multiple files for quicker indexing process, then it will call multiple threads to start indexing. 
	- You need to have these dependecies in your created java project:
	JSOUP, Lucene V8.0 ,JSON
	- Preprocessing needs a stop words file also ((provided in this directory)


3) Java files for implementing the graph-based keyword extraction methods: k-Core and k-Truss:
DocGraph.java
graphBasedAnalysis.java


4) BackgroundLinking.java
===========================
In this file, you will find the implementation of the background linking process using each keyword extraction methods as described in the paper. In order to run the linking methods, you need:
	- stop words file (provided in this directory)
	- Query information files (provided in this directory), where each line has the topic number, the query document id , date and title.
	- an inverted index created using lucene
	- the directories for queries extracted using the python libraries extracted above.
	- for sCake, we didn't provide the code here as it is available on Github. You just need to run it using the text files of the query articles and then copy 		the final directory "SCSCore" created by sCake that contains the ".csv" files for the queries. check: https://github.com/SDuari/sCAKE-and-LAKE


