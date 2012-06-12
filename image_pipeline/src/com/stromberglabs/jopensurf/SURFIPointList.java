package com.stromberglabs.jopensurf;

import java.util.ArrayList;

public class SURFIPointList extends ArrayList<SURFInterestPoint> {
	public int index = 0;
	public int tailPosition;
	
	public SURFIPointList() {
		super(1500);
	}
}
