package com.stromberglabs.jopensurf;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

public class IsExtremumTask implements Runnable {
	private ResponseLayer b, m, t;
	private final FastHessian fh;
	private final CountDownLatch latch;
	private final LinkedBlockingQueue<SURFInterestPoint> queue;
	
	public IsExtremumTask(ResponseLayer bb, ResponseLayer mm, ResponseLayer tt, FastHessian f, CountDownLatch l, LinkedBlockingQueue<SURFInterestPoint> q) {
		b = bb;
		m = mm;
		t = tt;
		fh = f;
		latch = l;
		queue = q;
	}
	
	@Override
	public void run() {
		for(int r = 0; r < t.getHeight(); r++) {
	    	for(int c = 0; c < t.getWidth(); c++) {
	    		if (fh.isExtremum(r, c, t, m, b)) {
	    			SURFInterestPoint point = fh.interpolateExtremum(r, c, t, m, b);
	    			if (point != null) {
	    				try {
//	    					System.out.println("----------------------------+1");
							queue.put(point);
						} catch (InterruptedException e) {
							System.out.println("IsExtremumTask InterruptedException");
						}
	    			}
	    		}
	    	}
	    }
		latch.countDown();
	}
}
