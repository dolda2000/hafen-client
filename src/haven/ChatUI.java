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
import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.font.TextAttribute;
import java.awt.font.TextHitInfo;
import java.awt.image.BufferedImage;
import java.text.*;
import java.text.AttributedCharacterIterator.Attribute;
import java.net.URI;
import java.util.regex.*;
import java.io.IOException;
import java.awt.datatransfer.*;

public class ChatUI extends Widget {
    public static final RichText.Foundry fnd = new RichText.Foundry(new ChatParser(TextAttribute.FONT, Text.dfont.deriveFont(UI.scale(12f)), TextAttribute.FOREGROUND, Color.BLACK)).aa(true);
    public static final Text.Foundry qfnd = new Text.Foundry(Text.dfont, 12, new java.awt.Color(192, 255, 192));
    public static final int selw = UI.scale(130);
    public static final Coord marg = UI.scale(new Coord(9, 9));
    public static final Color[] urgcols = new Color[] {
	null,
	new Color(0, 128, 255),
	new Color(255, 128, 0),
	new Color(255, 0, 0),
    };
    public Channel sel = null;
    public int urgency = 0;
    private final Selector chansel;
    private Coord base = Coord.z;
    private QuickLine qline = null;
    private final LinkedList<Notification> notifs = new LinkedList<Notification>();
    private UI.Grab qgrab;

    public ChatUI() {
	super(Coord.of(0, UI.scale(Math.max(minh, Utils.getprefi("chatsize", minh)))));
	chansel = add(new Selector(new Coord(selw, sz.y - marg.y)), marg);
	setfocusctl(true);
	setcanfocus(true);
    }

    protected void added() {
	base = this.c;
	resize(this.sz);
    }

    public static class ChatAttribute extends Attribute {
	private ChatAttribute(String name) {
	    super(name);
	}

	public static final Attribute HYPERLINK = new ChatAttribute("hyperlink");
    }

    public static class ChatParser extends RichText.Parser {
	public static final Pattern urlpat = Pattern.compile("\\b((https?://)|(www\\.[a-z0-9_.-]+\\.[a-z0-9_.-]+))[a-z0-9/_.~#%+?&:*=-]*", Pattern.CASE_INSENSITIVE);
	public static final Map<? extends Attribute, ?> urlstyle = RichText.fillattrs(TextAttribute.FOREGROUND, new Color(64, 64, 255),
										      TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);

	public ChatParser(Object... args) {
	    super(args);
	}

	protected RichText.Part text(PState s, String text, Map<? extends Attribute, ?> attrs) throws IOException {
	    RichText.Part ret = null;
	    int p = 0;
	    while(true) {
		Matcher m = urlpat.matcher(text);
		if(!m.find(p))
		    break;
		URI uri;
		try {
		    String su = text.substring(m.start(), m.end());
		    if(su.indexOf(':') < 0)
			su = "http://" + su;
		    uri = Utils.uri(su);
		} catch(IllegalArgumentException e) {
		    p = m.end();
		    continue;
		}
		RichText.Part lead = new RichText.TextPart(text.substring(p, m.start()), attrs);
		if(ret == null) ret = lead; else ret.append(lead);
		Map<Attribute, Object> na = new HashMap<Attribute, Object>(attrs);
		na.putAll(urlstyle);
		na.put(ChatAttribute.HYPERLINK, uri);
		ret.append(new RichText.TextPart(text.substring(m.start(), m.end()), na));
		p = m.end();
	    }
	    if(ret == null)
		ret = new RichText.TextPart(text, attrs);
	    else
		ret.append(new RichText.TextPart(text.substring(p), attrs));
	    return(ret);
	}
    }

    public static abstract class Channel extends Widget {
	public final List<RenderedMessage> rmsgs = new ArrayList<>();
	public int urgency = 0;
	private final Scrollbar sb;
	private final IButton cb;
	private double dy;

	/* Deprecated? */
	public final List<Message> msgs = new AbstractList<Message>() {
		public int size() {return(rmsgs.size());}
		public Message get(int i) {return(rmsgs.get(i).msg);}
	    };

	public static abstract class Message {
	    public final double time = Utils.ntime();

	    public abstract Indir<Text> render(int w);
	    public boolean valid(Indir<Text> prev) {
		return(true);
	    }
	    public boolean mousedown(Channel chan, CharPos pos, Coord c, int btn) {return(false);}
	    public boolean mouseup(Channel chan, CharPos pos, Coord c, int btn) {return(false);}
	    public boolean clicked(Channel chan, CharPos pos, Coord c, int btn) {return(false);}
	}

	private RenderedMessage soldest = null, snewest = null;
	public class RenderedMessage {
	    public final Message msg;
	    public final int idx;
	    private Indir<Text> data;
	    private Text text;
	    RenderedMessage snext = null, sprev = null;
	    double lseen = 0;
	    int w, y;

	    public RenderedMessage(Message msg, int idx, int iw) {
		this.msg = msg;
		this.idx = idx;
		this.w = iw;
	    }

	    public Indir<Text> data() {
		if(data == null)
		    data = msg.render(w);
		return(data);
	    }

	    private void slink() {
		synchronized(Channel.this) {
		    sprev = null;
		    snext = snewest;
		    if(soldest == null)
			soldest = this;
		    if(snewest != null)
			snewest.sprev = this;
		    snewest = this;
		}
	    }

	    private void sunlink() {
		synchronized(Channel.this) {
		    if(sprev != null)
			sprev.snext = snext;
		    if(snext != null)
			snext.sprev = sprev;
		    if(snewest == this)
			snewest = snext;
		    if(soldest == this)
			soldest = sprev;
		}
	    }

	    public Text text() {
		lseen = ui.lasttick;
		if(text == null) {
		    text = data().get();
		} else {
		    sunlink();
		}
		slink();
		return(text);
	    }

	    private Coord sz = null;
	    public int h() {
		if(sz == null)
		    sz = text().sz();
		return(sz.y);
	    }

	    public void clear() {
		if(text != null) {
		    text.dispose();
		    text = null;
		    sunlink();
		}
	    }

