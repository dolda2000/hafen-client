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
import javax.media.opengl.*;

public class GLConfig implements java.io.Serializable, Console.Directory {
    public boolean usedl = true, fsaa = false;
    public int maxlights;
    public Collection<String> exts;
    public transient GLCapabilities caps;
    public boolean shuse;
    public transient GLState deflight = Light.vlights;
    
    private GLConfig() {
    }
    
    public static GLConfig fromgl(GL gl, GLContext ctx, GLCapabilities caps) {
	GLConfig c = new GLConfig();
	int[] buf = {0};
	gl.glGetIntegerv(GL.GL_MAX_LIGHTS, buf, 0);
	c.maxlights = buf[0];
	c.exts = Arrays.asList(gl.glGetString(GL.GL_EXTENSIONS).split(" "));
	c.caps = caps;
	c.shuse = c.haveglsl();
	return(c);
    }
    
    public boolean havefsaa() {
	return(exts.contains("GL_ARB_multisample") && caps.getSampleBuffers());
    }
    
    public boolean haveglsl() {
	return(exts.contains("GL_ARB_fragment_shader") && exts.contains("GL_ARB_vertex_shader"));
    }
    
    private transient Map<String, Console.Command> cmdmap = new TreeMap<String, Console.Command>();
    {
	cmdmap.put("gl", new Console.Command() {
		public void run(Console cons, String[] args) throws Exception {
		    if(args.length >= 2) {
			String var = args[1].intern();
			if(var == "usedl") {
			    usedl = Utils.parsebool(args[2], false);
			} else if(var == "fsaa") {
			    boolean fsaa = Utils.parsebool(args[2], false);
			    if(fsaa && !havefsaa())
				throw(new Exception("FSAA not supported."));
			    GLConfig.this.fsaa = fsaa;
			} else if(var == "shuse") {
			    boolean shuse = Utils.parsebool(args[2], false);
			    if(shuse && !haveglsl())
				throw(new Exception("GLSL not supported."));
			    GLConfig.this.shuse = shuse;
			} else if(var == "light") {
			    if(args[2].equals("vlight")) {
				deflight = Light.vlights;
			    } else if(args[2].equals("plight")) {
				if(!shuse)
				    throw(new Exception("Per-pixel lighting requires shader usage."));
				deflight = Light.plights;
			    } else if(args[2].equals("pslight")) {
				if(!shuse)
				    throw(new Exception("Per-pixel lighting requires shader usage."));
				deflight = Light.pslights;
			    } else if(args[2].equals("vcel")) {
				if(!shuse)
				    throw(new Exception("Cel-shading requires shader usage."));
				deflight = Light.vcel;
			    } else if(args[2].equals("pcel")) {
				if(!shuse)
				    throw(new Exception("Cel-shading requires shader usage."));
				deflight = Light.pcel;
			    } else {
				throw(new Exception("No such light setting: " + args[2]));
			    }
			} else {
			    throw(new Exception("No such setting: " + var));
			}
		    }
		}
	    });
    }
    public Map<String, Console.Command> findcmds() {
	return(cmdmap);
    }
}
