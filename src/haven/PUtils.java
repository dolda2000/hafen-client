/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.*;

public class PUtils {
    public static Coord imgsz(BufferedImage img) {
	return(new Coord(img.getWidth(), img.getHeight()));
    }

    public static Coord imgsz(Raster img) {
	return(new Coord(img.getWidth(), img.getHeight()));
    }
	
    public static WritableRaster alpharaster(Coord sz) {
	return(Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, sz.x, sz.y, 1, null));
    }

    public static WritableRaster imgraster(Coord sz) {
	return(Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, sz.x, sz.y, 4, null));
    }

    public static BufferedImage rasterimg(WritableRaster img) {
	return(new BufferedImage(TexI.glcm, img, false, null));
    }

    public static WritableRaster imggrow(WritableRaster img, int rad) {
	int h = img.getHeight(), w = img.getWidth();
	int[] buf = new int[w * h];
	int o = 0;
	for(int y = 0; y < h; y++) {
	    for(int x = 0; x < w; x++) {
		int m = 0;
		int u = Math.max(0, y - rad), b = Math.min(h - 1, y + rad);
		int l = Math.max(0, x - rad), r = Math.min(w - 1, x + rad);
		for(int y2 = u; y2 <= b; y2++) {
		    for(int x2 = l; x2 <= r; x2++) {
			m = Math.max(m, img.getSample(x2, y2, 0));
		    }
		}
		buf[o++] = m;
	    }
	}
	img.setSamples(0, 0, w, h, 0, buf);
	return(img);
    }

    public static WritableRaster imgblur(WritableRaster img, int rad, double var) {
	int h = img.getHeight(), w = img.getWidth();
	double[] gk = new double[(rad * 2) + 1];
	for(int i = 0; i <= rad; i++)
	    gk[rad + i] = gk[rad - i] = Math.exp(-0.5 * Math.pow(i / var, 2.0));
	double s = 0;
	for(double cw : gk) s += cw;
	s = 1.0 / s;
	for(int i = 0; i <= rad * 2; i++)
	    gk[i] *= s;
	int[] buf = new int[w * h];
	for(int band = 0; band < img.getNumBands(); band++) {
	    int o;
	    o = 0;
	    for(int y = 0; y < h; y++) {
		for(int x = 0; x < w; x++) {
		    double v = 0;
		    int l = Math.max(0, x - rad), r = Math.min(w - 1, x + rad);
		    for(int x2 = l, ks = l - (x - rad); x2 <= r; x2++, ks++)
			v += img.getSample(x2, y, band) * gk[ks];
		    buf[o++] = (int)v;
		}
	    }
	    img.setSamples(0, 0, w, h, band, buf);
	    o = 0;
	    for(int y = 0; y < h; y++) {
		for(int x = 0; x < w; x++) {
		    double v = 0;
		    int u = Math.max(0, y - rad), b = Math.min(h - 1, y + rad);
		    for(int y2 = u, ks = u - (y - rad); y2 <= b; y2++, ks++)
			v += img.getSample(x, y2, band) * gk[ks];
		    buf[o++] = (int)v;
		}
	    }
	    img.setSamples(0, 0, w, h, band, buf);
	}
	return(img);
    }

    public static WritableRaster alphadraw(WritableRaster dst, Raster alpha, Coord ul, Color col) {
	int r = col.getRed(), g = col.getGreen(), b = col.getBlue(), ba = col.getAlpha();
	int w = alpha.getWidth(), h = alpha.getHeight();
	for(int y = 0; y < h; y++) {
	    for(int x = 0; x < w; x++) {
		int a = (alpha.getSample(x, y, 0) * ba) / 255;
		int dx = x + ul.x, dy = y + ul.y;
		dst.setSample(dx, dy, 0, ((r * a) + (dst.getSample(dx, dy, 0) * (255 - a))) / 255);
		dst.setSample(dx, dy, 1, ((g * a) + (dst.getSample(dx, dy, 1) * (255 - a))) / 255);
		dst.setSample(dx, dy, 2, ((b * a) + (dst.getSample(dx, dy, 2) * (255 - a))) / 255);
		dst.setSample(dx, dy, 3, Math.max((ba * a) / 255, dst.getSample(dx, dy, 3)));
	    }
	}
	return(dst);
    }

    public static WritableRaster alphablit(WritableRaster dst, Raster src, Coord off) {
	int w = src.getWidth(), h = src.getHeight();
	for(int y = 0; y < h; y++) {
	    for(int x = 0; x < w; x++) {
		int a = src.getSample(x, y, 3);
		int dx = x + off.x, dy = y + off.y;
		dst.setSample(dx, dy, 0, ((src.getSample(x, y, 0) * a) + (dst.getSample(dx, dy, 0) * (255 - a))) / 255);
		dst.setSample(dx, dy, 1, ((src.getSample(x, y, 1) * a) + (dst.getSample(dx, dy, 1) * (255 - a))) / 255);
		dst.setSample(dx, dy, 2, ((src.getSample(x, y, 2) * a) + (dst.getSample(dx, dy, 2) * (255 - a))) / 255);
		dst.setSample(dx, dy, 3, Math.max(src.getSample(x, y, 3), dst.getSample(dx, dy, 3)));
	    }
	}
	return(dst);
    }

    public static WritableRaster copyband(WritableRaster dst, int dband, Coord doff, Raster src, int sband, Coord soff, Coord sz) {
	dst.setSamples(doff.x, doff.y, sz.x, sz.y, dband, src.getSamples(soff.x, soff.y, sz.x, sz.y, sband, (int[])null));
	return(dst);
    }

    public static WritableRaster copyband(WritableRaster dst, int dband, Coord doff, Raster src, int sband) {
	return(copyband(dst, dband, doff, src, sband, Coord.z, imgsz(src)));
    }

    public static WritableRaster copyband(WritableRaster dst, int dband, Raster src, int sband) {
	return(copyband(dst, dband, Coord.z, src, sband));
    }

    public static WritableRaster blurmask(Raster img, int grad, int brad, Color col) {
	Coord marg = new Coord(grad + brad, grad + brad), sz = imgsz(img).add(marg.mul(2));
	return(alphadraw(imgraster(sz), imgblur(imggrow(copyband(alpharaster(sz), 0, marg, img, 3), grad), brad, brad), Coord.z, col));
    }

    public static WritableRaster blurmask2(Raster img, int grad, int brad, Color col) {
	return(alphablit(blurmask(img, grad, brad, col), img, new Coord(grad + brad, grad + brad)));
    }

    public static void dumpband(Raster img, int band) {
	int w = img.getWidth(), h = img.getHeight();
	for(int y = 0; y < h; y++) {
	    for(int x = 0; x < w; x++) {
		System.err.print((char)('a' + ((img.getSample(x, y, band) * ('z' - 'a')) / 255)));
	    }
	    System.err.println();
	}
    }
    
    public static BufferedImage monochromize(BufferedImage img, Color col) {
	Coord sz = Utils.imgsz(img);
	BufferedImage ret = TexI.mkbuf(sz);
	Raster src = img.getRaster();
	WritableRaster dst = ret.getRaster();
	for(int y = 0; y < sz.y; y++) {
	    for(int x = 0; x < sz.x; x++) {
		int r = src.getSample(x, y, 0),
		    g = src.getSample(x, y, 1),
		    b = src.getSample(x, y, 2),
		    a = src.getSample(x, y, 3);
		int max = Math.max(r, Math.max(g, b)),
		    min = Math.min(r, Math.min(g, b));
		int val = (max + min) / 2;
		dst.setSample(x, y, 0, (col.getRed()   * val) / 255);
		dst.setSample(x, y, 1, (col.getGreen() * val) / 255);
		dst.setSample(x, y, 2, (col.getBlue()  * val) / 255);
		dst.setSample(x, y, 3, a);
	    }
	}
	return(ret);
    }
}
