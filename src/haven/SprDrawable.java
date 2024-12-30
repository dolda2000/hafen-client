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

import java.util.*;
import haven.render.*;

public class SprDrawable extends Drawable implements Sprite.Owner {
    public final Sprite spr;

    public SprDrawable(Gob gob, Sprite.Mill<?> mk) {
	super(gob);
	this.spr = mk.create(this);
    }

    public SprDrawable(Gob gob, Sprite spr) {
	super(gob);
	this.spr = spr;
    }

    public void added(RenderTree.Slot slot) {
	slot.add(spr);
	super.added(slot);
    }

    public void dispose() {
	if(spr != null)
	    spr.dispose();
    }

    public void ctick(double dt) {
	spr.tick(dt);
    }

    public void gtick(Render g) {
	spr.gtick(g);
    }

    /* This is only deprecated becuase Sprite.Owner.getres is. Its
     * override from Drawable is not considered deprecated. */
    @Deprecated
    public Resource getres() {
	return(null);
    }

    private static final ClassResolver<SprDrawable> ctxr = new ClassResolver<SprDrawable>()
	.add(SprDrawable.class, d -> d);
    public <T> T context(Class<T> cl) {return(OwnerContext.orparent(cl, ctxr.context(cl, this, false), gob));}
    public Random mkrandoom() {return(gob.mkrandoom());}
}
