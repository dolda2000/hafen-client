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

import haven.render.*;
import haven.render.Texture2D.Sampler2D;

public class TexRaw implements Tex {
    public final Sampler2D back;
    public final boolean invert;
    private final ColorTex st;

    public TexRaw(Sampler2D back, boolean invert) {
	this.back = back;
	this.invert = invert;
	this.st = new ColorTex(back);
    }

    public TexRaw(Sampler2D back) {
	this(back, false);
    }

    public Coord sz() {return(back.tex.sz());}

    public void render(GOut g, Coord dul, Coord dbr, Coord tul, Coord tbr) {
	Coord tdim = sz();
	float tl = (float)tul.x / (float)tdim.x;
	float tu = (float)tul.y / (float)tdim.y;
	float tr = (float)tbr.x / (float)tdim.x;
	float tb = (float)tbr.y / (float)tdim.y;
	float[] data = {
	    dbr.x, dul.y, tr, invert ? tb : tu,
	    dbr.x, dbr.y, tr, invert ? tu : tb,
	    dul.x, dul.y, tl, invert ? tb : tu,
	    dul.x, dbr.y, tl, invert ? tu : tb,
	};
	g.usestate(st);
	g.drawt(Model.Mode.TRIANGLE_STRIP, data);
	g.usestate(ColorTex.slot);
    }

    public void dispose() {
    }
}
