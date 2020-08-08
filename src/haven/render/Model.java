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

package haven.render;

import java.util.function.*;
import haven.Disposable;

public class Model implements Rendered, RenderTree.Node, Disposable {
    public final Mode mode;
    public final VertexArray va;
    public final Indices ind;
    public final int f, n;
    public final int ninst;
    public Disposable ro;

    public enum Mode {
	POINTS, LINES, LINE_STRIP, TRIANGLES, TRIANGLE_STRIP, TRIANGLE_FAN
    }

    public Model(Mode mode, VertexArray va, Indices ind, int f, int n, int ninst) {
	if((this.mode = mode) == null)
	    throw(new NullPointerException());
	if((this.va = va) == null)
	    throw(new NullPointerException());
	this.ind = ind;
	this.f = f;
	if(n >= 0)
	    this.n = n;
	else
	    this.n = (ind == null) ? va.num() : ind.n;
	this.ninst = ninst;
    }

    public Model(Mode mode, VertexArray va, Indices ind, int f, int n) {
	this(mode, va, ind, f, n, 1);
    }

    public Model(Mode mode, VertexArray va, Indices ind) {
	this(mode, va, ind, 0, -1);
    }

    public static class Indices implements DataBuffer, Disposable {
	public final NumberFormat fmt;
	public final int n;
	public final Usage usage;
	public final Filler<? super Indices> init;
	public boolean shared = false;
	public Disposable ro;

	public Indices(int n, NumberFormat fmt, Usage usage, Filler<? super Indices> init) {
	    this.fmt = fmt;
	    this.n = n;
	    this.usage = usage;
	    this.init = init;
	}

	public int size() {
	    return(n * fmt.size);
	}

	public Indices shared() {
	    this.shared = true;
	    return(this);
	}

	public void dispose() {
	    synchronized(this) {
		if(ro != null) {
		    ro.dispose();
		    ro = null;
		}
	    }
	}
    }

    public void dispose() {
	synchronized(this) {
	    if(ro != null) {
		ro.dispose();
		ro = null;
	    }
	}
	if((ind != null) && !ind.shared)
	    ind.dispose();
	if(!va.shared)
	    va.dispose();
    }

    public void draw(Pipe state, Render out) {
	out.draw(state, this);
    }
}
