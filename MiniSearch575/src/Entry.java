import manage.CrawlerManager;
import manage.QueryManager;
import manage.StorageManager;
import WebServer.MyWebServer;
import tests.JSoupTest;


public class Entry {

	public static void main(String[] args){
		
		CrawlerManager cm = CrawlerManager.getClawerManager();
		cm.startClawerManager();
		
		//start simple web server.  
		// use http://localhost:80 to visit;
		MyWebServer mws = new MyWebServer();
		mws.startMywebServer();
		
	}
	
}
