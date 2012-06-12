package com.stromberglabs.jopensurf;

import java.util.concurrent.Callable;

public class BuildResponseMapTask implements Callable<ResponseLayer> {
	private final int h, w, s, filter;
	private IntegralImage mIntegralImage, image;
	private static ThreadLocal<IntegralImage> localImage;
	
	public BuildResponseMapTask(int ww, int hh, int ss, int f, final IntegralImage i) {
		h = hh;
		w = ww;
		s = ss;
		filter = f;
		image = i;
	}
	
	@Override
	public ResponseLayer call() {
		localImage = new ThreadLocal<IntegralImage>() {
			@Override
			protected IntegralImage initialValue() {
				return (IntegralImage)image.clone();
			}
		};
		mIntegralImage = localImage.get();
		return new ResponseLayer(w, h, s, filter, mIntegralImage);
	}
}
