package manage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import data.PDocument;
import data.crawler.CrawledPage;
import data.crawler.CrawlerJob;

import util.Common;
import util.Logger;

/**
 * manage process of doing the clawer job.
 * 
 * @author gnibrE
 * 
 */
public class CrawlerManager {

//	final String[] startCrawerUrls = { "http://cnn.com",
//			"http://www.foxnews.com/‎", "http://www.cnn.com",
//			"http://ycombinator.com", "http://abcnews.go.com/",
//			"http://www.nytimes.com/" };
	final String[] startCrawerUrls = {
			
//			"http://google.com",
			"http://ycombinator.com",
			"http://cnn.com",
			"http://www.wired.com/mp3/features002.mp3", // stucks at this one; this one is a mp3 file of size 52MB.
			"http://www.nytimes.com/",
			"http://travel.yahoo.com/"
	};

	StorageManager sm;

	// fifo queue for crawler job.
	// likedlist not guaranteed to be synchronized,
	LinkedList<CrawlerJob> jobList = new LinkedList<CrawlerJob>();
	
	// hashset to tell if this job already exist in the list; unique check;
	HashSet<String> urlJobSet = new HashSet<String>();

	public CrawlerManager() {
	}
	
	private static CrawlerManager sClawerManager;
	public static synchronized CrawlerManager getClawerManager(){
		if(sClawerManager==null){
			sClawerManager = new CrawlerManager();
			sClawerManager.loadSavedData();
		}
		return sClawerManager;
	}

	/**
	 * on start, things to be done:
	 * 
	 * 0, read status, know what's happening now. 1, read from saved file, see
	 * if everything is under control. 2, do crawler job if it's needed.
	 * 
	 * 
	 */
	public void startClawerManager() {

		Logger.println("startClawerManager" + new Date());
		// skip 0,1; start doing 2;
		initCraw();
		CrawThread ct = new CrawThread();
		new Thread(ct).start();

	}

	private void initCraw() {
		for (String url : startCrawerUrls) {
			CrawlerJob cj = new CrawlerJob(url, 0, null);
			addCrawlJob(cj);
		}
	}

	private int addCrawlJob(CrawlerJob cj) {
		if(urlJobSet.contains(cj.url)){
//			Logger.print("url-exist;");
			return 0;
		}
		jobList.add(cj);
		urlJobSet.add(cj.url);
		return 1;
	}

	private synchronized CrawlerJob getNextJob() {
		if (jobList != null) {
			return jobList.poll();
		}
		return null;
	}

	private class CrawThread implements Runnable {
		@Override
		public void run() {
			DocumentsManager dm = DocumentsManager.getDocumentsManager();
			int max = 112220;
			int c = 0;
			while (true&&c<max) {
				Logger.println("  loop : "+c);
				c++;
				CrawlerJob job = getNextJob();
				if (job == null) {
					// nothing in the list now. // cehck queued?
					Logger.println(" no job break");
					break;
				}
				// working on this.
				Logger.println(" got a job to be crawled: "+job.url);
				
				//remove it only when it is documented.
//				urlJobSet.remove(job.url);
				
				boolean runIt = shallJobBeRun(dm,job);
				if(!runIt){
					Logger.println("giveup crawl this job: "+job.url);
					continue;
				}

				// with url, we download the page and then do parse and go on
				// crawl.
				CrawledPage cp = new CrawledPage(job.url, job.depth,job.reference);
				
				if (!cp.getDoc) {
					// or maybe shall try later?
					Logger.println(" failed to download the page, giveup this url:" + job.url);
					continue;
				}

				int findLinks = cp.parseLinks();
				if (findLinks > 0) {
					Logger.println(" find "+findLinks+"  links to go on crawl; ");
					ArrayList<CrawlerJob> ja = cp.getCrawlJobArray();
					int addedCount = 0;
					if(ja!=null&&ja.size()>0){
						for(CrawlerJob cj:ja){
							addedCount+= addCrawlJob(cj);
						}
					}
					Logger.println("[[]] total links: "+findLinks+"  added: "+addedCount);
				}
				
				
				// this page is already downloaded. and links are all added to list for further crawl
				// parse the content, to get terms and invert index;
				cp.parseContent();
				
				cp.releaseAllResource();
				
			}//end of while loop for all jobs.
			
			Logger.println(" after break;  size still in the job list: "+jobList.size());
			Logger.println(" after break;  size still in the job list: "+urlJobSet.size());
			
			
			
			
			TermsManager.getTermManagerInstance().info();
			DocumentsManager.getDocumentsManager().info();
		
			//see what's on top;
			TermsManager.getTermManagerInstance().tops();
			
			// after this crawl thread is done, do test query
//			QueryManager qm = new QueryManager();
//			qm.doSample();
			
			
		}
	}
	
	/**
	 * for the webpage; 
	 * @return
	 */
	public String getStatusString(){
		String ret;
		
		if(jobList!=null&&jobList.size()>0){
			ret = "Crawler still crawling.... unfinished jobs: "+jobList.size();
		}else{
			ret = "Crawler Finished crawling!!! : ";
		}
		Logger.println("clawler get status: "+ret );
		return ret;
	}

	private boolean shallJobBeRun(DocumentsManager dm, CrawlerJob job) {
		// check if exist.
		PDocument dd = dm.getDocumentByUrl(job.url);
		if (dd != null) {
			System.out.println("=== URL SKIP CRAWL! EXIST!!!   "+job.url);
			// already exist. maybe we don't need to do further.
			long timePassed = dd.lastUpdate.getTime()
					- System.currentTimeMillis();
			Logger.println(" time passed since last update: " + timePassed);
			if (timePassed > Common.UPDATE_FOR_EXIST_DOC) {
				Logger.println(" update this job is needed");
				// for update frequence of term, we shall start with saved term freq/posting deleted;
				// or do that after we parsed the content;mark the content as update, and do freq fix; do that later;
				return false;
//				return true;
			}
			Logger.println("NOT , do not need to update this job");
			// don't need to update;
			return false;
		}
		return true;
	}
	
	
	
	private final String CRAWL_DATA_NAME = "crawl_st";
	private boolean creating = false;
	public synchronized void storeToDisk(){
//		Map<String, PDocument> urlToDocMap = new java.util.concurrent.ConcurrentHashMap<String, PDocument>();
		// the map is the most important thing;
		Logger.println(" save---  crawl jobList size: "+jobList.size());
		StorageManager.saveObject(jobList, CRAWL_DATA_NAME);
	}
//	LinkedList<CrawlerJob> jobList = new LinkedList<CrawlerJob>();
	public synchronized void loadSavedData(){
		creating = true;
		ObjectInputStream ois = StorageManager.getObject(CRAWL_DATA_NAME);
		if(ois==null){
			Logger.println(" read file faild; "+CRAWL_DATA_NAME);
			return;
		}
		try {
			jobList = (LinkedList<CrawlerJob>)ois.readObject();
			Logger.println(" read file success; "+CRAWL_DATA_NAME);
			Logger.println(" retrieved doc count: "+jobList.size());
			
//			int index = 0;
//			for(CrawlerJob sample:jobList){
//				if(index++>20){
//					break;
//				}
//				System.out.println(" saved "+index+"  :"+sample );
//			}
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		creating = false;
	}

}
