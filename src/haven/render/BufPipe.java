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
import haven.render.State.Slot;
import static haven.Utils.eq;

public class BufPipe implements Pipe {
    private State[] states;

    /* Implementation-side only */
    public BufPipe(State[] states) {
	this.states = states;
    }

    public BufPipe() {
	this(new State[State.Slot.slots.idlist.length]);
    }

    @SuppressWarnings("unchecked")
    public <T extends State> T get(Slot<T> slot) {
	if(states.length <= slot.id)
	    return(null);
	return((T)states[slot.id]);
    }

    public <T extends State> void put(Slot<? super T> slot, T state) {
	if(states.length <= slot.id)
	    states = Arrays.copyOf(states, slot.id + 1);
	states[slot.id] = state;
    }

    public State[] states() {
	return(states);
    }

    public BufPipe copy() {
	return(new BufPipe(Arrays.copyOf(states, states.length)));
    }

    public void copy(Pipe from) {
	State[] states = from.states();
	this.states = Arrays.copyOf(states, Math.max(states.length, this.states.length));
    }

    public int hashCode() {
	return(Pipe.hashCode(states));
    }

    public boolean equals(Object o) {
	if(!(o instanceof Pipe))
	    return(false);
	return(Pipe.equals(states, (o instanceof BufPipe) ? ((BufPipe)o).states : ((Pipe)o).states()));
    }

    public String toString() {
	StringBuilder buf = new StringBuilder();
	buf.append('[');
	for(int i = 0; i < states.length; i++) {
	    if(i > 0)
		buf.append(", ");
	    if((i % 5) == 0) {
		buf.append(i);
		buf.append('=');
	    }
	    if(states[i] == null)
		buf.append("null");
	    else
		buf.append(states[i].toString());
	}
	buf.append(']');
	return(buf.toString());
    }
}
