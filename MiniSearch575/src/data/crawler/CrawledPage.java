package data.crawler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import manage.DocumentsManager;
import manage.TermsManager;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import data.PDocument;
import data.TokenItem;

import util.Common;
import util.Logger;

/**
 * page content is here. page crawl process is also here.
 * 
 * process
 * download the content,
 * 
 * based on the content, use Jsoup to parse content/links:
 * links for further crawl,
 * terms from content, for search, 
 * 
 * @author gnibrE
 */
public class CrawledPage {
	String url;
	int depth;
	String ref;
	String encoding;
	byte[] pageRawByte;
	TermsManager tm;
	String title;
//	public boolean downloadDone = false;
	public boolean getDoc = false;
	
	Document JSoupDoc;
	// save content til this page is parsed;
	String content;

	ArrayList<CrawlerJob> jobArray = new ArrayList<CrawlerJob>();
	
	public CrawledPage(String url,int d,String refs) {
		this.depth = d;
		this.url = url.toLowerCase();
		this.ref = refs;
		if (!url.startsWith(Common.HTTP_URL_PREFIX)) {
			// not valid.
			return;
		}
		tm = TermsManager.getTermManagerInstance();

		// check saved, then if not, download it.
		getJSoupDoc();
	}
	
	
	private void getJSoupDoc(){
		try {
			JSoupDoc = Jsoup.connect(url).get();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e){
			e.printStackTrace();
		}
		if(JSoupDoc==null){
			Logger.println(" failed to get jsoup doc. ");
			return;
		}
		 
		// get page title
//		String title = JSoupDoc.title();
//		System.out.println("title : " + title);
////		System.out.println(" base uri :"+doc.baseUri());
//		Element body = JSoupDoc.body();
//		System.out.println("body text : " + body.text());
		getDoc = true;
	}

