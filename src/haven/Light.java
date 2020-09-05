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
import java.awt.Color;
import haven.render.*;
import haven.render.RenderList;
import haven.render.sl.ShaderMacro;
import haven.render.sl.Uniform;
import static haven.Utils.c2fa;

public abstract class Light implements RenderTree.Node {
    public float[] amb, dif, spc;
    public int prio;
    
    private static final float[] defamb = {0.0f, 0.0f, 0.0f, 1.0f};
    private static final float[] defdif = {1.0f, 1.0f, 1.0f, 1.0f};
    private static final float[] defspc = {1.0f, 1.0f, 1.0f, 1.0f};

    public Light() {
	this.amb = defamb;
	this.dif = defdif;
	this.spc = defspc;
    }
    
    public Light(FColor col) {
	this.amb = defamb;
	this.dif = this.spc = col.to4a();
    }

    public Light(Color col) {
	this.amb = defamb;
	this.dif = this.spc = c2fa(col);
    }

    public Light(FColor amb, FColor dif, FColor spc) {
	this.amb = amb.to4a();
	this.dif = dif.to4a();
	this.spc = spc.to4a();
    }

    public Light(Color amb, Color dif, Color spc) {
	this.amb = c2fa(amb);
	this.dif = c2fa(dif);
	this.spc = c2fa(spc);
    }

    public Light prio(int prio) {
	this.prio = prio;
	return(this);
    }

    public abstract Object[] params(GroupPipe state);

    public static final State.Slot<Lights> clights = new State.Slot<>(State.Slot.Type.SYS, Lights.class);
    public static final class Lights extends State {
	private final Object[][] lights;

	public Lights(Object[][] lights) {
	    this.lights = lights;
	}

	public ShaderMacro shader() {return(null);}

	public void apply(Pipe p) {p.put(clights, this);}
    }

    public static final State.Slot<LightList> lights = new State.Slot<>(State.Slot.Type.SYS, LightList.class);
    public static final class LightList extends State {
	public final List<RenderList.Slot<Light>> ll = new ArrayList<>();

	public Lights compile() {
	    Object[][] cl;
	    synchronized(ll) {
		cl = new Object[ll.size()][];
		for(int i = 0; i < cl.length; i++) {
		    cl[i] = ll.get(i).obj().params(ll.get(i).state());
		}
	    }
	    return(new Lights(cl));
	}

	public void add(RenderList.Slot<Light> light) {
	    synchronized(ll) {
		int i, p = light.obj().prio;
		for(i = 0; i < ll.size(); i++) {
		    if(ll.get(i).obj().prio <= p)
			break;
		}
		ll.add(i, light);
	    }
	}

	public void remove(RenderList.Slot<Light> light) {
	    synchronized(ll) {
		ll.remove(light);
	    }
	}

	public ShaderMacro shader() {return(null);}

	public void apply(Pipe p) {p.put(lights, this);}

	public int index(Light l) {
	    synchronized(ll) {
		for(int i = 0; i < ll.size(); i++) {
		    if(ll.get(i).obj() == l)
			return(i);
		}
		return(-1);
	    }
	}
    }

    public void added(RenderTree.Slot slot) {
	LightList ll = slot.state().get(lights);
	if(ll != null)
	    ll.add(slot.cast(Light.class));
    }

    public void removed(RenderTree.Slot slot) {
	LightList ll = slot.state().get(lights);
	if(ll != null)
	    ll.remove(slot.cast(Light.class));
    }

    public static final State.Slot<PhongLight> lighting = new State.Slot<>(State.Slot.Type.DRAW, PhongLight.class);

    @Material.ResName("col")
    public static class PhongLight extends State {
	private static final Uniform.Data<Object[]> getlights = new Uniform.Data<Object[]>(p -> {
		Lights l = p.get(clights);
		return((l == null) ? new Object[0][] : l.lights);
	    }, clights);
	public static final ShaderMacro vlight = prog -> new Phong(prog.vctx, getlights,
								   new Uniform.Data<>(p -> p.get(lighting).material, lighting));
	public static final ShaderMacro flight = prog -> new Phong(prog.fctx, getlights,
								   new Uniform.Data<>(p -> p.get(lighting).material, lighting));
	private final ShaderMacro shader;
	private final Object[] material;

	public static final FColor defamb = new FColor(0.2f, 0.2f, 0.2f);
	public static final FColor defdif = new FColor(0.8f, 0.8f, 0.8f);
	public static final FColor defspc = new FColor(0.0f, 0.0f, 0.0f);
	public static final FColor defemi = new FColor(0.0f, 0.0f, 0.0f);

	public PhongLight(boolean frag, FColor amb, FColor dif, FColor spc, FColor emi, float shine) {
	    this.shader = frag ? flight : vlight;
	    this.material = new Object[] {emi, amb, dif, spc, shine};
	}

