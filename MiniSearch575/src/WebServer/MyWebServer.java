package WebServer;

import java.io.*; // Get the Input Output libraries 
import java.net.*; // Get the Java networking libraries
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.PriorityQueue;

import manage.CrawlerManager;
import manage.DocumentsManager;
import manage.QueryManager;
import manage.TermsManager;

import data.PDocument;


import util.Logger;

/**
 * this file actually was not created for this IR project, but it's good for
 * this project; the author of this file is also me, so I think it's ok to use
 * it here;;
 * 
 * running function startMyWebServer will build a simple webserver on the machine that can run java so
 * we don't need tools like Tomcat, don't need to setup, don't need to download libraries to build a web server quickly;
 * 
 * when http://localhost:80 is visited by a browser (tested IE and Chrome), 
 * return page will be dynamically generated by the program, and returned to the browser 
 * using generateDynamicSearchResult and generateWelcomePage function;
 * 
 * @author gnibrE
 * 
 */
class Worker extends Thread { // Class definition
	Socket sock; // Class member, socket, local to Worker.

	Worker(Socket s) {
		sock = s;
	} // Constructor, assign arg s to local sock

	public void run() {
		// Get I/O streams in/out from the socket:
		PrintStream out = null;
		BufferedReader in = null;
		try {
			in = new BufferedReader(
					new InputStreamReader(sock.getInputStream()));
			out = new PrintStream(sock.getOutputStream());
			// Note that this branch might not execute when expected:
			if (MyWebServer.controlSwitch != true) {
				System.out
						.println("Listener is now shutting down as per client request.");
				out.println("Server is now shutting down. Goodbye!");
			} else
				try {
					String input;
					input = in.readLine();
					Logger.println(" got input from web client: "+input);
					if(input==null){
						return;
					}
					// get commond shutdown from user input, set controlswitch
					// to false, server will shut down.
					if (input.equals("shutdown")) {
						MyWebServer.controlSwitch = false;
						System.out
								.println("Worker has captured a shutdown request.");
						out.println("Shutdown request has been noted by worker.");
						out.println("Please send final shutdown request to listener.");
					} else {
						// / try look up this address, if the commond is not
						// shutdown.

						// System.out.println("Looking up(it's the name from the client) : "
						// + name);
						// printRemoteAddress(input, out);

						HttpRequestResponseParser rp = new HttpRequestResponseParser();

						while (in.ready()) {
							writeToLogLineByLine(input);
							rp.newRequestLine(input);
							input = in.readLine();
						}
						writeToLogLineByLine("<------------------------------------------------------END OF LAST REQUEST");
						String response = rp.makeResponseToClient();
						out.append(response);
						out.flush();

					}
				} catch (IOException x) {
					System.out.println("Server read error");
					x.printStackTrace();
				}
			sock.close(); // close this connection, but not the server;
		} catch (IOException ioe) {
			System.out.println(ioe);
		}
	}

	// **sample response from server:

	// HTTP/1.1 200 OK
	// Date: Mon, 17 Feb 2014 02:12:40 GMT
	// Server: Apache/2.2.3 (Red Hat)
	// Last-Modified: Fri, 16 Sep 2005 18:08:50 GMT
	// ETag: "1497b-2f-400e77c517080"
	// Accept-Ranges: bytes
	// Content-Length: 47
	// Content-Type: text/plain
	// Connection: close
	//
	// This is Elliott's dog file on condor and hawk.

	private class MIMETYPE {
		public static final String TYPE_TEXT_PLAIN = "text/plain";
		public static final String TYPE_TEXT_HTML = "text/html";
	}

	/**
	 * server got reuqest, line by line, wanna know what this request wanna do;
	 * 
	 * @author Yubing
	 */
	private class HttpRequestResponseParser {
		String filePath;
		File contentFile;
		String status;
		String contentType = MIMETYPE.TYPE_TEXT_HTML;
		final String FILE_NOT_FOUND_NAME = "404nf.html";
		final String CGI_CALC_RES = "cgi_res.html";
		final String DIR_VIEW = "dir.html";
		final String SEARCH_VIEW = "query_res.html";
		final String WELCOME_VIEW = "index.html";
		private boolean cgi = false;
		String person;
		int n1;
		int n2;

