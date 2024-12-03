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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.annotation.*;
import java.util.*;
import java.util.function.*;
import java.util.regex.*;
import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import javax.imageio.*;
import java.awt.image.BufferedImage;

public class Resource implements Serializable {
    public static final Config.Variable<URI> resurl = Config.Variable.propu("haven.resurl", "");
    public static final Config.Variable<Path> resdir = Config.Variable.propp("haven.resdir", System.getenv("HAFEN_RESDIR"));
    private static ResCache prscache;
    public static ThreadGroup loadergroup = null;
    private static Map<String, LayerFactory<?>> ltypes = new TreeMap<String, LayerFactory<?>>();
    public static Class<Image> imgc = Image.class;
    public static Class<Neg> negc = Neg.class;
    public static Class<Props> props = Props.class;
    public static Class<Obstacle> obst = Obstacle.class;
    public static Class<Anim> animc = Anim.class;
    public static Class<Pagina> pagina = Pagina.class;
    public static Class<AButton> action = AButton.class;
    public static Class<Audio> audio = Audio.class;
    public static Class<Tooltip> tooltip = Tooltip.class;
    
    public final String name;
    public int ver;
    public ResSource source;
    public final transient Pool pool;
    protected Collection<Layer> layers = new LinkedList<Layer>();
    private boolean used = false;

    public abstract static class Named implements Indir<Resource>, Serializable {
	public final String name;
	public final int ver;

	public Named(String name, int ver) {
	    this.name = name;
	    this.ver = ver;
	}

	public boolean equals(Object other) {
	    if(!(other instanceof Named))
		return(false);
	    Named o = (Named)other;
	    return(o.name.equals(this.name) && (o.ver == this.ver));
	}

	public int hashCode() {
	    int ret = name.hashCode();
	    ret = (ret * 31) + ver;
	    return(ret);
	}

	public String toString() {
	    return(String.format("#<res-name %s v%d>", name, ver));
	}
    }

    public static class Spec extends Named implements Serializable {
	public final transient Pool pool;

	public Spec(Pool pool, String name, int ver) {
	    super(name, ver);
	    this.pool = pool;
	}

	public Spec(Pool pool, String name) {
	    this(pool, name, -1);
	}

	public Resource get(int prio) {
	    return(pool.load(name, ver, prio).get());
	}
	
	public Resource get() {
	    return(get(0));
	}
   }

    public static class Saved extends Named implements Serializable {
	public final transient Pool pool;
	public int prio = 0;
	private transient Indir<Resource> wver = null;
	private Throwable verr = null;
	private transient Resource loaded;

	public Saved(Pool pool, String name, int ver) {
	    super(name, ver);
	    this.pool = pool;
	}

	public Resource get(int prio) {
	    if(loaded != null)
		return(loaded);
	    if(verr == null) {
		try {
		    if(wver == null)
			wver = pool.load(name, ver, prio);
		    return(loaded = wver.get());
		} catch(Loading l) {
		    throw(l);
		} catch(Exception e) {
		    verr = e;
		    wver = null;
		}
	    }
	    try {
		return(loaded = pool.load(name, -1, prio).get());
	    } catch(Throwable t) {
		t.addSuppressed(verr);
		throw(t);
	    }
	}

	public Resource get() {
	    return(get(prio));
	}

	public int savever() {
	    if((loaded != null) && (loaded.ver > this.ver))
		return(loaded.ver);
	    return(this.ver);
	}
    }

    public static interface Resolver {
	public Indir<Resource> getres(int id);

	public default Indir<Resource> dynres(UID uid) {
	    return(() -> {throw(new NoSuchResourceException(String.format("dyn/%x", uid.longValue()), 1, null));});
	}

	public default Indir<Resource> getresv(Object desc) {
	    if(desc == null)
		return(null);
	    if(desc instanceof UID)
		return(dynres((UID)desc));
	    if(desc instanceof Number) {
		int id = ((Number)desc).intValue();
		if(id < 0)
		    return(null);
		return(this.getres(id));
	    }
	    if(desc instanceof Resource)
		return(((Resource)desc).indir());
	    if(desc instanceof Indir) {
		@SuppressWarnings("unchecked") Indir<Resource> ret = (Indir<Resource>)desc;
		return(ret);
	    }
	    throw(new ClassCastException("unknown type for resource id: " + desc));
	}

	public class ResourceMap implements Resource.Resolver {
	    public final Resource.Resolver bk;
	    public final Map<Integer, Integer> map;

	    public ResourceMap(Resource.Resolver bk, Map<Integer, Integer> map) {
		this.bk = bk;
		this.map = map;
	    }

	    public ResourceMap(Resource.Resolver bk, Message data) {
		this(bk, decode(data));
	    }

	    public ResourceMap(Resource.Resolver bk, Object[] args) {
		this(bk, decode(args));
	    }

	    public static Map<Integer, Integer> decode(Message sdt) {
		if(sdt.eom())
		    return(Collections.emptyMap());
		int n = sdt.uint8();
		Map<Integer, Integer> ret = new HashMap<>();
		for(int i = 0; i < n; i++)
		    ret.put(sdt.uint16(), sdt.uint16());
		return(ret);
	    }

	    public static Map<Integer, Integer> decode(Object[] args) {
		if(args.length == 0)
		    return(Collections.emptyMap());
		Map<Integer, Integer> ret = new HashMap<>();
		for(int a = 0; a < args.length; a += 2)
		    ret.put(Utils.iv(args[a]), Utils.iv(args[a + 1]));
		return(ret);
	    }

	    public Indir<Resource> getres(int id) {
		return(bk.getres(map.get(id)));
	    }

	    public Indir<Resource> dynres(UID uid) {
		return(bk.dynres(uid));
	    }

	    public String toString() {
		return(map.toString());
	    }
	}
    }

    private Resource(Pool pool, String name, int ver) {
	this.pool = pool;
	this.name = name;
	this.ver = ver;
    }

    public static class Virtual extends Resource {
	public Virtual(Pool pool, String name, int ver) {
	    super(pool, name, ver);
	}

	public Virtual(String name, int ver) {
	    this(remote(), name, ver);
	}

	public void add(Layer layer) {
	    layers.add(layer);
	}
    }
	
    public static void setcache(ResCache cache) {
	prscache = cache;
    }

    public String basename() {
	int p = name.lastIndexOf('/');
	if(p < 0)
	    return(name);
	return(name.substring(p + 1));
    }

    public static interface ResSource {
	public InputStream get(String name) throws IOException;
    }
    
    public static abstract class TeeSource implements ResSource, Serializable {
	public ResSource back;
	
	public TeeSource(ResSource back) {
	    this.back = back;
	}
	
	public InputStream get(String name) throws IOException {
	    StreamTee tee = new StreamTee(back.get(name));
	    tee.setncwe();
	    tee.attach(fork(name));
	    return(tee);
	}
	
	public abstract OutputStream fork(String name) throws IOException;
	
	public String toString() {
	    return("forking source backed by " + back);
	}
    }
    
    public static class CacheSource implements ResSource, Serializable {
	public final transient ResCache cache;
	public final String cachedesc;
	
	public CacheSource(ResCache cache) {
	    this.cache = cache;
	    this.cachedesc = String.valueOf(cache);
	}
	
	public InputStream get(String name) throws IOException {
	    return(cache.fetch("res/" + name));
	}
	
	public String toString() {
	    return("cache source backed by " + cachedesc);
	}
    }

    public static class FileSource implements ResSource, Serializable {
	public static final Collection<String> wintraps =
	    new HashSet<>(Arrays.asList("con", "prn", "aux", "nul",
					"com0", "com1", "com2", "com3", "com4",
					"com5", "com6", "com7", "com8", "com9",
					"lpt0", "lpt1", "lpt2", "lpt3", "lpt4",
					"lpt5", "lpt6", "lpt7", "lpt8", "lpt9"));
	public static final boolean windows = System.getProperty("os.name", "").startsWith("Windows");
	private static final boolean[] winsafe;
	public final Path base;

	static {
	    boolean[] buf = new boolean[128];
	    String safe = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_@";
	    for(int i = 0; i < safe.length(); i++)
		buf[safe.charAt(i)] = true;
	    winsafe = buf;
	}

