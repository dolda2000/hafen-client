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

package haven.render.gl;

import java.util.*;
import java.util.function.*;
import java.util.concurrent.atomic.*;
import java.nio.ByteBuffer;
import haven.*;
import haven.render.*;
import haven.render.sl.*;

public class GLDrawList implements DrawList {
    public static final int idx_vao = 0;
    public static final int idx_fbo = 1;
    public static final int idx_pst = 2;
    public static final int idx_uni = idx_pst + GLPipeState.all.length;
    public final GLEnvironment env;
    private final Map<SettingKey, DepSetting> settings = new HashMap<>();
    private final Map<Slot<? extends Rendered>, DrawSlot> slotmap = new IdentityHashMap<>();
    private final Map<Pipe, Object> psettings = new IdentityHashMap<>();
    private final Map<Pipe, Object> orderidx = new IdentityHashMap<>();
    private final GLDoubleBuffer settingbuf = new GLDoubleBuffer();
    private DrawSlot root = null;
    private boolean disposed = false;

    private static int btheight(DrawSlot s) {
	return((s == null) ? 0 : s.th);
    }
    private static int btsubsize(DrawSlot s) {
	return((s == null) ? 0 : s.tsubsize);
    }
    private static void setp(DrawSlot s, DrawSlot p) {
	if(s != null)
	    s.tp = p;
    }

    DrawSlot first() {
	if(root == null)
	    return(null);
	for(DrawSlot s = root; true; s = s.tl) {
	    if(s.tl == null)
		return(s);
	}
    }

    private static final Comparator<DrawSlot> order = new Comparator<DrawSlot>() {
	    public int compare(DrawSlot a, DrawSlot b) {
		int c;
		if((c = Rendered.Order.cmp.compare(a.gorder, b.gorder)) != 0)
		    return(c);
		if((c = Utils.sidcmp(a.prog, b.prog)) != 0)
		    return(c);
		if((c = Utils.sidcmp(a.settings[idx_fbo], b.settings[idx_fbo])) != 0)
		    return(c);
		if((c = Utils.sidcmp(((VaoSetting)a.settings[idx_vao]).st, ((VaoSetting)b.settings[idx_vao]).st)) != 0)
		    return(c);
		return((a.sortid < b.sortid) ? -1 : 1);
	    }
	};
    private static AtomicLong uniqid = new AtomicLong();
    private class DrawSlot {
	/* List structure */
	final long sortid;
	DrawSlot tp, tl, tr;
	int th = 0;
	int tsubsize = 0;	/* Not critical, only used for debugging purposes. */

	DrawSlot prev() {
	    if(tl != null) {
		for(DrawSlot s = tl; true; s = s.tr) {
		    if(s.tr == null)
			return(s);
		}
	    } else {
		for(DrawSlot s = tp, ps = this; s != null; ps = s, s = s.tp) {
		    if(s.tl != ps)
			return(s);
		}
		return(null);
	    }
	}
	DrawSlot next() {
	    if(tr != null) {
		for(DrawSlot s = tr; true; s = s.tl) {
		    if(s.tl == null)
			return(s);
		}
	    } else {
		for(DrawSlot s = tp, ps = this; s != null; ps = s, s = s.tp) {
		    if(s.tr != ps)
			return(s);
		}
		return(null);
	    }
	}

	private int setheight() {
	    tsubsize = btsubsize(tl) + btsubsize(tr) + 1;
	    return(th = (Math.max(btheight(tl), btheight(tr)) + 1));
	}

