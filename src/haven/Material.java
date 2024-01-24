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
    @ResName("nofacecull")
    public static class $nofacecull implements ResCons {
	public Pipe.Op cons(Resource res, Object... args) {return(nofacecull);}
    }

    @ResName("maskcol")
    public static class $maskcol implements ResCons {
	final Pipe.Op mask = p -> p.put(FragColor.slot, null);
	public Pipe.Op cons(Resource res, Object... args) {
	    return(mask);
	}
    }

    @ResName("maskdepth")
    public static class $maskdepth implements ResCons {
	public Pipe.Op cons(Resource res, Object... args) {
	    return(States.maskdepth);
	}
    }

    @Material.ResName("vcol")
    public static class $vcol implements Material.ResCons {
	public Pipe.Op cons(Resource res, Object... args) {
	    return(new BaseColor((Color)args[0]));
	}
    }

    @ResName("blend")
    public static class $blend implements ResCons {
	private static BlendMode.Function fn(Resource res, char desc) {
	    switch(desc) {
	    case '+': return BlendMode.Function.ADD;
	    case '-': return BlendMode.Function.SUB;
	    case '_': return BlendMode.Function.RSUB;
	    case '>': return BlendMode.Function.MAX;
	    case '<': return BlendMode.Function.MIN;
	    default: throw(new Resource.LoadException("Unknown blend function: " + desc, res));
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
	    default: throw(new Resource.LoadException("Unknown blend factor: " + desc, res));
	    }
	}

	public Pipe.Op cons(Resource res, Object... args) {
	    BlendMode.Function cfn, afn;
	    BlendMode.Factor csrc, cdst, asrc, adst;
	    String desc = (String)args[0];
	    if(desc.length() < 3)
		throw(new Resource.LoadException("Bad blend description: " + desc, res));
	    cfn = fn(res, desc.charAt(0));
	    csrc = fac(res, desc.charAt(1));
	    cdst = fac(res, desc.charAt(2));
	    if(desc.length() < 6) {
		afn = cfn; asrc = csrc; adst = cdst;
	    } else {
		afn = fn(res, desc.charAt(3));
		asrc = fac(res, desc.charAt(4));
		adst = fac(res, desc.charAt(5));
	    }
	    return(FragColor.blend(new BlendMode(cfn, csrc, cdst, afn, asrc, adst)));
	}
    }

    @ResName("order")
    public static class $order implements ResCons {
	public Pipe.Op cons(Resource res, Object... args) {
	    String nm = (String)args[0];
	    if(nm.equals("first")) {
		return(Rendered.first);
	    } else if(nm.equals("last")) {
		return(Rendered.last);
	    } else if(nm.equals("def")) {
		return(Rendered.deflt);
	    } else if(nm.equals("pfx")) {
		return(Rendered.postpfx);
	    } else if(nm.equals("eye")) {
		return(Rendered.eyesort);
	    } else if(nm.equals("earlyeye")) {
		return(Rendered.eeyesort);
	    } else if(nm.equals("premap")) {
		return(MapMesh.premap);
	    } else if(nm.equals("postmap")) {
		return(MapMesh.postmap);
	    } else {
		throw(new Resource.LoadException("Unknown draw order: " + nm, res));
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

    private static class LegacyOwner implements Owner {
	final Glob glob;
	LegacyOwner(Glob glob) {this.glob = glob;}

	private static final ClassResolver<LegacyOwner> ctxr = new ClassResolver<LegacyOwner>()
	    .add(Glob.class, o -> o.glob)
	    .add(Session.class, o -> o.glob.sess);
	public <T> T context(Class<T> cl) {return(ctxr.context(cl, this));}
    }

    public static class Res extends Resource.Layer implements Resource.IDLayer<Integer> {
	public final int id;
	private transient List<Pipe.Op> states = new LinkedList<>(), dynstates = new LinkedList<>();
	private transient List<Resolver> left = new LinkedList<>();
	private transient Material m;

	public interface Resolver {
	    public void resolve(Collection<Pipe.Op> buf, Collection<Pipe.Op> dynbuf);
	}

	public Res(Resource res, int id) {
	    res.super();
	    this.id = id;
	}

	public Material get() {
	    synchronized(this) {
		if(m == null) {
		    for(Iterator<Resolver> i = left.iterator(); i.hasNext();) {
			Resolver r = i.next();
			r.resolve(states, dynstates);
			i.remove();
		    }
		    m = new Material(states.toArray(new Pipe.Op[0]), dynstates.toArray(new Pipe.Op[0])) {
			    public String toString() {
				return(super.toString() + "@" + getres().name);
			    }
			};
		}
		return(m);
	    }
	}

	public void init() {}

	public Integer layerid() {
	    return(id);
	}
    }

    @ResName("mlink")
    public static class $mlink implements ResCons2 {
	public Res.Resolver cons(final Resource res, Object... args) {
	    final Indir<Resource> lres;
	    final int id;
	    if(args[0] instanceof String) {
		lres = res.pool.load((String)args[0], (Integer)args[1]);
		id = (args.length > 2)?(Integer)args[2]:-1;
	    } else {
		lres = res.indir();
		id = (Integer)args[0];
	    }
	    return(new Res.Resolver() {
		    public void resolve(Collection<Pipe.Op> buf, Collection<Pipe.Op> dynbuf) {
			if(id >= 0) {
			    Res mat = lres.get().layer(Res.class, id);
			    if(mat == null)
				throw(new Resource.LoadException("No such material in " + lres.get() + ": " + id, res));
			    Material m = mat.get();
			    if(m.states != Pipe.Op.nil)
				buf.add(m.states);
			    if(m.dynstates != Pipe.Op.nil)
				dynbuf.add(m.dynstates);
			} else {
			    Material mat = fromres((Owner)null, lres.get(), Message.nil);
			    if(mat == null)
				throw(new Resource.LoadException("No material in " + lres.get(), res));
			    if(mat.states != Pipe.Op.nil)
				buf.add(mat.states);
			    if(mat.dynstates != Pipe.Op.nil)
				dynbuf.add(mat.dynstates);
			}
		    }
		});
	}
    }

    @dolda.jglob.Discoverable
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ResName {
	public String value();
    }

    public interface ResCons {
	public Pipe.Op cons(Resource res, Object... args);
    }

    public interface ResCons2 {
	public Res.Resolver cons(Resource res, Object... args);
    }

    private static final Map<String, ResCons2> rnames = new TreeMap<String, ResCons2>();

    static {
	for(Class<?> cl : dolda.jglob.Loader.get(ResName.class).classes()) {
	    String nm = cl.getAnnotation(ResName.class).value();
	    if(ResCons.class.isAssignableFrom(cl)) {
		final ResCons scons;
		scons = Utils.construct(cl.asSubclass(ResCons.class));
		rnames.put(nm, new ResCons2() {
			public Res.Resolver cons(Resource res, Object... args) {
			    final Pipe.Op ret = scons.cons(res, args);
			    return(new Res.Resolver() {
				    public void resolve(Collection<Pipe.Op> buf, Collection<Pipe.Op> dynbuf) {
					if(ret != null)
					    buf.add(ret);
				    }
				});
			}
		    });
	    } else if(ResCons2.class.isAssignableFrom(cl)) {
		rnames.put(nm, Utils.construct(cl.asSubclass(ResCons2.class)));
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
	    } else {
		throw(new Error("Illegal material constructor class: " + cl));
	    }
	}
    }

    @Resource.LayerName("mat2")
    public static class NewMat implements Resource.LayerFactory<Res> {
	public Res cons(Resource res, Message buf) {
	    int id = buf.uint16();
	    Res ret = new Res(res, id);
	    while(!buf.eom()) {
		String nm = buf.string();
		Object[] args = buf.list();
		ResCons2 cons = rnames.get(nm);
		if(cons != null)
		    ret.left.add(cons.cons(res, args));
		else
		    new Resource.LoadWarning(res, "unknown material part name in %s: %s", res.name, nm).issue();
	    }
	    return(ret);
	}
    }
}
