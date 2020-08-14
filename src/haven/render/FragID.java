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

import java.awt.Color;
import haven.render.sl.*;
import haven.render.sl.ValBlock.Value;

public class FragID<T extends Texture.Image> extends State {
    public static final Slot<FragID> tex = new Slot<>(Slot.Type.SYS, FragID.class);
    public static final Slot<ID> id = new Slot<>(Slot.Type.DRAW, ID.class)
	.instanced(st -> ID.instancer);
    public static final FragData fragid = new FragData(Type.INT, "fragid", p -> p.get(tex).image, tex);
    private static final InstancedUniform uid = new InstancedUniform.Int("id", p -> {
	    ID v = p.get(id);
	    return((v == null) ? 0 : v.val);
	}, id);
    public final T image;

    public FragID(T image) {
	this.image = image;
    }

    public static class ID extends State implements InstanceBatch.AttribState {
	public final int val;

	public ID(int val) {
	    this.val = val;
	}

	public ShaderMacro shader() {return(null);}
	public void apply(Pipe p) {p.put(id, this);}

	public int hashCode() {
	    return(val);
	}

	public boolean equals(Object o) {
	    return((o instanceof ID) &&
		   (((ID)o).val == this.val));
	}

	static final Instancer<ID> instancer = new Instancer<ID>() {
		final ID instanced = new ID(0) {
		    public ShaderMacro shader() {return(mkinstanced);}
		};

		public ID inststate(ID uinst, InstanceBatch bat) {
		    return(instanced);
		}
	};

	public InstancedAttribute[] attribs() {
	    return(new InstancedAttribute[] {uid.attrib});
	}
    }

    public static final AutoVarying transfer = new AutoVarying(Type.INT) {
	    protected Interpol ipol(Context ctx) {return(Interpol.FLAT);}
	    protected Expression root(VertexContext vctx) {
		return(uid.ref());
	    }
	};

    public static Value fragid(FragmentContext fctx) {
	return(fctx.mainvals.ext(fragid, () -> fctx.mainvals.new Value(Type.INT) {
		public Expression root() {
		    return(transfer.ref());
		}

		protected void cons2(Block blk) {
		    blk.add(new LBinOp.Assign(fragid.ref(), init));
		}
	    }));
    }

    private static final ShaderMacro shader = prog -> fragid(prog.fctx).force();
    public ShaderMacro shader() {
	return(shader);
    }

    public void apply(Pipe p) {p.put(tex, this);}

    public int hashCode() {
	return(System.identityHashCode(image));
    }

    public boolean equals(Object o) {
	return((o instanceof FragID) &&
	       (((FragID)o).image == this.image));
    }
}
