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
import haven.render.*;
import haven.render.sl.*;
import static haven.Utils.eq;

public class Applier {
    public final GLEnvironment env;
    /* Soft pipeline state */
    private State[] cur = new State[0];
    /* Program state */
    private ShaderMacro[] shaders = new ShaderMacro[0];
    private int shash = 0;
    private GLProgram prog;
    private Object[] uvals = new Object[0];
    /* GL states */
    public GLState[] glstates = new GLState[GLState.slots.length];

    public Applier(GLEnvironment env) {
	this.env = env;
    }

    private void assume(Applier that) {
	this.cur = Arrays.copyOf(that.cur, that.cur.length);
	this.shaders = Arrays.copyOf(that.shaders, that.shaders.length);
	this.shash = that.shash;
	this.prog = that.prog;
	this.uvals = Arrays.copyOf(that.uvals, that.uvals.length);
	this.glstates = Arrays.copyOf(that.glstates, that.glstates.length);
    }

    public Applier clone() {
	Applier ret = new Applier(env);
	ret.assume(this);
	return(ret);
    }

    private void setprog(GLProgram prog) {
	this.prog = prog;
	this.uvals = new Object[(prog == null) ? 0 : prog.uniforms.length];
    }

    public GLProgram prog() {return(prog);}

    private <T> void uapply(BGL gl, GLProgram prog, int ui, Object val) {
	Uniform var = prog.uniforms[ui];
	if(val != uvals[ui]) {
	    UniformApplier.TypeMapping.apply(gl, prog, var, val);
	    uvals[ui] = val;
	}
    }

    private Object getuval(GLProgram prog, int ui, Pipe pipe) {
	Object val = prog.uniforms[ui].value.apply(pipe);
	if(val == null)
	    throw(new NullPointerException(String.format("tried to set null for uniform %s on %s", prog.uniforms[ui], pipe)));
	return(val);
    }

    private Object getfval(GLProgram prog, int fi, Pipe pipe) {
	return(prog.fragdata[fi].value.apply(pipe));
    }

    @SuppressWarnings("unchecked")
    private static <T extends State> void glpapply(GLEnvironment env, BGL gl, GLPipeState<T> st, State from, State to) {
	st.apply(env, gl, (T)from, (T)to);
    }

    private void assume(State[] ns) {
	int shash = 0, i;
	if(this.cur.length < ns.length) {
	    this.cur = Arrays.copyOf(this.cur, ns.length);
	    this.shaders = Arrays.copyOf(this.shaders, this.cur.length);
	}
	State[] cur = this.cur;
	ShaderMacro[] shaders = this.shaders;
	ShaderMacro[] nshaders = new ShaderMacro[shaders.length];
	for(i = 0; i < ns.length; i++) {
	    nshaders[i] = (ns[i] == null) ? null : ns[i].shader();
	    shash ^= System.identityHashCode(nshaders[i]);
	}
	GLProgram prog = env.getprog(shash, nshaders);
	Pipe tp = new BufPipe(ns);
	Object[] nuvals = new Object[prog.uniforms.length];
	for(i = 0; i < prog.uniforms.length; i++)
	    nuvals[i] = env.prepuval(getuval(prog, i, tp));
	Object[] nfvals = new Object[prog.fragdata.length];
	FragTarget[] nfconf = new FragTarget[prog.fragdata.length];
	for(i = 0; i < prog.fragdata.length; i++) {
	    Object fval = getfval(prog, i, tp);
	    if(fval instanceof FragTarget)
		fval = (nfconf[i] = (FragTarget)fval).buf;
	    else
		nfconf[i] = FboState.NIL_CONF;
	    nfvals[i] = env.prepfval(fval);
	}
	DepthBuffer sdbuf = (ns.length > DepthBuffer.slot.id) ? ((DepthBuffer)ns[DepthBuffer.slot.id]) : null;
	Object ndbuf = env.prepfval((sdbuf != null) ? sdbuf.image : null);

	this.shash = shash;
	setprog(prog);
	for(i = 0; i < ns.length; i++) {
	    cur[i] = ns[i];
	    shaders[i] = nshaders[i];
	}
	for(; i < cur.length; i++) {
	    cur[i] = null;
	    shaders[i] = null;
	}
	for(i = 0; i < prog.uniforms.length; i++)
	    uvals[i] = nuvals[i];
	FboState.set(null, this, ndbuf, nfvals, nfconf);
    }

