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
import java.util.function.*;
import java.nio.channels.ClosedByInterruptException;

public class Bootstrap implements UI.Receiver, UI.Runner {
    public static final Config.Variable<String> authuser = Config.Variable.prop("haven.authuser", null);
    public static final Config.Variable<NamedSocketAddress> authserv = Config.Variable.proph("haven.server", AuthClient.DEFPORT, new NamedSocketAddress("localhost", AuthClient.DEFPORT));
    public static final Config.Variable<NamedSocketAddress> gameserv = Config.Variable.proph("haven.gameserv", 1870, null);
    public static final Config.Variable<Integer> gameport = Config.Variable.propi("haven.gameport", 1870);
    public static final Config.Variable<byte[]> authck = Config.Variable.propb("haven.authck", null);
    public static final Config.Variable<byte[]> authtoken = Config.Variable.propb("haven.inittoken", null);
    public static final Config.Variable<String[]> servargs = Config.Variable.def(() -> null);
    public static final Config.Variable<java.nio.file.Path> replay = Config.Variable.def(() -> null);
    public static boolean useinitauth = true;
    public final NamedSocketAddress server;
    public final String confname;
    private final EventHandler.Source<Message> msgsrc = new EventHandler.Source<>();
    String inituser = null;
    byte[] initcookie = null;
    byte[] inittoken = null;

    public static class Message {
	public final int id;
	public final String name;
	public final Object[] args;

	public Message(int id, String name, Object... args) {
	    this.id = id;
	    this.name = name;
	    this.args = args;
	}
    }

    public Bootstrap(NamedSocketAddress server) {
	this.server = server;
	this.confname = server.host;
    }

    public Bootstrap() {
	this(authserv.get());
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
	return(Utils.getpref(name + "@" + confname, def));
    }

    private void setpref(String name, String val) {
	Utils.setpref(name + "@" + confname, val);
    }

