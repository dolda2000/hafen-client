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
import java.nio.ByteBuffer;
import haven.render.*;
import haven.render.DataBuffer;
import haven.render.Texture2D.Sampler2D;
import haven.render.Texture.Image;
import haven.Defer.Future;

public abstract class TexL extends TexRender {
    protected Mipmapper mipmap = null;
    private Future<Prepared> decode = null;

    public abstract BufferedImage fill();

    private static class Filler implements DataBuffer.Filler<Image> {
	private TexL tex;

	public FillBuffer fill(Image img, Environment env) {
	    return(tex.fill(img, env));
	}

	public void done() {
	    tex.decode = null;
	}
    }

    private static Sampler2D mkimg(Coord sz) {
	if((sz.x != Tex.nextp2(sz.x)) || (sz.y != Tex.nextp2(sz.y)))
	    throw(new RuntimeException("TexL does not support non-power-of-two textures"));
	Texture2D tex = new Texture2D(sz.x, sz.y, DataBuffer.Usage.STATIC, new VectorFormat(4, NumberFormat.UNORM8), new VectorFormat(4, NumberFormat.UNORM8), new Filler());
	return(new Sampler2D(tex));
    }

    public TexL(Coord sz) {
	super(mkimg(sz));
	((Filler)img.tex.init).tex = this;
	img.tex.desc(this);
    }

    public void mipmap(Mipmapper mipmap) {
	this.mipmap = mipmap;
    }

    private class Prepared {
	final Environment env;
	FillBuffer[] data;

	private FillBuffer filldata(DataBuffer tgt, byte[] pixels) {
	    FillBuffer buf = env.fillbuf(tgt);
	    buf.pull(ByteBuffer.wrap(pixels));
	    return(buf);
	}

	private Prepared(Environment env) {
	    this.env = env;
	    Texture2D tex = TexL.this.img.tex;
	    BufferedImage img = fill();
	    if(!Utils.imgsz(img).equals(tex.sz()))
		throw(new RuntimeException("Generated TexL image from " + TexL.this + " does not match declared size"));
	    VectorFormat ifmt = TexI.detectfmt(img);
	    FillBuffer[] data = new FillBuffer[tex.images().size()];
	    if((ifmt != null) && (ifmt.nc == 3)) {
		if((mipmap != null) && !(mipmap instanceof Mipmapper.Mipmapper3))
		    ifmt = null;
	    }
	    /* XXXRENDER
	    if((ifmt != null) && (ifmt.nc == 3)) {
		byte[] pixels = ((DataBufferByte)img.getRaster().getDataBuffer()).getData();
		data.add(pixels);
		if(mipmap != null) {
		    Coord msz = dim;
		    Mipmapper.Mipmapper3 alg = (Mipmapper.Mipmapper3)mipmap;
		    while((msz.x > 1) || (msz.y > 1)) {
			pixels = alg.gen3(msz, pixels, ifmt);
			data.add(pixels);
			msz = Mipmapper.nextsz(msz);
		    }
		}
	    } else {
	    */
	    byte[] pixels;
	    if((ifmt != null) && (ifmt.nc == 4) && (ifmt.cf == NumberFormat.UNORM8)) {
		pixels = ((DataBufferByte)img.getRaster().getDataBuffer()).getData();
	    } else {
		pixels = TexI.convert(img, tex.sz());
		ifmt = new VectorFormat(4, NumberFormat.UNORM8);
	    }
	    data[0] = filldata(tex.image(0), pixels);
	    if(mipmap != null) {
		Coord msz = tex.sz();
		int level = 1;
		while((msz.x > 1) || (msz.y > 1)) {
		    pixels = mipmap.gen4(msz, pixels, ifmt);
		    data[level] = filldata(tex.image(level), pixels);
		    msz = Mipmapper.nextsz(msz);
		    level++;
		}
	    }
	    this.data = data;
	}

	void dispose() {
	    if(data != null) {
		for(int i = 0; i < data.length; i++) {
		    if(data[i] != null)
			data[i].dispose();
		}
		data = null;
	    }
	}
    }

    private Prepared prepare(Environment env) {
	while(true) {
	    synchronized(this) {
		if(this.decode == null) {
		    this.decode = Defer.later(new Defer.Callable<Prepared>() {
			    public Prepared call() {
				return(new Prepared(env));
			    }

			    public String toString() {
				String nm = loadname();
				if(nm != null)
				    return("Finalizing " + nm + "...");
				else
				    return("Finalizing texture...");
			    }
			});
		}
		Prepared ret = this.decode.get();
		if(ret.env == env)
		    return(ret);
		ret.dispose();
		this.decode = null;
	    }
	}
    }

    public String loadname() {
	return(null);
    }

    private FillBuffer fill(Image img, Environment env) {
	return(prepare(env).data[img.level]);
    }

    public static class Fixed extends TexL {
	public final BufferedImage img;

	public Fixed(BufferedImage img) {
	    super(PUtils.imgsz(img));
	    this.img = img;
	}

	public BufferedImage fill() {
	    return(img);
	}
    }
}
