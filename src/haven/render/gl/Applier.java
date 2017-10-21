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

    public Applier clone() {
	Applier ret = new Applier(env);
	ret.cur = Arrays.copyOf(this.cur, this.cur.length);
	ret.shaders = Arrays.copyOf(this.shaders, this.shaders.length);
	ret.shash = this.shash;
	ret.prog = this.prog;
	ret.uvals = Arrays.copyOf(this.uvals, this.uvals.length);
	ret.glstates = Arrays.copyOf(this.glstates, this.glstates.length);
	return(ret);
    }

    private void setprog(GLProgram prog) {
	this.prog = prog;
	this.uvals = new Object[prog.uniforms.length];
    }

    public GLProgram prog() {return(prog);}

    private <T> void uapply(BGL gl, GLProgram prog, int ui, Object val) {
	Uniform var = prog.uniforms[ui];
	if(val != uvals[ui]) {
	    UniformApplier.TypeMapping.apply(gl, var.type, prog.uniform(var), val);
	    uvals[ui] = val;
	}
    }

    private Object getuval(GLProgram prog, int ui, Pipe pipe) {
	Object val = prog.uniforms[ui].value.apply(pipe);
	if(val == null)
	    throw(new NullPointerException(String.format("tried to set null for uniform %s on %s", prog.uniforms[ui], pipe)));
	return(val);
    }

    private Object prepuval(Object val) {
	return(val);
    }

    @SuppressWarnings("unchecked")
    private static <T extends State> void glpapply(BGL gl, GLPipeState<T> st, State from, State to) {
	st.apply(gl, (T)from, (T)to);
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
	Object[] nuvals = new Object[prog.uniforms.length];
	Pipe tp = new Pipe(ns);
	for(i = 0; i < prog.uniforms.length; i++)
	    nuvals[i] = prepuval(getuval(prog, i, tp));

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
	if(prog == this.prog) {
	    boolean[] ch = new boolean[prog.uniforms.length];
	    for(int i = 0; i < pn; i++) {
		if((prog.umap.length <= pdirty[i]) || prog.umap[pdirty[i]] == null)
		    continue;
		for(int ui : prog.umap[pdirty[i]]) {
		    if(!ch[ui]) {
			udirty[un++] = ui;
			ch[ui] = true;
		    }
		}
	    }
	} else {
	    un = udirty.length;
	    for(int i = 0; i < udirty.length; i++)
		udirty[i] = i;
	}
	Object[] nuvals = new Object[un];
	for(int i = 0; i < un; i++) {
	    int ui = udirty[i];
	    nuvals[i] = prepuval(getuval(prog, ui, to));
	}

	for(int i = 0; i < pn; i++) {
	    int slot = pdirty[i];
	    if((slot < GLPipeState.matching.length) && (GLPipeState.matching[slot] != null))
		glpapply(gl, GLPipeState.matching[slot], cur[slot], ns[slot]);
	    cur[slot] = ns[slot];
	}
	for(int i = 0; i < sn; i++)
	    shaders[sdirty[i]] = nshaders[i];
	this.shash = shash;
	if(prog != this.prog) {
	    setprog(prog);
	    prog.apply(gl);
	}
	for(int i = 0; i < un; i++)
	    uapply(gl, prog, udirty[i], nuvals[i]);
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
	if(gl == null)
	    throw(new NullPointerException());
	if(this.cur.length < that.cur.length) {
	    this.cur = Arrays.copyOf(this.cur, that.cur.length);
	    this.shaders = Arrays.copyOf(this.shaders, this.cur.length);
	}
	int i;
	for(i = 0; i < this.glstates.length; i++)
	    apply(gl, i, that.glstates[i]);
	for(GLPipeState<?> glp : GLPipeState.all) {
	    if(!eq(this.cur[glp.slot.id], that.cur[glp.slot.id]))
		glpapply(gl, glp, this.cur[glp.slot.id], that.cur[glp.slot.id]);
	}
	if(this.prog != that.prog) {
	    this.shash = that.shash;
	    setprog(that.prog);
	    that.prog.apply(gl);
	}
	for(i = 0; i < that.cur.length; i++) {
	    this.cur[i] = that.cur[i];
	    this.shaders[i] = that.shaders[i];
	}
	for(; i < this.cur.length; i++) {
	    this.cur[i] = null;
	    this.shaders[i] = null;
	}
	for(i = 0; i < prog.uniforms.length; i++) {
	    if(this.uvals[i] != that.uvals[i]) {
		uapply(gl, prog, i, that.uvals[i]);
	    }
	}
    }
}
