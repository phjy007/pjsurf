package com.stromberglabs.jopensurf;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

class IPointsDescriptionProcessor implements Runnable {
	private SURFInterestPoint point;
	private LinkedBlockingQueue<SURFInterestPoint> queue;
	private boolean upright;
	private Surf surf;
	private int ipointNum;
	private CountDownLatch latch;
	private boolean getMDescriptorConcurrency;
	private ExecutorService exec_getMDescriptor_pool;
	
	public IPointsDescriptionProcessor(LinkedBlockingQueue<SURFInterestPoint> q, boolean up, Surf s, int ipNum, 
									   CountDownLatch l, boolean getMDescriptorConcurrency, ExecutorService pool) {
		queue = q;
		upright = up;
		surf = s;
		ipointNum = ipNum;
		latch = l;
		this.getMDescriptorConcurrency = getMDescriptorConcurrency;
		exec_getMDescriptor_pool = pool;
	}
	
	@Override
	public void run() {
		try {
			while(!Thread.currentThread().isInterrupted()) {
				synchronized(surf) {
					if(surf.finishedPointNum < ipointNum) {
						point = queue.take();
						surf.finishedPointNum++;
					} else {
						latch.countDown();
						return;
					}
				}
				surf.getOrientation(point);
				surf.getMDescriptor(point, upright, getMDescriptorConcurrency, exec_getMDescriptor_pool);
			}
		} catch(InterruptedException e) {
			System.out.println("IPointsDescriptionProcessor InterruptedException");
		}
	}
}