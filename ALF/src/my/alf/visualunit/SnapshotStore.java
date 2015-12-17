package my.alf.visualunit;

import java.util.ArrayList;
import org.opencv.core.Rect;

public class SnapshotStore {
	public ArrayList<Snapshot> snapshots = new ArrayList<Snapshot>();
	long timeToKeep = 2000;
	public void deleteOldItems(long ms) {
		// todo: Implement clean up
	}
	public void addSnapshot(double dFrameNumber,  Rect[] aItems ) {
		snapshots.add(new Snapshot(dFrameNumber, aItems));
		long ms = System.currentTimeMillis();
		while(snapshots.get(0).localTimestamp < ms - timeToKeep) {
			snapshots.remove(0);
		}
	}
	public void addSnapshot(Snapshot s) {
		snapshots.add(s);
	}
}