	private void bbtrl() {
	    if(btheight(tr.tl) > btheight(tr.tr))
		tr.bbtrr();
	    DrawSlot p = tp, r = tr, rl = r.tl;
	    setp(tr = rl, this);
	    setheight();
	    setp(r.tl = this, r);
	    r.setheight();
	    if(p == null)
		setp(root = r, null);
	    else if(p.tl == this)
		setp(p.tl = r, p);
	    else
		setp(p.tr = r, p);
	}
	private void bbtrr() {
	    if(btheight(tl.tr) > btheight(tl.tl))
		tl.bbtrl();
	    DrawSlot p = tp, l = tl, lr = l.tr;
	    setp(tl = lr, this);
	    setheight();
	    setp(l.tr = this, l);
	    l.setheight();
	    if(p == null)
		setp(root = l, null);
	    else if(p.tl == this)
		setp(p.tl = l, p);
	    else
		setp(p.tr = l, p);
	}
	private void insert(DrawSlot child) {
	    int c = order.compare(child, this);
	    if(c < 0) {
		if(tl == null)
		    (tl = child).tp = this;
		else
		    tl.insert(child);
	    } else if(c > 0) {
		if(tr == null)
		    (tr = child).tp = this;
		else
		    tr.insert(child);
	    } else {
		throw(new RuntimeException());
	    }
	    if(btheight(tl) > btheight(tr) + 1)
		bbtrr();
	    else if(btheight(tr) > btheight(tl) + 1)
		bbtrl();
	    setheight();
	}
	private void tinsert() {
	    if((tp != null) || (root == this))
		throw(new IllegalStateException());
	    th = 1;
	    tsubsize = 1;
	    if(root == null) {
		root = this;
	    } else {
		root.insert(this);
	    }
	}
	private void tremove() {
	    if((tp == null) && (root != this))
		throw(new IllegalStateException());
	    DrawSlot rep;
	    if((tl != null) && (tr != null)) {
		for(rep = tr; rep.tl != null; rep = rep.tl);

		DrawSlot p = rep.tp;
		if(p.tl == rep)
		    setp(p.tl = rep.tr, p);
		else
		    setp(p.tr = rep.tr, p);
		p.setheight();

		setp(rep.tl = tl, rep);
		setp(rep.tr = tr, rep);
		rep.setheight();
	    } else if(tl != null) {
		rep = tl;
	    } else if(tr != null) {
		rep = tr;
	    } else {
		rep = null;
	    }
	    if(tp != null) {
		if(tp.tl == this)
		    tp.tl = rep;
		else
		    tp.tr = rep;
	    } else {
		root = rep;
	    }
	    if(rep != null)
		rep.tp = tp;
	    for(DrawSlot p = tp, pp; p != null; p = pp) {
		p.setheight();
		pp = p.tp;
		if(btheight(p.tl) > btheight(p.tr) + 1)
		    p.bbtrr();
		else if(btheight(p.tr) > btheight(p.tl) + 1)
		    p.bbtrl();
	    }
	    tr = tl = tp = null;
	}

	/* Render information */
	final Slot<? extends Rendered> bk;
	final GLProgram prog;
	final Setting[] settings;
	BufferBGL compiled, main;
	Rendered.Order gorder;
	final Pipe ordersrc;
	private volatile boolean disposed = false;

	private GLProgram progfor(Slot<? extends Rendered> sl) {
	    State[] st = sl.state().states();
	    ShaderMacro[] shaders = new ShaderMacro[st.length];
	    int shash = 0;
	    for(int i = 0; i < st.length; i++) {
		shaders[i] = (st[i] == null) ? null : st[i].shader();
		shash ^= System.identityHashCode(shaders[i]);
	    }
	    return(env.getprog(shash, shaders));
	}

	private void getsettings() {
	    GroupPipe bst = bk.state();
	    settings[idx_vao] = vao_nil;
	    settings[idx_fbo] = getframe(prog, bst);
	    for(int i = 0; i < GLPipeState.all.length; i++)
		settings[idx_pst + i] = getpipest(GLPipeState.all[i], bst);
	    for(int i = 0; i < prog.uniforms.length; i++)
		settings[idx_uni + i] = getuniform(prog, prog.uniforms[i], bst);
	}

	private void glupdate(DrawSlot prev) {
	    if(prev == null) {
		compiled = main;
	    } else if(prev.prog == this.prog) {
		BufferBGL gl = new BufferBGL();
		for(int i = 0; i < this.settings.length; i++) {
		    if(this.settings[i] != prev.settings[i])
			gl.bglSubmit(this.settings[i].gl);
		}
		gl.bglCallList(main);
		compiled = gl.trim();
	    } else {
		BufferBGL gl = new BufferBGL();
		GLProgram.apply(gl, prev.prog, this.prog);
		for(int i = 0; i < this.settings.length; i++)
		    gl.bglSubmit(this.settings[i].gl);
		gl.bglCallList(main);
		compiled = gl.trim();
	    }
	}

