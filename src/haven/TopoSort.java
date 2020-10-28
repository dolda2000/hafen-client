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

public class TopoSort<T> {
    protected final Hash<? super T> hash;
    protected final Set<T> nodes;
    protected final Graph<T> edges;

    public static class Graph<T> {
	public final Hash<? super T> hash;
	public final Map<T, Set<T>> fwd;
	public final Map<T, Set<T>> rwd;

	public Graph(Hash<? super T> hash) {
	    this.hash = hash;
	    fwd = new HashedMap<>(hash);
	    rwd = new HashedMap<>(hash);
	}

	public <S extends T> Graph(Hash<? super T> hash, Graph<S> from) {
	    this(hash);
	    for(Map.Entry<S, Set<S>> fwd : from.fwd.entrySet()) {
		T f = fwd.getKey();
		for(T t : fwd.getValue())
		    add(f, t);
	    }
	}

	public void add(T from, T to) {
	    fwd.computeIfAbsent(from, k -> new HashedSet<>(hash)).add(to);
	    rwd.computeIfAbsent(to, k -> new HashedSet<>(hash)).add(from);
	}

	public void remove(T from, T to) {
	    Set<T> f = fwd.get(from);
	    if((f == null) || !f.remove(to))
		throw(new NoSuchElementException());
	    if(f.isEmpty())
		fwd.remove(from);
	    Set<T> r = rwd.get(to);
	    if((r == null) || !r.remove(from))
		throw(new NoSuchElementException());
	    if(r.isEmpty())
		rwd.remove(to);
	}

	public boolean removefrom(T from) {
	    Collection<T> to = fwd.get(from);
	    if(to == null)
		return(false);
	    if(to.isEmpty()) throw(new AssertionError());
	    for(T t : new ArrayList<>(to))
		remove(from, t);
	    return(true);
	}

	public boolean removeto(T to) {
	    Collection<T> from = rwd.get(to);
	    if(from == null)
		return(false);
	    if(from.isEmpty()) throw(new AssertionError());
	    for(T f : new ArrayList<>(from))
		remove(f, to);
	    return(true);
	}

	public Collection<T> from(T from) {
	    Collection<T> to = fwd.get(from);
	    if(to == null)
		return(Collections.emptyList());
	    return(to);
	}

	public Collection<T> to(T to) {
	    Collection<T> from = rwd.get(to);
	    if(from == null)
		return(Collections.emptyList());
	    return(from);
	}
    }

    public TopoSort(Hash<? super T> hash) {
	this.hash = hash;
	this.nodes = new HashedSet<>(hash);
	this.edges = new Graph<>(hash);
    }

    public TopoSort() {
	this(Hash.id);
    }

    public TopoSort add(T before, T after) {
	nodes.add(before);
	nodes.add(after);
	edges.add(before, after);
	return(this);
    }

    public TopoSort add(Iterable<? extends T> order) {
	Iterator<? extends T> i = order.iterator();
	if(!i.hasNext())
	    return(this);
	T last = i.next();
	if(i.hasNext()) {
	    while(i.hasNext()) {
		T next = i.next();
		add(last, next);
		last = next;
	    }
	} else {
	    nodes.add(last);
	}
	return(this);
    }

    private void remove(T before, T after) {
	edges.remove(before, after);
    }

    protected List<T> order;

    public List<T> sort() {
	this.order = new ArrayList<>();
	Collection<T> first = new ArrayList<>();
	while(!nodes.isEmpty()) {
	    first.clear();
	    for(T node : nodes) {
		if(!edges.rwd.containsKey(node))
		    first.add(node);
	    }
	    List<T> pick;
	    if(first.isEmpty()) {
		pick = pickbad();
	    } else if(first.size() == 1) {
		pick = Collections.singletonList(Utils.el(first));
	    } else {
		pick = pick(first);
	    }
	    for(T node : pick) {
		order.add(node);
		nodes.remove(node);
		edges.removefrom(node);
	    }
	}
	return(order);
    }

    public static class InconsistentOrder extends RuntimeException {
	public InconsistentOrder(String message) {super(message);}
	public InconsistentOrder() {}
    }

    protected List<T> pickbad() {
	throw(new InconsistentOrder(findcycles().toString()));
    }

    protected List<T> pick(Collection<T> from) {
	return(new ArrayList<>(from));
    }

    /* XXX: It would be nicer if this could find only unpreceded, strict cycles. */
    protected Collection<Collection<T>> findcycles() {
	Graph<T> edges = new Graph<>(hash, this.edges);
	Set<T> nodes = new HashedSet<>(hash, this.nodes);
	while(true) {
	    boolean done = true;
	    for(Iterator<T> i = nodes.iterator(); i.hasNext();) {
		T node = i.next();
		if(!edges.fwd.containsKey(node)) {
		    done = false;
		    i.remove();
		    edges.removeto(node);
		}
	    }
	    if(done)
		break;
	}
	Collection<Collection<T>> ret = new ArrayList<>();
	while(!nodes.isEmpty()) {
	    Collection<T> open = new LinkedList<>(Collections.singletonList(Utils.take(nodes)));
	    Collection<T> taken = new ArrayList<>();
	    while(!open.isEmpty()) {
		T node = Utils.take(open);
		taken.add(node);
		for(T other : edges.from(node)) {
		    if(nodes.contains(other)) {
			open.add(other);
			nodes.remove(other);
		    }
		}
	    }
	    ret.add(taken);
	}
	return(ret);
    }
}
