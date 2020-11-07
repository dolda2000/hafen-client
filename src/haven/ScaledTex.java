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

public class ScaledTex<T extends Tex> implements Tex {
    private final T impl;
    private final Coord sz;

    public ScaledTex(T impl, Coord sz) {
        this.impl = impl;
	this.sz = sz;
    }

    public T impl() {
	return(impl);
    }

    public Coord sz() {
	return(sz);
    }

    public void render(GOut g, float[] gc, float[] tc) {
	impl.render(g, gc, tc);
    }

    public void render(GOut g, Coord dul, Coord dbr, Coord tul, Coord tbr) {
	impl.render(g, dul, dbr, tul, tbr);
    }

    public void render(GOut g, Coord c) {
	impl.render(g, Coord.z, sz(), c, c.add(sz()));
    }

    public void crender(GOut g, Coord c, Coord dsz, Coord cul, Coord cbr) {
	impl.crender(g, c, dsz, cul, cbr);
    }

    public void crender(GOut g, Coord c, Coord ul, Coord br) {
	impl.crender(g, c, sz(), ul, br);;
    }

    public void dispose() {
	impl.dispose();
    }
}
