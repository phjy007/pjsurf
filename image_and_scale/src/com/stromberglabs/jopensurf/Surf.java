package com.stromberglabs.jopensurf;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

public class Surf implements Serializable {
	private static final long 	serialVersionUID 		= 1L;
	
	private static final int 	HESSIAN_OCTAVES 		= 5;
	//private static final int HESSIAN_INTERVALS = 4;
	private static final int 	HESSIAN_INIT_SAMPLE 	= 2;
	private static final float 	HESSIAN_THRESHOLD 		= 0.0004F;
	private static final float 	HESSIAN_BALANCE_VALUE 	= 0.81F;
	
	private transient BufferedImage mOriginalImage;
	private FastHessian mHessian;
	private List<SURFInterestPoint> mFreeOrientedPoints;
	private List<SURFInterestPoint> mUprightPoints;
	private List<SURFInterestPoint> mDescriptorFreeInterestPoints;
	
	//octaves = steps
	private int mNumOctaves 	= HESSIAN_OCTAVES;
	//private int mNumIntervals = HESSIAN_INTERVALS;
	private float mThreshold 	= HESSIAN_THRESHOLD;
	private float mBalanceValue = HESSIAN_BALANCE_VALUE;
	
	private IntegralImage mIntegralImage;
	
	private ExecutorService exec_pool;
	
	private final int getDescriptionThreadNum;
	
	private final boolean getPointConcurrency;
	
	public Surf(BufferedImage image, int getDescriptionThreadNumber, boolean getIPointConcurrency) {
		this(image,HESSIAN_BALANCE_VALUE,HESSIAN_THRESHOLD,HESSIAN_OCTAVES, getDescriptionThreadNumber, getIPointConcurrency);
	}
	
	public Surf(BufferedImage image, float balanceValue, float threshold, int octaves, int getDescriptionThreadNumber, boolean getIPointConcurrency) {
		mOriginalImage 	= image;
		mNumOctaves 	= octaves;
		mBalanceValue 	= balanceValue;
		mThreshold 		= threshold;
		exec_pool = Executors.newCachedThreadPool();
		getDescriptionThreadNum = getDescriptionThreadNumber;
		getPointConcurrency = getIPointConcurrency;
		
		mIntegralImage = new IntegralImage(mOriginalImage);
		
		mHessian = new FastHessian(mIntegralImage, mNumOctaves, HESSIAN_INIT_SAMPLE, mThreshold, mBalanceValue, exec_pool, this, getDescriptionThreadNum, getPointConcurrency);
		
		mDescriptorFreeInterestPoints = mHessian.getIPoints();
	}
	
	
	
	
	
	public List<SURFInterestPoint> getUprightInterestPoints() {
		return getPoints(true);
	}
	
	public List<SURFInterestPoint> getFreeOrientedInterestPoints() {
		return getPoints(false);
	}
	
	private List<SURFInterestPoint> getPoints(boolean upright) {
		List<SURFInterestPoint> points = upright ? mUprightPoints : mFreeOrientedPoints;
		if ( points == null ) {
			points = getDescriptorFreeInterestPoints();
			if (upright) {
				mUprightPoints = points;
			} else {
				mFreeOrientedPoints = points;
			}
			for ( SURFInterestPoint point : points ) {
				getOrientation(point);
				getMDescriptor(point, upright);
			}
		}
		return points;
	}
	
