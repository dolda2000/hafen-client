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
import java.net.*;
import java.io.*;
import java.security.*;
import javax.imageio.*;
import java.awt.image.BufferedImage;

public class Resource implements Serializable {
    private static ResCache prscache;
    public static ThreadGroup loadergroup = null;
    private static Map<String, LayerFactory<?>> ltypes = new TreeMap<String, LayerFactory<?>>();
    public static Class<Image> imgc = Image.class;
    public static Class<Neg> negc = Neg.class;
    public static Class<Anim> animc = Anim.class;
    public static Class<Pagina> pagina = Pagina.class;
    public static Class<AButton> action = AButton.class;
    public static Class<Audio> audio = Audio.class;
    public static Class<Tooltip> tooltip = Tooltip.class;
    
    private Collection<Layer> layers = new LinkedList<Layer>();
    public final String name;
    public int ver;
    public ResSource source;
    public final transient Pool pool;
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

	public static Resource loadsaved(Resource.Pool pool, Resource.Spec spec) {
	    try {
		if(spec.pool == null)
		    return(pool.load(spec.name, spec.ver).get());
		return(spec.get());
	    } catch(Loading l) {
		throw(l);
	    } catch(Exception e) {
		return(pool.load(spec.name).get());
	    }
	}

	public Resource loadsaved(Resource.Pool pool) {
	    return(loadsaved(pool, this));
	}

