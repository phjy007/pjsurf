package com.stromberglabs.jopensurf;

import java.io.Serializable;
import java.util.Arrays;

public class SURFInterestPoint implements Serializable, Cloneable, InterestPoint {
	private static final long serialVersionUID = 1L;
	private final float mX,mY;
	private final float mScale;
	private float mOrientation;
	private final int mLaplacian;
	private float[] mDescriptor = new float[64];
	private double lenArray0, lenArray1, lenArray2, lenArray3;
	private double len;
	private float mDx,mDy;
	private int mClusterIndex;
	
	public SURFInterestPoint(float x, float y, float scale, int laplacian){
		mX = x;
		mY = y;
		mScale = scale;
		mLaplacian = laplacian;
	}
	
	public float getX() {
		return mX;
	}

	public float getY() {
		return mY;
	}

	public float getScale() {
		return mScale;
	}

	public float getOrientation() {
		return mOrientation;
	}

	public void setOrientation(float orientation) {
		mOrientation = orientation;
	}

	public int getLaplacian() {
		return mLaplacian;
	}

	public float[] getDescriptor() {
		return mDescriptor;
	}
	
	/**
	 * To take care of the InterestPoint Interface
	 */
	public float[] getLocation() {
		return mDescriptor;
	}

	public void setDescriptor(float[] descriptor) {
		mDescriptor = descriptor;
	}
	public void setDescriptor(float[] descriptor, int id, double len_p) {
		for(int i = id*16; i < id*16 + 16; i++)
			mDescriptor[i] = descriptor[i - id*16];
		switch(id) {
			case 0:
				lenArray0 = len_p;
				break;
			case 1:
				lenArray1 = len_p;
				break;
			case 2:
				lenArray2 = len_p;
				break;
			case 3:
				lenArray3 = len_p;
				break;
		}
	}
	
	public void calculateMDescriptor() {
		len = Math.sqrt(lenArray0 + lenArray1 + lenArray2 + lenArray3);
//		System.out.println("len=" + len);
		for(int i = 0; i < 64; i++) {
			mDescriptor[i] /= len;
//			System.out.print(mDescriptor[i] + " & ");
		}
//		System.out.println();
	}

	public float getDx() {
		return mDx;
	}

	public void setDx(float dx) {
		mDx = dx;
	}

	public float getDy() {
		return mDy;
	}

	public void setDy(float dy) {
		mDy = dy;
	}

	public int getClusterIndex() {
		return mClusterIndex;
	}

	public void setClusterIndex(int clusterIndex) {
		mClusterIndex = clusterIndex;
	}
	
	public double getDistance(InterestPoint point){
		double sum = 0;
		if ( point.getLocation() == null || mDescriptor == null ) return Float.MAX_VALUE;
		for (int i = 0; i < mDescriptor.length; i++) {
			double diff = mDescriptor[i] - point.getLocation()[i];
			sum += diff*diff; 
		}
		return (double)Math.sqrt(sum);
	}

	public Float getCoord(int dimension) {
		return mDescriptor[dimension];
	}

	public int getDimensions() {
		return mDescriptor.length;
	}

    public Object clone() throws CloneNotSupportedException {
            return super.clone();
    }
    
    public boolean isEquivalentTo(SURFInterestPoint point){
    	boolean isEquivalent = true;
    	isEquivalent &= mX == point.getX();
    	isEquivalent &= mY == point.getY();
    	isEquivalent &= mDx == point.getDx();
    	isEquivalent &= mDy == point.getDy();
    	isEquivalent &= mOrientation == point.getOrientation();
    	isEquivalent &= mScale == point.getScale();
    	isEquivalent &= mLaplacian == point.getLaplacian();
    	isEquivalent &= Arrays.equals(mDescriptor,point.getDescriptor());
    	return isEquivalent;
    }
}
