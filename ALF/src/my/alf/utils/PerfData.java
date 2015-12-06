package my.alf.utils;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

/**
 * @author Gernot
 *
 */
public class PerfData {
	// publics
	/**
	 * Time for grabbing and converting a frame from webcam
	 */
	public static final int VIS_FRAME_GRAB = 0;
	/**
	 * Duration of the complete frame analytics cycle including transmission time
	 */
	public static final int VIS_FRAME_ANALYZE = 1; // Complete Frame analytics cycle
	/**
	 * Net Time for Face detection
	 */
	public static final int VIS_FACE_DETECT = 2;
	/**
	 * Time for inner loop (frame grab, dispatching, display stuff)
	 */
	public static final int VIS_FRAME_LOOP = 3;
	/**
	 * number of last grabbed frame
	 */
	public static final int VIS_GRABBED_FRAME_NUM = 4;
	/**
	 * number of last processed frame
	 */
	public static final int VIS_ANALYZED_FRAME_NUM = 5;
	
	// privates
	private static int buffersize = 6;
	private static long[] store = new long[buffersize];
	private static String[] labels = new String[buffersize];
	private static String[] normlabels = new String[buffersize];
	//private static List<String> indiStore = new ArrayList<String>();
	Hashtable<String, Long> indiStore = new Hashtable<String, Long>();
	Hashtable<String, Long> indiTemp = new Hashtable<String, Long>();
	private static int tabSize = 8;

	public PerfData() {
		labels[VIS_FRAME_GRAB] 			= "VIS_FRAME_GRAB: ";
		labels[VIS_FRAME_ANALYZE] 		= "VIS_FRAME_ANALYZE: ";
		labels[VIS_FACE_DETECT] 		= "VIS_FACE_DETECT: ";
		labels[VIS_FRAME_LOOP] 			= "VIS_FRAME_LOOP: ";
		labels[VIS_GRABBED_FRAME_NUM] 	= "VIS_GRABBED_FRAME_NUM: ";
		labels[VIS_ANALYZED_FRAME_NUM] 	= "VIS_ANALYZED_FRAME_NUM: ";
		
		// Normalize Length by tabs
		int i, j;
		int maxlen=0;
		int tabsize = 8;
		int tabsNeeded = 0;
		for(i=0;i<buffersize;i++) if(labels[i].length()>maxlen) maxlen = labels[i].length();
		for(i=0; i<buffersize;i++) {
			tabsNeeded = (int) Math.ceil(  (double)(maxlen-labels[i].length())  /  (double)tabsize  );
			normlabels[i] = labels[i];
			for(j=0;j< tabsNeeded;j++) normlabels[i] = normlabels[i] + "\t";
		}
		
	}
	
	public void set(int i, long value) {
		store[i] = value;
	}
	public long get(int i) {
		return store[i];
	}
	public String getString(int[] values) {
		String s = new String();
		int i;
		for(i=0; i<values.length; i++) {
			s = s + normlabels[values[i]] + store[values[i]] + "\n";
		}
		return s;
	}
	
	public void set(String key, long millisec) {
		if(key!=null) indiStore.put(key, millisec);
	}
	public long get(String key) {
		Long l = indiStore.get(key);
		return l==null?0:l;
	}
	public String getIndiOutput() {
		int i, maxlen=0, tabsNeeded=0;
		String retval = "";
		Set<String> keys = indiStore.keySet();
		for (String key : keys) if(key.length()>maxlen)maxlen = key.length();
		for (String key : keys) {
			tabsNeeded = (int) Math.ceil(  (double)(maxlen-key.length())  /  (double)tabSize  );
			retval += key+": ";
			for(i=0;i<tabsNeeded;i++) retval += "\t";
			retval += indiStore.get(key) + "\n";
		}
		return retval ;
	}
	public void startTimekeeping(String key) {
		indiTemp.put(key, System.currentTimeMillis());
	}
	public long endTimekeeping(String key) {
		if(!indiTemp.containsKey(key)) return 0;
		long ms = System.currentTimeMillis() - indiTemp.get(key);
		indiStore.put(key, ms);
		return ms;
	}
	
}
