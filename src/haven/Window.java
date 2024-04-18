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

import haven.render.*;
import java.util.function.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import static haven.PUtils.*;

public class Window extends Widget implements DTarget {
    public static final Pipe.Op bgblend = FragColor.blend.nil;
    public static final Pipe.Op cblend  = FragColor.blend(new BlendMode(BlendMode.Function.ADD, BlendMode.Factor.SRC_ALPHA, BlendMode.Factor.INV_SRC_ALPHA,
									BlendMode.Function.ADD, BlendMode.Factor.ONE, BlendMode.Factor.INV_SRC_ALPHA));
    public static final Tex bg = Resource.loadtex("gfx/hud/wnd/lg/bg");
    public static final Tex bgl = Resource.loadtex("gfx/hud/wnd/lg/bgl");
    public static final Tex bgr = Resource.loadtex("gfx/hud/wnd/lg/bgr");
    public static final Tex cl = Resource.loadtex("gfx/hud/wnd/lg/cl");
    public static final TexI cm = new TexI(Resource.loadsimg("gfx/hud/wnd/lg/cm"));
    public static final Tex cr = Resource.loadtex("gfx/hud/wnd/lg/cr");
    public static final Tex tm = Resource.loadtex("gfx/hud/wnd/lg/tm");
    public static final Tex tr = Resource.loadtex("gfx/hud/wnd/lg/tr");
    public static final Tex lm = Resource.loadtex("gfx/hud/wnd/lg/lm");
    public static final Tex lb = Resource.loadtex("gfx/hud/wnd/lg/lb");
    public static final Tex rm = Resource.loadtex("gfx/hud/wnd/lg/rm");
    public static final Tex bl = Resource.loadtex("gfx/hud/wnd/lg/bl");
    public static final Tex bm = Resource.loadtex("gfx/hud/wnd/lg/bm");
    public static final Tex br = Resource.loadtex("gfx/hud/wnd/lg/br");
    public static final Tex sizer = Resource.loadtex("gfx/hud/wnd/sizer");
    public static final Coord tlm = UI.scale(18, 30);
    public static final Coord brm = UI.scale(13, 22);
    public static final Coord cpo = UI.rscale(36, 16.4);
    public static final int capo = 7, capio = 2;
    public static final Coord dlmrgn = UI.scale(23, 14);
    public static final Coord dsmrgn = UI.scale(9, 9);
    public static final BufferedImage ctex = Resource.loadsimg("gfx/hud/fonttex");
    public static final Text.Furnace cf = new Text.Imager(new PUtils.TexFurn(new Text.Foundry(Text.fraktur, 15).aa(true), ctex)) {
	    protected BufferedImage proc(Text text) {
		// return(rasterimg(blurmask2(text.img.getRaster(), 1, 1, Color.BLACK)));
		return(rasterimg(blurmask2(text.img.getRaster(), UI.rscale(0.75), UI.rscale(1.0), Color.BLACK)));
	    }
	};
    public static final IBox wbox = new IBox("gfx/hud/wnd", "tl", "tr", "bl", "br", "extvl", "extvr", "extht", "exthb") {
	    final Coord co = UI.scale(3, 3), bo = UI.scale(2, 2);

	    public Coord btloff() {return(super.btloff().sub(bo));}
	    public Coord ctloff() {return(super.ctloff().sub(co));}
	    public Coord bbroff() {return(super.bbroff().sub(bo));}
	    public Coord cbroff() {return(super.cbroff().sub(co));}
	    public Coord bisz() {return(super.bisz().sub(bo.mul(2)));}
	    public Coord cisz() {return(super.cisz().sub(co.mul(2)));}
	};
    private static final BufferedImage[] cbtni = new BufferedImage[] {
	Resource.loadsimg("gfx/hud/wnd/lg/cbtnu"),
	Resource.loadsimg("gfx/hud/wnd/lg/cbtnd"),
	Resource.loadsimg("gfx/hud/wnd/lg/cbtnh")};
    public Deco deco;
    public boolean dt = false;
    public String cap;
    public TexRaw gbuf = null;
    private FragColor gout;
    private Pipe.Op gbasic;
    private UI.Grab dm = null;
    private Coord doff;
    public boolean decohide = false;
    public boolean large = false;

