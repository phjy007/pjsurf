package com.stromberglabs.jopensurf;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

public class RunSurfConcurrency {
	public static void main(String args[]) {				
		int picNo 							= 14;
		boolean getPointConcurrency 		= false;
		int getPointThreadNum 				= 4;
		boolean getMDescriptorConcurrency 	= false;
		int getMDescriptorThreadNum			= 4;
		boolean buildResponseMapConcurrency = false;
		boolean responseLayerExtremum 		= false;
		boolean multiImageConcurrency 		= false;
		int multiImageConcurrencyNum 		= 2;
		int LongTimeImageNum 				= 48;
		if (args.length == 10) {
			picNo 						= new Integer(args[0]);
			getPointConcurrency 		= new Boolean(args[1]);
			getPointThreadNum 			= new Integer(args[2]);
			getMDescriptorConcurrency 	= new Boolean(args[3]);
			getMDescriptorThreadNum 	= new Integer(args[4]);
			buildResponseMapConcurrency = new Boolean(args[5]);
			responseLayerExtremum	 	= new Boolean(args[6]);
			multiImageConcurrency		= new Boolean(args[7]);
			multiImageConcurrencyNum 	= new Integer(args[8]);
			LongTimeImageNum			= new Integer(args[9]);
		}
		System.out.println("picNo=" + picNo + " [getPointConcurrency=" + getPointConcurrency + " Thread=" + getPointThreadNum + "]"
						   + " [getMDescriptruetorConcurrency=" + getMDescriptorConcurrency + " Thread=" + getMDescriptorThreadNum + "]");
		System.out.println("	 [buildResponseMapConcurrency=" + buildResponseMapConcurrency + "]  [responseLayerExtremum=" + responseLayerExtremum + "]");
		System.out.println("	 [multiImageConcurrency=" + multiImageConcurrency + " multiImage=" + multiImageConcurrencyNum + "]  [LongTimeImageNum=" + LongTimeImageNum + "]");
		if(!multiImageConcurrency) {
			try {
				int count = 0; 
				//File imageFile = new File("/opt/image/14/img" + picNo + ".bmp");
				while(count++ < LongTimeImageNum) {
					BufferedImage image = ImageIO.read(new File("/opt/image/48/img" + (count % 48 + 1) + ".bmp"));
					Surf board = new Surf(image, buildResponseMapConcurrency, responseLayerExtremum, getMDescriptorThreadNum);
					List<SURFInterestPoint> points = board.getUprightInterestPoints(getPointConcurrency, getPointThreadNum, getMDescriptorConcurrency, getMDescriptorThreadNum);
//					System.out.println((count % 48 + 1) +" Found " + points.size() + " interest points");
				}
				System.out.println("Finish");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
				ExecutorService exec_double_pic_pool = Executors.newFixedThreadPool(multiImageConcurrencyNum);
				CountDownLatch latch = new CountDownLatch(multiImageConcurrencyNum);
				for(int i = 0; i < multiImageConcurrencyNum; i++) {
					exec_double_pic_pool.execute(new SingleSurfTask(picNo, getPointThreadNum, getPointConcurrency, getMDescriptorConcurrency, getMDescriptorThreadNum,
																	buildResponseMapConcurrency, responseLayerExtremum, latch, multiImageConcurrencyNum, LongTimeImageNum));
				}
				try {
					latch.await();
					System.out.println("Multi-image finish");
					exec_double_pic_pool.shutdownNow();
				} catch (InterruptedException e) {
					System.out.println("Main InterruptedException");
				}
		}
	}
}

class SingleSurfTask implements Runnable {
	private static int			 COUNT = 0;
	private final int			 id = COUNT++;
	private final int 			 picNo;
	private final int			 getPointThreadNum;
	private final boolean 		 getPointConcurrency;
	private final boolean 		 getMDescriptorConcurrency;
	private final int 			 getMDescriptorThreadNum;
	private final boolean 		 buildResponseMapConcurrency;
	private final boolean 		 responseLayerExtremum;
//	private final int			 multiImageConcurrencyNum;
	private final CountDownLatch latch;
	private static int			 count;
	
	public SingleSurfTask(int picNo, int getPointThreadNum, boolean getPointConcurrency, boolean getMDescriptorConcurrency, int getMDescriptorThreadNum,
						  boolean buildResponseMapConcurrency, boolean responseLayerExtremum , CountDownLatch latch, int multiImageConcurrencyNum, int c) {
		this.picNo 							= picNo;
		this.getPointThreadNum 				= getPointThreadNum;
		this.getPointConcurrency 			= getPointConcurrency;
		this.getMDescriptorConcurrency		= getMDescriptorConcurrency;
		this.getMDescriptorThreadNum 		= getMDescriptorThreadNum;
		this.buildResponseMapConcurrency 	= buildResponseMapConcurrency;
		this.responseLayerExtremum 			= responseLayerExtremum;
		this.latch 							= latch;
		this.count 							= c / multiImageConcurrencyNum;
	}
	
	@Override
	public void run() {
		try {
			int i = 0;
			while(i < count) {
				//BufferedImage image = ImageIO.read(new File("/opt/image/14/img" + picNo + ".bmp"));
				BufferedImage image = ImageIO.read(new File("/opt/image/48/img" + (i % 48 + 1)+ ".bmp"));
				Surf board = new Surf(image, buildResponseMapConcurrency, responseLayerExtremum, getMDescriptorThreadNum);
				List<SURFInterestPoint> points = board.getUprightInterestPoints(getPointConcurrency, getPointThreadNum, getMDescriptorConcurrency, getMDescriptorThreadNum);
//				System.out.println("id=" + id + " " + (i % 48 + 1) + " Found " + points.size() + " interest points");
				i++;
			}
		} catch(Exception e) {
			System.out.println("SingleSurfTask Exception");
			e.printStackTrace();
		}
		latch.countDown();
	}
}
