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

import javax.media.opengl.*;

public abstract class GLState {
    public abstract void apply(GOut g);
    public abstract void unapply(GOut g);
    
    public boolean applyfrom(GOut g, GLState from) {
	return(false);
    }

    private class Wrapping implements Rendered {
	private final Rendered r;
	
	private Wrapping(Rendered r) {
	    this.r = r;
	}
	
	public void draw(GOut g) {
	    g.matsel(GLState.this);
	    r.draw(g);
	}

	public Order setup(RenderList rl) {
	    return(r.setup(rl));
	}
    }
    
    private class FWrapping extends Wrapping implements FRendered {
	private final FRendered f; /* :-P */

	private FWrapping(FRendered r) {
	    super(r);
	    this.f = r;
	}
	
	public void drawflat(GOut g) {
	    g.matsel(null);
	    f.drawflat(g);
	}
    }

    public Rendered apply(Rendered r) {
	if(r instanceof FRendered)
	    return(new FWrapping((FRendered)r));
	else
	    return(new Wrapping(r));
    }
    
    public static final GLState xray = new GLState() {
	    public void apply(GOut g) {
		g.gl.glDisable(GL.GL_DEPTH_TEST);
	    }
	    
	    public void unapply(GOut g) {
		g.gl.glEnable(GL.GL_DEPTH_TEST);
	    }
	};
}