		String rawQuery;
		ArrayList<String> queryTerms  = new ArrayList<String>();
		// request coming line by line;
		public void newRequestLine(String line) {
			// sample request:
			// GET /cat.html HTTP/1.1
			// Host: localhost:2540
			// User-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64; rv:27.0)
			// Gecko/20100101 Firefox/27.0
			// Accept:
			// text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
			// Accept-Language: en-US,en;q=0.5
			// Accept-Encoding: gzip, deflate
			// Connection: keep-alive

			// or maybe GET / HTTP/1.1
			if (line.startsWith("GET")) {
				String[] split = line.split(" ");
				filePath = split[1];
				System.out.println(" get request file path:  " + filePath);

				// take this path as query keyword?
				if(filePath.contains("query=")){
					int index = filePath.indexOf("query=");
					rawQuery = filePath.substring(index+6);
					Logger.println("rawQuery is : "+rawQuery);
					
					TermsManager tm = TermsManager.getTermManagerInstance();
					queryTerms = tm.tokenizeRemStopAndStem(rawQuery);
					for(String t:queryTerms){
						Logger.println(t);
					}
					
					System.out.println(" ==========print this array[ tokens in query];      size : "+queryTerms.size());
					for(int i=0;i<queryTerms.size();++i){
						System.out.println(" i: "+i+"  "+queryTerms.get(i));
					}
					
				}else{
					Logger.println("failed to get user query : "+filePath);
					queryTerms = null;
				}
			}

		}

		private void getRequestStatus() {

			System.out.println(" file paht: " + filePath);

			contentFile = null;
			
			if(queryTerms!=null){
				
				generateDynamicSearchResult();
				contentFile = new File(SEARCH_VIEW);
			}else{
				generateWelcomePage();
				contentFile = new File(WELCOME_VIEW);
			}
			status = "200 OK";
			
			
//			generateDynamicSearchResult(filePath);
//			contentFile = new File(DIR_VIEW);
//			System.out.println("file not ok. no file return;");
//			generateDynamicNotFoundFile(filePath);
//			filePath = FILE_NOT_FOUND_NAME;
//			status = "404 Found";
//			contentFile = new File(filePath);
//
//			System.out
//					.println(" result file to show: " + contentFile.getName());

		}

		public String makeResponseToClient() {
			if (filePath == null) {
				// not inited; not good request;
				return null;
			}

			// System.out.println(" start with: "+filePath.charAt(0));
			// System.out.println("  file separator "+File.separator);
			// System.out.println("  file pathSeparatorChar "+File.pathSeparatorChar);
			// System.out.println("  file pathSeparatorChar "+File.pathSeparator);

			// this function will decide what content to return to the client;
			getRequestStatus();

			System.out.println(" file to show: " + contentFile.getName() + "  "
					+ contentFile.getAbsolutePath());

			String res = makeResponse(contentFile, status);
			System.out.println(" response to client is : ");
			System.out.println(res);
			return res;
		}

