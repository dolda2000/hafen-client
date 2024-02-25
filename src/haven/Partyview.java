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

import haven.Party.Member;

import java.util.*;
import java.awt.Color;
import java.util.Map.Entry;

public class Partyview extends Widget {
    public static final int marg = UI.scale(4);
    public final Party party;
    public final long ign;
    private final Button leave;
    private Map<Member, MemberView> avs = Collections.emptyMap();
    private Map<Long, Member> om = null;

    @RName("pv")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    return(new Partyview(ui.sess.glob.party, Utils.uiv(args[0])));
	}
    }

    Partyview(Party party, long ign) {
	super(Coord.of(Avaview.dasz.x + Window.wbox.bisz().x, 140));
	this.party = party;
	this.ign = ign;
	this.leave = add(new Button(sz.x, "Leave"), Coord.z);
	this.leave.settip("Leave party");
	pack();
	this.leave.hide();
    }

    protected void added() {
	update();
    }

    public static class MemberView extends Frame {
	public final Member m;
	public Color color = Color.WHITE;
	private Tex tooltip = null;

	public MemberView(Coord sz, Member m) {
	    super(sz, false);
	    this.m = m;
	    add(new Avaview(this.sz.sub(box.bisz()), m.gobid, "avacam"), Coord.z).canactivate = true;
	}

	public void wdgmsg(Widget sender, String msg, Object... args) {
	    if(sender instanceof Avaview)
		wdgmsg(msg, args);
	    else
		super.wdgmsg(sender, msg, args);
	}

	public void drawframe(GOut g) {
	    g.chcolor(color);
	    box.draw(g, Coord.z, sz);
	}

	public Object tooltip(Coord c, Widget prev) {
	    Gob gob = m.getgob();
	    if(gob == null)
		return(tooltip);
	    KinInfo ki = gob.getattr(KinInfo.class);
	    if(ki == null)
		return(null);
	    return(tooltip = ki.rendered());
	}
    }

    private void update() {
	int asz = (sz.x - marg) / 2;
	if(party.memb != this.om) {
	    Map<Member, MemberView> old = new HashMap<>(this.avs);
	    Map<Member, MemberView> avs = null;
	    for(Member m : party.memb.values()) {
		if(m.gobid == ign)
		    continue;
		MemberView ava = old.remove(m);
		if(ava == null)
		    ava = add(new MemberView(Coord.of(asz), m));
		if(avs == null)
		    avs = new HashMap<>();
		avs.put(m, ava);
	    }
	    for(MemberView ava : old.values())
		ava.reqdestroy();
	    if(avs == null)
		avs = Collections.emptyMap();
	    List<Member> order = new ArrayList<>(avs.keySet());
	    Collections.sort(order, Comparator.comparing(m -> m.seq));
	    int i = 0;
	    for(Member m : order) {
		avs.get(m).move(leave.pos("bl").add((i % 2) * (sz.x - asz), (i / 2) * (asz + marg) + marg));
		i++;
	    }
	    this.om = party.memb;
	    this.avs = avs;
	    if(leave.show(!avs.isEmpty()))
		pack();
	}
	for(Map.Entry<Member, MemberView> e : avs.entrySet())
	    e.getValue().color = e.getKey().col;
    }

    public void tick(double dt) {
	update();
	super.tick(dt);
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
	if(sender == leave) {
	    wdgmsg("leave");
	    return;
	}
	if(sender instanceof MemberView) {
	    wdgmsg("click", (int)((MemberView)sender).m.gobid, args[0]);
	    return;
	}
	super.wdgmsg(sender, msg, args);
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "list") {
	    Map<Long, Member> nmemb = new HashMap<>(), cmemb = party.memb;
	    for(int a = 0; a < args.length; a++) {
		long id = Utils.uiv(args[a]);
		Member m = cmemb.get(id);
		if(m == null)
		    m = party.new Member(id);
		nmemb.put(id, m);
	    }
	    party.memb = nmemb;
	} else if(msg == "ldr") {
	    party.leader = party.memb.get(Utils.uiv(args[0]));
	} else if(msg == "m") {
	    int a = 0;
	    Member m = party.memb.get(Utils.uiv(args[a++]));
	    if(m != null) {
		Coord2d c = null;
		if((a < args.length) && (args[a] instanceof Coord))
		    c = ((Coord)args[a++]).mul(OCache.posres);
		if((a < args.length) && (args[a] instanceof Color))
		    m.col = (Color)args[a++];
		m.setc(c);
	    }
	} else {
	    super.uimsg(msg, args);
	}
    }

    public void dispose() {
	/* XXX: Arguably, glob.party should be removed entirely, but
	 * until then, at least clear it when logging out. */
	party.memb = Collections.emptyMap();
	party.leader = null;
	super.dispose();
    }
}
