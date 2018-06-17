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
import java.util.concurrent.atomic.*;
import haven.FColor;
import haven.render.*;
import haven.render.sl.*;

public class GLDrawList implements DrawList {
    public static final int idx_vao = 0;
    public static final int idx_fbo = 1;
    public static final int idx_pst = 2;
    public static final int idx_uni = idx_pst + GLPipeState.all.length;
    public final GLEnvironment env;
    private final Map<SettingKey, Setting> settings = new HashMap<>();
    private DrawSlot root = null;

    private static int btheight(DrawSlot s) {
	return((s == null) ? 0 : s.th);
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
		return((a.sortid < b.sortid) ? -1 : 1);
	    }
	};
    private static AtomicLong uniqid = new AtomicLong();
    private class DrawSlot {
	/* List structure */
	final long sortid;
	DrawSlot tp, tl, tr;
	int th = 0;

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
	    return(th = (Math.max(btheight(tl), btheight(tr)) + 1));
	}

	private void bbtrl() {
	    if(btheight(tr.tl) > btheight(tr.tr))
		tr.bbtrr();
	    DrawSlot p = tp, r = tr, rl = r.tl;
	    (tr = rl).tp = this;
	    setheight();
	    (r.tl = this).tp = r;
	    r.setheight();
	    if(p == null)
		(root = r).tp = null;
	    else if(p.tl == this)
		(p.tl = r).tp = p;
	    else
		(p.tr = r).tp = p;
	}
	private void bbtrr() {
	    if(btheight(tl.tr) > btheight(tl.tl))
		tl.bbtrl();
	    DrawSlot p = tp, l = tl, lr = l.tr;
	    (tl = lr).tp = this;
	    setheight();
	    (l.tr = this).tp = l;
	    l.setheight();
	    if(p == null)
		(root = l).tp = null;
	    else if(p.tl == this)
		(p.tl = l).tp = p;
	    else
		(p.tr = l).tp = p;
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
	void insert() {
	    if((tp != null) || (root == this))
		throw(new IllegalStateException());
	    th = 1;
	    if(root == null) {
		root = this;
	    } else {
		root.insert(this);
	    }
	}
	void remove() {
	    if((tp == null) && (root != this))
		throw(new IllegalStateException());
	    DrawSlot rep;
	    if((tl != null) && (tr != null)) {
		for(rep = tr; rep.tl != null; rep = rep.tl);
		if(rep.tr != null) {
		    DrawSlot p = rep.tp;
		    if(p.tl == rep)
			(p.tl = rep.tr).tp = p;
		    else
			(p.tr = rep.tr).tp = p;
		}
		(rep.tl = tl).tp = rep;
		(rep.tr = tr).tp = rep;
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
	    if(tp != null) {
		for(DrawSlot p = tp, pp = p.tp; p != null; pp = (p = pp).tp) {
		    if(btheight(p.tl) > btheight(p.tr) + 1)
			p.bbtrr();
		    else if(btheight(p.tr) > btheight(p.tl) + 1)
			p.bbtrl();
		}
	    }
	    tr = tl = tp = null;
	}

	/* Render information */
	final Slot<Rendered> bk;
	final GLProgram prog;
	final Setting[] settings;

	private GLProgram progfor(Slot<Rendered> sl) {
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
	    settings[idx_fbo] = getframe(prog, bst);
	    for(int i = 0; i < GLPipeState.all.length; i++)
		settings[idx_pst + i] = getpipest(GLPipeState.all[i], bst);
	    for(int i = 0; i < prog.uniforms.length; i++)
		settings[idx_uni + i] = getuniform(prog, prog.uniforms[i], bst);
	}

	DrawSlot(Slot<Rendered> bk) {
	    this.sortid = uniqid.getAndIncrement();
	    this.bk = bk;
	    this.prog = progfor(bk);
	    this.settings = new Setting[idx_uni + prog.uniforms.length];
	    getsettings();
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

	@Override public int hashCode() {
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

	@Override public boolean equals(Object o) {
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

    static Pipe[] makedepid(GroupPipe state, Collection<State.Slot<?>> deps) {
	Iterator<State.Slot<?>> it = deps.iterator();
	if(!it.hasNext())
	    return(new Pipe[0]);
	Pipe[] grp = state.groups();
	int[] gids = state.gstates();
	int one = gids[it.next().id];
	int ni = 1;
	while(it.hasNext()) {
	    int cid = it.next().id;
	    if(gids[cid] != one) {
		Pipe[] ret = new Pipe[deps.size()];
		for(int i = 0; i < ni; i++)
		    ret[i] = grp[one];
		ret[ni++] = grp[gids[cid]];
		while(it.hasNext())
		    ret[ni++] = grp[gids[it.next().id]];
		return(ret);
	    }
	    ni++;
	}
	return(new Pipe[] {grp[one]});
    }

    abstract class Setting {
	final SettingKey key;
	int rc;

	Setting(SettingKey key) {
	    this.key = key;
	}

	abstract void compile(BGL gl);
	abstract State.Slot[] depslots();

	Pipe compstate() {
	    if(key.depid_1 != null)
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

    class FrameSetting extends Setting {
	final GLProgram prog;

	FrameSetting(SettingKey key) {
	    super(key);
	    this.prog = key.prog;
	}

	void compile(BGL gl) {
	    Pipe pipe = compstate();
	    Object depth = env.prepfval(pipe.get(DepthBuffer.slot));
	    Object[] fvals = new Object[prog.fragdata.length];
	    for(int i = 0; i < fvals.length; i++)
		fvals[i] = env.prepfval(prog.fragdata[i].value.apply(pipe));
	    FboState.make(env, depth, fvals).apply(gl);
	}

	State.Slot[] depslots() {
	    return(progfslots(prog));
	}
    }

    Setting getframe(GLProgram prog, GroupPipe state) {
	SettingKey key = new SettingKey(prog, FboState.class, makedepid(state, Arrays.asList(progfslots(prog))));
	Setting ret = settings.get(key);
	if(ret == null) {
	    ret = new FrameSetting(key);
	    settings.put(key, ret);
	}
	ret.rc++;
	return(ret);
    }

    class PipeSetting<T extends State> extends Setting {
	final GLPipeState<T> setting;

	PipeSetting(SettingKey key, GLPipeState<T> setting) {
	    super(key);
	    this.setting = setting;
	}

	void compile(BGL gl) {
	    T st = compstate().get(setting.slot);
	    setting.apply(gl, st);
	}

	State.Slot[] depslots() {
	    return(new State.Slot[] {setting.slot});
	}
    }

    Setting getpipest(GLPipeState<?> pst, GroupPipe state) {
	SettingKey key = new SettingKey(null, pst, makedepid(state, Arrays.asList(pst.slot)));
	Setting ret = settings.get(key);
	if(ret == null) {
	    ret = new PipeSetting<>(key, pst);
	    settings.put(key, ret);
	}
	ret.rc++;
	return(ret);
    }

    class UniformSetting extends Setting {
	final GLProgram prog;
	final Uniform var;

	UniformSetting(SettingKey key) {
	    super(key);
	    this.prog = key.prog;
	    this.var = (Uniform)key.vid;
	}

	void compile(BGL gl) {
	    Object val = env.prepuval(var.value.apply(compstate()));
	    UniformApplier.TypeMapping.apply(gl, prog, var, val);
	}

	State.Slot[] depslots() {
	    return(var.deps.toArray(new State.Slot[key.depid_v.length]));
	}
    }

    Setting getuniform(GLProgram prog, Uniform var, GroupPipe state) {
	SettingKey key = new SettingKey(prog, var, makedepid(state, var.deps));
	Setting ret = settings.get(key);
	if(ret == null) {
	    ret = new UniformSetting(key);
	    settings.put(key, ret);
	}
	ret.rc++;
	return(ret);
    }

    private void verify(DrawSlot t) {
	if(t.tl != null) {
	    if(t.tl.tp != t)
		throw(new AssertionError());
	    if(order.compare(t.tl, t) >= 0)
		throw(new AssertionError());
	    verify(t.tl);
	}
	if(t.tr != null) {
	    if(t.tr.tp != t)
		throw(new AssertionError());
	    if(order.compare(t.tr, t) <= 0)
		throw(new AssertionError());
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

    class SlotRender implements Render {
	final BGL gl;
	private boolean done;

	SlotRender(BGL gl) {
	    this.gl = gl;
	}

	public Environment env() {return(env);}
	public void draw(Pipe st, Model mod) {
	    if(done)
		throw(new IllegalStateException("Can only render once in drawlist"));
	    if(GLVertexArray.ephemeralp(mod)) {
		throw(new NotImplemented("ephemeral models in drawlist"));
	    } else {
	    }
	    done = true;
	}

	/* Somewhat unclear whether DrawList should implement
	 * clearing. Just implement it if and when it turns out to be
	 * reasonable. */
	public void clear(Pipe pipe, FragData buf, FColor val) {throw(new NotImplemented());}
	public void clear(Pipe pipe, double val) {throw(new NotImplemented());}
	public void dispose() {}
    }

    public GLDrawList(GLEnvironment env) {
	this.env = env;
    }

    public void draw(Render r) {
    }

    public void add(Slot<Rendered> slot) {
	
    }

    public void remove(Slot<Rendered> slot) {
    }

    public void update(Slot<Rendered> slot) {
    }

    public void update(Pipe group) {
    }

    public void dispose() {
    }
}
