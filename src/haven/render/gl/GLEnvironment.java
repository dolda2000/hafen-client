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
import javax.media.opengl.*;
import haven.Utils;
import haven.render.*;
import haven.render.sl.*;

public class GLEnvironment implements Environment {
    public final GLContext ctx;
    private BGL prep = null;
    final Object drawmon = new Object();
    final Object prepmon = new Object();
    private final Pipe state = new Pipe();

    public GLEnvironment(GLContext ctx) {
	this.ctx = ctx;
    }

    public GLRender render() {
	return(new GLRender(this));
    }

    void prepare(GLObject obj) {
	synchronized(prepmon) {
	    if(prep == null)
		prep = new BufferBGL();
	    prep.bglCreate(obj);
	}
    }

    static class SavedProg {
	final int hash;
	final ShaderMacro[] shaders;
	final GLProgram prog;
	SavedProg next;
	boolean used = true;

	SavedProg(int hash, ShaderMacro[] shaders, GLProgram prog) {
	    this.hash = hash;
	    this.shaders = Arrays.copyOf(shaders, shaders.length);
	    this.prog = prog;
	}
    }

    private final Object pmon = new Object();
    private SavedProg[] ptab = new SavedProg[32];
    private int nprog = 0;
    private SavedProg findprog(int hash, ShaderMacro[] shaders) {
	int idx = hash & (ptab.length - 1);
	outer: for(SavedProg s = ptab[idx]; s != null; s = s.next) {
	    if(s.hash != hash)
		continue;
	    ShaderMacro[] a, b;
	    if(shaders.length < s.shaders.length) {
		a = shaders; b = s.shaders;
	    } else {
		a = s.shaders; b = shaders;
	    }
	    int i = 0;
	    for(; i < a.length; i++) {
		if(a[i] != b[i])
		    continue outer;
	    }
	    for(; i < b.length; i++) {
		if(b[i] != null)
		    continue outer;
	    }
	    return(s);
	}
	return(null);
    }

    private void rehash(int nlen) {
	SavedProg[] ntab = new SavedProg[nlen];
	for(int i = 0; i < ptab.length; i++) {
	    while(ptab[i] != null) {
		SavedProg s = ptab[i];
		ptab[i] = s.next;
		int ni = s.hash & (nlen - 1);
		s.next = ntab[ni];
		ntab[ni] = s;
	    }
	}
	ptab = ntab;
    }

    private void putprog(int hash, ShaderMacro[] shaders, GLProgram prog) {
	int idx = hash & (ptab.length - 1);
	SavedProg save = new SavedProg(hash, shaders, prog);
	save.next = ptab[idx];
	ptab[idx] = save;
	nprog++;
	if(nprog > ptab.length)
	    rehash(ptab.length * 2);
    }

    public GLProgram getprog(int hash, ShaderMacro[] shaders) {
	synchronized(pmon) {
	    SavedProg s = findprog(hash, shaders);
	    if(s != null) {
		s.used = true;
		return(s.prog);
	    }
	}
	Collection<ShaderMacro> mods = new LinkedList<>();
	for(int i = 0; i < shaders.length; i++) {
	    if(shaders[i] != null)
		mods.add(shaders[i]);
	}
	GLProgram prog = GLProgram.build(this, mods);
	synchronized(pmon) {
	    SavedProg s = findprog(hash, shaders);
	    if(s != null) {
		prog.dispose();
		s.used = true;
		return(s.prog);
	    }
	    putprog(hash, shaders, prog);
	    return(prog);
	}
    }

    private void cleanprogs() {
	synchronized(pmon) {
	    for(int i = 0; i < ptab.length; i++) {
		SavedProg c, p;
		for(c = ptab[i], p = null; c != null; c = c.next) {
		    if(c.used) {
			c.used = false;
			p = c;
		    } else {
			if(p == null)
			    ptab[i] = c.next;
			else
			    p.next = c.next;
			c.prog.dispose();
			nprog--;
		    }
		}
	    }
	    /* XXX: Rehash into smaller table? It's probably not a
	     * problem, but it might be nice just for
	     * completeness. */
	}
    }

    private double lastpclean = Utils.rtime();
    public void clean() {
	double now = Utils.rtime();
	if(now - lastpclean > 60) {
	    cleanprogs();
	    lastpclean = now;
	}
    }

    public int numprogs() {return(nprog);}
}
