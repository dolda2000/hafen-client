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
import java.io.*;
import java.nio.*;
import java.net.*;
import com.codedisaster.steamworks.*;
import com.jogamp.common.os.Platform;
import com.jogamp.common.jvm.JNILibLoaderBase;
import com.jogamp.common.util.cache.TempJarCache;

public class Steam {
    private final Collection<Listener> listening = new HashSet<>();
    private final API api = new API(this);

    public static class SvcError extends RuntimeException {
	public SvcError(String message) {super(message);}
	public SvcError(Throwable cause) {super(cause.getMessage(), cause);}
	public SvcError(String message, Throwable cause) {super(message, cause);}
    }

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
	    try {
		if(!API.init())
		    return(null);
	    } catch(NoClassDefFoundError e) {
		return(null);
	    }
	    instance = new Steam();
	}
	return(instance);
    }

    private static class API {
	final Steam host;

	private API(Steam host) {
	    this.host = host;
	}

	final SteamUtils utils = new SteamUtils(new SteamUtilsCallback() {
	    });
	final SteamFriends friends = new SteamFriends(new SteamFriendsCallback() {
	    });
	final SteamRemoteStorage rs = new SteamRemoteStorage(new SteamRemoteStorageCallback() {
		public void onFileReadAsyncComplete(SteamAPICall call, SteamResult result, int offset, int read) {
		    byte[] data = null;
		    if(result == SteamResult.OK) {
			data = new byte[read];
			if(!rs.fileReadAsyncComplete(call, ByteBuffer.wrap(data), read))
			    data = null;
		    }
		    host.post("onFileReadAsyncComplete", call, result, offset, data);
		}
	    });
	final SteamUser user = new SteamUser(new SteamUserCallback() {
		public void onGetTicketForWebApi(SteamAuthTicket tkt, SteamResult result, byte[] data) {
		    host.post("onGetTicketForWebApi", tkt, result, data);
		}

		public void onMicroTxnAuthorization(int appID, long orderID, boolean authorized) {
		    host.post("onMicroTxnAuthorization", appID, orderID, authorized);
		}
	    });

	private static boolean loaded = false, inited = false, failed = false;
	private static synchronized boolean init() {
	    if(failed)
		return(false);
	    if(!loaded) {
		if(!SteamAPI.loadLibraries(new SteamLibraryLoaderJogl())) {
		    failed = true;
		    return(false);
		}
		loaded = true;
	    }
	    if(!inited) {
		try {
		    if(!SteamAPI.init()) {
			failed = true;
			return(false);
		    }
		    inited = true;
		} catch(SteamException e) {
		    failed = true;
		    return(false);
		}
	    }
	    return(true);
	}
    }

    public synchronized int appid() {
	return(api.utils.getAppID());
    }

    public synchronized int userid() {
	SteamID id = api.user.getSteamID();
	if(!id.isValid())
	    return(-1);
	return(id.getAccountID());
    }

    public synchronized String displayname() {
	return(api.friends.getPersonaName());
    }

    public synchronized void browse(URI uri, boolean modal) {
	api.friends.activateGameOverlayToWebPage(uri.toString(), modal ? SteamFriends.OverlayToWebPageMode.Modal : SteamFriends.OverlayToWebPageMode.Default);
    }

    public synchronized void setrp(String key, String val) {
	api.friends.setRichPresence(key, val);
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
		    api.user.cancelAuthTicket(handle);
		    cancelled = true;
		}
	    }
	}
    }

    public WebTicket webticket() throws InterruptedException {
	try(Waiter w = new Waiter("onGetTicketForWebApi")) {
	    SteamAuthTicket tkt;
	    synchronized(this) {
		tkt = api.user.getAuthTicketForWebApi();
	    }
	    while(true) {
		Object[] cb = w.get();
		if(tkt.equals(cb[0])) {
		    if(cb[1] != SteamResult.OK)
			throw(new SvcError("GetAuthTicketForWebApi failed: " + cb[1]));
		    return(new WebTicket(tkt, (byte[])cb[2]));
		}
	    }
	}
    }

    public static enum StorageStatus {
	OK, OFF_APP, OFF_USR,
    }

    public StorageStatus checkstorage() {
	if(!api.rs.isCloudEnabledForApp())
	    return(StorageStatus.OFF_APP);
	if(!api.rs.isCloudEnabledForAccount())
	    return(StorageStatus.OFF_USR);
	return(StorageStatus.OK);
    }

    public void enablestorage(boolean on) {
	api.rs.setCloudEnabledForApp(on);
    }

    private void checkcloud() throws IOException {
	switch(checkstorage()) {
	case OFF_APP: throw(new IOException("Steam cloud storage not enabled for app"));
	case OFF_USR: throw(new IOException("Steam cloud storage locally disabled"));
	}
    }

    public class FileReader {
	public final String name;
	public final int sz;

	public FileReader(String name) throws IOException {
	    checkcloud();
	    this.name = name;
	    this.sz = api.rs.getFileSize(name);
	    if(!api.rs.fileExists(name) || this.sz == 0)
		throw(new FileNotFoundException(name));
	}

	public byte[] read(int off, int len) throws IOException {
	    if((off >= sz) || (len <= 0))
		return(new byte[0]);
	    len = Math.min(len, sz - off);
	    try(Waiter w = new Waiter("onFileReadAsyncComplete")) {
		SteamAPICall call = api.rs.fileReadAsync(name, off, len);
		if(!call.isValid())
		    throw(new IOException("Steam async read failed for unspecified reasons"));
		while(true) {
		    Object[] cb;
		    try {
			cb = w.get();
		    } catch(InterruptedException e) {
			throw((IOException)new java.nio.channels.ClosedByInterruptException().initCause(e));
		    }
		    if(cb[0] != call)
			continue;
		    if(cb[1] != SteamResult.OK)
			throw(new IOException("Steam async read failed: " + cb[1]));
		    if(((Number)cb[2]).intValue() != off)
			throw(new IOException("Steam async read file offset unexpectedly mismatched: requested " + off + ", got " + cb[2]));
		    return((byte[])cb[3]);
		}
	    }
	}
    }

    public class FileWriter {
	public final SteamUGCFileWriteStreamHandle h;
	private boolean closed = false;

	public FileWriter(String name) throws IOException {
	    checkcloud();
	    this.h = api.rs.fileWriteStreamOpen(name);
	    if(SteamNativeHandle.getNativeHandle(this.h) == -1)
		throw(new IOException("Steam cloud storage failed for unspecified reasons (quota exceeded?)"));
	}

	public void cancel() throws IOException {
	    if(!closed && !api.rs.fileWriteStreamCancel(h))
		throw(new IOException("Steam cloud abort failed for unspecified reasons"));
	    closed = true;
	}

	public void close() throws IOException {
	    if(!closed && !api.rs.fileWriteStreamClose(h))
		throw(new IOException("Steam cloud commit failed for unspecified reasons"));
	    closed = true;
	}

	public void write(byte[] data, int off, int len) throws IOException {
	    ByteBuffer buf = ByteBuffer.allocateDirect(len);
	    buf.put(data, off, len);
	    buf.flip();
	    if(!api.rs.fileWriteStreamWriteChunk(h, buf))
		throw(new IOException("Steam cloud write failed for unspecified reasons (quota exceeded?)"));
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