	public static boolean winsafechar(char c) {
	    return((c >= winsafe.length) || winsafe[c]);
	}

	public FileSource(Path base) {
	    this.base = base;
	}

	private static String checkpart(String part, String whole) throws FileNotFoundException {
	    if(windows && wintraps.contains(part))
		throw(new FileNotFoundException(whole));
	    return(part);
	}

	public InputStream get(String name) throws IOException {
	    Path cur = base;
	    String[] parts = name.split("/");
	    for(int i = 0; i < parts.length - 1; i++)
		cur = cur.resolve(checkpart(parts[i], name));
	    cur = cur.resolve(checkpart(parts[parts.length - 1], name) + ".res");
	    try {
		return(Files.newInputStream(cur));
	    } catch(NoSuchFileException e) {
		throw((FileNotFoundException)new FileNotFoundException(name).initCause(e));
	    }
	}

	public String toString() {
	    return("filesystem res source (" + base + ")");
	}
    }

    public static class JarSource implements ResSource, Serializable {
	public final String base;

	public JarSource(String base) {
	    this.base = base;
	}

	public InputStream get(String name) throws FileNotFoundException {
	    String full = "/" + base + "/" + name + ".res";
	    InputStream s = Resource.class.getResourceAsStream(full);
	    if(s == null)
		throw(new FileNotFoundException("Could not find resource locally: " + full));
	    return(s);
	}

	public String toString() {
	    return("local res source (" + base + ")");
	}
    }

    public static class HttpSource implements ResSource, Serializable {
	public URI base;

	public HttpSource(URI base) {
	    this.base = base;
	}

	private URI encodeuri(URI raw) throws IOException {
	    /* This is kinda crazy, but it is, actually, how the Java
	     * documentation recommends that it be done... */
	    try {
		return(new URI(new URI(raw.getScheme(), raw.getUserInfo(), raw.getHost(), raw.getPort(), raw.getPath(), raw.getQuery(), raw.getFragment()).toASCIIString()));
	    } catch(URISyntaxException e) {
		throw(new IOException(e));
	    }
	}

	public InputStream get(String name) throws IOException {
	    return(Http.fetch(encodeuri(base.resolve(name + ".res")).toURL(), c -> {
			/* Apparently, some versions of Java Web Start has
			 * a bug in its internal cache where it refuses to
			 * reload a URL even when it has changed. */
			c.setUseCaches(false);
		    }));
	}

	public String toString() {
	    return("HTTP res source (" + base + ")");
	}
    }

    public static class Loading extends haven.Loading {
	private final Pool.Queued res;

	private Loading(Pool.Queued res) {
	    super("Waiting for resource " + res.name + "...");
	    this.res = res;
	}

	public String toString() {
	    return("#<Resource " + res.name + ">");
	}

	public void waitfor(Runnable callback, Consumer<Waitable.Waiting> reg) {
	    synchronized(res) {
		if(res.done) {
		    reg.accept(Waitable.Waiting.dummy);
		    callback.run();
		} else {
		    reg.accept(res.wq.add(callback));
		}
	    }
	}

	public boolean boostprio(int prio) {
	    res.boostprio(prio);
	    return(true);
	}
    }

    public static class BadResourceException extends RuntimeException {
	public final String name;
	public final int ver;

	public BadResourceException(String name, int ver, String message, Throwable cause) {
	    super(message, cause);
	    this.name = name;
	    this.ver = ver;
	}

	public BadResourceException(String name, int ver, String message) {
	    this(name, ver, message, null);
	}

	public BadResourceException(String name, int ver, Throwable cause) {
	    this(name, ver, null, cause);
	}

	public BadResourceException(String name, int ver) {
	    this(name, ver, null, null);
	}
    }

    public static class LoadFailedException extends BadResourceException {
	public LoadFailedException(String name, int ver, LoadException cause) {
	    super(name, ver, cause);
	}

	public String getMessage() {
	    return(String.format("Failed to load resource %s (v%d)", name, ver));
	}
    }

    public static class NoSuchResourceException extends LoadFailedException {
	public NoSuchResourceException(String name, int ver, LoadException cause) {
	    super(name, ver, cause);
	}
    }

    public static class BadVersionException extends BadResourceException {
	public final int curver;
	public final String cursrc;

	public BadVersionException(String name, int ver, int curver, ResSource cursrc) {
	    super(name, ver);
	    this.curver = curver;
	    this.cursrc = (cursrc == null) ? null : String.valueOf(cursrc);
	}

	public String getMessage() {
	    if(cursrc == null)
		return(String.format("Obsolete version %d of %s requested, loaded version is %d", ver, name, curver));
	    else
		return(String.format("Obsolete version %d of %s requested, loaded version is %d, from %s", ver, name, curver, cursrc));
	}
    }

    public static class Pool {
	public int nloaders = 2;
	private final Collection<Loader> loaders = new LinkedList<Loader>();
	private final List<ResSource> sources = new LinkedList<ResSource>();
	private final Map<String, Resource> cache = new CacheMap<String, Resource>();
	private final PrioQueue<Queued> queue = new PrioQueue<Queued>();
	private final Map<String, Queued> queued = new HashMap<String, Queued>();
	private final Pool parent;

	public Pool(Pool parent, ResSource... sources) {
	    this.parent = parent;
	    for(ResSource source : sources)
		this.sources.add(source);
	}

	public Pool(ResSource... sources) {
	    this(null, sources);
	}

	public void add(ResSource src) {
	    sources.add(src);
	}

	private class Queued extends Named implements Prioritized, Serializable {
	    transient final Collection<Queued> rdep = new LinkedList<Queued>();
	    final Waitable.Queue wq = new Waitable.Queue();
	    volatile int prio;
	    Queued awaiting;
	    volatile boolean done = false;
	    Resource res;
	    LoadException error;
	    boolean found = false;

	    Queued(String name, int ver, int prio) {
		super(name, ver);
		this.prio = prio;
	    }

	    public int priority() {
		return(prio);
	    }

	    public void boostprio(int prio) {
		if(this.prio < prio)
		    this.prio = prio;
		Queued p = awaiting;
		if(p != null)
		    p.boostprio(prio);
	    }

	    public Resource get() {
		if(!done)
		    throw(new Loading(this));
		if(error != null) {
		    if(!found)
			throw(new NoSuchResourceException(name, ver, error));
		    throw(new LoadFailedException(name, ver, error));
		}
		return(res);
	    }

	    private void done() {
		synchronized(this) {
		    done = true;
		    for(Iterator<Queued> i = rdep.iterator(); i.hasNext();) {
			Queued dq = i.next();
			i.remove();
			dq.prior(this);
		    }
		    wq.wnotify();
		}
		if(res != null) {
		    synchronized(cache) {
			cache.put(name, res);
		    }
		    synchronized(queue) {
			queued.remove(name);
		    }
		}
	    }

	    private void prior(Queued prior) {
		if((res = prior.res) == null) {
		    error = prior.error;
		    synchronized(queue) {
			queue.add(this);
			queue.notify();
		    }
		    ckld();
		} else {
		    done();
		}
	    }

	    public String toString() {
		return(String.format("<q:%s(v%d)>", name, ver));
	    }
	}

	private void handle(Queued res) {
	    for(ResSource src : sources) {
		try(InputStream in = src.get(res.name)) {
		    Message msg = new StreamMessage(in);
		    if(msg.eom()) {
			/* XXX? This should not be necessary, but for some reason
			 * it seems that custom client resources find their way to
			 * create empty cache files by the same name. I don't know
			 * how. */
			throw(new FileNotFoundException("empty file"));
		    }
		    res.found = true;
		    Resource ret = new Resource(this, res.name, res.ver);
		    ret.source = src;
		    ret.load(msg);
		    res.res = ret;
		    res.error = null;
		    break;
		} catch(Throwable t) {
		    if(!(t instanceof FileNotFoundException))
			res.found = true;
		    LoadException error;
		    if(t instanceof LoadException)
			error = (LoadException)t;
		    else
			error = new LoadException(String.format("Load error in resource %s(v%d), from %s", res.name, res.ver, src), t, null);
		    error.src = src;
		    if(res.error != null) {
			error.prev = res.error;
			error.addSuppressed(res.error);
		    }
		    res.error = error;
		}
	    }
	    res.done();
	}

