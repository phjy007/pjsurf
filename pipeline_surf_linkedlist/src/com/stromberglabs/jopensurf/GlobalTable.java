package com.stromberglabs.jopensurf;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.*;

public class GlobalTable extends LinkedList<GlobalTableItem> {
	private static final long serialVersionUID = 1L;
	private final Lock lock;
	
	public GlobalTable() {
		lock = new ReentrantLock();
	}
	
	public List<SURFInterestPoint> detectPoints(IntegralImage mIntegralImage, FastHessian mHessian, LinkedList<GlobalTableItem> globalTable, IntegralImage image) {
		lock.lock();
		try {
			int position = globalTable.size();
			List<SURFInterestPoint> InterestPoints 	= mHessian.getIPoints(position, image);
			globalTable.offer(new GlobalTableItem(mIntegralImage , InterestPoints));
			
//			System.out.println("Found " + InterestPoints.size() + " points");
			return InterestPoints;
			
		} finally {
			lock.unlock();
		}
	}
}