	private List<SURFInterestPoint> getDescriptorFreeInterestPoints() {
		List<SURFInterestPoint> points = new ArrayList<SURFInterestPoint>(mDescriptorFreeInterestPoints.size());
		for ( SURFInterestPoint point : mDescriptorFreeInterestPoints ) {
			try {
				points.add((SURFInterestPoint)point.clone());
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
		}
		return points;
	}
	
	private float haarX(int row, int column, int s){
		return ImageTransformUtils.BoxIntegral(mIntegralImage, row-s/2, column, s, s/2)
			-1 * ImageTransformUtils.BoxIntegral(mIntegralImage, row-s/2, column-s/2, s, s/2);
	}

	private float haarY(int row, int column, int s){
		return ImageTransformUtils.BoxIntegral(mIntegralImage, row, column-s/2, s/2, s)
			-1 * ImageTransformUtils.BoxIntegral(mIntegralImage, row-s/2, column-s/2, s/2, s);
	}
	
	public void getOrientation(SURFInterestPoint input){
		double gauss;
		float scale = input.getScale();
		int s = (int)Math.round(scale);
		int r = (int)Math.round(input.getY());
		int c = (int)Math.round(input.getX());
		List<Double> xHaarResponses = new ArrayList<Double>();
		List<Double> yHaarResponses = new ArrayList<Double>();
		List<Double> angles = new ArrayList<Double>();
		for(int i = -6; i <= 6; ++i) {
			for(int j = -6; j <= 6; ++j){
				if(i*i + j*j < 36){
					gauss = GaussianConstants.Gauss25[Math.abs(i)][Math.abs(j)];
					double xHaarResponse = gauss * haarX(r+j*s, c+i*s, 4*s);
					double yHaarResponse = gauss * haarY(r+j*s, c+i*s, 4*s);
					xHaarResponses.add(xHaarResponse);
					yHaarResponses.add(yHaarResponse);
					angles.add(getAngle(xHaarResponse,yHaarResponse));
				}
			}
		}
		float sumX = 0, sumY = 0;
		float ang1, ang2, ang;
		float max = 0;
		float orientation = 0;
		
		for(ang1 = 0; ang1 < 2*Math.PI;  ang1+=0.15f) {
			ang2 = (float)( ang1+Math.PI/3.0f > 2*Math.PI ? ang1-5.0f*Math.PI/3.0f : ang1+Math.PI/3.0f );
			sumX = sumY = 0;
			for ( int k = 0; k < angles.size(); k++ ) {
				ang = angles.get(k).floatValue();
				
				if (ang1 < ang2 && ang1 < ang && ang < ang2) {
					sumX += xHaarResponses.get(k).floatValue();
					sumY += yHaarResponses.get(k).floatValue();
				} else if (ang2 < ang1 && ((ang > 0 && ang < ang2) || (ang > ang1 && ang < 2*Math.PI) )) {
					sumX += xHaarResponses.get(k).floatValue();
					sumY += yHaarResponses.get(k).floatValue();
				}
			}
		    if (sumX*sumX + sumY*sumY > max) {
		      max = sumX*sumX + sumY*sumY;
		      orientation = (float)getAngle(sumX, sumY);
		    }
		}
		input.setOrientation(orientation);
	}
	
	public void getMDescriptor(SURFInterestPoint point, boolean upright) {
		  int y, x, count=0;
		  int sample_x, sample_y;
		  double scale, dx, dy, mdx, mdy, co = 1F, si = 0F;
		  float desc[] = new float[64];
		  double gauss_s1 = 0.0D, gauss_s2 = 0.0D, xs = 0.0D, ys = 0.0D;
		  double rx = 0.0D, ry = 0.0D, rrx = 0.0D, rry = 0.0D, len = 0.0D;
		  int i = 0, ix = 0, j = 0, jx = 0;
		  
		  float cx = -0.5f, cy = 0.0f;
		  
		  scale = point.getScale();
		  x = Math.round(point.getX());
		  y = Math.round(point.getY());
		  if ( !upright ){
			  co = Math.cos(point.getOrientation());
			  si = Math.sin(point.getOrientation());
		  }
		  i = -8;
		  while ( i < 12 ) {
			  j = -8;
			  i = i - 4;
			  cx += 1.0F;
			  cy = -0.5F;
			  while ( j < 12 ) {
				  dx=dy=mdx=mdy=0.0F;
				  cy += 1.0F;
				  j = j - 4;
				  ix = i + 5;
				  jx = j + 5;
				  xs = Math.round(x + ( -jx*scale*si + ix*scale*co));
				  ys = Math.round(y + ( jx*scale*co + ix*scale*si));
				  for (int k = i; k < i + 9; ++k) {
					  for (int l = j; l < j + 9; ++l) {
						  sample_x = (int)Math.round(x + (-1D * l * scale * si + k * scale * co));
						  sample_y = (int)Math.round(y + (      l * scale * co + k * scale * si));
						  gauss_s1 = gaussian(xs-sample_x,ys-sample_y,2.5F*scale);
						  
						  rx = haarX(sample_y, sample_x, (int)(2*Math.round(scale)));
						  ry = haarY(sample_y, sample_x, (int)(2*Math.round(scale)));
						  
						  rrx = gauss_s1 * (-rx*si + ry*co);
						  rry = gauss_s1 * (rx*co + ry*si);
						  
						  dx += rrx;
						  dy += rry;
						  
						  mdx += Math.abs(rrx);
						  mdy += Math.abs(rry);
					  }
				  }
				  gauss_s2 = gaussian(cx-2.0f,cy-2.0f,1.5f);
				  desc[count++] = (float)(dx*gauss_s2);
				  desc[count++] = (float)(dy*gauss_s2);
				  desc[count++] = (float)(mdx*gauss_s2);
				  desc[count++] = (float)(mdy*gauss_s2);
				  len += (dx*dx + dy*dy + mdx*mdx + mdy*mdy) * (gauss_s2 * gauss_s2);
				  j += 9;
			  }
			  i += 9;
		  }
		  len = Math.sqrt(len);
		  for(i = 0; i < 64; i++)
			  desc[i] /= len;
		  point.setDescriptor(desc);
//		  for ( double v : desc ){
//			  System.out.printf("%.7f",v);
//			  System.out.print(",");
//		  }
//		  System.out.println();
	}
	
	private double getAngle(double xHaarResponse, double yHaarResponse) {
		if(xHaarResponse >= 0 && yHaarResponse >= 0)
			return Math.atan(yHaarResponse/xHaarResponse);
		if(xHaarResponse < 0 && yHaarResponse >= 0)
			return Math.PI - Math.atan(-yHaarResponse/xHaarResponse);
		if(xHaarResponse < 0 && yHaarResponse < 0)
			return Math.PI + Math.atan(yHaarResponse/xHaarResponse);
		if(xHaarResponse >= 0 && yHaarResponse < 0)
			return 2*Math.PI - Math.atan(-yHaarResponse/xHaarResponse);
		return 0;
	}
	
	private double gaussian(double x, double y, double sig) {
		return (1.0f/(2.0f*Math.PI*sig*sig)) * Math.exp( -(x*x+y*y)/(2.0f*sig*sig));
	}
	
	
	
	public static void main(String args[]) {
		int LongTimeImageNum = 48;
		boolean getPointConcurrency = false;
		int getDescriptionThreadNum = 6;
		boolean imageParallel = false;
		int threadNum = 2;
		if (args.length == 5) {
			LongTimeImageNum = new Integer(args[0]);
			getPointConcurrency = new Boolean(args[1]);
			getDescriptionThreadNum = new Integer(args[2]);
			imageParallel = new Boolean(args[3]);
			threadNum = new Integer(args[4]);
		}
		try {
			System.out.println("[LongTimeImageNum=" + LongTimeImageNum  + "] [getPointConcurrency=" + getPointConcurrency +" getDescriptionThreadNum=" + getDescriptionThreadNum + "]");
			System.out.println("[imageParallel=" + imageParallel  + "  threadNum=" + threadNum + "]");
			if(!imageParallel) {
				int count = 0; 
				while(count++ < LongTimeImageNum) {
					BufferedImage image = ImageIO.read(new File("/opt/image/48/img" + (count % 48 + 1) + ".bmp"));
					Surf board = new Surf(image, getDescriptionThreadNum, getPointConcurrency);
				}
				System.out.println("Finish");
			}
			else {
				for(int i = 0; i < threadNum; i++) {
					Thread t = new Thread(new Task(LongTimeImageNum/threadNum, getPointConcurrency, getDescriptionThreadNum));
					t.start();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

class Task implements Runnable {
	int LongTimeImageNum = 48;
	boolean getPointConcurrency = false;
	int getDescriptionThreadNum = 6;
	
	public Task(int a, boolean b, int c) {
		LongTimeImageNum = a;
		getPointConcurrency = b;
		getDescriptionThreadNum = c;
	}
	
	@Override
	public void run() {
		int count = 0; 
		try {
			while(count++ < LongTimeImageNum) {
				BufferedImage image = ImageIO.read(new File("/opt/image/48/img" + (count % 48 + 1) + ".bmp"));
				Surf board = new Surf(image, getDescriptionThreadNum, getPointConcurrency);
			}
			System.out.println(Thread.currentThread().getId() + " Finish");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
