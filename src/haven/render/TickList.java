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

import java.util.*;

public class TickList implements RenderList<TickList.TickNode> {
    private final Map<Ticking, Entry> cur = new HashMap<>();

    private static class Entry {
	final Ticking tick;
	final Object mon;
	int rc = 0;
	Object users = null;

	public Entry(Ticking tick, Object mon) {
	    this.tick = tick;
	    this.mon = mon;
	}

	public void get(TickNode user) {
	    if((rc == 0) && (users == null)) {
		rc = 1;
		users = user;
	    } else if((rc == 1) && (users instanceof TickNode)) {
		rc = 2;
		users = new TickNode[] {(TickNode)users, user};
	    } else if(users instanceof TickNode[]) {
		TickNode[] ul = (TickNode[])users;
		if(ul.length == rc)
		    users = ul = Arrays.copyOf(ul, ul.length * 2);
		ul[rc++] = user;
	    } else {
		throw(new AssertionError());
	    }
	}

	public boolean put(TickNode user) {
	    if((rc == 1) && (users == user)) {
		users = null;
		rc = 0;
		return(true);
	    } else if((rc > 1) && (users instanceof TickNode[])) {
		TickNode[] ul = (TickNode[])users;
		for(int i = 0; i < rc; i++) {
		    if(ul[i] == user) {
			rc--;
			ul[i] = ul[rc];
			ul[rc] = null;
			if(rc == 1)
			    users = ul[0];
			return(false);
		    }
		}
		throw(new AssertionError());
	    } else {
		throw(new AssertionError());
	    }
	}
    }

    public static interface Ticking {
	public default void autotick(double dt) {}
	public default void autogtick(Render g) {}
    }

    public static interface TickNode {
	public Ticking ticker();
    }

    public void add(Slot<? extends TickNode> slot) {
	Ticking tick = slot.obj().ticker();
	synchronized(cur) {
	    Entry ent = cur.get(tick);
	    Monitor ms = slot.state().get(Monitor.slot);
	    Object mon = (ms == null) ? null : ms.mon;
	    if(ent == null) {
		cur.put(tick, ent = new Entry(tick, mon));
	    } else {
		if((mon != null) && (ent.mon != mon))
		    throw(new RuntimeException("cannot specify different monitors for one tick"));
	    }
	    ent.get(slot.obj());
	}
    }

    public void remove(Slot<? extends TickNode> slot) {
	Ticking tick = slot.obj().ticker();
	synchronized(cur) {
	    Entry ent = cur.get(tick);
	    if(ent.put(slot.obj()))
		cur.remove(tick);
	}
    }

    public void update(Slot<? extends TickNode> slot) {}
    public void update(Pipe group, int[] statemask) {}

    public void tick(double dt) {
	List<Entry> copy;
	synchronized(cur) {
	    copy = new ArrayList<>(cur.values());
	}
	for(Entry ent : copy) {
	    /* XXX: Parallelize */
	    if(ent.mon == null) {
		ent.tick.autotick(dt);
	    } else {
		synchronized(ent.mon) {
		    ent.tick.autotick(dt);
		}
	    }
	}
    }

    public void gtick(Render g) {
	List<Entry> copy;
	synchronized(cur) {
	    copy = new ArrayList<>(cur.values());
	}
	for(Entry ent : copy) {
	    /* XXX: Parallelize */
	    if(ent.mon == null) {
		ent.tick.autogtick(g);
	    } else {
		synchronized(ent.mon) {
		    ent.tick.autogtick(g);
		}
	    }
	}
    }

    public static class Monitor extends State {
	public static final Slot<Monitor> slot = new Slot<>(Slot.Type.SYS, Monitor.class);
	public final Object mon;

	public Monitor(Object mon) {
	    this.mon = mon;
	}

	public haven.render.sl.ShaderMacro shader() {return(null);}
	public void apply(Pipe p) {p.put(slot, this);}
    }
}
