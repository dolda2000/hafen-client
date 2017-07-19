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
import java.io.*;
import haven.Disposable;
import haven.render.*;
import haven.render.sl.*;

public class GLProgram implements Disposable {
    public static boolean dumpall = true;
    public final String vsrc, fsrc;
    public final Uniform[] uniforms;
    public final int[][] umap;

    public GLProgram(ProgramContext ctx) {
	{
	    StringWriter buf = new StringWriter();
	    ctx.fctx.construct(buf);
	    fsrc = buf.toString();
	}
	{
	    StringWriter buf = new StringWriter();
	    ctx.vctx.construct(buf);
	    vsrc = buf.toString();
	}
	Uniform[] uniforms = ctx.uniforms.toArray(new Uniform[0]);
	int[][] umap = new int[0][];
	for(int i = 0; i < uniforms.length; i++) {
	    for(State.Slot slot : uniforms[i].deps) {
		if(umap.length <= slot.id)
		    umap = Arrays.copyOf(umap, slot.id + 1);
		umap[slot.id] = (umap[slot.id] == null) ? new int[1] : Arrays.copyOf(umap[slot.id], umap[slot.id].length + 1);
		umap[slot.id][umap[slot.id].length - 1] = i;
	    }
	}
	this.uniforms = uniforms;
	this.umap = umap;
    }

    public static GLProgram build(Collection<ShaderMacro> mods) {
	ProgramContext prog = new ProgramContext();
	for(ShaderMacro mod : mods)
	    mod.modify(prog);
	GLProgram ret = new GLProgram(prog);
	if(dumpall || prog.dump) {
	    System.err.println(mods + ":");
	    System.err.println("---> Vertex shader:");
	    System.err.print(ret.vsrc);
	    System.err.println("---> Fragment shader:");
	    System.err.print(ret.fsrc);
	    System.err.println();
	    System.err.println("-------- " + ret);
	    System.err.println();
	}
	return(ret);
    }

    public void dispose() {
    }
}
