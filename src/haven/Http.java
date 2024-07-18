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
import java.io.*;
import java.net.*;

public class Http {
    public static final String USER_AGENT = useragent();
    public static final SslHelper ssl = sslconf();

    private static String useragent() {
	StringBuilder buf = new StringBuilder();
	buf.append("Haven/1.0");
	if(!Config.confid.equals(""))
	    buf.append(" (" + Config.confid + ")");
	String jv = Utils.getprop("java.version", null);
	if((jv != null) && !jv.equals(""))
	    buf.append(" Java/" + jv);
	return(buf.toString());
    }

    private static SslHelper sslconf() {
	SslHelper ssl = new SslHelper();
	try {
	    ssl.trust(Resource.class.getResourceAsStream("ressrv.crt"));
	} catch(java.security.cert.CertificateException e) {
	    throw(new Error("Invalid built-in certificate", e));
	} catch(IOException e) {
	    throw(new Error(e));
	}
	ssl.ignoreName();
	return(ssl);
    }

    public static URLConnection open(URL url) throws IOException {
	URLConnection conn;
	if(url.getProtocol().equals("https"))
	    conn = ssl.connect(url);
	else
	    conn = url.openConnection();
	conn.addRequestProperty("User-Agent", USER_AGENT);
	return(conn);
    }

    public static InputStream fetch(URL url, Consumer<URLConnection> init) throws IOException {
	RetryingInputStream ret = new RetryingInputStream() {
		protected InputStream create() throws IOException {
		    URLConnection c = open(url);
		    if(init != null)
			init.accept(c);
		    return(c.getInputStream());
		}
	    };
	ret.check();
	return(ret);
    }

    public static InputStream fetch(URL url) throws IOException {
	return(fetch(url, null));
    }
}
