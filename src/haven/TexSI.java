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

public class TexSI implements Tex {
    public final Tex parent;
    public final Coord ul, br;

    public TexSI(Tex parent, Coord ul, Coord br) {
	this.parent = parent;
	this.ul = ul;
	this.br = br;
    }

    public Coord sz() {
	return(br.sub(ul));
    }

    public void render(GOut g, float[] gc, float[] tc) {
	float[] ptc = {
	    tc[0] + ul.x, tc[1] + ul.y,
	    tc[2] + ul.x, tc[3] + ul.y,
	    tc[4] + ul.x, tc[5] + ul.y,
	    tc[6] + ul.x, tc[7] + ul.y,
	};
	parent.render(g, gc, ptc);
    }
}