	    public void invalidate() {
		clear();
		if(data != null) {
		    if(data instanceof Disposable)
			((Disposable)data).dispose();
		    data = null;
		}
		sz = null;
	    }

	    public void resize(int w) {
		if(this.w != w) {
		    this.w = w;
		    invalidate();
		}
	    }

	    public boolean update() {
		if((data == null) || msg.valid(data))
		    return(false);
		invalidate();
		return(true);
	    }
	}

	private void trimunseen() {
	    double now = ui.lasttick;
	    while(true) {
		RenderedMessage rm = soldest;
		if((rm == null) || (now - rm.lseen < 10))
		    break;
		rm.clear();
	    }
	}

	public static class SimpleMessage extends Message {
	    public final String text;
	    public final Color col;

	    public SimpleMessage(String text, Color col) {
		this.text = text;
		this.col = col;
	    }

	    @Deprecated
	    public SimpleMessage(String text, Color col, int w) {this(text, col);}

	    public Indir<Text> render(int w) {
		if(col == null)
		    return(() -> fnd.render(RichText.Parser.quote(text), w));
		else
		    return(() -> fnd.render(RichText.Parser.quote(text), w, TextAttribute.FOREGROUND, col));
	    }
	}

	public Channel(boolean closable) {
	    sb = add(new Scrollbar(0, 0, 0));
	    if(closable)
		cb = add(new IButton("gfx/hud/chat-close", "", "-d", "-h"));
	    else
		cb = null;
	}

	public void append(Message msg, int urgency) {
	    synchronized(rmsgs) {
		RenderedMessage rm = new RenderedMessage(msg, rmsgs.size(), iw());
		if(rmsgs.isEmpty()) {
		    rm.y = 0;
		} else {
		    RenderedMessage lm = rmsgs.get(rmsgs.size() - 1);
		    rm.y = lm.y + lm.h();
		}
		rmsgs.add(rm);
		boolean b = sb.val >= sb.max;
		sb.max = rm.y + rm.h() - ih();
		if(b)
		    sb.val = sb.max;
	    }
	    getparent(ChatUI.class).notify(this, msg, urgency);
	    updurgency(Math.max(this.urgency, urgency));
	}

	public void append(Message msg) {
	    append(msg, 0);
	}

	public void append(String line, Color col) {
	    append(new SimpleMessage(line, col));
	}

	public int iw() {
	    return(sz.x - sb.sz.x);
	}

	public int ih() {
	    return(sz.y);
	}

	public void updurgency(int urg) {
	    if(urgency != urg) {
		urgency = urg;
		int mu = 0;
		ChatUI p = getparent(ChatUI.class);
		for(Selector.DarkChannel ch : p.chansel.chls)
		    mu = Math.max(mu, ch.chan.urgency);
		p.urgency = mu;
	    }
	}

	public int messageat(int y, boolean nearest) {
	    synchronized(rmsgs) {
		int t = 0, b = rmsgs.size();
		while(b > t) {
		    int c = (t + b) / 2;
		    RenderedMessage rm = rmsgs.get(c);
		    if(rm.y > y) {
			b = c;
		    } else if(rm.y + rm.h() < y) {
			t = c + 1;
		    } else {
			return(c);
		    }
		}
		return(nearest ? t : -1);
	    }
	}

	public RenderedMessage messageat(Coord c, Coord hc) {
	    synchronized(rmsgs) {
		int mi = messageat(sb.val + c.y, false);
		if(mi < 0)
		    return(null);
		RenderedMessage rm = rmsgs.get(mi);
		if(hc != null) {
		    hc.x = c.x;
		    hc.y = sb.val + c.y - rm.y;
		}
		return(rm);
	    }
	}

	private void updyseq(int mi) {
	    synchronized(rmsgs) {
		mi = Math.min(mi, rmsgs.size() - 1);
		RenderedMessage lm = rmsgs.get(mi++);
		int y = lm.y + lm.h();
		while(mi < rmsgs.size()) {
		    RenderedMessage rm = rmsgs.get(mi++);
		    rm.y = y;
		    y += rm.h();
		}
		boolean b = sb.val >= sb.max;
		sb.max = y - ih();
		if(b)
		    sb.val = sb.max;
		if(sb.val > sb.max)
		    sb.val = sb.max;
	    }
	}

	public void draw(GOut g) {
	    g.chcolor(0, 0, 0, 128);
	    g.frect(Coord.z, sz);
	    g.chcolor();
	    int sy = (int)Math.round(dy), h = ih(), w = iw();
	    boolean sel = false;
	    synchronized(rmsgs) {
		int smi = messageat(sy, true);
		if(smi < rmsgs.size()) {
		    int y = rmsgs.get(smi).y;
		    boolean upd = false;
		    for(int mi = smi; mi < rmsgs.size(); mi++) {
			RenderedMessage rm = rmsgs.get(mi);
			if(y > sy + h)
			    break;
			if(rm.w != w) {
			    rm.resize(w);
			    upd = true;
			}
			if(rm.update())
			    upd = true;
			if((selstart != null) && (selend != null)) {
			    if((rm.idx >= selstart.rm.idx) && (rm.idx <= selend.rm.idx))
				drawsel(g, rm, rm.y - sy);
			}
			g.image(rm.text().tex(), new Coord(0, y - sy));
			y += rm.h();
		    }
		    if(upd)
			updyseq(smi);
		}
	    }
	    super.draw(g);
	    updurgency(0);
	    trimunseen();
	}

	public void tick(double dt) {
	    super.tick(dt);
	    double ty = sb.val;
	    dy = ty + (Math.pow(2, -dt * 40) * (dy - ty));
	}

	public boolean mousewheel(Coord c, int amount) {
	    sb.ch(amount * 45);
	    return(true);
	}