    @RName("wnd")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    Coord sz = UI.scale((Coord)args[0]);
	    String cap = (args.length > 1) ? (String)args[1] : null;
	    boolean lg = (args.length > 2) ? Utils.bv(args[2]) : false;
	    return(new Window(sz, cap, lg));
	}
    }

    private Window(Coord sz, String cap, boolean lg, Deco deco, boolean defdeco) {
	super(sz);
	this.cap = cap;
	this.large = lg;
	setfocustab(true);
	chdeco(defdeco ? makedeco() : deco);
    }

    public Window(Coord sz, String cap, boolean lg, Deco deco) {
	this(sz, cap, lg, deco, false);
    }

    public Window(Coord sz, String cap, boolean lg) {
	this(sz, cap, lg, null, true);
    }

    public Window(Coord sz, String cap) {
	this(sz, cap, false);
    }

    protected Deco makedeco() {
	return(new DefaultDeco(this.large));
    }

    protected void added() {
	super.added();
	parent.setfocus(this);
	initanim();
    }

    public void chcap(String cap) {
	this.cap = cap;
    }

    public void chdeco(Deco deco) {
	Coord psz, poff;
	if(this.deco != null) {
	    Area ca = this.deco.contarea();
	    psz = ca.sz();
	    poff = ca.ul;
	    this.deco.reqdestroy();
	    this.deco = null;
	} else {
	    psz = this.sz;
	    poff = Coord.z;
	}
	if(deco != null)
	    this.deco = add(deco);
	resize2(psz);
	Coord coff = poff.sub(ca().ul);
	this.c = this.c.add(coff);
	if(dm != null)
	    this.doff = this.doff.sub(coff);
    }

    public static abstract class Deco extends Widget {
	public Deco() {
	    z(-100);
	}

	public abstract void iresize(Coord isz);
	public abstract Area contarea();
    }

    public abstract static class DragDeco extends Deco {
	public boolean mousedown(Coord c, int button) {
	    if(super.mousedown(c, button))
		return(true);
	    if(checkhit(c)) {
		Window wnd = (Window)parent;
		wnd.parent.setfocus(wnd);
		wnd.raise();
		if(button == 1)
		    wnd.drag(c);
		return(true);
	    }
	    return(false);
	}
    }

    public static class DefaultDeco extends DragDeco {
	public final boolean lg;
	public final IButton cbtn;
	public boolean dragsize;
	public Area aa, ca;
	public Coord cptl = Coord.z, cpsz = Coord.z;
	public int cmw;
	public Text cap = null;

	public DefaultDeco(boolean lg) {
	    this.lg = lg;
	    cbtn = add(new IButton(cbtni[0], cbtni[1], cbtni[2])).action(() -> ((Window)parent).reqclose());
	}
	public DefaultDeco() {this(false);}

	public DefaultDeco dragsize(boolean v) {
	    this.dragsize = v;
	    return(this);
	}

	public void iresize(Coord isz) {
	    Coord mrgn = lg ? dlmrgn : dsmrgn;
	    Coord asz = isz;
	    Coord csz = asz.add(mrgn.mul(2));
	    Coord wsz = csz.add(tlm).add(brm);
	    resize(wsz);
	    ca = Area.sized(tlm, csz);
	    aa = Area.sized(ca.ul.add(mrgn), asz);
	    cbtn.c = Coord.of(sz.x - cbtn.sz.x, 0);
	}

	public Area contarea() {
	    return(aa);
	}

	protected void cdraw(GOut g) {
	    ((Window)parent).cdraw(g);
	}

	protected void drawbg(GOut g) {
	    g.usestate(bgblend);
	    Coord bgc = new Coord();
	    for(bgc.y = ca.ul.y; bgc.y < ca.br.y; bgc.y += bg.sz().y) {
		for(bgc.x = ca.ul.x; bgc.x < ca.br.x; bgc.x += bg.sz().x)
		    g.image(bg, bgc, ca.ul, ca.br);
	    }
	    g.defstate();
	    bgc.x = ca.ul.x;
	    for(bgc.y = ca.ul.y; bgc.y < ca.br.y; bgc.y += bgl.sz().y)
		g.image(bgl, bgc, ca.ul, ca.br);
	    bgc.x = ca.br.x - bgr.sz().x;
	    for(bgc.y = ca.ul.y; bgc.y < ca.br.y; bgc.y += bgr.sz().y)
		g.image(bgr, bgc, ca.ul, ca.br);
	}

	protected void drawframe(GOut g) {
	    Window wnd = (Window)parent;
	    if((cap == null) || (cap.text != wnd.cap)) {
		cap = (wnd.cap == null) ? null : cf.render(wnd.cap);
		cmw = (cap == null) ? 0 : cap.sz().x;
		cmw = Math.max(cmw, this.sz.x / 4);
		cptl = Coord.of(ca.ul.x, 0);
		cpsz = Coord.of(cpo.x + cmw, cm.sz().y).sub(cptl);
		cmw = cmw - (cl.sz().x - cpo.x) - UI.scale(5);
	    }
	    if(dragsize)
		g.image(sizer, ca.br.sub(sizer.sz()));
	    Coord mdo, cbr;
	    g.image(cl, Coord.z);
	    mdo = Coord.of(cl.sz().x, 0);
	    cbr = mdo.add(cmw, cm.sz().y);
	    for(int x = 0; x < cmw; x += cm.sz().x)
		g.image(cm, mdo.add(x, 0), Coord.z, cbr);
	    g.image(cr, Coord.of(cl.sz().x + cmw, 0));
	    g.image(cap.tex(), cpo);
	    mdo = Coord.of(cl.sz().x + cmw + cr.sz().x, 0);
	    cbr = Coord.of(sz.x - tr.sz().x, tm.sz().y);
	    for(; mdo.x < cbr.x; mdo.x += tm.sz().x)
		g.image(tm, mdo, Coord.z, cbr);
	    g.image(tr, Coord.of(sz.x - tr.sz().x, 0));

	    mdo = Coord.of(0, cl.sz().y);
	    cbr = Coord.of(lm.sz().x, sz.y - bl.sz().y);
	    if(cbr.y - mdo.y >= lb.sz().y) {
		cbr.y -= lb.sz().y;
		g.image(lb, Coord.of(0, cbr.y));
	    }
	    for(; mdo.y < cbr.y; mdo.y += lm.sz().y)
		g.image(lm, mdo, Coord.z, cbr);

	    mdo = Coord.of(sz.x - rm.sz().x, tr.sz().y);
	    cbr = Coord.of(sz.x, sz.y - br.sz().y);
	    for(; mdo.y < cbr.y; mdo.y += rm.sz().y)
		g.image(rm, mdo, Coord.z, cbr);

	    g.image(bl, Coord.of(0, sz.y - bl.sz().y));
	    mdo = Coord.of(bl.sz().x, sz.y - bm.sz().y);
	    cbr = Coord.of(sz.x - br.sz().x, sz.y);
	    for(; mdo.x < cbr.x; mdo.x += bm.sz().x)
		g.image(bm, mdo, Coord.z, cbr);
	    g.image(br, sz.sub(br.sz()));
	}

	public void draw(GOut g) {
	    drawbg(g);
	    cdraw(g.reclip(aa.ul, aa.sz()));
	    drawframe(g);
	    super.draw(g);
	}

	private UI.Grab szdrag;
	private Coord szdragc;
	public boolean mousedown(Coord c, int button) {
	    if(dragsize) {
		Coord cc = c.sub(ca.ul);
		if((button == 1) && (c.x < ca.br.x) && (c.y < ca.br.y) && (c.y >= ca.br.y - UI.scale(25) + (ca.br.x - c.x))) {
		    szdrag = ui.grabmouse(this);
		    szdragc = aa.sz().sub(c);
		    return(true);
		}
	    }
	    return(super.mousedown(c, button));
	}

	public void mousemove(Coord c) {
	    if(szdrag != null)
		((Window)parent).resize(c.add(szdragc));
	    super.mousemove(c);
	}

	public boolean mouseup(Coord c, int button) {
	    if((button == 1) && (szdrag != null)) {
		szdrag.remove();
		szdrag = null;
		return(true);
	    }
	    return(super.mouseup(c, button));
	}

	public boolean checkhit(Coord c) {
	    Coord cpc = c.sub(cptl);
	    return(ca.contains(c) || (c.isect(cptl, cpsz) && (cm.back.getRaster().getSample(cpc.x % cm.back.getWidth(), cpc.y, 3) >= 128)));
	}
    }

    public void cdraw(GOut g) {
    }

    public Pipe.Op gbasic() {
	if((gbuf == null) || !Utils.eq(sz, gbuf.back.tex.sz())) {
	    if(gbuf != null)
		gbuf.dispose();
	    gbuf = new TexRaw(new Texture2D.Sampler2D(new Texture2D(this.sz, DataBuffer.Usage.STATIC, new VectorFormat(4, NumberFormat.UNORM8), null)), true);
	    gbuf.back.minfilter(Texture.Filter.LINEAR).magfilter(Texture.Filter.LINEAR);
	    gout = new FragColor<>(gbuf.back.tex.image(0));
	    Area garea = Area.sized(this.sz);
	    gbasic = Pipe.Op.compose(gout, DepthBuffer.slot.nil, cblend,
				     new States.Viewport(garea), new Ortho2D(garea));
	}
	return(gbasic);
    }

    protected void drawbuf(GOut g) {
	super.draw(g);
    }

    protected void drawfin(GOut g, Tex buf) {
	if(anim != null)
	    anim.draw(g, buf);
	else
	    g.image(buf, Coord.z);
    }

    public void draw(GOut og) {
	if(animst != "dest") {
	    GOut g = new GOut(og.out, og.basicstate().prep(gbasic()), this.sz);
	    g.out.clear(g.state(), FragColor.fragcol, FColor.BLACK_T);
	    drawbuf(g);
	}
	if(gbuf != null)
	    drawfin(og, gbuf);
    }

    public Coord contentsz() {
	Coord max = new Coord(0, 0);
	for(Widget wdg = child; wdg != null; wdg = wdg.next) {
	    if(wdg == deco)
		continue;
	    if(!wdg.visible)
		continue;
	    Coord br = wdg.c.add(wdg.sz);
	    if(br.x > max.x)
		max.x = br.x;
	    if(br.y > max.y)
		max.y = br.y;
	}
	return(max);
    }

    public Area ca() {
	if(deco == null)
	    return(Area.sized(this.sz));
	else
	    return(deco.contarea());
    }

    public Coord csz() {
	return(ca().sz());
    }

    private void resize2(Coord sz) {
	Coord psz = this.sz;
	if(deco != null) {
	    deco.iresize(sz);
	    deco.c = deco.contarea().ul.inv();
	    this.sz = deco.sz;
	} else {
	    this.sz = sz;
	}
	for(Widget ch = child; ch != null; ch = ch.next)
	    ch.presize();
    }

    public void resize(Coord sz) {
	resize2(sz);
    }

    @Deprecated
    public void decohide(boolean h) {
	chdeco(h ? null : makedeco());
	this.decohide = h;
    }

    @Deprecated
    public boolean decohide() {
	return(decohide);
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "dt") {
	    dt = Utils.bv(args[0]);
	} else if(msg == "cap") {
	    String cap = (String)args[0];
	    chcap(cap.equals("") ? null : cap);
	} else if(msg == "dhide") {
	    decohide(Utils.bv(args[0]));
	} else {
	    super.uimsg(msg, args);
	}
    }

    public Coord xlate(Coord c, boolean in) {
	if(deco == null)
	    return(c);
	if(in)
	    return(c.add(deco.contarea().ul));
	else
	    return(c.sub(deco.contarea().ul));
    }

    public void drag(Coord off) {
	dm = ui.grabmouse(this);
	doff = off;
    }

    public boolean checkhit(Coord c) {
	return((deco == null) || deco.checkhit(c));
    }

    public boolean mousedown(Coord c, int button) {
	if(super.mousedown(c, button)) {
	    parent.setfocus(this);
	    raise();
	    return(true);
	}
	return(false);
    }

    public boolean mouseup(Coord c, int button) {
	if(dm != null) {
	    dm.remove();
	    dm = null;
	} else {
	    super.mouseup(c, button);
	}
	return(true);
    }

    public void mousemove(Coord c) {
	if(dm != null) {
	    this.c = this.c.add(c.add(doff.inv()));
	} else {
	    super.mousemove(c);
	}
    }

    public boolean mousehover(Coord c, boolean hovering) {
	super.mousehover(c, hovering);
	return(hovering);
    }

    public boolean keydown(java.awt.event.KeyEvent ev) {
	if(super.keydown(ev))
	    return(true);
	if(key_esc.match(ev)) {
	    reqclose();
	    return(true);
	}
	return(false);
    }

    public boolean drop(Coord cc, Coord ul) {
	if(dt) {
	    wdgmsg("drop", cc);
	    return(true);
	}
	return(false);
    }

    public boolean iteminteract(Coord cc, Coord ul) {
	return(false);
    }

    public Object tooltip(Coord c, Widget prev) {
	if(!checkhit(c))
	    return(super.tooltip(c, prev));
	Object ret = super.tooltip(c, prev);
	if(ret != null)
	    return(ret);
	else
	    return("");
    }

    public void reqclose() {
	wdgmsg("close");
    }

    public static interface Animation {
	public boolean tick(double dt);
	public void draw(GOut g, Tex tex);
    }

    public static interface Transition<S extends Animation, H extends Animation> {
	public S show(Window wnd, H hiding);
	public H hide(Window wnd, S showing);
    }

    private Transition<?, ?> trans = null;
    private Animation anim = null;
    private String animst = null;
    public void tick(double dt) {
	super.tick(dt);
	if(anim != null) {
	    if(anim.tick(dt)) {
		if(animst == "show") {
		} else if(animst == "hide") {
		    super.hide();
		} else if(animst == "dest") {
		    destroy();
		} else {
		    throw(new AssertionError(animst));
		}
		anim = null;
		animst = null;
	    }
	}
    }

    @SuppressWarnings("unchecked")
    private <H extends Animation> Animation show0(Transition<?, H> trans, Animation h) {
	return(trans.show(this, (H)h));
    }
    @SuppressWarnings("unchecked")
    private <S extends Animation> Animation hide0(Transition<S, ?> trans, Animation s) {
	return(trans.hide(this, (S)s));
    }

    public void settrans(Transition<?, ?> trans) {
	if(this.anim != null)
	    throw(new IllegalStateException(String.valueOf(this.anim)));
	this.trans = trans;
    }

    public boolean visible() {
	return(visible && ((animst == null) || (animst == "show")));
    }

    private void initanim() {
	if(trans == null)
	    trans = deftrans();
	if(visible) {
	    anim = trans.show(this, null);
	    animst = "show";
	}
    }

    public void show() {
	if(parent == null) {
	    super.show();
	    return;
	}
	if(!visible)
	    super.show();
	if(animst == null) {
	    anim = trans.show(this, null);
	    animst = "show";
	} else if(animst == "show") {
	} else if(animst == "hide") {
	    anim = show0(trans, anim);
	    animst = "show";
	} else if(animst == "dest") {
	} else {
	    throw(new AssertionError(animst));
	}
    }

    public void hide() {
	if(parent == null) {
	    super.hide();
	    return;
	}
	if(animst == null) {
	    anim = trans.hide(this, null);
	    animst = "hide";
	} else if(animst == "show") {
	    anim = hide0(trans, anim);
	    animst = "hide";
	} else if(animst == "hide") {
	} else if(animst == "dest") {
	} else {
	    throw(new AssertionError(animst));
	}
    }

    public void reqdestroy() {
	if(parent == null) {
	    super.reqdestroy();
	    return;
	}
	if(animst == null) {
	    anim = trans.hide(this, null);
	    animst = "dest";
	} else if(animst == "show") {
	    anim = hide0(trans, anim);
	    animst = "dest";
	} else if(animst == "hide") {
	    animst = "dest";
	} else if(animst == "dest") {
	} else {
	    throw(new AssertionError(animst));
	}
    }

    public static class NilAnim implements Animation {
	public boolean tick(double dt) {return(true);}
	public void draw(GOut g, Tex tex) {g.image(tex, Coord.z);}
    }

    public static final Transition<?, ?> niltrans = new Transition<Animation, Animation>() {
	    public NilAnim show(Window wnd, Animation hide) {return(new NilAnim());}
	    public NilAnim hide(Window wnd, Animation show) {return(new NilAnim());}
	};

    public abstract static class NormAnim implements Animation {
	public final double s;
	public final boolean rev;
	public double a = 0.0, na = 0.0;

	public NormAnim(double t, double fromn, boolean rev) {
	    this.s = 1.0 / t;
	    this.na = fromn;
	    this.rev = rev;
	    this.a = (rev ? (1.0 - fromn) : fromn) * t;
	}
	public NormAnim(double t, NormAnim from, boolean rev) {
	    this(t, (from == null) ? (rev ? 1.0 : 0.0) : from.na, rev);
	}
	public NormAnim(double t) {this(t, 0.0, false);}

	public boolean tick(double dt) {
	    a += dt;
	    double na = Math.min(a * s, 1.0);
	    stick(this.na = rev ? (1.0 - na) : na);
	    return(na >= 1.0);
	}

	public void stick(double a) {}
    }

    public static class FadeAnim extends NormAnim {
	public static final double minfac = 0.1;
	public static final double time = 0.1;

	public FadeAnim(boolean hide, FadeAnim from) {
	    super(time, from, hide);
	}

	public void draw(GOut g, Tex tex) {
	    double na = Utils.smoothstep(this.na);
	    g.chcolor(255, 255, 255, (int)(na * 255));
	    Coord sz = tex.sz();
	    double fac = minfac * (1.0 - na);
	    g.image(tex, Coord.of((int)(sz.x * fac), (int)(sz.y * fac)),
		    Coord.of((int)(sz.x * (1.0 - (fac * 2))), (int)(sz.y * (1.0 - (fac * 2)))));
	}

	public static final Transition<?, ?> trans = new Transition<FadeAnim, FadeAnim>() {
		public FadeAnim show(Window wnd, FadeAnim hide) {return(new FadeAnim(false, hide));}
		public FadeAnim hide(Window wnd, FadeAnim show) {return(new FadeAnim(true,  show));}
	    };
    }

    protected Transition<?, ?> deftrans() {
	return(FadeAnim.trans);
    }

    public static void main(String[] args) {
	Window wnd = new Window(new Coord(300, 200), "Inventory", true);
	new haven.rs.DrawBuffer(haven.rs.Context.getdefault().env(), new Coord(512, 512))
	    .draw(g -> {
		    wnd.draw(g);
		    g.getimage(img -> Debug.dumpimage(img, args[0]));
	    });
    }
}
