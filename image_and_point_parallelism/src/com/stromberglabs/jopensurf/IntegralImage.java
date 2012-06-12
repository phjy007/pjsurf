package com.stromberglabs.jopensurf;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.Serializable;

public class IntegralImage implements Serializable, Cloneable {
	private static final long serialVersionUID = 1L;
	
	public final float[][] mIntImage;
	public final int mWidth;
	public final int mHeight;
	
	public float[][] getValues(){
		return mIntImage;
	}
	
	public int getWidth() {
		return mWidth;
	}
	
	public int getHeight() {
		return mHeight;
	}
	
	public float getValue(int column, int row) {
		return mIntImage[column][row];
	}
	
	public IntegralImage(IntegralImage image) {
		mIntImage = new float[image.mWidth][image.mHeight];
		mWidth = image.mWidth;
		mHeight = image.mHeight;
		for(int i = 0; i < mWidth; i++)
			for(int j = 0; j < mHeight; j++)
				mIntImage[i][j] = image.mIntImage[i][j];
	}
	
	public IntegralImage(BufferedImage input) {
		mIntImage = new float[input.getWidth()][input.getHeight()];
		mWidth = mIntImage.length;
		mHeight = mIntImage[0].length;
		
		int width = input.getWidth();
		int height = input.getHeight();
		
		WritableRaster raster = input.getRaster();
		int[] pixel = new int[4];
		float sum;
		for (int y = 0; y < height; y++) {
			sum = 0F;
			for (int x = 0; x < width; x++) {
				raster.getPixel(x, y, pixel);
				float intensity = Math.round((0.299D*pixel[0] + 0.587D*pixel[1] + 0.114D*pixel[2]))/255F;
				sum += intensity;
				if (y == 0) {
					mIntImage[x][y] = sum;
				} else {
					mIntImage[x][y] = sum + mIntImage[x][y - 1];
				}
			}
		}
	}
	
	protected Object clone() {
		try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
	}
}
