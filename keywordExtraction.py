import yake
import os
import re
import time
import string

import pke
from nltk.corpus import stopwords

from position_rank import position_rank
from tokenizer import StanfordCoreNlpTokenizer


def getQueryUsingYake(text, numOfKeywords):
    language = "en"
    max_ngram_size = 1
    deduplication_thresold = 1#0.9
    deduplication_algo = 'seqm'
    custom_kw_extractor = yake.KeywordExtractor(lan=language, n=max_ngram_size, dedupLim=deduplication_thresold,
                                                dedupFunc=deduplication_algo, windowsSize=1, top=numOfKeywords,
                                                features=None)
    keywords = custom_kw_extractor.extract_keywords(text)
    #print(keywords)
    sq=""
    for kw in keywords:
        score=1.0/kw[1]
        sq=sq+kw[0]+"^"+str(score)+" "
    return sq

#the below method takes the directory where each query to be processed have a .txt file with the title and all paragraphs concatenated.
def generateAllQueriesUsingYake(queriesDir,OutputDir,windowSize,numOfKeywords):
    #Creating a directory to save the outputs in
    outputDirName=OutputDir+str(windowSize)+"_"+str(numOfKeywords)
    os.mkdir(outputDirName)
    # Reading the queries and issue search queries for each using Yake
    count=0
    totaltime=0
    for subdir, dirs, files in os.walk(queriesDir):
        for filename in files:
            if(filename.__contains__(".txt")):
                count=count+1
                print("processing:",filename)
                writerQuery = open(outputDirName + "/"+filename, "w")
                f = open(queriesDir+"/"+filename, newline='', encoding='utf-8')
                for line in f.readlines():
                    starttime=time.time()
                    searchQuery=getQueryUsingYake(line,numOfKeywords)
                    totaltime=totaltime+(time.time()-starttime)
                    print(searchQuery)
                    writerQuery.write(searchQuery.strip())
                    writerQuery.close()
                    break
    print("time",totaltime)
    print("processed:",count,"queries")

def getQueryUsingPRank_WithPhrases(tokenizer,line,windowSize):
    sq=""
    Results = position_rank(line, tokenizer,num_keyphrase = 1000,window_size=windowSize)
    print(Results)
    Keywords={}
    for r in Results.keys():
        tokens=re.sub("[^a-zA-Z]+", " ",r).lower().split(" ")
        for t in tokens:
            if Keywords.__contains__(t):
                Keywords[t]=Keywords[t]+Results[r]
            else:
                Keywords[t] = Results[r]
    sorteddict={k: v for k, v in sorted(Keywords.items(), key=lambda item: item[1],reverse=True)}
    for keyw in sorteddict:
        sq=sq+keyw+","+str(sorteddict[keyw])+"#"
    return sq

def generateAllQueriesUsingPRank(queriesDir,OutputDir,windowSize):
    tokenizer = StanfordCoreNlpTokenizer("http://localhost", port=9000)
    #Creating a directory to save the outputs in
    outputDirName=OutputDir+str(windowSize)
    os.mkdir(outputDirName)
    # Reading the queries and issue search queries for each using Yake
    count=0
    Totaltime=0
    for subdir, dirs, files in os.walk(queriesDir):
        for filename in files:
            if(filename.__contains__(".txt")):
                count=count+1
                print("processing:",filename)
                writerQuery = open(outputDirName + "/"+filename, "w")
                f = open(queriesDir+"/"+filename, newline='', encoding='utf-8')
                for line in f.readlines():
                    now = round(time.time())
                    searchQuery=getQueryUsingPRank_WithPhrases(tokenizer,line,windowSize)
                    Totaltime=Totaltime+( round(time.time())-now)
                    #print(searchQuery)
                    writerQuery.write(searchQuery.strip())
                    writerQuery.close()
                    break

    print("TotalTime:",Totaltime)

def pkeExtractorTopicRank(keywords,queriesDir):
    outputDirName="TopicRank"
    count=0
    totaltime = 0
    for subdir, dirs, files in os.walk(queriesDir):
        for filename in files:
            if (filename.__contains__(".txt")):
                count = count + 1
                print("processing:", filename)
                writerQuery = open(outputDirName + "/" + filename, "w")
                extractor = pke.unsupervised.TopicRank()
                t1 = time.time_ns()
                extractor.load_document(input=queriesDir + "/" + filename, language='en')
                extractor.candidate_selection()
                # candidate weighting, in the case of TopicRank: using a random walk algorithm
                extractor.candidate_weighting()
                keyphrases = extractor.get_n_best(n=keywords)
                totaltime = totaltime + (time.time_ns() - t1)
                searchQuery = ""
                i = 0
                while i < len(keyphrases):
                    searchQuery = searchQuery + keyphrases[i][0] + "##" + str(keyphrases[i][1]) + "@@"
                    i = i + 1
                print(searchQuery)
                writerQuery.write(searchQuery.strip())
                writerQuery.close()
    print(totaltime)
    print("Processed",count)
    print("done")
def pkeExtractorMultipartiteRank(keywords,queriesDir):
    outputDirName="MultipartiteRank"
    count=0
    totaltime=0
    for subdir, dirs, files in os.walk(queriesDir):
        for filename in files:
            if (filename.__contains__(".txt")):
                count = count + 1
                print("processing:", filename)
                writerQuery = open(outputDirName + "/" + filename, "w")
                extractor = pke.unsupervised.MultipartiteRank()
                pos = {'NOUN', 'PROPN', 'ADJ'}
                stoplist = list(string.punctuation)
                stoplist += ['-lrb-', '-rrb-', '-lcb-', '-rcb-', '-lsb-', '-rsb-']
                stoplist += stopwords.words('english')
                t1 = time.time_ns()
                extractor.load_document(input=queriesDir + "/" + filename, language='en')
                extractor.candidate_selection(pos=pos, stoplist=stoplist)
                # candidate weighting, in the case of TopicRank: using a random walk algorithm
                extractor.candidate_weighting(alpha=1.1,
                              threshold=0.74,
                              method='average')
                # N-best selection, keyphrases contains the 10 highest scored candidates as
                # (keyphrase, score) tuples
                keyphrases = extractor.get_n_best(n=keywords)
                totaltime=totaltime+(time.time_ns()-t1)
                searchQuery = ""
                i = 0
                while i < len(keyphrases):
                    searchQuery = searchQuery + keyphrases[i][0] + "##" + str(keyphrases[i][1]) + "@@"
                    i = i + 1
                print(searchQuery)
                writerQuery.write(searchQuery.strip())
                writerQuery.close()
    print(totaltime//1_000_000)

           # break

    print("Processed",count)
    print("done")

if __name__ == '__main__':
    generateAllQueriesUsingYake("QueriesTxtAll","Yake/", 1, 1000)
    generateAllQueriesUsingPRank("QueriesTxtAll", "PosRank/",3)
    pkeExtractorMultipartiteRank(1000,"QueriesTxtAll")
    pkeExtractorTopicRank(1000,"QueriesTxtAll")
