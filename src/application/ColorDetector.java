package application;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import utils.Utils.CubeColor;
import utils.Utils.Tuple;


/**
 * Java Code to get a color name from rgb/hex value/awt color
 *
 * The part of looking up a color name from the rgb values is edited from
 * https://gist.github.com/nightlark/6482130#file-gistfile1-java (that has some errors) by Ryan Mast (nightlark)
 *
 * @author Xiaoxiao Li
 *
 */
public class ColorDetector {

	/**
	 * Initialize the color list that we have.
	 */
	private static ArrayList<ColorName> initColorList() {

		//try load serialized calibrated colors
		File f = new File("calibratedColors.ser");
		if(f.exists() && !f.isDirectory()) {
			System.out.println("getting calibrated data");
			FileInputStream fis;
			try {
				fis = new FileInputStream("calibratedColors.ser");
				ObjectInputStream ois = new ObjectInputStream(fis);
		        ArrayList<ColorName> arraylist = (ArrayList<ColorName>) ois.readObject();
		        ois.close();
		        fis.close();
		        System.out.println("returned calibrated data");
		        return arraylist;
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		ArrayList<ColorName> colorList = new ArrayList<ColorName>();
		colorList.add(new ColorName(CubeColor.BLUE,0x00,0x00,0xFF ));


		colorList.add(new ColorName(CubeColor.RED,0xFF,0x00,0x00 ));

		colorList.add(new ColorName(CubeColor.GREEN,0x00,0xFF,0x00 ));

		colorList.add(new ColorName(CubeColor.WHITE,0xFF,0xFF,0xFF ));

		colorList.add(new ColorName(CubeColor.ORANGE,0xff,0xA5,0x00 ));

		colorList.add(new ColorName(CubeColor.YELLOW,0xFF,0xFF,0x00 ));

		return colorList;
	}

	/**
	 * init color list with only basic bgr colors
	 */
	public static void initColorListBasicColors(){
		colorList = new ArrayList<ColorName>();
		colorList.add(new ColorName(CubeColor.BLUE,0x00,0x00,0xFF ));

		colorList.add(new ColorName(CubeColor.RED,0xFF,0x00,0x00 ));

		colorList.add(new ColorName(CubeColor.GREEN,0x00,0xFF,0x00 ));

		colorList.add(new ColorName(CubeColor.WHITE,0xFF,0xFF,0xFF ));

		colorList.add(new ColorName(CubeColor.ORANGE,0xff,0xA5,0x00 ));

		colorList.add(new ColorName(CubeColor.YELLOW,0xFF,0xFF,0x00 ));
	}

	public static ArrayList<ColorName> colorList = null;
	/**
	 * Get the closest color name from our list
	 *
	 * @param r
	 * @param g
	 * @param b
	 * @return
	 */
	public static Tuple<CubeColor,Double> getColorNameFromRgb(int r, int g, int b) {
		if(colorList == null){
			colorList = initColorList();
		}
		ColorName closestMatch = null;
		double minMSE = Double.MAX_VALUE;
		int mse;
		for (ColorName c : colorList) {
			mse = c.computeMSE(r, g, b);
			if (mse < minMSE) {
				minMSE = mse;
				closestMatch = c;
			}
		}

		if (closestMatch != null) {
			return new Tuple<CubeColor,Double>(closestMatch.getName(), minMSE);
		} else {
			return  new Tuple<CubeColor,Double>(CubeColor.WHITE, Double.MAX_VALUE);
		}
	}

	/**
	 * Convert hexColor to rgb, then call getColorNameFromRgb(r, g, b)
	 *
	 * @param hexColor
	 * @return
	 */
	public static CubeColor getColorNameFromHex(int hexColor) {
		int r = (hexColor & 0xFF0000) >> 16;
		int g = (hexColor & 0xFF00) >> 8;
		int b = (hexColor & 0xFF);
		return getColorNameFromRgb(r, g, b).first;
	}

	/**
	 * SubClass of ColorUtils. In order to lookup color name
	 *
	 * @author Xiaoxiao Li
	 *
	 */
	public static class ColorName implements java.io.Serializable{
		public int r, g, b;
		public CubeColor name;

		public ColorName(CubeColor name, int r, int g, int b) {
			this.r = r;
			this.g = g;
			this.b = b;
			this.name = name;
		}

		public int computeMSE(int pixR, int pixG, int pixB) {

//			//convert to hsb
//			float[] hsb1 = new float[3], hsb2 = new float[3];
//			Color.RGBtoHSB(r, g, b, hsb1);
//			Color.RGBtoHSB(pixR, pixG, pixB, hsb2);
//
//			return (int) (((hsb1[0] - hsb2[0]) * (hsb1[0] - hsb2[0]) + (hsb1[1] - hsb2[1]) * (hsb1[1] - hsb2[1]) + (hsb1[2] - hsb2[2])
//					* (hsb1[2] - hsb2[2])) / 3.0);

			return (int) (((pixR - r) * (pixR - r) + (pixG - g) * (pixG - g) + (pixB - b)
					* (pixB - b)) / 3.0);
		}

		public int getR() {
			return r;
		}

		public int getG() {
			return g;
		}

		public int getB() {
			return b;
		}

		public CubeColor getName() {
			return name;
		}
	}
}