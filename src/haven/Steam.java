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
import java.util.concurrent.*;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.net.*;
import com.codedisaster.steamworks.*;
import com.jogamp.common.os.Platform;
import com.jogamp.common.jvm.JNILibLoaderBase;
import com.jogamp.common.util.cache.TempJarCache;

public class Steam {
    private final Collection<Listener> listening = new CopyOnWriteArrayList<>();
    private final API api = new API(this);

    public static class SvcError extends RuntimeException {
	public SvcError(String message) {super(message);}
	public SvcError(Throwable cause) {super(cause.getMessage(), cause);}
	public SvcError(String message, Throwable cause) {super(message, cause);}
    }

    public static class ResultError extends SvcError {
	public final SteamResult res;

	public ResultError(String message, SteamResult res) {
	    super(message);
	    this.res = res;
	}
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
	    });
	final SteamUGC ugc = new SteamUGC(new SteamUGCCallback() {
		public void onCreateItem(SteamPublishedFileID id, boolean wla, SteamResult result) {
		    host.post("onCreateItem", id, wla, result);
		}
		public void onSubmitItemUpdate(SteamPublishedFileID id, boolean wla, SteamResult result) {
		    host.post("onSubmitItemUpdate", id, wla, result);
		}
		public void onUGCQueryCompleted(SteamUGCQuery query, int n, int total, boolean cached, SteamResult result) {
		    host.post("onUGCQueryCompleted", query, n, total, cached, result);
		}
		public void onDownloadItemResult(int appid, SteamPublishedFileID item, SteamResult result) {
		    host.post("onDownloadItemResult", appid, item, result);
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

	int fileRead(String name, ByteBuffer buf) throws IOException {
	    try {
		return(rs.fileRead(name, buf));
	    } catch(SteamException e) {
		throw(new IOException(e));
	    }
	}

	void fileWrite(String name, ByteBuffer data) throws IOException {
	    try {
		if(!rs.fileWrite(name, data))
		    throw(new IOException("Steam clould storage write failed for unspecified reasons (quota exceeded?)"));
	    } catch(SteamException e) {
		throw(new IOException(e));
	    }
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

    private ByteBuffer xfbuf = null;
    private ByteBuffer xfbuf(int sz) {
	if((xfbuf == null) || (xfbuf.capacity() < sz))
	    xfbuf = ByteBuffer.allocateDirect(sz);
	xfbuf.clear();
	return(xfbuf);
    }

    public synchronized byte[] readfile(String name) throws IOException {
	checkcloud();
	int sz = api.rs.getFileSize(name);
	if(sz <= 0)
	    throw(new FileNotFoundException(name));
	ByteBuffer buf = xfbuf(sz);
	buf.position(0).limit(sz);
	int rv = api.fileRead(name, buf);
	if(rv == 0)
	    throw(new FileNotFoundException(name));
	buf.position(0).limit(rv);
	byte[] data = new byte[rv];
	buf.get(data);
	return(data);
    }

    public synchronized void writefile(String name, byte[] data) throws IOException {
	checkcloud();
	ByteBuffer buf = xfbuf(data.length);
	buf.put(data);
	buf.flip();
	api.fileWrite(name, buf);
    }

    public class UGItem {
	public final SteamPublishedFileID id;
	private final SteamUGC.ItemInstallInfo iinf = new SteamUGC.ItemInstallInfo();
	private final SteamUGC.ItemDownloadInfo dinf = new SteamUGC.ItemDownloadInfo();
	private Collection<SteamUGC.ItemState> state;

	public UGItem(SteamPublishedFileID id) {
	    this.id = id;
	    update();
	}

	public long fid() {
	    return(SteamNativeHandle.getNativeHandle(id));
	}

	public void update() {
	    state = api.ugc.getItemState(id);
	    if(state.contains(SteamUGC.ItemState.Installed))
		api.ugc.getItemInstallInfo(id, iinf);
	    if(state.contains(SteamUGC.ItemState.NeedsUpdate))
		api.ugc.getItemDownloadInfo(id, dinf);
	}

	public Path path() {
	    String sp = iinf.getFolder();
	    if(sp == null)
		return(null);
	    return(Utils.path(sp));
	}

	public SteamResult dlresult = SteamResult.OK;
	public void download(boolean prioritized) {
	    synchronized(this) {
		if(dlresult != null) {
		    if(!api.ugc.downloadItem(this.id, prioritized))
			throw(new SvcError("download failed for unspecified reasons"));
		    dlresult = null;
		    add(new Listener() {
			    public void callback(String id, Object[] args) {
				if((id == "onDownloadItemResult") && Utils.eq(args[1], UGItem.this.id)) {
				    remove((Listener)this);
				    synchronized(UGItem.this) {
					dlresult = (SteamResult)args[2];
				    }
				}
			    }
			});
		}
	    }
	}

	public boolean installed() {return(state.contains(SteamUGC.ItemState.Installed));}
	public boolean stale() {return(state.contains(SteamUGC.ItemState.NeedsUpdate));}
	public boolean fetching() {return(state.contains(SteamUGC.ItemState.Downloading));}
	public boolean pending() {return(state.contains(SteamUGC.ItemState.DownloadPending));}
	public long got() {return(dinf.getBytesDownloaded());}
	public long size() {return(dinf.getBytesTotal());}

	public class Details {
	    public final String title, description;
	    public final SteamID owner;

	    public Details(SteamUGCDetails info) {
		this.title = info.getTitle();
		this.description = info.getDescription();
		this.owner = info.getOwnerID();
	    }
	}

	private Future<Details> details = null;
	public Future<Details> details() {
	    if(details == null)
		ugqueryitems(Arrays.asList(this));
	    return(details);
	}

	public class Update implements Listener {
	    public final SteamUGCUpdateHandle id;

	    public Update() {
		this.id = api.ugc.startItemUpdate(appid(), UGItem.this.id);
	    }

	    public void title(String title) {api.ugc.setItemTitle(id, title);}
	    public void description(String desc) {api.ugc.setItemDescription(id, desc);}
	    public void metadata(String data) {api.ugc.setItemMetadata(id, data);}
	    public void tags(String... tags) {api.ugc.setItemTags(id, tags);}
	    public void contents(Path dir) {api.ugc.setItemContent(id, dir.toAbsolutePath().toString());}
	    public void preview(Path file) {api.ugc.setItemPreview(id, file.toAbsolutePath().toString());}

	    public void setprivate()     {api.ugc.setItemVisibility(id, SteamRemoteStorage.PublishedFileVisibility.Private);}
	    public void setfriendsonly() {api.ugc.setItemVisibility(id, SteamRemoteStorage.PublishedFileVisibility.FriendsOnly);}
	    public void setpublic()      {api.ugc.setItemVisibility(id, SteamRemoteStorage.PublishedFileVisibility.Public);}

	    public void submit(String message) {
		add((Listener)this);
		api.ugc.submitItemUpdate(id, message);
	    }

	    public void callback(String id, Object[] args) {
		if((id == "onSubmitItemUpdate") && Utils.eq(args[0], UGItem.this.id)) {
		    remove((Listener)this);
		    synchronized(this) {
			agreed = !(Boolean)args[1];
			done = (SteamResult)args[2];
		    }
		}
	    }

	    public SteamResult done = null;
	    public boolean agreed;
	    public SteamUGC.ItemUpdateStatus state = SteamUGC.ItemUpdateStatus.Invalid;
	    public long prog, size;
	    public void getprogress() {
		synchronized(this) {
		    if(done == null) {
			SteamUGC.ItemUpdateInfo buf = new SteamUGC.ItemUpdateInfo();
			state = api.ugc.getItemUpdateProgress(id, buf);
			prog = buf.getBytesProcessed();
			size = buf.getBytesTotal();
		    }
		}
	    }
	}

	public boolean agreed = true;
	public URI legalurl() {
	    return(Utils.uri("steam://url/CommunityFilePage/" +  Long.toUnsignedString(fid())));
	}
    }

    public UGItem ugitem(long id) {
	UGItem item = new UGItem(new SteamPublishedFileID(id));
	/* XXX: Would be nice with a way to validate the ID. */
	return(item);
    }

    public Collection<UGItem> ugitems() {
	SteamPublishedFileID[] items = new SteamPublishedFileID[api.ugc.getNumSubscribedItems()];
	int n = api.ugc.getSubscribedItems(items);
	Collection<UGItem> ret = new ArrayList<>();
	for(int i = 0; i < n; i++)
	    ret.add(new UGItem(items[i]));
	return(ret);
    }

    public UGItem mkugitem() throws InterruptedException {
	try(Waiter w = new Waiter("onCreateItem")) {
	    api.ugc.createItem(appid(), SteamRemoteStorage.WorkshopFileType.Community);
	    Object[] cb = w.get();
	    if(cb[2] != SteamResult.OK)
		throw(new SvcError("CreateItem failed: " + cb[2]));
	    UGItem ret = new UGItem((SteamPublishedFileID)cb[0]);
	    ret.agreed = !(Boolean)cb[1];
	    return(ret);
	}
    }

    public void ugqueryitems(Collection<UGItem> items) {
	class Result extends Future.Simple<UGItem.Details> {
	    final UGItem item;
	    Result(UGItem item) {this.item = item;}
	}
	Collection<SteamPublishedFileID> ids = new ArrayList<>();
	Map<SteamPublishedFileID, Result> futures = new HashMap<>();
	for(UGItem item : items) {
	    ids.add(item.id);
	    Result future = new Result(item);
	    item.details = future;
	    futures.put(item.id, future);
	}
	SteamUGCQuery query = api.ugc.createQueryUGCDetailsRequest(ids);
	class Callback implements Listener {
	    public void callback(String id, Object[] args) {
		if((id == "onUGCQueryCompleted") && Utils.eq(args[0], query)) {
		    remove((Listener)this);
		    try {
			SteamResult res = (SteamResult)args[4];
			if(res == SteamResult.OK) {
			    int n = (Integer)args[1];
			    SteamUGCDetails buf = new SteamUGCDetails();
			    for(int i = 0; i < n; i++) {
				api.ugc.getQueryUGCResult(query, i, buf);
				Result future = futures.remove(buf.getPublishedFileID());
				if(future == null)
				    continue;
				if(buf.getResult() == SteamResult.OK) {
				    future.set(future.item.new Details(buf));
				} else {
				    future.error(new ResultError("UGC result failed: " + buf.getResult(), buf.getResult()));
				}
			    }
			    if(!futures.isEmpty()) {
				SvcError exc = new SvcError("no result received for query");
				for(Result future : futures.values()) {
				    future.error(exc);
				}
			    }
			} else {
			    ResultError exc = new ResultError("UGC query failed: " + res, res);
			    for(Result future : futures.values())
				future.error(exc);
			}
		    } finally {
			api.ugc.releaseQueryUserUGCRequest(query);
		    }
		}
	    }
	}
	add((Listener)new Callback());
	api.ugc.sendQueryUGCRequest(query);
    }

    public static void main(String[] args) throws Exception {
	Steam s = get();
	System.out.printf("%x `%s'\n", s.userid(), s.displayname());
	try(WebTicket tkt = s.webticket()) {
	    System.out.println(Utils.byte2hex(tkt.data));
	}
    }
}
