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

import java.util.*;

public interface NodeWrap {
    public RenderTree.Node apply(RenderTree.Node node);

    public static interface Wrapping {
	public NodeWrap wrap();
	public RenderTree.Node wrapped();
    }

    public static class Composed implements NodeWrap {
	private final NodeWrap[] wraps;

	public Composed(NodeWrap... wraps) {
	    this.wraps = wraps;
	}

	public RenderTree.Node apply(RenderTree.Node node) {
	    for(int i = wraps.length - 1; i >= 0; i--)
		node = wraps[i].apply(node);
	    return(node);
	}

	public boolean equals(Object o) {
	    if(o == this)
		return(true);
	    if(!(o instanceof Composed))
		return(false);
	    return(Arrays.equals(wraps, ((Composed)o).wraps));
	}

	public int hashCode() {
	    return(Arrays.hashCode(wraps));
	}

	public String toString() {
	    return("#<composed " + Arrays.asList(wraps) + ">");
	}
    }

    public static NodeWrap compose(NodeWrap... w) {
	int n = 0;
	for(int i = 0; i < w.length; i++) {
	    if(w[i] != null)
		n++;
	}
	if(n != w.length) {
	    NodeWrap[] buf = new NodeWrap[n];
	    for(int i = 0, o = 0; i < w.length; i++) {
		if(w[i] != null)
		    buf[o++] = w[i];
	    }
	    w = buf;
	}
	if(w.length == 0)
	    return(nil);
	if(w.length == 1)
	    return(w[0]);
	return(new Composed(w));
    }

    public static final NodeWrap nil = n -> n;
}