	public void resize(Coord sz) {
	    super.resize(sz);
	    if(sb != null) {
		sb.move(new Coord(sz.x - (UI.scale(12) - marg.x), UI.scale(34) - marg.y));
		sb.resize(ih() - sb.c.y);
		if(!rmsgs.isEmpty()) {
		    RenderedMessage lm = rmsgs.get(rmsgs.size() - 1);
		    boolean b = sb.val >= sb.max;
		    sb.max = lm.y + lm.h() - ih();
		    if(b)
			sb.val = sb.max;
		}
	    }
	    dy = sb.val;
	    if(cb != null) {
		cb.c = new Coord(sz.x + marg.x - cb.sz.x, -marg.y);
	    }
	}

	@Deprecated
	public void notify(Message msg, int urgency) {
	}

	public static class CharPos {
	    public final RenderedMessage rm;
	    public final int pn;
	    public final RichText.TextPart part;
	    public final TextHitInfo ch;

	    public CharPos(RenderedMessage rm, RichText.TextPart part, TextHitInfo ch) {
		this.rm = rm;
		this.pn = partnum((RichText)rm.text(), part);
		this.part = part;
		this.ch = ch;
	    }

	    private static int partnum(RichText text, RichText.TextPart part) {
		int i = 0;
		for(RichText.Part p = text.parts; p != null; p = p.next) {
		    if(p == part)
			return(i);
		    i++;
		}
		throw(new IllegalArgumentException("Text part not part of text"));
	    }

	    public boolean equals(Object oo) {
		if(!(oo instanceof CharPos)) return(false);
		CharPos o = (CharPos)oo;
		return((o.rm == this.rm) && (o.part == this.part) && o.ch.equals(this.ch));
	    }

	    public String toString() {
		return(String.format("#<charpos %s(%s) %s %s>", rm.msg, rm.idx, part, ch));
	    }
	}

	public final Comparator<CharPos> poscmp = new Comparator<CharPos>() {
	    public int compare(CharPos a, CharPos b) {
		if(a.rm != b.rm) {
		    return(a.rm.idx - b.rm.idx);
		} else if(a.pn != b.pn) {
		    return(a.pn - b.pn);
		} else {
		    return(a.ch.getInsertionIndex() - b.ch.getInsertionIndex());
		}
	    }
	};

	public CharPos charat(Coord c) {
	    if(c.y < -sb.val) {
		synchronized(rmsgs) {
		    if(rmsgs.isEmpty())
			return(null);
		    RenderedMessage rm = rmsgs.get(0);
		    if(!(rm.text() instanceof RichText))
			return(null);
		    RichText.TextPart fp = null;
		    for(RichText.Part part = ((RichText)rm.text()).parts; part != null; part = part.next) {
			if(part instanceof RichText.TextPart) {
			    fp = (RichText.TextPart)part;
			    break;
			}
		    }
		    if(fp == null)
			return(null);
		    return(new CharPos(rm, fp, TextHitInfo.leading(0)));
		}
	    }

	    Coord hc = new Coord();
	    RenderedMessage rm = messageat(c, hc);
	    if((rm == null) || !(rm.text() instanceof RichText))
		return(null);
	    RichText rt = (RichText)rm.text();
	    RichText.Part p = rt.partat(hc);
	    if(p == null) {
		RichText.TextPart lp = null;
		for(RichText.Part part = rt.parts; part != null; part = part.next) {
		    if(part instanceof RichText.TextPart)
			lp = (RichText.TextPart)part;
		}
		if(lp == null) return(null);
		return(new CharPos(rm, lp, TextHitInfo.trailing(lp.end - lp.start - 1)));
	    }
	    if(!(p instanceof RichText.TextPart))
		return(null);
	    RichText.TextPart tp = (RichText.TextPart)p;
	    return(new CharPos(rm, tp, tp.charat(hc)));
	}

	private CharPos selorig, lasthit, selstart, selend;
	private UI.Grab grab;
	private boolean dragging;
	public boolean mousedown(Coord c, int btn) {
	    if(super.mousedown(c, btn))
		return(true);
	    if(grab != null)
		return(true);
	    CharPos ch = charat(c);
	    selorig = ch;
	    if(btn == 1) {
		selstart = selend = null;
		if(ch != null) {
		    lasthit = ch;
		    dragging = false;
		    grab = ui.grabmouse(this);
		}
		return(true);
	    } else {
		if(ch != null)
		    ch.rm.msg.mousedown(this, ch, c, btn);
	    }
	    return(true);
	}

	public void mousemove(Coord c) {
	    if(grab != null) {
		CharPos ch = charat(c);
		if((ch != null) && !ch.equals(lasthit)) {
		    lasthit = ch;
		    if(!dragging && !ch.equals(selorig))
			dragging = true;
		    int o = poscmp.compare(selorig, ch);
		    if(o < 0) {
			selstart = selorig; selend = ch;
		    } else if(o > 0) {
			selstart = ch; selend = selorig;
		    } else {
			selstart = selend = null;
		    }
		}
	    } else {
		super.mousemove(c);
	    }
	}

	protected void selected(CharPos start, CharPos end) {
	    StringBuilder buf = new StringBuilder();
	    synchronized(rmsgs) {
		for(int mi = start.rm.idx; mi <= end.rm.idx; mi++) {
		    RenderedMessage rm = rmsgs.get(mi);
		    if(!(rm.text() instanceof RichText))
			continue;
		    RichText rt = (RichText)rm.text();
		    RichText.Part part = null;
		    if(rm == start.rm) {
			for(part = rt.parts; part != null; part = part.next) {
			    if(part == start.part)
				break;
			}
		    } else {
			part = rt.parts;
		    }
		    for(; part != null; part = part.next) {
			if(!(part instanceof RichText.TextPart))
			    continue;
			RichText.TextPart tp = (RichText.TextPart)part;
			CharacterIterator iter = tp.ti();
			int sch;
			if(tp == start.part)
			    sch = tp.start + start.ch.getInsertionIndex();
			else
			    sch = tp.start;
			int ech;
			if(tp == end.part)
			    ech = tp.start + end.ch.getInsertionIndex();
			else
			    ech = tp.end;
			for(int i = sch; i < ech; i++)
			    buf.append(iter.setIndex(i));
			if(part == end.part)
			    break;
			buf.append(' ');
		    }
		    if(rm != end.rm)
			buf.append('\n');
		}
	    }
	    Clipboard cl;
	    if((cl = java.awt.Toolkit.getDefaultToolkit().getSystemSelection()) == null)
		cl = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
	    try {
		final CharPos ownsel = selstart;
		cl.setContents(new StringSelection(buf.toString()),
			       new ClipboardOwner() {
			public void lostOwnership(Clipboard cl, Transferable tr) {
			    if(selstart == ownsel)
				selstart = selend = null;
			}
		    });
	    } catch(IllegalStateException e) {}
	}

