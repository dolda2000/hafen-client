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

package haven.ffi.dbus;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.net.*;
import java.nio.file.*;
import haven.*;
import haven.iosys.tk.*;
import haven.ffi.dbus.DBus.Variant;
import haven.ffi.dbus.DBus.Type;
import static haven.ffi.dbus.DBus.Type.*;
import static haven.PType.*;

public class DesktopPortal {
    public static final String NAME = "org.freedesktop.portal.Desktop";
    public static final String PATH = "/org/freedesktop/portal/desktop";
    private final ObjectProxy obj;
    private final Set<String> ifaces;

    private DesktopPortal(ObjectProxy obj) {
	this.obj = obj;
	ifaces = new HashSet<>(obj.introspect().ifaces.keySet());
	if(ifaces.isEmpty())
	    throw(new DBusError("org.freedesktop.DBus.Error.UnknownObject", "No desktop portal object"));
    }

    public class Request {
	public static final String IFACE = "org.freedesktop.portal.Request";
	private static final AtomicInteger nextid = new AtomicInteger(1);
	public String token = Integer.toString(nextid.getAndIncrement());
	public final IfaceProxy p;
	public final Promise<List<Object>> response = new Promise<>();
	private final DBus.Listener l;
	private boolean done = false;

	public Request() {
	    this.p = new IfaceProxy(new ObjectProxy(obj.bus, obj.name, path()), IFACE);
	    this.l = p.obj.bus.listen(null, p.obj.path, IFACE, "Response", this::response);
	}

	void checkret(String p) {
	    if(!Utils.eq(p, path()))
		cancel(null, new RuntimeException("returned response path strangely does not match"));
	}

	void checkret(List<Object> ret) {
	    checkret(STR.of(ret.get(0)));
	}

	private void cancel(List<Object> value, Throwable error) {
	    synchronized(this) {
		if(!done) {
		    p.call(Collections.emptyList(), "Close");
		    if(value == null)
			response.reject(error);
		    else
			response.resolve(value);
		    done = true;
		}
		l.remove();
	    }
	}

	public void cancel(List<Object> value) {
	    cancel(value, (value == null) ? new RuntimeException("cancelled") : null);
	}

	private boolean response(DBus.Signal sig) {
	    synchronized(this) {
		if(!done)
		    response.resolve(sig.args);
		done = true;
	    }
	    return(false);
	}

	private String path() {
	    String bn = obj.bus.name;
	    if(bn.charAt(0) == ':')
		bn = bn.substring(1);
	    bn = bn.replace('.', '_');
	    return("/org/freedesktop/portal/desktop/request/" + bn + "/" + token);
	}

	public Map<String, Variant> options() {
	    return(Utils.<String, Variant>map().put("handle_token", Variant.of(token)).map());
	}
    }

    public class OpenURI {
	public static final String IFACE = "org.freedesktop.portal.OpenURI";
	public final IfaceProxy p = new IfaceProxy(obj, IFACE);
	public final int version;

	public OpenURI() {
	    version = INT.of(p.property("version"));
	}

	private static final List<Type> sig_openuri = Arrays.asList(STRING, STRING, new Array(new Mapping(STRING, VARIANT)));
	public Request OpenURI(URI uri) {
	    Request req = new Request();
	    req.checkret(p.call(sig_openuri, "OpenURI", "", uri.toString(), req.options()).waitfor());
	    return(req);
	}

	private static final List<Type> sig_schemesupported = Arrays.asList(STRING, new Array(new Mapping(STRING, VARIANT)));
	public boolean SchemeSupported(String scheme) {
	    return(BOOL.of(p.call(sig_schemesupported, "SchemeSupported", scheme, new Object[0]).waitfor().get(0)));
	}
    }
    private OpenURI openuri = null;
    public synchronized OpenURI OpenURI() {
	if(openuri == null) {
	    if(!ifaces.contains(OpenURI.IFACE))
		throw(new DBusError("org.freedesktop.DBus.Error.UnknownInterface", "OpenURI interface not supported"));
	    openuri = new OpenURI();
	}
	return(openuri);
    }

    public class FileChooser {
	public static final String IFACE = "org.freedesktop.portal.FileChooser";
	public final IfaceProxy p = new IfaceProxy(obj, IFACE);
	public final int version;

	public FileChooser() {
	    version = INT.of(p.property("version"));
	}

	public FilePicker make(FilePicker.Mode mode, String window) {
	    return(new FilePicker() {
		final List<Object> filters = new ArrayList<>();

		public void filter(String desc, String... exts) {
		    List<Object> edesc = new ArrayList<>();
		    for(String ext : exts)
			edesc.add(Arrays.asList(0, "*." + ext));
		    filters.add(Arrays.asList(desc, edesc));
		}

		private static final List<Type> sig_openfile = Arrays.asList(STRING, STRING, new Array(new Mapping(STRING, VARIANT)));
		private static final List<Type> sig_savefile = Arrays.asList(STRING, STRING, new Array(new Mapping(STRING, VARIANT)));
		public Promise<Path> show() {
		    Request req = new Request();
		    Map<String, Variant> options = new HashMap<>(req.options());
		    if(!filters.isEmpty())
			options.put("filters", Variant.array(new Struct(STRING, new Array(new Struct(UINT32, STRING))), filters));
		    if(mode == Mode.OPEN)
			req.checkret(p.call(sig_openfile, "OpenFile", (window == null) ? "" : window, "Open file", options).waitfor());
		    else
			req.checkret(p.call(sig_savefile, "SaveFile", (window == null) ? "" : window, "Open file", options).waitfor());
		    return(req.response.map(resp -> {
			if(INT.of(resp.get(0)) != 0)
			    return(null);
			Map<?, ?> res = MAP.of(resp.get(1));
			List<?> paths = LIST.of(((Variant)res.get("uris")).value);
			if(paths.isEmpty())
			    return(null);
			if(paths.size() != 1)
			    throw(new RuntimeException("strange result from desktop-portal"));
			return(Paths.get(Utils.uri(STR.of(paths.get(0)))));
		    }));
		}
	    });
	}
    }
    private FileChooser filechooser = null;
    public synchronized FileChooser FileChooser() {
	if(filechooser == null) {
	    if(!ifaces.contains(FileChooser.IFACE))
		throw(new DBusError("org.freedesktop.DBus.Error.UnknownInterface", "FileChooser interface not supported"));
	    filechooser = new FileChooser();
	}
	return(filechooser);
    }

    private static DesktopPortal instance = null;
    public static synchronized DesktopPortal get() {
	if(instance == null)
	    instance = new DesktopPortal(new ObjectProxy(DBus.get(DBus.Std.SESSION), NAME, PATH));
	return(instance);
    }
}
