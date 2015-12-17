package my.alf.visualunit;

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
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import my.alf.operator.VisualSnapshotData;
import my.alf.utils.PerfData;

public class VisualUnit {
	private static Preferences prefs = null;
	private static class FrameData {
		public MatOfByte mobImage;
		public Mat matImage;
		public long n = 0;
		public int lock = 0;
	}
	private static FrameData frameData = new FrameData();
	private static JTextArea perfOutArea;
	private static JPanel imagePanel,imagePanel2;
	private static BufferedImage image, image2=null;
	private static PerfData pData = new PerfData();
	private static VisualSnapshotData vsdata;
	private static boolean doMainloop = true;
	static boolean resetImageProcessor = false;
	public static VisualProcessor visualProcessor = new VisualProcessor();
	public static void main(String s[]) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		prefs = Preferences.userNodeForPackage(VisualUnit.class);
		vsdata = new VisualSnapshotData();
		frameData.matImage = new Mat();
		frameData.mobImage = new MatOfByte();
		initFrame();
		Thread ipThread = new Thread(new ImageHandler());
		ipThread.start();
		mainLoop();
	}
	private static void mainLoop() {
		VideoCapture video = new VideoCapture(prefs.getInt("camNumber", 0));
		while (doMainloop) {
			pData.startTimekeeping("Frame Loop");
			grabFrame(video);
			imagePanel.repaint();
			imagePanel2.repaint();
			printMetrics();
			pData.endTimekeeping("Frame Loop");
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
					Imgcodecs.imencode(".bmp", frameData.matImage, frameData.mobImage);
					image = ImageIO.read(new ByteArrayInputStream(frameData.mobImage.toArray()));
				}
				frameData.lock--;
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
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
					Rect[] faces2 = vsdata.getFaces2();
					g.setColor(new Color(255, 0, 0));
					int i;
					for (i = 0; i < faces.length; i++) {
						g.drawRect(faces[i].x, faces[i].y, faces[i].width, faces[i].height);
					}
					g.setColor(new Color(255, 255, 0));
					for (i = 0; i < faces2.length; i++) {
						g.drawRect(faces2[i].x-1, faces2[i].y-1, faces2[i].width+2, faces2[i].height+2);
						g.drawRect(faces2[i].x-2, faces2[i].y-2, faces2[i].width+4, faces2[i].height+4);
					}
				}
			}
		};
		imagePanel.setPreferredSize(new Dimension(644, 484));

		// Image Panel2
		imagePanel2 = new JPanel() {
			private static final long serialVersionUID = 1L;
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				if (image2 != null) {
					g.drawImage(image2, 2, 2, this);
				}
				g.drawRect(0, 0, 643, 483);
				g.drawRect(1, 1, 641, 481);
				if (vsdata != null && vsdata.hasFaces()) {
					Rect[] faces = vsdata.getFaces();
					Rect[] faces2 = vsdata.getFaces2();
					int i;
					g.setColor(new Color(255, 255, 0));
					for (i = 0; i < faces2.length; i++) {
						g.drawRect(faces2[i].x-1, faces2[i].y-1, faces2[i].width+2, faces2[i].height+2);
						g.drawRect(faces2[i].x-2, faces2[i].y-2, faces2[i].width+4, faces2[i].height+4);
					}
				}
			}
		};
		imagePanel2.setPreferredSize(new Dimension(644, 484));

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
		mainPanel.add(imagePanel2);
		mainPanel.add(outputPanel);

		// Frame
		JFrame frame = new JFrame("ALF Visual Unit");
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
	public static class ImageHandler implements Runnable {
		MatOfByte mobImage = new MatOfByte();
		Mat matImage;
		long frameNumber;
		@Override
		public void run() {
			long lastFrame = 0;
			long m;
			while (doMainloop) {
				resetImageProcessor = false;
				while (frameData.n == 0) {
					try {
						Thread.sleep(20);
					} catch (InterruptedException e) {
					}
				}
				while (doMainloop) {
					pData.startTimekeeping("Lockloop");
					while (doMainloop && (frameData.lock > 0 || frameData.n == lastFrame)) {
						try {
							Thread.sleep(2);
						} catch (InterruptedException e) {
						}
					}
					pData.endTimekeeping("Lockloop");
					frameData.lock++;
					matImage = frameData.matImage.clone();
					lastFrame = frameData.n;
					frameData.lock--;
					m = System.currentTimeMillis();
					// Analyse
					visualProcessor.processImage(lastFrame, matImage);
					if(visualProcessor.mtOutput!= null) {
						Imgcodecs.imencode(".bmp", visualProcessor.mtOutput, mobImage);
						try {
							image2 = ImageIO.read(new ByteArrayInputStream(mobImage.toArray()));
						} catch (IOException e) {}
					}
					vsdata.frameNumber = (int) lastFrame;
					vsdata.setFaces(visualProcessor.getItems());
					vsdata.setFaces2(visualProcessor.getItems2());
					// Analyse
					pData.set(PerfData.VIS_ANALYZED_FRAME_NUM, lastFrame);
					pData.set(PerfData.VIS_FRAME_ANALYZE, System.currentTimeMillis() - m);
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