	public Resource loadsaved() {
	    return(loadsaved(this.pool));
	}
    }

    public static interface Resolver {
	public Indir<Resource> getres(int id);

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
		    ret.put((Integer)args[a], (Integer)args[a + 1]);
		return(ret);
	    }

	    public Indir<Resource> getres(int id) {
		return(bk.getres(map.get(id)));
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
	File base;
	
	public FileSource(File base) {
	    this.base = base;
	}
	
	public InputStream get(String name) throws FileNotFoundException {
	    File cur = base;
	    String[] parts = name.split("/");
	    for(int i = 0; i < parts.length - 1; i++)
		cur = new File(cur, parts[i]);
	    cur = new File(cur, parts[parts.length - 1] + ".res");
	    return(new FileInputStream(cur));
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
	private final transient SslHelper ssl;
	public URL baseurl;
	
	{
	    ssl = new SslHelper();
	    try {
		ssl.trust(Resource.class.getResourceAsStream("ressrv.crt"));
	    } catch(java.security.cert.CertificateException e) {
		throw(new Error("Invalid built-in certificate", e));
	    } catch(IOException e) {
		throw(new Error(e));
	    }
	    ssl.ignoreName();
	}
	
	public HttpSource(URL baseurl) {
	    this.baseurl = baseurl;
	}
		
	private URL encodeurl(URL raw) throws IOException {
	    /* This is "kinda" ugly. It is, actually, how the Java
	     * documentation recommend that it be done, though... */
	    try {
		return(new URL(new URI(raw.getProtocol(), raw.getHost(), raw.getPath(), raw.getRef()).toASCIIString()));
	    } catch(URISyntaxException e) {
		throw(new IOException(e));
	    }
	}

	public InputStream get(String name) throws IOException {
	    URL resurl = encodeurl(new URL(baseurl, name + ".res"));
	    return(new RetryingInputStream() {
		    protected InputStream create() throws IOException {
			URLConnection c;
			if(resurl.getProtocol().equals("https"))
			    c = ssl.connect(resurl);
			else
			    c = resurl.openConnection();
			/* Apparently, some versions of Java Web Start has
			 * a bug in its internal cache where it refuses to
			 * reload a URL even when it has changed. */
			c.setUseCaches(false);
			c.addRequestProperty("User-Agent", "Haven/1.0");
			return(c.getInputStream());
		    }
		});
	}

	public String toString() {
	    return("HTTP res source (" + baseurl + ")");
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

	public void boostprio(int prio) {
	    res.boostprio(prio);
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
		if(!done) {
		    boostprio(1);
		    throw(new Loading(this));
		}
		if(error != null)
		    throw(new RuntimeException("Delayed error in resource " + name + " (v" + ver + "), from " + error.src, error));
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
		try {
		    InputStream in = src.get(res.name);
		    try {
			Resource ret = new Resource(this, res.name, res.ver);
			ret.source = src;
			ret.load(in);
			res.res = ret;
			res.error = null;
			break;
		    } finally {
			in.close();
		    }
		} catch(Throwable t) {
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
			/* Throw LoadException rather than
			 * RuntimeException here, to make sure
			 * obsolete resources doing nested loading get
			 * properly handled. This could be the wrong
			 * way of going about it, however; I'm not
			 * sure. */
			throw(new LoadException(String.format("Weird version number on %s (%d > %d), loaded from %s", cur.name, cur.ver, ver, cur.source), cur));
		    }
		}
		synchronized(queue) {
		    Queued cq = queued.get(name);
		    if(cq != null) {
			if(ver != -1) {
			    if(ver < cq.ver) {
				throw(new LoadException(String.format("Weird version number on %s (%d > %d)", cq.name, cq.ver, ver), null));
			    } else if(ver == cq.ver) {
				cq.boostprio(prio);
				return(cq);
			    }
			} else {
			    if(cq.done && (cq.error != null)) {
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
		    queued.put(name, nq);
		    if(parent == null) {
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
			} else {
			    queued.remove(name);
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

	public Named load(String name, int ver) {return(load(name, ver, -5));}
	public Named load(String name) {return(load(name, -1));}

	public Indir<Resource> dynres(long id) {
	    return(load(String.format("dyn/%x", id), 1));
	}

	private void ckld() {
	    int qsz;
	    synchronized(queue) {
		qsz = queue.size();
	    }
	    synchronized(loaders) {
		while(loaders.size() < Math.min(nloaders, qsz)) {
		    final Loader n = new Loader();
		    Thread th = AccessController.doPrivileged(new PrivilegedAction<Thread>() {
			    public Thread run() {
				return(new HackThread(loadergroup, n, "Haven resource loader"));
			    }
			});
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
			if(Config.resdir != null)
			    local.add(new FileSource(new File(Config.resdir)));
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

    public static void addurl(URL url) {
	ResSource src = new HttpSource(url);
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

    @Deprecated
    public static Resource load(String name, int ver) {
	return(remote().loadwait(name, ver));
    }

    @Deprecated
    public Resource loadwait() {
	return(this);
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
	
    public abstract class Layer implements Serializable {
	public abstract void init();
	
	public Resource getres() {
	    return(Resource.this);
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
	ltypes.put(name, cons);
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
	try {
	    /* This can crash if not privileged due to ImageIO
	     * creating tempfiles without doing that privileged
	     * itself. It can very much be argued that this is a bug
	     * in ImageIO. */
	    return(AccessController.doPrivileged(new PrivilegedExceptionAction<BufferedImage>() {
		    public BufferedImage run() throws IOException {
			BufferedImage ret;
			ret = ImageIO.read(fp);
			if(ret == null)
			    throw(new ImageReadException());
			return(ret);
		    }
		}));
	} catch(PrivilegedActionException e) {
	    Throwable c = e.getCause();
	    if(c instanceof IOException)
		throw((IOException)c);
	    throw(new AssertionError(c));
	}
    }

    @LayerName("image")
    public class Image extends Layer implements Comparable<Image>, IDLayer<Integer> {
	public transient BufferedImage img;
	private transient BufferedImage scaled;
	private transient Tex tex, rawtex;
	public final int z, subz;
	public final boolean nooff;
	public final int id;
	public final Map<String, byte[]> kvdata;
	private float scale = 1;
	private int gay = -1;
	public Coord sz, o, tsz, ssz;

	public Image(Message buf) {
	    z = buf.int16();
	    subz = buf.int16();
	    int fl = buf.uint8();
	    /* Obsolete flag 1: Layered */
	    nooff = (fl & 2) != 0;
	    id = buf.int16();
	    o = cdec(buf);
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
		tsz = sz;
	    ssz = new Coord(Math.round(UI.scale(sz.x / scale)), Math.round(UI.scale(sz.y / scale)));
	}

	public BufferedImage scaled() {
	    if(scaled == null) {
		synchronized(this) {
		    if(scaled == null)
			scaled = PUtils.uiscale(img, ssz);
		}
	    }
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

	private boolean detectgay() {
	    for(int y = 0; y < sz.y; y++) {
		for(int x = 0; x < sz.x; x++) {
		    if((img.getRGB(x, y) & 0x00ffffff) == 0x00ff0080)
			return(true);
		}
	    }
	    return(false);
	}
		
	public boolean gayp() {
	    if(gay == -1)
		gay = detectgay()?1:0;
	    return(gay == 1);
	}

	public int compareTo(Image other) {
	    return(z - other.z);
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
	public interface Instancer {
	    public Object make(Class<?> cl, Resource res, Object... args);

	    public static <T> T stdmake(Class<T> cl, Resource ires, Object[] args) {
		try {
		    Constructor<T> cons = cl.getConstructor(Resource.class, Object[].class);
		    return(Utils.construct(cons, new Object[] {ires, args}));
		} catch(NoSuchMethodException e) {}
		try {
		    Constructor<T> cons = cl.getConstructor(Object[].class);
		    return(Utils.construct(cons, new Object[] {args}));
		} catch(NoSuchMethodException e) {}
		try {
		    Constructor<T> cons = cl.getConstructor(Resource.class);
		    return(Utils.construct(cons, new Object[] {ires}));
		} catch(NoSuchMethodException e) {}
		return(Utils.construct(cl));
	    }

	    public static final Instancer simple = (cl, res, args) -> {
		try {
		    Constructor<?> cons = cl.getConstructor(Object[].class);
		    return(Utils.construct(cons, args));
		} catch(NoSuchMethodException e) {}
		return(Utils.construct(cl));
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

    public class ResClassLoader extends ClassLoader {
	public ResClassLoader(ClassLoader parent) {
	    super(parent);
	}
	
	public Resource getres() {
	    return(Resource.this);
	}
	
	public String toString() {
	    return("cl:" + Resource.this.toString());
	}
    };

    public static Resource classres(final Class<?> cl) {
	return(AccessController.doPrivileged(new PrivilegedAction<Resource>() {
		    public Resource run() {
			ClassLoader l = cl.getClassLoader();
			if(l instanceof ResClassLoader)
			    return(((ResClassLoader)l).getres());
			throw(new RuntimeException("Cannot fetch resource of non-resloaded class " + cl));
		    }
		}));
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

    public static class LibClassLoader extends ClassLoader {
	private final ClassLoader[] classpath;
	
	public LibClassLoader(ClassLoader parent, Collection<ClassLoader> classpath) {
	    super(parent);
	    this.classpath = classpath.toArray(new ClassLoader[0]);
	}
	
	public Class<?> findClass(String name) throws ClassNotFoundException {
	    for(ClassLoader lib : classpath) {
		try {
		    return(lib.loadClass(name));
		} catch(ClassNotFoundException e) {}
	    }
	    throw(new ClassNotFoundException("Could not find " + name + " in any of " + Arrays.asList(classpath).toString()));
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
		    this.loader = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
			    public ClassLoader run() {
				ClassLoader ret = Resource.class.getClassLoader();
				if(classpath.size() > 0) {
				    Collection<ClassLoader> loaders = new LinkedList<ClassLoader>();
				    for(Indir<Resource> res : classpath) {
					loaders.add(res.get().layer(CodeEntry.class).loader());
				    }
				    ret = new LibClassLoader(ret, loaders);
				}
				if(clmap.size() > 0) {
				    ret = new ResClassLoader(ret) {
					    public Class<?> findClass(String name) throws ClassNotFoundException {
						Code c = clmap.get(name);
						if(c == null)
						    throw(new ResourceClassNotFoundException(name, Resource.this));
						return(defineClass(name, c.data, 0, c.data.length));
					    }
					};
				}
				return(ret);
			    }
			});
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
		    inst = AccessController.doPrivileged((PrivilegedAction<Object>)() -> {
			    PublishedCode.Instancer mk;
			    synchronized(PublishedCode.instancers) {
				mk = PublishedCode.instancers.computeIfAbsent(entry, k -> {
					if(k.instancer() == PublishedCode.Instancer.class)
					    return(PublishedCode.Instancer.simple);
					else
					    return(Utils.construct(k.instancer()));
				    });
			    }
			    return(mk.make(acl, Resource.this, args));
			});
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

    @LayerName("audio")
    public class Audio extends Layer implements IDLayer<String> {
	transient public byte[] coded;
	public final String id;
	public double bvol = 1.0;

	public Audio(byte[] coded, String id) {
	    this.coded = coded;
	    this.id = id.intern();
	}

	public Audio(Message buf) {
	    this(buf.bytes(), "cl");
	}

	public void init() {}

	public haven.Audio.CS stream() {
	    try {
		return(new haven.Audio.VorbisClip(new dolda.xiphutil.VorbisStream(new ByteArrayInputStream(coded))));
	    } catch(IOException e) {
		throw(new RuntimeException(e));
	    }
	}

	public String layerid() {
	    return(id);
	}
    }

    @LayerName("audio2")
    public static class Audio2 implements LayerFactory<Audio> {
	public Audio cons(Resource res, Message buf) {
	    int ver = buf.uint8();
	    if((ver == 1) || (ver == 2)) {
		String id = buf.string();
		double bvol = 1.0;
		if(ver == 2)
		    bvol = buf.uint16() / 1000.0;
		Audio ret = res.new Audio(buf.bytes(), id);
		ret.bvol = bvol;
		return(ret);
	    } else {
		throw(new LoadException("Unknown audio layer version: " + ver, res));
	    }
	}
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

    public <L extends Layer> L layer(Class<L> cl) {
	used = true;
	for(Layer l : layers) {
	    if(cl.isInstance(l))
		return(cl.cast(l));
	}
	return(null);
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

    public boolean equals(Object other) {
	if(!(other instanceof Resource))
	    return(false);
	Resource o = (Resource)other;
	return(o.name.equals(this.name) && (o.ver == this.ver));
    }

    private void load(InputStream st) throws IOException {
	Message in = new StreamMessage(st);
	byte[] sig = "Haven Resource 1".getBytes(Utils.ascii);
	if(!Arrays.equals(sig, in.bytes(sig.length)))
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
	    layers.add(lc.cons(this, buf));
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

    public static void updateloadlist(File file, File resdir) throws Exception {
	BufferedReader r = new BufferedReader(new FileReader(file));
	Map<String, Integer> orig = new HashMap<String, Integer>();
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
	r.close();
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
	Writer w = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
	try {
	    dumplist(cur, w);
	} finally {
	    w.close();
	}
    }

    public static void main(String[] args) throws Exception {
	String cmd = args[0].intern();
	if(cmd == "update") {
	    updateloadlist(new File(args[1]), new File(args[2]));
	}
    }
}
