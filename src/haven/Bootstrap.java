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
import java.util.*;

public class Bootstrap implements UI.Receiver, UI.Runner {
    public static final Config.Variable<String> authuser = Config.Variable.prop("haven.authuser", null);
    public static final Config.Variable<String> authserv = Config.Variable.prop("haven.authserv", null);
    public static final Config.Variable<String> defserv = Config.Variable.prop("haven.defserv", "localhost");
    public static final Config.Variable<Integer> mainport = Config.Variable.propi("haven.mainport", 1870);
    public static final Config.Variable<Integer> authport = Config.Variable.propi("haven.authport", 1871);
    public static final Config.Variable<byte[]> authck = Config.Variable.propb("haven.authck", null);
    public static final Config.Variable<byte[]> authtoken = Config.Variable.propb("haven.inittoken", null);
    public static final Config.Variable<String[]> servargs = Config.Variable.def(() -> null);
    public static boolean useinitauth = true;
    String hostname;
    int port;
    Queue<Message> msgs = new LinkedList<Message>();
    String inituser = null;
    byte[] initcookie = null;
    byte[] inittoken = null;

    public static class Message {
	int id;
	String name;
	Object[] args;

	public Message(int id, String name, Object... args) {
	    this.id = id;
	    this.name = name;
	    this.args = args;
	}
    }

    public Bootstrap(String hostname, int port) {
	this.hostname = hostname;
	this.port = port;
    }

    public Bootstrap() {
	this(defserv.get(), mainport.get());
	if(useinitauth) {
	    if((authuser.get() != null) && (authck.get() != null)) {
		setinitcookie(authuser.get(), authck.get());
		useinitauth = false;
	    } else if((authuser.get() != null) && (authtoken.get() != null)) {
		setinittoken(authuser.get(), authtoken.get());
		useinitauth = false;
	    }
	}
    }

    public void setinitcookie(String username, byte[] cookie) {
	inituser = username;
	initcookie = cookie;
    }

    public void setinittoken(String username, byte[] token) {
	inituser = username;
	inittoken = token;
    }

    private String getpref(String name, String def) {
	return(Utils.getpref(name + "@" + hostname, def));
    }

    private void setpref(String name, String val) {
	Utils.setpref(name + "@" + hostname, val);
    }

    private static byte[] getprefb(String name, String hostname, byte[] def, boolean zerovalid) {
	String sv = Utils.getpref(name + "@" + hostname, null);
	if(sv == null)
	    return(def);
	byte[] ret = Utils.hex.dec(sv);
	if((ret.length == 0) && !zerovalid)
	    return(def);
	return(ret);
    }

    private static String mangleuser(String user) {
	if(user.length() <= 32)
	    return(user);
	/* Mangle name because Java pref names have a somewhat
	 * ridiculously short limit. */
	return(Utils.hex.enc(Digest.hash(Digest.MD5, user.getBytes(Utils.utf8))));
    }

    public static byte[] gettoken(String user, String hostname) {
	return(getprefb("savedtoken-" + mangleuser(user), hostname, null, false));
    }

    public static void rottokens(String user, String hostname, boolean creat, boolean rm) {
	List<String> names = new ArrayList<>(Utils.getprefsl("saved-tokens@" + hostname, new String[] {}));
	creat = creat || (!rm && names.contains(user));
	if(rm || creat)
	    names.remove(user);
	if(creat)
	    names.add(0, user);
	Utils.setprefsl("saved-tokens@" + hostname, names);
    }

    public static void settoken(String user, String hostname, byte[] token) {
	String prefnm = user;
	Utils.setpref("savedtoken-" + mangleuser(user) + "@" + hostname, (token == null) ? "" : Utils.hex.enc(token));
	rottokens(user, hostname, token != null, true);
    }

    private Message getmsg() throws InterruptedException {
	Message msg;
	synchronized(msgs) {
	    while((msg = msgs.poll()) == null)
		msgs.wait();
	    return(msg);
	}
    }

    private static void preferhost(InetAddress[] hosts, SocketAddress prev) {
	if((prev == null) || !(prev instanceof InetSocketAddress))
	    return;
	InetAddress host = ((InetSocketAddress)prev).getAddress();
	Arrays.sort(hosts, (a, b) -> {
		boolean pa = Utils.eq(a, host), pb = Utils.eq(b, host);
		if(pa && pb)
		    return(0);
		else if(pa)
		    return(-1);
		else if(pb)
		    return(1);
		else
		    return(0);
	    });
    }

