package my.alf.visualunit;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import my.alf.utils.PerfData;

public class VisualProcessor {
	ArrayList<Mat> frames = new ArrayList<Mat>();
	int maxFrames = 10;
	Mat mtLast = null;
	Mat mtLastResult = null;
	
	ArrayList<Rect> items2;
	int items2gateway = 10;
	
	Mat mtOutput = null;
	
	final int CAM_ANGEL = 65;
	public SnapshotStore store = new SnapshotStore();
	public PerfData perf = new PerfData();
	CascadeClassifier cc;
	public VisualProcessor() {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		cc = new CascadeClassifier("C:\\opencv\\sources\\data\\haarcascades_cuda\\haarcascade_frontalface_default.xml");
	}
	public double processImage(double dFrameNumber, Mat img) {
		items2 = new ArrayList<Rect>();
		perf.startTimekeeping("Process Image");
		int i, j;
		frames.add(img.clone());
		if(frames.size()>maxFrames) {
			Mat tmpMat = frames.remove(0);	
			tmpMat.release();
		}
		
		// Face Recognition
		double rezizeFactor = 1.3;
		double msMaxObjSpeed = 1.5; // in m/s
		double tana = Math.tan((CAM_ANGEL/2) * (Math.PI / 180));
		double cmFrameWidthAtObjDst = 0, cmObjDst = 0; 
		double pxFrameWidth = 0, pxObjWidth = 0,  cmObjWidth= 17;
		double cmMaxObjMoveDst = 0, pxMaxObjMoveDst=0;
		double msTimeDiff = 0;
		pxFrameWidth = img.cols();
		MatOfRect results = new MatOfRect();
		Mat smallImg = new Mat();
		Imgproc.resize(img, smallImg, new Size((int)(img.cols()/rezizeFactor), (int) (img.rows()/rezizeFactor)));
		perf.startTimekeeping("Face Detection");
		cc.detectMultiScale(smallImg, results);
		perf.endTimekeeping("Face Detection");
		Rect[] resultItems = results.toArray();
		for(i=0; i<resultItems.length; i++) {
			resultItems[i].height = (int) (resultItems[i].height * rezizeFactor);
			resultItems[i].width = (int) (resultItems[i].width * rezizeFactor);
			resultItems[i].x = (int) (resultItems[i].x * rezizeFactor);
			resultItems[i].y = (int) (resultItems[i].y * rezizeFactor);
		}
		Snapshot s = new Snapshot(dFrameNumber, resultItems);
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
			if(s.followup[i]> items2gateway) items2.add(s.items[i].clone());
		}
		
		// Motion Tracking 1
		Mat mtMat = img.clone();
		Mat mtGray = new Mat(mtMat.rows(), mtMat.cols(), CvType.CV_8UC1, new Scalar(0));
		Mat mtResult = new Mat(mtMat.rows(), mtMat.cols(), CvType.CV_8UC1);
		if(frames.size()>1) {
			Imgproc.blur(mtMat, mtMat, new Size(8,8));
			Imgproc.cvtColor(mtMat, mtGray, Imgproc.COLOR_RGB2GRAY);
			Imgproc.equalizeHist(mtGray, mtGray);
			if(mtLast != null) {
				Core.absdiff(mtLast, mtGray, mtResult);
				//Imgproc.threshold(mtResult, mtResult, 32, 255, Imgproc.THRESH_BINARY);
			} else {
				 mtLast = new Mat(mtMat.rows(), mtMat.cols(), CvType.CV_8UC1);
			}
			mtLast = mtGray.clone();
			mtOutput = mtResult.clone();

		}
		mtResult.release();
		mtGray.release();
		mtMat.release();
		
		
		
		perf.endTimekeeping("Process Image");
		v(perf.getIndiOutput());
		results.release();
		smallImg.release();
		img.release();
		mtMat.release();
		return 0;
	}
	public Rect[] getItems() {
		if(store.snapshots.size()>0) {
			return store.snapshots.get(store.snapshots.size()-1).items;
		}
		return new Rect[0];
	}	
	public Rect[] getItems2() {
		Rect[] r = new Rect[items2.size()];
		items2.toArray(r);
		return r;
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
