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
import haven.render.sl.*;
import haven.render.sl.ValBlock.Value;
import static haven.render.sl.Type.*;
import static haven.render.sl.Cons.*;

public class MeshMorph {
    public final VertexContext vctx;
    private final Collection<Morpher> morphs = new ArrayList<>();
    private final Collection<Attrib> morphed = new ArrayList<>();

    public static enum MorphType {
	NONE, POS, DIR
    }

    public static interface Morpher {
	public void morph(Value val, MorphType type, VertexContext vctx);
    }

    private static class Attrib {
	final Value va;
	final MorphType type;

	Attrib(Value va, MorphType type) {
	    this.va = va;
	    this.type = type;
	}
    }

    private MeshMorph(VertexContext vctx) {
	this.vctx = vctx;
	vctx.prog.module(this);
	Homo3D homo = Homo3D.get(vctx.prog);
	add(homo.objv, MorphType.POS);
	add(homo.objn, MorphType.DIR);
    }

    public void add(Morpher morph) {
	morphs.add(morph);
	for(Attrib attr : morphed)
	    morph.morph(attr.va, attr.type, this.vctx);
    }

    public void add(Value va, MorphType type) {
	morphed.add(new Attrib(va, type));
	for(Morpher morph : morphs)
	    morph.morph(va, type, this.vctx);
    }

    public static MeshMorph get(VertexContext vctx) {
	MeshMorph ret = vctx.prog.getmod(MeshMorph.class);
	if(ret == null)
	    ret = new MeshMorph(vctx);
	return(ret);
    }
}
