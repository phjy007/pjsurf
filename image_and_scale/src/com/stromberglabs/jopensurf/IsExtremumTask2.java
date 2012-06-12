package com.stromberglabs.jopensurf;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class IsExtremumTask2 implements Runnable {
	private final FastHessian fh;
	private final CountDownLatch latch;
	private final LinkedBlockingQueue<SURFInterestPoint> finalQueue;
	private final List<ResponseLayer> mLayers;
	private final int[][] filter_map;
	private final int mOctaves;
	private final LinkedList<SURFInterestPoint> queue;
	private final IntegralImage mIntegralImage;
	public int finishedPointNum = 0;
	private final int getDescriptionThreadNum;
	private final boolean getPointConcurrency;

	public IsExtremumTask2(int Octaves, FastHessian f, CountDownLatch l,  LinkedBlockingQueue<SURFInterestPoint> q, List<ResponseLayer> layers, int[][] map,  IntegralImage image, int getDescriptionThreadNumber, boolean getIPointConcurrency) {
		fh = f;
		latch = l;
		finalQueue = q;
		mLayers = layers;
		filter_map = map;
		mOctaves = Octaves;
		queue = new LinkedList<SURFInterestPoint>();
		mIntegralImage = image;
		getDescriptionThreadNum = getDescriptionThreadNumber;
		getPointConcurrency = getIPointConcurrency;
	}

	@Override
	public void run() {
		for(int o = 1; o < mOctaves; o++) {
			for(int i = 0; i <= 1; i++) {
				if(o == 1 && i == 0) { // skip (b=1, m=3, t=4)
					continue;
				} else {
					ResponseLayer b = mLayers.get(filter_map[o][i]);
					ResponseLayer m = mLayers.get(filter_map[o][i + 1]);
					ResponseLayer t = mLayers.get(filter_map[o][i + 2]);
					for (int r = 0; r < t.getHeight(); r++) {
						for (int c = 0; c < t.getWidth(); c++) {
							if (fh.isExtremum(r, c, t, m, b)) {
								SURFInterestPoint point = fh.interpolateExtremum(r, c, t, m, b);
								if (point != null) {
									queue.add(point);
								}
							}
						}
					}
				}
			}
		}
		
//		for(SURFInterestPoint point : queue) {
//			getOrientation(point);
//			getMDescriptor(point, true);
//			try {
//				finalQueue.put(point);
//			} catch (InterruptedException e) {
//				System.out.println("finalQueue.put(point) exception!");
//			}
//		}
		
		final ExecutorService exec = Executors.newCachedThreadPool();
		if (getPointConcurrency) {
//			System.out.println("getPointConcurrency=true");
			final int threadNum = getDescriptionThreadNum;
			final int ipointNum = queue.size();
			CountDownLatch latch = new CountDownLatch(threadNum + 1);			
			LinkedBlockingQueue<SURFInterestPoint> ipointsQueue = new LinkedBlockingQueue<SURFInterestPoint>();
			
			exec.execute(new IPointsEnQueue(ipointsQueue, queue, latch));
			for (int i = 0; i < threadNum; i++)
				exec.execute(new IPointsDescriptionProcessor2(ipointsQueue, true, this, ipointNum, latch, false, null));
			try {
				latch.await();
			} catch (InterruptedException e) {
				System.out.println("CountDownLatch interrupted!");
			}
			exec.shutdownNow();
			
		} else {
//			System.out.println("getPointConcurrency=false");
			for (SURFInterestPoint point : queue) {
				getOrientation(point);
				getMDescriptor(point, true);
			}
			exec.shutdownNow();
		}
		latch.countDown();
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
	
	private float haarX(int row, int column, int s){
		return ImageTransformUtils.BoxIntegral(mIntegralImage, row-s/2, column, s, s/2)
			-1 * ImageTransformUtils.BoxIntegral(mIntegralImage, row-s/2, column-s/2, s, s/2);
	}

	private float haarY(int row, int column, int s){
		return ImageTransformUtils.BoxIntegral(mIntegralImage, row, column-s/2, s/2, s)
			-1 * ImageTransformUtils.BoxIntegral(mIntegralImage, row-s/2, column-s/2, s/2, s);
	}
	
	private double gaussian(double x, double y, double sig) {
		return (1.0f/(2.0f*Math.PI*sig*sig)) * Math.exp( -(x*x+y*y)/(2.0f*sig*sig));
	}
}
