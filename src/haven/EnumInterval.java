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

public class EnumInterval<E extends Enum<E>> extends AbstractList<E> {
    private final Class<? extends E> cl;
    private final E lo, hi;

    @SuppressWarnings("unchecked")
    public EnumInterval(E lo, E hi) {
	/* XXX? I'm not sure why this unchecked cast is necessary. */
	this.cl = (Class<? extends E>)lo.getClass();
	this.lo = lo;
	this.hi = hi;
	if(hi.ordinal() < lo.ordinal())
	    throw(new IllegalArgumentException());
    }

    public int size() {
	return(hi.ordinal() - lo.ordinal() + 1);
    }

    public E get(int i) {
	if((i < 0) || (i >= size()))
	    throw(new NoSuchElementException(Integer.toString(i)));
	return(cl.getEnumConstants()[lo.ordinal() + i]);
    }

    public int indexOf(Object o) {
	if((o == null) || o.getClass() != cl)
	    return(-1);
	int i = ((Enum)o).ordinal();
	if((i < lo.ordinal()) || (i > hi.ordinal()))
	    return(-1);
	return(i - lo.ordinal());
    }

    public boolean contains(Object o) {
	return(indexOf(o) >= 0);
    }
}
