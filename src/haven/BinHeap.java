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

public class BinHeap<T> extends AbstractCollection<T> implements Queue<T> {
    private static final Object[] EMPTY = new Object[0];
    private final Comparator<? super T> order;
    private final Map<Object, Integer> index = new IdentityHashMap<>();
    private Object[] heap = EMPTY;
    private int size = 0;

    public BinHeap(Comparator<? super T> order) {
	this.order = order;
    }

    @SuppressWarnings("unchecked")
    private T get(int i) {
	return((T)heap[i]);
    }

    private void raise(T ob, int idx) {
	while(idx > 0) {
	    int p = (idx - 1) >> 1;
	    if(order.compare(get(p), ob) < 0)
		break;
	    index.put(heap[idx] = heap[p], idx);
	    idx = p;
	}
	index.put(heap[idx] = ob, idx);
    }

    private void lower(T ob, int idx) {
	while(true) {
	    int c = (idx << 1) + 1;
	    if(c >= size)
		break;
	    if((c + 1 < size) && (order.compare(get(c + 1), get(c)) < 0))
		c = c + 1;
	    if(order.compare(get(c), ob) > 0)
		break;
	    index.put(heap[idx] = heap[c], idx);
	    idx = c;
	}
	index.put(heap[idx] = ob, idx);
    }

    public boolean add(T ob) {
	if(ob == null)
	    throw(new NullPointerException());
	if(index.containsKey(ob))
	    return(false);
	if(heap.length == size)
	    heap = Arrays.copyOf(heap, Math.max(heap.length * 2, 16));
	raise(ob, size++);
	return(true);
    }

    public boolean remove(Object ob) {
	if(ob == null)
	    return(false);
	Integer idxp = index.remove(ob);
	if(idxp == null)
	    return(false);
	int idx = idxp;
	T rep = get(--size);
	heap[size] = null;
	if(rep != ob) {
	    if((idx > 0) && (order.compare(get((idx - 1) >> 1), rep) > 0))
		raise(rep, idx);
	    else
		lower(rep, idx);
	}
	if((size <= heap.length >> 2) && (heap.length > 16))
	    heap = Arrays.copyOf(heap, heap.length / 2);
	return(true);
    }

    public int size() {
	return(size);
    }

    public Iterator<T> iterator() {
	return(new Iterator<T>() {
		int i = 0;

		public boolean hasNext() {
		    return(i < size);
		}

		public T next() {
		    return(get(i++));
		}
	    });
    }

    public void clear() {
	heap = EMPTY;
	size = 0;
    }

    public boolean contains(Object ob) {
	return(index.containsKey(ob));
    }

    public boolean offer(T ob) {
	add(ob);
	return(true);
    }

    public T remove() {
	if(size == 0)
	    throw(new NoSuchElementException());
	T ret = get(0);
	remove(ret);
	return(ret);
    }

    public T poll() {
	if(size == 0)
	    return(null);
	T ret = get(0);
	remove(ret);
	return(ret);
    }

    public T element() {
	if(size == 0) throw(new NoSuchElementException());
	return(get(0));
    }

    public T peek() {
	return((size == 0) ? null : get(0));
    }
}
