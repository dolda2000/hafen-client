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

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

public class ScaledBufferedImage {
    public static class Raster {
	private final WritableRaster impl;
	private final Coord sz;

	public Raster(WritableRaster impl, Coord sz) {
	    this.impl = impl;
	    this.sz = sz;
	}

	public WritableRaster get() {
	    return impl;
	}

	public int getNumBands() {
	    return impl.getNumBands();
	}

	public int getSample(int x, int y, int b) {
	    x = Math.min((int) Math.round((double) x * impl.getWidth() / sz.x), impl.getWidth() - 1);
	    y = Math.min((int) Math.round((double) y * impl.getHeight() / sz.y), impl.getHeight() - 1);
	    return impl.getSample(x, y, b);
	}
    }

    private final BufferedImage impl;
    private final Coord sz;

    public ScaledBufferedImage(BufferedImage impl, Coord sz) {
	this.impl = impl;
	this.sz = sz;
    }

    public BufferedImage get() {
	return impl;
    }

    public int getWidth() {
	return sz.x;
    }

    public int getHeight() {
	return sz.y;
    }

    public Raster getRaster() {
        return new Raster(impl.getRaster(), sz);
    }
}
