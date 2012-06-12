package com.stromberglabs.jopensurf;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.DecompositionSolver;
import org.apache.commons.math.linear.LUDecompositionImpl;
import org.apache.commons.math.linear.RealMatrix;

public class FastHessian implements Serializable {
	private static final long serialVersionUID = 1L;

	private static int[][] filter_map  = {{0,1,2,3}, {1,3,4,5}, {3,5,6,7}, {5,7,8,9}, {7,9,10,11}};
	
	private IntegralImage 			mIntegralImage;
	private List<SURFInterestPoint> mInterestPoints;
	private int 					mOctaves;
	private int 					mInitSample;
	private float 					mThreshold;
	private int 					mHeight;
	private int 					mWidth;
	private boolean 				mRecalculateInterestPoints = true;
	List<ResponseLayer> 			mLayers;
	private final ExecutorService	exec_pool;
	private final Surf 				surf;
	private final int				getDescriptionThreadNum;
	private final boolean 			getPointConcurrency;
	
	public FastHessian(IntegralImage integralImage, int octaves, int initSample, float threshold, float balanceValue, ExecutorService pool, Surf s, int getDescriptionThreadNumber, boolean getIPointConcurrency) {
		mIntegralImage 	= integralImage;
		mOctaves 		= octaves;
		mInitSample 	= initSample;
		mThreshold 		= threshold;
		mWidth 			= integralImage.getWidth();
		mHeight 		= integralImage.getHeight();
		exec_pool 		= pool;
		surf 			= s;
		getDescriptionThreadNum = getDescriptionThreadNumber;
		getPointConcurrency = getIPointConcurrency;
	}
	
