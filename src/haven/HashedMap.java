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

public class HashedMap<K, V> extends AbstractMap<K, V> {
    private static final Object NULL = new Object();
    public final Hash<? super K> hash;
    private Object[] tab;
    private int sz;

    public HashedMap(Hash<? super K> hash) {
	this.hash = hash;
	clear();
    }

    public HashedMap(Hash<? super K> hash, Map<? extends K, ? extends V> from) {
	this(hash);
	putAll(from);
    }

    private Object nullkey(K key) {
	return((key == null)?NULL:key);
    }

    @SuppressWarnings("unchecked")
    private K keynull(Object k) {
	return((k == NULL)?null:(K)k);
    }

    private static int nextidx(Object[] tab, int idx) {
	return((idx + 2) & (tab.length - 1));
    }

    private int hashidx(Object[] tab, K k) {
	return((hash.hash(k) << 1) & (tab.length - 1));
    }

    private void resize(int nsz) {
	Object[] ctab = this.tab;
	Object[] ntab = new Object[nsz * 2];
	for(int i = 0; i < ctab.length; i += 2) {
	    Object key = ctab[i];
	    if(key != null) {
		int idx = hashidx(ntab, keynull(key));
		for(; ntab[idx] != null; idx = nextidx(ntab, idx));
		ntab[idx] = key;
		ntab[idx + 1] = ctab[i + 1];
	    }
	}
	this.tab = ntab;
    }

    @SuppressWarnings("unchecked")
    public V put(K key, V val) {
	Object k = nullkey(key);
	Object[] tab = this.tab;
	int idx = hashidx(tab, key);
	while(true) {
	    Object cur = tab[idx];
	    if(cur == null)
		break;
	    if(hash.equal(key, keynull(cur))) {
		V pval = (V)tab[idx + 1];
		tab[idx + 1] = val;
		return(pval);
	    }
	    idx = nextidx(tab, idx);
	}
	tab[idx] = k;
	tab[idx + 1] = val;
	if(++sz >= ((tab.length * 3) / 8))
	    resize(tab.length);
	return(null);
    }

    private void remove(Object[] tab, int idx) {
	tab[idx] = tab[idx + 1] = null;
	for(int nx = nextidx(tab, idx); tab[nx] != null; nx = nextidx(tab, nx)) {
	    int oh = hashidx(tab, keynull(tab[nx]));
	    if((idx < nx)?((oh <= idx) || (nx < oh)):((oh <= idx) && (nx < oh))) {
		tab[idx] = tab[nx];
		tab[idx + 1] = tab[nx + 1];
		tab[nx] = tab[nx + 1] = null;
		idx = nx;
	    }
	}
	sz--;
    }

    @SuppressWarnings("unchecked")
    private int findidx(Object[] tab, Object key) {
	K fkey = (K)key;
	int idx = hashidx(tab, fkey);
	while(true) {
	    Object cur = tab[idx];
	    if(cur == null)
		return(-1);
	    if(hash.equal(fkey, keynull(cur)))
		return(idx);
	    idx = nextidx(tab, idx);
	}
    }

    @SuppressWarnings("unchecked")
    public V remove(Object key) {
	Object[] tab = this.tab;
	int idx = findidx(tab, key);
	if(idx < 0)
	    return(null);
	V pval = (V)tab[idx + 1];
	remove(tab, idx);
	return(pval);
    }

    @SuppressWarnings("unchecked")
    public V get(Object key) {
	Object[] tab = this.tab;
	int idx = findidx(tab, key);
	if(idx < 0)
	    return(null);
	return((V)tab[idx + 1]);
    }

    public void clear() {
	this.tab = new Object[32];
	sz = 0;
    }

    public int size() {
	return(sz);
    }

    public boolean isEmpty() {
	return(sz == 0);
    }

    public boolean containsKey(Object key) {
	return(findidx(tab, key) >= 0);
    }

    private class EntryIterator implements Iterator<Entry<K, V>>, Entry<K, V> {
	private final Object[] tab = HashedMap.this.tab;
	private int idx = -2, st = 0;

	public boolean hasNext() {
	    if(st != 1) {
		for(idx += 2; (idx < tab.length) && (tab[idx] == null); idx += 2);
		st = 1;
	    }
	    return(idx < tab.length);
	}

	public Entry<K, V> next() {
	    if(!hasNext())
		throw(new NoSuchElementException());
	    st = 2;
	    return(this);
	}

	public void remove() {
	    if(st != 2)
		throw(new IllegalStateException());
	    if(tab != HashedMap.this.tab)
		throw(new ConcurrentModificationException());
	    HashedMap.this.remove(tab, idx);
	    idx -= 2;
	    st = 0;
	}

	public K getKey() {
	    if(st != 2)
		throw(new IllegalStateException());
	    return(keynull(tab[idx]));
	}

	@SuppressWarnings("unchecked")
	public V getValue() {
	    if(st != 2)
		throw(new IllegalStateException());
	    return((V)tab[idx + 1]);
	}

	@SuppressWarnings("unchecked")
	public V setValue(V val) {
	    if(st != 2)
		throw(new IllegalStateException());
	    V pval = (V)tab[idx + 1];
	    tab[idx + 1] = val;
	    return(pval);
	}
    }

    private class EntrySet extends AbstractSet<Entry<K, V>> {
	private EntrySet() {}

	public Iterator<Entry<K, V>> iterator() {
	    return(new EntryIterator());
	}

	public int size() {
	    return(sz);
	}

	public void clear() {
	    HashedMap.this.clear();
	}
    }

    private Set<Entry<K, V>> es = null;
    public Set<Entry<K, V>> entrySet() {
	if(es == null)
	    es = new EntrySet();
	return(es);
    }
}
