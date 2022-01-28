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

public class LockDebugList<E> extends AbstractList<E> {
    public final List<E> back;
    private Object monitor = null;

    public LockDebugList(List<E> back) {
	this.back = back;
    }

    public void check() {
	if((monitor != null) && !Thread.holdsLock(monitor))
	    throw(new RuntimeException("lock check failed"));
    }

    public LockDebugList<E> monitor(Object monitor) {
	this.monitor = monitor;
	return(this);
    }

    public E get(int i) {
	check();
	return(back.get(i));
    }

    public int size() {
	check();
	return(back.size());
    }

    public Iterator<E> iterator() {
	check();
	return(back.iterator());
    }

    public ListIterator<E> listIterator() {
	check();
	return(back.listIterator());
    }

    public ListIterator<E> listIterator(int i) {
	check();
	return(back.listIterator(i));
    }

    public boolean add(E el) {
	check();
	return(back.add(el));
    }

    public void add(int i, E el) {
	check();
	back.add(i, el);
    }

    public E set(int i, E el) {
	check();
	return(back.set(i, el));
    }

    public void clear() {
	check();
	back.clear();
    }

    public E remove(int i) {
	check();
	return(back.remove(i));
    }

    public boolean remove(Object el) {
	check();
	return(back.remove(el));
    }
}
