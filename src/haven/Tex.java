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

public interface Tex extends Disposable {
    public Coord sz();

    public void render(GOut g, float[] gc, float[] tc);

    /* Render texture coordinates [tul, tbr) at [dul, dbr), scaling if necessary. */
    public default void render(GOut g, Coord dul, Coord dbr, Coord tul, Coord tbr) {
	float[] gc = {
	    dul.x, dul.y, dbr.x, dul.y,
	    dbr.x, dbr.y, dul.x, dbr.y,
	};
	float[] tc = {
	    tul.x, tul.y, tbr.x, tul.y,
	    tbr.x, tbr.y, tul.x, tbr.y,
	};
	render(g, gc, tc);
    }

    public default void render(GOut g, Coord c) {
	render(g, Coord.z, sz(), c, c.add(sz()));
    }

    /* Render texture at c, scaled to dsz, clipping everything outside [cul, cbr). */
    public default void crender(GOut g, Coord c, Coord dsz, Coord cul, Coord cbr) {
	if((dsz.x < 1) || (dsz.y < 1))
	    return;
	if((c.x >= cbr.x) || (c.y >= cbr.y) ||
	   (c.x + dsz.x <= cul.x) || (c.y + dsz.y <= cul.y))
	    return;
	Coord dul = new Coord(c);
	Coord dbr = new Coord(c.add(dsz));
	Coord tsz = sz();
	Coord tul = new Coord(0, 0);
	Coord tbr = new Coord(tsz);
	if(dul.x < cul.x) {
	    int pd = cul.x - dul.x;
	    dul.x = cul.x;
	    tul.x = (pd * tsz.x) / dsz.x;
	}
	if(dul.y < cul.y) {
	    int pd = cul.y - dul.y;
	    dul.y = cul.y;
	    tul.y = (pd * tsz.y) / dsz.y;
	}
	if(dbr.x > cbr.x) {
	    int pd = dbr.x - cbr.x;
	    dbr.x = cbr.x;
	    tbr.x -= (pd * tsz.x) / dsz.x;
	}
	if(dbr.y > cbr.y) {
	    int pd = dbr.y - cbr.y;
	    dbr.y = cbr.y;
	    tbr.y -= (pd * tsz.y) / dsz.y;
	}
	render(g, dul, dbr, tul, tbr);
    }

    /* Render texture at c at normal size, clipping everything outside [ul, br). */
    public default void crender(GOut g, Coord c, Coord ul, Coord br) {
	crender(g, c, sz(), ul, br);
    }

    public static int nextp2(int in) {
	int h = Integer.highestOneBit(in);
	return((h == in)?h:(h * 2));
    }

    public default void dispose() {};
}
