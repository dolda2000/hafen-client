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

public class Texture3D extends Texture {
    public final int w, h, d;
    private final boolean pot;

    public Texture3D(int w, int h, int d, DataBuffer.Usage usage, VectorFormat ifmt, VectorFormat efmt, DataBuffer.Filler<? super Image> init) {
	super(usage, ifmt, efmt, init);
	if((w < 0) || (h < 0) || (d < 0))
	    throw(new IllegalArgumentException(String.format("Texture sizes must be non-negative, not (%d, %d, %d)", w, h, d)));
	this.w = w;
	this.h = h;
	this.d = d;
	this.pot = ((w & (w - 1)) == 0) && ((h & (h - 1)) == 0) && ((d & (d - 1)) == 0);
    }

    public Texture3D(int w, int h, int d, DataBuffer.Usage usage, VectorFormat ifmt, DataBuffer.Filler<? super Image> init) {
	this(w, h, d, usage, ifmt, ifmt, init);
    }

    public Image<Texture3D> image(int level) {
	if((level < 0) || (level >= 32))
	    throw(new IllegalArgumentException(Integer.toString(level)));
	if(level > 0) {
	    if(!pot)
		throw(new IllegalArgumentException("Non-power-of-two textures cannot be mipmapped"));
	    if(((w >> level) == 0) && ((h >> level) == 0) && ((d >> level) == 0))
		throw(new IllegalArgumentException(String.format("Invalid mipmap level %d for (%d, %d, %d) texture", level, w, h, d)));
	}
	return(new Image<>(this, Math.max(w >> level, 1), Math.max(h >> level, 1), Math.max(d >> level, 1), level));
    }

    public Collection<Image<Texture3D>> images() {
	return(new AbstractCollection<Image<Texture3D>>() {
		public int size() {
		    if((w == 0) || (h == 0) || (d == 0)) {
			return(0);
		    } else if(pot) {
			return(Math.max(Math.max(Integer.numberOfTrailingZeros(w),
						 Integer.numberOfTrailingZeros(h)),
					Integer.numberOfTrailingZeros(d)) +
			       1);
		    } else {
			return(1);
		    }
		}

		public Iterator<Image<Texture3D>> iterator() {
		    return(new Iterator<Image<Texture3D>>() {
			    int i = 0, n = size();
			    public boolean hasNext() {return(i < n);}
			    public Image<Texture3D> next() {return(image(i++));}
			});
		}
	    });
    }

    public static class Sampler3D extends Sampler<Texture3D> {
	public Sampler3D(Texture3D tex) {
	    super(tex);
	}
    }

    public String toString() {
	return(String.format("#<tex3d %sx%d %dx%dx%d>", ifmt.cf, ifmt.nc, w, h, d));
    }
}