	public PhongLight(boolean frag, Color amb, Color dif, Color spc, Color emi, float shine) {
	    this(frag, new FColor(amb), new FColor(dif), new FColor(spc), new FColor(emi), shine);
	}

	public PhongLight(boolean frag, FColor col) {
	    this(frag, col.mul(defamb), col.mul(defdif), FColor.BLACK, FColor.BLACK, 0.0f);
	}

	public PhongLight(boolean frag) {
	    this(frag, defamb, defdif, defspc, defemi, 0.0f);
	}

	public PhongLight(Resource res, Object... args) {
	    this(true, (Color)args[0], (Color)args[1], (Color)args[2], (Color)args[3], (Float)args[4]);
	}

	public ShaderMacro shader() {return(shader);}

	public void apply(Pipe p) {p.put(lighting, this);}
    }

    @Material.ResName("light")
    public static class $light implements Material.ResCons {
	public Pipe.Op cons(Resource res, Object... args) {
	    switch((String)args[0]) {
	    case "def": case "n":
		return(null);
	    default:
		throw(new RuntimeException(String.format("%s using non-default lighting", res.name)));
	    }
	}
    }

    public static class CelShade extends State {
	public static final Slot<CelShade> slot = new Slot<CelShade>(Slot.Type.DRAW, CelShade.class);

	public CelShade(boolean dif, boolean spc) {
	    shader = new Phong.CelShade(dif, spc);
	}

	private final ShaderMacro shader;
	public ShaderMacro shader() {
	    return(shader);
	}
	public void apply(Pipe p) {p.put(slot, this);}
    }

    public static final CelShade celshade = new CelShade(true, false);
    @Material.ResName("cel")
    public static class $cel implements Material.ResCons {
	public Pipe.Op cons(Resource res, Object... args) {
	    if(args.length < 1)
		return(celshade);
	    String s = (String)args[0];
	    boolean dif = (s.indexOf('d') >= 0);
	    boolean spc = (s.indexOf('s') >= 0);
	    return(new CelShade(dif, spc));
	}
    }
    
    @Resource.LayerName("light")
    public static class Res extends Resource.Layer {
	public final int id;
	public final Color amb, dif, spc;
	public boolean hatt, hexp;
	public float ac, al, aq, exp;
	public Coord3f dir;
	
	private static Color cold(Message buf) {
	    return(new Color((int)(buf.cpfloat() * 255.0),
			     (int)(buf.cpfloat() * 255.0),
			     (int)(buf.cpfloat() * 255.0),
			     (int)(buf.cpfloat() * 255.0)));
	}
	
	private static Color cold2(Message buf) {
	    return(new Color((int)(buf.float32() * 255.0),
			     (int)(buf.float32() * 255.0),
			     (int)(buf.float32() * 255.0),
			     (int)(buf.float32() * 255.0)));
	}

	public Res(Resource res, Message buf) {
	    res.super();
	    int ver = buf.uint8();
	    if(ver == 0) {
		this.id = buf.int8();
		this.amb = cold(buf);
		this.dif = cold(buf);
		this.spc = cold(buf);
		while(!buf.eom()) {
		    int t = buf.uint8();
		    if(t == 1) {
			hatt = true;
			ac = (float)buf.cpfloat();
			al = (float)buf.cpfloat();
			aq = (float)buf.cpfloat();
		    } else if(t == 2) {
			float x = (float)buf.cpfloat();
			float y = (float)buf.cpfloat();
			float z = (float)buf.cpfloat();
			dir = new Coord3f(x, y, z);
		    } else if(t == 3) {
			hexp = true;
			exp = (float)buf.cpfloat();
		    } else {
			throw(new Resource.LoadException("Unknown light data: " + t, getres()));
		    }
		}
	    } else if(ver == 1) {
		this.id = buf.int16();
		this.amb = cold2(buf);
		this.dif = cold2(buf);
		this.spc = cold2(buf);
		while(!buf.eom()) {
		    int t = buf.uint8();
		    if(t == 1) {
			hatt = true;
			ac = buf.float32();
			al = buf.float32();
			aq = buf.float32();
		    } else if(t == 2) {
			float x = buf.float32();
			float y = buf.float32();
			float z = buf.float32();
			dir = new Coord3f(x, y, z);
		    } else if(t == 3) {
			hexp = true;
			exp = buf.float32();
		    } else {
			throw(new Resource.LoadException("Unknown light data: " + t, getres()));
		    }
		}
	    } else {
		throw(new Resource.LoadException("Unknown light version: " + ver, getres()));
	    }
	}
	
	public Light make() {
	    if(hatt) {
		PosLight ret;
		if(hexp)
		    ret = new SpotLight(amb, dif, spc, Coord3f.o, dir, exp);
		else
		    ret = new PosLight(amb, dif, spc, Coord3f.o);
		ret.att(ac, al, aq);
		return(ret);
	    } else {
		return(new DirLight(amb, dif, spc, dir));
	    }
	}
	
	public void init() {
	}
    }
}
