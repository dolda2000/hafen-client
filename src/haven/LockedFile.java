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

public class LockedFile implements AutoCloseable {
    public FileChannel f;
    public FileLock l;

    private LockedFile(FileChannel f, FileLock l) {this.f = f; this.l = l;}

    public void release() throws IOException {
	if(l != null) {
	    l.release();
	    l = null;
	}
    }

    public void close() throws IOException {
	release();
	if(f != null) {
	    f.close();
	    f = null;
	}
    }

    /* Assumes lock-times are short, so that there shouldn't be a
     * problem with blocking interruptions. */
    public static LockedFile lock(Path path, long pos, long len, boolean shared) throws IOException {
        boolean intr = false;
        try {
            while(true) {
                try {
                    FileChannel fp = null;
                    try {
                        fp = Utils.ioretry(() -> FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE));
                        FileLock lk = fp.lock(pos, len, shared);
                        LockedFile ret = new LockedFile(fp, lk);
                        fp = null;
                        return(ret);
                    } finally {
                        if(fp != null)
                            fp.close();
                    }
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

    public static LockedFile lock(Path path) throws IOException {
	return(lock(path, 0, 1, false));
    }
}
