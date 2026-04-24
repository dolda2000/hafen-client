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

package haven.render.gl;

import java.util.*;

/* Pair of render queues -- one for prep work that must run before
 * draw commands, one for submitted draw commands -- with the
 * "snapshot submitted before prep" invariant baked into drain().
 *
 * Extracted from GLEnvironment so the ordering invariant and the
 * invalid-rejection paths can be tested without standing up a real
 * GL context. The R type parameter lets tests use marker objects in
 * place of GLRender. */
final class RenderQueue<R> {
    private final Object submittedMon = new Object();
    private final Queue<R> submitted = new LinkedList<>();
    private final Object prepMon = new Object();
    private final Queue<R> prepq = new LinkedList<>();
    private volatile boolean invalid = false;

    /* Ordered pair returned by drain(): prep entries execute first,
     * then submitted entries. */
    static final class Snapshot<R> {
	final List<R> prep;
	final List<R> submitted;
	Snapshot(List<R> prep, List<R> submitted) {
	    this.prep = prep;
	    this.submitted = submitted;
	}
    }

    /* Returns true if accepted, false if the queue was invalidated.
     * On false, the caller is responsible for aborting/disposing the item. */
    boolean enqueueSubmitted(R r) {
	synchronized(submittedMon) {
	    if(invalid)
		return(false);
	    submitted.add(r);
	    submittedMon.notifyAll();
	    return(true);
	}
    }

    boolean enqueuePrep(R r) {
	synchronized(prepMon) {
	    if(invalid)
		return(false);
	    prepq.add(r);
	    return(true);
	}
    }

    void awaitSubmitted() throws InterruptedException {
	synchronized(submittedMon) {
	    while(submitted.peek() == null)
		submittedMon.wait();
	}
    }

    /* Snapshot submitted FIRST, then prep. This preserves the
     * invariant that every render in the submitted snapshot has its
     * prep already in the prep snapshot: any submit() that races and
     * lands after the submitted-snapshot will be deferred to the
     * next pass, and its prep (also enqueued before the render) will
     * land in that next pass too. */
    Snapshot<R> drain() {
	List<R> sCopy;
	synchronized(submittedMon) {
	    sCopy = new ArrayList<>(submitted);
	    submitted.clear();
	}
	List<R> pCopy;
	synchronized(prepMon) {
	    pCopy = new ArrayList<>(prepq);
	    prepq.clear();
	}
	return(new Snapshot<>(pCopy, sCopy));
    }

    void invalidate() {invalid = true;}
    boolean isInvalid() {return(invalid);}

    /* Test-only accessors. */
    int submittedSize() {synchronized(submittedMon) {return(submitted.size());}}
    int prepSize() {synchronized(prepMon) {return(prepq.size());}}
}
