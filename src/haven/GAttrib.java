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
import java.util.function.*;
import haven.render.*;

public abstract class GAttrib {
    public final Gob gob;
	
    public GAttrib(Gob gob) {
	this.gob = gob;
    }
	
    public void ctick(double dt) {
    }
    
    public void dispose() {
    }

    /* Private to Gob.java */
    Collection<RenderTree.Slot> slots;
    public void added(RenderTree.Slot slot) {
	if(slots == null)
	    slots = new ArrayList<>(1);
	slots.add(slot);
    }
    public void removed(RenderTree.Slot slot) {
	if(slots != null)
	    slots.remove(slot);
    }

    public static class ParserMaker implements Resource.PublishedCode.Instancer<Parser> {
	public Parser make(Class<?> cl, Resource ires, Object... argv) {
	    if(Parser.class.isAssignableFrom(cl))
		return(Resource.PublishedCode.Instancer.stdmake(Parser.class, cl.asSubclass(Parser.class), ires, argv));
	    try {
		Function<Object[], Void> parse = Utils.smthfun(cl, "parse", Void.TYPE, Gob.class, Message.class);
		return(new Parser() {
			public void apply(Gob gob, Message sdt) {
			    parse.apply(new Object[]{gob, sdt});
			}
		    });
	    } catch(NoSuchMethodException e) {}
	    throw(new RuntimeException(String.format("Could not find any suitable objdelta parser in %s", cl)));
	}
    }

    @Resource.PublishedCode(name = "objdelta", instancer = ParserMaker.class)
    public static interface Parser {
	public void apply(Gob gob, Message sdt);
    }
}
