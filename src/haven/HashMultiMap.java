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

public class HashMultiMap<K, V> implements MultiMap<K, V> {
    private final HashMap<K, Object> bk = new HashMap<>();
    private int size = 0;

    private static class TaggedList<T> extends ArrayList<T> {
    }

    @SuppressWarnings("unchecked")
    public void put(K key, V value) {
	if(value == null)
	    throw(new NullPointerException("value"));
	Object prev = bk.get(key);
	if(prev instanceof TaggedList) {
	    TaggedList<V> ls = (TaggedList<V>)prev;
	    ls.add(value);
	} else if(prev != null) {
	    TaggedList<V> ls = new TaggedList<>();
	    ls.add((V)prev);
	    ls.add(value);
	    bk.put(key, ls);
	} else {
	    bk.put(key, value);
	}
	size++;
    }

    @SuppressWarnings("unchecked")
    public V remove(K key, V value) {
	if(value == null)
	    throw(new NullPointerException("value"));
	Object cur = bk.get(key);
	V ret;
	if(cur instanceof TaggedList) {
	    TaggedList<V> ls = (TaggedList<V>)cur;
	    ret = ls.remove(value) ? value : null;
	    if(ls.size() == 1)
		bk.put(key, ls.get(0));
	    size--;
	} else if(cur != null) {
	    bk.remove(key);
	    ret = (V)cur;
	    size--;
	} else {
	    ret = null;
	}
	return(ret);
    }

    @SuppressWarnings("unchecked")
    public Collection<V> removeall(K key) {
	Object prev = bk.remove(key);
	if(prev instanceof TaggedList) {
	    TaggedList<V> ls = (TaggedList<V>)prev;
	    size -= ls.size();
	    return(ls);
	} else if(prev != null) {
	    size--;
	    return(Collections.singletonList((V)prev));
	} else {
	    return(Collections.emptyList());
	}
    }

    @SuppressWarnings("unchecked")
    public V pop(K key) {
	Object cur = bk.get(key);
	V ret;
	if(cur instanceof TaggedList) {
	    TaggedList<V> ls = (TaggedList<V>)cur;
	    int n = ls.size() - 1;
	    ret = ls.get(n);
	    ls.remove(n);
	    if(n == 1)
		bk.put(key, ls.get(0));
	    size--;
	} else if(cur != null) {
	    bk.remove(key);
	    ret = (V)cur;
	    size--;
	} else {
	    ret = null;
	}
	return(ret);
    }

    @SuppressWarnings("unchecked")
    public V get(K key) {
	Object cur = bk.get(key);
	if(cur instanceof TaggedList)
	    return(null);
	return((V)cur);
    }

    @SuppressWarnings("unchecked")
    public Collection<V> getall(K key) {
	Object cur = bk.get(key);
	if(cur instanceof TaggedList) {
	    return((TaggedList<V>)cur);
	} else if(cur != null) {
	    return(Collections.singletonList((V)cur));
	} else {
	    return(Collections.emptyList());
	}
    }

    public int size() {
	return(size);
    }

    private Collection<V> values = null;
    public Collection<V> values() {
	if(values == null) {
	    values = new AbstractCollection<V>() {
		    public int size() {
			return(size);
		    }

		    public Iterator<V> iterator() {
			return(new Iterator<V>() {
				Iterator<Map.Entry<K, Object>> bki = bk.entrySet().iterator();
				Map.Entry<K, Object> ent;
				V next, prev;
				TaggedList<V> ls;
				Iterator<V> lsi;

				@SuppressWarnings("unchecked")
				public boolean hasNext() {
				    if(next != null)
					return(true);
				    if(lsi != null) {
					if(lsi.hasNext()) {
					    next = lsi.next();
					    return(true);
					}
					lsi = null;
				    }
				    if(!bki.hasNext())
					return(false);
				    this.ent = bki.next();
				    Object v = this.ent.getValue();
				    if(v instanceof TaggedList) {
					this.ls = (TaggedList<V>)v;
					this.lsi = ls.iterator();
					this.next = this.lsi.next();
				    } else {
					next = (V)v;
				    }
				    return(true);
				}

				public V next() {
				    if(!hasNext())
					throw(new NoSuchElementException());
				    this.prev = this.next;
				    this.next = null;
				    return(this.prev);
				}

				public void remove() {
				    if(this.prev == null)
					throw(new IllegalStateException());
				    if(this.lsi != null) {
					this.lsi.remove();
					this.prev = null;
				    } else {
					HashMultiMap.this.remove(this.ent.getKey(), this.prev);
					this.prev = null;
				    }
				}
			    });
		    }
		};
	}
	return(values);
    }
}
