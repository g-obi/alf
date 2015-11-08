package my.alf.operator;

import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;

public class VisualSnapshotData {
	private Rect[] faces = null;
	public int lock = 0;
	public int frameNumber = 0;

	public Rect[] getFaces() {
		return faces;
	}

	public void setFaces(Rect[] faces) {
		this.faces = faces;
	}
	public boolean hasFaces() {
		if (faces == null) return false;
		if (faces.length == 0) return false;
		return true;
	}
}