	@SuppressWarnings("unchecked")
	private void orderreg() {
	    Object cur = orderidx.get(ordersrc);
	    if(cur == null) {
		orderidx.put(ordersrc, this);
	    } else if(cur instanceof DrawSlot) {
		List<DrawSlot> nl = new ArrayList<>(2);
		nl.add((DrawSlot)cur);
		nl.add(this);
		orderidx.put(ordersrc, nl);
	    } else if(cur instanceof List) {
		List<DrawSlot> ls = (List<DrawSlot>)cur;
		ls.add(this);
	    } else {
		throw(new RuntimeException());
	    }
	}

	@SuppressWarnings("unchecked")
	private void orderunreg() {
	    Object cur = orderidx.get(ordersrc);
	    if(cur == null) {
		throw(new RuntimeException());
	    } else if(cur == this) {
		orderidx.remove(ordersrc);
	    } else if(cur instanceof List) {
		List<DrawSlot> ls = (List<DrawSlot>)cur;
		ls.remove(this);
		if(ls.size() < 2)
		    orderidx.put(ordersrc, ls.get(0));
	    } else {
		throw(new RuntimeException());
	    }
	}

	void orderupdate() {
	    Rendered.Order norder = ordersrc.get(Rendered.order);
	    boolean fixed = false;
	    if((Rendered.Order.cmp.compare(gorder, norder) == 0) ||
	       ((Rendered.Order.cmp.compare(prev().gorder, norder) >= 0) &&
		(Rendered.Order.cmp.compare(next().gorder, norder) <= 0)))
	    {
		gorder = norder;
	    } else {
		tremove();
		gorder = norder;
		tinsert();
	    }
	}

	DrawSlot(Slot<? extends Rendered> bk) {
	    try {
		GroupPipe bst = bk.state();
		this.sortid = uniqid.getAndIncrement();
		this.bk = bk;
		this.prog = progfor(bk);
		this.prog.lock();
		this.settings = new Setting[idx_uni + prog.uniforms.length];
		getsettings();
		gorder = Rendered.deflt;
		{
		    int grp = bst.gstate(Rendered.order.id);
		    if(grp < 0) {
			ordersrc = null;
		    } else {
			ordersrc = bst.group(grp);
			gorder = ordersrc.get(Rendered.order);
			orderreg();
		    }
		}
		main = BufferBGL.empty;
		SlotRender g = new SlotRender(this);
		bk.obj().draw(bst, g);
	    } catch(RuntimeException exc) {
		dispose();
		throw(exc);
	    }
	}

	void insert() {
	    tinsert();
	    DrawSlot prev = prev(), next = next();
	    this.glupdate(prev);
	    if(next != null)
		next.glupdate(this);
	}

	void remove() {
	    DrawSlot prev = prev(), next = next();
	    tremove();
	    if(next != null)
		next.glupdate(prev);
	}

	void dispose() {
	    if(disposed)
		throw(new IllegalStateException());
	    this.disposed = true;
	    if(ordersrc != null)
		orderunreg();
	    if(settings != null) {
		for(int i = 0; i < settings.length; i++) {
		    if(settings[i] != null)
			settings[i].put();
		}
	    }
	    if(this.prog != null)
		this.prog.unlock();
	}
    }

    static class SettingKey {
	final GLProgram prog;
	final Object vid;
	final Pipe depid_1;
	final Pipe[] depid_v;

	SettingKey(GLProgram prog, Object vid, Pipe... depid) {
	    this.prog = prog;
	    this.vid = vid;
	    if(depid.length == 1) {
		this.depid_1 = depid[0];
		this.depid_v = null;
	    } else {
		this.depid_1 = null;
		this.depid_v = depid;
	    }
	}

	public int ndeps() {
	    return((depid_v == null) ? 1 : depid_v.length);
	}

	public int hashCode() {
	    int rv = System.identityHashCode(prog);
	    rv = (rv * 31) + System.identityHashCode(vid);
	    if(depid_v == null) {
		rv = (rv * 31) + System.identityHashCode(depid_1);
	    } else {
		for(int i = 0; i < depid_v.length; i++)
		    rv = (rv * 31) + System.identityHashCode(depid_v[i]);
	    }
	    return(rv);
	}

