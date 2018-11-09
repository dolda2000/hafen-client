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
import haven.render.RenderTree.Node;
import haven.render.RenderTree.Slot;
import haven.render.Pipe.Op;

public class RUtils {
    public static Collection<Slot> multiadd(Collection<Slot> slots, Node node) {
	Collection<Slot> added = new ArrayList<>(slots.size());
	try {
	    for(Slot slot : slots)
		added.add(slot.add(node));
	} catch(RuntimeException e) {
	    for(Slot slot : added)
		slot.remove();
	    throw(e);
	}
	return(added);
    }

    public static void multirem(Collection<Slot> slots) {
	for(Slot slot : slots)
	    slot.remove();
    }

    public static void readd(Collection<Slot> slots, Consumer<Slot> add, Runnable revert) {
	Collection<Slot> ch = new ArrayList<>(slots.size());
	try {
	    for(Slot slot : slots) {
		ch.add(slot);
		slot.clear();
		add.accept(slot);
	    }
	} catch(RuntimeException e) {
	    revert.run();
	    try {
		for(Slot slot : ch) {
		    slot.clear();
		    add.accept(slot);
		}
	    } catch(RuntimeException e2) {
		Error err = new Error("Unexpected non-local exit", e2);
		err.addSuppressed(e);
		throw(err);
	    }
	    throw(e);
	}
    }

    public abstract static class StateNode<R extends RenderTree.Node> implements Node {
	public final R r;
	private final Collection<Slot> slots = new ArrayList<>(1);
	private Op cstate = null;

	public StateNode(R r) {
	    this.r = r;
	}

	protected abstract Op state();

	public void update() {
	    Op nstate = state();
	    if(nstate == null)
		throw(new NullPointerException("state"));
	    if(Utils.eq(cstate, nstate))
		return;
	    for(Slot slot : slots)
		slot.ostate(nstate);
	    this.cstate = nstate;
	}

	public void added(Slot slot) {
	    if(cstate == null) {
		if((cstate = state()) == null)
		    throw(new NullPointerException("state"));
	    }
	    slot.ostate(cstate);
	    slot.add(r);
	    slots.add(slot);
	}

	public void removed(Slot slot) {
	    slots.remove(slot);
	}

	public static <R extends RenderTree.Node> StateNode from(R r, Supplier<? extends Op> st) {
	    return(new StateNode<R>(r) {
		    protected Op state() {return(st.get());}
		});
	}
    }
}