	public Named load(String name, int ver, int prio) {
	    Queued ret;
	    synchronized(cache) {
		Resource cur = cache.get(name);
		if(cur != null) {
		    if((ver == -1) || (cur.ver == ver)) {
			return(cur.indir());
		    } else if(ver < cur.ver) {
			throw(new BadVersionException(name, ver, cur.ver, cur.source));
		    }
		}
		synchronized(queue) {
		    Queued cq = queued.get(name);
		    if(cq != null) {
			if(ver != -1) {
			    if(ver < cq.ver) {
				throw(new BadVersionException(name, ver, cq.ver, null));
			    } else if(ver == cq.ver) {
				cq.boostprio(prio);
				return(cq);
			    }
			} else {
			    if((cq.ver != -1) && cq.done && (cq.error != null)) {
				/* XXX: This is probably not the right way to handle this. */
			    } else {
				cq.boostprio(prio);
				return(cq);
			    }
			}
			queued.remove(name);
			queue.removeid(cq);
		    }
		    Queued nq = new Queued(name, ver, prio);
		    if(parent == null) {
			queued.put(name, nq);
			queue.add(nq);
			queue.notify();
		    } else {
			Indir<Resource> pr = parent.load(name, ver, prio);
			if(pr instanceof Queued) {
			    Queued pq = (Queued)pr;
			    synchronized(pq) {
				if(pq.done) {
				    nq.prior(pq);
				} else {
				    nq.awaiting = pq;
				    pq.rdep.add(nq);
				}
			    }
			    queued.put(name, nq);
			} else {
			    nq.res = pr.get();
			    nq.done = true;
			}
		    }
		    ret = nq;
		}
	    }
	    ckld();
	    return(ret);
	}

	public Named load(String name, int ver) {return(load(name, ver, 0));}
	public Named load(String name) {return(load(name, -1));}

	public Indir<Resource> dynres(long id) {
	    return(load(String.format("dyn/%x", id), 1));
	}

	public Indir<Resource> dynres(UID id) {
	    return(dynres(id.bits));
	}

	private void ckld() {
	    int qsz;
	    synchronized(queue) {
		qsz = queue.size();
	    }
	    synchronized(loaders) {
		while(loaders.size() < Math.min(nloaders, qsz)) {
		    final Loader n = new Loader();
		    Thread th = new HackThread(loadergroup, n, "Haven resource loader");
		    th.setDaemon(true);
		    th.start();
		    while(!n.added) {
			try {
			    loaders.wait();
			} catch(InterruptedException e) {
			    Thread.currentThread().interrupt();
			    return;
			}
		    }
		}
	    }
	}

	public class Loader implements Runnable {
	    private boolean added = false;

	    public void run() {
		synchronized(loaders) {
		    loaders.add(this);
		    added = true;
		    loaders.notifyAll();
		}
		boolean intd = false;
		try {
		    while(true) {
			Queued cur;
			synchronized(queue) {
			    long start = System.currentTimeMillis(), now = start;
			    while((cur = queue.poll()) == null) {
				queue.wait(10000 - (now - start));
				now = System.currentTimeMillis();
				if(now - start >= 10000)
				    return;
			    }
			}
			handle(cur);
			cur = null;
		    }
		} catch(InterruptedException e) {
		    intd = true;
		} finally {
		    synchronized(loaders) {
			loaders.remove(this);
		    }
		    if(!intd)
			ckld();
		}
	    }
	}

	public int qdepth() {
	    int ret = (parent == null)?0:parent.qdepth();
	    synchronized(queue) {
		ret += queue.size();
	    }
	    return(ret);
	}

	public int numloaded() {
	    int ret = (parent == null)?0:parent.numloaded();
	    synchronized(cache) {
		ret += cache.size();
	    }
	    return(ret);
	}

	public Collection<Resource> cached() {
	    Set<Resource> ret = new HashSet<Resource>();
	    if(parent != null)
		ret.addAll(parent.cached());
	    synchronized(cache) {
		ret.addAll(cache.values());
	    }
	    return(ret);
	}

	public Collection<Resource> used() {
	    Collection<Resource> ret = cached();
	    for(Iterator<Resource> i = ret.iterator(); i.hasNext();) {
		Resource r = i.next();
		if(!r.used)
		    i.remove();
	    }
	    return(ret);
	}

	private final Set<Resource> loadwaited = new HashSet<Resource>();
	public Collection<Resource> loadwaited() {
	    Set<Resource> ret = new HashSet<Resource>();
	    if(parent != null)
		ret.addAll(parent.loadwaited());
	    synchronized(loadwaited) {
		ret.addAll(loadwaited);
	    }
	    return(ret);
	}

	private Resource loadwaited(Resource res) {
	    synchronized(loadwaited) {
		loadwaited.add(res);
	    }
	    return(res);
	}

	public Resource loadwaitint(String name, int ver) throws InterruptedException {
	    return(loadwaited(Loading.waitforint(load(name, ver, 10))));
	}

	public Resource loadwaitint(String name) throws InterruptedException {
	    return(loadwaitint(name, -1));
	}

	public Resource loadwait(String name, int ver) {
	    return(loadwaited(Loading.waitfor(load(name, ver, 10))));
	}

	public Resource loadwait(String name) {
	    return(loadwait(name, -1));
	}
    }

    private static Pool _local = null;
    public static Pool local() {
	if(_local == null) {
	    synchronized(Resource.class) {
		if(_local == null) {
		    Pool local = new Pool(new JarSource("res"));
		    try {
			if(resdir.get() != null)
			    local.add(new FileSource(resdir.get()));
		    } catch(Exception e) {
			/* Ignore these. We don't want to be crashing the client
			 * for users just because of errors in development
			 * aids. */
		    }
		    _local = local;
		}
	    }
	}
	return(_local);
    }

    private static Pool _remote = null;
    public static Pool remote() {
	if(_remote == null) {
	    synchronized(Resource.class) {
		if(_remote == null) {
		    Pool remote = new Pool(local(), new JarSource("res-preload"));
		    if(prscache != null)
			remote.add(new CacheSource(prscache));
		    _remote = remote;;
		}
	    }
	}
	return(_remote);
    }

    public static void addurl(URI uri) {
	ResSource src = new HttpSource(uri);
	if(prscache != null) {
	    class Caching extends TeeSource {
		private final transient ResCache cache;
		Caching(ResSource bk, ResCache cache) {super(bk); this.cache = cache;}

		public OutputStream fork(String name) throws IOException {
		    return(cache.store("res/" + name));
		}
	    }
	    src = new Caching(src, prscache);
	}
	remote().add(src);
    }

    public static class LoadException extends RuntimeException {
	public Resource res;
	public ResSource src;
	public LoadException prev;
	    
	public LoadException(String msg, Resource res) {
	    super(msg);
	    this.res = res;
	}

	public LoadException(String msg, Throwable cause, Resource res) {
	    super(msg, cause);
	    this.res = res;
	}
	    
	public LoadException(Throwable cause, Resource res) {
	    super("Load error in resource " + res.toString() + ", from " + res.source, cause);
	    this.res = res;
	}
    }

    public static class LoadWarning extends Warning {
	public final Resource res;

	public LoadWarning(Resource res, String msg) {
	    super(msg);
	    this.res = res;
	}

	public LoadWarning(Resource res, String msg, Object... args) {
	    this(res, String.format(msg, args));
	}
    }
    
    public static Coord cdec(Message buf) {
	return(new Coord(buf.int16(), buf.int16()));
    }
	
    public static class PoolMapper implements Function<Object, Object> {
	public final Pool pool;

	public PoolMapper(Pool pool) {this.pool = pool;}

	public Object apply(Object obj) {
	    if(obj instanceof Spec)
		return(new Spec(pool, ((Spec)obj).name, ((Spec)obj).ver));
	    return(obj);
	}
    }

    public abstract class Layer implements Serializable {
	public abstract void init();
	
	public Resource getres() {
	    return(Resource.this);
	}

