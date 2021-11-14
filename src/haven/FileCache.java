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
import java.nio.file.*;
import java.nio.channels.*;

public class FileCache implements ResCache {
    private final Path base;
    
    public FileCache(Path base) {
	this.base = base;
    }
    
    public static FileCache foruser() {
	try {
	    String path = System.getProperty("user.home", null);
	    if(path == null)
		return(null);
	    Path home = Utils.path(path);
	    if(!Files.exists(home) || !Files.isDirectory(home) || !Files.isReadable(home) || !Files.isWritable(home))
		return(null);
	    Path base = Utils.pj(home, ".haven", "hafen", "cache");
	    if(!Files.exists(base)) {
		try {
		    Files.createDirectories(base);
		} catch(IOException e) {
		    return(null);
		}
	    }
	    return(new FileCache(base));
	} catch(SecurityException e) {
	    return(null);
	}
    }
    
    private Path forres(String nm) {
	Path res = base;
	String[] comp = nm.split("/");
	for(int i = 0; i < comp.length - 1; i++)
	    res = res.resolve(comp[i]);
	return(res.resolve(comp[comp.length - 1] + ".cached"));
    }

    public OutputStream store(String name) throws IOException {
	Path nm = forres(name);
	Path dir = nm.getParent();
	if(!Files.exists(dir))
	    Files.createDirectories(dir);
	Path tmp = dir.resolve(nm.getFileName().toString() + ".new");
	Files.deleteIfExists(tmp);
	OutputStream ret = new FilterOutputStream(Files.newOutputStream(tmp)) {
		public void close() throws IOException {
		    super.close();
		    try  {
			Files.move(tmp, nm, StandardCopyOption.ATOMIC_MOVE);
		    } catch(AtomicMoveNotSupportedException e) {
			Files.move(tmp, nm, StandardCopyOption.REPLACE_EXISTING);
		    }
		}
	    };
	return(ret);
    }
    
    public InputStream fetch(String name) throws IOException {
	try {
	    return(Files.newInputStream(forres(name)));
	} catch(NoSuchFileException e) {
	    throw((FileNotFoundException)new FileNotFoundException(name).initCause(e));
	}
    }
    
    public String toString() {
	return("FileCache(" + base + ")");
    }
}
