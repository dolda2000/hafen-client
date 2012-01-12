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

public class LocationCam extends Camera {
    private final static Matrix4f base = makerot(new Matrix4f(), new Coord3f(0.0f, 0.0f, 1.0f), (float)(Math.PI / 2))
	.mul1(makerot(new Matrix4f(), new Coord3f(0.0f, 1.0f, 0.0f), (float)(Math.PI / 2)));
    public final Location loc;
    private Matrix4f ll;
    
    /* Oh, Java. <3 */
    private LocationCam(Location loc, Matrix4f lm) {
	super(base.mul(rxinvert(lm)));
	this.ll = lm;
	this.loc = loc;
    }

    public LocationCam(Location loc) {
	this(loc, loc.fin(Matrix4f.id));
    }
    
    public Matrix4f fin(Matrix4f p) {
	Matrix4f lm = loc.fin(Matrix4f.id);
	if(lm != ll)
	    update(base.mul(rxinvert(ll = lm)));
	return(super.fin(p));
    }
}
