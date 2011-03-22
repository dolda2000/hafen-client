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

import javax.media.opengl.*;

public class Location extends Transform {
    private Location p = null;
    private Matrix4f bk;
    
    public Location(Matrix4f xf) {
	super(xf);
    }
    
    public Matrix4f fin(Matrix4f p) {
	if(this.p == null)
	    return(super.fin(p));
	else
	    return(super.fin(this.p.fin(p)));
    }

    public void apply(GOut g) {
	bk = g.st.wxf;
	g.st.wxf = fin(g.st.wxf);
    }
    
    public void unapply(GOut g) {
	g.st.wxf = bk;
    }

    public void prep(Buffer b) {
	p = b.get(PView.loc);
	b.put(PView.loc, this);
    }
    
    public static Location xlate(Coord3f c) {
	return(new Location(makexlate(new Matrix4f(), c)));
    }
    
    public static Location rot(Coord3f axis, float angle) {
	return(new Location(makerot(new Matrix4f(), axis.norm(), angle)));
    }
    
    public String toString() {
	String ret = super.toString();
	if(p != null)
	    ret += " -> " + p;
	return(ret);
    }
}
