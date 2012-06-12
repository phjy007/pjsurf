package com.stromberglabs.jopensurf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Surf implements Serializable {
	private static final long serialVersionUID = 1L;

	private static final int HESSIAN_OCTAVES = 5;
	private static final int HESSIAN_INIT_SAMPLE = 2;
	private static final float HESSIAN_THRESHOLD = 0.0004F;
	private static final float HESSIAN_BALANCE_VALUE = 0.81F;
	private int mNumOctaves 	= HESSIAN_OCTAVES;
	private float mThreshold 	= HESSIAN_THRESHOLD;
	private float mBalanceValue = HESSIAN_BALANCE_VALUE;
	
	private SurfThreadPool detectPool, descriptionPool;
	private final SurfBuffer surfBuffer;
	private final int imageNum;
	
	public volatile boolean finishDetection = false;
	private final int detectPoolSize, descriptionPoolSize;
	
//	private final Thread GlobalTableGuard;
	private final int descriptPackageNum;
	
	private final ArrayList<List<GlobalTableItem>> tables;
	

	public Surf(int imageNumber, int detectSize, int descriptionSize, int descriptPackageNum) {
		this(HESSIAN_BALANCE_VALUE, HESSIAN_THRESHOLD, HESSIAN_OCTAVES, imageNumber, detectSize, descriptionSize, descriptPackageNum);
	}

	public Surf(float balanceValue, float threshold, int octaves, int imageNumber, int detectSize, int descriptionSize, int descriptPkgNum) {
		mNumOctaves = octaves;
		mBalanceValue = balanceValue;
		mThreshold = threshold;
		imageNum = imageNumber;
		surfBuffer = new SurfBuffer();
		detectPoolSize = detectSize;
		descriptionPoolSize = descriptionSize;
		descriptPackageNum = descriptPkgNum;

		tables = new ArrayList<List<GlobalTableItem>>(descriptionPoolSize + descriptPackageNum);

		detectPool 		= new SurfThreadPool(detectPoolSize, surfBuffer, 0, imageNum, this, descriptPackageNum, tables, 0);	
		descriptionPool = new SurfThreadPool(descriptionPoolSize, surfBuffer, 1, imageNum, this, descriptPackageNum, tables, detectPoolSize-1);	

//		GlobalTableGuard = new Thread(new Runnable() {
//			@Override
//			public void run() {
//				while(!Thread.interrupted()) {
//					for(int i = 0; i < globalTable.size(); i++) {
//						if(!globalTable.get(i).imageFinished) {
//							boolean imageFinish = true;
//							for(int j = 0; j < globalTable.get(i).points.size(); j++) {
//								if(!globalTable.get(i).points.get(j).isDescripted) {
//									imageFinish = false;
//									break;
//								}
//							}
//							if(imageFinish) {
//								globalTable.get(i).imageFinished = true;
//								System.out.println("Image " + globalTable.get(i).imageID + " finished");
//							} else {
//								continue;
//							}
//						}
//					}
//				}
//			}
//		});

		detectPool.beginWork();
		descriptionPool.beginWork();
	}

	public static void main(String args[]) {
		int imageNum 			= 10;
		int detectPoolSize 		= 1; 
		int descriptPoolSize 	= 3;
		int descriptPackageNum 	= 5;
		if (args.length == 4) {
			imageNum 			= new Integer(args[0]);
			detectPoolSize 		= new Integer(args[1]);
			descriptPoolSize 	= new Integer(args[2]);
			descriptPackageNum 	= new Integer(args[3]);
		}
		System.out.println("Pipeline_surf_arraylist  [imageNum=" + imageNum + "]  [detectPoolSize=" + detectPoolSize + " descriptPoolSize=" + descriptPoolSize + "]");
		System.out.println("[descriptPackageNum=" + descriptPackageNum + "]");
		Surf board = new Surf(imageNum, detectPoolSize, descriptPoolSize, descriptPackageNum);
	}
	
}
