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

package haven.glsl;

import java.util.*;

public class PostProc implements Walker {
    public final Object id;
    public final Context ctx;

    public interface Processed {
	public void process(PostProc proc);
	public Object ppid();
    }

    public static class AutoID {
	public final String name;
	public final int order;

	public AutoID(String name, int order) {
	    this.name = name;
	    this.order = order;
	}
	public AutoID(int order) {
	    this("<nil>", order);
	}

	public PostProc proc(Context ctx) {
	    return(new PostProc(this, ctx));
	}
    }

    public PostProc(Object id, Context ctx) {
	this.id = id;
	this.ctx = ctx;
    }

    public PostProc(Object id) {
	this(id, null);
    }

    public PostProc() {
	this.id = this;
	this.ctx = null;
    }

    public void el(Element el) {
	if((el instanceof Processed) && (((Processed)el).ppid() == id))
	    ((Processed)el).process(this);
	el.walk(this);
    }

    public static void autoproc(Context ctx) {
	final Collection<AutoID> closed = new ArrayList<AutoID>();
	final int[] curo = {Integer.MIN_VALUE};
	while(true) {
	    final List<AutoID> open = new ArrayList<AutoID>();
	    ctx.walk(new Walker() {
		    public void el(Element el) {
			if(el instanceof Processed) {
			    Object key = ((Processed)el).ppid();
			    if(key instanceof AutoID) {
				AutoID id = (AutoID)key;
				if(!closed.contains(id) && !open.contains(id)) {
				    if(id.order < curo[0])
					throw(new RuntimeException("New postprocessor " + id.name + " with order " + id.order + " added when at order " + curo[0]));
				    open.add(id);
				}
			    }
			}
			el.walk(this);
		    }
		});
	    if(open.isEmpty())
		return;
	    Collections.sort(open, new Comparator<AutoID>() {
		    public int compare(AutoID a, AutoID b) {
			return(a.order - b.order);
		    }
		});
	    for(AutoID id : open) {
		ctx.walk(id.proc(ctx));
		curo[0] = id.order;
		closed.add(id);
	    }
	    open.clear();
	}
    }
}
