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
import haven.Window;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class AutoStudy extends Task {
    private State state;

    @Override
    protected void onStart() {
        state = new FindCurio();
    }

    @Override
    protected void onTick(double dt) {
        this.state.tick(dt);
    }

    private void setState(State value) {
        this.state = value;
    }

    private interface State {
        void tick(double dt);
    }

    private class FindCurio implements State {

        @Override
        public void tick(double dt) {
            if (!Config.enableAutoStudy.get())
                return;

            try {
                Inventory studyInv = context().gui().chrwdg.sattr.findchild(Inventory.class);
                if (studyInv == null)
                    return;

                int cap = context().gui().ui.sess.glob.cattr.get("int").comp;
                StudyInfo study = new StudyInfo(studyInv, cap);
                if (study.attn >= cap) return;

                fillStudyFrom(study, context().playerInventory());

                if (Config.useCupboardsForAutoStudy.get()) {
                    List<Window> cupboards = context().findWindows("Cupboard");
                    for (Window window : cupboards) {
                        Inventory inv = window.findchild(Inventory.class);
                        if (inv != null)
                            fillStudyFrom(study, inv);
                    }
                }

                setState(new Wait(10, this));
            } catch (Loading ignored) {
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        private void fillStudyFrom(StudyInfo study, Inventory src) {
            for (WItem item : src.children(WItem.class)) {
                study.tryPlace(item);
            }
        }
    }

    private class Wait implements State {
        private final State next;
        private final double timeout;
        private double t;

        public Wait(double timeout, State next) {
            this.next = next;
            this.timeout = timeout;
        }

        @Override
        public void tick(double dt) {
            t += dt;
            if (t > timeout)
                setState(next);
        }
    }

    private static class StudyInfo {
        public final int cap;
        public final Inventory inventory;
        public final Set<String> names = new HashSet<String>();
        public int attn = 0;
        private final boolean[][] slots;

        public StudyInfo(Inventory studyInventory, int cap) {
            this.cap = cap;
            this.inventory = studyInventory;
            this.slots = new boolean[studyInventory.isz.x][studyInventory.isz.y];

            for (WItem item : studyInventory.children(WItem.class)) {
                String name = getItemName(item);
                if (name != null)
                    names.add(name);
                Curiosity cr = getCurioInfo(item);
                if (cr != null)
                    attn += cr.mw;
                Coord pos = getItemPos(item);
                Coord size = getItemSize(item);
                for (int x = 0; x < size.x; x++)
                    for (int y = 0; y < size.y; y++)
                        slots[pos.x + x][pos.y + y] = true;
            }
        }

        public void tryPlace(WItem item) {
            String name = getItemName(item);
            Curiosity cr = getCurioInfo(item);
            if (name != null && cr != null && cr.mw <= (cap - attn) && !names.contains(name)) {
                Coord slot = findFreeSlot(item);
                if (slot != null) {
                    item.item.wdgmsg("take", Coord.z);
                    inventory.wdgmsg("drop", slot);
                    names.add(name);
                    attn += cr.mw;

                    Coord size = getItemSize(item);
                    for (int i = 0; i < size.x; i++)
                        slots[slot.x + i][slot.y] = true;
                    for (int i = 0; i < size.y; i++)
                        slots[slot.x][slot.y + i] = true;
                }
            }
        }

        private Coord findFreeSlot(WItem item) {
            Coord size = getItemSize(item);
            for (int x = 0; x < slots.length - size.x + 1; x++)
                for (int y = 0; y < slots[x].length - size.y + 1; y++)
                    if (!slots[x][y]) {
                        boolean fits = true;
                        for (int i = 1; i < size.x; i++)
                            if (slots[x + i][y]) {
                                fits = false;
                                break;
                            }
                        if (fits) {
                            for (int i = 1; i < size.y; i++)
                                if (slots[x][y + i]) {
                                    fits = false;
                                    break;
                                }
                        }
                        if (fits)
                            return new Coord(x, y);
                    }
            return null;
        }
    }

    private static Curiosity getCurioInfo(WItem item) {
        return ItemInfo.find(Curiosity.class, item.item.info());
    }

    private static String getItemName(WItem item) {
        ItemInfo.Name name = ItemInfo.find(ItemInfo.Name.class, item.item.info());
        return (name != null) ? name.str.text : null;
    }

    private static Coord getItemPos(WItem item) {
        return item.c.div(Inventory.sqsz);
    }

    private static Coord getItemSize(WItem item) {
        return item.sz.div(Inventory.sqsz);
    }
}
