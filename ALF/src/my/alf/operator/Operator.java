package my.alf.operator;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import my.alf.transferdata.VisualAnalyticsResponse;
import my.alf.transferdata.VisualAnalyticsWorkorder;
import my.alf.utils.PerfData;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;



public class Operator {
	private static Preferences prefs = null;
	private static class FrameData {
		public MatOfByte mobImage;
		public Mat matImage;
		public long n = 0;
		public int lock = 0;
	}
	private static FrameData frameData = new FrameData();
	private static JTextArea perfOutArea;
	private static JPanel imagePanel;
	private static BufferedImage image;
	private static PerfData pData = new PerfData();
	private static VisualSnapshotData vsdata;
	private static boolean doMainloop = true;
	static boolean resetImageProcessor = false;
	public static void main(String s[]) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		prefs = Preferences.userNodeForPackage(Operator.class);
		vsdata = new VisualSnapshotData();
		frameData.matImage = new Mat();
		frameData.mobImage = new MatOfByte();
		initFrame();
		Thread ipThread = new Thread(new remoteImageProcessor());
		ipThread.start();
		mainLoop();
	}
	private static void mainLoop() {
		VideoCapture video = new VideoCapture(prefs.getInt("camNumber", 0));
		long ms;
		while (doMainloop) {
			ms = System.currentTimeMillis();
			grabFrame(video);
			imagePanel.repaint();
			printMetrics();
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
			}
			pData.set(PerfData.VIS_FRAME_LOOP, System.currentTimeMillis() - ms);
		}
		video.release();
	}
	private static void printMetrics() {
		String text;
		text = "PERFORMANCE DATA\n" + pData.getString(new int[] { 0, 1, 2, 3, 4, 5 });
		text += pData.getIndiOutput();
		perfOutArea.setText(text);
	}
	private static void grabFrame(VideoCapture video) {
		long msStart = System.currentTimeMillis();
		if (video.isOpened()) {
			try {
				while (doMainloop && frameData.lock > 0) {
					try {
						Thread.sleep(2);
					} catch (InterruptedException e) {
					}
				}
				frameData.lock++;
				frameData.n++;
				video.read(frameData.matImage);
				if (!frameData.matImage.empty()) {
					Imgcodecs.imencode(".jpg", frameData.matImage, frameData.mobImage);
					image = ImageIO.read(new ByteArrayInputStream(frameData.mobImage.toArray()));
				}
				frameData.lock--;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		pData.set(PerfData.VIS_GRABBED_FRAME_NUM, frameData.n);
		pData.set(PerfData.VIS_FRAME_GRAB, System.currentTimeMillis() - msStart);
	}
	private static void shutDown() {
		v("shutting down");
		doMainloop = false;
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
		System.exit(0);
		v("shutdown complete");
	}
	private static void prefsDialog() {
		JTextField fVisualWorkerServerName = new JTextField(prefs.get("visualWorkerServerName", ""));
		JTextField fVisualWorkerPort = new JTextField(String.valueOf(prefs.getInt("visualWorkerPort", 0)));
		Object[] message = { "VisualWorker Server", fVisualWorkerServerName, "VisualWorker Port", fVisualWorkerPort};
		JOptionPane pane = new JOptionPane(message, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		pane.createDialog(null, "Preferences").setVisible(true);
		if((int) pane.getValue() == JOptionPane.OK_OPTION) {
			prefs.put("visualWorkerServerName", fVisualWorkerServerName.getText());
			prefs.putInt("visualWorkerPort", Integer.parseInt(fVisualWorkerPort.getText()));
			resetImageProcessor = true;
		}
	}
	static void initFrame() {
		// MenuBar
		JMenuBar menuBar = new JMenuBar();
		JMenu mnFile = new JMenu("Action");
		JMenuItem mntmExit = new JMenuItem("Exit");
		mntmExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				shutDown();
			}
		});
		JMenuItem mntmPrefs = new JMenuItem("Preferences");
		mntmPrefs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				prefsDialog();
			}
		});
		mnFile.add(mntmPrefs);
		mnFile.add(mntmExit);
		menuBar.add(mnFile);

		// Image Panel
		imagePanel = new JPanel() {
			private static final long serialVersionUID = 1L;
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				if (image != null) {
					g.drawImage(image, 2, 2, this);
				}
				g.drawRect(0, 0, 643, 483);
				g.drawRect(1, 1, 641, 481);
				if (vsdata != null && vsdata.hasFaces()) {
					Rect[] faces = vsdata.getFaces();
					g.setColor(new Color(255, 0, 0));
					int i;
					for (i = 0; i < faces.length; i++) {
						g.drawRect(faces[i].x, faces[i].y, faces[i].width, faces[i].height);
					}
				}
			}
		};
		imagePanel.setPreferredSize(new Dimension(644, 484));

		// performance Output Panel
		perfOutArea = new JTextArea("PERFORMANCE DATA");
		perfOutArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
		perfOutArea.setOpaque(false);

		// output Panel
		JPanel outputPanel = new JPanel();
		outputPanel.add(perfOutArea);

		// Main Panel
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
		mainPanel.add(imagePanel);
		mainPanel.add(outputPanel);

		// Frame
		JFrame frame = new JFrame("ALF Operator");
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				shutDown();
			}
		});
		frame.setJMenuBar(menuBar);
		frame.add(mainPanel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setBounds(100, 100, 1400, 600);
		frame.setVisible(true);
		frame.repaint();
	}
	public static class remoteImageProcessor implements Runnable {
		@Override
		public void run() {
			int i;
			Socket socket = null;
			long lastFrame = 0;
			long m;
			VisualAnalyticsWorkorder testData = new VisualAnalyticsWorkorder();
			VisualAnalyticsResponse testResponse = new VisualAnalyticsResponse();
			Rect[] faces = null;
			ObjectOutputStream oos = null;
			ObjectInputStream ois = null;
			while (doMainloop) {
				resetImageProcessor = false;
				try {
					socket = new Socket(prefs.get("visualWorkerServerName", "localhost"), prefs.getInt("visualWorkerPort", 4321));
					OutputStream os = socket.getOutputStream();
					oos = new ObjectOutputStream(os);
					InputStream is = socket.getInputStream();
					ois = new ObjectInputStream(is);
					v("ImageProcessor: connected to remote worker");
				} catch (IOException e1) {
					v("ImageProcessor: connection failed");
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {}
				}
				while (frameData.n == 0) {
					try {
						Thread.sleep(20);
					} catch (InterruptedException e) {
					}
				}
				while (socket != null && !resetImageProcessor && socket.isBound() && doMainloop) {
					while (doMainloop && (frameData.lock > 0 || frameData.n == lastFrame)) {
						try {
							Thread.sleep(2);
						} catch (InterruptedException e) {
						}
					}
					frameData.lock++;
					testData.imageData = frameData.mobImage.toArray().clone();
					lastFrame = frameData.n;
					frameData.lock--;
					testData.frameNumber = (int) lastFrame;
					m = System.currentTimeMillis();
					if (oos != null && ois != null) {
						try {
							oos.writeObject(testData);
							oos.reset();
							testResponse = (VisualAnalyticsResponse) ois.readObject();
							faces = new Rect[testResponse.faces.length / 4];
							for (i = 0; i < testResponse.faces.length / 4; i++) {
								faces[i] = new Rect(testResponse.faces[i * 4 + 2], testResponse.faces[i * 4 + 3],
										testResponse.faces[i * 4 + 1], testResponse.faces[i * 4 + 0]);
							}
							vsdata.setFaces(faces);
							pData.set(PerfData.VIS_FACE_DETECT, testResponse.processingTime);
						} catch (ClassNotFoundException | IOException e) {
							v("ImageProcessor: IOException -> resetting connection");
							resetImageProcessor = true;
						}
					}
					pData.set(PerfData.VIS_ANALYZED_FRAME_NUM, lastFrame);
					pData.set(PerfData.VIS_FRAME_ANALYZE, System.currentTimeMillis() - m);
				}
			}
			if (socket != null) {
				try {
					socket.close();
					v("ImageProcessor: worker disconnected");
				} catch (IOException e) {
				}
			}

		}
	}
	public static void v(String s) {
		System.out.println(s);
	}
	public static void v(String s, int i) {
		System.out.println(s + String.valueOf(i));
	}
}
