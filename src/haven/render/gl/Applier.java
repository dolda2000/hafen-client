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
    private State[] cur = new State[0];
    private ShaderMacro[] shaders = new ShaderMacro[0];
    private int shash = 0;
    private GLProgram prog;

    private Applier(GLEnvironment env) {
	this.env = env;
    }

    public Applier(GLEnvironment env, Pipe init) {
	this.env = env;
	assume(init.states());
    }

    public Applier clone() {
	Applier ret = new Applier(env);
	ret.cur = Arrays.copyOf(this.cur, this.cur.length);
	ret.shaders = Arrays.copyOf(this.shaders, this.shaders.length);
	ret.shash = this.shash;
	ret.prog = this.prog;
	return(ret);
    }

    private void assume(State[] ns) {
	int hash = 0, i;
	if(this.cur.length < ns.length) {
	    this.cur = Arrays.copyOf(this.cur, ns.length);
	    this.shaders = Arrays.copyOf(this.shaders, this.cur.length);
	}
	State[] cur = this.cur;
	ShaderMacro[] shaders = this.shaders;
	for(i = 0; i < ns.length; i++) {
	    cur[i] = ns[i];
	    ShaderMacro shader = (cur[i] == null) ? null : cur[i].shader();
	    if(shader != shaders[i]) {
		hash ^= System.identityHashCode(shaders[i]);
		shaders[i] = shader;
		hash ^= System.identityHashCode(shaders[i]);
	    }
	}
	for(; i < cur.length; i++) {
	    cur[i] = null;
	    if(shaders[i] != null) {
		hash ^= System.identityHashCode(shaders[i]);
		shaders[i] = null;
	    }
	}
	this.shash = hash;
	this.prog = env.getprog(hash, shaders);
    }

    public void apply(BGL gl, Pipe to) {
	State[] ns = to.states();
	if(this.cur.length < ns.length) {
	    this.cur = Arrays.copyOf(this.cur, ns.length);
	    this.shaders = Arrays.copyOf(this.shaders, this.cur.length);
	}
	State[] cur = this.cur;
	ShaderMacro[] shaders = this.shaders;
	int[] ch = new int[ns.length];
	int n = 0;
	{
	    int i = 0;
	    for(; i < ns.length; i++) {
		if(!eq(ns[i], cur[i]))
		    ch[n++] = i;
	    }
	    for(; i < cur.length; i++) {
		if(cur[i] != null)
		    ch[n++] = i;
	    }
	}
	if(n == 0)
	    return;
	int shash = this.shash;
	for(int i = 0; i < n; i++) {
	    State s = (ch[i] < ns.length) ? ns[ch[i]] : null;
	    ShaderMacro nm = ((s == null) ? null : s.shader());
	    if(nm != shaders[ch[i]]) {
		shash ^= System.identityHashCode(shaders[ch[i]]);
		shaders[ch[i]] = nm;
		shash ^= System.identityHashCode(nm);
	    }
	}
	this.shash = shash;
	this.prog = env.getprog(shash, shaders);
    }
}
