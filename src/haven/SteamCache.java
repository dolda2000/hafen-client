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

import java.io.*;

public class SteamCache implements ResCache {
    private final Steam api;
    private final String prefix;

    public SteamCache(Steam api) {
	this.api = api;
	this.prefix = String.format("%s/%s/", api.appid(), api.userid());
    }

    public InputStream fetch(String name) throws IOException {
	Steam.FileReader in = api.new FileReader(prefix + name);
	class SteamStream extends InputStream {
	    int off = 0;

	    public int read() throws IOException {
		if(off >= in.sz)
		    return(-1);
		return(in.read(off++, 1)[0]);
	    }

	    public int read(byte[] b) throws IOException {
		return(read(b, 0, b.length));
	    }

	    public int read(byte[] b, int o, int l) throws IOException {
		byte[] data = in.read(off, l);
		System.arraycopy(data, 0, b, o, data.length);
		off += data.length;
		return(data.length);
	    }
	}
	return(new BufferedInputStream(new SteamStream(), Math.min(in.sz, 1 << 18)));
    }

    public OutputStream store(String name) throws IOException {
	Steam.FileWriter out = api.new FileWriter(prefix + name);
	class SteamStream extends OutputStream {
	    public void close() throws IOException {
		out.close();
	    }

	    public void write(int b) throws IOException {
		write(new byte[] {(byte)b});
	    }

	    public void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	    }

	    public void write(byte[] b, int off, int len) throws IOException {
		out.write(b, off, len);
	    }
	}
	SteamStream ret = new SteamStream();
	Finalizer.finalize(ret, () -> {
		try {
		    out.cancel();
		} catch(IOException e) {
		    new Warning(e, "could not abort steam stream output").issue();
		}
	    });
	return(new BufferedOutputStream(ret));
    }
}
