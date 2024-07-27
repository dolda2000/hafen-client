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

import java.util.*;
import java.awt.Graphics;
import java.awt.image.*;
import java.io.*;
import java.security.*;
import haven.Defer.Future;
import haven.render.*;
import static haven.render.Texture.Filter.*;

/* XXX: This should be an interface, but changing that would break resource code. */
public abstract class TexR extends Resource.Layer implements Resource.IDLayer<Integer> {
    public abstract TexL tex();

    public TexR(Resource res) {
	res.super();
    }

    public void init() {}

    @Resource.LayerName("tex")
    public static class Encoded extends TexR {
	transient private byte[] img, mask;
	transient private final TexL tex;
	private final Coord off, sz;
	public final int id;

	public Encoded(Resource res, Message buf) {
	    super(res);
	    this.id = buf.int16();
	    this.off = new Coord(buf.uint16(), buf.uint16());
	    this.sz = new Coord(buf.uint16(), buf.uint16());
	    this.tex = new Real();
	    Texture.Filter minfilter = null, magfilter = null, mipfilter = null;
	    while(!buf.eom()) {
		int t = buf.uint8();
		int fl = (t & 0xc0) >> 6;
		t = t & 0x3f;
		Message part;
		boolean fu;
		if(fl == 0) {
		    part = buf;
		    fu = true;
		} else if(fl == 1) {
		    part = new MessageBuf(buf.bytes(buf.uint8()));
		    fu = false;
		} else if(fl == 2) {
		    int sfl = buf.uint8();
		    part = new MessageBuf(buf.bytes(buf.int32()));
		    fu = false;
		} else {
		    throw(new Resource.LoadException("Unknown part flags " + fl + "  in " + res.name, getres()));
		}
		switch(t) {
		case 0:
		    this.img = part.bytes(part.int32());
		    break;
		case 1:
		    int ma = part.uint8();
		    tex.mipmap(new Mipmapper[] {
			    Mipmapper.avg, // Default
			    Mipmapper.avg, // Specific
			    Mipmapper.rnd,
			    Mipmapper.cnt,
			    Mipmapper.dav,
			}[ma]);
		    break;
		case 2:
		    int magf = part.uint8();
		    magfilter = new Texture.Filter[]{NEAREST, LINEAR}[magf];
		    break;
		case 3:
		    int minf = part.uint8();
		    minfilter = new Texture.Filter[] {NEAREST, LINEAR,
						      NEAREST, NEAREST,
						      LINEAR, LINEAR,
		    }[minf];
		    mipfilter = new Texture.Filter[] {null, null,
						      NEAREST, LINEAR,
						      NEAREST, LINEAR,
		    }[minf];
		    break;
		case 4:
		    this.mask = part.bytes(part.int32());
		    break;
		case 5:
		    /* Linear color values, not relevant right now */
		    break;
		default:
		    if(fu)
			throw(new Resource.LoadException("Unknown texture data part " + t + " in " + res.name, getres()));
		}
	    }
	    if(magfilter == null)
		magfilter = LINEAR;
	    if(minfilter == null) {
		minfilter = LINEAR;
		mipfilter = (tex.mipmap == null) ? null : LINEAR;
	    }
	    tex.img.magfilter(magfilter).minfilter(minfilter).mipfilter(mipfilter);
	}

	private class Real extends TexL {
	    private Real() {
		super(sz);
	    }

	    private BufferedImage rd(final byte[] data) {
		try {
		    return(Resource.readimage(new ByteArrayInputStream(data)));
		} catch(IOException e) {
		    throw(new RuntimeException("Invalid image data in " + getres().name, e));
		}
	    }

	    public BufferedImage fill() {
		if(mask == null) {
		    return(rd(Encoded.this.img));
		} else {
		    BufferedImage col = rd(Encoded.this.img);
		    BufferedImage mask = rd(Encoded.this.mask);
		    Coord sz = Utils.imgsz(mask);
		    BufferedImage ret = TexI.mkbuf(sz);
		    Graphics g = ret.createGraphics();
		    g.drawImage(col, 0, 0, sz.x, sz.y, null);
		    Raster mr = mask.getRaster();
		    if(mr.getNumBands() != 1)
			throw(new RuntimeException("Invalid separated alpha data in " + getres().name));
		    WritableRaster rr = ret.getRaster();
		    for(int y = 0; y < sz.y; y++) {
			for(int x = 0; x < sz.x; x++) {
			    rr.setSample(x, y, 3, mr.getSample(x, y, 0));
			}
		    }
		    g.dispose();
		    return(ret);
		}
	    }

	    public String toString() {
		return("#<texr " + getres().name + "(" + id + ")>");
	    }

	    public String loadname() {
		return("texture in " + getres().name);
	    }
	}

	public TexL tex() {
	    return(tex);
	}

	public Integer layerid() {
	    return(id);
	}
    }

    public static class Image extends TexR {
	public final TexL tex;
	public final int id;

	public Image(Resource res, TexL tex, int id) {
	    super(res);
	    this.tex = tex;
	    this.id = id;
	}

	public Image(Resource res, TexL tex) {
	    this(res, tex, -1);
	}

	public TexL tex() {
	    return(tex);
	}

	public Integer layerid() {
	    return(id);
	}
    }
}
