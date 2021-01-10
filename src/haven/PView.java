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
import haven.RenderContext.PostProcessor;
import haven.render.Texture2D.Sampler2D;

public abstract class PView extends Widget {
    public final RenderTree tree;
    public final RenderTree.Slot conf;
    public final RenderTree.Slot basic;
    public Texture2D fragcol = null;
    public Texture2D depth = null;
    protected Environment env = null;
    protected InstanceList instancer;
    protected DrawList back = null;
    protected Coord rsz;
    private final Map<Object, Pipe.Op> basicstates = new IdentityHashMap<>();
    private final Light.LightList lights = new Light.LightList();
    private ActAudio audio;
    private final ScreenList list2d = new ScreenList();
    private final TickList ticklist = new TickList();
    private Sampler2D fragsamp;

    public PView(Coord sz) {
	super(sz);
	tree = new RenderTree();
	tree.add(list2d, Render2D.class);
	tree.add(ticklist, TickList.TickNode.class);
	conf = tree.add((RenderTree.Node)null);
	conf.ostate(frame());
	basic = conf.add((RenderTree.Node)null);
	basic();
    }

    public static class WidgetContext extends RenderContext {
	private final PView wdg;

	public WidgetContext(PView wdg) {
	    this.wdg = wdg;
	}

	public PView widget() {
	    return(wdg);
	}

	private static final ClassResolver<PView> ctxr = new ClassResolver<PView>()
	    .add(Glob.class, wdg -> wdg.ui.sess.glob)
	    .add(Session.class, wdg -> wdg.ui.sess);
	public <T> T context(Class<T> cl) {return(ctxr.context(cl, wdg));}

	public Pipe.Op basic(Object id) {return(wdg.basic(id));}
	public void basic(Object id, Pipe.Op state) {wdg.basic(id, state);}

	public String toString() {return(String.format("#<widgetctx %s>", wdg.getClass()));}
    }

    protected Coord rendersz() {
	GSettings prefs = curprefs;
	if(prefs != null) {
	    float rscale = prefs.rscale.val;
	    return(new Coord((int)Math.round(sz.x * rscale), (int)Math.round(sz.y * rscale)));
	}
	return(this.sz);
    }

    private final WidgetContext ctx = new WidgetContext(this);
    private Pipe.Op conf() {
	rsz = rendersz();
	return(Pipe.Op.compose(new FrameConfig(rsz), ctx, curprefs));
    }

    private Pipe.Op curconf = null;
    private Pipe.Op curconf() {
	if(curconf == null)
	    curconf = conf();
	return(curconf);
    }

    private GSettings curprefs = null;
    protected GSettings gprefs() {
	if(ui == null)
	    return(null);
	return(ui.gprefs);
    }

    private Pipe.Op frame() {
	return(Pipe.Op.compose(curconf(),
			       new FrameInfo(),
			       ((ui == null) || (ui.sess == null)) ? null : new Glob.FrameInfo(ui.sess.glob)));
    }

    protected void reconf() {
	curconf = null;
	conf.ostate(frame());
    }

    public void resize(Coord sz) {
	super.resize(sz);
	reconf();
    }

    public Pipe.Op basic(Object id) {
	try(Locked lk = tree.lock()) {
	    return(basicstates.get(id));
	}
    }

    public void basic(Object id, Pipe.Op state) {
	try(Locked lk = tree.lock()) {
	    Pipe.Op prev;
	    Collection<Pipe.Op> comb = null;
	    if(state == null)
		prev = basicstates.remove(id);
	    else
		prev = basicstates.put(id, state);
	    if(!Utils.eq(prev, state)) {
		comb = new ArrayList<>(basicstates.values());
		try {
		    Collection<Pipe.Op> dcomb = comb;
		    basic.ostate(p -> {
			    for(Pipe.Op op : dcomb)
				op.apply(p);
			});
		} catch(RuntimeException e) {
		    if(prev == null)
			basicstates.remove(id);
		    else
			basicstates.put(id, prev);
		    throw(e);
		}
	    }
	}
    }

    /* XXX? Remove standard clearing and assume implementations to add
     * explicit clearing slot? */
    protected FColor clearcolor() {
	return(FColor.BLACK);
    }

    /* I've no idea why this function is necessary. */
    @SuppressWarnings("unchecked")
    private static RenderList.Slot<Rendered> uglyJavaCWorkAround(RenderList.Slot<?> slot) {
	return((RenderList.Slot<Rendered>)slot);
    }

    public void tick(double dt) {
	super.tick(dt);
	GSettings gprefs = gprefs();
	if(gprefs != this.curprefs) {
	    this.curprefs = gprefs;
	    reconf();
	}
	conf.ostate(frame());
	ticklist.tick(dt);
	if(audio != null)
	    audio.cycle();
    }

    private class Resampler extends PostProcessor {
	public void run(GOut g, Texture2D.Sampler2D in) {
	    g.image(new TexRaw(in, true), Coord.z, g.sz());
	}
    }

    private GOut resolveout(GOut def, PostProcessor next) {
	if(next == null)
	    return(def);
	if((next.buf != null) && !next.buf.tex.sz().equals(fragcol.sz())) {
	    next.buf.dispose();
	    next.buf = null;
	}
	if(next.buf == null) {
	    Texture2D tex = new Texture2D(fragcol.sz(), DataBuffer.Usage.STATIC, new VectorFormat(4, NumberFormat.UNORM8), null);
	    next.buf = new Sampler2D(tex);
	    next.buf.minfilter(Texture.Filter.LINEAR).magfilter(Texture.Filter.LINEAR);
	    next.buf.swrap(Texture.Wrapping.CLAMP).twrap(Texture.Wrapping.CLAMP);
	}
	Pipe st = new BufPipe();
	Area area = Area.sized(Coord.z, next.buf.tex.sz());
	st.prep(new FrameInfo()).prep(new States.Viewport(area)).prep(new Ortho2D(area));
	st.prep(new FragColor<>(next.buf.tex.image(0)));
	return(new GOut(def.out, st, new Coord(area.sz())));
    }