		private String makeResponse(File f, String status) {

			// HTTP/1.1 200 OK
			// Date: Mon, 17 Feb 2014 02:13:16 GMT
			// Server: Apache/2.2.3 (Red Hat)
			// Last-Modified: Mon, 05 Oct 2009 20:35:03 GMT
			// ETag: "19fe2-78-475360c5dcbc0"
			// Accept-Ranges: bytes
			// Content-Length: 120
			// Content-Type: text/html
			// Connection: close
			//

			// HTTP/1.1 302 Found
			// Date: Mon, 17 Feb 2014 02:06:06 GMT
			// Server: Apache/2.2.3 (Red Hat)
			// Location: http://www.depaul.edu
			// Content-Length: 205
			// Connection: close
			// Content-Type: text/html; charset=iso-8859-1
			//
			// <!DOCTYPE HTML PUBLIC "-//IETF//DTD HTML 2.0//EN">
			// <html><head>
			// <title>302 Found</title>
			// </head><body>
			// <h1>Found</h1>
			// <p>The document has moved <a
			// href="http://www.depaul.edu">here</a>.</p>
			// </body></html>

			if (f != null && f.exists()) {
				String name = f.getName();
				name = name.toLowerCase();
				if (name.endsWith("txt")) {
					contentType = MIMETYPE.TYPE_TEXT_PLAIN;
				} else if (name.endsWith("html") || name.endsWith("htm")) {
					contentType = MIMETYPE.TYPE_TEXT_HTML;
				}
			}

			StringBuilder sb = new StringBuilder();
			sb.append("HTTP/1.1 ");
			sb.append(status);
			sb.append("\r\n");
			sb.append("Date: ");
			Date d = new Date();
			sb.append(d.toGMTString());
			sb.append("\r\n");
			sb.append("Server: MYWEBSERVER");
			sb.append("\r\n");
			Date md = new Date(f.lastModified());
			sb.append("Last-Modified: ");
			sb.append(md.toGMTString());
			sb.append("\r\n");
			sb.append("Location: localhost");
			sb.append("\r\n");
			sb.append("Content-Length: " + f.length());
			sb.append("\r\n");
			sb.append("Content-Type: ");
			sb.append(contentType);
			sb.append("; charset=iso-8859-1");
			sb.append("\r\n");
			sb.append("Connection: close");
			sb.append("\r\n");

			sb.append("\r\n");
			sb.append("\r\n");
			String fileContent = readFile(f);
			sb.append(fileContent);
			return sb.toString();
		}

//		private void generateDynamicDirView(String path) {
//			System.out
//					.println("==================== try to create dynamic dir view for path: "
//							+ path);
//			File here0 = new File("2");
//			System.out.println(" server dir is always root???  : "
//					+ here0.getAbsolutePath());
//
//			File dirf = new File(DIR_VIEW);
//			File d = new File(path);
//
//			File here = new File("");
//			System.out.println(" server dir is always root???  : "
//					+ here.getAbsolutePath());
//
//			if (path == null || path.length() == 0) {
//				path = ".";
//			}
//
//			if (!path.equals(".") && !path.endsWith("/")) {
//				path = path + "/";
//			}
//
//			System.out.println("   formatted path : " + path);
//
//			File[] flist = d.listFiles();
//			// <h1>Index of /~elliott/435/.xyz</h1>
//			// <a href="/~elliott/435/">Parent Directory</a> <br>
//			File dummy = new File(path + "."); // dummy file in the dir, to get
//												// herf link base to this path;
//			dummy.exists();
//			File dummy2 = new File(path);
//			dummy2.exists();
//			dummy.listFiles();
//
//			String subp = path.substring(0, path.length() - 1);
//			int indLast = path.lastIndexOf("/");
//			String parentPath;
//			if (indLast < 0) {
//				parentPath = "/";
//			} else {
//				parentPath = subp.substring(0, indLast);
//			}
//
//			System.out.println("   result parentPath: " + parentPath);
//
//			String content = "Index of " + path;
//			try {
//				FileWriter fw = new FileWriter(dirf);
//				fw.append("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">");
//				fw.append("\r\n");
//				fw.append("<html><head>");
//				fw.append("\r\n");
//				fw.append("<title>DIR VIEW</title>");
//				fw.append("\r\n");
//				fw.append("</head><body>");
//				fw.append("\r\n");
//				fw.append("<h1>" + "" + content + "</h1>");
//				fw.append("\r\n");
//
//				if (!path.equals(".")) {
//					// not root, can have p;
//					// String link = "<a href=\""+parentPath+"\"> .. </a> <br>";
//					String link = "<a href=\"..\"> .. </a> <br>";
//					fw.append(link);
//					fw.append("\r\n");
//				}
//
//				// String prep = path+"/";
//				// if(path.equals(".")){
//				// prep ="";
//				// }
//				// System.out.println(" prep: "+prep);
//
//				// ////////////////////////relative links , are from the
//				// "CURRENT FILE", what the current file? it's where DIR_VIEW
//				// is;
//				// i received request, i controlled what to return; but why i
//				// can't control "current" file location....
//				// when user input site:port/1/2/3/4 , the cur is
//				// server_home/1/2/3/ parent of the input;
//				// when user input site:port/1/2/3/4/ the cur is
//				// server_home/1/2/3/4/ correctly
//				// it's controlled by the url user input; can't control that;
//
//				if (flist != null) {
//
//					// dirs first;
//					for (int i = 0; i < flist.length; ++i) {
//						if (!flist[i].isDirectory())
//							continue;
//						// String link =
//						// "<a href=\""+prep+flist[i].getName()+"\">"+flist[i].getName()+" </a> <br>";
//						// link, related;
//						// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!! we must add a / at the
//						// end;
//						String link = "<a href=\"" + flist[i].getName()
//								+ "/\">" + flist[i].getName() + "/ </a> <br>";
//						fw.append(link);
//						System.out.println(" link is : " + link);
//						fw.append("\r\n");
//					}
//
//					// files;
//					for (int i = 0; i < flist.length; ++i) {
//						if (flist[i].isDirectory())
//							continue;
//						String link = "<a href=\"" + flist[i].getName() + "\">"
//								+ flist[i].getName() + " </a> <br>";
//						fw.append(link);
//						System.out.println(" link is : " + link);
//						fw.append("\r\n");
//					}
//				}
//
//				fw.flush();
//				fw.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}

