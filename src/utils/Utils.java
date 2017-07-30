package utils;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.imageio.ImageIO;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import application.ColorDetector;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import rubiks_solver.Search;


public final class Utils
{

	public static enum CubeColor{
		BLUE, ORANGE, GREEN, RED, YELLOW, WHITE;

		public static int index(CubeColor c){
			List<CubeColor> colors = new ArrayList<CubeColor>();
			colors.add(BLUE);
			colors.add(ORANGE);
			colors.add(GREEN);
			colors.add(RED);
			colors.add(YELLOW);
			colors.add(WHITE);
			return colors.indexOf(c);
		}
		public static CubeColor getColor(int index){
			List<CubeColor> colors = new ArrayList<CubeColor>();
			colors.add(BLUE);
			colors.add(ORANGE);
			colors.add(GREEN);
			colors.add(RED);
			colors.add(YELLOW);
			colors.add(WHITE);
			return colors.get(index);
		}

	}

	public static Map<CubeColor, double[]> colorToBgr;

	public static Map<double[], CubeColor> cordsToColor;

	/**
	 * Convert a Mat object (OpenCV) in the corresponding Image for JavaFX
	 *
	 * @param frame
	 *            the {@link Mat} representing the current frame
	 * @return the {@link Image} to show
	 */
	public static Image mat2Image(Mat frame)
	{
		try
		{
			return SwingFXUtils.toFXImage(matToBufferedImage(frame), null);
		}
		catch (Exception e)
		{
			System.err.println("Cannot convert the Mat obejct: " + e);
			return null;
		}
	}

	public static String cubeToStringEncoding(Map<CubeColor, CubeColor[][]> faceToColors){

		//faces order:  URFDLB
		String[] sides = new String[]{"U","R","F","D","L","B"};
		CubeColor[] colors = new CubeColor[]{CubeColor.YELLOW, CubeColor.RED,CubeColor.BLUE,CubeColor.WHITE,CubeColor.ORANGE,CubeColor.GREEN};
		Map<CubeColor, String> faceToSide = new HashMap();
		for(int i = 0; i < sides.length; i++){
			faceToSide.put(colors[i], sides[i]);
		}

		String encoding = "";
		for(CubeColor c : colors){
			CubeColor[][] faceColors = faceToColors.get(c);
			for(int row = 0; row < 3; row++){
				for(int col = 0; col < 3; col++){
					encoding += faceToSide.get(faceColors[row][col]);
				}

			}
		}

		return encoding;
	}

