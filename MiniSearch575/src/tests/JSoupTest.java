package tests;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class JSoupTest {
	
	/**
	 * link to learn JSoup.  powerful select.
	 * 
	 * http://jsoup.org/cookbook/extracting-data/selector-syntax 
	 * 
	 */
	public void goJSoupTest(){
		Document doc;
		try {
	 
			String url = "http://www.google.com//intl/en/about.html";
			
//			url = "http://google.com";
			url = "http://en.wikipedia.org/wiki/Fruit";
			
			url = "http://www.foxnews.com/‎";
			url = "http://www.cnn.com";
			url = "http://ycombinator.com";
//			url = "http://www.wired.com/mp3/features002.mp3";
			// need http protocol
			doc = Jsoup.connect(url).get();
	 
			// get page title
			String title = doc.title();
			System.out.println("title : " + title);
	 
			
			System.out.println(" base uri :"+doc.baseUri());
			Element body = doc.body();
			System.out.println("text : " + body.text());
			
			// get all links
			Elements links = doc.select("a[href]");
			for (Element link : links) {
	 
				// get the value from href attribute
				System.out.println("\nlink : " + link.attr("href")+"            : "+link.baseUri()+link.attr("href"));
				
				
				
				System.out.println("text : " + link.text());
			}
	 
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
