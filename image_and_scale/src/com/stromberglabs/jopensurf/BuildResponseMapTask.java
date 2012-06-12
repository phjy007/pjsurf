package com.stromberglabs.jopensurf;

import java.util.concurrent.Callable;

public class BuildResponseMapTask implements Callable<ResponseLayer> {
	private final int h, w, s, filter;
	private IntegralImage mIntegralImage;
	
	public BuildResponseMapTask(int ww, int hh, int ss, int f, final IntegralImage i) {
		h = hh;
		w = ww;
		s = ss;
		filter = f;
		mIntegralImage = i;
	}
	
	@Override
	public ResponseLayer call() {
		return new ResponseLayer(w, h, s, filter, mIntegralImage);
	}
}
