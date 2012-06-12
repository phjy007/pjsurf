package com.stromberglabs.jopensurf;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

class IPointsDescriptionProcessor2 implements Runnable {
	private SURFInterestPoint point;
	private LinkedBlockingQueue<SURFInterestPoint> queue;
	private boolean upright;
	private IsExtremumTask2 task;
	private int ipointNum;
	private CountDownLatch latch;
	
	public IPointsDescriptionProcessor2(LinkedBlockingQueue<SURFInterestPoint> q, boolean up, IsExtremumTask2 t, int ipNum, 
									   CountDownLatch l, boolean getMDescriptorConcurrency, ExecutorService pool) {
		queue = q;
		upright = up;
		task = t;
		ipointNum = ipNum;
		latch = l;
	}
	
	@Override
	public void run() {
		try {
			while(!Thread.currentThread().isInterrupted()) {
				synchronized(task) {
					if(task.finishedPointNum < ipointNum) {
						point = queue.take();
						task.finishedPointNum++;
					} else {
						latch.countDown();
						return;
					}
				}
				task.getOrientation(point);
				task.getMDescriptor(point, upright);
			}
		} catch(InterruptedException e) {
			System.out.println("IPointsDescriptionProcessor InterruptedException");
		}
	}
}