		private void generateDynamicSearchResult() {
			
			
			long start = System.currentTimeMillis();
			QueryManager qm = QueryManager.getInstance();
			PriorityQueue<PDocument> dPQueue = qm.query(queryTerms);
			
			long done = System.currentTimeMillis();
			
			int resSize = 0;
			if(dPQueue==null||dPQueue.size()==0){
				resSize = 0;
			}else{
				resSize = dPQueue.size();
			}
			
			
			File searchRes = new File(SEARCH_VIEW);
			try {

				FileWriter fw = new FileWriter(searchRes);
				fw.append("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">");
				fw.append("\r\n");
				fw.append("<html><head>");
				fw.append("\r\n");
				fw.append("<title>SEARCH DONE!</title>");
				fw.append("\r\n");
				fw.append("</head><body>");
				fw.append("\r\n");
				
				
				fw.append("<h2> Enter text to start a query : ");
				// add query submit,  it's a http get;
				fw.append("\n<form method=\"GET\" action=\"http://localhost:80\">\n");
				fw.append("\n<input type=\"text\" name=\"query\" size=\"140\"/>     ");
				//submit button
				fw.append("<input type=\"submit\" value=\"GO\"" + "/>  </h2>");
				
				fw.append("\r\n");
				fw.append("<h3>  result for query:  " +rawQuery + "</h3>");
				
				// add info for query terms;
				
				
				// add all the content of search result
				
				
				String curDocString = "          [current docs in database: "+DocumentsManager.getDocumentsManager().getTotalDocCount()+"]";
				
				if(resSize==0){
					String notValidInput = "";
					if(queryTerms.size()==0){
						notValidInput=" query terms maybe all stop words!!";
					}
					fw.append("<h4>  NO result found for query, "+notValidInput+": " +rawQuery +curDocString+ "      </h4>");
					fw.append("\r\n");
				}else{
					
					fw.append("<h4>  totally , "+dPQueue.size()+" results found in the postings of all the terms ,   take time :"+(done-start)+"ms "+curDocString+"     </h4>");
					fw.append("\r\n");
					
					int ind = 0;
					PDocument pd;
					while(!dPQueue.isEmpty()&&ind<3000){
						ind++;
						// get top, thus ranked;
						pd = dPQueue.poll();
//								<a href="http://www.abc.com">text</a>
						
						String title = "";
						if(pd.title==null||pd.title.length()<1){
							title = pd.url;
						}else{
							title = pd.title;
						}
						
						fw.append("<h4>"+ind+".    <a href=\""+pd.url+"\">"+title+"</a> </h4>");
						//add score, so ranked;
						fw.append("\r\n");
						fw.append(pd.url+" <p>");
//						fw.append("<h12> "+pd.url+" </h12>");
						fw.append("\r\n");
						fw.append("rank final score: "+pd.getFinalScore()+"<p>");
//						fw.append("<h12>rank final score: "+pd.getFinalScore()+"</h12>");
						fw.append("\r\n");
					}
				}
				fw.append("\r\n");
				fw.append("</body></html>");
				fw.append("\r\n");
				fw.flush();
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * as default, welcome here, try to search something;
		 */
		private void generateWelcomePage(){
			
			File welcome = new File(WELCOME_VIEW);
			try {
				// generate html file 

				FileWriter fw = new FileWriter(welcome);
				fw.append("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">");
				fw.append("\r\n");
				fw.append("<html><head>");
				fw.append("\r\n");
				fw.append("<title>WELCOME, HERE IS A powerful SEARCH ENGINE BASED ON WHAT I'VE LEARNED FROM 575!!</title>");
				fw.append("\r\n");
				fw.append("</head><body>");
				fw.append("\r\n");
				
				//get current status and added to the welcome page; 
				fw.append("<h13>" + "" + CrawlerManager.getClawerManager().getStatusString() + "</p></h13>");
				fw.append("\r\n");
				fw.append("<h13>" + "" + DocumentsManager.getDocumentsManager().getStatusString() + "</p></h13>");
				fw.append("\r\n");
				fw.append("<h13>" + "" + TermsManager.getTermManagerInstance().getStatusString() + "</p></h13>");
				fw.append("\r\n");
				
				fw.append("<h2> Enter text to start a query : ");
				// add query submit,  it's a http get;
				fw.append("\n<form method=\"GET\" action=\"http://localhost:80\">\n");
				fw.append("\n<input type=\"text\" name=\"query\" size=\"140\"/>     ");
				//submit button
				fw.append("<input type=\"submit\" value=\"GO\"" + " </h2>");
				
				fw.append("\r\n");
				fw.append("</body></html>");
				fw.append("\r\n");
				fw.flush();
				fw.close();
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}

		private void generateDynamicCGIResult(String name, int n1, int n2) {
			File cgif = new File(CGI_CALC_RES);
			try {
				String content = "DEAR " + name + ",  the sum of " + n1
						+ " and " + n2 + " is " + (n1 + n2) + ".";

				FileWriter fw = new FileWriter(cgif);
				fw.append("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">");
				fw.append("\r\n");
				fw.append("<html><head>");
				fw.append("\r\n");
				fw.append("<title>CALCULATION DONE!</title>");
				fw.append("\r\n");
				fw.append("</head><body>");
				fw.append("\r\n");
				fw.append("<h1>" + "" + content + "</h1>");
				fw.append("\r\n");
				fw.flush();
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private void generateDynamicNotFoundFile(String path) {
			File nf = new File(FILE_NOT_FOUND_NAME);
			try {
				FileWriter fw = new FileWriter(nf);
				fw.append("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">");
				fw.append("\r\n");
				fw.append("<html><head>");
				fw.append("\r\n");
				fw.append("<title>404 Not Found</title>");
				fw.append("\r\n");
				fw.append("</head><body>");
				fw.append("\r\n");
				fw.append("<h1>Not Found</h1>");
				fw.append("\r\n");
				fw.append("<p>The requested URL ");
				fw.append(path);
				fw.append(" was not found on this server.</p>");
				fw.append("\r\n");
				fw.append("</body></html>");
				fw.append("\r\n");
				fw.flush();
				fw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		private String readFile(String path) {
			File f = new File(path);
			return readFile(f);
		}

		private String readFile(File f) {
			FileReader fr;
			String res = "";
			try {
				fr = new FileReader(f);
				char[] buf = new char[(int) f.length()];
				fr.read(buf);
				res = String.valueOf(buf);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return res;
		}
	}

	private void writeToLogLineByLine(String s) {

		Logger.println(s);

		// System.out.println(">"+s);
		// if(logFW==null){
		// //current dir
		// File f = new File(LOG_FILE);
		// //
		// System.out.println(" "+f.getParent()+"   "+f.getPath()+"   "+f.getAbsolutePath()+"   ");
		// // try {
		// // System.out.println(" "+f.getCanonicalPath());
		// // } catch (IOException e1) {
		// // e1.printStackTrace();
		// // }
		//
		// try {
		// logFW = new FileWriter(f,true);
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// }
		// if(logFW==null){
		// System.out.println("[warning] log writer is not working!!");
		// return ;
		// }
		// try {
		// logFW.append(s);
		// logFW.append("\r\n");
		// logFW.flush();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
	}

	// private final String LOG_DIR = "";
	// private final String LOG_FILE = "debug.log.txt";
	// private FileWriter logFW = null;

}

public class MyWebServer {

	public static boolean controlSwitch = true;

	public static final int PORT = 80;
	
	
	public void startMywebServer(){
		Runnable ra = new Runnable(){
			@Override
			public void run() {
				try {
					myWebServerRun();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		new Thread(ra).start();
	}

	private void myWebServerRun() throws IOException {
		// define queue size
		int q_len = 6; /* Number of requests for OpSys to queue */

		Socket sock;

		// java.net.ServerSocket
		ServerSocket servsock = new ServerSocket(PORT, q_len);

		System.out
				.println("Yubing Zhang's MyWebServer starting up, listening at port: "
						+ PORT + "  switch is on? " + controlSwitch + ".\n");
		while (controlSwitch) {
			// don't really understand this, haha
			// Listens for a connection to be made to this socket and accepts
			// it. The method blocks until a connection is made.
			// A new Socket s is created and, if there is a security manager,
			// the security manager's checkAccept method is
			// called with s.getInetAddress().getHostAddress() and s.getPort()
			// as its arguments to ensure the operation is allowed.
			// This could result in a SecurityException.
			sock = servsock.accept(); // wait for the next client connection
			if (controlSwitch) {

				// when we got socked, a connection is successfully made.
				// use worker class to process socket content
				new Worker(sock).start(); // Spawn worker to handle it

			}
			// Uncomment to see shutdown oddity:
			// after 10 second, got an exception.
			// try{Thread.sleep(10000);} catch(InterruptedException ex) {}
		}
	}

//	public static void main(String a[]) throws IOException {
//
//	}
}