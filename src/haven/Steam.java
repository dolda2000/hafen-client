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
import java.net.*;
import com.codedisaster.steamworks.*;
import com.jogamp.common.os.Platform;
import com.jogamp.common.jvm.JNILibLoaderBase;
import com.jogamp.common.util.cache.TempJarCache;

public class Steam {
    private final Collection<Listener> listening = new HashSet<>();

    public static interface Listener {
	public void callback(String id, Object[] args);
    }

    public void add(Listener l) {
	synchronized(listening) {
	    listening.add(l);
	}
    }

    public void remove(Listener l) {
	synchronized(listening) {
	    listening.remove(l);
	}
    }

    public class Waiter implements Listener, AutoCloseable {
	private final String id;
	private final Queue<Object[]> got = new LinkedList<>();

	public Waiter(String id) {
	    this.id = id;
	    add(this);
	}

	public void callback(String id, Object[] args) {
	    if(id == this.id) {
		synchronized(this) {
		    this.got.add(args);
		    this.notifyAll();
		}
	    }
	}

	public Object[] get() throws InterruptedException {
	    synchronized(this) {
		while(true) {
		    Object[] ret = got.poll();
		    if(ret != null)
			return(ret);
		    this.wait();
		}
	    }
	}

	public void close() {
	    remove(this);
	}
    }

    private void post(String cbid, Object... args) {
	synchronized(listening) {
	    for(Listener l : listening) {
		l.callback(cbid, args);
	    }
	}
    }

    private static class SteamLibraryLoaderJogl extends JNILibLoaderBase implements SteamLibraryLoader {
	public boolean loadLibrary(String nm) {
	    if((Platform.getOSType() == Platform.OSType.WINDOWS) && Platform.is64Bit())
		nm = nm + "64";
	    TempJarCache.initSingleton();
	    addNativeJarLibs(new Class<?>[] {SteamAPI.class}, null);
	    return(loadLibrary(nm, false, SteamAPI.class.getClassLoader()));
	}
    }

    private static boolean loaded = false;
    private static boolean inited = false;
    private static boolean init() {
	if(!loaded) {
	    if(!SteamAPI.loadLibraries(new SteamLibraryLoaderJogl()))
		return(false);
	    loaded = true;
	}
	if(!inited) {
	    try {
		if(!SteamAPI.init())
		    return(false);
		inited = true;
	    } catch(SteamException e) {
		return(false);
	    }
	}
	return(true);
    }

    private Steam() {
	Thread th = new HackThread(this::listen, "Steam callback thread");
	th.setDaemon(true);
	th.start();
    }

    private void listen() {
	try {
	    while(true) {
		SteamAPI.runCallbacks();
		Thread.sleep(100);
	    }
	} catch(InterruptedException e) {
	}
    }

    private static Steam instance = null;
    public static synchronized Steam get() {
	if(instance == null) {
	    if(!init())
		return(null);
	    instance = new Steam();
	}
	return(instance);
    }

    private final SteamFriends friends = new SteamFriends(new SteamFriendsCallback() {
	});
    private final SteamUser user = new SteamUser(new SteamUserCallback() {
	    public void onGetTicketForWebApi(SteamAuthTicket tkt, SteamResult result, byte[] data) {
		post("onGetTicketForWebApi", tkt, result, data);
	    }

	    public void onMicroTxnAuthorization(int appID, long orderID, boolean authorized) {
		post("onMicroTxnAuthorization", appID, orderID, authorized);
	    }
	});

    public synchronized int userid() {
	SteamID id = user.getSteamID();
	if(!id.isValid())
	    return(-1);
	return(id.getAccountID());
    }

    public synchronized String displayname() {
	return(friends.getPersonaName());
    }

    public synchronized void browse(URI uri, boolean modal) {
	friends.activateGameOverlayToWebPage(uri.toString(), modal ? SteamFriends.OverlayToWebPageMode.Modal : SteamFriends.OverlayToWebPageMode.Default);
    }

    public synchronized void setrp(String key, String val) {
	friends.setRichPresence(key, val);
    }

    public void setparty(String pid, int sz) {
	if(pid != null) {
	    setrp("steam_player_group", pid);
	    setrp("steam_player_group_size", Integer.toString(sz));
	} else {
	    setrp("steam_player_group", null);
	    setrp("steam_player_group_size", null);
	}
    }

    public class WebTicket implements AutoCloseable {
	public final byte[] data;
	private final SteamAuthTicket handle;
	private boolean cancelled = false;

	private WebTicket(SteamAuthTicket handle, byte[] data) {
	    this.handle = handle;
	    this.data = data;
	}

	public void close() {
	    synchronized(Steam.this) {
		if(!cancelled) {
		    user.cancelAuthTicket(handle);
		    cancelled = true;
		}
	    }
	}
    }

    public WebTicket webticket() throws InterruptedException, SteamException {
	try(Waiter w = new Waiter("onGetTicketForWebApi")) {
	    SteamAuthTicket tkt;
	    synchronized(this) {
		tkt = user.getAuthTicketForWebApi();
	    }
	    while(true) {
		Object[] cb = w.get();
		if(tkt.equals(cb[0])) {
		    if(cb[1] != SteamResult.OK)
			throw(new SteamException("GetAuthTicketForWebApi failed: " + cb[1]));
		    return(new WebTicket(tkt, (byte[])cb[2]));
		}
	    }
	}
    }

    public static void main(String[] args) throws Exception {
	Steam s = get();
	System.out.printf("%x `%s'\n", s.userid(), s.displayname());
	try(WebTicket tkt = s.webticket()) {
	    System.out.println(Utils.byte2hex(tkt.data));
	}
    }
}
