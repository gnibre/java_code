package util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Logger {
	
	private static boolean DEBUG = false;
	// info is not as much important as DEBUG;
	private static boolean INFO = false;
	private static final String LOG_NAME = "log.txt";
	private static int MAX_LINES_BEFORE_SAVE_TO_LOG = 10;
	static File logFile;
	
	static StringBuilder sb = new StringBuilder();
	static int count = 0;
	
	public static void println(String s){
		if(DEBUG){
			s = "> "+s;
			System.out.println(s);
			addToLog(s);
		}	
	}
	
	public static void print(String s){
		if(DEBUG){
			s = "> "+s;
			System.out.print(s);
			addToLog(s);
		}	
	}
	
	public static void info(String s){
		if(INFO){
			s = "<> "+s;
			System.out.println(s);
			addToLog(s);
		}	
	}
	
	public static void warning(String s){
		String st = "===[[[[WARNING]]]===> "+s;
		System.out.println(st);
		addToLog(st);
	}
	

	
	private static void addToLog(String s){
		if(sb==null){
			sb = new StringBuilder();
		}
		if(count>MAX_LINES_BEFORE_SAVE_TO_LOG){
			addToFile(sb.toString());
			count = 0;
			sb = new StringBuilder();
		}else{
			sb.append(s);
			sb.append("\r\n");
			count++;
		}
	}
	
	private static void addToFile(String s){
		if(logFile==null){
			logFile = new File(LOG_NAME);
		}
		try {
			FileWriter fw = new FileWriter(logFile,true);
			fw.write(s);
			fw.flush();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
	public static<T> void pArray(List<T> al, String name){
		println(" ==========print this array["+name+"];      size : "+al.size());
		for(int i=0;i<al.size();++i){
			System.out.println(" i: "+i+"  "+al.get(i));
		}
		
	}
	
}
