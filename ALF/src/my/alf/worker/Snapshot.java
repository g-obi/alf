package my.alf.worker;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Rect;

public class Snapshot {
	public static Rect[] items;
	public static double localTimestamp = 0;
	public static double operatorTimestamp = 0;
	public static double frameNumber = 0;
	public Snapshot() {
		localTimestamp = System.currentTimeMillis();
	}
	public Snapshot(double dFrameNumber,  Rect[] aItems ) {
		localTimestamp = System.currentTimeMillis();
		frameNumber = dFrameNumber;
		items = aItems;
	}
}
