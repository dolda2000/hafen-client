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
import java.nio.file.*;
import java.nio.channels.*;
import java.net.*;
import static haven.Utils.pj;

public class HashDirCache implements ResCache {
    private final Path base;
    public final URI id;
    private final long idhash;

    public static Path findbase() {
	try {
	    windows: {
		String path = System.getenv("APPDATA");
		if(path == null)
		    break windows;
		Path appdata = Utils.path(path);
		if(!Files.exists(appdata) || !Files.isDirectory(appdata) || !Files.isReadable(appdata) || !Files.isWritable(appdata))
		    break windows;
		Path base = pj(appdata, "Haven and Hearth", "data");
		if(!Files.exists(base)) {
		    try {
			Files.createDirectories(base);
		    } catch(IOException e) {
			break windows;
		    }
		}
		return(base);
	    }
	    fallback: {
		String path = System.getProperty("user.home", null);
		if(path == null)
		    break fallback;
		Path home = Utils.path(path);
		if(!Files.exists(home) || !Files.isDirectory(home) || !Files.isReadable(home) || !Files.isWritable(home))
		    break fallback;
		Path base = pj(home, ".haven", "data");
		if(!Files.exists(base)) {
		    try {
			Files.createDirectories(base);
		    } catch(IOException e) {
			break fallback;
		    }
		}
		return(base);
	    }
	} catch(SecurityException e) {
	}
	throw(new UnsupportedOperationException("Found no reasonable place to store local files"));
    }

    private HashDirCache(URI id) {
	this.base = findbase();
	this.id = id;
	this.idhash = namehash(0, id.toString());
    }

    private static final Map<URI, HashDirCache> current = new CacheMap<>();
    public static HashDirCache get(URI id) {
	synchronized(current) {
	    HashDirCache ret = current.get(id);
	    if(ret == null)
		current.put(id, ret = new HashDirCache(id));
	    return(ret);
	}
    }

    private static URI mkurn(String id) {
	return(Utils.uri("urn:haven-cache:" + id));
    }