	public boolean equals(Object o) {
	    if(!(o instanceof SettingKey))
		return(false);
	    SettingKey that = (SettingKey)o;
	    if((this.prog != that.prog) || (this.vid != that.vid))
		return(false);
	    if(depid_v == null) {
		if(this.depid_1 != that.depid_1)
		    return(false);
	    } else {
		if(this.depid_v.length != that.depid_v.length)
		    return(false);
		for(int i = 0; i < depid_v.length; i++) {
		    if(this.depid_v[i] != that.depid_v[i])
			return(false);
		}
	    }
	    return(true);
	}
    }

    private static Pipe nidx(GroupPipe st, int idx) {
	return((idx < 0) ? Pipe.nil : st.group(idx));
    }

    static Pipe[] makedepid(GroupPipe state, Collection<State.Slot<?>> deps) {
	Iterator<State.Slot<?>> it = deps.iterator();
	if(!it.hasNext())
	    return(new Pipe[0]);
	int one = state.gstate(it.next().id);
	int ni = 1;
	while(it.hasNext()) {
	    int cid = it.next().id;
	    int grp = state.gstate(cid);
	    if(grp != one) {
		Pipe[] ret = new Pipe[deps.size()];
		for(int i = 0; i < ni; i++)
		    ret[i] = nidx(state, one);
		ret[ni++] = nidx(state, grp);
		while(it.hasNext())
		    ret[ni++] = nidx(state, state.gstate(it.next().id));
		return(ret);
	    }
	    ni++;
	}
	return(new Pipe[] {nidx(state, one)});
    }

    abstract class Setting {
	final GLDoubleBuffer.Buffered gl = settingbuf.new Buffered();

	abstract void compile(BGL gl);

	void update() {
	    BufferBGL buf = new BufferBGL();
	    compile(buf);
	    this.gl.update(buf.trim());
	}

	void put() {}
    }

    abstract class DepSetting extends Setting {
	final SettingKey key;
	int rc = 0;

	DepSetting(SettingKey key) {
	    this.key = key;
	}

	abstract State.Slot[] depslots();

	private int depmask_1 = -1;
	private int[] depmask_v = null;
	void ckupdate(int[] mask) {
	    if((depmask_v == null) && (depmask_1 < 0)) {
		State.Slot[] slots = depslots();
		if(slots.length == 1) {
		    depmask_1 = slots[0].id;
		} else {
		    int[] depmask = new int[slots.length];
		    for(int i = 0; i < slots.length; i++)
			depmask[i] = slots[i].id;
		    depmask_v = depmask;
		}
	    }
	    if(depmask_v == null) {
		for(int i = 0; i < mask.length; i++) {
		    if(mask[i] == depmask_1) {
			update();
			break;
		    }
		}
	    } else {
		int[] dmask = this.depmask_v;
		for(int i = 0; i < mask.length; i++) {
		    for(int o = 0; o < dmask.length; o++) {
			if(mask[i] == dmask[o]) {
			    update();
			    break;
			}
		    }
		}
	    }
	}

	Pipe compstate() {
	    if(key.depid_v == null)
		return(key.depid_1);
	    State.Slot[] depslots = depslots();
	    return(new Pipe() {
		    public <T extends State> T get(State.Slot<T> slot) {
			for(int i = 0; i < depslots.length; i++) {
			    if(depslots[i] == slot)
				return(key.depid_v[i].get(slot));
			}
			throw(new RuntimeException("reading non-dependent slot"));
		    }

		    /* Not entirely clear whether these should even be
		     * implemented. Evaluate in case they are ever
		     * used. */
		    public Pipe copy() {throw(new NotImplemented());}
		    public State[] states() {throw(new NotImplemented());}
		});
	}

	void put() {
	    if(--rc <= 0) {
		if(rc < 0)
		    throw(new RuntimeException());
		del();
	    }
	}

	void del() {
	    delsetting(this);
	}
    }

