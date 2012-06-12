package com.stromberglabs.jopensurf;

import java.util.List;

public class GlobalTableItem {
	public final IntegralImage integralImage;
	public final List<SURFInterestPoint> points;
	public boolean imageFinished;
	
	public GlobalTableItem(IntegralImage image, List<SURFInterestPoint> p) {
		integralImage = image;
		points = p;
		imageFinished = false;
	}
	
}

