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
import haven.render.sl.ShaderMacro;
import haven.render.Texture2D.Sampler2D;

public abstract class RenderContext extends State implements OwnerContext {
    public static final Slot<RenderContext> slot = new Slot<>(Slot.Type.SYS, RenderContext.class);
    private final Collection<PostProcessor> post = new ArrayList<>();

    public static abstract class PostProcessor implements Disposable {
	public Sampler2D buf = null;

	public abstract void run(GOut g, Sampler2D in);

	public void dispose() {
	    if(buf != null)
		buf.dispose();
	}
    }

    public Collection<PostProcessor> postproc() {return(post);}
    public void add(PostProcessor post) {this.post.add(post);}
    public void remove(PostProcessor post) {this.post.remove(post);}

    public abstract Pipe.Op basic(Object id);
    public abstract void basic(Object id, Pipe.Op state);

    public ShaderMacro shader() {return(null);}
    public void apply(Pipe p) {p.put(slot, this);}
}