    private void apply2(BGL gl, State[] ns, Pipe to) {
	if(this.cur.length < ns.length) {
	    this.cur = Arrays.copyOf(this.cur, ns.length);
	    this.shaders = Arrays.copyOf(this.shaders, this.cur.length);
	}
	State[] cur = this.cur;
	ShaderMacro[] shaders = this.shaders;
	int[] pdirty = new int[cur.length];
	int pn = 0;
	{
	    int i = 0;
	    for(; i < ns.length; i++) {
		if(!eq(ns[i], cur[i]))
		    pdirty[pn++] = i;
	    }
	    for(; i < cur.length; i++) {
		if(cur[i] != null)
		    pdirty[pn++] = i;
	    }
	}
	if(pn == 0)
	    return;
	int shash = this.shash;
	int[] sdirty = new int[cur.length];
	ShaderMacro[] nshaders = new ShaderMacro[cur.length];
	int sn = 0;
	for(int i = 0; i < pn; i++) {
	    int slot = pdirty[i];
	    State s = (slot < ns.length) ? ns[slot] : null;
	    ShaderMacro nm = ((s == null) ? null : s.shader());
	    if(nm != shaders[slot]) {
		shash ^= System.identityHashCode(shaders[slot]) ^ System.identityHashCode(nm);
		sdirty[sn] = pdirty[i];
		nshaders[sn] = nm;
		sn++;
	    }
	}
	GLProgram prog = this.prog;
	if(sn > 0) {
	    ShaderMacro[] gshaders = Arrays.copyOf(shaders, shaders.length);
	    for(int i = 0; i < sn; i++)
		gshaders[sdirty[i]] = nshaders[i];
	    prog = env.getprog(shash, gshaders);
	}
	int[] udirty = new int[prog.uniforms.length];
	int un = 0;
	boolean fdirty = false;
	if(prog == this.prog) {
	    boolean[] ch = new boolean[prog.uniforms.length];
	    for(int i = 0; i < pn; i++) {
		if((prog.umap.length > pdirty[i]) && (prog.umap[pdirty[i]] != null)) {
		    for(int ui : prog.umap[pdirty[i]]) {
			if(!ch[ui]) {
			    udirty[un++] = ui;
			    ch[ui] = true;
			}
		    }
		}
		if((prog.fmap.length > pdirty[i]) && prog.fmap[pdirty[i]])
		    fdirty = true;
	    }
	} else {
	    un = udirty.length;
	    for(int i = 0; i < udirty.length; i++)
		udirty[i] = i;
	    if(((prog == null) != (this.prog == null)) || (prog.fragdata.length != this.prog.fragdata.length))
		fdirty = true;
	    if(!fdirty) {
		for(int i = 0; i < prog.fragdata.length; i++) {
		    if(prog.fragdata[i] != this.prog.fragdata[i]) {
			fdirty = true;
			break;
		    }
		}
	    }
	    if(!fdirty) {
		for(int i = 0; i < pn; i++) {
		    if((prog.fmap.length > pdirty[i]) && prog.fmap[pdirty[i]]) {
			fdirty = true;
			break;
		    }
		}
	    }
	}
	Object[] nuvals = new Object[un];
	for(int i = 0; i < un; i++) {
	    int ui = udirty[i];
	    nuvals[i] = env.prepuval(getuval(prog, ui, to));
	}
	Object[] nfvals = null;
	FragTarget[] nfconf = null;
	Object ndbuf = null;
	if(fdirty) {
	    int fn = (prog == null) ? 0 : prog.fragdata.length;
	    nfvals = new Object[fn];
	    nfconf = new FragTarget[fn];
	    for(int i = 0; i < fn; i++) {
		Object fval = getfval(prog, i, to);
		if(fval instanceof FragTarget)
		    fval = (nfconf[i] = (FragTarget)fval).buf;
		else
		    nfconf[i] = FboState.NIL_CONF;
		nfvals[i] = env.prepfval(fval);
	    }
	    DepthBuffer sdbuf = to.get(DepthBuffer.slot);
	    ndbuf = env.prepfval((sdbuf == null) ? null : sdbuf.image);
	}

	for(int i = 0; i < pn; i++) {
	    int slot = pdirty[i];
	    if((slot < GLPipeState.matching.length) && (GLPipeState.matching[slot] != null))
		glpapply(env, gl, GLPipeState.matching[slot], (slot < cur.length) ? cur[slot] : null, (slot < ns.length) ? ns[slot] : null);
	    cur[slot] = (slot < ns.length) ? ns[slot] : null;
	}
	for(int i = 0; i < sn; i++)
	    shaders[sdirty[i]] = nshaders[i];
	this.shash = shash;
	if(prog != this.prog) {
	    GLProgram.apply(gl, this.prog, prog);
	    setprog(prog);
	}
	for(int i = 0; i < un; i++)
	    uapply(gl, prog, udirty[i], nuvals[i]);
	if(fdirty)
	    FboState.set(gl, this, ndbuf, nfvals, nfconf);
    }