	public String toString() {
	    if(this instanceof IDLayer)
		return(String.format("#<%s (%s) in %s>", getClass().getSimpleName(), ((IDLayer)this).layerid(), Resource.this.name));
	    else
		return(String.format("#<%s in %s>", getClass().getSimpleName(), Resource.this.name));
	}

	protected Function<Object, Object> resmapper() {
	    return(new PoolMapper(Resource.this.pool));
	}
    }

    public interface LayerFactory<T extends Layer> {
	public T cons(Resource res, Message buf);
    }

    public static class LayerConstructor<T extends Layer> implements LayerFactory<T> {
	public final Class<T> cl;
	private final Constructor<T> cons;
	
	public LayerConstructor(Class<T> cl) {
	    this.cl = cl;
	    try {
		this.cons = cl.getConstructor(Resource.class, Message.class);
	    } catch(NoSuchMethodException e) {
		throw(new RuntimeException("No proper constructor found for layer type " + cl.getName(), e));
	    }
	}
	
	public T cons(Resource res, Message buf) {
	    try {
		return(cons.newInstance(res, buf));
	    } catch(InstantiationException e) {
		throw(new LoadException(e, res));
	    } catch(IllegalAccessException e) {
		throw(new LoadException(e, res));
	    } catch(InvocationTargetException e) {
		Throwable c = e.getCause();
		if(c instanceof RuntimeException)
		    throw((RuntimeException)c);
		else
		    throw(new LoadException(e, res));
	    }
	}
    }

    public static void addltype(String name, LayerFactory<?> cons) {
	if(ltypes.put(name, cons) != null)
	   Warning.warn("duplicated layer name: " + name);
    }
    
    public static <T extends Layer> void addltype(String name, Class<T> cl) {
	addltype(name, new LayerConstructor<T>(cl));
    }
    
    @dolda.jglob.Discoverable
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface LayerName {
	public String value();
    }

    static {
	for(Class<?> cl : dolda.jglob.Loader.get(LayerName.class).classes()) {
	    String nm = cl.getAnnotation(LayerName.class).value();
	    if(LayerFactory.class.isAssignableFrom(cl)) {
		addltype(nm, Utils.construct(cl.asSubclass(LayerFactory.class)));
	    } else if(Layer.class.isAssignableFrom(cl)) {
		addltype(nm, cl.asSubclass(Layer.class));
	    } else {
		throw(new Error("Illegal resource layer class: " + cl));
	    }
	}
    }

    public interface IDLayer<T> {
	public T layerid();
    }

    public static class ImageReadException extends IOException {
	public final String[] supported = ImageIO.getReaderMIMETypes();

	public ImageReadException() {
	    super("Could not decode image data");
	}
    }

    public static BufferedImage readimage(InputStream fp) throws IOException {
	BufferedImage ret = ImageIO.read(fp);
	if(ret == null)
	    throw(new ImageReadException());
	return(ret);
    }

    @LayerName("image")
    public class Image extends Layer implements IDLayer<Integer> {
	public transient BufferedImage img;
	private transient BufferedImage scaled;
	private transient Tex tex, rawtex;
	public final int z, subz;
	public final boolean nooff;
	public final int id;
	public final Map<String, byte[]> kvdata;
	public float scale = 1;
	public Coord sz, o, so, tsz, ssz, stsz;

	public Image(Message buf) {
	    z = buf.int16();
	    subz = buf.int16();
	    int fl = buf.uint8();
	    /* Obsolete flag 1: Layered */
	    nooff = (fl & 2) != 0;
	    id = buf.int16();
	    o = cdec(buf);
	    so = UI.scale(o);
	    boolean hasscale = false;
	    Map<String, byte[]> kvdata = new HashMap<>();
	    if((fl & 4) != 0) {
		while(true) {
		    String key = buf.string();
		    if(key.equals(""))
			break;
		    int len = buf.uint8();
		    if((len & 0x80) != 0)
			len = buf.int32();
		    byte[] data = buf.bytes(len);
		    Message val = new MessageBuf(data);
		    if(key.equals("tsz")) {
			tsz = val.coord();
		    } else if(key.equals("scale")) {
			scale = val.float32();
			hasscale = true;
		    } else {
			kvdata.put(key, data);
		    }
		}
	    }
	    this.kvdata = kvdata.isEmpty() ? Collections.emptyMap() : kvdata;
	    try {
		img = readimage(new MessageInputStream(buf));
	    } catch(IOException e) {
		throw(new LoadException(e, Resource.this));
	    }
	    sz = Utils.imgsz(img);
	    if(tsz == null)
		tsz = sz.add(o);
	    ssz = Coord.of(Math.round(UI.scale(sz.x / scale)), Math.round(UI.scale(sz.y / scale)));
	    stsz = Coord.of(Math.round(UI.scale(tsz.x / scale)), Math.round(UI.scale(tsz.y / scale)));
	    if(tsz != null) {
		/* This seems all kinds of ugly, but I'm not sure how
		 * to otherwise handle upwards rounding of both offset
		 * and size getting the image out of the intended
		 * area. */
		so = new Coord(Math.min(so.x, stsz.x - ssz.x), Math.min(so.y, stsz.y - ssz.y));
	    }
	    scaled = PUtils.uiscale(img, ssz);
	    if(false && !hasscale)
		scaled = PUtils.monochromize(PUtils.coercergba(scaled), java.awt.Color.RED);
	}

	public BufferedImage scaled() {
	    return(scaled);
	}

	public Tex rawtex() {
	    if(rawtex == null) {
		synchronized(this) {
		    if(rawtex == null) {
			rawtex = new TexI(img) {
				public String toString() {
				    return("TexI(" + Resource.this.name + ", " + id + ")");
				}
			    };
		    }
		}
	    }
	    return(rawtex);
	}

	public Tex tex() {
	    if(tex == null) {
		synchronized(this) {
		    if(tex == null) {
			tex = new TexI(scaled()) {
				public String toString() {
				    return("TexI(" + Resource.this.name + ", " + id + ")");
				}
			    };
		    }
		}
	    }
	    return(tex);
	}

	public Integer layerid() {
	    return(id);
	}
		
	public void init() {}
    }

    @LayerName("tooltip")
    public class Tooltip extends Layer {
	public final String t;
                
	public Tooltip(Message buf) {
	    t = new String(buf.bytes(), Utils.utf8);
	}
                
	public void init() {}
    }

    @LayerName("neg")
    public class Neg extends Layer {
	public Coord cc;
	public Coord[][] ep;
		
	public Neg(Message buf) {
	    cc = cdec(buf);
	    buf.skip(12);
	    ep = new Coord[8][0];
	    int en = buf.uint8();
	    for(int i = 0; i < en; i++) {
		int epid = buf.uint8();
		int cn = buf.uint16();
		ep[epid] = new Coord[cn];
		for(int o = 0; o < cn; o++)
		    ep[epid][o] = cdec(buf);
	    }
	}
		
	public void init() {}
    }

    @LayerName("props")
    public class Props extends Layer {
	public final Map<String, Object> props = new HashMap<>();

	public Props(Message buf) {
	    int ver = buf.uint8();
	    if(ver != 1)
		throw(new LoadException("Unknown property layer version: " + ver, getres()));
	    Object[] raw = buf.list();
	    for(int a = 0; a < raw.length - 1; a += 2)
		props.put((String)raw[a], raw[a + 1]);
	}

	public Object get(String nm) {
	    return(props.get(nm));
	}

	public void init() {}
    }

    @LayerName("obst")
    public class Obstacle extends Layer implements IDLayer<String> {
	public final String id;
	public final Coord2d[][] p;

	public Obstacle(Message buf) {
	    int ver = buf.uint8();
	    if((ver >= 1) && (ver <= 2)) {
		this.id = (ver >= 2) ? buf.string() : "";
		p = new Coord2d[buf.uint8()][];
		for(int i = 0; i < p.length; i++)
		    p[i] = new Coord2d[buf.uint8()];
		for(int i = 0; i < p.length; i++) {
		    for(int o = 0; o < p[i].length; o++)
			p[i][o] = Coord2d.of(buf.float16(), buf.float16()).mul(MCache.tilesz);
		}
	    } else {
		this.id = "#";
		this.p = new Coord2d[0][];
	    }
	}

