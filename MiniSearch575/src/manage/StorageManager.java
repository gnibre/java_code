package manage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;


/**
 * manage files saved on local disk.
 * @author gnibrE
 *
 */
public class StorageManager {

	private static final String SUFFIX = ".dat";
	
	/**
	 * save file s to current working directory
	 * @param map
	 * @param name
	 */
	public static void saveObject(Map map,String name){
		String dataName = name+SUFFIX;
		System.out.println(" save  to file : "+dataName);
		File f = new File(dataName);
		saveObject(map,f);
	}
	
	public static void saveObject(LinkedList l,String name){
		String dataName = name+SUFFIX;
		System.out.println(" save  to file : "+dataName);
		File f = new File(dataName);
		saveObject(l,f);
	}
	
	public static ObjectInputStream getObject(String name){
		String dataName = name+SUFFIX;
		System.out.println(" would like to retrieve file: "+dataName);
		File f = new File(dataName);
		return readObjectToStream(f);
	}
	
	
	private static int saveObject(Object o,File f){
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(f);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(o);
			oos.flush();
			oos.close();
			fos.close();
			return 0;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch(Exception e){
			e.printStackTrace();
		}
		return -1;
	}
	
	private static ObjectInputStream readObjectToStream(File f){
		FileInputStream fis;
		try {
			fis = new FileInputStream(f);
			ObjectInputStream ois = new ObjectInputStream(fis);
			return ois;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	
}