	protected void clicked(CharPos pos) {
	    AttributedCharacterIterator inf = pos.part.ti();
	    inf.setIndex(pos.ch.getCharIndex() + pos.part.start);
	    URI uri = (URI)inf.getAttribute(ChatAttribute.HYPERLINK);
	    if(uri != null) {
		try {
		    WebBrowser.sshow(uri.toURL());
		} catch(java.net.MalformedURLException e) {
		    getparent(GameUI.class).error("Could not follow link.");
		} catch(WebBrowser.BrowserException e) {
		    getparent(GameUI.class).error("Could not launch web browser.");
		}
	    }
	}

	public boolean mouseup(Coord c, int btn) {
	    if(super.mouseup(c, btn))
		return(true);
	    if(btn == 1) {
		if(grab != null) {
		    if(selstart != null)
			selected(selstart, selend);
		    else
			clicked(selorig);
		    grab.remove();
		    grab = null;
		    dragging = false;
		}
	    } else {
		CharPos ch = charat(c);
		if(ch != null) {
		    ch.rm.msg.mouseup(this, ch, c, btn);
		    if((selorig != null) && ch.equals(selorig))
			ch.rm.msg.clicked(this, ch, c, btn);
		}
	    }
	    return(true);
	}

	public void select() {
	    getparent(ChatUI.class).select(this);
	}

	public void display() {
	    select();
	    ChatUI chat = getparent(ChatUI.class);
	    chat.expand();
	    chat.parent.setfocus(chat);
	}

	private void drawsel(GOut g, RenderedMessage rm, int y) {
	    RichText rt = (RichText)rm.text();
	    boolean sel = rm != selstart.rm;
	    for(RichText.Part part = rt.parts; part != null; part = part.next) {
		if(!(part instanceof RichText.TextPart))
		    continue;
		RichText.TextPart tp = (RichText.TextPart)part;
		if(tp.start == tp.end)
		    continue;
		TextHitInfo a, b;
		if(sel) {
		    a = TextHitInfo.leading(0);
		} else if(tp == selstart.part) {
		    a = selstart.ch;
		    sel = true;
		} else {
		    continue;
		}
		if(tp == selend.part) {
		    sel = false;
		    b = selend.ch;
		} else {
		    b = TextHitInfo.trailing(tp.end - tp.start - 1);
		}
		Coord ul = new Coord(tp.x + (int)tp.advance(0, a.getInsertionIndex()), tp.y + y);
		Coord sz = new Coord((int)tp.advance(a.getInsertionIndex(), b.getInsertionIndex()), tp.height());
		g.chcolor(0, 0, 255, 255);
		g.frect(ul, sz);
		g.chcolor();
		if(!sel)
		    break;
	    }
	}

	public void uimsg(String name, Object... args) {
	    if(name == "sel") {
		select();
	    } else if(name == "dsp") {
		display();
	    } else {
		super.uimsg(name, args);
	    }
	}

	public void wdgmsg(Widget sender, String msg, Object... args) {
	    if(sender == cb) {
		wdgmsg("close");
	    } else {
		super.wdgmsg(sender, msg, args);
	    }
	}

	public abstract String name();

	public boolean selmousedown(Coord c, int btn) {return(false);}
	public boolean selmouseup(Coord c, int btn) {return(false);}
	public boolean selclicked(Coord c, int btn) {return(false);}

	private Indir<Resource> iconres = null;
	public Channel icon(Indir<Resource> res) {iconres = res; return(this);}
	public Resource.Image icon() {
	    return((iconres == null) ? null : iconres.get().layer(Resource.imgc));
	}
    }

    public static class Log extends Channel {
	private final String name;
	
	public Log(String name) {
	    super(false);
	    this.name = name;
	}
	
	public String name() {return(name);}
    }

    public static abstract class EntryChannel extends Channel {
	private final TextEntry in;
	private List<String> history = new ArrayList<String>();
	private int hpos = 0;
	private String hcurrent;

	public EntryChannel(boolean closable) {
	    super(closable);
	    setfocusctl(true);
	    this.in = new TextEntry(0, "") {
		    public void activate(String text) {
			if(text.length() > 0)
			    send(text);
			settext("");
			hpos = history.size();
		    }

		    public boolean keydown(KeyEvent ev) {
			if(ConsoleHost.kb_histprev.key().match(ev)) {
			    if(hpos > 0) {
				if(hpos == history.size())
				    hcurrent = text();
				rsettext(history.get(--hpos));
			    }
			    return(true);
			} else if(ConsoleHost.kb_histnext.key().match(ev)) {
			    if(hpos < history.size()) {
				if(++hpos == history.size())
				    rsettext(hcurrent);
				else
				    rsettext(history.get(hpos));
			    }
			    return(true);
			} else {
			    return(super.keydown(ev));
			}
		    }
		};
	    add(this.in);
	}

	public int ih() {
	    return(sz.y - in.sz.y);
	}

	public void resize(Coord sz) {
	    super.resize(sz);
	    if(in != null) {
		in.c = new Coord(0, this.sz.y - in.sz.y);
		in.resize(this.sz.x);
	    }
	}

	public void send(String text) {
	    history.add(text);
	    wdgmsg("msg", text);
	}
    }

    public static class SimpleChat extends EntryChannel {
	public final String name;

	public SimpleChat(boolean closable, String name) {
	    super(closable);
	    this.name = name;
	}

