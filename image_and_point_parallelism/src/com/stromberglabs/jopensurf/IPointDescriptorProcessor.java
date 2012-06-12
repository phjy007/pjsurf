package com.stromberglabs.jopensurf;

import java.util.concurrent.CountDownLatch;

public class IPointDescriptorProcessor implements Runnable {
	private final int id;
	private final SURFInterestPoint point;
	private final Surf surf;
	private final boolean upright;
	private final int x, y;
	private final double scale;
	private int count = 0;
	private int sample_x, sample_y;
	private double dx, dy, mdx, mdy, co = 1F, si = 0F;
	private float desc[] = new float[16];
	private double gauss_s1 = 0.0D, gauss_s2 = 0.0D, xs = 0.0D, ys = 0.0D;
	private double rx = 0.0D, ry = 0.0D, rrx = 0.0D, rry = 0.0D, len = 0.0D;
	private int i = 0, ix = 0, j = 0, jx = 0;
	private float cx = -0.5f, cy = 0.0f;
	private CountDownLatch latch;

	public IPointDescriptorProcessor(Surf s, SURFInterestPoint p, boolean up, int idn, CountDownLatch l, int xx, int yy, double sca) {
		surf = s;
		point = p;
		upright = up;
		id = idn;
		latch = l;
		x = xx;
		y = yy;
		scale = sca;
	}

	@Override
	public void run() {
		if (!upright) {
			co = Math.cos(point.getOrientation());
			si = Math.sin(point.getOrientation());
		}
		i = 5 * id - 12;
		j = -8;
		cx = cx + 1.0F * ((float) id + 1.0f);
		cy = -0.5F;
		while (j < 12) {
			dx = dy = mdx = mdy = 0.0F;
			cy += 1.0F;
			j = j - 4;
			ix = i + 5;
			jx = j + 5;
			xs = Math.round(x + (-jx * scale * si + ix * scale * co));
			ys = Math.round(y + (jx * scale * co + ix * scale * si));
			for (int k = i; k < i + 9; ++k) {
				for (int l = j; l < j + 9; ++l) {
					sample_x = (int)Math.round(x + (-1D * l * scale * si + k * scale * co));
					sample_y = (int)Math.round(y + (l * scale * co + k * scale * si));
					gauss_s1 = gaussian(xs - sample_x, ys - sample_y, 2.5F * scale);
					rx = haarX(sample_y, sample_x, (int) (2 * Math.round(scale)));
					ry = haarY(sample_y, sample_x, (int) (2 * Math.round(scale)));
					
					rrx = gauss_s1 * (-rx * si + ry * co);
					rry = gauss_s1 * ( rx * co + ry * si);
					dx += rrx;
					dy += rry;
					mdx += Math.abs(rrx);
					mdy += Math.abs(rry);
				}
			}
			gauss_s2 = gaussian(cx - 2.0f, cy - 2.0f, 1.5f);
			desc[count++] = (float) (dx * gauss_s2);
			desc[count++] = (float) (dy * gauss_s2);
			desc[count++] = (float) (mdx * gauss_s2);
			desc[count++] = (float) (mdy * gauss_s2);
			len += (dx * dx + dy * dy + mdx * mdx + mdy * mdy) * (gauss_s2 * gauss_s2);
			j += 9;
		}
		point.setDescriptor(desc, id, len);
		latch.countDown();
	}

	private double gaussian(double x, double y, double sig) {
		return (1.0f / (2.0f * Math.PI * sig * sig)) * Math.exp(-(x * x + y * y) / (2.0f * sig * sig));
	}

	private float haarX(int row, int column, int s) {
		return ImageTransformUtils.BoxIntegral(surf.getIntegralImage(), row - s / 2, column, s, s / 2) - 1 * ImageTransformUtils.BoxIntegral(surf.getIntegralImage(), row - s / 2, column - s / 2, s, s / 2);
	}

	private float haarY(int row, int column, int s) {
		return ImageTransformUtils.BoxIntegral(surf.getIntegralImage(), row, column - s / 2, s / 2, s) - 1 * ImageTransformUtils.BoxIntegral(surf.getIntegralImage(), row - s / 2, column - s / 2, s / 2, s);
	}

}
