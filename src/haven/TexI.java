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
import haven.render.*;
import haven.render.Texture2D.Sampler2D;
import haven.render.DataBuffer;

public class TexI implements Tex {
    public static ComponentColorModel glcm = PUtils.cm_rgba;
    public final BufferedImage back;
    protected final Coord sz;
    protected final Coord tdim;

    public TexI(BufferedImage back, boolean round) {
	this.back = back;
	this.sz = Utils.imgsz(back);
	if(round)
	    this.tdim = new Coord(Tex.nextp2(sz.x), Tex.nextp2(sz.y));
	else
	    this.tdim = sz;
    }

    public TexI(BufferedImage back) {
	this(back, true);
    }

    public Coord sz() {return(sz);}

    private ColorTex st = null;
    public ColorTex st() {
	ColorTex st = this.st;
	if(st == null) {
	    synchronized(this) {
		if(this.st == null) {
		    Texture2D tex = new Texture2D(tdim.x, tdim.y, DataBuffer.Usage.STATIC, new VectorFormat(4, NumberFormat.UNORM8), new VectorFormat(4, NumberFormat.UNORM8),
						  (img, env) -> {
						      if(img.level != 0)
							  return(null);
						      FillBuffer buf = env.fillbuf(img);
						      if(Utils.eq(tdim, sz) && Utils.eq(detectfmt(back), img.tex.efmt)) {
							  buf.pull(ByteBuffer.wrap(((DataBufferByte)back.getRaster().getDataBuffer()).getData()));
						      } else {
							  buf.pull(ByteBuffer.wrap(convert(back, tdim)));
						      }
						      return(buf);
						  });
		    tex.desc(this);
		    Sampler2D data = new Sampler2D(tex);
		    data.magfilter(Texture.Filter.NEAREST).minfilter(Texture.Filter.NEAREST);
		    st = this.st = new ColorTex(data);
		}
	    }
	}
	return(st);
    }

    public TexI magfilter(Texture.Filter filter) {
	st().data.magfilter(filter);
	return(this);
    }
    public TexI minfilter(Texture.Filter filter) {
	st().data.minfilter(filter);
	return(this);
    }
    public TexI filter(Texture.Filter filter) {
	magfilter(filter);
	minfilter(filter);
	return(this);
    }
    public TexI wrapmode(Texture.Wrapping mode) {
	st().data.wrapmode(mode);
	return(this);
    }

    public void render(GOut g, float[] gc, float[] tc) {
	float ix = 1.0f / tdim.x, iy = 1.0f / tdim.y;
	float[] data = {
	    gc[2], gc[3], tc[2] * ix, tc[3] * iy,
	    gc[4], gc[5], tc[4] * ix, tc[5] * iy,
	    gc[0], gc[1], tc[0] * ix, tc[1] * iy,
	    gc[6], gc[7], tc[6] * ix, tc[7] * iy,
	};
	g.usestate(st());
	g.drawt(Model.Mode.TRIANGLE_STRIP, data);
	g.usestate(ColorTex.slot);
    }

    public void dispose() {
	synchronized(this) {
	    if(st != null) {
		st.data.dispose();
		st = null;
	    }
	}
    }

    /* Java's image model is a wee bit complex, so these may not be
     * entirely correct. */
    public static VectorFormat detectfmt(BufferedImage img) {
	ColorModel cm = img.getColorModel();
	if(!(img.getSampleModel() instanceof PixelInterleavedSampleModel))
	    return(null);
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
	       (cm.getTransferType() == java.awt.image.DataBuffer.TYPE_BYTE) &&
	       (cm.getTransparency() == java.awt.Transparency.TRANSLUCENT))
	    {
		if((off[0] == 0) && (off[1] == 1) && (off[2] == 2) && (off[3] == 3))
		    return(new VectorFormat(4, NumberFormat.UNORM8));
		/* XXXRENDER: Support component swizzling:
		if((off[0] == 2) && (off[1] == 1) && (off[2] == 0) && (off[3] == 3))
		    return(GL.GL_BGRA);
		*/
	    }
	} else if((cm.getNumComponents() == 3) && (off.length == 3)) {
	    if(((cs[0] == 8) && (cs[1] == 8) && (cs[2] == 8)) &&
	       (cm.getTransferType() == java.awt.image.DataBuffer.TYPE_BYTE) &&
	       (cm.getTransparency() == java.awt.Transparency.OPAQUE))
	    {
		if((off[0] == 0) && (off[1] == 1) && (off[2] == 2))
		    return(new VectorFormat(3, NumberFormat.UNORM8));
		/* XXXRENDER: Support component swizzling:
		if((off[0] == 2) && (off[1] == 1) && (off[2] == 0))
		    return(GL2.GL_BGR);
		*/
	    }
	}
	return(null);
    }
    public static BufferedImage mkbuf(Coord sz) {
	WritableRaster buf = Raster.createInterleavedRaster(java.awt.image.DataBuffer.TYPE_BYTE, sz.x, sz.y, 4, null);
	BufferedImage tgt = new BufferedImage(glcm, buf, false, null);
	return(tgt);
    }

    public static byte[] convert(BufferedImage img, Coord tsz, Coord ul, Coord sz) {
	WritableRaster buf = Raster.createInterleavedRaster(java.awt.image.DataBuffer.TYPE_BYTE, tsz.x, tsz.y, 4, null);
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
