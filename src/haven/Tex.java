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

public abstract class Tex {
    protected Coord dim;
    
    public Tex(Coord sz) {
	dim = sz;
    }
	
    public Coord sz() {
	return(dim);
    }

    public static int nextp2(int in) {
	int ret;
	
	for(ret = 1; ret < in; ret <<= 1);
	return(ret);
    }

    public abstract void render(GOut g, Coord c, Coord ul, Coord br, Coord sz);
    public abstract float tcx(int x);
    public abstract float tcy(int y);
    public abstract GLState draw();
    public abstract GLState clip();

    public void render(GOut g, Coord c) {
	render(g, c, Coord.z, dim, dim);
    }
    
    public void crender(GOut g, Coord c, Coord ul, Coord sz, Coord tsz) {
	if((tsz.x == 0) || (tsz.y == 0))
	    return;
	if((c.x >= ul.x + sz.x) || (c.y >= ul.y + sz.y) ||
	   (c.x + tsz.x <= ul.x) || (c.y + tsz.y <= ul.y))
	    return;
	Coord t = new Coord(c);
	Coord uld = new Coord(0, 0);
	Coord brd = new Coord(dim);
	Coord szd = new Coord(tsz);
	if(c.x < ul.x) {
	    int pd = ul.x - c.x;
	    t.x = ul.x;
	    uld.x = (pd * dim.x) / tsz.x;
	    szd.x -= pd;
	}
	if(c.y < ul.y) {
	    int pd = ul.y - c.y;
	    t.y = ul.y;
	    uld.y = (pd * dim.y) / tsz.y;
	    szd.y -= pd;
	}
	if(c.x + tsz.x > ul.x + sz.x) {
	    int pd = (c.x + tsz.x) - (ul.x + sz.x);
	    szd.x -= pd;
	    brd.x -= (pd * dim.x) / tsz.x;
	}
	if(c.y + tsz.y > ul.y + sz.y) {
	    int pd = (c.y + tsz.y) - (ul.y + sz.y);
	    szd.y -= pd;
	    brd.y -= (pd * dim.y) / tsz.y;
	}
	render(g, t, uld, brd, szd);
    }
	
    public void crender(GOut g, Coord c, Coord ul, Coord sz) {
	crender(g, c, ul, sz, dim);
    }
		
    public void dispose() {}
}
