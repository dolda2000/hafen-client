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
import java.awt.image.*;
import java.io.*;
import javax.imageio.ImageIO;
import java.awt.color.ColorSpace;
import java.nio.ByteBuffer;
import javax.media.opengl.*;
import haven.Defer.Future;

@Resource.LayerName("tex")
public class TexR extends Resource.Layer implements Resource.IDLayer<Integer> {
    transient private byte[] img;
    transient private Mipmapper mipmap = null;
    transient private final Tex tex;
    private final Coord off, sz;
    private int minfilter = -1, magfilter = -1;
    public final int id;

    public TexR(Resource res, byte[] rbuf) {
	res.super();
	Message buf = new Message(0, rbuf);
	this.id = buf.int16();
	this.off = new Coord(buf.uint16(), buf.uint16());
	this.sz = new Coord(buf.uint16(), buf.uint16());
	while(!buf.eom()) {
	    int t = buf.uint8();
	    switch(t) {
	    case 0:
		this.img = buf.bytes(buf.int32());
		break;
	    case 1:
		int ma = buf.uint8();
		this.mipmap = new Mipmapper[] {
		    Mipmapper.avg, // Default
		    Mipmapper.avg, // Specific
		    Mipmapper.rnd,
		}[ma];
		break;
	    case 2:
		int magf = buf.uint8();
		this.magfilter = new int[] {GL.GL_NEAREST, GL.GL_LINEAR}[magf];
		break;
	    case 3:
		int minf = buf.uint8();
		this.minfilter = new int[] {GL.GL_NEAREST, GL.GL_LINEAR,
					    GL.GL_NEAREST_MIPMAP_NEAREST, GL.GL_NEAREST_MIPMAP_LINEAR,
					    GL.GL_LINEAR_MIPMAP_NEAREST, GL.GL_LINEAR_MIPMAP_LINEAR}[minf];
		break;
	    default:
		throw(new Resource.LoadException("Unknown texture data part " + t + " in " + res.name, getres()));
	    }
	}
	if(minfilter == -1)
	    minfilter = (mipmap == null)?GL.GL_LINEAR:GL.GL_LINEAR_MIPMAP_LINEAR;
	if(magfilter == -1)
	    magfilter = GL.GL_LINEAR;
	this.tex = new Texture();
    }

    private class Prepared {
	BufferedImage img;
	byte[][] data;
	int ifmt;

	private Prepared() {
	    try {
		img = ImageIO.read(new ByteArrayInputStream(TexR.this.img));
	    } catch(IOException e) {
		throw(new RuntimeException("Invalid image data in " + getres().name, e));
	    }
	    Coord sz = Utils.imgsz(img);
	    if((sz.x != Tex.nextp2(sz.x)) || (sz.y != Tex.nextp2(sz.y)))
		throw(new RuntimeException("Non-power-of-two texture in " + getres().name));
	    ifmt = TexI.detectfmt(img);
	    LinkedList<byte[]> data = new LinkedList<byte[]>();
	    if((ifmt == GL.GL_RGB) || (ifmt == GL2.GL_BGR)) {
		if((mipmap != null) && !(mipmap instanceof Mipmapper.Mipmapper3))
		    ifmt = -1;
	    }
	    if((ifmt == GL.GL_RGB) || (ifmt == GL2.GL_BGR)) {
		byte[] pixels = ((DataBufferByte)img.getRaster().getDataBuffer()).getData();
		data.add(pixels);
		if(mipmap != null) {
		    Coord msz = sz;
		    Mipmapper.Mipmapper3 alg = (Mipmapper.Mipmapper3)mipmap;
		    while((msz.x > 1) || (msz.y > 1)) {
			pixels = alg.gen3(msz, pixels, ifmt);
			data.add(pixels);
			msz = Mipmapper.nextsz(msz);
		    }
		}
	    } else {
		byte[] pixels;
		if((ifmt == GL.GL_RGBA) || (ifmt == GL.GL_BGRA)) {
		    pixels = ((DataBufferByte)img.getRaster().getDataBuffer()).getData();
		} else {
		    pixels = TexI.convert(img, sz);
		    ifmt = GL.GL_RGBA;
		}
		data.add(pixels);
		if(mipmap != null) {
		    Coord msz = sz;
		    while((msz.x > 1) || (msz.y > 1)) {
			pixels = mipmap.gen4(msz, pixels, ifmt);
			data.add(pixels);
			msz = Mipmapper.nextsz(msz);
		    }
		}
	    }
	    this.data = data.toArray(new byte[0][]);
	}
    }

    private Future<Prepared> prepare() {
	return(Defer.later(new Defer.Callable<Prepared>() {
		    public Prepared call() {
			Prepared ret = new Prepared();
			return(ret);
		    }
		}));
    }

    public class Texture extends TexGL {
	Future<Prepared> decode = null;

	private Texture() {
	    super(sz);
	    if((off.x != 0) || (off.y != 0))
		throw(new RuntimeException("Non-zero texture offsets are not supported yet."));
	    magfilter(TexR.this.magfilter);
	    minfilter(TexR.this.magfilter);
	}

	public void fill(GOut g) {
	    if(decode == null)
		decode = prepare();
	    Prepared prep;
	    try {
		prep = decode.get();
	    } catch(Loading l) {
		throw(RenderList.RLoad.wrap(l));
	    }
	    decode = null;
	    GL2 gl = g.gl;
	    gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
	    Coord cdim = tdim;
	    for(int i = 0; i < prep.data.length; i++) {
		gl.glTexImage2D(GL.GL_TEXTURE_2D, i, GL.GL_RGBA, cdim.x, cdim.y, 0, prep.ifmt, GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(prep.data[i]));
		cdim = Mipmapper.nextsz(cdim);
	    }
	}

	public String toString() {
	    return("TexR(" + getres().name + ", " + id + ")");
	}
    }

    public Tex tex() {
	return(tex);
    }

    public Integer layerid() {
	return(id);
    }

    public void init() {}
}
