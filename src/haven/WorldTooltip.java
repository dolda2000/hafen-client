/*
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

package haven;

import java.util.ArrayList;
import java.util.List;

public class WorldTooltip {
    private static Message getDrawableData(Gob gob) {
        Drawable dr = gob.getattr(Drawable.class);
        ResDrawable d = (dr instanceof ResDrawable) ? (ResDrawable) dr : null;
        if (d != null)
            return d.sdt.clone();
        else
            return null;
    }

    private static boolean isSpriteKind(String kind, Gob gob) {
        Resource.CodeEntry ce = gob.getres().layer(Resource.CodeEntry.class);
        if (ce!=null) {
            Class spc = ce.getClassByTag("spr", false);
            return spc != null && (spc.getSimpleName().equals(kind) || spc.getSuperclass().getSimpleName().equals(kind));
        } else {
            return false;
        }
    }

    public static List<String> getTooltipFromGob(Gob gob) {
        if (gob.getres() != null) {
            ArrayList<String> res = new ArrayList<String>();
            Resource.Tooltip tip = gob.getres().layer(Resource.tooltip);
            if (tip != null)
                res.add(tip.t);
            else
                res.add(gob.getres().name);
            try {
                if (isSpriteKind("GrowingPlant", gob) || isSpriteKind("TrellisPlant", gob)) {
                    int maxStage = 0;
                    for (FastMesh.MeshRes layer : gob.getres().layers(FastMesh.MeshRes.class)) {
                        if (layer.id / 10 > maxStage)
                            maxStage = layer.id / 10;
                    }
                    Message data = getDrawableData(gob);
                    if (data != null) {
                        res.add(String.format("Stage: %d/%d", data.uint8(), maxStage));
                    }
                } else if (isSpriteKind("Tree", gob)) {
                    Message data = getDrawableData(gob);
                    if (data != null && !data.eom()) {
                        res.add(String.format("Growth: %d%%", data.uint8()));
                    }
                }
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
            GobHealth hp = gob.getattr(GobHealth.class);
            if (hp != null)
                res.add(String.format("HP: %d/%d", hp.hp, 4));
            return res;
        } else return null;
    }

    public static String getTooltipFromMap(MCache m, Coord mc) {
        try {
            int tile = m.gettile(mc.div(MCache.tilesz));
            Resource r = m.tilesetr(tile);
            if (r != null)
                return r.name;
            else
                return null;
        } catch (Loading e) {
            return null;
        }
    }
}
