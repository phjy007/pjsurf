package com.stromberglabs.jopensurf;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

public class IPointsEnQueue implements Runnable {
	private LinkedBlockingQueue<SURFInterestPoint> queue;
	private List<SURFInterestPoint> points;
	private CountDownLatch latch;
	
	
	public IPointsEnQueue(LinkedBlockingQueue<SURFInterestPoint> q, List<SURFInterestPoint> ps, CountDownLatch l) {
		queue = q;
		points = ps;
		latch = l;
	}
	
	@Override
	public void run() {
		for(SURFInterestPoint point : points)
			queue.add(point);
		latch.countDown();
	}
}
