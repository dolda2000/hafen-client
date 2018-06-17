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

import java.util.*;
import java.lang.reflect.*;

public abstract class GLState {
    @SuppressWarnings("unchecked")
    public static final Class<? extends GLState>[] slots = (Class<? extends GLState>[])new Class[] {
	VaoState.class,
	VboState.class,
	FboState.class,
    };

    public static int slotidx(Class<? extends GLState> cl) {
	for(int i = 0; i < slots.length; i++) {
	    if(slots[i] == cl)
		return(i);
	}
	throw(new RuntimeException("No slot for " + cl));
    }

    public abstract void apply(BGL gl);
    public abstract void unapply(BGL gl);
    public abstract int slotidx();

    public void applyto(BGL gl, GLState to) {
	unapply(gl);
	to.apply(gl);
    }
}
