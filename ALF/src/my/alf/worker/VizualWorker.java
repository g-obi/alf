package my.alf.worker;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.objdetect.CascadeClassifier;

import my.alf.transferdata.VisualAnalyticsResponse;
import my.alf.transferdata.VisualAnalyticsWorkorder;



public class VizualWorker {
	private ExecutorService executors = Executors.newFixedThreadPool(10);
	private boolean isRunning = true;
	static BufferedImage image = null;
	static JPanel imagePanel = null;

	public static void main(String[] args) throws Exception {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		initFrame();
		int port = 4321;
		new VizualWorker().launch(port);
	}

	public void launch(int port) throws Exception {
		ServerSocket sso = new ServerSocket(port);
		while (isRunning) {
			v("waiting for connection");
			Socket s = sso.accept();
			executors.execute(new Worker(s));
		}
		sso.close();
	}

	static void initFrame() {
		JMenuBar menuBar = new JMenuBar();
		JMenu mnFile = new JMenu("File");
		JMenuItem mntmExit = new JMenuItem("Exit");
		mntmExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				System.exit(0);
			}
		});
		mnFile.add(mntmExit);
		menuBar.add(mnFile);
		imagePanel = new JPanel() {
			private static final long serialVersionUID = 1L;
		    public void paintComponent(Graphics g){
		        super.paintComponent(g);
		        if(image != null){
		            g.drawImage(image, 2, 2, this);
		        }
		        g.drawRect(0, 0, 643, 483);
		        g.drawRect(1, 1, 641, 481);
		    }
		};
		imagePanel.setPreferredSize(new Dimension(644, 484));

		JFrame frame = new JFrame("Visual Analytics Server");
		frame.setJMenuBar(menuBar);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setBounds(100, 100, 1400, 600);
		frame.add(imagePanel);
		frame.setVisible(true);
		frame.repaint();
	}

	private class Worker implements Runnable {
		private BufferedReader in = null;
		private Socket s = null;

		Worker(Socket socket) throws Exception {
			v("Worker created");
			s = socket;
		}

		public void run() {
			v("Worker running");
			int i;
			InputStream is = null;
			ObjectInputStream ois = null;
			OutputStream os = null;
			ObjectOutputStream oos = null;
			VisualAnalyticsWorkorder testData = null;
			VisualAnalyticsResponse testResponse = null;
			MatOfByte mob = null;
			Mat mat = null;
			MatOfRect results = null;;
			Rect[] aResults;
			long m = 0;
			CascadeClassifier cc = new CascadeClassifier("C:\\opencv\\sources\\data\\haarcascades_cuda\\haarcascade_frontalface_default.xml");
			boolean thisRunning = true;
			try {
				is = s.getInputStream();
				os = s.getOutputStream();
				ois = new ObjectInputStream(is);
				oos = new ObjectOutputStream(os);
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}

			while (thisRunning && s.isConnected() && s.isBound()) {
				try {
					if((testData = (VisualAnalyticsWorkorder)ois.readObject()) != null) {
						testResponse = new VisualAnalyticsResponse();
						if (true) {
							mob = new MatOfByte(testData.imageData);
							mat = Imgcodecs.imdecode(mob, Imgcodecs.CV_LOAD_IMAGE_COLOR);
							try {
								image = ImageIO.read(new ByteArrayInputStream(testData.imageData.clone()));
							} catch (IOException e1) {
								e1.printStackTrace();
							}
							imagePanel.repaint();
							v("Frame Number: " + String.valueOf(testData.frameNumber));
							results = new MatOfRect();
							try {
								m = System.currentTimeMillis();
								cc.detectMultiScale(mat, results);
								testResponse.processingTime = System.currentTimeMillis() - m;
								aResults = results.toArray();
								v("faces: ", aResults.length);
								testResponse.faces = new int[aResults.length*4];
								for(i=0; i<aResults.length; i++) {
									testResponse.faces[4*i+0] = aResults[i].height;
									testResponse.faces[4*i+1] = aResults[i].width;
									testResponse.faces[4*i+2] = aResults[i].x;
									testResponse.faces[4*i+3] = aResults[i].y;
								}
								
							} catch (Exception e) {
								e.printStackTrace();
							}
							mob.release();
							mat.release();
						}
						
						oos.writeObject(testResponse);
						oos.reset();
					} else {	
						v("testdata ist null");
					}
					

				} catch (ClassNotFoundException | IOException e) {
					thisRunning = false;
					//e.printStackTrace();
				}
				testData = null;
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			v("worker shutting down");



		}
	}

	public static void v(String s) {
		System.out.println(s);
	}
	public static void v(String s, int i) {
		System.out.println(s + String.valueOf(i));
	}
}
