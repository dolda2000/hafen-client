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
import java.io.*;
import java.awt.image.*;
import com.jogamp.opengl.*;

public class Debug {
    public static final int FRAME_DEBUG_KEY = java.awt.event.KeyEvent.VK_F8;
    public static boolean kf1, kf2, kf3, kf4;
    public static boolean pk1, pk2, pk3, pk4;
    public static boolean fdk, pfdk, ff;
    public static PrintWriter log = new PrintWriter(System.err);

    public static void cycle(int modflags) {
	pk1 = kf1; pk2 = kf2; pk3 = kf3; pk4 = kf4;
	kf1 = (modflags & 1) != 0;
	kf2 = (modflags & 2) != 0;
	kf3 = (modflags & 4) != 0;
	kf4 = (modflags & 8) != 0;
	ff = fdk && !pfdk; pfdk = fdk;
    }

    public static void dumpimage(BufferedImage img, File path) {
	try {
	    javax.imageio.ImageIO.write(img, "PNG", path);
	} catch(IOException e) {
	    throw(new RuntimeException(e));
	}
    }

    public static void dumpimage(BufferedImage img, String fn) {
	dumpimage(img, new File(fn));
    }

    public static void dumpimage(BufferedImage img) {
	dumpimage(img, "/tmp/test.png");
    }

    public static File somedir(String basename) {
	String home = System.getProperty("user.home", null);
	if(home == null)
	    return(new File(basename));
	return(new File(new File(home), basename));
    }

    private static void dump_r(Object... stuff) {
	if(stuff.length > 0) {
	    for(int i = 0; i < stuff.length; i++) {
		if(i > 0)
		    System.err.print(' ');
		if(stuff[i] instanceof Object[]) {
		    Object[] oa = (Object[])stuff[i];
		    System.err.print('[');
		    for(int o = 0; o < oa.length; o++) {
			if(o > 0)
			    System.err.print(", ");
			dump_r(oa[o]);
		    }
		    System.err.print(']');
		} else if(stuff[i] instanceof byte[]) {
		    byte[] ba = (byte[])stuff[i];
		    if(ba.length < 32) {
			System.err.print(Utils.byte2hex(ba));
		    } else {
			System.err.println();
			Utils.hexdump(ba, System.err, 0);
		    }
		} else if(stuff[i] instanceof int[]) {
		    Utils.dumparr((int[])stuff[i], System.err, false);
		} else if(stuff[i] instanceof float[]) {
		    Utils.dumparr((float[])stuff[i], System.err, false);
		} else if(stuff[i] instanceof short[]) {
		    Utils.dumparr((short[])stuff[i], System.err, false);
		} else if(stuff[i] instanceof boolean[]) {
		    boolean[] ba = (boolean[])stuff[i];
		    System.err.print('[');
		    for(int o = 0; o < ba.length; o++)
			System.err.print(ba[o] ? "\u22a4" : "\u22a5");
		    System.err.print(']');
		} else {
		    System.err.print(stuff[i]);
		}
	    }
	}
    }

    public static void dump(Object... stuff) {
	synchronized(System.err) {
	    dump_r(stuff);
	    System.err.println();
	}
    }

    public static class DumpGL extends TraceGL4bc {
	public final ByteArrayOutputStream buf;

	private DumpGL(GL4bc bk, ByteArrayOutputStream buf) {
	    super(bk, new PrintStream(buf));
	    this.buf = buf;
	}
	public DumpGL(GL4bc bk) {
	    this(bk, new ByteArrayOutputStream());
	}

	public void reset() {
	    buf.reset();
	}

	public void dump(String fn) {
	    try {
		OutputStream out = new FileOutputStream(fn);
		try {
		    out.write(buf.toByteArray());
		} finally {
		    out.close();
		}
	    } catch(IOException e) {
		throw(new RuntimeException(e));
	    }
	}
    }

    static int dumpseq = 0;
    public static PrintWriter getdump() {
	try {
	    return(new java.io.PrintWriter(new java.io.FileWriter("/tmp/dbdump-" + dumpseq++)));
	} catch(java.io.IOException e) {
	    throw(new RuntimeException(e));
	}
    }

    public static class DataException extends RuntimeException {
	public final Serializable data;

	public DataException(String msg, Throwable cause, Serializable data) {
	    super(msg, cause);
	    this.data = data;
	}

	public DataException(String msg, Serializable data) {
	    super(msg);
	    this.data = data;
	}
    }
}
