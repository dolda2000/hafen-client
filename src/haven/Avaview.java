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
import haven.render.*;
import haven.Composited.Desc;
import haven.Composited.MD;
import haven.Composited.ED;

public class Avaview extends PView {
    public static final Tex missing = Resource.loadtex("gfx/hud/equip/missing");
    public static final Coord dasz = missing.sz();
    public Color color = Color.WHITE;
    public FColor clearcolor = FColor.BLACK;
    public long avagob;
    public Desc avadesc;
    public Resource.Resolver resmap = null;
    private Composited comp;
    private RenderTree.Slot compslot;
    private List<Composited.MD> cmod = null;
    private List<Composited.ED> cequ = null;
    private final String camnm;

    @RName("av")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    long avagob = -1;
	    Coord sz = dasz;
	    String camnm = "avacam";
	    if(args[0] != null)
		avagob = Utils.uint32((Integer)args[0]);
	    if((args.length > 1) && (args[1] != null))
		sz = UI.scale((Coord)args[1]);
	    if((args.length > 2) && (args[2] != null))
		camnm = (String)args[2];
	    return(new Avaview(sz, avagob, camnm));
	}
    }

    public Avaview(Coord sz, long avagob, String camnm) {
	super(sz);
	this.camnm = camnm;
	this.avagob = avagob;
	basic.add(new DirLight(Color.WHITE, Color.WHITE, Color.WHITE, new Coord3f(1, 1, 1).norm()), null);
	makeproj();
    }

    protected void makeproj() {
	float field = 0.5f;
	float aspect = ((float)sz.y) / ((float)sz.x);
	basic(Projection.class, Projection.frustum(-field, field, -aspect * field, aspect * field, 1, 5000));
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "upd") {
	    if(args[0] == null)
		this.avagob = -1;
	    else
		this.avagob = Utils.uint32((Integer)args[0]);
	    this.avadesc = null;
	} else if(msg == "col") {
	    this.color = (Color)args[0];
	} else if(msg == "pop") {
	    pop(Desc.decode(ui.sess, args));
	} else if(msg == "bg") {
	    clearcolor = new FColor((Color)args[0]);
	} else {
	    super.uimsg(msg, args);
	}
    }

    public void pop(Desc ava, Resource.Resolver resmap) {
	this.avadesc = ava;
	this.resmap = resmap;
	this.avagob = -1;
    }

    public void pop(Desc ava) {
	pop(ava, null);
    }

    private Collection<ResData> nposes = null, lposes = null;
    private boolean nposesold;
    public void chposes(Collection<ResData> poses, boolean interp) {
	nposes = poses;
	nposesold = !interp;
    }
    private void updposes() {
	if(nposes == null) {
	    nposes = lposes;
	    nposesold = true;
	}
    }

    private static final OwnerContext.ClassResolver<Avaview> ctxr = new OwnerContext.ClassResolver<Avaview>()
	.add(Glob.class, v -> v.ui.sess.glob)
	.add(Session.class, v -> v.ui.sess)
	.add(Resource.Resolver.class, v -> (v.resmap == null ? v.ui.sess : v.resmap));
    private class AvaOwner implements Sprite.Owner, Skeleton.ModOwner {
	public Random mkrandoom() {return(new Random());}
	public Resource getres() {return(null);}
	public <T> T context(Class<T> cl) {return(ctxr.context(cl, Avaview.this));}
	@Deprecated public Glob glob() {return(context(Glob.class));}

	public Coord3f getc() {return(Coord3f.o);}
	public double getv() {return(0);}
    }
    private final AvaOwner avaowner = new AvaOwner();

    private void initcomp(Composite gc) {
	if((comp == null) || (comp.skel != gc.comp.skel)) {
	    comp = new Composited(gc.comp.skel);
	    comp.eqowner = avaowner;
	    if(compslot != null) {
		compslot.remove();
		compslot = null;
	    }
	    updposes();
	}
    }

    private static Camera makecam(Resource base, Composited comp, String camnm) {
	Skeleton.BoneOffset bo = base.layer(Skeleton.BoneOffset.class, camnm);
	if(bo == null)
	    throw(new Loading());
	Pipe buf = new BufPipe();
	buf.prep(bo.forpose(comp.pose).get());
	return(new LocationCam(buf.get(Homo3D.loc)));
    }

    private Composite getgcomp() {
	Gob gob = ui.sess.glob.oc.getgob(avagob);
	if(gob == null)
	    return(null);
	Drawable d = gob.getattr(Drawable.class);
	if(!(d instanceof Composite))
	    return(null);
	Composite gc = (Composite)d;
	if(gc.comp == null)
	    return(null);
	return(gc);
    }

    private static List<MD> copy1(List<MD> in) {
	List<MD> ret = new ArrayList<>();
	for(MD ob : in)
	    ret.add(ob.clone());
	return(ret);
    }

    private static List<ED> copy2(List<ED> in) {
	List<ED> ret = new ArrayList<>();
	for(ED ob : in)
	    ret.add(ob.clone());
	return(ret);
    }

    private Indir<Resource> lbase = null;
    public void updcomp() {
	/* XXX: This "retry mechanism" is quite ugly and should
	 * probably be rewritten to use the Loader instead. */
	if(avagob != -1) {
	    Composite gc = getgcomp();
	    if(gc == null)
		throw(new Loading());
	    initcomp(gc);
	    if(gc.base != lbase)
		basic(Camera.class, makecam((lbase = gc.base).get(), comp, camnm));
	    if(gc.comp.cmod != this.cmod)
		comp.chmod(this.cmod = copy1(gc.comp.cmod));
	    if(gc.comp.cequ != this.cequ)
		comp.chequ(this.cequ = copy2(gc.comp.cequ));
	} else if(avadesc != null) {
	    Desc d = avadesc;
	    if((d.base != lbase) || (comp == null)) {
		lbase = d.base;
		comp = new Composited(d.base.get().layer(Skeleton.Res.class).s);
		comp.eqowner = avaowner;
		basic(Camera.class, makecam(d.base.get(), comp, camnm));
		updposes();
	    }
	    if(d.mod != this.cmod) {
		comp.chmod(d.mod);
		this.cmod = d.mod;
	    }
	    if(d.equ != this.cequ) {
		comp.chequ(d.equ);
		this.cequ = d.equ;
	    }
	}
	if(compslot == null) {
	    compslot = basic.add(comp);
	}
    }

    public void tick(double dt) {
	super.tick(dt);
	if(comp != null) {
	    comp.tick(dt);
	    if(nposes != null) {
		try {
		    Composited.Poses np = comp.new Poses(Composite.loadposes(nposes, avaowner, comp.skel, nposesold));
		    np.set(nposesold ? 0 : 0.2f);
		    lposes = nposes;
		    nposes = null;
		} catch(Loading e) {}
	    }
	}
    }

    protected FColor clearcolor() {
	return(clearcolor);
    }

    public void draw(GOut g) {
	boolean drawn = false;
	try {
	    if(avagob != -1) {
		Gob gob = ui.sess.glob.oc.getgob(avagob);
		if(gob != null) {
		    Avatar ava = gob.getattr(Avatar.class);
		    if(ava != null) {
			List<Resource.Image> imgs = ava.images();
			if(imgs != null) {
			    for(Resource.Image img : imgs) {
				g.image(img.tex(), Coord.z, this.sz);
			    }
			    drawn = true;
			}
		    }
		}
	    }
	} catch(Loading e) {
	}
	if(!drawn) {
	    try {
		updcomp();
		super.draw(g);
	    } catch(Loading e) {
		g.image(missing, Coord.z, sz);
	    }
	}
	if(color != null) {
	    g.chcolor(color);
	    Window.wbox.draw(g, Coord.z, sz);
	}
    }

    public boolean mousedown(Coord c, int button) {
	if(canactivate) {
	    wdgmsg("click", button);
	    return(true);
	}
	return(super.mousedown(c, button));
    }
}
