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
import haven.render.States;	// XXXRENDRM

public class Material implements Pipe.Op {
    public final Pipe.Op[] states;

    public static final Pipe.Op nofacecull = (p -> p.put(States.facecull, null));
    @ResName("nofacecull")
    public static class $nofacecull implements ResCons {
	public Pipe.Op cons(Resource res, Object... args) {return(nofacecull);}
    }

    @ResName("order")
    public static class $order implements ResCons {
	public Pipe.Op cons(Resource res, Object... args) {
	    String nm = (String)args[0];
	    if(nm.equals("first")) {
		return(Rendered.first);
	    } else if(nm.equals("last")) {
		return(Rendered.last);
	    } else if(nm.equals("pfx")) {
		return(Rendered.postpfx);
	    /* XXXRENDER
	    } else if(nm.equals("eye")) {
		return(Rendered.eyesort);
	    } else if(nm.equals("earlyeye")) {
		return(Rendered.eeyesort);
	    */
	    } else if(nm.equals("premap")) {
		return(MapMesh.premap);
	    } else if(nm.equals("postmap")) {
		return(MapMesh.postmap);
	    } else {
		/* XXXRENDER
		throw(new Resource.LoadException("Unknown draw order: " + nm, res));
		*/
		return(null);
	    }
	}
    }

    public Material(Pipe.Op... states) {
	this.states = states;
    }

    public String toString() {
	return(Arrays.asList(states).toString());
    }

    public void apply(Pipe p) {
	for(Pipe.Op op : states)
	    op.apply(p);
    }

    public interface Owner extends OwnerContext {
    }

    @Resource.PublishedCode(name = "mat")
    public static interface Factory {
	public default Material create(Owner owner, Resource res, Message sdt) {
	    try {
		return(create(owner.context(Glob.class), res, sdt));
	    } catch(OwnerContext.NoContext e) {
		return(create((Glob)null, res, sdt));
	    }
	}
	@Deprecated
	public default Material create(Glob glob, Resource res, Message sdt) {
	    throw(new AbstractMethodError("material factory missing either create method"));
	}
    }

    public static Material fromres(Owner owner, Resource res, Message sdt) {
	Factory f = res.getcode(Factory.class, false);
	if(f != null) {
	    return(f.create(owner, res, sdt));
	}
	return(res.layer(Material.Res.class).get());
    }

    private static class LegacyOwner implements Owner {
	final Glob glob;
	LegacyOwner(Glob glob) {this.glob = glob;}

	private static final ClassResolver<LegacyOwner> ctxr = new ClassResolver<LegacyOwner>()
	    .add(Glob.class, o -> o.glob)
	    .add(Session.class, o -> o.glob.sess);
	public <T> T context(Class<T> cl) {return(ctxr.context(cl, this));}
    }
    @Deprecated
    public static Material fromres(Glob glob, Resource res, Message sdt) {
	return(fromres(new LegacyOwner(glob), res, sdt));
    }

    public static class Res extends Resource.Layer implements Resource.IDLayer<Integer> {
	public final int id;
	private transient List<Pipe.Op> states = new LinkedList<>();
	private transient List<Resolver> left = new LinkedList<>();
	private transient Material m;

	public interface Resolver {
	    public void resolve(Collection<Pipe.Op> buf);
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
			r.resolve(states);
			i.remove();
		    }
		    m = new Material(states.toArray(new Pipe.Op[0])) {
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
	    final Indir<Resource> lres = res.pool.load((String)args[0], (Integer)args[1]);
	    final int id = (args.length > 2)?(Integer)args[2]:-1;
	    return(new Res.Resolver() {
		    public void resolve(Collection<Pipe.Op> buf) {
			if(id >= 0) {
			    Res mat = lres.get().layer(Res.class, id);
			    if(mat == null)
				throw(new Resource.LoadException("No such material in " + lres.get() + ": " + id, res));
			    buf.add(mat.get());
			} else {
			    Res mat = lres.get().layer(Res.class);
			    if(mat == null)
				throw(new Resource.LoadException("No material in " + lres.get(), res));
			    buf.add(mat.get());
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
		try {
		    scons = cl.asSubclass(ResCons.class).newInstance();
		} catch(InstantiationException e) {
		    throw(new Error(e));
		} catch(IllegalAccessException e) {
		    throw(new Error(e));
		}
		rnames.put(nm, new ResCons2() {
			public Res.Resolver cons(Resource res, Object... args) {
			    final Pipe.Op ret = scons.cons(res, args);
			    return(new Res.Resolver() {
				    public void resolve(Collection<Pipe.Op> buf) {
					if(ret != null)
					    buf.add(ret);
				    }
				});
			}
		    });
	    } else if(ResCons2.class.isAssignableFrom(cl)) {
		try {
		    rnames.put(nm, cl.asSubclass(ResCons2.class).newInstance());
		} catch(InstantiationException e) {
		    throw(new Error(e));
		} catch(IllegalAccessException e) {
		    throw(new Error(e));
		}
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
				    public void resolve(Collection<Pipe.Op> buf) {
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
		/* XXXRENDER
		if(cons == null)
		    throw(new Resource.LoadException("Unknown material part name: " + nm, res));
		*/
		if(cons != null)
		    ret.left.add(cons.cons(res, args));
	    }
	    return(ret);
	}
    }
}
