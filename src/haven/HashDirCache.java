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
import java.nio.channels.*;
import java.net.URI;

public class HashDirCache implements ResCache {
    private final File base;
    public final URI id;
    private final long idhash;

    public static File findbase() {
	try {
	    windows: {
		String path = System.getenv("APPDATA");
		if(path == null)
		    break windows;
		File appdata = new File(path);
		if(!appdata.exists() || !appdata.isDirectory() || !appdata.canRead() || !appdata.canWrite())
		    break windows;
		File base = new File(new File(appdata, "Haven and Hearth"), "data");
		if(!base.exists() && !base.mkdirs())
		    break windows;
		return(base);
	    }
	    fallback: {
		String path = System.getProperty("user.home", null);
		if(path == null)
		    break fallback;
		File home = new File(path);
		if(!home.exists() || !home.isDirectory() || !home.canRead() || !home.canWrite())
		    break fallback;
		File base = new File(new File(home, ".haven"), "data");
		if(!base.exists() && !base.mkdirs())
		    break fallback;
		return(base);
	    }
	} catch(SecurityException e) {
	}
	throw(new UnsupportedOperationException("Found no reasonable place to store local files"));
    }

    public HashDirCache(URI id) {
	this.base = findbase();
	this.id = id;
	this.idhash = namehash(0, id.toString());
    }

    private static URI mkurn(String id) {
	try {
	    return(new URI("urn:haven-cache:" + id));
	} catch(java.net.URISyntaxException e) {
	    throw(new IllegalArgumentException(id));
	}
    }

    public HashDirCache(String id) {
	this(mkurn(id));
    }

    private long namehash(long h, String name) {
	for(int i = 0; i < name.length(); i++)
	    h = (h * 31) + name.charAt(i);
	return(h);
    }

    private static class Header {
	String cid, name;
    }

    private Header readhead(DataInput fp) throws IOException {
	try {
	    int ver = fp.readByte();
	    if(ver == 1) {
		Header head = new Header();
		head.cid = fp.readUTF();
		head.name = fp.readUTF();
		return(head);
	    } else {
		return(null);
	    }
	} catch(EOFException e) {
	    return(null);
	}
    }

    private void writehead(DataOutput fp, String name) throws IOException {
	fp.writeByte(1);
	fp.writeUTF(id.toString());
	fp.writeUTF(name);
    }

    /* These locks should never have to be waited for long at all, so
     * blocking interruptions until complete should be perfectly
     * okay. */
    private static FileLock lock2(FileChannel ch) throws IOException {
	boolean intr = false;
	try {
	    while(true) {
		try {
		    return(ch.lock());
		} catch(FileLockInterruptionException e) {
		    Thread.currentThread().interrupted();
		    intr = true;
		}
	    }
	} finally {
	    if(intr)
		Thread.currentThread().interrupt();
	}
    }

    private static final Map<File, Object> monitors = new WeakHashMap<File, Object>();
    private File lookup(String name, boolean creat) throws IOException {
	long h = namehash(idhash, name);
	File lfn = new File(base, String.format("%016x.0", h));
	if(!lfn.exists() && !creat)
	    return(null);
	Object mon;
	synchronized(monitors) {
	    /* Apparently, Java doesn't allow two threads in one JVM
	     * to lock the same file... */
	    if((mon = monitors.get(lfn)) == null)
		monitors.put(lfn, mon = new Object());
	}
	synchronized(mon) {
	    RandomAccessFile lf = new RandomAccessFile(lfn, "rw");
	    FileLock lk = null;
	    try {
		lk = lock2(lf.getChannel());
		for(int idx = 0; ; idx++) {
		    File path = new File(base, String.format("%016x.%d", h, idx));
		    if(!path.exists() && !creat)
			return(null);
		    RandomAccessFile fp = (idx == 0)?lf:new RandomAccessFile(path, "rw");
		    try {
			Header head = readhead(fp);
			if(head == null) {
			    if(!creat)
				return(null);
			    fp.setLength(0);
			    writehead(fp, name);
			    return(path);
			}
			if(head.cid.equals(id.toString()) && head.name.equals(name))
			    return(path);
		    } finally {
			if(idx != 0)
			    fp.close();
		    }
		}
	    } finally {
		if(lk != null)
		    lk.release();
		lf.close();
	    }
	}
    }