	public static void saveMat(Mat mat, String path){
	//	Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY, 0);

		// Create an empty image in matching format
		BufferedImage bi = matToBufferedImage(mat);

		//save bufferedImage to disk
		File outputfile = new File(path);
		try {
			ImageIO.write(bi, "jpg", outputfile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static String getCubeSolution(Map<CubeColor, CubeColor[][]> faceToColors){
		String encoding = Utils.cubeToStringEncoding(faceToColors);


		return new Search().solution(encoding, 23, 100, 0, 0);

	}

	public static boolean sameColors(CubeColor[][] one, CubeColor[][] two){
		if(one.length != two.length)
			return false;
		for(int i = 0; i < one.length; i++){
			if(one[i].length != two[i].length)
				return false;
			for(int j = 0; j < one[i].length; j++){
				if(one[i][j] != two[i][j]){
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Generic method for putting element running on a non-JavaFX thread on the
	 * JavaFX thread, to properly update the UI
	 *
	 * @param property
	 *            a {@link ObjectProperty}
	 * @param value
	 *            the value to set for the given {@link ObjectProperty}
	 */
	public static <T> void onFXThread(final ObjectProperty<T> property, final T value)
	{
		Platform.runLater(() -> {
			property.set(value);
		});
	}




	/**
	 * flood fills given matrix, in place
	 * @param mat matrix to flood fill
	 * @param row start row
	 * @param col start col
	 * @param MAX_PIXELS maximal flood size, if flood area is greater than MAX_PIXELS - will not flood at all
	 * @return
	 */
	public static List<int[]> floodFill(Mat mat, int row, int col, int fillValue, final int MAX_FLOOD_PIXELS){
		Queue<int[]> q = new LinkedList<int[]>();
		q.add(new int[]{row, col});
		List<int[]> points = new ArrayList<int[]>();
		while(!q.isEmpty()){
			if(points.size() >= MAX_FLOOD_PIXELS){
				return new ArrayList<int[]>();
			}
			int[] pos = q.remove();
			row = pos[0];
			col = pos[1];
			if(row < 0 || row >= mat.rows() || col < 0 || col >= mat.cols() || mat.get(row, col)[0] != 0){
				continue;
			}
			mat.put(row, col, fillValue);

			points.add(new int[]{row,col});
			q.add(new int[]{row+1, col});
			q.add(new int[]{row-1, col});
			q.add(new int[]{row, col+1});
			q.add(new int[]{row, col-1});

		}

		return points;
	}

	private static void putIfOk(Queue<int[]> queue, Mat mat, int x, int y){
		if (x < 0  || x >= mat.rows() || y < 0 || y >= mat.cols() || mat.get(x, y)[0] != 0){
			return;
		}
		//System.out.println(mat.get(x, y)[0]);
		queue.add(new int[]{x,y});
	}

	/**
	 * Support for the {@link mat2image()} method
	 *
	 * @param original
	 *            the {@link Mat} object in BGR or grayscale
	 * @return the corresponding {@link BufferedImage}
	 */
	private static BufferedImage matToBufferedImage(Mat original)
	{
		// init
		BufferedImage image = null;
		int width = original.width(), height = original.height(), channels = original.channels();
		byte[] sourcePixels = new byte[width * height * channels];
		original.get(0, 0, sourcePixels);
	//	System.out.println(original.channels());
		if (original.channels() > 1)
		{
			image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		}
		else
		{
			image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		}
		final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);

		return image;
	}


	public static double median(double[] nums){
		Arrays.sort(nums);
		return nums[nums.length/2];
	}

	public static double average(double[] nums){
		double sum = 0.0;
		for(double num : nums){
			sum += num;
		}
		return sum / nums.length;
	}

	public static List<Point> toPointList(List<int[]> pointsArrs){
		List<Point> pointsObjs = new ArrayList<Point>();
		for(int[] p : pointsArrs){
			pointsObjs.add(new Point(p[0],p[1]));
		}
		return pointsObjs;
	}

	public static double getRSquared(List<Point> points){
		double sumX = 0, sumY = 0, sumXY = 0, sumXSquares = 0, sumYSquares = 0;
		for(Point p : points){
			sumX += p.row;
			sumXSquares += p.row * p.row;
			sumY += p.col;
			sumYSquares += p.col * p.col;
			sumXY += p.row * p.col;

		}
		int n = points.size();
		return Math.pow((n*sumXY - sumX * sumY) / Math.sqrt((n * sumXSquares - (sumX*sumX))*(n * sumYSquares - (sumY*sumY))) , 2);

	}

	public static double getDistFromMeanSlope(List<Point> points, boolean horizontal){
		final int VERTICAL_SLOPE_CONST = 0;
		Point meanPoint = new Point(0,0);
		for(Point p : points){
			meanPoint.row += p.row;
			meanPoint.col += p.col;
		}
		meanPoint.row /= points.size();
		meanPoint.col /= points.size();

		//System.out.println("mean point:" + meanPoint);

		double slopesSum = 0;
		int numInSum = points.size();
		for(Point p : points){
			if(p.row == meanPoint.row && p.col == meanPoint.col){
				numInSum --;
				continue;
			}
			if(horizontal){
				if(p.col == meanPoint.col){
					slopesSum += VERTICAL_SLOPE_CONST;
					continue;
				}
				slopesSum += Math.abs(((double)(p.row - meanPoint.row)) /(p.col - meanPoint.col));
			}else{
				if(p.row == meanPoint.row){
					slopesSum += VERTICAL_SLOPE_CONST;
					continue;
				}
				slopesSum += Math.abs(((double)(p.col - meanPoint.col)) /(p.row - meanPoint.row));
			}
		}
		double meanSlope = slopesSum /numInSum;

	//	System.out.println("mean slope:" + meanSlope);

		double distsFromMeanSlopeSum = 0;
		numInSum = points.size();
		for(Point p : points){
			if(p.row == meanPoint.row && p.col == meanPoint.col){
				numInSum --;
				continue;
			}
			double slope = 0;
			if(horizontal){
				if(p.col == meanPoint.col){
					slope += VERTICAL_SLOPE_CONST;
				}else{
					slope = Math.abs(((double)(p.row - meanPoint.row)) /(p.col - meanPoint.col));
				}
			}else{
				if(p.row == meanPoint.row){
					slope += VERTICAL_SLOPE_CONST;
				}else{
					slope = Math.abs(((double)(p.col - meanPoint.col)) /(p.row - meanPoint.row));
				}
			}

			distsFromMeanSlopeSum += Math.abs(slope - meanSlope);
		}

		return distsFromMeanSlopeSum / numInSum;

	}

	public static Point getMeanPoint(List<Point> points){
		Point sum = new Point(0,0);
		for(Point p : points){
			sum.row += p.row;
			sum.col += p.col;
		}
		sum.row /= points.size();
		sum.col /= points.size();
		return sum;
	}

	public static double getAvgWidthHeight(List<Point> points){
		double minRow = Integer.MAX_VALUE, maxRow = Integer.MIN_VALUE;
		double minCol = Integer.MAX_VALUE, maxCol = Integer.MIN_VALUE;
		for(Point p : points){
			minRow = Math.min(minRow, p.row);
			maxRow = Math.max(maxRow, p.row);
			minCol = Math.min(minCol, p.col);
			maxCol = Math.max(maxCol, p.col);
		}

		return ((maxRow - minRow) + (maxCol - minCol))/2.0;

	}

	public static double getWidth(List<Point> points){
		double minCol = Integer.MAX_VALUE, maxCol = Integer.MIN_VALUE;
		for(Point p : points){
			minCol = Math.min(minCol, p.col);
			maxCol = Math.max(maxCol, p.col);
		}

		return (maxCol - minCol);
	}

	public static double getHeight(List<Point> points){
		double minRow = Integer.MAX_VALUE, maxRow = Integer.MIN_VALUE;
		for(Point p : points){
			minRow = Math.min(minRow, p.row);
			maxRow = Math.max(maxRow, p.row);

		}

		return (maxRow - minRow);
	}

	public static double getL2FromAvgWidth(List<Point> points){
		Set<Point> pointsXY = new HashSet<Point>();
		for(Point p : points){
			pointsXY.add(new Point(p.row,p.col));
		}


		int minRow = Integer.MAX_VALUE, maxRow = Integer.MIN_VALUE;
		for(Point loc : pointsXY){

			minRow = Math.min(loc.row, minRow);
			maxRow = Math.max(loc.row, maxRow);
		}
		List<Integer> widths = new ArrayList<Integer>();
		double widthsSum = 0;
		final int MAX_ROW_WIDTH = 5;
		//loop through rows
		for(int row = minRow; row <= maxRow; row++){
			//System.out.println("new row");
			//find width of current row
			int minCol = Integer.MAX_VALUE, maxCol = Integer.MIN_VALUE;
			for(Point loc : pointsXY){
				//System.out.println(loc[0]);
				//System.out.println(row);
				if(Math.abs(row - loc.row) > MAX_ROW_WIDTH)
					continue;
				minCol = Math.min(loc.col, minCol);
				maxCol = Math.max(loc.col, maxCol);

			}
			widths.add(maxCol - minCol);
		//	System.out.println(maxCol - minCol);
			widthsSum += maxCol - minCol;
		}
		//calc average row width
		double avgWidth = widthsSum / widths.size();
	//	System.out.println(avgWidth);
		double deviationsL2 = 0;
		//calculate L2 distance from average row width
		for(Integer w : widths){
			deviationsL2 +=Math.pow(Math.abs(w - avgWidth), 2);
		}
		return deviationsL2 / widths.size(); // return normalized L2 distance from row width
	}

	public static double getL2FromAvgHeight(List<Point> points){

		Set<Point> pointsXY = new HashSet<Point>();
		for(Point p : points){
			pointsXY.add(new Point(p.row,p.col));
		}


		int minCol = Integer.MAX_VALUE, maxCol = Integer.MIN_VALUE;
		for(Point loc : pointsXY){

			minCol = Math.min(loc.col, minCol);
			maxCol= Math.max(loc.col, maxCol);
		}
		List<Integer> heights = new ArrayList<Integer>();
		double heightsSum = 0;
		final int MAX_ROW_HEIGHT = 5;
		//loop through rows
		for(int col = minCol; col <= maxCol; col++){
			//System.out.println("new row");
			//find width of current row
			int minRow = Integer.MAX_VALUE, maxRow= Integer.MIN_VALUE;
			for(Point loc : pointsXY){
				//System.out.println(loc[0]);
				//System.out.println(row);
				if(Math.abs(col - loc.col) > MAX_ROW_HEIGHT)
					continue;
				minRow = Math.min(loc.row, minRow);
				maxRow = Math.max(loc.row, maxRow);

			}
			heights.add(maxRow - minRow);
		//	System.out.println(maxCol - minCol);
			heightsSum += maxRow - minRow;
		}
		//calc average row width
		double avgHeight = heightsSum / heights.size();
	//	System.out.println(avgWidth);
		double deviationsL2 = 0;
		//calculate L2 distance from average row width
		for(Integer h : heights){
			deviationsL2 +=Math.pow(Math.abs(h - avgHeight), 2);
		}
		return deviationsL2 / heights.size(); // return normalized L2 distance from row width
	}



	public static double getDist(Point p1, Point p2){
		return Math.sqrt(Math.pow(p1.row - p2.row, 2) + Math.pow(p1.col - p2.col, 2));
	}
	/**
	 *  gets a sequence of pairs of real numbers and computes the
	 *  best fit (least squares) line y  = ax + b through the set of points.
	 *  Also computes the correlation coefficient and the standard errror
	 *  of the regression coefficients.
	 */
	public static double getR2Error(List<Point> points){
		  int MAXN = 1000;
	        int n = 0;
	        double[] x = new double[points.size()];
	        double[] y = new double[points.size()];



	        // first pass: read in data, compute xbar and ybar
	        double sumx = 0.0, sumy = 0.0, sumx2 = 0.0;
	        for(int i = 0; i < x.length; i++) {
	        	x[i] = points.get(i).row;
	        	y[i]= points.get(i).col;
	            sumx  += x[i];
	            sumx2 += x[i] * x[i];
	            sumy  += y[i];
	            n++;
	        }
	        double xbar = sumx / n;
	        double ybar = sumy / n;

	        // second pass: compute summary statistics
	        double xxbar = 0.0, yybar = 0.0, xybar = 0.0;
	        for (int i = 0; i < n; i++) {
	            xxbar += (x[i] - xbar) * (x[i] - xbar);
	            yybar += (y[i] - ybar) * (y[i] - ybar);
	            xybar += (x[i] - xbar) * (y[i] - ybar);
	        }
	        double beta1 = xybar / xxbar;
	        double beta0 = ybar - beta1 * xbar;

	        // print results
	    //    System.out.println("y   = " + beta1 + " * x + " + beta0);

	        // analyze results
	        int df = n - 2;
	        double rss = 0.0;      // residual sum of squares
	        double ssr = 0.0;      // regression sum of squares
	        for (int i = 0; i < n; i++) {
	            double fit = beta1*x[i] + beta0;
	            rss += (fit - y[i]) * (fit - y[i]);
	            ssr += (fit - ybar) * (fit - ybar);
	        }
	        double R2    = ssr / yybar;
	        double svar  = rss / df;
	        double svar1 = svar / xxbar;
	        double svar0 = svar/n + xbar*xbar*svar1;

	        return R2;

	   }

	public static double[] getColorBgr(CubeColor color){
		if(colorToBgr == null){
			initColorToBgr();
		}
		return colorToBgr.get(color);
	}

	public static CubeColor getClosestColor(Mat img, int row, int col){
		//System.out.println("b" + Arrays.toString(img.get(row, col)));
		return getClosestColor(img.get(row, col)).first;
	}

	/**
	 *
	 * @param bgr
	 * @return (closestColor, distance)
	 */
	public static Tuple<CubeColor, Double> getClosestColor(double[] bgr){
		return ColorDetector.getColorNameFromRgb((int)bgr[2], (int)bgr[1],(int)bgr[0]);

	}


	private static void initColorToBgr() {
		colorToBgr = new HashMap<CubeColor, double[]>();
		colorToBgr.put(CubeColor.WHITE,new double[]{255,255,255});
		colorToBgr.put(CubeColor.RED, new double[]{0,0,255});
		colorToBgr.put(CubeColor.YELLOW, new double[]{0,255,255});
		colorToBgr.put(CubeColor.BLUE, new double[]{255,0,0});
		colorToBgr.put(CubeColor.ORANGE, new double[]{0,127,255});
		colorToBgr.put(CubeColor.GREEN, new double[]{0,255,0});

	}

	public static double[] getHsvXYZcords(Mat img, int row, int col){
		double[] bgr = img.get(row, col);
		Mat src = new Mat(img, new org.opencv.core.Rect(col, row, 1, 1));
		Mat dst = new Mat(img, new org.opencv.core.Rect(col, row, 1, 1));

		return bgrToHsvCords(bgr);

	}

	public static double getHue(double[] bgr){
		double B = bgr[0], G = bgr[1], R = bgr[2];
		double M = Math.max(Math.max(bgr[0], bgr[1]), bgr[2]);
		double m = Math.min(Math.min(bgr[0], bgr[1]), bgr[2]);
		double C = M - m;

		if(C == 0)
			return 0;

		if(M == R){
			return (((G-B)/C)%6) * 60;
		}
		if (M == G){
			return (((B - R)/C)+2) * 60;
		}
		if(M == B){
			return (((R-G)/C)+4)*60;
		}
		return 0;
	}



	public static double[] bgrToHsvCords(double[] bgr){
		double chroma = Math.max(Math.max(bgr[0], bgr[1]), bgr[2]) - Math.min(Math.min(bgr[0], bgr[1]), bgr[2]);
		double light = (bgr[0] + bgr[1] + bgr[2]) / 3.0;
		double H = getHue(bgr) / 180.0; // 0 <= h <= 2
		double C = (chroma / 510) + 255; // 0 <= c <= 1

		return new double[]{C * Math.cos(H * Math.PI),C * Math.cos(H * Math.PI), light};
	}

	public static class Borders{
		public List<Point> up,right,down,left;

		public static Borders getBorders(List<int[]> areaPoints){

			Set<Point> pointsSet = new HashSet<Point>();

			for(int[] p : areaPoints){
				pointsSet.add(new Point(p[0],p[1]));
			}

			List<Point> upBorder = new ArrayList<Point>();
			List<Point> rightBorder = new ArrayList<Point>();
			List<Point> downBorder = new ArrayList<Point>();
			List<Point> leftBorder = new ArrayList<Point>();
			for(Point p : pointsSet){
				if(!pointsSet.contains(new Point(p.row-1,p.col))){
					upBorder.add(p);
				}if(!pointsSet.contains(new Point(p.row,p.col+1))){
					rightBorder.add(p);
				}if(!pointsSet.contains(new Point(p.row+1,p.col))){
					downBorder.add(p);
				}
				if(!pointsSet.contains(new Point(p.row,p.col-1))){
					leftBorder.add(p);
				}
			}

			return new Borders(upBorder, rightBorder, downBorder, leftBorder);

		}

		public static double[] getRowColMeanDistFromMean(List<Point> points){
			double sumRow = 0, sumCol = 0;
			for(Point p : points){
				sumRow += p.row;
				sumCol += p.col;
			}
			double meanRow = sumRow / points.size();
			double meanCol = sumCol / points.size();

			double sumRowMeanDist = 0, sumColMeanDist = 0;
			for(Point p : points){
				sumRowMeanDist += Math.abs(p.row - meanRow);
				sumColMeanDist += Math.abs(p.col - meanCol);
			}
			return new double[]{sumRowMeanDist / points.size(), sumColMeanDist / points.size()};

		}

		public static double[] getRowColMaxDistFromMean(List<Point> points){
			double sumRow = 0, sumCol = 0;
			for(Point p : points){
				sumRow += p.row;
				sumCol += p.col;
			}
			double meanRow = sumRow / points.size();
			double meanCol = sumCol / points.size();

			double maxRowDist = 0, maxColDist = 0;
			for(Point p : points){
				maxRowDist = Math.max(maxRowDist, Math.abs(p.row - meanRow));
				maxColDist = Math.max(maxColDist, Math.abs(p.col - meanCol));
			}
			return new double[]{maxRowDist, maxColDist};

		}

		private Borders(List<Point> up,List<Point> right,List<Point> down,List<Point> left){
			this.up = up;
			this.right = right;
			this.down = down;
			this.left = left;
		}



	}

	public static class Tuple<T,S>{
		public T first;
		public S second;

		public Tuple(T first, S second){
			this.first = first;
			this.second = second;
		}
	}

	public static class Point{
		public int row, col;
		public Point(int row, int col){
			this.row = row;
			this.col = col;
		}

		@Override
		public int hashCode(){
			int result = row;
			result = 31 * result + col;
			return result;
		}

		@Override
		public boolean equals(Object o){
			if(o == null )
				return false;
			if(!(o instanceof Point))
					return false;

			Point otherP = (Point) o;
			return row == otherP.row && col == otherP.col;

		}

		public String toString(){
			return "[" + row + "," + col + "]";
		}

	}

	public static class Rect{
		private static final double SQUARE_TRESH = .05;
		public int x, y, width, height;

		public Rect(int x1, int y1, int x2, int y2){
			x = x1;
			y = y1;
			width = x2 - x1;
			height = y2 - y1;
		}

		public void stretch(int amount){
			x -= amount;
			y -= amount;
			width += amount;
			height += amount;
		}

		public boolean isSquare(){
			return (Math.abs(((double)width/height) - 1)) < SQUARE_TRESH;
		}

		public void Squarify() {

			width = Math.max(width, height);
			height = Math.max(width, height);

		}

		public double getMaxWidthHeightRatio() {
			double d_w = width;
			double d_h = height;
			return d_w>d_h ? d_w / d_h : d_h / d_w;
		}

	}

}