	public List<SURFInterestPoint> getIPoints() {
		if ( mInterestPoints == null || mRecalculateInterestPoints ) {
			mInterestPoints = new LinkedList<SURFInterestPoint>();
			buildResponseMap();
			ResponseLayer b, m, t;

//			for ( int o = 0; o < mOctaves; o++ ) {
//				for ( int i = 0; i <= 1; i++ ) {
//				    b = mLayers.get(filter_map[o][i]);
//				    m = mLayers.get(filter_map[o][i+1]);
//				    t = mLayers.get(filter_map[o][i+2]);
//				    for ( int r = 0; r < t.getHeight(); r++ ) {
//				    	for ( int c = 0; c < t.getWidth(); c++ ) {
//				    		if ( isExtremum(r, c, t, m, b) ){
//				    			SURFInterestPoint point = interpolateExtremum(r, c, t, m, b);
//				    			if ( point != null ) {
//				    				mInterestPoints.add(point);
//				    			}
//				    		}
//				    	}
//				    }
//				}
//			}
			
			CountDownLatch latch = new CountDownLatch(4);
			final LinkedBlockingQueue<SURFInterestPoint> iPointsQueue = new LinkedBlockingQueue<SURFInterestPoint>();
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						while(!Thread.interrupted()) {
							mInterestPoints.add(iPointsQueue.take());
						}
					} catch (InterruptedException e) {
//						System.out.println("getIPoints_Thread InterruptedException");
					}
				}
			});
			thread.start();
			for(int o = 0; o < 1; o++) {
				for(int i = 0; i <= 1; i++) {
				    b = mLayers.get(filter_map[o][i]);
				    m = mLayers.get(filter_map[o][i + 1]);
				    t = mLayers.get(filter_map[o][i + 2]);
				    exec_pool.execute(new IsExtremumTask(b, m, t, this, latch, iPointsQueue, mIntegralImage, getDescriptionThreadNum, getPointConcurrency));
				}
			}
			exec_pool.execute(new IsExtremumTask(mLayers.get(filter_map[1][0]), mLayers.get(filter_map[1][1]), mLayers.get(filter_map[1][2]), this, latch, iPointsQueue, mIntegralImage, getDescriptionThreadNum, getPointConcurrency));
			exec_pool.execute(new IsExtremumTask2(mOctaves, this, latch, iPointsQueue, mLayers, filter_map, mIntegralImage, getDescriptionThreadNum, getPointConcurrency));
			try {
				latch.await();
				while(iPointsQueue.size() > 0);
				thread.interrupt();
//				System.out.println("Found " + mInterestPoints.size() + " points");
			} catch(InterruptedException e) {
				System.out.println("getIPoints_latch InterruptedException!");
			}
			exec_pool.shutdownNow();
		}
		return mInterestPoints;
	}
	
	private void buildResponseMap(){
		mLayers = new LinkedList<ResponseLayer>();
		int w = mWidth / mInitSample;
		int h = mHeight / mInitSample;
		int s = mInitSample;
//		if ( mOctaves >= 1 ) {
//			mLayers.add(new ResponseLayer(w, h, s, 9, mIntegralImage));
//			mLayers.add(new ResponseLayer(w, h, s, 15, mIntegralImage));
//			mLayers.add(new ResponseLayer(w, h, s, 21, mIntegralImage));
//			mLayers.add(new ResponseLayer(w, h, s, 27, mIntegralImage));
//		}
//		if ( mOctaves >= 2 ) {
//			mLayers.add(new ResponseLayer(w/2, h/2, s*2, 39, mIntegralImage));
//			mLayers.add(new ResponseLayer(w/2, h/2, s*2, 51, mIntegralImage));
//		}
//		if ( mOctaves >= 3 ) {
//			mLayers.add(new ResponseLayer(w/4, h/4, s*4, 75, mIntegralImage));
//			mLayers.add(new ResponseLayer(w/4, h/4, s*4, 99, mIntegralImage));
//
//		}
//		if ( mOctaves >= 4 ) {
//			mLayers.add(new ResponseLayer(w/8, h/8, s*8, 147, mIntegralImage));
//			mLayers.add(new ResponseLayer(w/8, h/8, s*8, 195, mIntegralImage));
//		}
//		if ( mOctaves >= 5 ) {
//			mLayers.add(new ResponseLayer(w/16, h/16, s*16, 291, mIntegralImage));
//			mLayers.add(new ResponseLayer(w/16, h/16, s*16, 387, mIntegralImage));
//		}
		
		FutureTask<ResponseLayer> future0 = new FutureTask<ResponseLayer>(new BuildResponseMapTask(w, h, s, 9,  mIntegralImage));
		FutureTask<ResponseLayer> future1 = new FutureTask<ResponseLayer>(new BuildResponseMapTask(w, h, s, 15, mIntegralImage));
		FutureTask<ResponseLayer> future2 = new FutureTask<ResponseLayer>(new BuildResponseMapTask(w, h, s, 21, mIntegralImage));
		FutureTask<ResponseLayer> future3 = new FutureTask<ResponseLayer>(new BuildResponseMapTask(w, h, s, 27, mIntegralImage));
		FutureTask<List<ResponseLayer>> future4 = new FutureTask<List<ResponseLayer>>(new BuildResponseMapTask2(w, h, s, mIntegralImage, mOctaves));
		
		exec_pool.execute(future0);
		exec_pool.execute(future1);
		exec_pool.execute(future2);
		exec_pool.execute(future3);
		exec_pool.execute(future4);
		
		try {
			mLayers.add(future0.get());
			mLayers.add(future1.get());
			mLayers.add(future2.get());
			mLayers.add(future3.get());
			List<ResponseLayer> mLayers2 = future4.get();
			for(ResponseLayer l : mLayers2) {
				mLayers.add(l);
			}
		} catch(InterruptedException e) {
			System.out.println("future.get() InterruptedException");
		} catch (ExecutionException e) {
			System.out.println("future.get() ExecutionException");
		}
//		exec_pool.shutdownNow();
	}
	
	public boolean isExtremum(int r, int c, ResponseLayer t, ResponseLayer m, ResponseLayer b) {
		int layerBorder = (t.getFilter() + 1)/(2 * t.getStep());
		if ( r <= layerBorder || r >= t.getHeight() - layerBorder || c <= layerBorder || c >= t.getWidth() - layerBorder )
			return false;
		double candidate = m.getResponse(r, c, t);
		if ( candidate < mThreshold )
			return false;
		for ( int rr = -1; rr <= 1; rr++ ) {
			for ( int cc = -1; cc <= 1; cc++ ) {
				if (t.getResponse(r+rr, c+cc) >= candidate ||
						((rr != 0 || cc != 0) && m.getResponse(r+rr, c+cc, t) >= candidate) ||
						b.getResponse(r+rr, c+cc, t) >= candidate)
					return false;
			}
		}
		return true;
	}
	
	public SURFInterestPoint interpolateExtremum(int r, int c, ResponseLayer t, ResponseLayer m, ResponseLayer b) {
		int filterStep = m.getFilter() - b.getFilter();
		
		double xi = 0, xr = 0, xc = 0;
		double[] values = interpolateStep(r, c, t, m, b);
		xi = values[0];
		xr = values[1];
		xc = values[2];
		
		if ( Math.abs(xi) < 0.5f && Math.abs(xr) < 0.5f && Math.abs(xc) < 0.5f ) {
			float x = (float)(c+xc)*t.getStep();
			float y = (float)(r+xr)*t.getStep();
			float scale = (float)(0.1333F * (m.getFilter() + xi * filterStep));
			int laplacian = (int)m.getLaplacian(r,c,t);
			return new SURFInterestPoint(x,y,scale,laplacian);
		}
		return null;
	}
	
	private double[] interpolateStep(int r, int c, ResponseLayer t, ResponseLayer m, ResponseLayer b) {
		double[] values = new double[3];
		RealMatrix partialDerivs = getPartialDerivativeMatrix(r, c, t, m, b);
		RealMatrix hessian3D = getHessian3DMatrix(r, c, t, m, b);
		DecompositionSolver solver = new LUDecompositionImpl(hessian3D).getSolver();
		RealMatrix X = solver.getInverse().multiply(partialDerivs);
		values[0] = -X.getEntry(2, 0);
		values[1] = -X.getEntry(1, 0);
		values[2] = -X.getEntry(0, 0);
		return values;
	}
	
	private RealMatrix getPartialDerivativeMatrix(int r, int c, ResponseLayer t, ResponseLayer m, ResponseLayer b) {
		double[][] derivs = new double[3][1];
		derivs[0][0] = ( m.getResponse(r, c+1, t) - m.getResponse(r, c-1, t)) / 2.0D;
		derivs[1][0] = ( m.getResponse(r+1, c, t) - m.getResponse(r-1, c, t)) / 2.0D;
		derivs[2][0] = ( t.getResponse(r, c) - b.getResponse(r, c, t)) / 2.0D;
		RealMatrix matrix = new Array2DRowRealMatrix(derivs);
		return matrix;
	}

	private RealMatrix getHessian3DMatrix(int r, int c, ResponseLayer t, ResponseLayer m, ResponseLayer b) {
		double[][] hessian = new double[3][3]; 
		double v =  m.getResponse(r, c, t);
		
		//dxx
		hessian[0][0] = m.getResponse(r, c+1, t) + m.getResponse(r, c-1, t) - 2 * v;
		
		//dyy
		hessian[1][1] = m.getResponse(r+1, c, t) + m.getResponse(r-1,c,t) - 2 * v;
		
		//dss
		hessian[2][2] = t.getResponse(r, c) + b.getResponse(r, c, t) - 2 * v;
		
		//dxy
		hessian[0][1] = hessian[1][0] = ( m.getResponse(r + 1, c + 1, t) - 
											m.getResponse(r + 1, c - 1, t) -
											m.getResponse(r - 1, c + 1, t) + 
											m.getResponse(r - 1, c - 1, t) ) / 4.0;
		
		//dxs
		hessian[0][2] = hessian[2][0] = ( t.getResponse(r, c + 1) - 
											t.getResponse(r, c - 1) -
											b.getResponse(r, c + 1, t) + 
											b.getResponse(r, c - 1, t) ) / 4.0;
		
		//dys
		hessian[1][2] = hessian[2][1] = ( t.getResponse(r + 1, c) - 
											t.getResponse(r - 1, c) -
											b.getResponse(r + 1, c, t) + 
											b.getResponse(r - 1, c, t) ) / 4.0;

		return new Array2DRowRealMatrix(hessian);
	}
}
