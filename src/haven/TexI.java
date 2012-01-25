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

import java.awt.Graphics;
import java.awt.image.*;
import java.awt.color.ColorSpace;
import java.nio.ByteBuffer;
import javax.media.opengl.*;

public class TexI extends TexGL {
    public static ComponentColorModel glcm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[] {8, 8, 8, 8}, true, false, ComponentColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE);
    public BufferedImage back;
    private int fmt = GL.GL_RGBA;

    public TexI(BufferedImage img) {
	super(Utils.imgsz(img));
	back = img;
    }

    public TexI(Coord sz) {
	super(sz);
    }
    
    /* Java's image model is a little bit complex, so these may not be
     * entirely correct. They should be corrected if oddities are
     * detected. */
    private int detectfmt(BufferedImage img) {
	ColorModel cm = img.getColorModel();
	if(!(img.getSampleModel() instanceof PixelInterleavedSampleModel))
	    return(-1);
	PixelInterleavedSampleModel sm = (PixelInterleavedSampleModel)img.getSampleModel();
	int[] cs = cm.getComponentSize();
	int[] off = sm.getBandOffsets();
	/*
	System.err.print(this + ": " + cm.getNumComponents() + ", (");
	for(int i = 0; i < off.length; i++)
	    System.err.print(((i > 0)?" ":"") + off[i]);
	System.err.print("), (");
	for(int i = 0; i < off.length; i++)
	    System.err.print(((i > 0)?" ":"") + cs[i]);
	System.err.print(")");
	System.err.println();
	*/
	if((cm.getNumComponents() == 4) && (off.length == 4)) {
	    if(((cs[0] == 8) && (cs[1] == 8) && (cs[2] == 8) && (cs[3] == 8)) &&
	       (cm.getTransferType() == DataBuffer.TYPE_BYTE) &&
	       (cm.getTransparency() == java.awt.Transparency.TRANSLUCENT)) {
		if((off[0] == 0) && (off[1] == 1) && (off[2] == 2) && (off[3] == 3))
		    return(GL.GL_RGBA);
		if((off[0] == 2) && (off[1] == 1) && (off[2] == 0) && (off[3] == 3))
		    return(GL.GL_BGRA);
	    }
	} else if((cm.getNumComponents() == 3) && (off.length == 3)) {
	    if(((cs[0] == 8) && (cs[1] == 8) && (cs[2] == 8)) &&
	       (cm.getTransferType() == DataBuffer.TYPE_BYTE) &&
	       (cm.getTransparency() == java.awt.Transparency.OPAQUE)) {
		if((off[0] == 0) && (off[1] == 1) && (off[2] == 2))
		    return(GL.GL_RGB);
		if((off[0] == 2) && (off[1] == 1) && (off[2] == 0))
		    return(GL.GL_BGR);
	    }
	}
	return(-1);
    }

    protected void fill(GOut g) {
	GL gl = g.gl;
	Coord sz = Utils.imgsz(back);
	int ifmt = detectfmt(back);
	if((ifmt == GL.GL_RGBA) || (ifmt == GL.GL_BGRA)) {
	    byte[] pixels = ((DataBufferByte)back.getRaster().getDataBuffer()).getData();
	    if(sz.equals(tdim)) {
		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, fmt, tdim.x, tdim.y, 0, ifmt, GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(pixels));
		if(mipmap)
		    genmipmap(gl, 1, tdim, pixels, ifmt);
	    } else {
		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, fmt, tdim.x, tdim.y, 0, ifmt, GL.GL_UNSIGNED_BYTE, null);
		gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, sz.x, sz.y, ifmt, GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(pixels));
	    }
	} else if((ifmt == GL.GL_RGB) || (ifmt == GL.GL_BGR)) {
	    gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
	    byte[] pixels = ((DataBufferByte)back.getRaster().getDataBuffer()).getData();
	    if(sz.equals(tdim)) {
		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, fmt, tdim.x, tdim.y, 0, ifmt, GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(pixels));
		if(mipmap)
		    genmipmap3(gl, 1, tdim, pixels, ifmt);
	    } else {
		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, fmt, tdim.x, tdim.y, 0, ifmt, GL.GL_UNSIGNED_BYTE, null);
		gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, sz.x, sz.y, ifmt, GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(pixels));
	    }
	} else {
	    /* System.err.println("Weird: " + this); */
	    byte[] pixels = convert(back, tdim);
	    gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, fmt, tdim.x, tdim.y, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(pixels));
	    if(mipmap)
		genmipmap(gl, 1, tdim, pixels, GL.GL_RGBA);
	}
    }
    
    private void genmipmap(GL gl, int lev, Coord dim, byte[] data, int ifmt) {
	int dst = dim.x * 4;
	dim = dim.div(2);
	boolean lx = false, ly = false;
	if(dim.x < 1) {dim.x = 1; lx = true;}
	if(dim.y < 1) {dim.y = 1; ly = true;}
	byte[] ndata = new byte[dim.x * dim.y * 4];
	int[] r = new int[4], g = new int[4], b = new int[4], a = new int[4];
	int na = 0, da = 0;
	for(int y = 0; y < dim.y; y++) {
	    for(int x = 0; x < dim.x; x++) {
		r[0] = ((int)data[da + 0]) & 0xff;
		g[0] = ((int)data[da + 1]) & 0xff;
		b[0] = ((int)data[da + 2]) & 0xff;
		a[0] = ((int)data[da + 3]) & 0xff;
		if(!lx) {
		    r[1] = ((int)data[da + 0 + 4]) & 0xff;
		    g[1] = ((int)data[da + 1 + 4]) & 0xff;
		    b[1] = ((int)data[da + 2 + 4]) & 0xff;
		    a[1] = ((int)data[da + 3 + 4]) & 0xff;
		} else {
		    r[1] = r[0]; g[1] = g[0]; b[1] = b[0]; a[1] = a[0];
		}
		if(!ly) {
		    r[2] = ((int)data[da + 0 + dst]) & 0xff;
		    g[2] = ((int)data[da + 1 + dst]) & 0xff;
		    b[2] = ((int)data[da + 2 + dst]) & 0xff;
		    a[2] = ((int)data[da + 3 + dst]) & 0xff;
		} else {
		    r[2] = r[0]; g[2] = g[0]; b[2] = b[0]; a[2] = a[0];
		}
		if(!lx && !ly) {
		    r[3] = ((int)data[da + 0 + dst + 4]) & 0xff;
		    g[3] = ((int)data[da + 1 + dst + 4]) & 0xff;
		    b[3] = ((int)data[da + 2 + dst + 4]) & 0xff;
		    a[3] = ((int)data[da + 3 + dst + 4]) & 0xff;
		} else if(!ly) {
		    r[3] = r[2]; g[3] = g[2]; b[3] = b[2]; a[3] = a[2];
		} else {
		    r[3] = r[1]; g[3] = g[1]; b[3] = b[1]; a[3] = a[1];
		}
		int n = 0, cr = 0, cg = 0, cb = 0;
		for(int i = 0; i < 4; i++) {
		    if(a[i] < 128)
			continue;
		    cr += r[i];
		    cg += g[i];
		    cb += b[i];
		    n++;
		}
		if(n <= 1) {
		    ndata[na + 3] = 0;
		} else {
		    ndata[na + 0] = (byte)(cr / n);
		    ndata[na + 1] = (byte)(cg / n);
		    ndata[na + 2] = (byte)(cb / n);
		    ndata[na + 3] = (byte)255;
		}
		na += 4;
		da += lx?4:8;
	    }
	    da += ly?0:dst;
	}
	gl.glTexImage2D(GL.GL_TEXTURE_2D, lev, fmt, dim.x, dim.y, 0, ifmt, GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(ndata));
	if((dim.x > 1) || (dim.y > 1))
	    genmipmap(gl, lev + 1, dim, ndata, ifmt);
    }
	
    private void genmipmap3(GL gl, int lev, Coord dim, byte[] data, int ifmt) {
	int dst = dim.x * 3;
	dim = dim.div(2);
	boolean lx = false, ly = false;
	if(dim.x < 1) {dim.x = 1; lx = true;}
	if(dim.y < 1) {dim.y = 1; ly = true;}
	byte[] ndata = new byte[dim.x * dim.y * 3];
	int[] r = new int[4], g = new int[4], b = new int[4];
	int na = 0, da = 0;
	for(int y = 0; y < dim.y; y++) {
	    for(int x = 0; x < dim.x; x++) {
		r[0] = ((int)data[da + 0]) & 0xff;
		g[0] = ((int)data[da + 1]) & 0xff;
		b[0] = ((int)data[da + 2]) & 0xff;
		if(!lx) {
		    r[1] = ((int)data[da + 0 + 3]) & 0xff;
		    g[1] = ((int)data[da + 1 + 3]) & 0xff;
		    b[1] = ((int)data[da + 2 + 3]) & 0xff;
		} else {
		    r[1] = r[0]; g[1] = g[0]; b[1] = b[0];
		}
		if(!ly) {
		    r[2] = ((int)data[da + 0 + dst]) & 0xff;
		    g[2] = ((int)data[da + 1 + dst]) & 0xff;
		    b[2] = ((int)data[da + 2 + dst]) & 0xff;
		} else {
		    r[2] = r[0]; g[2] = g[0]; b[2] = b[0];
		}
		if(!lx && !ly) {
		    r[3] = ((int)data[da + 0 + dst + 3]) & 0xff;
		    g[3] = ((int)data[da + 1 + dst + 3]) & 0xff;
		    b[3] = ((int)data[da + 2 + dst + 3]) & 0xff;
		} else if(!ly) {
		    r[3] = r[2]; g[3] = g[2]; b[3] = b[2];
		} else {
		    r[3] = r[1]; g[3] = g[1]; b[3] = b[1];
		}
		int cr = 0, cg = 0, cb = 0;
		for(int i = 0; i < 4; i++) {
		    cr += r[i];
		    cg += g[i];
		    cb += b[i];
		}
		ndata[na + 0] = (byte)(cr / 4);
		ndata[na + 1] = (byte)(cg / 4);
		ndata[na + 2] = (byte)(cb / 4);
		na += 3;
		da += lx?3:6;
	    }
	    da += ly?0:dst;
	}
	gl.glTexImage2D(GL.GL_TEXTURE_2D, lev, fmt, dim.x, dim.y, 0, ifmt, GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(ndata));
	if((dim.x > 1) || (dim.y > 1))
	    genmipmap3(gl, lev + 1, dim, ndata, ifmt);
    }
	
    public int getRGB(Coord c) {
	return(back.getRGB(c.x, c.y));
    }
	
    public TexI mkmask() {
	TexI n = new TexI(back);
	n.fmt = GL.GL_ALPHA;
	return(n);
    }
	
    public static BufferedImage mkbuf(Coord sz) {
	WritableRaster buf = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, sz.x, sz.y, 4, null);
	BufferedImage tgt = new BufferedImage(glcm, buf, false, null);
	return(tgt);
    }
	
    public static byte[] convert(BufferedImage img, Coord tsz, Coord ul, Coord sz) {
	WritableRaster buf = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, tsz.x, tsz.y, 4, null);
	BufferedImage tgt = new BufferedImage(glcm, buf, false, null);
	Graphics g = tgt.createGraphics();
	g.drawImage(img, 0, 0, sz.x, sz.y, ul.x, ul.y, ul.x + sz.x, ul.y + sz.y, null);
	g.dispose();
	return(((DataBufferByte)buf.getDataBuffer()).getData());
    }

    public static byte[] convert(BufferedImage img, Coord tsz) {
	return(convert(img, tsz, Coord.z, Utils.imgsz(img)));
    }
}
