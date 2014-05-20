package manage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Date;
import java.util.Map;

import data.PDocument;

import util.Logger;


/**
 * maintain all the documents that downloaded and parsed;
 * save infos so we don't need to scan again;
 * 
 * @author gnibrE
 *
 */
public class DocumentsManager {
	
	Date d;
	Map<String, PDocument> urlToDocMap = new java.util.concurrent.ConcurrentHashMap<String, PDocument>();
	Map<String, String> idToUrlMap = new java.util.concurrent.ConcurrentHashMap<String, String>();

	private static DocumentsManager sDocumentsManager;
	private static boolean creating = false;
	public static synchronized DocumentsManager getDocumentsManager(){
		if(sDocumentsManager==null){
			sDocumentsManager = new DocumentsManager();
			sDocumentsManager.d = new Date();
			sDocumentsManager.loadSavedData();
		}
		return sDocumentsManager; 
	}
	
	public PDocument getDocumentByUrl(String url){
		if(urlToDocMap.containsKey(url)){
			return urlToDocMap.get(url);
		}
		return null;
	}
	
//	public PDocument getDocumentById(String id){
//		if(idToUrlMap.containsKey(id)){
//			return getDocumentByUrl(idToUrlMap.get(id));
//		}
//		return null;
//	}
	
	public void saveNewDocument(PDocument pd){
		idToUrlMap.put(pd.id, pd.url);
		urlToDocMap.put(pd.url, pd);
		
		// scenario is we do not delete files.., the save function is too simple... 
		if(urlToDocMap.size()%50==0){
			storeToDisk();
			TermsManager.getTermManagerInstance().storeToDisk();
			CrawlerManager.getClawerManager().storeToDisk();
		}
	}
	
	
	public int getTotalDocCount(){
		return urlToDocMap.size();
	}
	
	public void info(){
		Logger.println("DocumentsManager info,  doc added: "+urlToDocMap.size());
	}
	
	public String getStatusString(){
		String ret;
		
		
		
		ret = "Documents/Pages already loaded to the search engine : "+urlToDocMap.size();
		String timeCost = "";
		if(d!=null&&d.getTime()>0){
			long cur = System.currentTimeMillis();
			long passed = (cur-d.getTime());
			if(urlToDocMap.size()>0){
				long avg = passed/urlToDocMap.size();
				timeCost = "   ,Average time each doc: "+avg+"ms";
			}
		}
		ret+=timeCost;
		Logger.println("document manager get status: "+ret);
		return ret;
	}
	
	
	private final String DOC_DATA_NAME = "doc_st";
	public synchronized void storeToDisk(){
//		Map<String, PDocument> urlToDocMap = new java.util.concurrent.ConcurrentHashMap<String, PDocument>();
		// the map is the most important thing;
		Logger.println(" save---  doc size: "+urlToDocMap.size());
		StorageManager.saveObject(urlToDocMap, DOC_DATA_NAME);
	}
	
	public synchronized void loadSavedData(){
		creating = true;
		ObjectInputStream ois = StorageManager.getObject(DOC_DATA_NAME);
		if(ois==null){
			System.out.println(" read file faild; "+DOC_DATA_NAME);
			return;
		}
		try {
			urlToDocMap = (Map<String, PDocument>)ois.readObject();
			Logger.println(" read file success; "+DOC_DATA_NAME);
			Logger.println(" retrieved doc count: "+urlToDocMap.size());
			
//			int index = 0;
//			for(String sample:urlToDocMap.keySet()){
//				if(index++>20){
//					break;
//				}
//				System.out.println(" saved "+index+"  :"+sample );
//				PDocument pd = urlToDocMap.get(sample);
//				if(pd!=null)
//					System.out.println("  sample: "+pd.toString());
//			}
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		creating = false;
	}
}
