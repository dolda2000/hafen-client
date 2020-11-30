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

public class Texture2DMS extends Texture {
    public final int w, h, s;
    public final boolean fixed;

    public Texture2DMS(int w, int h, int s, boolean fixed, VectorFormat ifmt) {
	super(DataBuffer.Usage.STATIC, ifmt, ifmt, null);
	if((w < 0) || (h < 0))
	    throw(new IllegalArgumentException(String.format("Texture sizes must be non-negative, not (%d, %d)", w, h)));
	this.w = w;
	this.h = h;
	this.s = s;
	this.fixed = fixed;
    }

    public Texture2DMS(Coord dim, int s, boolean fixed, VectorFormat ifmt) {
	this(dim.x, dim.y, s, fixed, ifmt);
    }
    public Texture2DMS(Coord dim, int s, VectorFormat ifmt) {
	this(dim, s, false, ifmt);
    }

    public Coord sz() {
	return(new Coord(w, h));
    }

    public Image<Texture2DMS> image() {
	return(new Image<>(this, w, h, 1, 0));
    }

    public Collection<Image<Texture2DMS>> images() {
	return(new AbstractCollection<Image<Texture2DMS>>() {
		public int size() {
		    return(1);
		}

		public Iterator<Image<Texture2DMS>> iterator() {
		    return(new Iterator<Image<Texture2DMS>>() {
			    boolean f = true;
			    public boolean hasNext() {return(f);}
			    public Image<Texture2DMS> next() {return(image());}
			});
		}
	    });
    }

    public static class Sampler2DMS extends Sampler<Texture2DMS> {
	public Sampler2DMS(Texture2DMS tex) {
	    super(tex);
	}
    }

    public String toString() {
	return(String.format("#<tex2d-ms %sx%d %dx%d %d %s samples>", ifmt.cf, ifmt.nc, w, h, s, fixed ? "fixed" : "non-fixed"));
    }
}