    public static HashDirCache get(String id) {
	return(get(mkurn(id)));
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

    private Header readhead(ReadableByteChannel ch) throws IOException {
	return(readhead(new DataInputStream(Channels.newInputStream(ch))));
    }

    private void writehead(DataOutput fp, String name) throws IOException {
	fp.writeByte(1);
	fp.writeUTF(id.toString());
	fp.writeUTF(name);
    }

    private void writehead(WritableByteChannel ch, String name) throws IOException {
	writehead(new DataOutputStream(Channels.newOutputStream(ch)), name);
    }

    private static FileChannel open2(Path path, OpenOption... mode) throws IOException {
	return(Utils.ioretry(() -> FileChannel.open(path, mode)));
    }

    private static class CacheFile implements AutoCloseable {
	final Path p;
	final Header h;
	FileChannel f;

	CacheFile(Path p, Header h, LockedFile lf) throws IOException {
	    this.p = p;
	    this.h = h;
	    this.f = lf.f;
	    lf.release();
	    lf.f = null;
	}

	FileChannel acquire() {
	    FileChannel ret = this.f;
	    this.f = null;
	    return(ret);
	}

	public void close() throws IOException {
	    if(f != null) {
		f.close();
		f = null;
	    }
	}
    }

    private static final Map<Path, int[]> monitors = new HashMap<>();
    private static boolean monwarned = false;
    private CacheFile lookup(String name, boolean creat) throws IOException {
	long h = namehash(idhash, name);
	Path lfn = pj(base, String.format("%016x.0", h));
	if(!Files.exists(lfn) && !creat)
	    return(null);
	int[] mon;
	synchronized(monitors) {
	    if(!monwarned && (monitors.size() > 100)) {
		Warning.warn("HashDirCache monitors growing suspiciously many: " + monitors.size());
		monwarned = true;
	    }
	    /* Apparently, Java doesn't allow two threads in one JVM
	     * to lock the same file... */
	    if((mon = monitors.get(lfn)) == null)
		monitors.put(lfn, mon = new int[] {1});
	    else
		mon[0]++;
	}
	try {
	    synchronized(mon) {
		try(LockedFile lf = LockedFile.lock(lfn)) {
		    for(int idx = 0; ; idx++) {
			Path path = pj(base, String.format("%016x.%d", h, idx));
			if(!Files.exists(path) && !creat)
			    return(null);
			FileChannel fp = (idx == 0) ? lf.f : open2(path, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
			try {
			    Header head = readhead(fp);
			    if(head == null) {
				if(!creat)
				    return(null);
				fp.truncate(0);
				writehead(fp, name);
				head = new Header();
				head.cid = id.toString();
				head.name = name;
				return(new CacheFile(path, head, lf));
			    }
			    if(head.cid.equals(id.toString()) && head.name.equals(name))
				return(new CacheFile(path, head, lf));
			} finally {
			    if(idx != 0)
				fp.close();
			}
		    }
		}
	    }
	} finally {
	    synchronized(monitors) {
		mon[0]--;
		if(mon[0] < 0) {
		    throw(new AssertionError(String.format("monitor refcount %d for %s (%s)", mon[0], lfn, name)));
		} else if(mon[0] == 0) {
		    if(monitors.remove(lfn) != mon)
			throw(new AssertionError(String.format("monitor identity crisis for %s (%s)", lfn, name)));
		}
	    }
	}
    }

    private static DirectoryStream.Filter<Path> fnfilter(java.util.function.Predicate<String> filter) {
	return(p -> filter.test(p.getFileName().toString()));
    }

    private Iterator<String> list(boolean filter) throws IOException {
	Iterator<Path> files =
	    Files.newDirectoryStream(base, fnfilter(f -> (f.length() >= 18) &&
						    (f.charAt(16) == '.') &&
						    Utils.strcheck(f.substring(17), Character::isDigit)))
	    .iterator();
	return(new Iterator<String>() {
		int i = 0;
		String next = null;

		public boolean hasNext() {
		    if(next != null)
			return(true);
		    try {
			while(files.hasNext()) {
			    FileChannel fp = open2(files.next(), StandardOpenOption.READ);
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

    private Iterator<String> list() throws IOException {
	return(list(true));
    }

    public OutputStream store(String name) throws IOException {
	Path path;
	try(CacheFile cf = lookup(name, true)) {
	    path = cf.p;
	}
	Path dir = path.getParent();
	Path tmp = Files.createTempFile(dir, "cache", ".new");
	FileChannel fp = open2(tmp, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
	writehead(fp, name);
	OutputStream st = Channels.newOutputStream(fp);
	class Cleaner implements Finalizer.Cleaner {
	    boolean closed = false;

	    public void clean() {
		if(!closed) {
		    try {
			st.close();
			Files.delete(tmp);
		    } catch(IOException e) {
			new Warning(e, "cleaning unclosed cache-stream").issue();
		    }
		}
	    }
	}
	Cleaner cleaner = new Cleaner();
	return(new OutputStream() {
		private Runnable clean = Finalizer.finalize(this, cleaner);

		public void write(int b) throws IOException {
		    st.write(b);
		}

		public void write(byte[] buf, int off, int len) throws IOException {
		    st.write(buf, off, len);
		}

		public void close() throws IOException {
		    st.close();
		    Utils.ioretry(() -> {
			    try {
				return(Files.move(tmp, path, StandardCopyOption.ATOMIC_MOVE));
			    } catch(AtomicMoveNotSupportedException e) {
				return(Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING));
			    }
			});
		    cleaner.closed = true;
		    clean.run();
		}
	    });
    }

    public InputStream fetch(String name) throws IOException {
	try(CacheFile cf = lookup(name, false)) {
	    if(cf == null)
		throw(new FileNotFoundException(name));
	    FileChannel fp = cf.acquire();
	    return(Channels.newInputStream(fp));
	}
    }

    public void remove(String name) throws IOException {
	try(CacheFile cf = lookup(name, false)) {
	    if(cf == null)
		throw(new FileNotFoundException(name));
	    cf.close();
	    Files.deleteIfExists(cf.p);
	}
    }

    public String toString() {
	return("HashDirCache(" + id + ")");
    }

    public static HashDirCache create() {
	HashDirCache ret;
	try {
	    if(cachebase.get() != null)
		return(get(cachebase.get()));
	    if(Resource.resurl.get() != null)
		return(get(Resource.resurl.get()));
	    return(get("default"));
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
	    cache = get(URI.create(args[0]));
	else
	    cache = get(args[0]);
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
	case "purge":
	    int n = 0;
	    for(Iterator<String> i = cache.list(); i.hasNext();) {
		String nm = i.next();
		try {
		    cache.remove(nm);
		    n++;
		} catch(FileNotFoundException e) {
		    System.err.printf("%s: not found\n", nm);
		}
	    }
	    System.err.printf("%s: %d files purged\n", cache.id, n);
	    break;
	case "rm":
	    for(int i = 2; i < args.length; i++) {
		try {
		    cache.remove(args[i]);
		} catch(FileNotFoundException e) {
		    System.err.printf("%s: not found\n", args[i]);
		}
	    }
	    break;
	default:
	    System.err.printf("%s: no such command\n", args[2]);
	    break;
	}
    }
}
