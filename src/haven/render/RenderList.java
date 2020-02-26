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
import java.util.*;

public interface RenderList<R> {
    public interface Slot<R> {
	public GroupPipe state();
	public R obj();

	@SuppressWarnings("unchecked")
	public default <T> Slot<T> cast(Class<T> cl) {
	    Object obj = obj();
	    if((obj != null) && !cl.isInstance(obj()))
		throw(new ClassCastException(obj.getClass().toString() + " => " + cl.toString()));
	    return((Slot<T>)this);
	}
    }

    public interface Adapter {
	public Locked lock();
	public Iterable<? extends Slot<?>> slots();
	public <R> void add(RenderList<R> list, Class<? extends R> type);
	public void remove(RenderList<?> list);

	public default String stats() {return("N/A");}
    }

    public void add(Slot<? extends R> slot);
    public void remove(Slot<? extends R> slot);
    public void update(Slot<? extends R> slot);
    public void update(Pipe group, int[] statemask);

    public default void syncadd(Adapter tree, Class<? extends R> type) {
	Collection<Slot<?>> initial;
	try(Locked lk = tree.lock()) {
	    for(Slot<?> slot : tree.slots()) {
		if(type.isInstance(slot.obj())) {
		    while(true) {
			try {
			    this.add(slot.cast(type));
			    break;
			} catch(Loading l) {
			    try {
				l.waitfor();
			    } catch(InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			    }
			}
		    }
		}
	    }
	    tree.add(this, type);
	}
    }

    public default void asyncadd(Adapter tree, Class<? extends R> type) {
	syncadd(tree, type);
	/* XXX Does not preserve proper sequencing.
	Collection<Slot<?>> initial;
	try(Locked lk = tree.lock()) {
	    initial = new ArrayList<>();
	    tree.slots().forEach(initial::add);
	    tree.add(this, type);
	}
	new HackThread(() -> {
		try {
		    for(Slot<?> slot : initial) {
			if(type.isInstance(slot.obj())) {
			    while(true) {
				try {
				    this.add(slot.cast(type));
				    break;
				} catch(Loading l) {
				    -* XXX: Make nonblocking *-
				    l.waitfor();
				}
			    }
			}
		    }
		} catch(InterruptedException e) {
		}
	}, "Initial adder").start();
	*/
    }
}