    private void delsettingp(DepSetting set, Pipe dp) {
	Object cur = psettings.get(dp);
	if(cur == null) {
	} else if(cur == set) {
	    psettings.remove(dp);
	} else if(cur instanceof DepSetting[]) {
	    DepSetting[] sl = (DepSetting[])cur;
	    int n = -1;
	    find: for(int i = 0; i < sl.length; i++) {
		if(sl[i] == set) {
		    if((i == sl.length - 1) || (sl[i + 1] == null)) {
			sl[i] = null;
			n = i;
			break find;
		    } else {
			for(int o = sl.length - 1; o > i; o--) {
			    if(sl[o] != null) {
				sl[i] = sl[o];
				sl[o] = null;
				n = o;
				break find;
			    }
			}
			throw(new RuntimeException());
		    }
		}
	    }
	    if(n < 0) {
	    } else if(n == 0) {
		throw(new RuntimeException());
	    } else if(n == 1) {
		psettings.put(dp, sl[0]);
	    }
	} else {
	    throw(new RuntimeException());
	}
    }

    private void delsetting(DepSetting set) {
	settings.remove(set.key);
	if(set.key.depid_v != null) {
	    Pipe[] deps = set.key.depid_v;
	    intern: for(int i = 0; i < deps.length; i++) {
		for(int o = 0; o < i; o++) {
		    if(deps[i] == deps[o])
			continue intern;
		}
		delsettingp(set, deps[i]);
	    }
	} else if(set.key.depid_1 != null) {
	    delsettingp(set, set.key.depid_1);
	}
    }

    private void addsettingp(DepSetting set, Pipe dp) {
	Object cur = psettings.get(dp);
	if(cur == null) {
	    psettings.put(dp, set);
	} else if(cur instanceof DepSetting) {
	    if(cur == set)
		throw(new RuntimeException());
	    psettings.put(dp, new DepSetting[] {(DepSetting)cur, set});
	} else if(cur instanceof DepSetting[]) {
	    DepSetting[] sl = (DepSetting[])cur;
	    for(int i = 0; i < sl.length; i++) {
		if(sl[i] == set)
		    throw(new RuntimeException());
	    }
	    if(sl[sl.length - 1] == null) {
		for(int i = sl.length - 1; i > 0; i--) {
		    if(sl[i - 1] != null) {
			sl[i] = set;
			return;
		    }
		}
		throw(new RuntimeException());
	    } else {
		DepSetting[] nsl = Arrays.copyOf(sl, sl.length + 1);
		nsl[sl.length] = set;
		psettings.put(dp, nsl);
	    }
	} else {
	    throw(new RuntimeException());
	}
    }

    private void addsetting(DepSetting set) {
	settings.put(set.key, set);
	if(set.key.depid_v != null) {
	    Pipe[] deps = set.key.depid_v;
	    intern: for(int i = 0; i < deps.length; i++) {
		for(int o = 0; o < i; o++) {
		    if(deps[i] == deps[o])
			continue intern;
		}
		addsettingp(set, deps[i]);
	    }
	} else if(set.key.depid_1 != null) {
	    addsettingp(set, set.key.depid_1);
	}
    }

    private static State.Slot[] progfslots(GLProgram prog) {
	int n = 1;
	for(FragData var : prog.fragdata)
	    n += var.deps.size();
	State.Slot[] ret = new State.Slot[n];
	n = 0;
	ret[n++] = DepthBuffer.slot;
	for(FragData var : prog.fragdata) {
	    for(State.Slot dep : var.deps)
		ret[n++] = dep;
	}
	return(ret);
    }

    class FrameSetting extends DepSetting {
	final GLProgram prog;

	FrameSetting(SettingKey key) {
	    super(key);
	    this.prog = key.prog;
	    update();
	}

	void compile(BGL gl) {
	    Pipe pipe = compstate();
	    DepthBuffer dbuf = pipe.get(DepthBuffer.slot);
	    Object depth = env.prepfval((dbuf != null) ? dbuf.image : null);
	    Object[] fvals = new Object[prog.fragdata.length];
	    FragTarget[] fconf = new FragTarget[prog.fragdata.length];
	    for(int i = 0; i < fvals.length; i++) {
		Object fval = prog.fragdata[i].value.apply(pipe);
		if(fval instanceof FragTarget)
		    fval = (fconf[i] = (FragTarget)fval).buf;
		else
		    fconf[i] = FboState.NIL_CONF;
		fvals[i] = env.prepfval(fval);
	    }
	    FboState.make(env, depth, fvals, fconf).apply(gl);
	}

