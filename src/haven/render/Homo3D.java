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

import haven.*;
import haven.render.sl.*;
import haven.render.State.Slot;
import haven.render.sl.ValBlock.Value;
import static haven.render.sl.Type.*;
import static haven.render.sl.Cons.*;

public class Homo3D {
    public static final Slot<Projection> prj = new Slot<>(Slot.Type.SYS, Projection.class);
    public static final Slot<Camera> cam = new Slot<>(Slot.Type.SYS, Camera.class);
    public static final Slot<Location.Chain> loc = new Slot<>(Slot.Type.GEOM, Location.Chain.class)
	.instanced(st -> Location.Chain.instancer);
    public static final Attribute vertex = new Attribute(VEC3, "vertex");
    public static final Attribute normal = new Attribute(VEC3, "normal");
    static final Uniform u_prj = new Uniform(MAT4, "proj", Homo3D::prjxf, prj);
    static final Uniform u_cam = new Uniform(MAT4, "cam", Homo3D::camxf, cam);
    static final InstancedUniform u_wxf = new InstancedUniform.Mat4("wxf", Homo3D::locxf, loc);
    public final Value objv, mapv, eyev, objn, eyen;

    public static Matrix4f prjxf(Pipe p) {
	Projection prj_s = p.get(prj);
	return((prj_s == null) ? Matrix4f.id : prj_s.fin(Matrix4f.id));
    }
    public static Matrix4f camxf(Pipe p) {
	Camera cam_s = p.get(cam);
	return((cam_s == null) ? Matrix4f.id : cam_s.fin(Matrix4f.id));
    }
    public static Matrix4f locxf(Pipe p) {
	Location.Chain loc_s = p.get(loc);
	return((loc_s == null) ? Matrix4f.id : loc_s.fin(Matrix4f.id));
    }

    public Homo3D(ProgramContext prog) {
	prog.module(this);
	objv = prog.vctx.mainvals.new Value(Type.VEC4, new Symbol.Gen("objv")) {
		public Expression root() {
		    return(vec4(vertex.ref(), l(1.0)));
		}
	    };
	mapv = prog.vctx.mainvals.new Value(Type.VEC4, new Symbol.Gen("mapv")) {
		public Expression root() {
		    return(mul(u_wxf.ref(), objv.depref()));
		}
	    };
	eyev = prog.vctx.mainvals.new Value(Type.VEC4, new Symbol.Gen("eyev")) {
		public Expression root() {
		    return(mul(u_cam.ref(), mapv.depref()));
		}
	    };

	objn = prog.vctx.mainvals.new Value(Type.VEC3, new Symbol.Gen("objn")) {
		public Expression root() {
		    return(normal.ref());
		}
	    };
	eyen = prog.vctx.mainvals.new Value(Type.VEC3, new Symbol.Gen("eyen")) {
		public Expression root() {
		    return(mul(mat3(u_cam.ref()), mat3(u_wxf.ref()), objn.depref()));
		}
	    };

	prog.vctx.posv.mod(in -> mul(u_prj.ref(), eyev.depref()), 0);
    }

    public Expression plocxf(Expression v) {
	return(mul(u_wxf.ref(), v));
    }
    public Expression pcamxf(Expression v) {
	return(mul(u_cam.ref(), v));
    }
    public Expression pmvxf(Expression v) {
	return(pcamxf(plocxf(v)));
    }
    public Expression pprjxf(Expression v) {
	return(mul(u_prj.ref(), v));
    }

     /* If, at some unexpected point in an unexpected future, I were
      * to use anisotropic transforms, this will have to get a matrix
      * inverter implemented for it. */
    public Expression nlocxf(Expression v) {
	return(mul(mat3(u_wxf.ref()), v));
    }
    public Expression ncamxf(Expression v) {
	return(mul(mat3(u_cam.ref()), v));
    }
    public Expression nmvxf(Expression v) {
	return(ncamxf(nlocxf(v)));
    }

    public static Homo3D get(ProgramContext prog) {
	Homo3D ret = prog.getmod(Homo3D.class);
	if(ret == null)
	    ret = new Homo3D(prog);
	return(ret);
    }

    private static final ShaderMacro shader = prog -> {Homo3D.get(prog);};

    public static final State state = new State() {
	    public ShaderMacro shader() {return(shader);}
	    public void apply(Pipe p) {p.put(States.vxf, this);}
	};

    /* Optional derived values */
    public static final AutoVarying fragobjv = new AutoVarying(VEC3, "s_objv") {
	    protected Expression root(VertexContext vctx) {
		return(pick(get(vctx.prog).objv.depref(), "xyz"));
	    }
	};
    public static final AutoVarying fragmapv = new AutoVarying(VEC3, "s_mapv") {
	    protected Expression root(VertexContext vctx) {
		return(pick(get(vctx.prog).mapv.depref(), "xyz"));
	    }
	};
    public static final AutoVarying frageyev = new AutoVarying(VEC3, "s_eyev") {
	    protected Expression root(VertexContext vctx) {
		return(pick(get(vctx.prog).eyev.depref(), "xyz"));
	    }
	};
    private static final Object vertedir_id = new Object();
    public static Value vertedir(final VertexContext vctx) {
	return(vctx.mainvals.ext(vertedir_id, () ->
				 vctx.mainvals.new Value(VEC3, new Symbol.Gen("edir")) {
					 public Expression root() {
					     return(neg(normalize(pick(Homo3D.get(vctx.prog).eyev.depref(), "xyz"))));
					 }
				     }));
    }
    private static final Object fragedir_id = new Object();
    public static Value fragedir(final FragmentContext fctx) {
	return(fctx.mainvals.ext(fragedir_id, () ->
				 fctx.mainvals.new Value(VEC3, new Symbol.Gen("edir")) {
					 public Expression root() {
					     return(neg(normalize(frageyev.ref())));
					 }
				     }));
    }

    private static final AutoVarying frageyen = new AutoVarying(VEC3, "s_eyen") {
	    protected Expression root(VertexContext vctx) {
		return(get(vctx.prog).eyen.depref());
	    }
	};
    public static Value frageyen(FragmentContext fctx) {
	return(fctx.mainvals.ext(frageyen, () -> {
		    Value ret = fctx.mainvals.new Value(VEC3, new Symbol.Gen("eyen")) {
			    public Expression root() {
				return(frageyen.ref());
			    }
			};
		    ret.mod(in -> normalize(in), 0);
		    return(ret);
		}));
    }

    public static HomoCoord4f obj2clip(Coord3f objc, Pipe state) {
	HomoCoord4f c = new HomoCoord4f(objc);
	Location.Chain s_loc = state.get(loc);
	if(s_loc != null) c = s_loc.fin(Matrix4f.id).mul4(c);
	Camera s_cam = state.get(cam);
	if(s_cam != null) c = s_cam.fin(Matrix4f.id).mul4(c);
	Projection s_prj = state.get(prj);
	if(s_prj != null) c = s_prj.fin(Matrix4f.id).mul4(c);
	return(c);
    }

    public static Coord3f obj2view(Coord3f objc, Pipe state, Area view) {
	return(obj2clip(objc, state).toview(view));
    }

    public static Coord3f obj2view(Coord3f c, Pipe state) {
	return(obj2view(c, state, state.get(States.viewport).area));
    }
}
