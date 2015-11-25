package my.alf.worker;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Rect;

public class SnapshotStore {
	public static ArrayList<Snapshot> snapshots = new ArrayList<Snapshot>();
	public void deleteOldItems(long ms) {
		// todo: Implement clean up
	}
	public void addSnapshot(double dFrameNumber,  Rect[] aItems ) {
		snapshots.add(new Snapshot(dFrameNumber, aItems));
	}
	public void addSnapshot(Snapshot s) {
		snapshots.add(s);
	}
}