	State.Slot[] depslots() {
	    return(progfslots(prog));
	}
    }

    DepSetting getframe(GLProgram prog, GroupPipe state) {
	SettingKey key = new SettingKey(prog, FboState.class, makedepid(state, Arrays.asList(progfslots(prog))));
	DepSetting ret = settings.get(key);
	if(ret == null)
	    addsetting(ret = new FrameSetting(key));
	ret.rc++;
	return(ret);
    }

    class PipeSetting<T extends State> extends DepSetting {
	final GLPipeState<T> setting;

	PipeSetting(SettingKey key, GLPipeState<T> setting) {
	    super(key);
	    this.setting = setting;
	    update();
	}

	void compile(BGL gl) {
	    T st = compstate().get(setting.slot);
	    setting.apply(env, gl, st);
	}

	State.Slot[] depslots() {
	    return(new State.Slot[] {setting.slot});
	}

	public String toString() {
	    return(String.format("#<pipe-setting %s>", setting));
	}
    }

    DepSetting getpipest(GLPipeState<?> pst, GroupPipe state) {
	SettingKey key = new SettingKey(null, pst, makedepid(state, Arrays.asList(pst.slot)));
	DepSetting ret = settings.get(key);
	if(ret == null)
	    addsetting(ret = new PipeSetting<>(key, pst));
	ret.rc++;
	return(ret);
    }

    class UniformSetting extends DepSetting {
	final GLProgram prog;
	final Uniform var;
	GLObject vref;

	UniformSetting(SettingKey key) {
	    super(key);
	    this.prog = key.prog;
	    this.var = (Uniform)key.vid;
	    update();
	}

	void compile(BGL gl) {
	    Object val = env.prepuval(var.value.apply(compstate()));
	    UniformApplier.TypeMapping.apply(gl, prog, var, val);
	    GLObject pref = this.vref;
	    if(val instanceof GLObject)
		(this.vref = (GLObject)val).get();
	    else
		this.vref = null;
	    if(pref != null)
		pref.put();
	}

	State.Slot[] depslots() {
	    return(var.deps.toArray(new State.Slot[key.ndeps()]));
	}

	void del() {
	    if(vref != null) {
		vref.put();
		this.vref = null;
	    }
	    super.del();
	}
    }

    DepSetting getuniform(GLProgram prog, Uniform var, GroupPipe state) {
	SettingKey key = new SettingKey(prog, var, makedepid(state, var.deps));
	DepSetting ret = settings.get(key);
	if(ret == null)
	    addsetting(ret = new UniformSetting(key));
	ret.rc++;
	return(ret);
    }

    class VaoSetting extends Setting {
	final VaoBindState st;

	VaoSetting(GLVertexArray vao, GLBuffer ebo) {
	    this.st = new VaoBindState(vao, ebo);
	    update();
	}

	void compile(BGL gl) {
	    st.apply(gl);
	}

	void put() {
	    if(st.vao != null)
		st.vao.put();
	    if(st.ebo != null)
		st.ebo.put();
	    super.put();
	}
    }
    private final Map<Pair<GLVertexArray, GLBuffer>, VaoSetting> vaos = new CacheMap<>(CacheMap.RefType.WEAK);
    private VaoSetting getvao(GLVertexArray vao, GLBuffer ebo) {
	Pair<GLVertexArray, GLBuffer> key = new Pair<>(vao, ebo);
	VaoSetting ret = vaos.get(key);
	if(ret == null)
	    vaos.put(key, ret = new VaoSetting(vao, ebo));
	if(vao != null)
	    vao.get();
	if(ebo != null)
	    ebo.get();
	return(ret);
    }
    private final VaoSetting vao_nil = getvao(null, null);

    class SlotRender implements Render {
	final DrawSlot slot;
	private boolean done;

	SlotRender(DrawSlot slot) {
	    this.slot = slot;
	}

