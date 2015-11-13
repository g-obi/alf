package my.alf.transferdata;

import java.io.Serializable;

public class VisualAnalyticsWorkorder implements Serializable {
	private static final long serialVersionUID = 1497653807346980388L;
	public static final int ACTION_FACERECOGNITION = 1;
	public static final int ACTION_SOMTINGELSE = 2;
	public byte[] imageData = null;
	public int frameNumber = 0;
}