    public void assume(Pipe to) {
	assume(to.states());
    }

    public void apply(BGL gl, Pipe to) {
	State[] ns = to.states();
	if(gl == null) {
	    assume(ns);
	    return;
	}
	apply2(gl, ns, to);
    }

    public void apply(BGL gl, int slot, GLState st) {
	if(gl == null) {
	    glstates[slot] = st;
	    return;
	}
	GLState cur = glstates[slot];
	if((cur == null) && (st != null)) {
	    st.apply(gl);
	    glstates[slot] = st;
	} else if((cur != null) && (st == null)) {
	    cur.unapply(gl);
	    glstates[slot] = null;
	} else if ((cur != null) && (st != null)) {
	    cur.applyto(gl, st);
	    glstates[slot] = st;
	}
    }

    public void apply(BGL gl, GLState st) {
	apply(gl, st.slotidx(), st);
    }

    public void apply(BGL gl, Applier that) {
	if(gl == null) {
	    assume(that);
	    return;
	}
	if(this.cur.length < that.cur.length) {
	    this.cur = Arrays.copyOf(this.cur, that.cur.length);
	    this.shaders = Arrays.copyOf(this.shaders, this.cur.length);
	}
	int i;
	for(i = 0; i < this.glstates.length; i++)
	    apply(gl, i, that.glstates[i]);
	for(GLPipeState<?> glp : GLPipeState.all) {
	    State a = (glp.slot.id < this.cur.length) ? this.cur[glp.slot.id] : null;
	    State b = (glp.slot.id < that.cur.length) ? that.cur[glp.slot.id] : null;
	    if(!eq(a, b))
		glpapply(env, gl, glp, a, b);
	}
	if(this.prog != that.prog) {
	    GLProgram.apply(gl, this.prog, that.prog);
	    this.shash = that.shash;
	    setprog(that.prog);
	}
	for(i = 0; i < that.cur.length; i++) {
	    this.cur[i] = that.cur[i];
	    this.shaders[i] = that.shaders[i];
	}
	for(; i < this.cur.length; i++) {
	    this.cur[i] = null;
	    this.shaders[i] = null;
	}
	if(prog != null) {
	    for(i = 0; i < prog.uniforms.length; i++) {
		if(this.uvals[i] != that.uvals[i]) {
		    uapply(gl, prog, i, that.uvals[i]);
		}
	    }
	}
    }
}