	public Environment env() {return(env);}
	public void draw(Pipe st, Model mod) {
	    if(done)
		throw(new IllegalStateException("Can only render once in drawlist"));
	    if(st != slot.bk.state())
		throw(new IllegalArgumentException("Must render with state from rendertree"));

	    BufferBGL gl = new BufferBGL(1);
	    if(GLVertexArray.ephemeralp(mod)) {
		throw(new NotImplemented("ephemeral models in drawlist"));
	    } else {
		GLVertexArray vao = env.prepare(mod, slot.prog);
		GLBuffer ebo;
		if(mod.ind == null) {
		    ebo = null;
		} else {
		    Disposable ro = env.prepare(mod.ind);
		    if(ro instanceof StreamBuffer)
			ebo = ((StreamBuffer)ro).rbuf;
		    else
			ebo = (GLBuffer)ro;
		}
		slot.settings[idx_vao] = getvao(vao, ebo);
		if(mod.ind == null) {
		    if(mod.ninst == 1)
			gl.glDrawArrays(GLRender.glmode(mod.mode), mod.f, mod.n);
		    else
			gl.glDrawArraysInstanced(GLRender.glmode(mod.mode), mod.f, mod.n, mod.ninst);
		} else {
		    if(mod.ninst == 1)
			gl.glDrawElements(GLRender.glmode(mod.mode), mod.n, GLRender.glindexfmt(mod.ind.fmt), mod.f * mod.ind.fmt.size);
		    else
			gl.glDrawElementsInstanced(GLRender.glmode(mod.mode), mod.n, GLRender.glindexfmt(mod.ind.fmt), mod.f * mod.ind.fmt.size, mod.ninst);
		}
		slot.main = gl;
	    }
	    done = true;
	}

	/* Somewhat unclear whether DrawList should implement
	 * these. Just implement it if and when it turns out to be
	 * reasonable. */
	public void submit(Render sub) {throw(new NotImplemented());}
	public void clear(Pipe pipe, FragData buf, FColor val) {throw(new NotImplemented());}
	public void clear(Pipe pipe, double val) {throw(new NotImplemented());}
	public void pget(Pipe pipe, FragData buf, Area area, VectorFormat fmt, Consumer<ByteBuffer> callback) {throw(new NotImplemented());}
	public void pget(Texture.Image img, VectorFormat fmt, Consumer<ByteBuffer> callback) {throw(new NotImplemented());}
	public void timestamp(Consumer<Long> callback) {throw(new NotImplemented());}
	public void fence(Runnable callback) {throw(new NotImplemented());}
	public <T extends DataBuffer> void update(T buf, DataBuffer.PartFiller<? super T> data, int from, int to) {throw(new NotImplemented());}
	public <T extends DataBuffer> void update(T buf, DataBuffer.Filler<? super T> data) {throw(new NotImplemented());}
	public void dispose() {}
    }

    private void verify(DrawSlot t) {
	if(t.tl != null) {
	    if(t.tl.tp != t)
		throw(new AssertionError(Long.toString(t.tl.sortid)));
	    if(order.compare(t.tl, t) >= 0)
		throw(new AssertionError(Long.toString(t.tl.sortid)));
	    verify(t.tl);
	}
	if(t.tr != null) {
	    if(t.tr.tp != t)
		throw(new AssertionError(Long.toString(t.tr.sortid)));
	    if(order.compare(t.tr, t) <= 0)
		throw(new AssertionError(Long.toString(t.tr.sortid)));
	    verify(t.tr);
	}
    }
    private void verify() {
	if(root != null) {
	    if(root.tp != null)
		throw(new AssertionError());
	    verify(root);
	}
    }

    public GLDrawList(GLEnvironment env) {
	this.env = env;
    }

    public static class ProgramMismatchException extends RuntimeException {
	public final GLProgram.Dump got, expected;
	public final Object pdump;

	public ProgramMismatchException(GLProgram got, GLProgram expected) {
	    super("unexpected program after immediate application");
	    this.got = got.dump();
	    this.expected = expected.dump();
	    this.pdump = expected.env.progdump();
	}
    }