	public void uimsg(String msg, Object... args) {
	    if((msg == "msg") || (msg == "log")) {
		String line = (String)args[0];
		Color col = null;
		if(args.length > 1) col = (Color)args[1];
		if(col == null) col = Color.WHITE;
		int urgency = (args.length > 2) ? Utils.iv(args[2]) : 0;
		Message cmsg = new SimpleMessage(line, col);
		append(cmsg, urgency);
	    } else {
		super.uimsg(msg, args);
	    }
	}

	public String name() {
	    return(name);
	}
    }

    public static class MultiChat extends EntryChannel {
	public final int urgency;
	private final String name;
	private final Map<Integer, Color> pc = new HashMap<Integer, Color>();

	public class NamedMessage extends Message {
	    public final int from;
	    public final String text;
	    public final Color col;

	    public NamedMessage(int from, String text, Color col) {
		this.from = from;
		this.text = text;
		this.col = col;
	    }

	    public class Rendered implements Indir<Text> {
		public final int w;
		public final String nm;

		public Rendered(int w, String nm) {
		    this.w = w;
		    this.nm = nm;
		}

		public Text get() {
		    return(fnd.render(RichText.Parser.quote(String.format("%s: %s", nm, text)), w, TextAttribute.FOREGROUND, col));
		}
	    }

	    private String nm() {
		BuddyWnd.Buddy b = getparent(GameUI.class).buddies.find(from);
		return((b == null) ? "???" : b.name);
	    }

	    public Indir<Text> render(int w) {
		return(new Rendered(w, nm()));
	    }

	    public boolean valid(Indir<Text> data) {
		return(((Rendered)data).nm.equals(nm()));
	    }
	}

	public class MyMessage extends SimpleMessage {
	    public MyMessage(String text) {
		super(text, new Color(192, 192, 255));
	    }

	    @Deprecated
	    public MyMessage(String text, int w) {this(text);}
	}

	public MultiChat(boolean closable, String name, int urgency) {
	    super(closable);
	    this.name = name;
	    this.urgency = urgency;
	}

	private float colseq = 0;
	private Color nextcol() {
	    return(new Color(Color.HSBtoRGB(colseq = ((colseq + (float)Math.sqrt(2)) % 1.0f), 0.5f, 1.0f)));
	}

	public Color fromcolor(int from) {
	    synchronized(pc) {
		Color c = pc.get(from);
		if(c == null)
		    pc.put(from, c = nextcol());
		return(c);
	    }
	}

	public void uimsg(String msg, Object... args) {
	    if(msg == "msg") {
		Number from = (Number)args[0];
		String line = (String)args[1];
		if(from == null) {
		    append(new MyMessage(line), -1);
		} else {
		    Message cmsg = new NamedMessage(from.intValue(), line, fromcolor(from.intValue()));
		    append(cmsg, urgency);
		}
	    } else {
		super.uimsg(msg, args);
	    }
	}

	public String name() {
	    return(name);
	}
    }
    
    public static class PartyChat extends MultiChat {
	public PartyChat() {
	    super(false, "Party", 2);
	}

	public void uimsg(String msg, Object... args) {
	    if(msg == "msg") {
		Number from = (Number)args[0];
		long gobid = Utils.uiv(args[1]);
		String line = (String)args[2];
		Color col = Color.WHITE;
		synchronized(ui.sess.glob.party.memb) {
		    Party.Member pm = ui.sess.glob.party.memb.get(gobid);
		    if(pm != null)
			col = pm.col;
		}
		if(from == null) {
		    append(new MyMessage(line), -1);
		} else {
		    Message cmsg = new NamedMessage(from.intValue(), line, Utils.blendcol(col, Color.WHITE, 0.5));
		    append(cmsg, urgency);
		}
	    } else {
		super.uimsg(msg, args);
	    }
	}
    }
    
    public static class PrivChat extends EntryChannel {
	private final int other;
	
	public class InMessage extends SimpleMessage {
	    public InMessage(String text) {
		super(text, new Color(255, 128, 128, 255));
	    }
	}

	public class OutMessage extends SimpleMessage {
	    public OutMessage(String text) {
		super(text, new Color(128, 128, 255, 255));
	    }
	}

	public PrivChat(boolean closable, int other) {
	    super(closable);
	    this.other = other;
	}

	public void uimsg(String msg, Object... args) {
	    if(msg == "msg") {
		String t = (String)args[0];
		String line = (String)args[1];
		if(t.equals("in")) {
		    Message cmsg = new InMessage(line);
		    append(cmsg, 3);
		} else if(t.equals("out")) {
		    append(new OutMessage(line), -1);
		}
	    } else if(msg == "err") {
		String err = (String)args[0];
		Message cmsg = new SimpleMessage(err, Color.RED);
		append(cmsg, 3);
	    } else {
		super.uimsg(msg, args);
	    }
	}
	
	public String name() {
	    BuddyWnd.Buddy b = getparent(GameUI.class).buddies.find(other);
	    if(b == null)
		return("???");
	    else
		return(b.name);
	}
    }
    
    @RName("schan")
    public static class $SChan implements Factory {
	public Widget create(UI ui, Object[] args) {
	    String name = (String)args[0];
	    Channel ret = new SimpleChat(false, name);
	    if(args.length > 1)
		ret.icon(ui.sess.getresv(args[1]));
	    return(ret);
	}
    }
    @RName("mchat")
    public static class $MChat implements Factory {
	public Widget create(UI ui, Object[] args) {
	    String name = (String)args[0];
	    int urgency = Utils.iv(args[1]);
	    Channel ret = new MultiChat(false, name, urgency);
	    if(args.length > 2)
		ret.icon(ui.sess.getresv(args[2]));
	    return(ret);
	}
    }
    @RName("pchat")
    public static class $PChat implements Factory {
	public Widget create(UI ui, Object[] args) {
	    Channel ret = new PartyChat();
	    if(args.length > 0)
		ret.icon(ui.sess.getresv(args[0]));
	    return(ret);
	}
    }
    @RName("pmchat")
    public static class $PMChat implements Factory {
	public Widget create(UI ui, Object[] args) {
	    int other = Utils.iv(args[0]);
	    Channel ret = new PrivChat(true, other);
	    if(args.length > 1)
		ret.icon(ui.sess.getresv(args[1]));
	    return(ret);
	}
    }

