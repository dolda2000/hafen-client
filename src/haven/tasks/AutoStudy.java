package haven.tasks;/*
 *  This file is part of bdew's Haven & Hearth modified client.
 *  Copyright (C) 2015 bdew
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

import haven.*;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class AutoStudy extends Task {
    private long last = Long.MIN_VALUE;

    private static void closeAllInv(GameUI gui) {
        for (Widget c = gui.child; c != null; c = c.next) {
            if (c instanceof haven.Window) {
                Inventory inv = c.findchild(Inventory.class);
                if (inv != null && inv != gui.maininv && inv.parent != gui.studywnd) {
                    c.wdgmsg("close");
                }
            }
        }
    }

    @Override
    protected void onTick(double dt) {
        if (!Config.enableAutoStudy.get())
            return;

        Inventory study = context().gui().chrwdg.sattr.findchild(Inventory.class);
        if (study == null || System.currentTimeMillis() < last + 10000) return;
        last = System.currentTimeMillis();
        try {
            int attn = 0;
            int cap = context().gui().ui.sess.glob.cattr.get("int").comp;
            Set<String> active = new HashSet<String>();

            for (GItem i : study.children(GItem.class)) {
                ItemInfo.Name nm = ItemInfo.find(ItemInfo.Name.class, i.info());
                if (nm != null)
                    active.add(nm.str.text);
                Curiosity cr = ItemInfo.find(Curiosity.class, i.info());
                if (cr != null)
                    attn += cr.mw;
            }

            if (attn >= cap) return;

            for (GItem i : context().gui().maininv.children(GItem.class)) {
                ItemInfo.Name nm = ItemInfo.find(ItemInfo.Name.class, i.info());
                Curiosity cr = ItemInfo.find(Curiosity.class, i.info());
                if (nm != null && cr != null && cr.mw <= cap - attn && !active.contains(nm.str.text)) {
                    context().gui().msg("Auto adding curiosity: " + nm.str.text, Color.WHITE);
                    closeAllInv(context().gui());
                    active.add(nm.str.text);
                    attn += cr.mw;
                    i.wdgmsg("transfer", Coord.z);
                }
            }
        } catch (Loading ignored) {
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
