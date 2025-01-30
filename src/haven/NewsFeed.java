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
import java.text.*;
import java.time.*;
import java.time.chrono.*;
import java.time.format.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.w3c.dom.*;

public class NewsFeed extends SListBox<NewsFeed.Entry, Widget> {
    public static final Config.Variable<URI> feed = Config.Services.var("newsfeed", "");
    public static final DateTimeFormatter timefmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static final String ATOM = "http://www.w3.org/2005/Atom";
    private final Filtered entries;

    public NewsFeed(int h, Filtered entries) {
	super(Coord.of(Display.size.x + Scrollbar.width, h), Display.size.y, UI.scale(5));
	this.entries = entries;
    }

    public List<Entry> items() {return(entries.unseen);}
    public Widget makeitem(Entry ent, int idx, Coord sz) {return(new Display(this, ent));}

    public static class LinkText extends TextItem implements CursorQuery.Handler {
	public static final Resource hand = Resource.local().loadwait("gfx/hud/curs/hand");
	public final NewsFeed list;
	public final Entry entry;

	public LinkText(int w, NewsFeed list, Entry entry) {
	    super(w);
	    this.entry = entry;
	    this.list = list;
	}

	public String text() {
	    return(entry.name());
	}

	public boolean getcurs(CursorQuery ev) {
	    return(ev.set(hand));
	}

	public boolean mousedown(MouseDownEvent ev) {
	    try {
		WebBrowser.sshow(entry.href().toURL());
	    } catch(Exception e) {
		ui.error("Could not launch web browser: " + e.getMessage());
		return(true);
	    }
	    list.entries.see(Arrays.asList(entry));
	    list.entries.save();
	    return(true);
	}
    }

    public static class Display extends Widget {
	public static final Coord size = UI.scale(300, 35);

	public Display(NewsFeed list, Entry ent) {
	    super(size);
	    add(new LinkText(sz.x, list, ent), UI.scale(2, 2));
	    adda(new Label(timefmt.format(ZonedDateTime.ofInstant(ent.time(), ZoneId.systemDefault()))), pos("cbl").adds(2, -2), 0.0, 1.0);
	}
    }

    public static Iterable<Element> els(Element el, String ns, String tag) {
	NodeList l = el.getElementsByTagNameNS(ns, tag);
	return(() -> new Iterator<Element>() {
		int i = 0;
		public boolean hasNext() {return(i < l.getLength());}
		public Element next() {return((Element)l.item(i++));}
	    });
    }

    public static Element el(Element el, String ns, String tag) {
	NodeList l = el.getElementsByTagNameNS(ns, tag);
	if(l.getLength() == 0)
	    throw(new DOMException(DOMException.NOT_FOUND_ERR, ns + ":" + tag));
	return((Element)l.item(0));
    }

    public static class Entry {
	public static final DocumentBuilderFactory cpconf = DocumentBuilderFactory.newInstance();
	public final Element data;

	public Entry(Element data) {
	    this.data = data;
	}

	public String id() {return(el(data, ATOM, "id").getTextContent());}
	public URI href() {return(Utils.uri(el(data, ATOM, "link").getAttribute("href")));}
	public String name() {
	    for(Element el : els(data, ATOM, "title")) {
		String type = el.getAttribute("type");
		if(type.equals("") || type.equals("text"))
		    return(el.getTextContent());
	    }
	    return(el(data, ATOM, "title").getTextContent());
	}
	public Instant time() {
	    return(Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(el(data, ATOM, "updated").getTextContent())));
	}

	private DocumentFragment contents = null;
	public DocumentFragment contents() {
	    if(contents == null) {
		try {
		    String text = "<content>" + el(data, ATOM, "content").getTextContent() + "</content>";
		    Document doc = cpconf.newDocumentBuilder().parse(new InputSource(new StringReader(text)));
		    DocumentFragment ret = doc.createDocumentFragment();
		    NodeList l = doc.getDocumentElement().getChildNodes();
		    while(l.getLength() > 0)
			ret.appendChild(l.item(0));
		    contents = ret;
		} catch(SAXException e) {
		    throw(new RuntimeException(e));
		} catch(ParserConfigurationException e) {
		    throw(new RuntimeException(e));
		} catch(IOException e) {
		    throw(new RuntimeException(e));
		}
	    }
	    return(contents);
	}
    }

    public static class Filtered {
	public final List<Entry> unseen = new ArrayList<>();
	public final List<Entry> seen = new ArrayList<>();

	public Filtered(List<Entry> fetched) {
	    Set<String> seen = new HashSet<>(Utils.getprefsl("newsfeed-seen", new String[0]));
	    if(seen.isEmpty()) {
		/* Assume this is the first launch and treat all existing news as uninteresting. */
		this.seen.addAll(fetched);
		save();
	    } else {
		for(Entry ent : fetched)
		    (seen.contains(ent.id()) ? this.seen : this.unseen).add(ent);
	    }
	}

	public Filtered see(Collection<Entry> add) {
	    for(Entry ent : add) {
		unseen.remove(ent);
		if(!seen.contains(ent))
		    seen.add(ent);
	    }
	    return(this);
	}

	public Filtered save() {
	    List<String> seen = Utils.getprefsl("newsfeed-seen", new String[0]);
	    List<String> save = new ArrayList<>();
	    for(Entry ent : this.seen) {
		seen.remove(ent.id());
		save.add(ent.id());
	    }
	    for(int i = 0; (i < seen.size()) && (i < 10); i++)
		save.add(seen.get(i));
	    Utils.setprefsl("newsfeed-seen", save);
	    return(this);
	}
    }

    public static List<Entry> fetch() throws IOException {
	URL url = feed.get().toURL();
	try(InputStream fp = new RetryingInputStream() {
		protected InputStream create() throws IOException {
		    URLConnection conn = url.openConnection();
		    conn.addRequestProperty("User-Agent", Http.USER_AGENT);
		    return(conn.getInputStream());
		}
	    })
	{
	    DocumentBuilderFactory conf = DocumentBuilderFactory.newInstance();
	    conf.setNamespaceAware(true);
	    List<Entry> ret = new ArrayList<>();
	    els(conf.newDocumentBuilder().parse(fp).getDocumentElement(), ATOM, "entry").forEach(data -> ret.add(new Entry(data)));
	    return(ret);
	} catch(ParserConfigurationException e) {
	    throw(new RuntimeException(e));
	} catch(SAXException e) {
	    throw(new IOException("feed syntax error", e));
	}
    }

    public static void main(String... args) throws IOException {
	for(Entry ent : fetch()) {
	    Debug.dump(timefmt.format(ZonedDateTime.ofInstant(ent.time(), ZoneId.systemDefault())));
	}
    }
}
