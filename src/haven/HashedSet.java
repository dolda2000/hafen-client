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

public class HashedSet<E> extends AbstractSet<E> {
    private static final double loadfac = 0.5;
    public final Hash<? super E> hash;
    private Object[] tab;
    private int sz;

    public HashedSet(Hash<? super E> hash) {
	this.hash = hash;
	clear();
    }

    public HashedSet(Hash<? super E> hash, Collection<? extends E> from) {
	this(hash);
	addAll(from);
    }

    public void clear() {
	this.tab = new Object[32];
	this.sz = 0;
    }

    public int size() {
	return(sz);
    }

    public boolean isEmpty() {
	return(sz == 0);
    }

    private int nextidx(Object[] tab, int idx) {
	return((idx + 1) & (tab.length - 1));
    }

    private int hashidx(Object[] tab, E el) {
	return(hash.hash(el) & (tab.length - 1));
    }

    @SuppressWarnings("unchecked")
    private int findidx(Object[] tab, Object e) {
	E el = (E)e;
	int idx = hashidx(tab, el);
	while(true) {
	    E cur = (E)tab[idx];
	    if(cur == null)
		return(-1);
	    if(hash.equal(el, cur))
		return(idx);
	    idx = nextidx(tab, idx);
	}
    }

    @SuppressWarnings("unchecked")
    private void remove(Object[] tab, int idx) {
	tab[idx] = null;
	for(int nx = nextidx(tab, idx); tab[nx] != null; nx = nextidx(tab, nx)) {
	    int oh = (hash.hash((E)tab[nx])) & (tab.length - 1);
	    if((idx < nx) ? ((oh <= idx) || (nx < oh)) : ((oh <= idx) && (nx < oh))) {
		tab[idx] = tab[nx];
		tab[nx] = null;
		idx = nx;
	    }
	}
	sz--;
    }

    public boolean remove(Object el) {
	if(el == null)
	    return(false);
	Object[] tab = this.tab;
	int idx = findidx(tab, el);
	if(idx < 0)
	    return(false);
	remove(tab, idx);
	ckshrink();
	return(true);
    }

    private void ckshrink() {
	int nsz = tab.length;
	while((nsz > 32) && (sz < (loadfac * 0.25)))
	    nsz >>= 1;
	if(nsz < tab.length)
	    resize(nsz);
    }

    @SuppressWarnings("unchecked")
    private void resize(int nsz) {
	Object[] ctab = this.tab;
	Object[] ntab = new Object[nsz];
	for(int i = 0; i < ctab.length; i++) {
	    E cur = (E)ctab[i];
	    if(cur != null) {
		int idx = hash.hash(cur) & (ntab.length - 1);
		for(; ntab[idx] != null; idx = nextidx(ntab, idx));
		ntab[idx] = cur;
	    }
	}
	this.tab = ntab;
    }

    @SuppressWarnings("unchecked")
    public boolean add(E el) {
	if(el == null)
	    throw(new NullPointerException());
	Object[] tab = this.tab;
	int idx = hashidx(tab, el);
	while(true) {
	    E cur = (E)tab[idx];
	    if(cur == null)
		break;
	    if(hash.equal(el, cur))
		return(false);
	    idx = nextidx(tab, idx);
	}
	tab[idx] = el;
	if(++sz >= (tab.length * loadfac))
	    resize(tab.length * 2);
	return(true);
    }

    public Iterator<E> iterator() {
	return(new Iterator<E>() {
		int i = 0, lasti;
		E next = null, last = null;

		@SuppressWarnings("unchecked")
		public boolean hasNext() {
		    if(next != null)
			return(true);
		    while(true) {
			if(i >= tab.length)
			    return(false);
			if(((next = (E)tab[i]) != null))
			    return(true);
			i++;
		    }
		}

		public E next() {
		    if(!hasNext())
			throw(new NoSuchElementException());
		    last = next;
		    next = null;
		    lasti = i++;
		    return(last);
		}

		public void remove() {
		    if(last == null)
			throw(new IllegalStateException());
		    if(tab[lasti] != last)
			throw(new ConcurrentModificationException());
		    HashedSet.this.remove(tab, lasti);
		    last = null;
		}
	    });
    }

    public boolean contains(Object el) {
	return(findidx(tab, el) >= 0);
    }

    @SuppressWarnings("unchecked")
    public E find(E el) {
	Object[] tab = this.tab;
	int idx = findidx(tab, el);
	if(idx < 0)
	    return(null);
	return((E)tab[idx]);
    }

    public E intern(E el) {
	E ret = find(el);
	if(ret == null)
	    add(ret = el);
	return(ret);
    }
}
