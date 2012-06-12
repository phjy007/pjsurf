package com.stromberglabs.jopensurf;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.*;

public class SurfBuffer extends LinkedList<SURFInterestPoint>{
	private static final long serialVersionUID = 1L;
	private final Lock lock;
	
	public SurfBuffer() {
		lock = new ReentrantLock();
	}
	
	public void putIntoBuffer(List<SURFInterestPoint> InterestPoints) {
		lock.lock();
		try {
			for(SURFInterestPoint p : InterestPoints) {
				this.offer(p);
			}
		} finally {
			lock.unlock();
		}
	}
	
	public SURFInterestPoint myPoll() {
		lock.lock();
		try {
			return this.poll();
		} finally {
			lock.unlock();
		}
	}
	
	public LinkedList<SURFInterestPoint> myPoll(int n) {
		lock.lock();
		try {
			LinkedList<SURFInterestPoint> points = new LinkedList<SURFInterestPoint>();
//			System.out.println("buffer.size=" + this.size() + "  n=" + n);
//			System.out.println("buffer.size=" + this.size());
			if(this.size() > n) {
				for(int i = 0; i < n; i++) {
					points.offer(this.poll());
				}
				return points;
			} else if(this.size() > 0) {
				for(int i = 0; i < this.size(); i++) {
					points.offer(this.poll());
				}
				return points;
			}
			return null;
		} finally {
			lock.unlock();
		}
	}
	
	public ArrayList<SURFInterestPoint> myPoll2(int n) {
		lock.lock();
		try {
			ArrayList<SURFInterestPoint> points = new ArrayList<SURFInterestPoint>(n);
			if(this.size() > n) {
				for(int i = 0; i < n; i++) {
					points.add(this.poll());
				}
				return points;
			} else if(this.size() > 0) {
				for(int i = 0; i < this.size(); i++) {
					points.add(this.poll());
				}
				return points;
			}
			return null;
		} finally {
			lock.unlock();
		}
	}
}
