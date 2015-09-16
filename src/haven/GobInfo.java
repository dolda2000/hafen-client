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

import java.awt.*;

public class GobInfo {
    private String text;
    private Color color;

    public GobInfo(String text, Color color) {
        this.text = text;
        this.color = color;
    }

    public PView.Draw2D draw(final Coord sc) {
        return new PView.Draw2D() {
            @Override
            public void draw2d(GOut g) {
                g.atextstroked(text, sc, color, Color.BLACK);
            }
        };
    }

    public static GobInfo get(Gob gob) {
        if (gob == null || gob.getres() == null) return null;
        try {
            if (isSpriteKind("GrowingPlant", gob) || isSpriteKind("TrellisPlant", gob)) {
                int maxStage = 0;
                for (FastMesh.MeshRes layer : gob.getres().layers(FastMesh.MeshRes.class)) {
                    if (layer.id / 10 > maxStage)
                        maxStage = layer.id / 10;
                }
                Message data = getDrawableData(gob);
                if (data != null) {
                    int stage = data.uint8();
                    if (stage >= maxStage) {
                        return new GobInfo(String.format("%d/%d", stage, maxStage), Color.GREEN);
                    } else {
                        return new GobInfo(String.format("%d/%d", stage, maxStage), Color.RED);
                    }
                }
            } else if (isSpriteKind("Tree", gob)) {
                Message data = getDrawableData(gob);
                if (data != null && !data.eom()) {
                    int growth = data.uint8();
                    if (growth < 100)
                        return new GobInfo(String.format("%d%%", growth), Color.YELLOW);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        GobHealth hp = gob.getattr(GobHealth.class);
        if (hp != null && hp.hp < 4)
            return new GobInfo(String.format("%.0f%%", (1f - hp.hp / 4f) * 100f), Color.RED);
        return null;
    }

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
        if (ce != null) {
            Class spc = ce.getClassByTag("spr", false);
            return spc != null && (spc.getSimpleName().equals(kind) || spc.getSuperclass().getSimpleName().equals(kind));
        } else {
            return false;
        }
    }

};
