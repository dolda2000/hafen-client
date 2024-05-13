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

import java.util.function.*;
import java.security.*;

public interface Digest {
    public static final Algorithm SHA256 = Builtin.alg("SHA-256");
    public static final Algorithm SHA384 = Builtin.alg("SHA-384");
    public static final Algorithm SHA512 = Builtin.alg("SHA-512");
    public static final Algorithm SHA1 = Builtin.alg("SHA-1");
    public static final Algorithm MD5 = Builtin.alg("MD5");

    public static interface Algorithm extends Supplier<Digest> {
	public String name();
    }

    public Digest update(byte[] part);
    public byte[] digest();
    public default String hexdigest() {
	return(Utils.byte2hex(digest()));
    }

    public static byte[] hash(Algorithm alg, byte[]... parts) {
	Digest dig = alg.get();
	for(byte[] part : parts)
	    dig.update(part);
	return(dig.digest());
    }

    public static class Builtin implements Digest {
	private final MessageDigest md;

	Builtin(MessageDigest md) {
	    this.md = md;
	}

	public Digest update(byte[] part) {
	    md.update(part);
	    return(this);
	}

	public byte[] digest() {
	    return(md.digest());
	}

	private static Algorithm alg(String name) {
	    return(new Algorithm() {
		    public Digest get() {
			try {
			    return(new Builtin(MessageDigest.getInstance(name)));
			} catch(NoSuchAlgorithmException e) {
			    throw(new AssertionError(e));
			}
		    }

		    public String name() {return(name);}
		    public String toString() {return(name());}
		});
	}
    }
}
