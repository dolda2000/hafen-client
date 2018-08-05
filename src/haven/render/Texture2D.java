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

public class Texture2D extends Texture {
    public final int w, h;
    private final boolean pot;

    public Texture2D(int w, int h, DataBuffer.Usage usage, VectorFormat ifmt, VectorFormat efmt, DataBuffer.Filler<? super Image> init) {
	super(usage, ifmt, efmt, init);
	if((w < 0) || (h < 0))
	    throw(new IllegalArgumentException(String.format("Texture sizes must be non-negative, not (%d, %d)", w, h)));
	this.w = w;
	this.h = h;
	this.pot = ((w & (w - 1)) == 0) && ((h & (h - 1)) == 0);
    }

    public Texture2D(int w, int h, DataBuffer.Usage usage, VectorFormat ifmt, DataBuffer.Filler<? super Image> init) {
	this(w, h, usage, ifmt, ifmt, init);
    }
    public Texture2D(Coord dim, DataBuffer.Usage usage, VectorFormat ifmt, VectorFormat efmt, DataBuffer.Filler<? super Image> init) {
	this(dim.x, dim.y, usage, ifmt, efmt, init);
    }
    public Texture2D(Coord dim, DataBuffer.Usage usage, VectorFormat ifmt, DataBuffer.Filler<? super Image> init) {
	this(dim, usage, ifmt, ifmt, init);
    }

    public Coord sz() {
	return(new Coord(w, h));
    }

    public Image<Texture2D> image(int level) {
	if((level < 0) || (level >= 32))
	    throw(new IllegalArgumentException(Integer.toString(level)));
	if(level > 0) {
	    if(!pot)
		throw(new IllegalArgumentException("Non-power-of-two textures cannot be mipmapped"));
	    if(((w >> level) == 0) && ((h >> level) == 0))
		throw(new IllegalArgumentException(String.format("Invalid mipmap level %d for (%d, %d) texture", level, w, h)));
	}
	return(new Image<>(this, Math.max(w >> level, 1), Math.max(h >> level, 1), 1, level));
    }

    public Collection<Image<Texture2D>> images() {
	return(new AbstractCollection<Image<Texture2D>>() {
		public int size() {
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

		public Iterator<Image<Texture2D>> iterator() {
		    return(new Iterator<Image<Texture2D>>() {
			    int i = 0, n = size();
			    public boolean hasNext() {return(i < n);}
			    public Image<Texture2D> next() {return(image(i++));}
			});
		}
	    });
    }

    public static class Sampler2D extends Sampler<Texture2D> {
	public Sampler2D(Texture2D tex) {
	    super(tex);
	}
    }

    public String toString() {
	return(String.format("#<tex2d %sx%d %dx%d>", ifmt.cf, ifmt.nc, w, h));
    }
}
