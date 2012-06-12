package com.stromberglabs.jopensurf;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

public class BuildResponseMapTask2 implements Callable<List<ResponseLayer>> {
	private final int h, w, s;
	private IntegralImage mIntegralImage, image;
	private static ThreadLocal<IntegralImage> localImage;
	private List<ResponseLayer> mLayers;
	private int mOctaves;
	
	public BuildResponseMapTask2(int ww, int hh, int ss, final IntegralImage i, int Octaves) {
		h = hh;
		w = ww;
		s = ss;
		image = i;
		mLayers = new LinkedList<ResponseLayer>();
		mOctaves = Octaves;
	}
	
	@Override
	public List<ResponseLayer> call() {
		localImage = new ThreadLocal<IntegralImage>() {
			@Override
			protected IntegralImage initialValue() {
				return (IntegralImage)image.clone();
			}
		};
		mIntegralImage = localImage.get();
		if(mOctaves >= 2) {
			mLayers.add(new ResponseLayer(w/2, h/2, s*2, 39, mIntegralImage));
	        mLayers.add(new ResponseLayer(w/2, h/2, s*2, 51, mIntegralImage));
		}
		if(mOctaves >= 3) {
	        mLayers.add(new ResponseLayer(w/4, h/4, s*4, 75, mIntegralImage));
	        mLayers.add(new ResponseLayer(w/4, h/4, s*4, 99, mIntegralImage));
		}
		if(mOctaves >= 4) {
	        mLayers.add(new ResponseLayer(w/8, h/8, s*8, 147, mIntegralImage));
	        mLayers.add(new ResponseLayer(w/8, h/8, s*8, 195, mIntegralImage));
		}
		if(mOctaves >= 5) {
	        mLayers.add(new ResponseLayer(w/16, h/16, s*16, 291, mIntegralImage));
	        mLayers.add(new ResponseLayer(w/16, h/16, s*16, 387, mIntegralImage));
		}
		return mLayers;
	}
}