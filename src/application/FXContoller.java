package application;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import utils.Utils;
import utils.Utils.Borders;
import utils.Utils.CubeColor;
import utils.Utils.Point;
import utils.Utils.Rect;
import utils.Utils.Tuple;


public class FXContoller
{
	// the FXML button
	@FXML
	private Button button, solveButton, calibrateButton;

	// the FXML image view
	@FXML
	private ImageView currentFrame;

	// a timer for acquiring the video stream
	private ScheduledExecutorService timer;
	// the OpenCV object that realizes the video capture
	private VideoCapture capture = new VideoCapture();
	// a flag to change the button behavior
	private boolean cameraActive = false;
	// the id of the camera to be used
	private static int cameraId = 0;

	private boolean drawBoundingRect = true;

	private static Rect currentBoungingRect = new Rect(0,0,10,10);

	private int max_rect_area = 1;

	private static Map<CubeColor, CubeColor[][]> faceToColors;

	private static Map<CubeColor, Double> colorToMinMaxDis; //hold the most certain decision's score. the certainty score is the maximum L2 distance of any color's classification.

	private Stage primaryStage = null;

	private Mat imgDbg = new Mat();

	public void setPrimaryStage(Stage stage){
		this.primaryStage = stage;
	}

	private static void init(){
		faceToColors = new HashMap<>();
		colorToMinMaxDis = new HashMap<>();
		for(CubeColor c : CubeColor.values()){
			CubeColor[][] faceColors = new CubeColor[3][3];
			for(int row = 0; row < 3; row++){
				for(int col = 0; col < 3; col++){
					faceColors[row][col] = c;
				}
			}
			faceToColors.put(c, faceColors);
			colorToMinMaxDis.put(c, Double.MAX_VALUE);
		}
	}

	static ReentrantLock lock = new ReentrantLock();


