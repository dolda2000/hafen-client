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
import java.net.*;
import java.nio.file.*;
import java.lang.reflect.*;
import java.security.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.List;
import haven.Steam.UGItem;
import com.codedisaster.steamworks.SteamResult;
import static haven.Utils.*;

public class WorkshopLauncher {
    public static class Client {
	public final UGItem item;
	public final Path path;
	public final Properties props;

	public Client(UGItem item, Path path, Properties props) {
	    this.item = item;
	    this.path = path;
	    this.props = props;
	}

	public String name() {
	    return(props.getProperty("name", Long.toString(item.fid())));
	}
    }

    public static class MessageException extends IOException {
	public MessageException(String msg) {
	    super(msg);
	}
    }

    public static Path findjvm() throws IOException {
	Path jvm, javadir = pj(path(System.getProperty("java.home")), "bin");
	if(Files.exists(jvm = pj(javadir, "java")))
	   return(jvm);
	if(Files.exists(jvm = pj(javadir, "javaw.exe")))
	   return(jvm);
	if(Files.exists(jvm = pj(javadir, "java.exe")))
	   return(jvm);
	throw(new IOException("could not find a Java executable"));
    }

    public static void centerwnd(JFrame wnd) {
	wnd.setVisible(true);
	Dimension ssz = Toolkit.getDefaultToolkit().getScreenSize();
	Dimension fsz = wnd.getSize();
	wnd.setLocation((ssz.width - fsz.width) / 2, (ssz.height - fsz.height) / 2);
    }

    public static class ClassUnloader extends ClassLoader {
	public final URI unload;

	public ClassUnloader() {
	    super(ClassUnloader.class.getClassLoader());
	    try {
		unload = ClassUnloader.class.getProtectionDomain().getCodeSource().getLocation().toURI();
	    } catch(URISyntaxException e) {
		throw(new RuntimeException(e));
	    }
	}

	private boolean resunloadp(URL url) {
	    if(url.getProtocol().equals("jar")) {
		/* This may look ad-hoc, but it's the same condition
		 * that JarURLConnection uses, so it should be
		 * correct. */
		String spec = url.getFile();
		int p = spec.indexOf("!/");
		try {
		    @SuppressWarnings("deprecation")
		    URI jar = new URL(spec.substring(0, p)).toURI();
		    return(jar.equals(unload));
		} catch(MalformedURLException | URISyntaxException e) {
		}
	    }
	    return(false);
	}

	public URL getResource(String name) {
	    URL ret = getParent().getResource(name);
	    if((ret != null) && resunloadp(ret))
		return(null);
	    return(ret);
	}

	public Enumeration<URL> getResources(String name) throws IOException {
	    Enumeration<URL> ret = getParent().getResources(name);
	    return(new Enumeration<URL>() {
		    URL next;
		    boolean hasnext = false;

		    public boolean hasMoreElements() {
			if(hasnext)
			    return(true);
			while(ret.hasMoreElements()) {
			    next = ret.nextElement();
			    if((next == null) || !resunloadp(next))
				return(hasnext = true);
			}
			return(false);
		    }

		    public URL nextElement() {
			if(!hasMoreElements())
			    throw(new NoSuchElementException());
			URL ret = next;
			hasnext = false;
			return(ret);
		    }
		});
	}

