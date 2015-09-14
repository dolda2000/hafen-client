package haven;

import java.util.List;

public class ItemQuality {
    public final int substance;
    public final int essence;
    public final int vitality;

    public ItemQuality(int substance, int essence, int vitality) {
        this.substance = substance;
        this.essence = essence;
        this.vitality = vitality;
    }

    public static ItemQuality fromItemInfo(List<ItemInfo> infos) {
        int substance = 0;
        int essence = 0;
        int vitality = 0;
        try {
            for (ItemInfo info : infos) {
                if ("QBuff".equals(info.getClass().getSimpleName())) {
                    String name = (String)info.getClass().getDeclaredField("name").get(info);
                    int value = (Integer)info.getClass().getDeclaredField("q").get(info);
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

    public double avg() {
        return (substance + essence + vitality) / 3.0;
    }
}
