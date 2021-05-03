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

public class Texture2DArray extends TextureArray {
    public final int w, h;
    private final boolean pot;

    public Texture2DArray(int w, int h, int n, DataBuffer.Usage usage, VectorFormat ifmt, VectorFormat efmt, DataBuffer.Filler<? super Image> init) {
	super(n, usage, ifmt, efmt, init);
	if((w < 0) || (h < 0))
	    throw(new IllegalArgumentException(String.format("Texture sizes must be non-negative, not (%d, %d, %d)", w, h, n)));
	this.w = w;
	this.h = h;
	this.pot = ((w & (w - 1)) == 0) && ((h & (h - 1)) == 0);
    }

    public Texture2DArray(int w, int h, int n, DataBuffer.Usage usage, VectorFormat ifmt, DataBuffer.Filler<? super Image> init) {
	this(w, h, n, usage, ifmt, ifmt, init);
    }

    public ArrayImage<Texture2DArray> image(int layer, int miplevel) {
	if((layer < 0) || (layer >= n))
	    throw(new IllegalArgumentException(Integer.toString(layer)));
	if((miplevel < 0) || (miplevel >= 32))
	    throw(new IllegalArgumentException(Integer.toString(miplevel)));
	if(miplevel > 0) {
	    if(!pot)
		throw(new IllegalArgumentException("Non-power-of-two textures cannot be mipmapped"));
	    if(((w >> miplevel) == 0) && ((h >> miplevel) == 0))
		throw(new IllegalArgumentException(String.format("Invalid mipmap level %d for (%d, %d) texture", miplevel, w, h)));
	}
	return(new ArrayImage<>(this, Math.max(w >> miplevel, 1), Math.max(h >> miplevel, 1), 1, layer, miplevel));
    }

    public Collection<ArrayImage<Texture2DArray>> images() {
	return(new AbstractCollection<ArrayImage<Texture2DArray>>() {
		final int levels = nlevels();

		int nlevels() {
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
		    return(levels * n);
		}

		public Iterator<ArrayImage<Texture2DArray>> iterator() {
		    return(new Iterator<ArrayImage<Texture2DArray>>() {
			    int lay = 0, lev = 0;
			    public boolean hasNext() {return((lay < n) || (lev < levels));}
			    public ArrayImage<Texture2DArray> next() {
				ArrayImage<Texture2DArray> ret = image(lay, lev);
				if(++lev >= levels) {
				    lay++;
				    lev = 0;
				}
				return(ret);
			    }
			});
		}
	    });
    }

    public static class Sampler2DArray extends Sampler<Texture2DArray> {
	public Sampler2DArray(Texture2DArray tex) {
	    super(tex);
	}
    }

    public String toString() {
	return(String.format("#<tex2d[] %sx%d %dx%d x %d>", ifmt.cf, ifmt.nc, w, h, n));
    }
}
