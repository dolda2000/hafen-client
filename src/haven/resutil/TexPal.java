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

package haven.resutil;

import java.util.*;
import haven.*;
import haven.render.*;
import haven.render.sl.*;
import haven.render.sl.ValBlock.Value;
import static haven.render.sl.Cons.*;
import static haven.render.sl.Function.PDir.*;
import static haven.render.sl.Type.*;

public class TexPal extends State {
    public static final Slot<TexPal> slot = new Slot<TexPal>(Slot.Type.DRAW, TexPal.class);
    public final TexRender tex;

    public TexPal(TexRender tex) {
	this.tex = tex;
    }

    private static final Uniform ctex = new Uniform(SAMPLER2D, p -> p.get(slot).tex.img, slot);
    private static final ShaderMacro shader = prog -> {
	Tex2D.get(prog).color().mod(in -> texture2D(ctex.ref(), pick(in, "rg")), -100);
    };

    public ShaderMacro shader() {return(shader);}

    public void apply(Pipe buf) {
	buf.put(slot, this);
    }

    @Material.ResName("pal")
    public static class $res implements Material.ResCons2 {
	public Material.Res.Resolver cons(final Resource res, Object... args) {
	    final Indir<Resource> tres;
	    final int tid;
	    int a = 0;
	    if(args[a] instanceof String) {
		tres = res.pool.load((String)args[a], Utils.iv(args[a + 1]));
		tid = Utils.iv(args[a + 2]);
		a += 3;
	    } else {
		tres = res.indir();
		tid = Utils.iv(args[a]);
		a += 1;
	    }
	    return(new Material.Res.Resolver() {
		    public void resolve(Collection<Pipe.Op> buf, Collection<Pipe.Op> dynbuf) {
			TexR rt = tres.get().layer(TexR.class, tid);
			if(rt == null)
			    throw(new RuntimeException(String.format("Specified texture %d for %s not found in %s", tid, res, tres)));
			buf.add(new TexPal(rt.tex()));
		    }
		});
	}
    }
}
