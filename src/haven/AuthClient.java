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
import java.net.*;
import java.security.MessageDigest;

public class AuthClient {
    private static final SslHelper ssl;
    private Socket sk;
    private InputStream skin;
    private OutputStream skout;
    
    static {
	ssl = new SslHelper();
	try {
	    ssl.trust(ssl.loadX509(Resource.class.getResourceAsStream("authsrv.crt")));
	} catch(Exception e) {
	    throw(new RuntimeException(e));
	}
    }

    public AuthClient(String host, int port) throws IOException {
	sk = ssl.connect(host, port);
	skin = sk.getInputStream();
	skout = sk.getOutputStream();
    }
    
    private static byte[] digest(String pw) {
	MessageDigest dig;
	byte[] buf;
	try {
	    dig = MessageDigest.getInstance("SHA-256");
	    buf = pw.getBytes("utf-8");
	} catch(java.security.NoSuchAlgorithmException e) {
	    throw(new RuntimeException(e));
	} catch(java.io.UnsupportedEncodingException e) {
	    throw(new RuntimeException(e));
	}
	dig.update(buf);
	for(int i = 0; i < buf.length; i++)
	    buf[i] = 0;
	return(dig.digest());
    }

    public String trypasswd(String user, String pw) throws IOException {
	Message rpl = cmd("pw", user, digest(pw));
	String stat = rpl.string();
	if(stat.equals("ok")) {
	    String acct = rpl.string();
	    return(acct);
	} else if(stat.equals("no")) {
	    return(null);
	} else {
	    throw(new RuntimeException("Unexpected reply `" + stat + "' from auth server"));
	}
    }

    public String tryparadox(String user, String pw) throws IOException {
	Message rpl = cmd("pdx", user, pw);
	String stat = rpl.string();
	if(stat.equals("ok")) {
	    String acct = rpl.string();
	    return(acct);
	} else if(stat.equals("no")) {
	    return(null);
	} else {
	    throw(new RuntimeException("Unexpected reply `" + stat + "' from auth server"));
	}
    }

    public String trytoken(String user, byte[] token) throws IOException {
	Message rpl = cmd("token", user, token);
	String stat = rpl.string();
	if(stat.equals("ok")) {
	    String acct = rpl.string();
	    return(acct);
	} else if(stat.equals("no")) {
	    return(null);
	} else {
	    throw(new RuntimeException("Unexpected reply `" + stat + "' from auth server"));
	}
    }
    
    public byte[] getcookie() throws IOException {
	Message rpl = cmd("cookie");
	String stat = rpl.string();
	if(stat.equals("ok")) {
	    return(rpl.bytes(32));
	} else {
	    throw(new RuntimeException("Unexpected reply `" + stat + "' from auth server"));
	}
    }

    public byte[] gettoken() throws IOException {
	Message rpl = cmd("mktoken");
	String stat = rpl.string();
	if(stat.equals("ok")) {
	    return(rpl.bytes(32));
	} else {
	    throw(new RuntimeException("Unexpected reply `" + stat + "' from auth server"));
	}
    }
    
    public void close() throws IOException {
	sk.close();
    }

    private void sendmsg(Message msg) throws IOException {
	if(msg.blob.length > 65535)
	    throw(new RuntimeException("Too long message in AuthClient (" + msg.blob.length + " bytes)"));
	byte[] buf = new byte[msg.blob.length + 2];
	buf[0] = (byte)((msg.blob.length & 0xff00) >> 8);
	buf[1] = (byte)(msg.blob.length & 0x00ff);
	System.arraycopy(msg.blob, 0, buf, 2, msg.blob.length);
	skout.write(buf);
    }
    
    private void esendmsg(Object... args) throws IOException {
	Message buf = new Message(0);
	for(Object arg : args) {
	    if(arg instanceof String) {
		buf.addstring((String)arg);
	    } else if(arg instanceof byte[]) {
		buf.addbytes((byte[])arg);
	    } else {
		throw(new RuntimeException("Illegal argument to esendmsg: " + arg.getClass()));
	    }
	}
	sendmsg(buf);
    }

    private static void readall(InputStream in, byte[] buf) throws IOException {
	int rv;
	for(int i = 0; i < buf.length; i += rv) {
	    rv = in.read(buf, i, buf.length - i);
	    if(rv < 0)
		throw(new IOException("Premature end of input"));
	}
    }

    private Message recvmsg() throws IOException {
	byte[] header = new byte[2];
	readall(skin, header);
	int len = (Utils.ub(header[0]) << 8) | Utils.ub(header[1]);
	byte[] buf = new byte[len];
	readall(skin, buf);
	return(new Message(0, buf));
    }
    
    private Message cmd(Object... args) throws IOException {
	esendmsg(args);
	return(recvmsg());
    }
    
    public static void main(final String[] args) throws Exception {
	Thread t = new HackThread(new Runnable() {
		public void run() {
		    try {
			AuthClient test = new AuthClient("127.0.0.1", 1871);
			try {
			    String acct = test.trypasswd(args[0], args[1]);
			    if(acct == null) {
				System.err.println("failed");
				return;
			    }
			    System.out.println(acct);
			    System.out.println(Utils.byte2hex(test.getcookie()));
			} finally {
			    test.close();
			}
		    } catch(Exception e) {
			throw(new RuntimeException(e));
		    }
		}
	    }, "Test");
	t.start();
	t.join();
    }
}