	public void init() {}
	public String layerid() {return(id);}
    }

    @LayerName("anim")
    public class Anim extends Layer {
	private int[] ids;
	public int id, d;
	public Image[][] f;
		
	public Anim(Message buf) {
	    id = buf.int16();
	    d = buf.uint16();
	    ids = new int[buf.uint16()];
	    for(int i = 0; i < ids.length; i++)
		ids[i] = buf.int16();
	}
		
	public void init() {
	    f = new Image[ids.length][];
	    Image[] typeinfo = new Image[0];
	    for(int i = 0; i < ids.length; i++) {
		LinkedList<Image> buf = new LinkedList<Image>();
		for(Image img : layers(Image.class)) {
		    if(img.id == ids[i])
			buf.add(img);
		}
		f[i] = buf.toArray(typeinfo);
	    }
	}
    }

    @LayerName("pagina")
    public class Pagina extends Layer {
	public final String text;
		
	public Pagina(Message buf) {
	    text = new String(buf.bytes(), Utils.utf8);
	}
		
	public void init() {}
    }

    @LayerName("action")
    public class AButton extends Layer {
	public final String name;
	public final Named parent;
	public final char hk;
	public final String[] ad;
		
	public AButton(Message buf) {
	    String pr = buf.string();
	    int pver = buf.uint16();
	    if(pr.length() == 0) {
		parent = null;
	    } else {
		try {
		    parent = pool.load(pr, pver);
		} catch(RuntimeException e) {
		    throw(new LoadException("Illegal resource dependency", e, Resource.this));
		}
	    }
	    name = buf.string();
	    buf.string(); /* Prerequisite skill */
	    hk = (char)buf.uint16();
	    ad = new String[buf.uint16()];
	    for(int i = 0; i < ad.length; i++)
		ad[i] = buf.string();
	}
		
	public void init() {}
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface PublishedCode {
	String name();
	Class<? extends Instancer> instancer() default Instancer.class;
	public interface Instancer<I> {
	    public I make(Class<?> cl, Resource res, Object... args);

	    public static <T, U extends T> T stdmake(Class<T> type, Class<U> cl, Resource ires, Object[] args) {
		try {
		    Function<Object[], T> make = Utils.smthfun(cl, "instantiate", type, Resource.class, Object[].class);
		    return(make.apply(new Object[] {ires, args}));
		} catch(NoSuchMethodException e) {}
		try {
		    Function<Object[], T> make = Utils.smthfun(cl, "instantiate", type, Object[].class);
		    return(make.apply(new Object[] {args}));
		} catch(NoSuchMethodException e) {}
		try {
		    Function<Object[], T> make = Utils.smthfun(cl, "instantiate", type, Resource.class);
		    return(make.apply(new Object[] {ires}));
		} catch(NoSuchMethodException e) {}
		try {
		    Constructor<U> cons = cl.getConstructor(Resource.class, Object[].class);
		    return(Utils.construct(cons, new Object[] {ires, args}));
		} catch(NoSuchMethodException e) {}
		try {
		    Constructor<U> cons = cl.getConstructor(Object[].class);
		    return(Utils.construct(cons, new Object[] {args}));
		} catch(NoSuchMethodException e) {}
		try {
		    Constructor<U> cons = cl.getConstructor(Resource.class);
		    return(Utils.construct(cons, new Object[] {ires}));
		} catch(NoSuchMethodException e) {}
		return(Utils.construct(cl));
	    }

	    public static class Direct<I> implements Instancer<I> {
		public final Class<I> type;

		public Direct(Class<I> type) {
		    this.type = type;
		}

		public I make(Class<?> cl, Resource res, Object... args) {
		    if(!type.isAssignableFrom(cl))
			return(null);
		    return(stdmake(type, cl.asSubclass(type), res, args));
		}
	    }

	    public static class StaticCall<I, R> implements Instancer<I> {
		public final Class<I> type;
		public final String name;
		public final Class<R> rtype;
		public final Class<?>[] args;
		public final Function<Function<Object[], R>, I> maker;

		public StaticCall(Class<I> type, String name, Class<R> rtype, Class<?>[] args, Function<Function<Object[], R>, I> maker) {
		    this.type = type;
		    this.name = name;
		    this.rtype = rtype;
		    this.args = args;
		    this.maker = maker;
		}

		public I make(Class <?> cl, Resource res, Object... args) {
		    Function<Object[], R> make;
		    try {
			make = Utils.smthfun(cl, name, rtype, this.args);
		    } catch(NoSuchMethodException e) {
			return(null);
		    }
		    return(maker.apply(make));
		}
	    }

	    public static class Construct<I, R> implements Instancer<I> {
		public final Class<I> type;
		public final Class<R> rtype;
		public final Class<?>[] args;
		public final Function<Function<Object[], ? extends R>, I> maker;

		public Construct(Class<I> type, Class<R> rtype, Class<?>[] args, Function<Function<Object[], ? extends R>, I> maker) {
		    this.type = type;
		    this.rtype = rtype;
		    this.args = args;
		    this.maker = maker;
		}

		public I make(Class <?> cl, Resource res, Object... args) {
		    if(!rtype.isAssignableFrom(cl))
			return(null);
		    Class<? extends R> scl = cl.asSubclass(rtype);
		    Function<Object[], ? extends R> cons;
		    try {
			cons = Utils.consfun(scl, this.args);
		    } catch(NoSuchMethodException e) {
			return(null);
		    }
		    return(maker.apply(cons));
		}
	    }

	    public static class Chain<I> implements Instancer<I> {
		public final Class<I> type;
		private final Collection<Instancer<? extends I>> sub = new ArrayList<>();

		public Chain(Class<I> type) {
		    this.type = type;
		}

		public void add(Instancer<? extends I> el) {
		    sub.add(el);
		}

		public I make(Class<?> cl, Resource res, Object... args) {
		    for(Instancer<? extends I> el : sub) {
			I inst = type.cast(el.make(cl, res, args));
			if(inst != null)
			    return(inst);
		    }
		    throw(new RuntimeException(String.format("Could not find any suitable constructor for %s in %s", type, cl)));
		}
	    }

	    public static final Instancer<Object> simple = (cl, res, args) -> {
		return(stdmake(Object.class, cl, res, args));
	    };
	}
	public static final Map<PublishedCode, Instancer> instancers = new WeakHashMap<>();
    }

    @LayerName("code")
    public class Code extends Layer {
	public final String name;
	transient public final byte[] data;
		
	public Code(Message buf) {
	    name = buf.string();
	    data = buf.bytes();
	}
		
	public void init() {}
    }

    public static class ResClassLoader extends ClassLoader {
	/* Please make sure you have read and understood
	 * doc/resource-code if you feel tempted to change
	 * OVERRIDE_ALL to true. */
	public static final boolean OVERRIDE_ALL = false;
	public final CodeEntry entry;

	public ResClassLoader(ClassLoader parent, CodeEntry entry) {
	    super(parent);
	    this.entry = entry;
	}

	public Code findcode(String name) {
	    return(entry.clmap.get(name));
	}

	public Class<?> findClass(String name) throws ClassNotFoundException {
	    Code c = findcode(name);
	    if(c == null)
		throw(new ResourceClassNotFoundException(name, entry.getres()));
	    return(defineClass(name, c.data, 0, c.data.length));
	}

	public static FromResource getsource(Class<?> cl) {
	    for(; cl != null; cl = cl.getEnclosingClass()) {
		FromResource src = cl.getAnnotation(FromResource.class);
		if(src != null)
		    return(src);
	    }
	    return(null);
	}

	public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
	    synchronized(getClassLoadingLock(name)) {
		Class<?> ret = findLoadedClass(name);
		if(ret == null) {
		    try {
			ret = getParent().loadClass(name);
			if(findcode(name) != null) {
			    boolean override = false;
			    FromResource src = getsource(ret);
			    if((src != null) && ((src.name().equals(getres().name) && (src.version() == getres().ver)) || src.override()))
				override = true;
			    if(!override) {
				String fmt;
				if(OVERRIDE_ALL) {
				    fmt = "local copy of %s (%s) would be overridden by code from %s";
				} else {
				    fmt = "local copy of %s (%s) is overridden by code from %s; please refer to doc/resource-code";
				    ret = null;
				}
				Warning.warn(fmt, name,
					     (src == null) ? "unannotated" : String.format("fetched from %s v%d", src.name(), src.version()),
					     String.format("%s v%d", getres().name, getres().ver));
			    }
			}
		    } catch(ClassNotFoundException e) {
		    }
		}
		if(ret == null)
		    ret = findClass(name);
		if(resolve)
		    resolveClass(ret);
		return(ret);
	    }
	}

