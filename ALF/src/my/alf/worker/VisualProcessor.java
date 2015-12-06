package my.alf.worker;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.objdetect.CascadeClassifier;

import my.alf.utils.PerfData;

public class VisualProcessor {
	
	final int CAM_ANGEL = 65;
	public SnapshotStore store = new SnapshotStore();
	public PerfData perf = new PerfData();
	CascadeClassifier cc;
	public VisualProcessor() {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		cc = new CascadeClassifier("C:\\opencv\\sources\\data\\haarcascades_cuda\\haarcascade_frontalface_default.xml");
	}
	double processImage(double dFrameNumber, Mat img) {
		int i, j;
		double msMaxObjSpeed = 1; // in m/s
		double tana = Math.tan((CAM_ANGEL/2) * (Math.PI / 180));
		double cmFrameWidthAtObjDst = 0, cmObjDst = 0; 
		double pxFrameWidth = 0, pxObjWidth = 0,  cmObjWidth= 17;
		double cmMaxObjMoveDst = 0, pxMaxObjMoveDst=0;
		double msTimeDiff = 0;
		pxFrameWidth = img.cols();
		MatOfRect results = new MatOfRect();
		cc.detectMultiScale(img, results);
		Snapshot s = new Snapshot(dFrameNumber, results.toArray());
		store.addSnapshot(s);
		for(i=0; i<s.items.length; i++) {
			pxObjWidth = s.items[i].width;
			cmFrameWidthAtObjDst = (pxFrameWidth/pxObjWidth) * cmObjWidth;
			cmObjDst = cmFrameWidthAtObjDst / (tana * 2);
			s.distance[i] = (int) cmObjDst;
			s.xCenter[i] = s.items[i].x + s.items[i].width/2;
			s.yCenter[i] = s.items[i].y + s.items[i].height/2;
			s.followup[i] = 0;
			if (store.snapshots.size() > 1) {
				Snapshot s2 = store.snapshots.get(store.snapshots.size() - 2);
				msTimeDiff = (s.localTimestamp - s2.localTimestamp) / 1000;
				cmMaxObjMoveDst = msMaxObjSpeed * msTimeDiff * 100;
				pxMaxObjMoveDst = (pxFrameWidth * cmMaxObjMoveDst) / cmFrameWidthAtObjDst;
				for (j = 0; j < s2.items.length; j++) {
					if ((s2.distance[j] > s.distance[i] - 10 && s2.distance[j] < s.distance[i] + 10)
							&& (s2.xCenter[j] > s.xCenter[i] - pxMaxObjMoveDst && s2.xCenter[j] < s.xCenter[i] + pxMaxObjMoveDst)
							&& (s2.yCenter[j] > s.yCenter[i] - pxMaxObjMoveDst && s2.yCenter[j] < s.yCenter[i] + pxMaxObjMoveDst)) {
						s.followup[i] = s2.followup[j] + 1;
					}
				}
			}
		}
		return 0;
	}
	Rect[] getItems() {
		if(store.snapshots.size()>0) {
			return store.snapshots.get(0).items;
		}
		return new Rect[0];
	}
	public static void v(String s) {
		System.out.println(s);
	}
	public static void v(String s, int i) {
		System.out.println(s + String.valueOf(i));
	}
	public static void v(String s, double d) {
		System.out.println(s + String.valueOf(d));
	}
}