    public void addchild(Widget child, Object... args) {
	add(child);
    }

    public <T extends Widget> T add(T w) {
	if(w instanceof Channel) {
	    Channel chan = (Channel)w;
	    chan.c = chansel.c.add(chansel.sz.x, 0);
	    chan.resize(sz.x - marg.x - chan.c.x, sz.y - chan.c.y);
	    super.add(w);
	    chansel.add(chan);
	    select(chan, false);
	    return(w);
	} else {
	    return(super.add(w));
	}
    }

    public void cdestroy(Widget w) {
	if(w instanceof Channel) {
	    Channel chan = (Channel)w;
	    if(chan == sel)
		sel = null;
	    chansel.rm(chan);
	}
    }
    
    private static final Tex chandiv = Resource.loadtex("gfx/hud/chat-cdiv");
    private static final Tex chanseld = Resource.loadtex("gfx/hud/chat-csel");
    private class Selector extends Widget {
	public final BufferedImage ctex = Resource.loadimg("gfx/hud/chantex");
	public final Text.Foundry tf = new Text.Foundry(Text.serif.deriveFont(Font.BOLD, UI.scale(12))).aa(true);
	public final Color[] uc = {
	    new Color(80, 40, 0),
	    new Color(0, 128, 255),
	    new Color(255, 128, 0),
	    new Color(255, 0, 0),
	};
	private final List<DarkChannel> chls = new ArrayList<DarkChannel>();
	private final int iconsz = UI.scale(16), ellw = tf.strsize("...").x, maxnmw = selw - iconsz;
	private final int offset = chandiv.sz().y + chanseld.sz().y;
	private int ts = 0;
	private double ds = 0;
	private Channel cstart;

	private Text namedeco(String name, BufferedImage img, Color col) {
	    PUtils.tilemod(img.getRaster(), ctex.getRaster(), Coord.z);
	    img = PUtils.rasterimg(PUtils.blurmask2(img.getRaster(), 1, 1, col));
	    return(new Text(name, img));
	}

	public Text nmrender(String name, Color col) {
	    return(namedeco(name, tf.render(name).img, col));
	}

	public int chidx(Channel chan) {
	    for(int i = 0; i < chls.size(); i++) {
		if(chls.get(i).chan == chan)
		    return(i);
	    }
	    return(-1);
	}

	public void resize(Coord sz) {
	    int si = chidx(sel);
	    boolean fit = (((si * offset) - ts) >= 0) && (((si * offset) - ts + chanseld.sz().y) <= this.sz.y);
	    super.resize(sz);
	    ds = clips((int)Math.round(ds));
	    ts = clips(ts);
	    if(fit)
		show(si);
	}

	private class DarkChannel {
	    public final Channel chan;
	    public Text rname;
	    public Tex ricon;
	    private int urgency = 0;
	    private Resource.Image icon;

	    private DarkChannel(Channel chan) {
		this.chan = chan;
	    }

	    public Text rname() {
		String name = chan.name();
		int urg = chan.urgency;
		if((rname == null) || !rname.text.equals(name) || (urgency != urg)) {
		    Text.Line raw = tf.render(name);
		    if(raw.sz().x > maxnmw) {
			int len = raw.charat(maxnmw - ellw);
			raw = tf.render(name.substring(0, len) + "...");
		    }
		    BufferedImage img = raw.img;
		    rname = namedeco(name, img, uc[urgency = urg]);
		}
		return(rname);
	    }

	    public Tex ricon() {
		if((ricon == null) || (icon != chan.icon())) {
		    Resource.Image img = chan.icon();
		    if(img == null)
			return(null);
		    Coord sz;
		    if(img.sz.x > img.sz.y)
			sz = Coord.of(iconsz, (iconsz * img.sz.y) / img.sz.x);
		    else
			sz = Coord.of((iconsz * img.sz.x) / img.sz.y, iconsz);
		    ricon = new TexI(PUtils.uiscale(img.img, sz));
		    icon = img;
		}
		return(ricon);
	    }
	}

	public Selector(Coord sz) {
	    super(sz);
	}

	private void add(Channel chan) {
	    synchronized(chls) {
		chls.add(new DarkChannel(chan));
	    }
	}

	private void rm(Channel chan) {
	    synchronized(chls) {
		for(Iterator<DarkChannel> i = chls.iterator(); i.hasNext();) {
		    DarkChannel c = i.next();
		    if(c.chan == chan)
			i.remove();
		}
	    }
	}

	public void draw(GOut g) {
	    int ds = (int)Math.round(this.ds);
	    synchronized(chls) {
		for(int i = ds / offset; i < chls.size(); i++) {
		    int y = i * offset - ds;
		    if(y >= sz.y)
			break;
		    DarkChannel ch = chls.get(i);
		    if(ch.chan == sel)
			g.image(chanseld, Coord.of(0, y));
		    Tex name = ch.rname().tex(), icon = null;
		    try {
			icon = ch.ricon();
		    } catch(Loading l) {}
		    int my = y + (chanseld.sz().y / 2) - UI.scale(1);
		    if(icon == null) {
			g.aimage(name, Coord.of(sz.x / 2, my), 0.5, 0.5);
		    } else {
			int w = name.sz().x + icon.sz().x;
			int x = (sz.x - w) / 2;
			g.aimage(icon, Coord.of(x, my), 0.0, 0.5); x += icon.sz().x;
			g.aimage(name, Coord.of(x, my), 0.0, 0.5);
		    }
		    g.image(chandiv, Coord.of(0, y + chanseld.sz().y));
		}
	    }
	}

	public void tick(double dt) {
	    super.tick(dt);
	    ds = ts + (Math.pow(2, -dt * 20) * (ds - ts));
	}

	public void show(int si) {
	    int ty = si * offset;
	    if(ty - ts + chanseld.sz().y > sz.y)
		ts = ty + chanseld.sz().y - sz.y;
	    else if(ty - ts < 0)
		ts = ty;
	}

