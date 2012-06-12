package com.stromberglabs.jopensurf;

import java.util.ArrayList;
import java.util.concurrent.locks.*;

public class SurfBuffer extends ArrayList<SURFIPointList>{
	private static final long serialVersionUID = 1L;
	private final Lock lock;
	public int head;
	
	public SurfBuffer() {
		super(480);
		head = 0;
		lock = new ReentrantLock();
	}
	
	public void putIntoBuffer(SURFIPointList InterestPoints) {	//put the new node into the tail of the buffer
		lock.lock();
		try {
			this.add(InterestPoints);
		} finally {
			lock.unlock();
		}
	}
	
	public int[] myPoll(int n) {	//pick one node from the head of the buffer
		lock.lock();
		int[] ans = new int[3]; //[head, index, number]
		try {
			if(this.size() > 0) {
				if(this.size() > head) {
					int remainNum = this.get(head).tailPosition - this.get(head).index;
					if(remainNum > n) {
						ans[0] = head;
						ans[1] = this.get(head).index;
						ans[2] = n;
						this.get(head).index += n;
						return ans;
					} else if(remainNum > 0) {
						ans[0] = head;
						ans[1] = this.get(head).index;
						ans[2] = remainNum;
						this.get(head).index = this.get(head).tailPosition;
						head++;
						return ans;
					} else {
						head++;
						return null;
					}
				}
			}
			return null;
		} finally {
			lock.unlock();
		}
	}
}
