package com.stromberglabs.jopensurf;

import java.util.List;

public class GlobalTableItem {
	public final IntegralImage integralImage;
	public final SURFIPointList points;
	public boolean imageFinished;
	
	public GlobalTableItem(IntegralImage image, SURFIPointList p) {
		integralImage = image;
		points = p;
		imageFinished = false;
	}
	
}

