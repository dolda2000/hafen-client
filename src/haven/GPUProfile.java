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
import haven.render.*;

public class GPUProfile extends Profile {
    private Collection<Frame> waiting = new LinkedList<Frame>();

    public GPUProfile(int hl) {
	super(hl);
    }

    public class Frame extends Profile.Frame {
	private List<String> nw = new ArrayList<>(16);
	private List<Long> queries = new ArrayList<>(16);

	public Frame(Render out) {
	    query(out);
	}

	private void query(Render out) {
	    queries.add(0l);
	    int idx = queries.size() - 1;
	    out.timestamp(val -> queries.set(idx, val));
	}

	public void tick(Render out, String nm) {
	    query(out);
	    nw.add(nm);
	}

	public void fin(Render out) {
	    query(out);
	    waiting.add(this);
	    check();
	}

	public void fin2() {
	    long[] tms = new long[queries.size()];
	    for(int i = 0; i < tms.length; i++)
		tms[i] = queries.get(i);
	    int np = tms.length - 2;
	    double total = (tms[tms.length - 1] - tms[0]) / 1000000000.0;
	    String[] nm = new String[np];
	    double[] prt = new double[np];
	    for(int i = 0; i < prt.length; i++) {
		nm[i] = nw.get(i);
		prt[i] = (tms[i + 1] - tms[i]) / 1000000000.0;
	    }
	    fin(total, nm, prt);
	    nw = null;
	    queries = null;
	}
    }

    public void check() {
	for(Iterator<Frame> i = waiting.iterator(); i.hasNext();) {
	    Frame f = i.next();
	    for(long qo : f.queries) {
		if(qo == 0)
		    return;
	    }
	    f.fin2();
	    i.remove();
	}
    }
}