    private PostProcessor pp_resamp = null;
    protected void resolve(GOut g) {
	List<PostProcessor> copy = new ArrayList<PostProcessor>(ctx.postproc());
	if(!rsz.equals(this.sz)) {
	    if(pp_resamp == null)
		pp_resamp = new Resampler();
	    copy.add(pp_resamp);
	} else {
	    if(pp_resamp != null) {
		pp_resamp.dispose();
		pp_resamp = null;
	    }
	}
	Iterator<PostProcessor> post = copy.iterator();
	PostProcessor next = post.hasNext() ? post.next() : null;
	resolveout(g, next).image(new TexRaw(fragsamp, true), Coord.z);
	while(next != null) {
	    PostProcessor cur = next;
	    next = post.hasNext() ? post.next() : null;
	    cur.run(resolveout(g, next), cur.buf);
	}
    }

    public void add(PostProcessor post) {ctx.add(post);}
    public void remove(PostProcessor post) {ctx.remove(post);}

    protected void envsetup() {
	back = env.drawlist();
	instancer = new InstanceList(tree);
	instancer.add(back, Rendered.class);
	instancer.asyncadd(tree, Rendered.class);
    }

    protected void envdispose() {
	tree.remove(instancer);
	back.dispose(); back = null;
	instancer.dispose(); instancer = null;
    }

    protected void maindraw(Render out) {
	back.draw(out);
    }

    public void gtick(Render out) {
	ticklist.gtick(out);
    }

    public void draw(GOut g) {
	if((back == null) || !back.compatible(g.out.env())) {
	    if(env != null) {
		envdispose();
		env = null;
	    }
	    env = g.out.env();
	    envsetup();
	}
	lights();
	FColor cc = clearcolor();
	if(cc != null)
	    g.out.clear(basic.state(), FragColor.fragcol, cc);
	g.out.clear(basic.state(), 1.0);
	try(Locked lk = tree.lock()) {
	    instancer.commit(g.out);
	    maindraw(g.out);
	}
	resolve(g);
	list2d.draw(g);
    }

    public void dispose() {
	if(audio != null) {
	    audio.clear();
	}
	if(env != null) {
	    envdispose();
	    env = null;
	}
	super.dispose();
    }

    private static final Object id_fb = new Object(), id_view = new Object(), id_misc = new Object();
    protected void basic() {
	basic(id_fb, p -> {
		FrameConfig fb = p.get(FrameConfig.slot);
		if((fragcol == null) || !fragcol.sz().equals(fb.sz)) {
		    if(fragcol != null)
			fragcol.dispose();
		    fragcol = new Texture2D(fb.sz, DataBuffer.Usage.STATIC, new VectorFormat(4, NumberFormat.UNORM8), null);
		    fragsamp = new Sampler2D(fragcol);
		}
		if((depth == null) || !depth.sz().equals(fb.sz)) {
		    if(depth != null)
			depth.dispose();
		    depth = new Texture2D(fb.sz, DataBuffer.Usage.STATIC, Texture.DEPTH, new VectorFormat(1, NumberFormat.FLOAT32), null);
		}
		p.prep(new FragColor<>(fragcol.image(0))).prep(new DepthBuffer<>(depth.image(0)));
	    });
	basic(id_view, p -> {
		FrameConfig fb = p.get(FrameConfig.slot);
		Area area = Area.sized(Coord.z, fb.sz);
		p.prep(new States.Viewport(area));
		p.prep(Homo3D.state);
	    });
	basic(id_misc, Pipe.Op.compose(FragColor.blend(new BlendMode(BlendMode.Function.ADD, BlendMode.Factor.SRC_ALPHA, BlendMode.Factor.INV_SRC_ALPHA,
								     BlendMode.Function.MAX, BlendMode.Factor.SRC_ALPHA, BlendMode.Factor.INV_SRC_ALPHA)),
				       new States.Depthtest(States.Depthtest.Test.LE),
				       new States.Facecull()));
	lights();
    }

    protected void attached() {
	basic(ActAudio.class, this.audio = new ActAudio(ui.audio));
    }

    protected void lights() {
	basic(Light.class, Pipe.Op.compose(lights, lights.compile()));
    }

    public interface Render2D extends RenderTree.Node {
	public void draw(GOut g, Pipe state);
    }

    public static class ScreenList implements RenderList<Render2D> {
	private final Set<Slot<? extends Render2D>> cur = new HashSet<>();

	public void draw(GOut g) {
	    List<Slot<? extends Render2D>> copy;
	    synchronized(cur) {
		copy = new ArrayList<>(cur);
	    }
	    for(Slot<? extends Render2D> slot : copy) {
		slot.obj().draw(g, slot.state());
	    }
	}

	public void add(Slot<? extends Render2D> slot) {
	    synchronized(cur) {
		cur.add(slot);
	    }
	}

	public void remove(Slot<? extends Render2D> slot) {
	    synchronized(cur) {
		cur.remove(slot);
	    }
	}

	public void update(Slot<? extends Render2D> slot) {}
	public void update(Pipe group, int[] statemask) {}
    }
}
