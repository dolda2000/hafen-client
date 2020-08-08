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

package haven.render;

import haven.*;
import java.util.*;

public class TextureCube extends Texture {
    public final int w, h;
    private final boolean pot;

    public enum Face {
	XP, XN, YP, YN, ZP, ZN
    }

    public TextureCube(int w, int h, DataBuffer.Usage usage, VectorFormat ifmt, VectorFormat efmt, DataBuffer.Filler<? super Image> init) {
	super(usage, ifmt, efmt, init);
	if((w < 0) || (h < 0))
	    throw(new IllegalArgumentException(String.format("Texture sizes must be non-negative, not (%d, %d)", w, h)));
	this.w = w;
	this.h = h;
	this.pot = ((w & (w - 1)) == 0) && ((h & (h - 1)) == 0);
    }

    public TextureCube(int w, int h, DataBuffer.Usage usage, VectorFormat ifmt, DataBuffer.Filler<? super Image> init) {
	this(w, h, usage, ifmt, ifmt, init);
    }
    public TextureCube(Coord dim, DataBuffer.Usage usage, VectorFormat ifmt, VectorFormat efmt, DataBuffer.Filler<? super Image> init) {
	this(dim.x, dim.y, usage, ifmt, efmt, init);
    }
    public TextureCube(Coord dim, DataBuffer.Usage usage, VectorFormat ifmt, DataBuffer.Filler<? super Image> init) {
	this(dim, usage, ifmt, ifmt, init);
    }

    public Coord sz() {
	return(new Coord(w, h));
    }

    public static class CubeImage extends Image<TextureCube> {
	public final Face face;

	public CubeImage(TextureCube tex, int w, int h, Face face, int level) {
	    super(tex, w, h, 1, level);
	    this.face = face;
	}

	public boolean equals(CubeImage that) {
	    return(equals((Image)that) && (this.face == that.face));
	}

	public boolean equals(Object that) {
	    return((that.getClass() == CubeImage.class) ? equals((CubeImage)that) : false);
	}

	public String toString() {
	    return(String.format("#<texcube.image %s %d %dx%dx%d, %s>", tex, level, w, h, d, face));
	}
    }

    public CubeImage image(Face face, int level) {
	if((level < 0) || (level >= 32))
	    throw(new IllegalArgumentException(Integer.toString(level)));
	if(level > 0) {
	    if(!pot)
		throw(new IllegalArgumentException("Non-power-of-two textures cannot be mipmapped"));
	    if(((w >> level) == 0) && ((h >> level) == 0))
		throw(new IllegalArgumentException(String.format("Invalid mipmap level %d for (%d, %d) texture", level, w, h)));
	}
	return(new CubeImage(this, Math.max(w >> level, 1), Math.max(h >> level, 1), face, level));
    }

    public Collection<CubeImage> images() {
	return(new AbstractCollection<CubeImage>() {
		private int msize() {
		    if((w == 0) || (h == 0)) {
			return(0);
		    } else if(pot) {
			return(Math.max(Integer.numberOfTrailingZeros(w),
					Integer.numberOfTrailingZeros(h)) +
			       1);
		    } else {
			return(1);
		    }
		}

		public int size() {
		    return(msize() * Face.values().length);
		}

		public Iterator<CubeImage> iterator() {
		    return(new Iterator<CubeImage>() {
			    int i = 0, n = msize(), f = 0;
			    public boolean hasNext() {return(f < Face.values().length);}
			    public CubeImage next() {
				CubeImage ret = image(Face.values()[f], i++);
				if(i >= n) {
				    i = 0;
				    f++;
				}
				return(ret);
			    }
			});
		}
	    });
    }

    public static class SamplerCube extends Sampler<TextureCube> {
	public SamplerCube(TextureCube tex) {
	    super(tex);
	}
    }

    public String toString() {
	return(String.format("#<texcube %sx%d %dx%d>", ifmt.cf, ifmt.nc, w, h));
    }
}
