package haven;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.List;

public class ItemQuality {
    private static final Color COLOR_ESSENCE = new Color(240, 140, 255);
    private static final Color COLOR_SUBSTANCE = new Color(255, 236, 115);
    private static final Color COLOR_VITALITY = new Color(152, 255, 140);
    private static final DecimalFormat format = new DecimalFormat("#.##");

    public final Element essence;
    public final Element substance;
    public final Element vitality;
    public final Element average;

    public ItemQuality(double essence, double substance, double vitality) {
        this.essence = new Element(essence, COLOR_ESSENCE);
        this.substance = new Element(substance, COLOR_SUBSTANCE);
        this.vitality = new Element(vitality, COLOR_VITALITY);
        this.average = new Element((substance + essence + vitality) / 3.0, Color.WHITE);
    }

    public Element getMaxElement() {
        if (essence.value > substance.value && essence.value > vitality.value)
            return essence;
        if (substance.value > vitality.value)
            return substance;
        return vitality;
    }

    public boolean equals(ItemQuality other) {
        if (other != null) {
            return Utils.equals(essence.value, other.essence.value)
                && Utils.equals(substance.value, other.substance.value)
                && Utils.equals(vitality.value, other.vitality.value);
        }
        return false;
    }

    public static ItemQuality fromItemInfo(List<ItemInfo> infos) {
        double substance = 0;
        double essence = 0;
        double vitality = 0;
        try {
            for (ItemInfo info : infos) {
                if ("QBuff".equals(info.getClass().getSimpleName())) {
                    String name = (String)info.getClass().getDeclaredField("name").get(info);
                    double value = (Double)info.getClass().getDeclaredField("q").get(info);
                    if ("Essence".equals(name))
                        essence = value;
                    else if ("Substance".equals(name))
                        substance = value;
                    else if ("Vitality".equals(name))
                        vitality = value;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return new ItemQuality(essence, substance, vitality);
    }

    public class Element {
        public final double value;
        public final Color color;
        private Tex tex;

        public Element(double value, Color color) {
            this.value = value;
            this.color = color;
        }

        public Tex tex() {
            if (tex == null) {
                String text = format.format(value);
                BufferedImage img = Text.render(text, color).img;
                tex = new TexI(Utils.outline2(img, Color.BLACK));
            }
            return tex;
        }
    }
}
