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

public class NamedSocketAddress {
    public final String host;
    public final int port;

    public NamedSocketAddress(String host, int port) {
	this.host = host;
	this.port = port;
    }

    public boolean equals(NamedSocketAddress that){
	return(Utils.eq(this.host, that.host) && (this.port == that.port));
    }

    public boolean equals(Object that) {
	return((that instanceof NamedSocketAddress) && equals((NamedSocketAddress)that));
    }

    public int hashCode() {
	return(Objects.hash(host, port));
    }

    public static NamedSocketAddress parse(String spec, int defport) {
	if(spec.length() < 1)
	    throw(new IllegalArgumentException("empty address"));
	if(spec.charAt(0) == '[') {
	    int p = spec.indexOf(']');
	    if(p > 0) {
		if(p < spec.length() - 1) {
		    if(spec.charAt(p + 1) != ':')
			throw(new IllegalArgumentException("explicit IPv6 literal not followed by port: " + spec));
		    try {
			return(new NamedSocketAddress(spec.substring(1, p), Integer.parseInt(spec.substring(p + 2))));
		    } catch(NumberFormatException e) {
			throw(new IllegalArgumentException("non-numeric port specification: " + spec));
		    }
		} else {
		    return(new NamedSocketAddress(spec.substring(1, p), defport));
		}
	    }
	}
	int p = spec.lastIndexOf(':');
	if(p > 0) {
	    try {
		return(new NamedSocketAddress(spec.substring(0, p), Integer.parseInt(spec.substring(p + 1))));
	    } catch(NumberFormatException e) {
		throw(new IllegalArgumentException("non-numeric port specification: " + spec));
	    }
	}
	return(new NamedSocketAddress(spec, defport));
    }

    public static NamedSocketAddress parse(String spec) {
	return(parse(spec, 0));
    }

    public String toString(int defport) {
	StringBuilder buf = new StringBuilder();
	if(host.indexOf(':') >= 0) {
	    buf.append('[');
	    buf.append(host);
	    buf.append(']');
	} else {
	    buf.append(host);
	}
	if(port != defport) {
	    buf.append(':');
	    buf.append(port);
	}
	return(buf.toString());
    }

    public String toString() {
	return(toString(0));
    }
}
