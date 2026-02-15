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

import java.awt.Color;
import java.util.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import haven.render.*;

public class Material implements Pipe.Op {
    public final Pipe.Op states, dynstates;

    public static final Pipe.Op nofacecull = (p -> p.put(States.facecull, null));
    @SpecName("nofacecull")
    public static class $nofacecull implements Spec {
	public void cons(Buffer buf, Object... args) {buf.states.add(nofacecull);}
    }

    @SpecName("maskcol")
    public static class $maskcol implements Spec {
	final Pipe.Op mask = p -> p.put(FragColor.slot, null);
	public void cons(Buffer buf, Object... args) {
	    buf.states.add(mask);
	}
    }

    @SpecName("maskdepth")
    public static class $maskdepth implements Spec {
	public void cons(Buffer buf, Object... args) {
	    buf.states.add(States.maskdepth);
	}
    }

    @SpecName("vcol")
    public static class $vcol implements Spec {
	public void cons(Buffer buf, Object... args) {
	    buf.states.add(new BaseColor((Color)args[0]));
	}
    }

    @SpecName("blend")
    public static class $blend implements Spec {
	private static BlendMode.Function fn(Resource res, char desc) {
	    switch(desc) {
	    case '+': return BlendMode.Function.ADD;
	    case '-': return BlendMode.Function.SUB;
	    case '_': return BlendMode.Function.RSUB;
	    case '>': return BlendMode.Function.MAX;
	    case '<': return BlendMode.Function.MIN;
	    default: throw(new Resource.UnknownFormatException(res, "blend function", desc));
	    }
	}

	private static BlendMode.Factor fac(Resource res, char desc) {
	    switch(desc) {
	    case '0': return BlendMode.Factor.ZERO;
	    case '1': return BlendMode.Factor.ONE;
	    case 'a': return BlendMode.Factor.SRC_ALPHA;
	    case 'A': return BlendMode.Factor.INV_SRC_ALPHA;
	    case 'c': return BlendMode.Factor.SRC_COLOR;
	    case 'C': return BlendMode.Factor.INV_SRC_COLOR;
	    default: throw(new Resource.UnknownFormatException(res, "blend factor", desc));
	    }
	}

	public void cons(Buffer buf, Object... args) {
	    BlendMode.Function cfn, afn;
	    BlendMode.Factor csrc, cdst, asrc, adst;
	    String desc = Utils.sv(args[0]);
	    if(desc.length() < 3)
		throw(new Resource.UnknownFormatException(buf.res, "blend description", desc));
	    cfn = fn(buf.res, desc.charAt(0));
	    csrc = fac(buf.res, desc.charAt(1));
	    cdst = fac(buf.res, desc.charAt(2));
	    if(desc.length() < 6) {
		afn = cfn; asrc = csrc; adst = cdst;
	    } else {
		afn = fn(buf.res, desc.charAt(3));
		asrc = fac(buf.res, desc.charAt(4));
		adst = fac(buf.res, desc.charAt(5));
	    }
	    buf.states.add(FragColor.blend(new BlendMode(cfn, csrc, cdst, afn, asrc, adst)));
	}
    }

    @SpecName("order")
    public static class $order implements Spec {
	public void cons(Buffer buf, Object... args) {
	    String nm = Utils.sv(args[0]);
	    if(nm.equals("first")) {
		buf.states.add(Rendered.first);
	    } else if(nm.equals("last")) {
		buf.states.add(Rendered.last);
	    } else if(nm.equals("def")) {
		buf.states.add(Rendered.deflt);
	    } else if(nm.equals("pfx")) {
		buf.states.add(Rendered.postpfx);
	    } else if(nm.equals("eye")) {
		buf.states.add(Rendered.eyesort);
	    } else if(nm.equals("earlyeye")) {
		buf.states.add(Rendered.eeyesort);
	    } else if(nm.equals("premap")) {
		buf.states.add(MapMesh.premap);
	    } else if(nm.equals("postmap")) {
		buf.states.add(MapMesh.postmap);
	    } else {
		throw(new Resource.UnknownFormatException(buf.res, "draw order", nm));
	    }
	}
    }

    public Material(Pipe.Op[] states, Pipe.Op[] dynstates) {
	this.states = Pipe.Op.compose(states);
	this.dynstates = Pipe.Op.compose(dynstates);
    }

    public Material(Pipe.Op... states) {
	this(states, new Pipe.Op[0]);
    }

    public String toString() {
	return(Arrays.asList(states, dynstates).toString());
    }

    public void apply(Pipe p) {
	states.apply(p);
	dynstates.apply(p);
    }

    /* This is actually an interesting inflection point. Right now,
     * Material overriding apply() seems more like an ugly hack to
     * support dynamic vs. static states. However, allowing materials
     * to more truly own their own wrapping slots opens up such
     * possibilities as them adding their children multiple times for
     * more complex rendering techniques, which might be quite useful,
     * and is well worth considering converting more fully to. In that
     * case, materials probably shouldn't even be pipe-ops at all, but
     * rather having apply() as their main and only interface. */
    public Wrapping apply(RenderTree.Node r) {
	if(dynstates != Pipe.Op.nil) {
	    if(states == Pipe.Op.nil)
		return(dynstates.apply(r, false));
	    r = dynstates.apply(r, false);
	}
	return(states.apply(r, true));
    }

