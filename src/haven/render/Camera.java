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

public class Camera extends Transform {
    public static final Coord3f defdir = new Coord3f(0, 0, -1);

    public Camera(Matrix4f xf) {
	super(xf);
    }

    public void apply(Pipe p) {
	p.put(Homo3D.cam, this);
    }

    public static Matrix4f makedir(Matrix4f d, Coord3f base, Coord3f dir) {
	Coord3f diff = defdir.cmul(dir);
	float a = (float)Math.asin(diff.abs());
	return(makerot(d, diff, -a)
	       .mul1(makexlate(new Matrix4f(), base.inv())));
    }

    public static Camera dir(Coord3f base, Coord3f dir) {
	return(new Camera(makedir(new Matrix4f(), base, dir)));
    }

    public static Matrix4f makepointed(Matrix4f d, Coord3f base, float dist, float e, float a) {
	return(makexlate(d, new Coord3f(0, 0, -dist))
	       .mul1(makerot(new Matrix4f(), new Coord3f(-1, 0, 0), ((float)Math.PI / 2) - e))
	       .mul1(makerot(new Matrix4f(), new Coord3f(0, 0, -1), ((float)Math.PI / 2) + a))
	       .mul1(makexlate(new Matrix4f(), base.inv())));
    }

    public static Camera pointed(Coord3f base, float dist, float e, float a) {
	return(new Camera(makepointed(new Matrix4f(), base, dist, e, a)));
    }

    private static final Matrix4f pbase = makerot(new Matrix4f(), new Coord3f(0.0f, 0.0f, 1.0f), (float)(Math.PI / 2))
	.mul1(makerot(new Matrix4f(), new Coord3f(0.0f, 1.0f, 0.0f), (float)(Math.PI / 2)));
    public static Matrix4f makeplaced(Location.Chain loc) {
	return(pbase.mul(rxinvert(loc.fin(Matrix4f.id))));
    }

    public static Camera placed(Location.Chain loc) {
	return(new Camera(makeplaced(loc)));
    }
}
