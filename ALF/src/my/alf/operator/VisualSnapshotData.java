package my.alf.operator;

import org.opencv.core.Rect;

public class VisualSnapshotData {
	private Rect[] faces = null;
	private Rect[] faces2 = null;
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

	public Rect[] getFaces2() {
		return faces2;
	}

	public void setFaces2(Rect[] faces2) {
		this.faces2 = faces2;
	}
}
