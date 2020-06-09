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

public class FastArrayList<E> extends AbstractList<E> {
    private Object[] bk = null;
    private int n = 0;

    public FastArrayList() {
    }

    public FastArrayList(int sz) {
	bk = new Object[sz];
    }

    private Object[] ensure(int sz) {
	if((bk == null) || (bk.length < sz)) {
	    int ns = (bk == null) ? 8 : bk.length;
	    while(ns < sz)
		ns <<= 1;
	    bk = Arrays.copyOf(bk, ns);
	}
	return(bk);
    }

    public int size() {
	return(n);
    }

    @SuppressWarnings("unchecked")
    public E get(int i) {
	if(i >= n)
	    throw(new IndexOutOfBoundsException(String.format("%d >= %d", i, n)));
	return((E)bk[i]);
    }

    public boolean add(E e) {
	ensure(n + 1)[n++] = e;
	return(true);
    }

    @SuppressWarnings("unchecked")
    public E set(int i, E el) {
	if(i >= n)
	    throw(new IndexOutOfBoundsException(String.format("%d >= %d", i, n)));
	E ret = (E)bk[i];
	bk[i] = el;
	return(ret);
    }

    public void add(int i, E el) {
	if(i > n)
	    throw(new IndexOutOfBoundsException(String.format("%d >= %d", i, n)));
	ensure(n + 1)[n++] = bk[i];
	bk[i] = el;
    }

    @SuppressWarnings("unchecked")
    public E remove(int i) {
	if(i >= n)
	    throw(new IndexOutOfBoundsException(String.format("%d >= %d", i, n)));
	E ret = (E)bk[i];
	n--;
	bk[i] = bk[n];
	bk[n] = null;
	return(ret);
    }

    public void clear() {
	bk = null;
	n = 0;
    }

    public Iterator<E> iterator() {
	return(listIterator());
    }

    public ListIterator<E> listIterator() {
	return(listIterator(0));
    }

    public ListIterator<E> listIterator(int start) {
	return(new ListIterator<E>() {
		private int cur = start, last = -1;

		public boolean hasPrevious() {return(cur > 0);}
		public boolean hasNext() {return(cur < n);}
		public int nextIndex() {return(cur);}
		public int previousIndex() {return(cur - 1);}

		@SuppressWarnings("unchecked")
		public E previous() {
		    if((cur <= 0) || (cur > n))
			throw(new NoSuchElementException());
		    return((E)bk[last = --cur]);
		}

		@SuppressWarnings("unchecked")
		public E next() {
		    if((cur < 0) || (cur >= n))
			throw(new NoSuchElementException());
		    return((E)bk[last = cur++]);
		}

		public void remove() {
		    if(last < 0)
			throw(new IllegalStateException());
		    FastArrayList.this.remove(last);
		    if(last < cur)
			cur--;
		    last = -1;
		}

		public void set(E e) {
		    if(last < 0)
			throw(new IllegalStateException());
		    FastArrayList.this.set(cur, e);
		}

		public void add(E e) {
		    FastArrayList.this.add(last = cur++, e);
		}
	    });
    }
}