	private void downloadPageContent() {
//		Logger.println("downloadPageContent , " + url);

		HttpClient httpclient = new DefaultHttpClient();

		HttpGet httpget = null;

		// Execute the request
		HttpResponse response = null;
		try {
			httpget = new HttpGet(url);
			response = httpclient.execute(httpget);
		} catch (ClientProtocolException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (Exception e){
			// do happen other exception like : url not valid; illegal char in url;
			// shall handle codecs
			e.printStackTrace();
		}

		if (response == null) {
			Logger.warning("failed download page: " + url+"   no response");
			return;
		}

		// Examine the response status
		StatusLine statusLine = response.getStatusLine();
		if (statusLine.getStatusCode() != Common.HTTP_RESPONSE_OK) {
			Logger.warning("failed download page: " + url+"   code not 200:  "+statusLine.getStatusCode());
			return;
		}

		// Get hold of the response entity
		HttpEntity entity = response.getEntity();

		// If the response does not enclose an entity, there is no need
		// to worry about connection release
		if (entity != null) {
			Header encodingHeader = entity.getContentEncoding();
			encoding = Common.DEFAULT_ENCODING;

			if (encodingHeader != null) {
				encoding = encodingHeader.getValue();
			}

			InputStream is = null;
//			ArrayList<byte[]> bufArray = new ArrayList<byte[]>();
			// this buffs all the byte array of unknown length;
			ByteArrayOutputStream bao;
			
			//10k
			int bufSize = 10240;
			int total = 0;
			int avai = 0;
			try {
				is = entity.getContent();
//				Logger.println("availabe is said to be: "+is.available());
				// download size of buf each time.
				byte[] buf = new byte[bufSize];
				int read;
				int lastSize = 0;
				 bao = new ByteArrayOutputStream();
				while ((read = is.read(buf)) > 0) {
					total+=read;
//					Logger.println(" got contnet of 100k : " + read);
//					bufArray.add(buf);
//					if (read != bufSize) {
//						lastSize = read;
//						Logger.println("last block size: : " + lastSize);
//					}
					bao.write(buf, 0, read);
					buf = new byte[bufSize];
				}

//				if (bufArray.size() == 0) {
//					Logger.warning(" don't get data from url : " + url);
//				}

				// combine;
//				int totalSize = bufSize * (bufArray.size() - 1) + lastSize;
//				Logger.println("totalSize of this page: : " + totalSize);
//				pageRawByte = new byte[totalSize];
//
//				int offset;
//				for (int i = 0; i < bufArray.size() - 1; ++i) {
//					offset = bufSize * i;
//					byte[] bf = bufArray.get(i);
//					for (int j = 0; j < bufSize; ++j) {
//						pageRawByte[offset + j] = bf[j];
//					}
//				}
//				// last block;
//				offset = bufSize * (bufArray.size() - 1);
//				byte[] bf = bufArray.get(bufArray.size() - 1);
//				for (int j = 0; j < lastSize; ++j) {
//					pageRawByte[offset + j] = bf[j];
//				}
				
//				Logger.println("  total : "+total+"   avail said: "+avai);
				pageRawByte = bao.toByteArray();
				Logger.println("  res size:"+pageRawByte.length+"    same as total? : "+total);
				
				httpclient.getConnectionManager().shutdown();
				
				Logger.println("crawl  " + url + "  WELL DONE");
				
//				getBody
//				downloadDone = true;
			} catch (RuntimeException | IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (is != null) {
						is.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		} else {
			Logger.println("doHttpCrawl " + url + " entity null");
		}

	}

	
	/**
	 * find alll the links, package them in the ArrayList of Jobs;
	 * 
	 */
	public int parseLinks(){
//		String pageString = null;;
//		try {
//			pageString = new String(pageRawByte,encoding);
//			Logger.println(" pagestring length: "+pageString.length()+"   byte length: "+pageRawByte.length+"  , /8 is :"+(pageRawByte.length/8));
//		} catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
//		}
//		if(pageString==null) return -1;
//		Document doc = Jsoup.parse(pageString.toLowerCase());
		if(JSoupDoc==null){
			Logger.warning(" parse page with Jsoup failed.. content valid maybe valid :");
//			Logger.println(pageString);
			return -2;
		}
		//text if for terms.
		content = JSoupDoc.text();
		
		Logger.println("got text of this page: the page has size:  "+content.length());
//		Logger.println(content);
		
		
		title = url;
		Elements titleElements = JSoupDoc.getElementsByTag("title");
		if (titleElements != null) {
			title = titleElements.text();
		}
		Logger.println(" got title :"+title);
		
		Elements links = JSoupDoc.getElementsByTag("a");
		Logger.println("  found links count in this page: "+links.size());
		
		DocumentsManager dm = DocumentsManager.getDocumentsManager();
		
		jobArray = new ArrayList<CrawlerJob>();
		int count = 0;
		for(Element elink:links){
			count++;
			String linkHref = elink.attr("href");
			String linkText = elink.text();
//			Logger.println(""+count+",found new link : " + linkHref );
//			Logger.println("linktext for this link:  "+linkText);

			if(!linkHref.startsWith(Common.HTTP_URL_PREFIX)){
//				Logger.println(" not valid url,  : "+linkHref);
			}else{
				CrawlerJob cj = new CrawlerJob(linkHref,depth+1,url);
				jobArray.add(cj);
			}
			
		}
		// all link pages added to jobArray;
		// can be runned if we need dig further.
		return jobArray.size();
	}
	
	/**
	 * parse content of this page, 
	 * after parse, the page object will be deleted, to save memory;
	 * 
	 * parsed content that will work later for search are saved in PDocument and Each Token;
	 */
	public void parseContent(){
		DocumentsManager dm = DocumentsManager.getDocumentsManager();
		PDocument savedDoc = dm.getDocumentByUrl(url);
		List<String> termList = tm.tokenizeRemStopAndStem(content);
		if(savedDoc!=null){
			// we got new update for you
			savedDoc.setTerms(termList);
			savedDoc.setTitle(title);
			savedDoc.depth = this.depth;
			savedDoc.referenceUrl = ref;
			savedDoc.updated();
			dm.saveNewDocument(savedDoc);
		}else{
			PDocument pageDoc = new PDocument(url,title,depth,ref);
			dm.saveNewDocument(pageDoc);
		}
		
		calculateTermFreq(url,termList,title);
	}
	
	/**
	 * calc freq
	 * will work later for search are saved in PDocument and Each Token;
	 * 
	 * added title weight,( 10 times terms weight for each term)
	 * 
	 * @param url
	 */
	private void calculateTermFreq(String url,List<String> termList,String title){
		Logger.println("calculateTermFreq for this page ");
		// the freq count is only in this file(url);
		Map<String, Integer> mapOfTermToFreq = new HashMap<String, Integer>();
		for (String termStr : termList)
		{
			// total in this doc;
			Integer totalFreq = mapOfTermToFreq.get(termStr);
			if (totalFreq == null)
			{
				totalFreq = 0;
			}
			totalFreq = totalFreq + 1;
			mapOfTermToFreq.put(termStr, totalFreq);
		}
		
		ArrayList<String> titleArray = TermsManager.getTermManagerInstance().tokenizeRemStopAndStem(title);
		for (String titleTerm : termList)
		{
			// total in this doc;
			Integer totalFreq = mapOfTermToFreq.get(titleTerm);
			if (totalFreq == null)
			{
				totalFreq = 0;
			}
			// title weight is simply set to 10;
			totalFreq = totalFreq + Common.TITLE_WEIGHT;
			mapOfTermToFreq.put(titleTerm, totalFreq);
		}
		
		//for each terms, get length of the document;
		
		double lengthPowerSum = 0;
		
		// get sums; for each token;
		TokenItem ti;
		int freq ;
		for(String termStr: mapOfTermToFreq.keySet()){
			ti = tm.getToken(termStr);
			if(ti==null){
				Logger.warning(" this term not added to map : "+termStr);
				ti = tm.addToken(termStr);
			}
			freq = mapOfTermToFreq.get(termStr);
			
			lengthPowerSum+=freq*freq;
			
			
			/// everything needed in this file is saved to TokenItem
			ti.updateTermFreqWhenFoundInNewDoc(url, freq);
			// calc freq length of the page; for similarity calc;
		}
		
		//final length of this page;
		double lenghtOfPage = Math.sqrt(lengthPowerSum);
		DocumentsManager dm = DocumentsManager.getDocumentsManager();
		PDocument doc = dm.getDocumentByUrl(url);
		doc.setLength(lenghtOfPage);
		
	}
	
	
	public ArrayList<CrawlerJob> getCrawlJobArray(){
		return jobArray;
	}
	
	
	/**
	 * this crawled page is just to help search; after we got record, delete everything we can to release memory;
	 */
	public void releaseAllResource(){
		Logger.println("releaseAllResource");
		jobArray = null;
		pageRawByte = null;
		content = null;
	}
	
}