	@FXML
	protected void startCamera(ActionEvent event){

		init();


		if (!this.cameraActive)
		{
			// start the video capture
			this.capture.open(cameraId);

			// is the video stream available?
			if (this.capture.isOpened())
			{
				this.cameraActive = true;

				// grab a frame every 33 ms (30 frames/sec)
				Runnable frameGrabber = new Runnable() {

					@Override
					public void run()
					{
						// effectively grab and process a single frame
						//System.out.println("frame");
						Mat frame = grabFrame();

//						final Mat frameCpy = new Mat(frame.size(), 0);
//						frame.copyTo(frameCpy);


						if(drawBoundingRect && !calibrating){

							drawCubeBoundingRect(frame);
						}

						currentFrame.setOnMouseClicked(v -> {
							if(v.getButton() == MouseButton.SECONDARY){
								showImage(imgDbg);
							}
							double[] bgr = frame.get((int)v.getY(), (int)v.getX());
							if(calibrating){
								if(calibrateColorIndex >= 6){
									calibrating = false;
									calibrateButton.setText("Calibrate");
									System.out.println("calibrated");
									//save colors
									 FileOutputStream fos;
									try {
										fos = new FileOutputStream("calibratedColors.ser");
										ObjectOutputStream oos= new ObjectOutputStream(fos);
								        oos.writeObject(ColorDetector.colorList);
								        oos.close();
								        fos.close();
									} catch (FileNotFoundException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}

								}else{
									System.out.println("calibrated:" + Utils.CubeColor.getColor(calibrateColorIndex));
									ColorDetector.colorList.add(new ColorDetector.ColorName(Utils.CubeColor.getColor(calibrateColorIndex), (int)bgr[2], (int)bgr[1], (int)bgr[0]));
									calibrateColorIndex++;
								}

							}
							//System.out.println("click bgr:" + Arrays.toString(bgr));
							for(CubeColor c : Utils.colorToBgr.keySet()){
								double[] aBgr = Utils.colorToBgr.get(c);
								//System.out.println(Arrays.toString(aBgr));
								//System.out.println(Arrays.toString(bgr));
								if(bgr[0] == aBgr[0] && bgr[1] == aBgr[1] && aBgr[2] == bgr[2]){
									max_rect_area = 1;
									//reset colors of face
									CubeColor[][] current = faceToColors.get(c);
									for(int row = 0; row < 3; row++){
										for(int col = 0; col < 3; col++){
											current[row][col] = c;
										}
									}
									colorToMinMaxDis.put(c, Double.MAX_VALUE);
									//faceToColors.put(c, new CubeColor[3][3]);
								}

							}
						});

						// convert and show the frame
						Image imageToShow = Utils.mat2Image(frame);

						updateImageView(currentFrame, imageToShow);
//						try {
//							Thread.sleep(1000);
//						} catch (InterruptedException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
					}
				};

				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);

				// update the button content
				this.button.setText("Stop Camera");
			}
			else
			{
				// log the error
				System.err.println("Impossible to open the camera connection...");
			}
		}
		else
		{
			// the camera is not active at this point
			this.cameraActive = false;
			// update again the button content
			this.button.setText("Start Camera");
			// stop the timer
			this.stopAcquisition();
		}


	}



	public void drawCubeBoundingRect(Mat img){


		resize(img);
		List<List<Point>> areas = getSquaresAreas(img);
		//System.out.println(areas.size());

		//find bounding rect
		int minCol = Integer.MAX_VALUE, minRow = Integer.MAX_VALUE;
		int maxCol = Integer.MIN_VALUE, maxRow = Integer.MIN_VALUE;
		for(List<Point> points : areas){
			for(Point p : points){
				minCol = Math.min(minCol, p.col);
				minRow = Math.min(minRow, p.row);
				maxCol = Math.max(maxCol, p.col);
				maxRow = Math.max(maxRow, p.row);
			}
		}

		Rect cubeRect = new Rect(minCol, minRow, maxCol, maxRow);




		//cubeRect.stretch(20);
		if(cubeRect.isSquare()){// || Math.abs(cubeRect.getMaxWidthHeightRatio() - 1.5) < 0.1){
			currentBoungingRect = cubeRect;
		}
		cubeRect.Squarify();
		cubeRect = currentBoungingRect;

		if(cubeRect.width < 5 || cubeRect.height < 5 || cubeRect.width * cubeRect.height < max_rect_area * 0.5){
			//System.out.println("TOO SMALL");
			//max_rect_area *= .9;
			drawCubeFaces(img);
			return;
		}
		max_rect_area = Math.max(cubeRect.width * cubeRect.height, max_rect_area);
		//Imgcodecs.imwrite(filename, img)

		Mat imgCopy = new Mat();
		img.copyTo(imgCopy);

		//draw bounding rect
		for(int row = cubeRect.y; row <= cubeRect.y + cubeRect.height; row++){
			for(int col = -5 ; col <= 5; col++){
				col = 0;
				img.put(row, cubeRect.x + col, 0,0,255);
				img.put(row, cubeRect.x + cubeRect.width + col,  0,0,255);
				col = 6;
			}
		}
		for(int col = cubeRect.x; col <= cubeRect.x + cubeRect.width; col++){
			for(int row = -5; row <= 5; row++){
				row = 0;
				img.put(cubeRect.y + row, col,  0,0,255);
				img.put(cubeRect.y + cubeRect.height + row, col,  0,0,255);
				row = 6;

			}
		}

		//update face colors
		double[][][] pieceColors = new double[9][3][3];
		CubeColor[][] faceColors = new CubeColor[3][3];

		double maxDis = Integer.MIN_VALUE;
		double middlePieceDis = Integer.MAX_VALUE;
		int index = 0;
		for(int row = cubeRect.y + cubeRect.height / 6; row < cubeRect.y + cubeRect.height; row += cubeRect.height / 3){
			for(int col = cubeRect.x + cubeRect.width/6; col < cubeRect.x + cubeRect.width; col += cubeRect.width / 3){
				pieceColors[index/3][index%3] = img.get(row, col);
				Tuple<CubeColor, Double> closestColorAndDis = Utils.getClosestColor(pieceColors[index/3][index%3]);
				faceColors[index/3][index%3] = closestColorAndDis.first;
				double dis = closestColorAndDis.second;
				if(index/3 == 1 && index%3 == 1){
					middlePieceDis = dis;
				}
				if(dis > maxDis){
					maxDis = dis;
				}
				index++;
				for(int i = -2; i <= 2; i++){
					for(int j = -2; j <= 2; j++){

					//	img.put(row + i,col + j , Utils.getColorBgr(pieceColor) );
						img.put(row + i,col + j , 0,0,255);

					}
				}
			}
		}

		//only update face's colors if it's more certain than the current face's colors
		//the certainty of a prediction is measured by that maximum L2 distance of any of the face's colors. (the lower the distance more certain the prediction is)
		final int MAX_DIST_THRESH = 100, MAX_MIDDLE_PIECE_THRESH = 60;
		final CubeColor middleColor = Utils.getClosestColor(pieceColors[1][1]).first;

		boolean SAVE_IMG_TRAINING_DATA = false;



		CubeColor[][] current = faceToColors.get(middleColor);
		if(current != null && Utils.sameColors(current, faceColors)){
			if(SAVE_IMG_TRAINING_DATA){
				saveImageTrainigData(imgCopy, faceColors);
			}
		}

		if(maxDis <= colorToMinMaxDis.get(middleColor) || framesNoColorUpdate >= framesNoColorUpdateThresh){
			faceToColors.put(middleColor, faceColors);
			colorToMinMaxDis.put(middleColor, maxDis);

			if( framesNoColorUpdate >= framesNoColorUpdateThresh){
			//	System.out.println("thresh");
			}
			framesNoColorUpdate = 0;
		}else{
			framesNoColorUpdate++;
			//double diff = maxDis - colorToMinMaxDis.get(middleColor);
			//colorToMinMaxDis.put(middleColor, colorToMinMaxDis.get(middleColor)*1.1); //else - weaken that detection certainty, in order to not let the detection get fixated on a wrong color detection.
		}



		drawCubeFaces(img);


	}

	private void saveImageTrainigData(Mat img, CubeColor[][] colors) {
		final Size tarSize = new Size(200,200);


		final int RAND_INDEX = (int)(Math.random() * 9000 + 1000);
		String colorsString = "";
		for(int i = 0; i < 3; i++){
			for(int j = 0; j < 3; j++){
				colorsString += String.valueOf(colors[i][j]).substring(0,1);
			}
		}
		//String imgLabel = String.valueOf(resizedRect.x) + "," +String.valueOf(resizedRect.y) + "," +String.valueOf(resizedRect.width) + "," +String.valueOf(resizedRect.height) + "_" + String.valueOf(RAND_INDEX) + ".jpg";
		String imgLabel = colorsString + "_" + String.valueOf(RAND_INDEX) + ".jpg";

		Mat resized = new Mat();
		Imgproc.resize(img, resized, new Size(100,100));

		//Highgui.imread("labeledData/" + imgLabel,resized);
		Utils.saveMat(resized, "labeledData/" + imgLabel);
		System.out.println("saved:" + "labeledData/" + imgLabel);

	}

	static int framesNoColorUpdate = 0;
	static int framesNoColorUpdateThresh = 30;

	private void drawCubeFaces(Mat img) {
		//draw cube face on image

		final int PIECE_WIDTH = 15;
		final int FACE_WIDTH = PIECE_WIDTH * 4;
		for(CubeColor face : CubeColor.values()){
			//face = CubeColor.GREEN;

			for(int row = 0; row < 3; row++){
				for(int col = 0; col < 3; col++){
					//	System.out.println("B");
					CubeColor pieceColor = faceToColors.get(face)[row][col];
					double[] colorBgr = Utils.getColorBgr(pieceColor);
					for(int r = row * PIECE_WIDTH; r <= (row+1) * PIECE_WIDTH; r++){
						for(int c = col * PIECE_WIDTH; c <= (col+1) * PIECE_WIDTH; c++){

							img.put(r ,c + (FACE_WIDTH * CubeColor.index(face)) ,colorBgr);
						}
					}
				}
			}
		}
	}

	@FXML
	public void solveButtonClick(ActionEvent event) throws IOException{

		String solution = Utils.getCubeSolution(faceToColors);
		//System.out.println("solution:"  + solution);

		   final Stage dialog = new Stage();
           dialog.initModality(Modality.APPLICATION_MODAL);
           dialog.initOwner(primaryStage);
           VBox dialogVbox = new VBox(20);
           dialogVbox.getChildren().add(new Text("Solution:" + solution));
           Scene dialogScene = new Scene(dialogVbox, 500, 50);
           dialog.setScene(dialogScene);
           dialog.show();


	}



	public void resize(Mat img){
		//System.out.println(img.size());
//		final double WANTED_WIDTH = 500, WANTED_HEIGHT = 400;
//		double scaleX = WANTED_WIDTH / img.size().width,  scaleY = WANTED_HEIGHT / img.size().height;
//		double scale = Math.abs(scaleX - 1) <=  Math.abs(scaleY - 1) ? scaleX : scaleY;
//		Imgproc.resize(img, img, new Size(0,0), scale, scale, 1);

		final double WANTED_SIZE = 500 * 400;
		double currentSize = img.size().width * img.size().height;
		double scale = Math.sqrt(WANTED_SIZE / currentSize);
		Imgproc.resize(img, img, new Size(0,0), scale, scale, 1);


	}

	public List<List<Point>> getSquaresAreas(Mat img){
	//	System.out.println(img.size());
		//long t3 = System.currentTimeMillis();

		Mat edges = new Mat(img.size(), 0);
		Mat edgesCopy = new Mat(edges.size(), 0);
		edges.copyTo(edgesCopy);
		Imgproc.Canny(img, edges,50, 70);
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Mat hierarchy = new Mat(edges.size(), 0);
		Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
		for(MatOfPoint c : contours){
			List<MatOfPoint> cntrs = new ArrayList<MatOfPoint>();
			cntrs.add(c);
			Imgproc.drawContours(edges, cntrs, 0, new Scalar(255),5);

		}


		//imgDbg = edges;

		//System.out.println(edges.size());


		final int STEP_SIZE = 10;
		final int MAX_FLOOD_PIXELS = edges.rows() * edges.cols() / 9;
		final int MIN_FLOOD_PIXELS = edges.rows() * edges.cols() / 3000;
		List<List<int[]>> possibleAreas = new ArrayList<List<int[]>>();
		for(int row = 0; row < edges.rows(); row+=STEP_SIZE){
			for(int col = 0; col < edges.cols(); col+=STEP_SIZE){
				List<int[]> points = Utils.floodFill(edges, row, col, 1, MAX_FLOOD_PIXELS);
				if(points.size() >= MIN_FLOOD_PIXELS && points.size() <= MAX_FLOOD_PIXELS){
					possibleAreas.add(points);
				}

			}
		}


		//remove areas based on the straightness of the area's borders
		List<List<Point>> possibleRects = new ArrayList<List<Point>>();
		final double CUREVE_TOLERANCE = 2;
		for(List<int[]> areaPoints : possibleAreas){
			Borders borders = Borders.getBorders(areaPoints);
			double upMean = Utils.getDistFromMeanSlope(borders.up, true);
			double downMean = Utils.getDistFromMeanSlope(borders.down, true);
			double rightMean = Utils.getDistFromMeanSlope(borders.right, false);
			double leftMean = Utils.getDistFromMeanSlope(borders.left, false);


			if(upMean < CUREVE_TOLERANCE && downMean < CUREVE_TOLERANCE && rightMean < CUREVE_TOLERANCE && leftMean < CUREVE_TOLERANCE){
				possibleRects.add(Utils.toPointList(areaPoints));
			}

		}




		//remove areas based on their width - height ratio
		//Utils.getAvgWidthHeight(points)
		final double MAX_L2_WIDTH_HEIGHT = 1.5;
		List<List<Point>> toRemove = new ArrayList<List<Point>>();
		for(List<Point> points : possibleRects){
			if(Utils.getL2FromAvgWidth(points) > MAX_L2_WIDTH_HEIGHT || Utils.getL2FromAvgHeight(points) > MAX_L2_WIDTH_HEIGHT){
				toRemove.add(points);
			}else{
				//System.out.println("L2 distance: " + Utils.getL2FromAvgWidth(points) + "," + Utils.getL2FromAvgHeight(points));

			}
		}


		possibleRects.removeAll(toRemove);


		Mat edgesCpy2 = new Mat();
		edges.copyTo(edgesCpy2);

		for(List<Point> points : possibleRects){
			for(Point p : points){
				edgesCpy2.put(p.row, p.col, 127);
			}
		}
		imgDbg = edgesCpy2;

		if(possibleRects.size() < 3){ //return empty list if didn't recognize enough rectangles
			return new ArrayList<List<Point>>();
		}

	//	find mean center point of areas
		List<Point> centers = new ArrayList<Point>();
		for(List<Point> points : possibleRects){
			centers.add(Utils.getMeanPoint(points));
		}
		Point meanCenter = Utils.getMeanPoint(centers);
		for(int row = meanCenter.row - 10; row <= meanCenter.row + 10; row++){
			for(int col = meanCenter.col - 10; col <= meanCenter.col + 10; col++){

				edges.put(row, col, 175);
			}
		}

		double avgWidthHeight = 0;
		for(List<Point> points : possibleRects){
			avgWidthHeight += Utils.getAvgWidthHeight(points);
		}
		avgWidthHeight /= possibleRects.size();

		//remove points based on their distance from the mean center point

		final double DIST_TOL = 3;
		toRemove = new ArrayList<List<Point>>();
		List<double[]> indexToDist = new ArrayList<double[]>();
		for(int i = 0; i < possibleRects.size(); i++){
			List<Point> centersCopy = new ArrayList<Point>();
			for(Point p : centers){
				centersCopy.add(p);
			}
			final int iCopy = i;
			Collections.sort(centersCopy, new Comparator<Point>(){

				@Override
				public int compare(Point o1, Point o2) {
					double d1 = Utils.getDist(centers.get(iCopy), o1);
					double d2 = Utils.getDist(centers.get(iCopy), o2);
					return (int)(d1 - d2);
				}

			});
			double dis1 = Utils.getDist(centersCopy.get(1), centers.get(i));
			double dis2 = Utils.getDist(centersCopy.get(2), centers.get(i));
			if (dis1 > avgWidthHeight * DIST_TOL || dis2 > avgWidthHeight * DIST_TOL){
				//System.out.println("removing:" + i);
				toRemove.add(possibleRects.get(i));
			}

			indexToDist.add(new double[]{i, dis1+dis2});
			//System.out.println(i);
		}
		Collections.sort(indexToDist, new Comparator<double[]>() {

			@Override
			public int compare(double[] o1, double[] o2) {
				return (int)(o1[1] - o2[1]);
			}
		});


		for(int i = indexToDist.size()-1; i >= 9; i--){

			toRemove.add(possibleRects.get((int)indexToDist.get(i)[0]));
		}


		for(List<Point> area : possibleRects){
			for(Point p : area){
				edges.put(p.row, p.col, 127);
			}
		}

		possibleRects.removeAll(toRemove);
		imgDbg = edges;

		return possibleRects;

	}

	int calibrateColorIndex = 0; //current calibration color index
	boolean calibrating = false;
	@FXML
	public void calibrateButtonClick(ActionEvent e){
		System.out.println("calibrate");
		calibrating = true;
		calibrateColorIndex = 0;
		ColorDetector.initColorListBasicColors();
		calibrateButton.setText("Calibrating");
	}

	public static void showImage(Mat img){
		ImageView iv = new ImageView(Utils.mat2Image(img));
		BorderPane bp = new BorderPane(iv);
		Scene s = new Scene(bp);
		Stage st = new Stage();
		st.setScene(s);
		st.show();
	}

	public static void showImage(Mat img, EventHandler<? extends javafx.scene.input.MouseEvent> callback){
		ImageView iv = new ImageView(Utils.mat2Image(img));
		iv.setOnMouseClicked((EventHandler<? super javafx.scene.input.MouseEvent>) callback);
		BorderPane bp = new BorderPane(iv);
		Scene s = new Scene(bp);
		Stage st = new Stage();
		st.setScene(s);
		st.show();
	}



	/**
	 * Get a frame from the opened video stream (if any)
	 *
	 * @return the {@link Mat} to show
	 */
	private Mat grabFrame()
	{
		// init everything
		Mat frame = new Mat();

		// check if the capture is open
		if (this.capture.isOpened())
		{
			try
			{
				// read the current frame
				this.capture.read(frame);

				// if the frame is not empty, process it
				if (!frame.empty())
				{
					//Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
				}

			}
			catch (Exception e)
			{
				// log the error
				System.err.println("Exception during the image elaboration: " + e);
			}
		}

		return frame;
	}

	/**
	 * Stop the acquisition from the camera and release all the resources
	 */
	private void stopAcquisition()
	{
		if (this.timer!=null && !this.timer.isShutdown())
		{
			try
			{
				// stop the timer
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e)
			{
				// log any exception
				System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
			}
		}

		if (this.capture.isOpened())
		{
			// release the camera
			this.capture.release();
		}
	}

	/**
	 * Update the {@link ImageView} in the JavaFX main thread
	 *
	 * @param view
	 *            the {@link ImageView} to update
	 * @param image
	 *            the {@link Image} to show
	 */
	private void updateImageView(ImageView view, Image image)
	{
		Utils.onFXThread(view.imageProperty(), image);
	}

	/**
	 * On application close, stop the acquisition from the camera
	 */
	protected void setClosed()
	{
		this.stopAcquisition();
	}

}