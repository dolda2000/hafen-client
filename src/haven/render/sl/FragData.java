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

package haven.render.sl;

import java.util.*;

public class FragData extends Variable.Global {
    public boolean primary = false;

    public FragData(Type type, Symbol name) {
	super(type, name);
    }

    public FragData(Type type, String infix) {
	this(type, new Symbol.Shared("s_" + infix));
    }

    public FragData(Type type) {
	this(type, new Symbol.Shared());
    }

    public FragData primary() {
	primary = true;
	return(this);
    }

    private static final Object defid = new PostProc.AutoID("fragdata", 15000) {
	    public void proc(Context ctx) {
		FragmentContext fctx = (FragmentContext)ctx;
		Collection<FragData> used = new HashSet<>();
		for(Toplevel tl : fctx.vardefs) {
		    if(tl instanceof Def)
			used.add(((Def)tl).var());
		}
		FragData primary = null;
		FragData[] slots = new FragData[used.size()];
		int s = 0;
		for(FragData f : used) {
		    if(f.primary) {
			if(primary == null) {
			    slots[0] = primary = f;
			    s = 1;
			} else {
			    throw(new RuntimeException("Several fragment data require primary slot: " + primary + " and " + f));
			}
		    }
		}
		for(FragData f : used) {
		    if(f != primary)
			slots[s++] = f;
		}
		if(s != slots.length)
		    throw(new AssertionError());
		if(slots.length > 1) {
		    for(int i = 0; i < slots.length; i++)
			fctx.main.code.add(new LBinOp.Assign(new Index(fctx.gl_FragData.ref(), new IntLiteral(i)), slots[i].ref()));
		} else if(slots.length == 1) {
		    fctx.main.code.add(new LBinOp.Assign(fctx.gl_FragColor.ref(), slots[0].ref()));
		}
		fctx.prog.fragdata.addAll(used);
	    }
	};
    private class Def extends Definition implements PostProc.Processed {
	public void output(Output out) {
	    if(out.ctx instanceof ShaderContext) {
		((ShaderContext)out.ctx).prog.fragdata.add(FragData.this);
	    }
	    super.output(out);
	}

	public void process(PostProc proc) {}
	public Object ppid() {return(defid);}
	private FragData var() {return(FragData.this);}
    }

    public void use(Context ctx) {
	if(!defined(ctx))
	    ctx.vardefs.add(new Def());
    }
}