    public UI.Runner run(UI ui) throws InterruptedException {
	ui.setreceiver(this);
	ui.newwidgetp(1, ($1, $2) -> new LoginScreen(hostname), 0, new Object[] {Coord.z});
	String loginname = getpref("loginname", "");
	boolean savepw = false;
	String authserver = (authserv.get() == null) ? hostname : authserv.get();
	int authport = Bootstrap.authport.get();
	Session sess;
	retry: do {
	    byte[] cookie, token;
	    Session.User acct;
	    SocketAddress authaddr = null;
	    if(initcookie != null) {
		acct = new Session.User(inituser);
		cookie = initcookie;
		initcookie = null;
	    } else if((inituser != null) && (inittoken != null)) {
		ui.uimsg(1, "prg", "Authenticating...");
		byte[] inittoken = this.inittoken;
		this.inittoken = null;
		authed: try(AuthClient auth = new AuthClient(authserver, authport)) {
		    authaddr = auth.address();
		    if(!Arrays.equals(inittoken, getprefb("lasttoken-" + mangleuser(inituser), hostname, null, false))) {
			String authed = null;
			try {
			    authed = new AuthClient.TokenCred(inituser, inittoken).tryauth(auth);
			} catch(AuthClient.Credentials.AuthException e) {
			}
			setpref("lasttoken-" + mangleuser(inituser), Utils.hex.enc(inittoken));
			if(authed != null) {
			    acct = new Session.User(authed);
			    cookie = auth.getcookie();
			    if(Connection.encrypt.get())
				acct.alias(auth.getalias());
			    settoken(authed, hostname, auth.gettoken());
			    break authed;
			}
		    }
		    if((token = gettoken(inituser, hostname)) != null) {
			try {
			    String authed = new AuthClient.TokenCred(inituser, token).tryauth(auth);
			    acct = new Session.User(authed);
			    cookie = auth.getcookie();
			    if(Connection.encrypt.get())
				acct.alias(auth.getalias());
			    break authed;
			} catch(AuthClient.Credentials.AuthException e) {
			    settoken(inituser, hostname, null);
			}
		    }
		    ui.uimsg(1, "error", "Launcher login expired");
		    continue retry;
		} catch(IOException e) {
		    ui.uimsg(1, "error", e.getMessage());
		    continue retry;
		}
	    } else {
		AuthClient.Credentials creds;
		ui.uimsg(1, "login");
		while(true) {
		    Message msg = getmsg();
		    if(msg.id == 1) {
			if(msg.name == "login") {
			    creds = (AuthClient.Credentials)msg.args[0];
			    savepw = (Boolean)msg.args[1];
			    loginname = creds.name();
			    break;
			}
		    }
		}
		ui.uimsg(1, "prg", "Authenticating...");
		try(AuthClient auth = new AuthClient(authserver, authport)) {
		    authaddr = auth.address();
		    try {
			acct = new Session.User(creds.tryauth(auth));
		    } catch(AuthClient.Credentials.AuthException e) {
			settoken(creds.name(), hostname, null);
			ui.uimsg(1, "error", e.getMessage());
			continue retry;
		    }
		    cookie = auth.getcookie();
		    if(Connection.encrypt.get())
			acct.alias(auth.getalias());
		    if(savepw) {
			byte[] ntoken = (creds instanceof AuthClient.TokenCred) ? ((AuthClient.TokenCred)creds).token : auth.gettoken();
			settoken(acct.name, hostname, ntoken);
		    }
		} catch(UnknownHostException e) {
		    ui.uimsg(1, "error", "Could not locate server");
		    continue retry;
		} catch(IOException e) {
		    ui.uimsg(1, "error", e.getMessage());
		    continue retry;
		}
	    }
	    ui.uimsg(1, "prg", "Connecting...");
	    try {
		InetAddress[] addrs = InetAddress.getAllByName(hostname);
		if(addrs.length == 0)
		    throw(new UnknownHostException(hostname));
		preferhost(addrs, authaddr);
		connect: {
		    for(int i = 0; i < addrs.length; i++) {
			if(i > 0)
			    ui.uimsg(1, "prg", String.format("Connecting (address %d/%d)...", i + 1, addrs.length));
			try {
			    sess = new Session(new InetSocketAddress(addrs[i], port), acct, Connection.encrypt.get(), cookie);
			    break connect;
			} catch(Connection.SessionConnError err) {
			} catch(Connection.SessionError err) {
			    ui.uimsg(1, "error", err.getMessage());
			    continue retry;
			}
		    }
		    ui.uimsg(1, "error", "Could not connect to server");
		    continue retry;
		}
	    } catch(UnknownHostException e) {
		ui.uimsg(1, "error", "Could not locate server");
		continue retry;
	    }
	    setpref("loginname", loginname);
	    rottokens(loginname, hostname, false, false);
	    break retry;
	} while(true);
	ui.destroy(1);
	haven.error.ErrorHandler.setprop("usr", sess.user.name);
	return(new RemoteUI(sess));
    }

    public void rcvmsg(int widget, String msg, Object... args) {
	synchronized(msgs) {
	    msgs.add(new Message(widget, msg, args));
	    msgs.notifyAll();
	}
    }
}