	public void show(Channel chan) {
	    int si = chidx(chan);
	    if(si >= 0)
		show(si);
	}

	public boolean up() {
	    Channel prev = null;
	    for(DarkChannel ch : chls) {
		if(ch.chan == sel) {
		    if(prev != null) {
			select(prev);
			return(true);
		    } else {
			return(false);
		    }
		}
		prev = ch.chan;
	    }
	    return(false);
	}

	public boolean down() {
	    for(Iterator<DarkChannel> i = chls.iterator(); i.hasNext();) {
		DarkChannel ch = i.next();
		if(ch.chan == sel) {
		    if(i.hasNext()) {
			select(i.next().chan);
			return(true);
		    } else {
			return(false);
		    }
		}
	    }
	    return(false);
	}

	private Channel bypos(Coord c) {
	    int ds = (int)Math.round(this.ds);
	    int i = (c.y + ds) / offset;
	    if((i >= 0) && (i < chls.size()))
		return(chls.get(i).chan);
	    return(null);
	}

	public boolean mousedown(Coord c, int button) {
	    Channel chan = bypos(c);
	    cstart = chan;
	    if(chan != null) {
		if(button == 1) {
		    select(chan);
		} else {
		    chan.selmousedown(c, button);
		}
	    }
	    return(true);
	}

	public boolean mouseup(Coord c, int button) {
	    Channel chan = bypos(c);
	    if(chan != null) {
		if(button != 1) {
		    chan.selmouseup(c, button);
		    if(cstart == chan)
			chan.selclicked(c, button);
		}
	    }
	    cstart = null;
	    return(true);
	}

	private int clips(int s) {
	    int maxh = (chls.size() * offset) - sz.y - chandiv.sz().y;
	    return(Math.max(Math.min(s, maxh), 0));
	}

	public boolean mousewheel(Coord c, int amount) {
	    if(!ui.modshift) {
		ts = clips(ts + (amount * UI.scale(40)));
	    } else {
		if(amount < 0)
		    up();
		else if(amount > 0)
		    down();
	    }
	    return(true);
	}
    }

    public void select(Channel chan, boolean focus) {
	Channel prev = sel;
	sel = chan;
	if(prev != null)
	    prev.hide();
	sel.show();
	chansel.show(chan);
	resize(sz);
	if(focus || hasfocus)
	    setfocus(chan);
    }

    public void select(Channel chan) {
	select(chan, true);
    }

    private class Notification {
	public final Channel chan;
	public final Channel.Message msg;
	public final Text chnm, rmsg;
	public final double time = Utils.ntime();

	private Notification(Channel chan, Channel.Message msg) {
	    this.chan = chan;
	    this.msg = msg;
	    this.chnm = fnd.render(chan.name(), 0, TextAttribute.FOREGROUND, Color.WHITE);
	    this.rmsg = msg.render(sz.x - selw).get();
	}
    }

    private Text.Line rqline = null;
    private int rqpre;
    public void drawsmall(GOut g, Coord br, int h) {
	Coord c;
	if(qline != null) {
	    if((rqline == null) || !qline.buf.lneq(rqline.text)) {
		String pre = String.format("%s> ", qline.chan.name());
		rqline = qfnd.render(pre + qline.buf.line());
		rqpre = pre.length();
	    }
	    int point = qline.buf.point(), mark = qline.buf.mark();
	    int px = rqline.advance(point + rqpre) + UI.scale(1);
	    if(mark >= 0) {
		int mx = rqline.advance(mark + rqpre) + UI.scale(1);
		g.chcolor(TextEntry.selcol);
		g.frect2(Coord.of(br.x + Math.min(px, mx), br.y - UI.scale(18)),
			 Coord.of(br.x + Math.max(px, mx), br.y - UI.scale(6)));
		g.chcolor();
	    }
	    c = br.sub(UI.scale(0, 20));
	    g.image(rqline.tex(), c);
	    g.line(Coord.of(br.x + px, br.y - UI.scale(18)), Coord.of(br.x + px, br.y - UI.scale(6)), 1);
	} else {
	    c = br.sub(UI.scale(0, 5));
	}
	double now = Utils.ntime();
	synchronized(notifs) {
	    for(Iterator<Notification> i = notifs.iterator(); i.hasNext();) {
		Notification n = i.next();
		if(now - n.time > 5.0) {
		    i.remove();
		    continue;
		}
		int mh = Math.max(n.chnm.sz().y, n.rmsg.sz().y);
		if((c.y -= mh) < br.y - h)
		    break;
		g.chcolor(0, 0, 0, 192);
		g.frect2(c.add(selw - UI.scale(12) - n.chnm.sz().x, 0),
			 c.add(selw + n.rmsg.sz().x + UI.scale(2), mh));
		g.chcolor(192, 192, 192, 255);
		g.line(c.add(selw - UI.scale(5), 1), c.add(selw - UI.scale(5), mh - 1), 1);
		g.chcolor();
		g.image(n.chnm.tex(),
			c.add(selw - UI.scale(10) - n.chnm.sz().x, (mh - n.chnm.sz().y) / 2),
			br.sub(0, h), br.add(selw - UI.scale(10), 0));
		g.image(n.rmsg.tex(), c.add(selw, (mh - n.rmsg.sz().y) / 2));
	    }
	}
    }

    private static final Tex bulc = Resource.loadtex("gfx/hud/chat-lc");
    private static final Tex burc = Resource.loadtex("gfx/hud/chat-rc");
    private static final Tex bhb = Resource.loadtex("gfx/hud/chat-hori");
    private static final Tex bvlb = Resource.loadtex("gfx/hud/chat-verti");
    private static final Tex bvrb = bvlb;
    private static final Tex bmf = Resource.loadtex("gfx/hud/chat-mid");
    private static final Tex bcbd = Resource.loadtex("gfx/hud/chat-close-g");
    public void draw(GOut g) {
	g.rimage(Window.bg, marg, sz.sub(marg.x * 2, marg.y));
	super.draw(g);
	g.image(bulc, new Coord(0, 0));
	g.image(burc, new Coord(sz.x - burc.sz().x, 0));
	g.rimagev(bvlb, new Coord(0, bulc.sz().y), sz.y - bulc.sz().y);
	g.rimagev(bvrb, new Coord(sz.x - bvrb.sz().x, burc.sz().y), sz.y - burc.sz().y);
	g.rimageh(bhb, new Coord(bulc.sz().x, 0), sz.x - bulc.sz().x - burc.sz().x);
	g.aimage(bmf, new Coord(sz.x / 2, 0), 0.5, 0);
	if((sel == null) || (sel.cb == null))
	    g.aimage(bcbd, new Coord(sz.x, 0), 1, 0);
    }

