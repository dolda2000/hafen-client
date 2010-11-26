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

public abstract class Location extends Transform {
    private Location p = null;

    private void chain(GOut g) {
	if(p != null)
	    p.chain(g);
	xf(g);
    }

    public final void apply(GOut g) {
	GL gl = g.gl;
	g.st.matmode(GL.GL_MODELVIEW);
	gl.glPushMatrix();
	chain(g);
    }

    public void prep(Buffer b) {
	p = b.get(PView.loc);
	b.put(PView.loc, this);
    }
    
    public static Location xlate(final Coord3f c) {
	return(new Location() {
		public void xf(GOut g) {
		    GL gl = g.gl;
		    gl.glTranslatef(c.x, c.y, c.z);
		}
	    });
    }
    
    public static Location rot(final Coord3f axis, final float angle) {
	return(new Location() {
		public void xf(GOut g) {
		    GL gl = g.gl;
		    gl.glRotatef(angle * 180.0f / (float)Math.PI, axis.x, axis.y, axis.z);
		}
	    });
    }
}
