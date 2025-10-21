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
import java.util.function.*;
import haven.render.*;

public class GPUProfile extends Profile {
    private final Collection<Frame> waiting = new LinkedList<>();
    private long fno = 1;

    public GPUProfile(int hl) {
	super(hl);
    }

    public abstract class Part extends Profile.Part {
	private long f, t;
	private Part curp;
	private boolean fin;
	boolean done;

	public Part(Object nm) {
	    super(nm);
	}

	public double f() {return(f * 1e-9);}
	public double t() {return(t * 1e-9);}

	protected abstract Frame frame();

	void query(Render out, Consumer<Long> prev) {
	    out.timestamp(val -> {
		    this.f = val;
		    if(prev != null)
			prev.accept(val);
		});
	}

	public Part part(Render out, Object nm) {
	    Part p = new FramePart(nm, frame());
	    p.query(out, (curp != null) ? curp.tfin() : null);
	    frame().waiting.add(p);
	    add(p);
	    return(curp = p);
	}

	private Consumer<Long> tfin() {
	    if(fin)
		return(val -> {});
	    Consumer<Long> prev = (curp != null) ? curp.tfin() : val -> {};
	    return(val -> {
		    t = val;
		    done = true;
		    prev.accept(val);
		});
	}

	public void fin(Render out) {
	    if(fin)
		return;
	    out.timestamp(tfin());
	    fin = true;
	}
    }

    private class FramePart extends Part {
	private final Frame frame;
	private FramePart(Object nm, Frame frame) {super(nm); this.frame = frame;}
	protected Frame frame() {return(frame);}
    }

    public class Frame extends Part {
	private List<Part> waiting = new LinkedList<>();

	public Frame(Render out) {
	    super(String.format("frame %,d", fno++));
	    query(out, null);
	}

	protected Frame frame() {return(this);}

	public void fin(Render out) {
	    super.fin(out);
	    GPUProfile.this.waiting.add(this);
	    check();
	}

	private void fin2() {
	    waiting = null;
	    GPUProfile.this.add(this);
	}
    }

    public void check() {
	for(Iterator<Frame> i = waiting.iterator(); i.hasNext();) {
	    Frame f = i.next();
	    if(!f.done)
		return;
	    for(Iterator<Part> o = f.waiting.iterator(); o.hasNext();) {
		Part p = o.next();
		if(!p.done)
		    return;
		o.remove();
	    }
	    f.fin2();
	    i.remove();
	}
    }
}