	public Resource getres() {
	    return(entry.getres());
	}
	
	public String toString() {
	    return("cl:" + entry.getres());
	}
    };

    public static class LibClassLoader extends ClassLoader {
	private final ClassLoader[] classpath;
	
	public LibClassLoader(ClassLoader parent, Collection<ClassLoader> classpath) {
	    super(parent);
	    this.classpath = classpath.toArray(new ClassLoader[0]);
	}
	
	public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
	    /* XXX? This scheme seems a bit strange and weird, but I'm
	     * not sure what the better alternative would be to still
	     * allow selectively overriding resource classes. */
	    Class<?> ret = null;
	    try {
		ret = getParent().loadClass(name);
	    } catch(ClassNotFoundException e) {}
	    for(ClassLoader lib : classpath) {
		try {
		    Class<?> found = lib.loadClass(name);
		    if(found.getClassLoader() instanceof ResClassLoader)
			ret = found;
		    break;
		} catch(ClassNotFoundException e) {}
	    }
	    if(ret == null)
		throw(new ClassNotFoundException("Could not find " + name + " in any of " + Arrays.asList(classpath).toString()));
	    return(ret);
	}
    }

    public static class ResourceClassNotFoundException extends ClassNotFoundException {
	public final String clname;
	public final Resource res;

	public ResourceClassNotFoundException(String clname, Resource res) {
	    super(String.format("Could not find class %s in resource %s", clname, res));
	    this.clname = clname;
	    this.res = res;
	}
    }

    @LayerName("codeentry")
    public class CodeEntry extends Layer {
	private final Map<String, Code> clmap = new HashMap<>();
	private final Map<String, String> pe = new HashMap<>();
	private final Map<String, Object[]> pa = new HashMap<>();
	private final Collection<Indir<Resource>> classpath = new ArrayList<>();
	transient private ClassLoader loader;
	transient private final Map<String, Class<?>> lpe = new HashMap<>();
	transient private final Map<String, Object> ipe = new HashMap<>();

	public CodeEntry(Message buf) {
	    while(!buf.eom()) {
		int t = buf.uint8();
		if((t == 1) || (t == 3)) {
		    while(true) {
			String en = buf.string();
			String cn = buf.string();
			if(en.length() == 0)
			    break;
			pe.put(en, cn);
			if(t == 3)
			    pa.put(en, buf.list());
		    }
		} else if(t == 2) {
		    while(true) {
			String ln = buf.string();
			if(ln.length() == 0)
			    break;
			int ver = buf.uint16();
			classpath.add(pool.load(ln, ver));
		    }
		} else {
		    throw(new LoadException("Unknown codeentry data type: " + t, Resource.this));
		}
	    }
	}

	public void init() {
	    for(Code c : layers(Code.class))
		clmap.put(c.name, c);
	}

	public ClassLoader loader() {
	    synchronized(CodeEntry.this) {
		if(this.loader == null) {
		    ClassLoader loader = Resource.class.getClassLoader();
		    if(classpath.size() > 0) {
			Collection<ClassLoader> loaders = new LinkedList<ClassLoader>();
			for(Indir<Resource> res : classpath) {
			    loaders.add(res.get().flayer(CodeEntry.class).loader());
			}
			loader = new LibClassLoader(loader, loaders);
		    }
		    if(clmap.size() > 0)
			loader = new ResClassLoader(loader, CodeEntry.this);
		    this.loader = loader;
		}
	    }
	    return(this.loader);
	}

	private Class<?> getentry(Class<?> cl, boolean fail) {
	    PublishedCode entry = cl.getAnnotation(PublishedCode.class);
	    if(entry == null)
		throw(new RuntimeException("Tried to fetch non-published res-loaded class " + cl.getName() + " from " + Resource.this.name));
	    synchronized(CodeEntry.this) {
		Class<?> ret = lpe.get(entry.name());
		if(ret == null) {
		    String clnm = pe.get(entry.name());
		    if(clnm == null) {
			if(fail)
			    throw(new RuntimeException("Tried to fetch non-present res-loaded class " + cl.getName() + " from " + Resource.this.name));
			return(null);
		    }
		    try {
			ret = loader().loadClass(clnm);
		    } catch(ClassNotFoundException e) {
			throw(new LoadException(e, Resource.this));
		    }
		    lpe.put(entry.name(), ret);
		}
		return(ret);
	    }
	}

	public <T> Class<? extends T> getcl(Class<T> cl, boolean fail) {
	    Class<?> acl = getentry(cl, fail);
	    if(acl == null)
		return(null);
	    try {
		return(acl.asSubclass(cl));
	    } catch(ClassCastException e) {
		throw(new RuntimeException(String.format("Illegal entry-point class specified for %s in %s", cl.getName(), Resource.this.name), e));
	    }
	}

	public <T> Class<? extends T> getcl(Class<T> cl) {
	    return(getcl(cl, true));
	}

	public <T> T get(Class<T> cl, boolean fail) {
	    PublishedCode entry = cl.getAnnotation(PublishedCode.class);
	    if(entry == null)
		throw(new RuntimeException("Tried to fetch non-published res-loaded class " + cl.getName() + " from " + Resource.this.name));
	    synchronized(CodeEntry.this) {
		Object inst;
		if((inst = ipe.get(entry.name())) == null) {
		    Class<?> acl = getentry(cl, fail);
		    if(acl == null)
			return(null);
		    Object[] args = pa.getOrDefault(entry.name(), new Object[0]);
		    PublishedCode.Instancer<?> mk;
		    synchronized(PublishedCode.instancers) {
			mk = PublishedCode.instancers.computeIfAbsent(entry, k -> {
				if(k.instancer() == PublishedCode.Instancer.class)
				    return(PublishedCode.Instancer.simple);
				else
				    return(Utils.construct(k.instancer()));
			    });
		    }
		    inst = mk.make(acl, Resource.this, args);
		    ipe.put(entry.name(), inst);
		}
		try {
		    return(cl.cast(inst));
		} catch(ClassCastException e) {
		    throw(new RuntimeException(String.format("Illegal entry-point class specified for %s in %s", entry.name(), Resource.this.name), e));
		}
	    }
	}

	public <T> T get(Class<T> cl) {
	    return(get(cl, true));
	}
    }

    public static Resource classres(final Class<?> cl) {
	ClassLoader l = cl.getClassLoader();
	if(l instanceof ResClassLoader)
	    return(((ResClassLoader)l).getres());
	FromResource src = ResClassLoader.getsource(cl);
	if(src != null) {
	    /* XXX? This feels like a hack, but I can't think of
	     * any better way to let resource code that has been
	     * downloaded with `get-code' reference data in its
	     * originating resource. */
	    return(remote().loadwait(src.name(), src.version()));
	}
	throw(new RuntimeException("Cannot fetch resource of non-resloaded class " + cl));
    }

    public <T> T getcode(Class<T> cl, boolean fail) {
	CodeEntry e = layer(CodeEntry.class);
	if(e == null) {
	    if(fail)
		throw(new RuntimeException("Tried to fetch non-present res-loaded class " + cl.getName() + " from " + Resource.this.name));
	    return(null);
	}
	return(e.get(cl, fail));
    }

    @LayerName("audio2")
    public class Audio extends Layer implements haven.Audio.Clip {
	transient public byte[] coded;
	public final String id;
	public double bvol = 1.0;

	public Audio(Message buf) {
	    int ver = buf.uint8();
	    if((ver >= 1) && (ver <= 2)) {
		this.id = buf.string();
		if(ver >= 2)
		    bvol = buf.uint16() * 0.001;
		this.coded = buf.bytes();
	    } else {
		throw(new LoadException("Unknown audio layer version: " + ver, getres()));
	    }
	}

	public void init() {}

	public haven.Audio.CS stream() {
	    try {
		return(new haven.Audio.VorbisClip(new ByteArrayInputStream(coded)));
	    } catch(IOException e) {
		throw(new RuntimeException(e));
	    }
	}

	public String layerid() {return(id);}
	public double bvol() {return(bvol);}
    }

    @LayerName("midi")
    public class Music extends Resource.Layer {
	transient javax.sound.midi.Sequence seq;

	public Music(Message buf) {
	    try {
		seq = javax.sound.midi.MidiSystem.getSequence(new MessageInputStream(buf));
	    } catch(javax.sound.midi.InvalidMidiDataException e) {
		throw(new LoadException("Invalid MIDI data", Resource.this));
	    } catch(IOException e) {
		throw(new LoadException(e, Resource.this));
	    }
	}

	public void init() {}
    }

    @LayerName("font")
    public class Font extends Layer {
	public transient final java.awt.Font font;

	public Font(Message buf) {
	    int ver = buf.uint8();
	    if(ver == 1) {
		int type = buf.uint8();
		if(type == 0) {
		    try {
			this.font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, new MessageInputStream(buf));
		    } catch(Exception e) {
			throw(new RuntimeException(e));
		    }
		} else {
		    throw(new LoadException("Unknown font type: " + type, Resource.this));
		}
	    } else {
		throw(new LoadException("Unknown font layer version: " + ver, Resource.this));
	    }
	}

	public void init() {}
    }

    private void readall(InputStream in, byte[] buf) throws IOException {
	int ret, off = 0;
	while(off < buf.length) {
	    ret = in.read(buf, off, buf.length - off);
	    if(ret < 0)
		throw(new LoadException("Incomplete resource at " + name, this));
	    off += ret;
	}
    }

    public <L extends Layer> Collection<L> layers(final Class<L> cl) {
	used = true;
	return(new DefaultCollection<L>() {
		public Iterator<L> iterator() {
		    return(Utils.filter(layers.iterator(), cl));
		}
	    });
    }

    public static class NoSuchLayerException extends NoSuchElementException {
	public NoSuchLayerException(String message) {
	    super(message);
	}
    }

    public <L extends Layer> L layer(Class<L> cl) {
	used = true;
	for(Layer l : layers) {
	    if(cl.isInstance(l))
		return(cl.cast(l));
	}
	return(null);
    }
    public <L extends Layer> L flayer(Class<L> cl) {
	L l = layer(cl);
	if(l == null) throw(new NoSuchLayerException("no " + cl + " in " + name));
	return(l);
    }

    public <L> Collection<L> layers(Class<L> cl, Predicate<? super L> sel) {
	used = true;
	if(sel == null)
	    sel = l -> true;
	Predicate<? super L> dsel = sel;
	return(new DefaultCollection<L>() {
		public Iterator<L> iterator() {
		    return(Utils.filter(Utils.filter(layers.iterator(), cl), dsel));
		}
	    });
    }

    public <L> L layer(Class<L> cl, Predicate<? super L> sel) {
	used = true;
	for(Layer l : layers) {
	    if(cl.isInstance(l)) {
		L lc = cl.cast(l);
		if((sel == null) || sel.test(lc))
		    return(lc);
	    }
	}
	return(null);
    }

    public <L> L flayer(Class<L> cl, Predicate<? super L> sel) {
	L l = layer(cl, sel);
	if(l == null) throw(new NoSuchLayerException("no " + cl + " in " + name + " selected by " + sel));
	return(l);
    }

    public <I, L extends IDLayer<I>> L layer(Class<L> cl, I id) {
	used = true;
	for(Layer l : layers) {
	    if(cl.isInstance(l)) {
		L ll = cl.cast(l);
		if(ll.layerid().equals(id))
		    return(ll);
	    }
	}
	return(null);
    }
    public <I, L extends IDLayer<I>> L flayer(Class<L> cl, I id) {
	L l = layer(cl, id);
	if(l == null) throw(new NoSuchLayerException("no " + cl + " in " + name + " with id " + id));
	return(l);
    }

    public boolean equals(Object other) {
	if(!(other instanceof Resource))
	    return(false);
	Resource o = (Resource)other;
	return(o.name.equals(this.name) && (o.ver == this.ver));
    }

    private static final byte[] RESOURCE_SIG = "Haven Resource 1".getBytes(Utils.ascii);
    private void load(Message in) {
	if(!Arrays.equals(RESOURCE_SIG, in.bytes(RESOURCE_SIG.length)))
	    throw(new LoadException("Invalid res signature", this));
	int ver = in.uint16();
	List<Layer> layers = new LinkedList<Layer>();
	if(this.ver == -1)
	    this.ver = ver;
	else if(ver != this.ver)
	    throw(new LoadException("Wrong res version (" + ver + " != " + this.ver + ")", this));
	while(!in.eom()) {
	    LayerFactory<?> lc = ltypes.get(in.string());
	    int len = in.int32();
	    if(lc == null) {
		in.skip(len);
		continue;
	    }
	    Message buf = new LimitMessage(in, len);
	    Layer l = lc.cons(this, buf);
	    if(l != null)
		layers.add(l);
	    buf.skip();
	}
	this.layers = layers;
	for(Layer l : layers)
	    l.init();
	used = false;
    }

    private transient Named indir = null;
    public Named indir() {
	if(indir != null)
	    return(indir);
	class Ret extends Named implements Serializable {
	    Ret(String name, int ver) {super(name, ver);}

	    public Resource get() {
		return(Resource.this);
	    }

	    public String toString() {
		return(String.format("<indir:%s(v%d)>", name, ver));
	    }
	}
	indir = new Ret(name, ver);
	return(indir);
    }

    public static Image loadrimg(String name) {
	return(local().loadwait(name).layer(imgc));
    }

    public static BufferedImage loadimg(String name) {
	return(loadrimg(name).img);
    }

    public static BufferedImage loadsimg(String name) {
	return(loadrimg(name).scaled());
    }

    public static Tex loadtex(String name) {
	return(loadrimg(name).tex());
    }

    public String toString() {
	return(name + "(v" + ver + ")");
    }

    public static void loadlist(Pool pool, InputStream list, int prio) throws IOException {
	BufferedReader in = new BufferedReader(new InputStreamReader(list, "us-ascii"));
	String ln;
	while((ln = in.readLine()) != null) {
	    int pos = ln.indexOf(':');
	    if(pos < 0)
		continue;
	    String nm = ln.substring(0, pos);
	    int ver;
	    try {
		ver = Integer.parseInt(ln.substring(pos + 1));
	    } catch(NumberFormatException e) {
		continue;
	    }
	    try {
		pool.load(nm, ver, prio);
	    } catch(RuntimeException e) {
	    }
	}
	in.close();
    }

    public static void dumplist(Collection<Resource> list, Writer dest) {
	PrintWriter out = new PrintWriter(dest);
	List<Resource> sorted = new ArrayList<Resource>(list);
	Collections.sort(sorted, new Comparator<Resource>() {
		public int compare(Resource a, Resource b) {
		    return(a.name.compareTo(b.name));
		}
	    });
	for(Resource res : sorted)
	    out.println(res.name + ":" + res.ver);
    }

    public static void updateloadlist(Path file, Path resdir) throws Exception {
	Map<String, Integer> orig;
	try(BufferedReader r = Files.newBufferedReader(file)) {
	    orig = new HashMap<>();
	    String ln;
	    while((ln = r.readLine()) != null) {
		int pos = ln.indexOf(':');
		if(pos < 0) {
		    System.err.println("Weird line: " + ln);
		    continue;
		}
		String nm = ln.substring(0, pos);
		int ver = Integer.parseInt(ln.substring(pos + 1));
		orig.put(nm, ver);
	    }
	}
	Pool pool = new Pool(new FileSource(resdir));
	for(String nm : orig.keySet())
	    pool.load(nm);
	while(true) {
	    int d = pool.qdepth();
	    if(d == 0)
		break;
	    System.out.print("\033[1GLoading... " + d + "\033[K");
	    Thread.sleep(500);
	}
	System.out.println();
	Collection<Resource> cur = new LinkedList<Resource>();
	for(Map.Entry<String, Integer> e : orig.entrySet()) {
	    String nm = e.getKey();
	    int ver = e.getValue();
	    Resource res = Loading.waitfor(pool.load(nm));
	    if(res.ver != ver)
		System.out.println(nm + ": " + ver + " -> " + res.ver);
	    cur.add(res);
	}
	try(Writer w = Files.newBufferedWriter(file)) {
	    dumplist(cur, w);
	}
    }

    private static void usage_getcode(PrintStream out) {
	out.println("usage: haven.Resource get-code [-h] [-U RESOURCE-URL] [-o DEST-DIR] RESOURCE-NAME...");
    }

    public static void cmd_getcode(String[] args) {
	URI url = null;
	PosixArgs opt = PosixArgs.getopt(args, "hqo:U:");
	if(opt == null) {
	    usage_getcode(System.err);
	    System.exit(1);
	}
	boolean quiet = false;
	Path dst = Utils.path("src");
	for(char c : opt.parsed()) {
	    switch(c) {
	    case 'h':
		usage_getcode(System.out);
		System.exit(0);
		break;
	    case 'q':
		quiet = true;
		break;
	    case 'o':
		dst = Utils.path(opt.arg);
		break;
	    case 'U':
		try {
		    url = Utils.uri(opt.arg);
		} catch(IllegalArgumentException e) {
		    System.err.println("get-code: malformed url: " + opt.arg);
		    System.exit(1);
		}
		break;
	    }
	}
	if(opt.rest.length < 1) {
	    usage_getcode(System.err);
	    System.exit(1);
	}
	if(!Files.isDirectory(dst)) {
	    System.err.println("get-code: destination directory does not exist: " + dst);
	    System.exit(1);
	}
	if(url == null) {
	    if((url = resurl.get()) == null) {
		System.err.println("get-code: no resource URL configured");
		System.exit(1);
	    }
	}
	ResSource source = new HttpSource(url);
	for(String resnm : opt.rest) {
	    Map<String, byte[]> code = new HashMap<>();
	    int resver;
	    try(InputStream inp = source.get(resnm)) {
		Message fp = new StreamMessage(inp);
		if(!Arrays.equals(RESOURCE_SIG, fp.bytes(RESOURCE_SIG.length))) {
		    System.err.println("get-code: invalid resource: " + resnm);
		    System.exit(1);
		}
		resver = fp.uint16();
		while(!fp.eom()) {
		    String laynm = fp.string();
		    Message lay = new LimitMessage(fp, fp.int32());
		    if(laynm.equals("src")) {
			int fver = lay.uint8();
			if(fver == 1) {
			    String nm = lay.string();
			    code.put(nm, lay.bytes());
			} else {
			    System.err.println("get-code: warning: unknown source code layer version: " + fver);
			}
		    }
		    lay.skip();
		}
	    } catch(FileNotFoundException e) {
		System.err.println("get-code: no such resource: " + resnm);
		System.exit(1);
		return;
	    } catch(IOException e) {
		System.err.println("get-code: error when fetching " + resnm + ": " + e);
		System.exit(1);
		return;
	    }
	    if(code.isEmpty()) {
		System.err.println("get-code: no source code found in resource: " + resnm);
		System.exit(1);
	    }
	    Pattern classpat = Pattern.compile("^(public\\s+)?(abstract\\s+)?(class|interface)\\s+(\\S+)");
	    Pattern pkgpat   = Pattern.compile("^package\\s+(\\S+)\\s*;");
	    for(String nm : code.keySet()) {
		Path dir = dst;
		List<String> lines = new LinkedList<>(Arrays.asList(new String(code.get(nm), Utils.utf8).split("\n")));
		for(ListIterator<String> i = lines.listIterator(); i.hasNext();) {
		    String ln = i.next();
		    Matcher m = classpat.matcher(ln);
		    if(m.find()) {
			i.previous();
			i.add("@haven.FromResource(name = \"" + resnm + "\", version = " + resver + ")");
			break;
		    }
		    m = pkgpat.matcher(ln);
		    if(m.find())
			dir = Utils.pj(dir, m.group(1).split("\\."));
		}
		Path out = Utils.pj(dir, nm);
		try {
		    if(!Files.isDirectory(out.getParent()))
			Files.createDirectories(out.getParent());
		    Files.write(out, lines, Utils.utf8);
		    if(!quiet)
			System.err.println("wrote " + out);
		} catch(IOException e) {
		    System.err.println("get-code: could not write " + nm + ": " + e);
		    System.exit(1);
		}
	    }
	}
    }

    private static void usage_findupdates(PrintStream out) {
	out.println("usage: haven.Resource find-updates [-h] [-U RESOURCE-URL] [SOURCE-DIR]");
    }

    public static void cmd_findupdates(String[] args) {
	URI url = null;
	PosixArgs opt = PosixArgs.getopt(args, "hU:");
	if(opt == null) {
	    usage_findupdates(System.err);
	    System.exit(1);
	}
	for(char c : opt.parsed()) {
	    switch(c) {
	    case 'h':
		usage_findupdates(System.out);
		System.exit(0);
		break;
	    case 'U':
		try {
		    url = Utils.uri(opt.arg);
		} catch(IllegalArgumentException e) {
		    System.err.println("get-code: malformed url: " + opt.arg);
		    System.exit(1);
		}
		break;
	    }
	}
	Path src = (opt.rest.length > 1) ? Utils.path(opt.rest[1]) : Utils.path("src");
	if(!Files.isDirectory(src)) {
	    System.err.println("get-code: destination directory does not exist: " + src);
	    System.exit(1);
	}
	if(url == null) {
	    if((url = resurl.get()) == null) {
		System.err.println("get-code: no resource URL configured");
		System.exit(1);
	    }
	}
	ResSource source = new HttpSource(url);
	Pattern srcpat = Pattern.compile("@.*FromResource\\s*\\([^)]*name\\s*=\\s*\"([^\"]*)\"[^)]*version\\s*=\\s*(\\d+)[^)]*\\)");
	Map<String, Integer> found = new HashMap<>();
	try {
	    for(Path sp : (Iterable<Path>)Files.walk(src)::iterator) {
		if(Files.isRegularFile(sp) && sp.getFileName().toString().endsWith(".java")) {
		    Matcher m = srcpat.matcher(new String(Files.readAllBytes(sp), Utils.utf8));
		    if(m.find()) {
			String nm = m.group(1);
			int ver = Integer.parseInt(m.group(2));
			Integer pver = found.get(nm);
			if((pver != null) && (pver != ver)) {
			    System.err.println("find-updates: warning: found conflicting versions of " + nm + ": " + ver + " and " + pver);
			    found.put(nm, Math.min(ver, pver));
			} else {
			    found.put(nm, ver);
			}
		    }
		}
	    }
	} catch(IOException e) {
	    throw(new RuntimeException(e));
	}
	for(Map.Entry<String, Integer> ent : found.entrySet()) {
	    String nm = ent.getKey();
	    int fver = ent.getValue();
	    try(InputStream in = source.get(nm)) {
		Message fp = new StreamMessage(in);
		if(!Arrays.equals(RESOURCE_SIG, fp.bytes(RESOURCE_SIG.length))) {
		    System.err.println("find-updates: warning: remote resource in invalid: " + nm);
		    continue;
		}
		int resver = fp.uint16();
		if(resver > fver) {
		    System.out.println(nm);
		} else if(resver < fver) {
		    System.err.println("find-updates: warning: " + nm + " is, strangely, newer locally (" + fver + " locally, " + resver + " remotely)");
		}
	    } catch(FileNotFoundException e) {
		System.err.println("find-updates: warning: resource no longer found: " + nm);
	    } catch(IOException e) {
		System.err.println("find-updates: warning: error when checking " + nm + ": " + e);
	    }
	}
    }

    public static void main(String[] args) throws Exception {
	String cmd = args[0].intern();
	if(cmd == "update-list") {
	    updateloadlist(Utils.path(args[1]), Utils.path(args[2]));
	} else if(cmd == "get-code") {
	    cmd_getcode(Utils.splice(args, 1));
	} else if(cmd == "find-updates") {
	    cmd_findupdates(Utils.splice(args, 1));
	}
    }
}
