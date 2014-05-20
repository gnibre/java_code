package data;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import manage.DocumentsManager;

import util.Logger;


/**
 * 
 * class for each single term
 * and informations that go with this term only;
 * 
 * 
 * 
 * term Dictionary : 
 * 
 *  1 each term total freq count, in the TermsManager, have link to all the terms/tokens;
 * 
 * 	2 each term total file count(files that contain this term); in the manager,have link to all the terms/tokens;;
 * 						 used to calc idf;
 *  
 * 	3 each term, in certain doc, freq
 * 							:this is TF,
 * 
 *  
 * posting file:
 * 
 *   each term, list of doclist+freq in that doc;
 * 
 * 
 * IDF, it goes with term;
 * idf = log(N/nk)  N is document count in the system;
 *                  nk is document count that contains this term;
 * 
 * TF*IDF is used for term weight;
 *
 * 
 * from PPT;
 * it's said that, better save a list of doc-ids where this term appear.
 *  save doc id, and also freq in that one ;
 *  -------- my idea is to save ids only, and find term/token links in that doc(PDocument);
 *  
 * save idf of cause
 * 
 * @author gnibrE
 *
 */
public class TokenItem implements Serializable{
	String term;
	public int totalFreq = 0;
	public int totalFilesContainsThisToken =0;
	
	// for each doc that contain this token, save doc id and token freq in this doc;
	// doc is presented with url;
	HashMap<String,Integer> docToFreqMap = new HashMap<String,Integer>();
	
	// not gonna use this , just use docToFreqMap instead; 
   //	ArrayList<PostingFormatWithDocAndFreq> postings;
	
	// don't know if we need an ordered list for docs, maybe hashmap is just enough; 
	
	public TokenItem(String t){
		term = t;
	}
	

	/**
	 * this term is found in new page/doc/url;  
	 * update total freq, update total files , update posting;
	 * @param url
	 * @param freq
	 */
	public void updateTermFreqWhenFoundInNewDoc(String url,int freq){
		if(docToFreqMap.containsKey(url)){
			Logger.warning(" try to add freq calc res for the same file the second time, to a term: "+term+"      url:"+url);
			return;
		}
		docToFreqMap.put(url, freq);
		totalFreq+=freq;
		totalFilesContainsThisToken = docToFreqMap.size();
	}
	
	public HashMap<String,Integer> getPostings(){
		return docToFreqMap;
	}
	
	public int getTotalFreq(){
		return totalFreq;
	}
	
	public int getTotalDoc(){
		return totalFilesContainsThisToken;
	}
	
	
	public double idf;
	/**
	 * when the database changes/grows,  need to calc weight;
	 * 
	 * tf = term freq in doc, saved in :docToFreqMap , grows over time;
	 * N = total number of docs in C, grows over time
	 * nk = docs that contain this term :  docToFreqMap.size
	 * 
	 * IDF = log(N/nk)
	 */
	public void calcIDF(){
		try{
			DocumentsManager dm = DocumentsManager.getDocumentsManager();
			int N = dm.getTotalDocCount();
			int nk = docToFreqMap.size();
			idf = Math.log10((double)N*1.0d/nk);
		}catch(Exception e){
			e.printStackTrace();
			idf = 1;
		}
		Logger.println(" term  "+term+"   idf: "+idf);
	}
	
	
	public String toString(){
		String s = "term: "+term+"  totalFreq: "+totalFreq+"  related docs: "+totalFilesContainsThisToken;
		
//		String sample = "";
//		if(docToFreqMap==null){
//			sample = " doc->freq map for this term is null/";
//		}else{
//			int ind = 0;
//			for(String url:docToFreqMap.keySet()){
//				ind++;
//				sample+= ""+docToFreqMap.get(url)+"=>url : "+url;
//				if(ind>3) break;
//			}
//		}
		return s;
	}
	
	public void info(){
		Logger.println(toString());
	}
	
	
	private void writeObject(ObjectOutputStream o)
		    throws IOException {  
		    
		    o.writeObject(term);  
		    o.writeObject(totalFreq);
		    o.writeObject(totalFilesContainsThisToken);
		    o.writeObject(docToFreqMap);
		  }
		  
		  private void readObject(ObjectInputStream o)
		    throws IOException, ClassNotFoundException {
			  term = (String) o.readObject();  
			  totalFreq = (int) o.readObject();
			  totalFilesContainsThisToken = (int) o.readObject();
			  docToFreqMap = (HashMap<String,Integer>) o.readObject();
		  }
}
