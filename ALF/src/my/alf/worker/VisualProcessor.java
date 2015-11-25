package my.alf.worker;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.objdetect.CascadeClassifier;

public class VisualProcessor {
	
	final int CAM_ANGEL = 50;
	public SnapshotStore store = new SnapshotStore();
	CascadeClassifier cc;
	public VisualProcessor() {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		cc = new CascadeClassifier("C:\\opencv\\sources\\data\\haarcascades_cuda\\haarcascade_frontalface_default.xml");
	}
	double processImage(double dFrameNumber, Mat img) {
		double tana = Math.tan(CAM_ANGEL);
		double A = 0, B = 0;
		MatOfRect results = new MatOfRect();
		cc.detectMultiScale(img, results);
		Snapshot s = new Snapshot(dFrameNumber, results.toArray());
		store.addSnapshot(s);
		
		return 0;
	}
	Rect[] getItems() {
		if(store.snapshots.size()>0) {
			return store.snapshots.get(0).items;
		}
		return new Rect[0];
	}
}