    private static byte[] getprefb(String name, String confname, byte[] def, boolean zerovalid) {
	String sv = Utils.getpref(name + "@" + confname, null);
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

    public static byte[] gettoken(String user, String confname) {
	return(getprefb("savedtoken-" + mangleuser(user), confname, null, false));
    }

    public static void rottokens(String user, String confname, boolean creat, boolean rm) {
	List<String> names = new ArrayList<>(Utils.getprefsl("saved-tokens@" + confname, new String[] {}));
	creat = creat || (!rm && names.contains(user));
	if(rm || creat)
	    names.remove(user);
	if(creat)
	    names.add(0, user);
	Utils.setprefsl("saved-tokens@" + confname, names);
    }

    public static void settoken(String user, String confname, byte[] token) {
	if(user == null)
	    return;
	String prefnm = user;
	Utils.setpref("savedtoken-" + mangleuser(user) + "@" + confname, (token == null) ? null : Utils.hex.enc(token));
	rottokens(user, confname, token != null, true);
    }

    private Promise<Message> fmessage(Predicate<Message> filter) {
	Promise<Message> ret = new Promise<>();
	msgsrc.add(new EventHandler<Message>() {
	    {ret.cancellable(this::remove);}

	    public boolean handle(Message msg) {
		if(filter.test(msg)) {
		    remove();
		    msgsrc.remove(this);
		    ret.resolve(msg);
		    return(true);
		}
		return(false);
	    }

	    private void remove() {
		msgsrc.remove(this);
	    }
	});
	return(ret);
    }

    public static void preferhost(List<InetSocketAddress> hosts, SocketAddress prev) {
	if((prev == null) || !(prev instanceof InetSocketAddress))
	    return;
	InetAddress host = ((InetSocketAddress)prev).getAddress();
	Collections.sort(hosts, (a, b) -> {
		boolean pa = Utils.eq(a.getAddress(), host), pb = Utils.eq(b.getAddress(), host);
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

    public static class Auth {
	public final Session.User acct;
	public final byte[] cookie;

	public Auth(Session.User acct, byte[] cookie) {
	    this.acct = acct;
	    this.cookie = cookie;
	}
    }

    public static interface AuthMech {
	public Auth authenticate(AuthClient auth) throws IOException, InterruptedException;
    }

    public static class ConnectError extends RuntimeException {
	public ConnectError(String msg, Throwable cause) {super(msg, cause);}
	public ConnectError(String msg) {this(msg, null);}
	public ConnectError(Throwable cause) {this(cause.getMessage(), cause);}
    }

    public class Connector {
	public AuthMech authmech = null;
	public SocketAddress authaddr = null;
	public Auth auth = null;
	List<NamedSocketAddress> hosts = Collections.emptyList();

	public Connector(Auth auth) {
	    this.auth = auth;
	}

	public Connector(AuthMech authmech) {
	    this.authmech = authmech;
	}

	public Session connect(UI ui) throws InterruptedException {
	    if(auth == null) {
		ui.uimsg(1, "prg", "Authenticating...");
		try(AuthClient cl = new AuthClient(server)) {
		    auth = authmech.authenticate(cl);
		    if(Connection.encrypt.get())
			auth.acct.alias(cl.getalias());
		    hosts = cl.gethosts(new NamedSocketAddress(server.host, gameport.get()));
		    authaddr = cl.address();
		} catch(ClosedByInterruptException e) {
		    throw(new InterruptedException());
		} catch(UnknownHostException e) {
		    throw(new ConnectError("Could not locate server"));
		} catch(ConnectException e) {
		    throw(new ConnectError("Could not connect to server"));
		} catch(IOException e) {
		    throw(new RuntimeException(e));
		}
	    }
	    try {
		List<InetSocketAddress> addrs = new ArrayList<>();
		NamedSocketAddress ea = gameserv.get();
		if(ea != null) {
		    for(InetAddress addr : InetAddress.getAllByName(ea.host))
			addrs.add(new InetSocketAddress(addr, ea.port));
		    if(addrs.isEmpty())
			throw(new UnknownHostException(ea.host));
		} else {
		    if(hosts.isEmpty()) {
			for(InetAddress addr : InetAddress.getAllByName(server.host))
			    addrs.add(new InetSocketAddress(addr, gameport.get()));
			if(addrs.isEmpty())
			    throw(new UnknownHostException(ea.host));
		    } else {
			for(NamedSocketAddress addr : hosts) {
			    for(InetAddress host : InetAddress.getAllByName(addr.host))
				addrs.add(new InetSocketAddress(host, addr.port));
			}
			if(addrs.isEmpty())
			    throw(new UnknownHostException(server.host));
		    }
		}
		preferhost(addrs, authaddr);
		for(int i = 0; i < addrs.size(); i++) {
		    if(i > 0)
			ui.uimsg(1, "prg", String.format("Connecting (address %d/%d)...", i + 1, addrs.size()));
		    else
			ui.uimsg(1, "prg", "Connecting...");
		    try {
			return(Session.connect(addrs.get(i), auth.acct, Connection.encrypt.get(), auth.cookie));
		    } catch(Connection.SessionConnError err) {
		    } catch(Connection.SessionError err) {
			throw(new ConnectError(err));
		    }
		}
		throw(new ConnectError("Could not connect to server"));
	    } catch(UnknownHostException e) {
		throw(new ConnectError("Could not locate server"));
	    }
	}
    }

    public Connector initauth() {
	if(initcookie != null) {
	    Auth auth = new Auth(new Session.User(inituser), initcookie);
	    initcookie = null;
	    return(new Connector(auth));
	}
	return(null);
    }

    public Connector initmech() {
	if((this.inituser == null) || (this.inittoken == null))
	    return(null);
	byte[] inittoken = this.inittoken;
	this.inittoken = null;
	return(new Connector(auth -> {
	    if(!Arrays.equals(inittoken, getprefb("lasttoken-" + mangleuser(inituser), confname, null, false))) {
		AuthClient.Credentials creds = new AuthClient.TokenCred(inituser, inittoken);
		Session.User acct = null;
		try {
		    acct = creds.tryauth(auth);
		} catch(AuthClient.Credentials.AuthException e) {
		}
		setpref("lasttoken-" + mangleuser(inituser), Utils.hex.enc(inittoken));
		if(acct != null) {
		    settoken(creds.authname(), confname, auth.gettoken());
		    return(new Auth(acct, auth.getcookie()));
		}
	    }
	    byte[] token = gettoken(inituser, confname);
	    if(token != null) {
		AuthClient.Credentials creds = new AuthClient.TokenCred(inituser, token);
		try {
		    Session.User acct = creds.tryauth(auth);
		    return(new Auth(acct, auth.getcookie()));
		} catch(AuthClient.Credentials.AuthException e) {
		    settoken(creds.authname(), confname, null);
		}
	    }
	    throw(new ConnectError("Launcher login expired"));
	}));
    }

    public Connector credauth(AuthClient.Credentials creds, boolean savetoken) {
	return(new Connector(auth -> {
	    Session.User acct;
	    try {
		acct = creds.tryauth(auth);
	    } catch(AuthClient.Credentials.AuthException e) {
		settoken(creds.authname(), confname, null);
		throw(new ConnectError(e));
	    }
	    if(savetoken) {
		byte[] ntoken = (creds instanceof AuthClient.TokenCred) ? ((AuthClient.TokenCred)creds).token : auth.gettoken();
		settoken(creds.authname(), confname, ntoken);
	    }
	    return(new Auth(acct, auth.getcookie()));
	}));
    }

    private Session connect(Connector c, UI ui) throws InterruptedException {
	Promise<Session> ret = new Promise<>();
	Thread th = new HackThread(() -> {
	    try {
		ret.resolve(c.connect(ui));
	    } catch(InterruptedException e) {
		ret.resolve(null);
	    }
	}, "Session connector");
	th.start();
	Promise<Message> fmsg = fmessage(m -> ((m.id == 1) && (m.name == "cancel")));
	try {
	    fmsg.consume(msg -> th.interrupt());
	    return(ret.waitfor(true, true));
	} catch(Future.PastException e) {
	    throw((RuntimeException)e.getCause());
	} finally {
	    fmsg.cancel();
	}
    }

    public UI.Runner run(UI ui) throws InterruptedException {
	ui.setreceiver(this);
	ui.newwidgetp(1, ($1, $2) -> new LoginScreen(confname), 0, new Object[] {Coord.z});
	Session sess;
	while(true) {
	    Connector c = initauth();
	    String loginname = null;
	    if(c == null)
		c = initmech();
	    if(c == null) {
		ui.uimsg(1, "login");
		Message msg = fmessage(m -> ((m.id == 1) && (m.name == "login"))).waitfor(true, true);
		AuthClient.Credentials creds = (AuthClient.Credentials)msg.args[0];
		loginname = creds.authname();
		c = credauth(creds, (Boolean)msg.args[1]);
	    }
	    try {
		if((sess = connect(c, ui)) == null)
		    continue;
	    } catch(ConnectError e) {
		ui.uimsg(1, "error", e.getMessage());
		continue;
	    } catch(RuntimeException e) {
		new Warning(e, "unexpected connector error").issue();
		ui.uimsg(1, "error", String.format("Unexpected error: %s: %s", e.getClass().getSimpleName(), e.getMessage()));
		continue;
	    }
	    if(loginname != null) {
		setpref("loginname", loginname);
		rottokens(loginname, confname, false, false);
	    }
	    break;
	}
	ui.destroy(1);
	haven.error.ErrorHandler.setprop("usr", sess.user.name);
	return(new RemoteUI(sess));
    }

    public void rcvmsg(int widget, String msg, Object... args) {
	msgsrc.submit(new Message(widget, msg, args));
    }
}