    public interface Owner extends OwnerContext {
    }

    @Resource.PublishedCode(name = "mat")
    public static interface Factory {
	public Material create(Owner owner, Resource res, Message sdt);
    }

    public static Material fromres(Owner owner, Resource res, Message sdt) {
	Factory f = res.getcode(Factory.class, false);
	if(f != null) {
	    return(f.create(owner, res, sdt));
	}
	Res mat = res.layer(Material.Res.class);
	if(mat == null)
	    return(null);
	return(mat.get());
    }

    public static class Buffer {
	public final Resource res;
	public final List<Pipe.Op> states, dynstates;
	private final List<Object[]> left;

	public Buffer(Resource res, List<Object[]> specs) {
	    this.res = res;
	    this.left = new LinkedList<>(specs);
	    this.states = new ArrayList<>(left.size());
	    this.dynstates = new ArrayList<>(left.size());
	}
    }

    public static interface Spec {
	public void cons(Buffer buf, Object... args);
    }

    public static class Res extends Resource.Layer implements Resource.IDLayer<Integer> {
	public final int id;
	private transient Material m;
	private transient Buffer cons;

	public Res(Resource res, int id, List<Object[]> specs) {
	    res.super();
	    this.id = id;
	    this.cons = new Buffer(res, specs);
	}

	public Material get() {
	    if(m == null) {
		synchronized(this) {
		    if(m == null) {
			for(Iterator<Object[]> i = cons.left.iterator(); i.hasNext();) {
			    Object[] spec = i.next();
			    String nm = Utils.sv(spec[0]);
			    Spec part = rnames.get(nm);
			    if(part == null)
				Warning.warn("unknown material part name in %s: %s", cons.res.name, nm);
			    else
				part.cons(cons, Utils.splice(spec, 1));
			    i.remove();
			}
			m = new Material(cons.states.toArray(new Pipe.Op[0]), cons.dynstates.toArray(new Pipe.Op[0])) {
				public String toString() {
				    return(super.toString() + "@" + getres().name);
				}
			    };
			cons = null;
		    }
		}
	    }
	    return(m);
	}

	public void init() {}

	public Integer layerid() {
	    return(id);
	}
    }

    @SpecName("mlink")
    public static class $mlink implements Spec {
	public void cons(Buffer buf, Object... args) {
	    KeywordArgs desc = new KeywordArgs(args, buf.res.pool, "?@res", "id");
	    Indir<Resource> lres = Utils.irv(desc.get("res", buf.res.indir()));
	    int id = Utils.iv(desc.get("id", -1));
	    Material linked;
	    if(id >= 0) {
		Res mat = lres.get().layer(Res.class, id);
		if(mat == null)
		    throw(new Resource.LoadException("No such material in " + lres.get() + ": " + id, buf.res));
		linked = mat.get();
	    } else {
		linked = fromres((Owner)null, lres.get(), Message.nil);
		if(linked == null)
		    throw(new Resource.LoadException("No material in " + lres.get(), buf.res));
	    }
	    if(linked.states != Pipe.Op.nil)
		buf.states.add(linked.states);
	    if(linked.dynstates != Pipe.Op.nil)
		buf.dynstates.add(linked.dynstates);
	}
    }

    @dolda.jglob.Discoverable
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface SpecName {
	public String value();
    }

    private static final Map<String, Spec> rnames = new HashMap<>();
 
    static {
	for(Class<?> cl : dolda.jglob.Loader.get(SpecName.class).classes()) {
	    String nm = cl.getAnnotation(SpecName.class).value();
	    if(Spec.class.isAssignableFrom(cl)) {
		rnames.put(nm, Utils.construct(cl.asSubclass(Spec.class)));
		/*
	    } else if(Pipe.Op.class.isAssignableFrom(cl)) {
		Constructor<? extends Pipe.Op> cons;
		try {
		    cons = cl.asSubclass(Pipe.Op.class).getConstructor(Resource.class, Object[].class);
		} catch(NoSuchMethodException e) {
		    throw(new Error("No proper constructor for res-consable GL state " + cl.getName(), e));
		}
		rnames.put(nm, new ResCons2() {
			public Res.Resolver cons(Resource res, Object... args) {
			    return(new Res.Resolver() {
				    public void resolve(Collection<Pipe.Op> buf, Collection<Pipe.Op> dynbuf) {
					buf.add(Utils.construct(cons, res, args));
				    }
				});
			}
		    });
		*/
	    } else {
		throw(new Error("Illegal material constructor class: " + cl));
	    }
	}
    }

    @Resource.LayerName("mat2")
    public static class NewMat implements Resource.LayerFactory<Res> {
	public Res cons(Resource res, Message buf) {
	    int id = buf.uint16();
	    List<Object[]> specs = new ArrayList<>();
	    while(!buf.eom()) {
		String nm = buf.string();
		Object[] args = buf.list(new Resource.PoolMapper(res.pool));
		specs.add(Utils.extend(new Object[] {nm}, args));
	    }
	    return(new Res(res, id, specs));
	}
    }
}
