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

package haven.rs;

import haven.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.security.*;

public class Server extends Thread {
    public static final Map<String, Command> commands = new HashMap<String, Command>();
    private final ServerSocket sk;
    private final Random rng = new SecureRandom();
    private final byte[] key;

    public interface Command {
	public Object[] run(Client cl, Object... args) throws InterruptedException;
    }

    static {
	commands.put("ava", AvaRender.call);
    }

    public class Client extends Thread {
	private final InputStream in;
	private final OutputStream out;
	private byte[] nonce = null;
	private boolean auth = false;

	private Client(InputStream in, OutputStream out, boolean auth) {
	    super("Render server handler");
	    this.in = in;
	    this.out = out;
	    this.auth = auth;
	    setDaemon(true);
	    start();
	}

	private Client(Socket sk) throws IOException {
	    this(sk.getInputStream(), sk.getOutputStream(), false);
	}

	byte[] read(InputStream in, int bytes) throws IOException {
	    byte[] ret = new byte[bytes];
	    int n = 0;
	    while(n < bytes) {
		int rv = in.read(ret, n, bytes - n);
		if(rv < 0)
		    throw(new IOException("Unexpected end-of-file"));
		n += rv;
	    }
	    return(ret);
	}

	public void run() {
	    try {
		while(true) {
		    try {
			int len = Utils.int32d(read(in, 4), 0);
			if(!auth && (len > 256))
			    return;
			Message msg = new MessageBuf(read(in, len));
			String cmd = msg.string();
			Object[] args = msg.list();
			Object[] reply;
			if(auth) {
			    Command cc = commands.get(cmd);
			    if(cc != null)
				reply = cc.run(this, args);
			    else
				reply = new Object[] {"nocmd"};
			} else {
			    if(cmd.equals("nonce")) {
				nonce = new byte[32];
				rng.nextBytes(nonce);
				reply = new Object[] {nonce};
			    } else if(cmd.equals("auth")) {
				if((nonce != null) &&
				   Arrays.equals((byte[])args[0],
						 Digest.hash(Digest.HMAC.of(Digest.SHA256, key), nonce)))
				{
				    reply = new Object[] {"ok"};
				    auth = true;
				} else {
				    reply = new Object[] {"no"};
				}
			    } else {
				return;
			    }
			}
			MessageBuf rb = new MessageBuf();
			rb.addlist(reply);
			byte[] rbuf = new byte[4 + rb.size()];
			Utils.uint32e(rb.size(), rbuf, 0);
			rb.fin(rbuf, 4);
			out.write(rbuf);
			out.flush();
		    } catch(IOException e) {
			return;
		    }
		}
	    } catch(InterruptedException e) {
	    } finally {
		try {
		    in.close();
		    out.close();
		} catch(IOException e) {
		    throw(new RuntimeException(e));
		}
	    }
	}
    }

    public Server(int port, byte[] key) throws IOException {
	super("Render server");
	this.key = key;
	sk = new ServerSocket(port);
	start();
    }

    public Server() {
	super("Render server");
	sk = null;
	key = null;
    }

    public static void stdio() {
    }

    public void run() {
	try {
	    while(true) {
		Socket nsk;
		try {
		    nsk = sk.accept();
		} catch(IOException e) {
		    break;
		}
		try {
		    new Client(nsk);
		} catch(IOException e) {
		}
	    }
	} finally {
	    try {
		sk.close();
	    } catch(IOException e) {
		throw(new RuntimeException(e));
	    }
	}
    }

    public static void main(String[] args) throws Exception {
	if(args[0].equals("-")) {
	    Server s = new Server();
	    Client c = s.new Client(System.in, System.out, true);
	    c.join();
	} else {
	    new Server(Integer.parseInt(args[0]), Utils.b64.dec(System.getenv("AUTHKEY")));
	}
    }
}