	public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
	    Class<?> ret = getParent().loadClass(name);
	    ProtectionDomain d = ret.getProtectionDomain();
	    if(d == null) return(ret);
	    CodeSource s = d.getCodeSource();
	    if(s == null) return(ret);
	    URL url = s.getLocation();
	    if(url == null) return(ret);
	    try {
		if(url.toURI().equals(unload))
		    throw(new ClassNotFoundException());
	    } catch(URISyntaxException e) {
		return(ret);
	    }
	    return(ret);
	}
    }

    public static boolean launch(Client cl) throws IOException {
	if(cl == null) {
	    MainFrame.main(new String[] {});
	    return(false);
	}
	String desc;
	if((desc = cl.props.getProperty("launcher")) != null) {
	    Path lf = cl.path.resolve(desc);
	    if(!Files.isRegularFile(lf))
		throw(new MessageException(cl.name() + "is misconfigured; its specified launcher file (" + lf + ") does not exist."));
	    Path lj = path("launcher.jar");
	    if(!Files.isRegularFile(lj))
		throw(new MessageException("Internal error: Launcher Jar not found."));
	    List<String> args = new ArrayList<>();
	    args.add(findjvm().toFile().toString());
	    args.add("-jar");
	    args.add(lj.toFile().toString());
	    args.add(lf.toFile().toString());
	    ProcessBuilder spec = new ProcessBuilder(args);
	    spec.inheritIO();
	    spec.start();
	    return(true);
	} else if((desc = cl.props.getProperty("main-class")) != null) {
	    Collection<URL> classpath = new ArrayList<>();
	    for(String js : cl.props.getProperty("class-path", "").split(":")) {
		Path cp = cl.path.resolve(js);
		if(!Files.isRegularFile(cp))
		    throw(new MessageException(cl.name() + "is misconfigured; the specified Jar file (" + cp + ") does not exist."));
		classpath.add(cp.toUri().toURL());
	    }
	    for(Map.Entry<Object, Object> ent : cl.props.entrySet()) {
		String k = (String)ent.getKey();
		if(k.startsWith("sysprop.")) {
		    System.setProperty(k.substring(8), (String)ent.getValue());
		}
	    }
	    if(classpath.isEmpty())
		throw(new MessageException(cl.name() + "is misconfigured; it has a main-class, but no class-path."));
	    ClassLoader code = new URLClassLoader(classpath.toArray(new URL[0]), new ClassUnloader());
	    Class<?> cls;
	    try {
		cls = code.loadClass(desc);
	    } catch(ClassNotFoundException e) {
		throw(new MessageException(cl.name() + "is misconfigured; its main-class (" + desc + ") was not found."));
	    }
	    Method main;
	    try {
		main = cls.getDeclaredMethod("main", String[].class);
	    } catch(NoSuchMethodException e) {
		throw(new MessageException(cl.name() + "is misconfigured; its main-class (" + desc + ") has no main-method."));
	    }
	    if((main.getModifiers() & Modifier.STATIC) == 0)
		throw(new MessageException(cl.name() + "is misconfigured; its main-method is not static."));
	    if((main.getModifiers() & Modifier.PUBLIC) == 0)
		throw(new MessageException(cl.name() + "is misconfigured; its main-method is not public."));
	    try {
		main.invoke(null, (Object)new String[0]);
	    } catch(IllegalAccessException e) {
		throw(new RuntimeException(e));
	    } catch(InvocationTargetException e) {
		throw(new RuntimeException(e));
	    }
	    return(false);
	} else {
	    throw(new MessageException(cl.name() + " is misconfigured; it has no valid launch parameter specified."));
	}
    }

    public static class Chooser extends JFrame {
	public final List<Client> clients;
	private JList clist;
	private JCheckBox memo;
	private JButton lbtn;

	public Chooser(List<Client> clients) {
	    super("Client launcher");
	    this.clients = clients;
	    add(new JPanel() {{
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(new JLabel("You have multiple clients installed. Please choose one to start:"));
		add(clist = new JList<String>(new AbstractListModel<String>() {
			public int getSize() {
			    return(clients.size() + 1);
			}

			public String getElementAt(int i) {
			    if(i == 0)
				return("Default client");
			    return(clients.get(i - 1).name());
			}
		}) {{
		    setPreferredSize(new Dimension(300, 100));
		    long saved = 0;
		    try {
			saved = Long.parseLong(Utils.getpref("workshop/last-client", "0"));
		    } catch(RuntimeException e) {}
		    setSelectedIndex(0);
		    for(int i = 0; i < clients.size(); i++) {
			if(clients.get(i).item.fid() == saved) {
			    setSelectedIndex(i + 1);
			    break;
			}
		    }
		}});
		add(memo = new JCheckBox("Remember this choice (until subscriptions are changed)"));
		add(new JLabel("<html><body style=\"width: 400px\"><hr>" +
			       "<span style=\"color: #ff0000\">WARNING</span>: " +
			       "Please keep in mind that custom clients are independent programs, " +
			       "and can do with your computer whatever any other downloaded program may do. " +
			       "You should only use clients by authors you trust.</body></html>"));
		add(new JPanel() {{
		    setLayout(new FlowLayout(FlowLayout.LEADING));
		    setAlignmentX(0);
		    add(lbtn = new JButton("Launch") {{
			setDefaultCapable(true);
			addActionListener(ev -> {
				int idx = clist.getSelectedIndex();
				if(idx == 0) {
				    claunch(null);
				} else {
				    claunch(clients.get(idx - 1));
				}
			    });
		    }});
		    add(new JButton("Cancel") {{
			addActionListener(ev -> {
				Chooser.this.dispose();
				System.exit(0);
			    });
		    }});
		}});
	    }});
	    getRootPane().setDefaultButton(lbtn);
	    getRootPane().registerKeyboardAction(ev -> {
		    Chooser.this.dispose();
		    System.exit(0);
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
		JComponent.WHEN_IN_FOCUSED_WINDOW);
	    addWindowListener(new WindowAdapter() {
		    public void windowClosing(WindowEvent ev) {
			Chooser.this.dispose();
			System.exit(0);
		    }
		});
	    setResizable(false);
	    pack();
	}

	private void save(Client cl) {
	    Utils.setpref("workshop/last-client", (cl == null) ? "0" : Long.toString(cl.item.fid()));
	    if(memo.isSelected()) {
		StringBuilder buf = new StringBuilder();
		for(int i = 0; i < clients.size(); i++) {
		    if(i > 0)
			buf.append(':');
		    buf.append(Long.toString(clients.get(i).item.fid()));
		}
		Utils.setpref("workshop/skip-chooser", buf.toString());
	    } else {
		Utils.setpref("workshop/skip-chooser", "");
	    }
	}

	public void claunch(Client cl) {
	    try {
		boolean exit = launch(cl);
		save(cl);
		dispose();
		if(exit)
		    System.exit(0);
	    } catch(MessageException e) {
		JOptionPane.showMessageDialog(this, e.getMessage(), "Client error", JOptionPane.ERROR_MESSAGE);
	    } catch(Throwable t) {
		JOptionPane.showMessageDialog(this, "Encountered an unexpected error when launching " + cl.name() + ": " + t.getMessage(), "Client error", JOptionPane.ERROR_MESSAGE);
	    }
	}

	static {
	    MainFrame.initawt();
	}
    }

    public static class Updater extends JFrame {
	public final Iterator<UGItem> items;
	private JProgressBar prog;

	public Updater(Collection<UGItem> items) {
	    super("Workshop");
	    this.items = items.iterator();
	    add(new JPanel() {{
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		JLabel lbl = new JLabel("Updating workshop items...");
		lbl.setAlignmentX(0);
		add(lbl);
		add(prog = new JProgressBar());
		prog.setMinimumSize(new Dimension(200, 0));
		prog.setStringPainted(true);
		add(new JButton("Cancel") {{
		    addActionListener(ev -> {
			    System.exit(0);
			});
		}});
	    }});
	    addWindowListener(new WindowAdapter() {
		    public void windowClosing(WindowEvent ev) {
			System.exit(0);
		    }
		});
	    pack();
	}

	public void run() {
	    SwingUtilities.invokeLater(() -> centerwnd(this));
	    try {
		while(items.hasNext()) {
		    UGItem item = items.next();
		    if(item.installed() && !item.stale())
			continue;
		    SwingUtilities.invokeLater(() -> {
			    prog.setIndeterminate(true);
			    prog.setString("Initializing...");
			});
		    item.download(true);
		    UGItem.Details details = Loading.waitfor(item.details());
		    while(item.dlresult == null) {
			item.update();
			SwingUtilities.invokeLater(() -> {
				if(item.pending()) {
				    prog.setIndeterminate(true);
				    prog.setString(details.title +": Pending");
				} else if(item.fetching()) {
				    if(item.size() > 0) {
					prog.setIndeterminate(false);
					prog.setMaximum((int)(item.size() / 1000));
					prog.setValue((int)(item.got() / 1000));
					prog.setString(String.format("%s: %,d / %,d kB", details.title, item.got() / 1000, item.size() / 1000));
				    } else {
					prog.setIndeterminate(true);
					prog.setString(details.title +": Downloading");
				    }
				} else {
				    prog.setIndeterminate(true);
				    prog.setString(details.title +"...");
				}
			    });
			try {
			    Thread.sleep(100);
			} catch(InterruptedException e) {
			    throw(new RuntimeException(e));
			}
		    }
		    if(item.dlresult != SteamResult.OK)
			throw(new RuntimeException("Download of " + details.title + " failed: " + item.dlresult));
		    item.update();
		}
	    } finally {
		SwingUtilities.invokeLater(this::dispose);
	    }
	}
    }

    public static boolean launchlast(Collection<Client> clients) {
	try {
	    String skipconf = Utils.getpref("workshop/skip-chooser", "");
	    if(skipconf.equals(""))
		return(false);
	    long lastid = Long.parseLong(Utils.getpref("workshop/last-client", "0"));
	    Client last = null;
	    if(lastid != 0) {
		for(Client cl : clients) {
		    if(cl.item.fid() == lastid) {
			last = cl;
			break;
		    }
		}
		if(last == null)
		    return(false);
	    }
	    Set<Long> conf = new HashSet<>();
	    for(String fid : skipconf.split(":"))
		conf.add(Long.parseLong(fid));
	    Set<Long> cur = new HashSet<>();
	    for(Client cl : clients)
		cur.add(cl.item.fid());
	    if(cur.equals(conf)) {
		try {
		    if(launch(last))
			System.exit(0);
		    return(true);
		} catch(IOException e) {
		    Utils.setpref("workshop/skip-chooser", "");
		    return(false);
		}
	    } else {
		Utils.setpref("workshop/skip-chooser", "");
		return(false);
	    }
	} catch(RuntimeException e) {
	    new Warning(e, "could not load client preferences").issue();
	    return(false);
	}
    }

    public static void main2(String[] args) {
	Steam api = Steam.get();
	if(api == null) {
	    MainFrame.main(args);
	    return;
	}
	Collection<UGItem> items = api.ugitems();
	for(UGItem item : items) {
	    if(!item.installed() || item.stale()) {
		new Updater(items).run();
		break;
	    }
	}
	List<Client> clients = new ArrayList<>();
	for(UGItem item : items) {
	    Path path = item.path();
	    if(path == null)
		continue;
	    Path pfile = path.resolve("workshop-client.properties");
	    if(!Files.exists(pfile) || !Files.isReadable(pfile))
		continue;
	    try {
		Properties props = new Properties();
		try(InputStream fp = Files.newInputStream(pfile)) {
		    props.load(fp);
		}
		clients.add(new Client(item, path, props));
	    } catch(IOException e) {
		new Warning(e, "could not load client info: " + path).issue();
	    }
	}
	if(clients.isEmpty()) {
	    MainFrame.main(args);
	    return;
	}
	if(!launchlast(clients)) {
	    api.overlaypos("TopRight");
	    SwingUtilities.invokeLater(() -> centerwnd(new Chooser(clients)));
	}
    }

    public static void main(final String[] args) {
	/* Set up the error handler as early as humanly possible. */
	ThreadGroup g = new ThreadGroup("Haven main group");
	String ed = Utils.getprop("haven.errorurl", "");
	if(!ed.equals("")) {
	    try {
		final haven.error.ErrorHandler hg = new haven.error.ErrorHandler(new java.net.URI(ed).toURL());
		hg.sethandler(new haven.error.ErrorGui(null) {
			public void errorsent() {
			    hg.interrupt();
			}
		    });
		g = hg;
		new DeadlockWatchdog(hg).start();
	    } catch(java.net.MalformedURLException | java.net.URISyntaxException e) {
	    }
	}
	Thread main = new HackThread(g, () -> main2(args), "Workshop main thread");
	main.start();
    }
}
