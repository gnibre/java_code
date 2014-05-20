package manage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import data.PDocument;
import data.TokenItem;

import util.Logger;
import util.PorterStem;
import util.StopWords;

/**
 * terms, also named tokens in the ppt;
 * this class helps parse tokens from content, and manage token items.
 * 
 * give up trying to use apache Lucene TokenStream package;.. just up java
 * StringTokenizer , to keep it simple and fool.
 * 
 *  when document pass it's content for get terms.( for save terms list in that page)
 * we also get update info for each term.
 * 
 *  we don't save any info to this class; this class only keep links to all the token/terms
 *  all the related data are in TokenItem; 
 * 
 * 
 *  * term Dictionary :  each term total freq count, 
 * 						each term total file count; in the manager;
 * 
 * 					each term, in certain doc, 
 * posting file,  each term, list of doclist+freq in that doc;
 * 
 * 
 * 
 * @author gnibrE
 */
public class TermsManager {

	
	private static TermsManager sTerms;
	private HashMap<String,TokenItem> allTokens = new HashMap<String,TokenItem>();

	private Set<String> stopSet;
	private PorterStem ps;

	public static synchronized TermsManager getTermManagerInstance() {
		if (sTerms == null) {
			sTerms = new TermsManager();
			sTerms.loadSavedData();
		}
		return sTerms;
	}

	public TermsManager() {
		stopSet = StopWords.stopSet;
		ps = new PorterStem();
	}

	/**
	 * 
	 * //http://pages.cs.wisc.edu/~hasti/cs302/examples/Parsing/parseString.html
	 * // stringTokenizer, string.split is strong if you know how to use
	 * 
	 * stop word is done by just remove words...
	 * 
	 * stem uses popular online version, PorterStem;
	 * 
	 * @param input
	 * @return
	 */
	public ArrayList<String> tokenizeRemStopAndStem(String input) {

		String lower = input.toLowerCase();
		// this is kinda good enough for this one i guess..
		StringTokenizer st = new StringTokenizer(lower, "[ .'-,?!]+");

		ArrayList<String> sl = new ArrayList<String>();
		String tk;
		String res;
		int count = 0;
		while (st.hasMoreTokens()) {
			count++;
			tk = st.nextToken();

			if (stopSet.contains(tk)) {
				continue;
			}

			char[] charArray = tk.toCharArray();
			PorterStem ps = new PorterStem();
			ps.add(charArray, charArray.length);
			ps.stem();
			// this is final after stem token;
			res = ps.toString();

			if (!allTokens.containsKey(res)) {
				TokenItem t = new TokenItem(res);
				allTokens.put(res,t);
//				Logger.println(" new term: " + res + "    [" + tk);
			}
			
			// all and every term are added to this list; cause we need calc frequency; don't remove any and better keep the order;
			sl.add(res);
		}
		return sl;
	}
	
	/**
	 * result can be null;
	 * @param term
	 * @return
	 */
	public TokenItem getToken(String term){
		if(allTokens==null){
			allTokens = new HashMap<String,TokenItem>();
		}
		return allTokens.get(term);
	}
	
	public TokenItem addToken(String term){
		if(allTokens==null){
			allTokens = new HashMap<String,TokenItem>();
		}
		TokenItem t = new TokenItem(term);
		allTokens.put(term,t);
		return t;
	}

	public String tops(){
		int maxT = 0;
		int maxF = 0;
		String tTerm = null;
		String fTerm = null;
		for(String t:allTokens.keySet()){
			int tf=  allTokens.get(t).totalFreq;
			if(tf>maxT&&t.length()>1){
				maxT = tf;
				tTerm = t;
			}
			int f= allTokens.get(t).totalFilesContainsThisToken;
			if(f>maxF){
				maxF = f;
				fTerm = t;
			}
		}
		
		
		Logger.println(" --------------------------------------------------");
		Logger.println(" term that apear most times: "+tTerm+"   time: "+maxT);
		Logger.println(" term that apear in most docs:"+fTerm+"   time: "+maxF);
		
		String res = " term that apear most times: ["+tTerm+"] count: "+maxT+"  ";
		
		int size = 10;
		String[] tp3 = new String[size];
		int ind = 0;
		int threshold = maxT/4;
		
		res+=" recommended search string to go: ";
		
		
		for(String t:allTokens.keySet()){
			if(t.length()>2&&allTokens.get(t).totalFreq>threshold){
				tp3[ind++] = t;
				res+="   "+t;
				Logger.println("  "+ind+":   "+t+"  time: "+allTokens.get(t).totalFreq);
			}
			if(ind==size) break;
		}
		return res;
	}
	
	public void info(){
		Logger.println("info in TermsManager;   total terms found: "+allTokens.size());
	}
	
	public String getStatusString(){
		String ret;
		ret = "Terms/Tokens already loaded to the search engine : "+allTokens.size();
		
		
		ret+=tops();
		Logger.println("terms manager get status: "+ret );
		return ret;
	}
	
	private boolean creating = false;
	private final String TERM_DATA_NAME = "term_st";
	public synchronized void storeToDisk(){
//		Map<String, PDocument> urlToDocMap = new java.util.concurrent.ConcurrentHashMap<String, PDocument>();
		// the map is the most important thing;
		Logger.println(" save---  term size: "+allTokens.size());
		StorageManager.saveObject(allTokens, TERM_DATA_NAME);
	}
	
	public synchronized void loadSavedData(){
		creating = true;
		ObjectInputStream ois = StorageManager.getObject(TERM_DATA_NAME);
		if(ois==null){
			Logger.println(" read file faild; "+TERM_DATA_NAME);
			return;
		}
		try {
			allTokens = (HashMap<String,TokenItem>)ois.readObject();
			Logger.println(" read file success; "+TERM_DATA_NAME);
			Logger.println(" retrieved term count: "+allTokens.size());
			
//			TokenItem ti = allTokens.get("CNN");
//			if(ti!=null)
//				System.out.println("  sample: "+ti.toString());
//			ti = allTokens.get("cnn");
//			if(ti!=null)
//				System.out.println("  sample: "+ti.toString());
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		creating = false;
	}
	
}
