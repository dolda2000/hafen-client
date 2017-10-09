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

package haven.render.gl;

import javax.media.opengl.GL;
import haven.Area;

public class Scissor extends GLState {
    public final Area area;

    public Scissor(Area area) {
	this.area = area;
    }

    private void apply0(BGL gl) {
	gl.glViewport(area.ul.x, area.ul.y, area.br.x - area.ul.x, area.br.y - area.ul.y);
    }

    public void apply(BGL gl) {
	apply0(gl);
	gl.glEnable(GL.GL_SCISSOR_TEST);
    }

    public void unapply(BGL gl) {
	gl.glDisable(GL.GL_SCISSOR_TEST);
    }

    public void applyto(BGL gl, GLState to) {
	((Scissor)to).apply0(gl);
    }

    public static int slot = slotidx(Scissor.class);
    public int slotidx() {return(slot);}
}
