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

public interface DefaultCollection<E> extends Collection<E> {
    public default int size() {
	int n = 0;
	for(E el : this)
	    n++;
	return(n);
    }

    public default boolean isEmpty() {
	return(!iterator().hasNext());
    }

    public default void clear() {
	for(Iterator<E> i = iterator(); i.hasNext();) {
	    i.next();
	    i.remove();
	}
    }

    public default boolean remove(Object el) {
	for(Iterator<E> i = iterator(); i.hasNext();) {
	    if(Objects.equals(i, el)) {
		i.remove();
		return(true);
	    }
	}
	return(false);
    }

    public default boolean add(E el) {
	throw(new UnsupportedOperationException());
    }

    public default boolean contains(Object o) {
	for(E el : this) {
	    if(Objects.equals(o, el))
		return(true);
	}
	return(false);
    }

    public default boolean retainAll(Collection<?> c) {
	boolean ret = false;
	for(Iterator<E> i = iterator(); i.hasNext();) {
	    if(!c.contains(i.next())) {
		i.remove();
		ret = true;
	    }
	}
	return(ret);
    }

    public default boolean removeAll(Collection<?> c) {
	boolean ret = false;
	for(Iterator<E> i = iterator(); i.hasNext();) {
	    if(c.contains(i.next())) {
		i.remove();
		ret = true;
	    }
	}
	return(ret);
    }

    public default boolean addAll(Collection<? extends E> c) {
	boolean ret = false;
	for(E el : c) {
	    if(add(el))
		ret = true;
	}
	return(ret);
    }

    public default boolean containsAll(Collection<?> c) {
	for(Object el : c) {
	    if(!contains(el))
		return(false);
	}
	return(true);
    }

    @SuppressWarnings("unchecked")
    public default <T> T[] toArray(T[] buf) {
	int sz = size();
	T[] ret;
	if(sz < buf.length)
	    ret = buf;
	else
	    ret = (T[])java.lang.reflect.Array.newInstance(buf.getClass().getComponentType(), sz);
	Iterator<E> i = iterator();
	for(int n = 0; n < ret.length; n++) {
	    if(!i.hasNext()) {
		if(ret == buf)
		    ret[n] = null;
		else if(n < ret.length)
		    return(Arrays.copyOf(ret, n));
		return(ret);
	    }
	    ret[n] = (T)i.next();
	}
	return(ret);
    }

    public default Object[] toArray() {
	return(toArray(new Object[0]));
    }
}
