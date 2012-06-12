package com.stromberglabs.jopensurf;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SurfThreadPool {
	public int poolSize;
	public final List<Thread> threadPool;
	public final SurfBuffer buffer;
	public final int taskType;
	public final int imageNum;
	public AtomicInteger currentImageNumAtomic;
	private final Surf surf;
	private final int descriptPackageNum;
	private final ArrayList<List<GlobalTableItem>> tables;
	private final int tableOffest;

	public SurfThreadPool(int size, SurfBuffer b, int type, int imageNumber, Surf s, int descriptPkgNum, ArrayList<List<GlobalTableItem>> t, int offset) {
		poolSize 				= size;
		threadPool 				= new ArrayList<Thread>();
		buffer 					= b;
		taskType				= type;
		imageNum 				= imageNumber;
		currentImageNumAtomic 	= new AtomicInteger(0);
		surf					= s;
		descriptPackageNum 		= descriptPkgNum;
		tables					= t;
		tableOffest				= offset;
		for(int i = 0; i < poolSize; i++)
			threadPool.add(new Thread(new Task(this, buffer, taskType, imageNum, surf, descriptPackageNum, tables, tableOffest + i)));
	}
	
	public void beginWork() {
		for(int i = 0; i < poolSize; i++)
			threadPool.get(i).start();
	}
	
	public void stopWork() {
		for(int i = 0; i < poolSize; i++)
			threadPool.get(i).interrupt();
	}

}
