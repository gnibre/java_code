package data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import util.Logger;

/**
 * name it PDocument as for document of a page; to be different from Jsoup.document;
 * 
 * will save all the infos that needed for search
 * shall never save content of the web page;
 * @author gnibrE
 *
 *must be serialized to so to right object;
 */
public class PDocument implements Serializable{

	
	@Override
	public String toString() {
		
		String s = "Document: [id=" + id + ", depth=" + depth + ", referenceUrl=" + referenceUrl + ", url=" + url
				+ ", createAt=" + createAt + ", lastUpdate=" + lastUpdate + "]";
		if(terms==null){
			s+=", terms= null";
		}else{
			s+=", terms=" + terms.size();
		}
		return s;
	}
	
	// use uuid, to make the doc id easier to define;
	public String id;
	public String title;
	public int depth;
	public String referenceUrl;
//	Integer httpStatusCode;
	public String url;
	
	// terms that tokenized, removed stop words, stemed
	List<String> terms;
	public Date createAt;
	public Date lastUpdate;
	
	// score for ranking.. this value is only used for a specific query term;
	// this value is used to order the documents
	// say it again, this value don't go with this document; for a new query/term, shall never reuse the score that saved before
	double finalScore;
	private double length;
	
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	
	public void setTerms(List<String> tl){
		terms = tl;
	}
	
	public void updated(){
		lastUpdate = new Date();
	}
	
	public PDocument(String sUrl,String sTitle,int d,String ref){
		url = sUrl;
		title = sTitle;
		depth = d;
		// oncreate;
		createAt = new Date();
		lastUpdate = new Date();
		id = UUID.randomUUID().toString();
	}
	
	public void resetScore(){
		finalScore = 0;
	}
	
	public void info(){
		Logger.println("  "+url+"      title: "+title);
	}
	
	public void setLength(double l){
		length = l;
	}
	// get doc length;
	public double getLength(){
		return length;
	}
	
	
	public void setFinalScore(double d){
		finalScore = d;
	}
	public double getFinalScore(){
		return finalScore;
	}
}