    private static final Resource notifsfx = Resource.local().loadwait("sfx/hud/chat");
    public void notify(Channel chan, Channel.Message msg, int urgency) {
	if(urgency > 0) {
	    synchronized(notifs) {
		notifs.addFirst(new Notification(chan, msg));
	    }
	    ui.sfx(notifsfx);
	}
    }

    private class Spring extends NormAnim {
	final int oy = base.y - c.y, ny;
	Spring(int ny) {
	    super(0.15);
	    this.ny = ny;
	    show();
	}

	public void ntick(double a) {
	    double b = Math.cos(Math.PI * 2.5 * a) * Math.exp(-5 * a);
	    c = Coord.of(c.x, base.y + ny + (int)((ny - oy) * b));
	    if((a == 1.0) && (ny >= 0)) {
		hide();
	    }
	}
    }

    public void resize(Coord sz) {
	super.resize(sz);
	if(visible)
	    this.c = base.add(0, -this.sz.y);
	chansel.resize(new Coord(selw, this.sz.y - marg.y));
	if(sel != null)
	    sel.resize(new Coord(this.sz.x - marg.x - sel.c.x, this.sz.y - sel.c.y));
    }

    public boolean targetshow = false;
    public void sshow(boolean show) {
	clearanims(Spring.class);
	new Spring(show ? -sz.y : 0);
	targetshow = show;
    }

    public void hresize(int h) {
	clearanims(Spring.class);
    }

    public void resize(int w) {
	resize(new Coord(Math.max(w, selw + marg.x + UI.scale(10) + marg.x), sz.y));
    }

    public void move(Coord base) {
	this.c = (this.base = base).add(0, visible ? -sz.y : 0);
    }

    public void expand() {
	if(!visible)
	    sshow(true);
    }

    public void show() {
	super.show();
	targetshow = true;
    }

    public void hide() {
	super.hide();
	targetshow = false;
    }

    private class QuickLine implements ReadLine.Owner {
	public final ReadLine buf;
	public final EntryChannel chan;
	
	private QuickLine(EntryChannel chan) {
	    this.buf = ReadLine.make(this, "");
	    this.chan = chan;
	}
	
	private void cancel() {
	    qline = null;
	    qgrab.remove();
	}
	
	public void done(ReadLine buf) {
	    if(!buf.empty())
		chan.send(buf.line());
	    cancel();
	}

	public boolean key(KeyEvent ev) {
	    if(key_esc.match(ev)) {
		cancel();
		return(true);
	    } else {
		return(buf.key(ev));
	    }
	}
    }

    private UI.Grab dm = null;
    private Coord doff;
    private static final int minh = 111;
    public boolean mousedown(Coord c, int button) {
	int bmfx = (sz.x - bmf.sz().x) / 2;
	if((button == 1) && (c.y < bmf.sz().y) && (c.x >= bmfx) && (c.x <= (bmfx + bmf.sz().x))) {
	    dm = ui.grabmouse(this);
	    doff = c;
	    return(true);
	} else {
	    return(super.mousedown(c, button));
	}
    }

    public void mousemove(Coord c) {
	if(dm != null) {
	    resize(sz.x, Math.max(UI.scale(minh), sz.y + doff.y - c.y));
	} else {
	    super.mousemove(c);
	}
    }

    public boolean mouseup(Coord c, int button) {
	if(dm != null) {
	    dm.remove();
	    dm = null;
	    Utils.setprefi("chatsize", UI.unscale(sz.y));
	    return(true);
	} else {
	    return(super.mouseup(c, button));
	}
    }

    public boolean keydown(KeyEvent ev) {
	boolean M = (ev.getModifiersEx() & (KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)) != 0;
	if(qline != null) {
	    if(M && (ev.getKeyCode() == KeyEvent.VK_UP)) {
		Channel prev = this.sel;
		while(chansel.up()) {
		    if(this.sel instanceof EntryChannel)
			break;
		}
		if(!(this.sel instanceof EntryChannel)) {
		    select(prev);
		    return(true);
		}
		qline = new QuickLine((EntryChannel)sel);
		return(true);
	    } else if(M && (ev.getKeyCode() == KeyEvent.VK_DOWN)) {
		Channel prev = this.sel;
		while(chansel.down()) {
		    if(this.sel instanceof EntryChannel)
			break;
		}
		if(!(this.sel instanceof EntryChannel)) {
		    select(prev);
		    return(true);
		}
		qline = new QuickLine((EntryChannel)sel);
		return(true);
	    }
	    qline.key(ev);
	    return(true);
	} else {
	    if(M && (ev.getKeyCode() == KeyEvent.VK_UP)) {
		chansel.up();
		return(true);
	    } else if(M && (ev.getKeyCode() == KeyEvent.VK_DOWN)) {
		chansel.down();
		return(true);
	    }
	    return(super.keydown(ev));
	}
    }

    public static final KeyBinding kb_quick = KeyBinding.get("chat-quick", KeyMatch.forcode(KeyEvent.VK_ENTER, 0));
    public boolean globtype(char key, KeyEvent ev) {
	if(kb_quick.key().match(ev)) {
	    if(!visible && (sel instanceof EntryChannel)) {
		qgrab = ui.grabkeys(this);
		qline = new QuickLine((EntryChannel)sel);
		return(true);
	    }
	}
	return(super.globtype(key, ev));
    }
}
