package utils;

public class LinearRegression {

	/******************************************************************************
	 *  Compilation:  javac LinearRegression.java StdIn.java
	 *  Execution:    java LinearRegression < data.txt
	 *
	 *  Reads in a sequence of pairs of real numbers and computes the
	 *  best fit (least squares) line y  = ax + b through the set of points.
	 *  Also computes the correlation coefficient and the standard errror
	 *  of the regression coefficients.
	 *
	 *  Note: the two-pass formula is preferred for stability.
	 *
	 ******************************************************************************/


	    public static void main(String[] args) {
	        int MAXN = 1000;
	        int n = 0;
	        double[] x = new double[]{0,1,2,3,4};
	        double[] y = new double[]{0,1,2,3,5};



	        // first pass: read in data, compute xbar and ybar
	        double sumx = 0.0, sumy = 0.0, sumx2 = 0.0;
	        for(int i = 0; i < x.length; i++) {
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
	        System.out.println("y   = " + beta1 + " * x + " + beta0);

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
	        System.out.println("R^2                 = " + R2);
	        System.out.println("std error of beta_1 = " + Math.sqrt(svar1));
	        System.out.println("std error of beta_0 = " + Math.sqrt(svar0));
	        svar0 = svar * sumx2 / (n * xxbar);
	        System.out.println("std error of beta_0 = " + Math.sqrt(svar0));

	        System.out.println("SSTO = " + yybar);
	        System.out.println("SSE  = " + rss);
	        System.out.println("SSR  = " + ssr);
	    }
	}


