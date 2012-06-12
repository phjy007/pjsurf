package com.stromberglabs.jopensurf;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

public class IsExtremumTask2 implements Runnable {
	private final FastHessian fh;
	private final CountDownLatch latch;
	private final LinkedBlockingQueue<SURFInterestPoint> queue;
	private final List<ResponseLayer> mLayers;
	private final int[][] filter_map;
	private final int mOctaves;

	public IsExtremumTask2(int Octaves, FastHessian f, CountDownLatch l, LinkedBlockingQueue<SURFInterestPoint> q, List<ResponseLayer> layers, int[][] map) {
		fh = f;
		latch = l;
		queue = q;
		mLayers = layers;
		filter_map = map;
		mOctaves = Octaves;
	}

	@Override
	public void run() {
		
		for(int o = 1; o < mOctaves; o++) {
			for(int i = 0; i <= 1; i++) {
				if(o == 1 && i == 0) { // skip (b=1, m=3, t=4)
					continue;
				} else {
					ResponseLayer b = mLayers.get(filter_map[o][i]);
					ResponseLayer m = mLayers.get(filter_map[o][i + 1]);
					ResponseLayer t = mLayers.get(filter_map[o][i + 2]);
					for (int r = 0; r < t.getHeight(); r++) {
						for (int c = 0; c < t.getWidth(); c++) {
							if (fh.isExtremum(r, c, t, m, b)) {
								SURFInterestPoint point = fh.interpolateExtremum(r, c, t, m, b);
								if (point != null) {
									try {
		//								System.out.println("==============================+1");
										queue.put(point);
									} catch (InterruptedException e) {
										System.out.println("IsExtremumTask InterruptedException");
									}
								}
							}
						}
					}
				}
			}
		}
		latch.countDown();
	}
}
