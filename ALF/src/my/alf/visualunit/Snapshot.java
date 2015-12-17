package my.alf.visualunit;

import org.opencv.core.Rect;

public class Snapshot {
	public Rect[] items;
	public int[] distance;
	public int[] xCenter;
	public int[] yCenter;
	public int[] followup;
	public double localTimestamp = 0;
	public double operatorTimestamp = 0;
	public double frameNumber = 0;
	public Snapshot() {
		localTimestamp = System.currentTimeMillis();
	}
	public Snapshot(double dFrameNumber,  Rect[] aItems ) {
		localTimestamp = System.currentTimeMillis();
		frameNumber = dFrameNumber;
		items = aItems;
		distance = new int[aItems.length];
		xCenter = new int[aItems.length];
		yCenter = new int[aItems.length];
		followup = new int[aItems.length];
	}
}
