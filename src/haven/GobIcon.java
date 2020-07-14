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
import java.awt.Color;
import java.awt.image.*;

public class GobIcon extends GAttrib {
    private static final int size = UI.scale(20);
    private static final Coord sz = new Coord(size, size);
    public static final PUtils.Convolution filter = new PUtils.Hanning(1);
    private static final Map<Indir<Resource>, Tex> cache = new WeakHashMap<Indir<Resource>, Tex>();
    public final Indir<Resource> res;
    private Tex tex;

    public GobIcon(Gob g, Indir<Resource> res) {
	super(g);
	this.res = res;
    }

    public Tex tex() {
	if(this.tex == null) {
	    synchronized(cache) {
		if(!cache.containsKey(res)) {
		    Resource.Image img = res.get().layer(Resource.imgc);
		    Tex tex = img.tex();
		    if ((tex.sz().x <= size) && (tex.sz().y <= size)) {
			cache.put(res, tex);
		    } else {
			BufferedImage buf = img.img;
			buf = PUtils.rasterimg(PUtils.blurmask2(buf.getRaster(), 1, 1, Color.BLACK));
			buf = PUtils.convolvedown(buf, sz, filter);
			cache.put(res, new TexI(buf));
		    }
		}
		this.tex = cache.get(res);
	    }
	}
	return(this.tex);
    }

    public static Coord sz(Coord sz) {
	return sz.mul((double) size / Math.max(sz.x, sz.y));
    }
}
