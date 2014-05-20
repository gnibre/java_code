package manage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import data.PDocument;
import data.TokenItem;


import util.Logger;

/**
 * rank the query result with algorithm that listed on ppt;
 * 
 * @author gnibrE
 *
 */
public class Ranker {
	
	
	/**
	 *
	 * related docs are ranked, before returned to requester
	 * 
	 * Document sorce is  K(term count in query)*idk*idk*C(term count in doc)
	 * 
	 * rules are as follows from ppt:
	 * 
	 * Let I be the IDF of T, and K be the count of T in Q;
    Set the weight of T in Q:   W = K * I;
    Let L be the list of TokenOccurences of T from H;

    For each TokenOccurence, O, in L:
	 *          Let D be the document of O, and C be the count of O (tf of T in D);
	    If D is not already in R (D was not previously retrieved)                      
               Then add D to R and initialize score to 0.0;
        Increment D’s score by W * I * C; (product of T-weight in Q and D)		
	 * 
	 * 
	 */
	public static PriorityQueue<PDocument> rankRelatedDocsWithScoreAG1(List<String> tls){
		
		// Query vector:   
		HashMap<String,Integer> termCount = new HashMap<String,Integer>();
		for(String queryTerm:tls){
			if(termCount.containsKey(queryTerm)){
				int count = termCount.get(queryTerm);
				count++;
				termCount.put(queryTerm, count);
			}else{
				termCount.put(queryTerm, 1);
			}
		}
		
		Logger.println(" got query vecoter for ranking : ");
		TermsManager tm = TermsManager.getTermManagerInstance();
		DocumentsManager dm =  DocumentsManager.getDocumentsManager();
		TokenItem ti;
		
		
		double powerSum = 0;
		// get/re-calc idf for each term;
		// also get the length of query vector;
		for(String t:termCount.keySet()){
			Logger.print("  "+t+"->"+termCount.get(t));
			
			ti = tm.getToken(t);
			if (ti == null) {
				Logger.println("query term not in the record, ignored... no document for this term: "
						+ t);
				continue;
			}
			ti.calcIDF();
			powerSum+=termCount.get(t)*termCount.get(t);
		}
		double lengthOfQuery = Math.sqrt(powerSum);
		Logger.println(" lengthOfQuery:  "+lengthOfQuery);
		
		
		// document url -> dot product for each doc, will add up to the final dot product;( related to this query of cause);
		HashMap<String,Double> docScoreMap = new HashMap<String,Double>();
		Set<String> allRelatedDocSet = new HashSet<String>();
		
		double score;
		PDocument pd;
		HashMap<String, Integer> postings;// postings for each term;
		for(String term:termCount.keySet()){
			ti = tm.getToken(term);
			if (ti == null) {
				Logger.println("query term not in the record, ignored... no document for this term: "
						+ term);
				continue;
			}
			
			//for each doc retrieved, we need calc score for each doc;
			
			// as said on ppt, when this doc is first time here(not in the set) , init the sorce as 0
			// for each doc, add value  K(term count in query)*idk*idk*C(term count in doc) to the score;
			// doc is presented as urls;	
			postings = ti.getPostings();
			for (String docUrl : postings.keySet()) {
				pd = dm.getDocumentByUrl(docUrl);
				if (pd == null) {
					Logger.warning(" while retrieving doc, this url point to no file: "
							+ docUrl);
					// no file , so , shall maybe update postings.
					continue;
				}
				
				if(!allRelatedDocSet.contains(docUrl)){
					//each related docs, keep a record;
					allRelatedDocSet.add(docUrl);
				}
				
				
				// increment score for related doc to this term;
				double incrementScore = ti.idf*ti.idf*termCount.get(term)*postings.get(docUrl);
				if (docScoreMap.containsKey(docUrl)) {
					// doc already inited;
					//update score for this term;
					score = docScoreMap.get(docUrl);
					score+=incrementScore;
					docScoreMap.put(docUrl, score);
					continue;
				}else{
					// first time for this doc;
					docScoreMap.put(docUrl, incrementScore);
				}
			}
		}
		
		Logger.println(" got dot product scores for each doc that related to the query terms ; next step is S/L*Y");
		
		//Let S be the current accumulated score of D;
//        (S is the dot-product of D and Q)
//   Let Y be the length of D as stored in its DocumentReference;
//   Normalize D’s final score to S/(L * Y);
		
//		L, query length;
//		Y , doc length;
		
		// for all the related docs, that know we have rank;
		
		//priorityqueue-> stack;
		//linkedlist->queue;
		
		Comparator<PDocument> docCmp = new Comparator<PDocument>(){
			@Override
			public int compare(PDocument pd1, PDocument pd2) {
				if(pd1.getFinalScore()>pd2.getFinalScore()){
					return -1;
				}else if(pd1.getFinalScore()<pd2.getFinalScore()){
					return 1;
				}
				return 0;
			}
		};
		
		if(allRelatedDocSet.size()==0){
			return null;
		}
		
		// create a priorityqueue of size docCount;
		PriorityQueue<PDocument> resDocList = new PriorityQueue<PDocument>(allRelatedDocSet.size(),docCmp);
		double fScore; //final score;
		for(String docUrl:allRelatedDocSet){
			pd = dm.getDocumentByUrl(docUrl);
			if(docScoreMap.containsKey(docUrl)){
				fScore = docScoreMap.get(docUrl);
			}else{
				fScore = 0;
			}
			// S/L*Y;
			fScore/=lengthOfQuery;
			fScore/=pd.getLength();
			pd.setFinalScore(fScore);
			//added to the queue, and ordered
			resDocList.add(pd);
		}
		
		return resDocList;
	}

}