    public void draw(Render r) {
	if(!(r instanceof GLRender))
	    throw(new IllegalArgumentException());
	GLRender g = (GLRender)r;
	if(!compatible(g.env))
	    throw(new IllegalArgumentException());
	synchronized(this) {
	    DrawSlot first = first(), last = null;
	    if(first == null)
		return;
	    try {
		settingbuf.get(0);
	    } catch(InterruptedException e) {
		boolean got = false;
		try {
		    got = settingbuf.get(5000);
		} catch(InterruptedException e2) {}
		Thread.currentThread().interrupt();
		if(!got)
		    throw(new RuntimeException("settingbuf wait timed out, dispatch thread stuck?", e));
	    }
	    g.state.apply(g.gl, first.bk.state());
	    g.state.apply(g.gl, VaoState.slot, ((VaoSetting)first.settings[idx_vao]).st);
	    if(g.state.prog() != first.prog)
		throw(new ProgramMismatchException(g.state.prog(), first.prog));
	    BGL gl = g.gl();
	    for(DrawSlot cur = first; cur != null; last = cur, cur = cur.next())
		gl.bglCallList(cur.compiled);
	    settingbuf.put(gl);
	    g.state.assume(last.bk.state());
	}
    }

    public void add(Slot<? extends Rendered> slot) {
	synchronized(this) {
	    if(disposed)
		throw(new IllegalStateException());
	    DrawSlot dslot = new DrawSlot(slot);
	    dslot.insert();
	    if(slotmap.put(slot, dslot) != null)
		throw(new AssertionError());
	}
    }

    public void remove(Slot<? extends Rendered> slot) {
	synchronized(this) {
	    DrawSlot dslot = slotmap.remove(slot);
	    if(dslot == null)
		throw(new IllegalStateException(String.format("removing non-present slot (%s)", slot.obj())));
	    dslot.remove();
	    dslot.dispose();
	}
    }

    public void update(Slot<? extends Rendered> slot) {
	synchronized(this) {
	    /* Handle exceptions from DrawSlot construction before
	     * removing previous slot. */
	    DrawSlot dslot = new DrawSlot(slot);
	    remove(slot);
	    dslot.insert();
	    if(slotmap.put(slot, dslot) != null)
		throw(new AssertionError());
	}
    }

    @SuppressWarnings("unchecked")
    private void orderupdate(Pipe group) {
	Object reg = orderidx.get(group);
	if(reg == null) {
	} else if(reg instanceof DrawSlot) {
	    ((DrawSlot)reg).orderupdate();
	} else if(reg instanceof List) {
	    List<DrawSlot> ls = (List<DrawSlot>)reg;
	    for(DrawSlot slot : ls)
		slot.orderupdate();
	} else {
	    throw(new RuntimeException());
	}
    }

    public void update(Pipe group, int[] mask) {
	synchronized(this) {
	    Object reg = psettings.get(group);
	    if(reg == null) {
	    } else if(reg instanceof DepSetting) {
		((DepSetting)reg).ckupdate(mask);
	    } else if(reg instanceof DepSetting[]) {
		for(DepSetting set : (DepSetting[])reg) {
		    if(set == null)
			break;
		    set.ckupdate(mask);
		}
	    } else {
		throw(new RuntimeException());
	    }

	    for(int i = 0; i < mask.length; i++) {
		if(mask[i] == Rendered.order.id)
		    orderupdate(group);
	    }
	}
    }

    public boolean compatible(Environment env) {
	return(env == this.env);
    }

    public void dispose() {
	synchronized(this) {
	    for(DrawSlot slot; (slot = root) != null; ) {
		slot.remove();
		slot.dispose();
	    }
	    disposed = true;
	}
    }

    protected void finalize() {
	dispose();
    }

    void treedump(java.io.PrintWriter out, DrawSlot slot) {
	if(slot == null) {
	    out.print("nil");
	    return;
	}
	out.print("(");
	out.print(slot.sortid);
	out.print(" ");
	treedump(out, slot.tl);
	out.print(" ");
	treedump(out, slot.tr);
	out.print(")");
    }
    String treedump(DrawSlot root) {
	java.io.StringWriter buf = new java.io.StringWriter();
	treedump(new java.io.PrintWriter(buf), root);
	return(buf.toString());
    }
    String treedump() {
	return(treedump(root));
    }

    public String stats() {
	return(String.format("%,d", btsubsize(root)));
    }
}
