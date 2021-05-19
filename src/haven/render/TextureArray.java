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

public abstract class TextureArray extends Texture {
    public final int n;

    public TextureArray(int n, DataBuffer.Usage usage, VectorFormat ifmt, VectorFormat efmt, DataBuffer.Filler<? super Image> init) {
	super(usage, ifmt, efmt, init);
	if(n < 0)
	    throw(new IllegalArgumentException(String.format("Array texture layer count must be non-negative, not %d", n)));
	this.n = n;
    }

    public TextureArray(int n, DataBuffer.Usage usage, VectorFormat ifmt, DataBuffer.Filler<? super Image> init) {
	this(n, usage, ifmt, ifmt, init);
    }

    public static class ArrayImage<T extends TextureArray> extends Image<T> {
	public final int layer;

	public ArrayImage(T tex, int w, int h, int d, int layer, int level) {
	    super(tex, w, h, d, level);
	    this.layer = layer;
	}

	public boolean equals(ArrayImage<?> that) {
	    return(equals((Image)that) && (this.layer == that.layer));
	}

	public boolean equals(Object that) {
	    return((that instanceof ArrayImage) && equals((ArrayImage<?>)that));
	}

	public String toString() {
	    return(String.format("#<tex.arrayimage %s %d %dx%dx%d@%d>", tex, level, w, h, d, layer));
	}
    }
}