    private Iterator<String> list(boolean filter) {
	final File[] files = base.listFiles(f -> (f.getName().length() >= 18) &&
					    (f.getName().charAt(16) == '.') &&
					    Utils.strcheck(f.getName().substring(17), Character::isDigit));
	return(new Iterator<String>() {
		int i = 0;
		String next = null;

		public boolean hasNext() {
		    if(next != null)
			return(true);
		    try {
			for(; i < files.length; i++) {
			    RandomAccessFile fp = new RandomAccessFile(files[i], "rw");
			    try {
				Header head = readhead(fp);
				if(head == null)
				    continue;
				if(filter && !head.cid.equals(id.toString()))
				    continue;
				next = head.name;
				i++;
				return(true);
			    } finally {
				fp.close();
			    }
			}
		    } catch(IOException e) {
			throw(new RuntimeException(e));
		    }
		    return(false);
		}

		public String next() {
		    if(!hasNext())
			throw(new NoSuchElementException());
		    String ret = next;
		    next = null;
		    return(ret);
		}
	    });
    }

    private Iterator<String> list() {
	return(list(true));
    }

    public OutputStream store(String name) throws IOException {
	final File path = lookup(name, true);
	File dir = path.getParentFile();
	final File tmp = File.createTempFile("cache", ".new", dir);
	final RandomAccessFile fp = new RandomAccessFile(tmp, "rw");
	writehead(fp, name);
	return(new OutputStream() {
		public void write(int b) throws IOException {
		    fp.write(b);
		}

		public void write(byte[] buf, int off, int len) throws IOException {
		    fp.write(buf, off, len);
		}

		public void close() throws IOException {
		    fp.close();
		    if(!tmp.renameTo(path)) {
			/* Apparently Java doesn't support atomic
			 * renames on Windows... :-/ */
			path.delete();
			tmp.renameTo(path);
		    }
		}
	    });
    }

    public InputStream fetch(String name) throws IOException {
	File path = lookup(name, false);
	if(path == null)
	    throw(new FileNotFoundException(name));
	final RandomAccessFile fp = new RandomAccessFile(path, "r");
	Header head = readhead(fp);
	if((head == null) || !head.cid.equals(id.toString()) || !head.name.equals(name))
	    throw(new AssertionError());
	return(new InputStream() {
		public int read() throws IOException {
		    return(fp.read());
		}

		public int read(byte[] buf, int off, int len) throws IOException {
		    return(fp.read(buf, off, len));
		}

		public void close() throws IOException {
		    fp.close();
		}
	    });
    }

    public String toString() {
	return("FileCache(" + id + ")");
    }

    public static HashDirCache forjnlp() {
	try {
	    javax.jnlp.BasicService basic = (javax.jnlp.BasicService)javax.jnlp.ServiceManager.lookup("javax.jnlp.BasicService");
	    if(basic == null)
		return(null);
	    return(new HashDirCache(basic.getCodeBase().toURI()));
	} catch(NoClassDefFoundError e) {
	    return(null);
	} catch(Exception e) {
	    return(null);
	}
    }

    public static HashDirCache create() {
	HashDirCache ret;
	try {
	    if((ret = forjnlp()) != null)
		return(ret);
	    if(Config.resurl != null)
		return(new HashDirCache(Config.resurl.toURI()));
	    return(new HashDirCache("default"));
	} catch(Exception e) {
	    return(null);
	}
    }

    public static void main(String[] args) throws IOException {
	if(args.length < 2) {
	    System.err.println("usage: HashDirCache [-h] ID-URI COMMAND [ARGS...]");
	    System.exit(1);
	}
	HashDirCache cache;
	if(args[0].indexOf(':') >= 0)
	    cache = new HashDirCache(URI.create(args[0]));
	else
	    cache = new HashDirCache(args[0]);
	switch(args[1]) {
	case "ls":
	    for(Iterator<String> i = cache.list(); i.hasNext();) {
		String nm = i.next();
		System.out.println(nm);
	    }
	    break;
	case "cat":
	    InputStream fp;
	    try {
		fp = cache.fetch(args[2]);
	    } catch(FileNotFoundException e) {
		System.err.printf("%s: not found\n", args[2]);
		System.exit(1);
		break;
	    }
	    byte[] buf = new byte[1024];
	    while(true) {
		int n = fp.read(buf);
		if(n < 0)
		    break;
		System.out.write(buf, 0, n);
	    }
	    break;
	default:
	    System.err.printf("%s: no such command\n", args[2]);
	    break;
	}
    }
}
