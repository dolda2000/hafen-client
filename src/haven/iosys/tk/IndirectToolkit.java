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

package haven.iosys.tk;

import java.util.*;
import java.nio.*;
import haven.*;
import haven.iosys.*;
import haven.render.*;
import java.awt.image.BufferedImage;

@Toolkit.Available(name = "indirect")
public class IndirectToolkit implements Toolkit {
    public final Acephal backend;
    public final Toolkit frontend;

    public IndirectToolkit(Acephal backend, Toolkit frontend) {
	this.backend = backend;
	this.frontend = frontend;
    }

    public class IndirectWindow implements Windeye {
	public final Windeye bk;
	private Coord bufsz = null;
	private Texture2D col = null;
	private Texture2D dpt = null;
	private Pipe.Op fbstate = null;
	private ByteBuffer xfbuf = null;
	private boolean prevdone = true;

	public IndirectWindow() {
	    this.bk = frontend.window();
	}

	public void add(EventListener l) {
	    bk.add(l);
	}

	public Toolkit toolkit() {return(IndirectToolkit.this);}

	public Windeye show(boolean st) {bk.show(st); return(this);}
	public Windeye title(String text) {bk.title(text); return(this);}
	public Windeye icon(BufferedImage img) {bk.icon(img); return(this);}
	public Windeye cursor(Cursor curs) {bk.cursor(curs); return(this);}
	public Windeye sizing(Sizing sz) {bk.sizing(sz); return(this);}
	public Windeye state(State st) {bk.state(st); return(this);}
	public Windeye drophandler(DropHandler h) {bk.drophandler(h); return(this);}

	public Coord size() {return(bk.size());}
	public State state() {return(bk.state());}
	public boolean focused() {return(bk.focused());}
	public Visibility visible() {return(bk.visible());}
	public Clipboard clipboard(Object id) {return(bk.clipboard(id));}
	public void stats(Collection<String> buf) {bk.stats(buf);}

	public Environment env() {
	    return(backend.env());
	}

	public Pipe.Op fbstate() {
	    Coord size = bk.size();
	    if(!Utils.eq(bufsz, bk.size())) {
		if(col != null) {
		    col.dispose();
		    dpt.dispose();
		}
		fbstate = Pipe.Op.compose(new FragColor<>((col = new Texture2D(size, DataBuffer.Usage.STATIC, new VectorFormat(4, NumberFormat.UNORM8), null)).image(0)),
					  new DepthBuffer<>((dpt = new Texture2D(size, DataBuffer.Usage.STATIC, Texture.DEPTH, new VectorFormat(1, NumberFormat.FLOAT32), null)).image(0)));
		xfbuf = ByteBuffer.allocateDirect(size.x * size.y * 4).order(ByteOrder.nativeOrder());
		bufsz = size;
	    }
	    return(fbstate);
	}

	public void swapbuffers(Render out, Object mode) {
	    Coord size = bufsz;
	    if(size == null)
		return;
	    out.fence(() -> {
		try {
		    synchronized(this) {
			while(!prevdone)
			    wait();
		    }
		    prevdone = false;
		} catch(InterruptedException e) {
		    Thread.currentThread().interrupt();
		}
	    });
	    Area wnd = Area.sized(size);
	    out.pget(col.image(0), new VectorFormat(4, NumberFormat.UNORM8), xfbuf, buf -> {
		Render fout = bk.env().render();
		Texture2D fbuf = new Texture2D(size, DataBuffer.Usage.STATIC, new VectorFormat(4, NumberFormat.UNORM8), DataBuffer.Filler.of(buf));
		Pipe base = new BufPipe();
		base.prep(bk.fbstate());
		base.prep(new States.Viewport(wnd)).prep(new Ortho2D(wnd));
		base.prep(new ColorTex(new Texture2D.Sampler2D(fbuf)));
		float[] data = {
		         0,      0, 0, 1,
		    size.x,      0, 1, 1,
		         0, size.y, 0, 0,
		    size.x, size.y, 1, 0,
		};
		Model mod = new Model(Model.Mode.TRIANGLE_STRIP, new VertexArray(GOut.vf_tex, new VertexArray.Buffer(data.length * 4, DataBuffer.Usage.EPHEMERAL, DataBuffer.Filler.of(data))), null, 0, data.length / 4);
		fout.draw(base, mod);
		bk.swapbuffers(fout, mode);
		mod.dispose();
		fbuf.dispose();
		fout.fence(() -> {
		    synchronized(this) {
			prevdone = true;
			notifyAll();
		    }
		});
		bk.env().submit(fout);
	    });
	}

	public void dispose() {
	    bk.dispose();
	}
    }

    public Windeye window() {
	return(new IndirectWindow());
    }

    public boolean sharedenvs() {return(true);}

    public Collection<Monitor> monitors() {
	return(frontend.monitors());
    }

    public Cursor makecursor(BufferedImage img, Coord hs) {
	return(frontend.makecursor(img, hs));
    }

    public Cursor.Caps cursorcaps() {
	return(frontend.cursorcaps());
    }

    public FilePicker.Factory picker() {
	return(frontend.picker());
    }

    public void browse(java.net.URI location) throws java.io.IOException {
	frontend.browse(location);
    }

    public String description() {
	return(String.format("Indirect (backend=%s, frontend=%s)", backend, frontend.description()));
    }

    public void dispose() {
	backend.dispose();
	frontend.dispose();
    }

    public static class Factory implements Providers.Factory<IndirectToolkit> {
	public IndirectToolkit open(String... args) {
	    Toolkit frontend;
	    if((args.length > 0) && (args[0].length() > 0)) {
		int p = args[0].indexOf(':');
		String fnm = (p < 0) ? args[0] : args[0].substring(0, p);
		Providers.Factory<? extends Toolkit> fp = Toolkit.toolkits().get(fnm);
		if(fp == null)
		    throw(new Unavailable("no such toolkit type: " + fnm));
		frontend = (p < 0) ? fp.open() : fp.open(args[0].substring(p + 1));
	    } else {
		frontend = Providers.findfirst(Toolkit.toolkits().values(), "toolkit");
	    }
	    return(new IndirectToolkit(Acephal.instance(), frontend));
	}

	public boolean autouse() {return(false);}
    }

    public static Providers.Factory<IndirectToolkit> get() {
	return(new Factory());
    }
}
