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

public class BaseFileCache implements ResCache {
    public final URI id;
    private final Path base;

    public static Path findroot() {
	try {
	    windows: {
		String path = System.getenv("APPDATA");
		if(path == null)
		    break windows;
		Path appdata = Utils.path(path);
		if(!Files.exists(appdata) || !Files.isDirectory(appdata) || !Files.isReadable(appdata) || !Files.isWritable(appdata))
		    break windows;
		Path base = pj(appdata, "Haven and Hearth", "cache");
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
		Path base = pj(home, ".haven", "cache");
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

    public static Path findbase(URI id) throws IOException {
	String idstr = id.toString();
	int idhash = 0;
	for(int i = 0; i < idstr.length(); i++)
	    idhash = (idhash * 31) + idstr.charAt(i);
	Path root = findroot();
	Path lfn = pj(root, ".index-lock");
	synchronized(BaseFileCache.class) {
	    /* Note: Synchronization is only because Java doesn't
	     * allow two threads in one JVM to lock the same file. */
	    try(LockedFile lock = LockedFile.lock(lfn)) {
		for(int idx = 0; ; idx++) {
		    Path base = pj(root, String.format("%08x.%d", idhash, idx));
		    Path propname = pj(base, "cache.properties");
		    if(Files.exists(base) && Files.isDirectory(base)) {
			Properties props = new Properties();
			try(InputStream fp = Files.newInputStream(propname)) {
			    props.load(fp);
			} catch(NoSuchFileException e) {
			    continue;
			}
			if(Utils.eq(props.get("base"), idstr))
			    return(base);
		    } else {
			boolean done = false;
			try {
			    Files.createDirectory(base);
			    Properties props = new Properties();
			    props.put("base", idstr);
			    try(OutputStream fp = Files.newOutputStream(propname)) {
				props.store(fp, null);
			    }
			    done = true;
			    return(base);
			} finally {
			    if(!done) {
				try { Files.delete(propname); } catch(NoSuchFileException e) {}
				try { Files.delete(base); } catch(NoSuchFileException e) {}
			    }
			}
		    }
		}
	    }
	}
    }

    public BaseFileCache(URI id) throws IOException {
	this.id = id;
	this.base = findbase(id);
    }

    private static final Map<URI, BaseFileCache> current = new CacheMap<>();
    public static BaseFileCache get(URI id) throws IOException {
	synchronized(current) {
	    BaseFileCache ret = current.get(id);
	    if(ret == null)
		current.put(id, ret = new BaseFileCache(id));
	    return(ret);
	}
    }

    private static URI mkurn(String id) {
	return(Utils.uri("urn:haven-cache:" + id));
    }

    public static BaseFileCache get(String id) throws IOException {
	return(get(mkurn(id)));
    }

    public static BaseFileCache create() {
	BaseFileCache ret;
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

    private String mangle(String el) {
	if(Resource.FileSource.windows) {
	    StringBuilder buf = new StringBuilder();
	    for(int i = 0; i < el.length(); i++) {
		char c = el.charAt(i);
		if((c != '@') && Resource.FileSource.winsafechar(c)) {
		    buf.append(c);
		} else {
		    buf.append('@');
		    buf.append(Utils.num2hex((c & 0xf000) >> 12));
		    buf.append(Utils.num2hex((c & 0x0f00) >>  8));
		    buf.append(Utils.num2hex((c & 0x00f0) >>  4));
		    buf.append(Utils.num2hex((c & 0x000f) >>  0));
		}
	    }
	    el = buf.toString();
	}
	if(Resource.FileSource.windows &&
	   (el.startsWith("windows-special-") ||
	    Resource.FileSource.wintraps.contains(el)))
	{
	    return("windows-special-" + el);
	}
	return(el);
    }

    private Path forres(String nm) {
	Path res = base;
	String[] comp = nm.split("/");
	for(int i = 0; i < comp.length - 1; i++)
	    res = res.resolve(mangle(comp[i]));
	return(res.resolve(comp[comp.length - 1] + ".cached"));
    }

    public InputStream fetch(String name) throws IOException {
	try {
	    return(Files.newInputStream(forres(name)));
	} catch(NoSuchFileException e) {
	    throw((FileNotFoundException)new FileNotFoundException(name).initCause(e));
	}
    }

    public OutputStream store(String name) throws IOException {
	Path path = forres(name);
	Path dir = path.getParent();
	if(!Files.exists(dir))
	    Files.createDirectories(dir);
	Path tmp = Files.createTempFile(dir, "cache", ".new");
	OutputStream fp = Files.newOutputStream(tmp);
	class Cleaner implements Finalizer.Cleaner {
	    boolean closed = false;

	    public void clean() {
		if(!closed) {
		    try {
			fp.close();
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
		    fp.write(b);
		}

		public void write(byte[] buf, int off, int len) throws IOException {
		    fp.write(buf, off, len);
		}

		public void close() throws IOException {
		    fp.close();
		    try {
			Files.move(tmp, path, StandardCopyOption.ATOMIC_MOVE);
		    } catch(AtomicMoveNotSupportedException e) {
			Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
		    }
		    cleaner.closed = true;
		    clean.run();
		}
	    });
    }

    public void remove(String name) throws IOException {
	try {
	    Files.delete(forres(name));
	} catch(NoSuchFileException e) {
	    throw((FileNotFoundException)new FileNotFoundException(name).initCause(e));
	}
    }

    public String toString() {
	return("BaseFileCache(" + base + ")");
    }
}
