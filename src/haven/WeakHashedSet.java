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
import java.lang.ref.*;

public class WeakHashedSet<E> extends AbstractSet<E> {
    private final ReferenceQueue<E> cleanq = new ReferenceQueue<>();
    public final Hash<? super E> hash;
    private Ref<E>[] tab;
    private int sz;

    static class Ref<T> extends WeakReference<T> {
	final int hash;

	Ref(T ob, ReferenceQueue<T> q) {
	    super(ob, q);
	    this.hash = ob.hashCode();
	}
    }

    public WeakHashedSet(Hash<? super E> hash) {
	this.hash = hash;
	clear();
    }

    @SuppressWarnings("unchecked")
    public void clear() {
	this.tab = (Ref<E>[])new Ref[32];
	this.sz = 0;
    }

    public int size() {
	return(sz);
    }

    public boolean isEmpty() {
	return(sz == 0);
    }

    private int nextidx(Ref[] tab, int idx) {
	return((idx + 1) & (tab.length - 1));
    }

    private int hashidx(Ref[] tab, E el) {
	return(hash.hash(el) & (tab.length - 1));
    }

    @SuppressWarnings("unchecked")
    private int findidx(Ref<E>[] tab, Object e) {
	E el = (E)e;
	int idx = hashidx(tab, el);
	while(true) {
	    Ref<E> cref = tab[idx];
	    if(cref == null)
		return(-1);
	    E cur = cref.get();
	    if((cur != null) && hash.equal(el, cur))
		return(idx);
	    idx = nextidx(tab, idx);
	}
    }

    private int refidx(Ref<E>[] tab, Ref ref) {
	int idx = ref.hash & (tab.length - 1);
	while(true) {
	    Ref cur = tab[idx];
	    if(cur == null)
		return(-1);
	    if(cur == ref)
		return(idx);
	    idx = nextidx(tab, idx);
	}
    }

    private void clean() {
	int psz = sz;
	Ref<E>[] tab = this.tab;
	Reference<? extends E> ref;
	while((ref = cleanq.poll()) != null) {
	    Ref rr = (Ref)ref;
	    int idx = refidx(tab, rr);
	    if(idx < 0)
		throw(new ConcurrentModificationException());
	    remove(tab, idx);
	}
	ckshrink();
    }

    private void remove(Ref<E>[] tab, int idx) {
	tab[idx] = null;
	for(int nx = nextidx(tab, idx); tab[nx] != null; nx = nextidx(tab, nx)) {
	    int oh = (tab[nx].hash) & (tab.length - 1);
	    if((idx < nx) ? ((oh <= idx) || (nx < oh)) : ((oh <= idx) && (nx < oh))) {
		tab[idx] = tab[nx];
		tab[nx] = null;
		idx = nx;
	    }
	}
	sz--;
    }

    public boolean remove(Object el) {
	clean();
	if(el == null)
	    return(false);
	Ref<E>[] tab = this.tab;
	int idx = findidx(tab, el);
	if(idx < 0)
	    return(false);
	remove(tab, idx);
	ckshrink();
	return(true);
    }

    private void ckshrink() {
	int nsz = tab.length;
	while((nsz > 32) && (sz < (nsz * 3) / 16))
	    nsz >>= 1;
	if(nsz < tab.length)
	    resize(nsz);
    }

    @SuppressWarnings("unchecked")
    private void resize(int nsz) {
	Ref<E>[] ctab = this.tab;
	Ref<E>[] ntab = (Ref<E>[])new Ref[nsz];
	for(int i = 0; i < ctab.length; i++) {
	    Ref<E> cur = ctab[i];
	    if(cur != null) {
		int idx = cur.hash & (ntab.length - 1);
		for(; ntab[idx] != null; idx = nextidx(ntab, idx));
		ntab[idx] = cur;
	    }
	}
	this.tab = ntab;
    }

    public boolean add(E el) {
	if(el == null)
	    throw(new NullPointerException());
	clean();
	Ref<E>[] tab = this.tab;
	int idx = hashidx(tab, el);
	while(true) {
	    Ref<E> cref = tab[idx];
	    if(cref == null)
		break;
	    E cur = cref.get();
	    if((cur != null) && hash.equal(el, cur))
		return(false);
	    idx = nextidx(tab, idx);
	}
	tab[idx] = new Ref<>(el, cleanq);
	if(++sz >= ((tab.length * 3) / 4))
	    resize(tab.length * 2);
	return(true);
    }

    public Iterator<E> iterator() {
	return(new Iterator<E>() {
		int i = 0, lasti;
		E next = null, last = null;

		public boolean hasNext() {
		    if(next != null)
			return(true);
		    while(true) {
			if(i >= tab.length)
			    return(false);
			if((tab[i] != null) && ((next = tab[i].get()) != null))
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
		    if(tab[lasti].get() != last)
			throw(new ConcurrentModificationException());
		    WeakHashedSet.this.remove(tab, lasti);
		    last = null;
		}
	    });
    }

    public boolean contains(Object el) {
	return(findidx(tab, el) >= 0);
    }

    public E find(E el) {
	Ref<E>[] tab = this.tab;
	int idx = findidx(tab, el);
	if(idx < 0)
	    return(null);
	return(tab[idx].get());
    }

    public E intern(E el) {
	E ret = find(el);
	if(ret == null)
	    add(ret = el);
	return(ret);
    }
}
