package data.crawler;

import java.io.Serializable;
import java.util.Date;

import util.Logger;

/**
 * each document is from a search job of a url.
 * @author gnibrE
 *
 */
public class CrawlerJob implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 6940406465321209039L;
	@Override
	public String toString() {
		return "CrawlerJob [depth=" + depth + ", reference=" + reference + ", url=" + url + ", createTime=" + createTime + "]";
	}
	
	/**
	 * 
	 * @param d  depth of the crawler job in a bfs tree.
	 * @param url   url of page/doc
	 * @param ref  the parent, where this url is from; it's found in the ref page;
	 */
	public CrawlerJob(String u,int d,String ref){
		depth = d;
		url = u;
		reference = ref;
		createTime = new Date();
//		Logger.println("new crawler job created: "+depth+"|"+createTime+"       => "+url);
	}
	
	public int depth;
	public String reference;
	public String url;
	Date createTime;
}
