package haven;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.lang.reflect.Type;
import java.util.*;

public class BeltConfig {
    public static final String BELTS_JSON = "config/belts.config";

    private static final List<Belt> belts;

    static {
        belts = new ArrayList<Belt>();
        Belt[] loaded = load();
        if (loaded != null)
            belts.addAll(Arrays.asList(loaded));

    }

    public static Coord getBeltPosition(String name, Coord defaultValue) {
        Belt belt = findBelt(name);
        return (belt != null) ? new Coord(belt.x, belt.y) : defaultValue;
    }

    public static void setBeltPosition(String name, Coord value) {
        Belt belt = findOrAddBelt(name);
        belt.x = value.x;
        belt.y = value.y;
        save();
    }

    public static Orientation getBeltOrienation(String name, Orientation defaultValue) {
        Belt belt = findBelt(name);
        if (belt != null)
            return (belt.orientation == 0) ? Orientation.Horizontal : Orientation.Vertical;
        return defaultValue;
    }

    public static void setBeltOrienation(String name, Orientation value) {
        Belt belt = findOrAddBelt(name);
        belt.orientation = (value == Orientation.Horizontal) ? 0 : 1;
        save();
    }

    public static List<Indir<Resource>> getSlots(String beltName, String accountName, String charName) {
        List<Indir<Resource>> slots = new ArrayList<Indir<Resource>>();
        Belt belt = findBelt(beltName);
        if (belt != null) {
            SlotCollection sc = belt.findSlots(accountName, charName);
            if (sc != null) {
                if (sc.content != null) {
                    for (int i = 0; i < sc.content.length; i++) {
                        String resName = sc.content[i];
                        slots.add((resName != null) ? Resource.remote().load(sc.content[i]) : null);
                    }
                }
            }
        }
        return slots;
    }

    public static void saveSlots(String beltName, String accountName, String charName, List<Indir<Resource>> slots) {
        Belt belt = findOrAddBelt(beltName);
        SlotCollection sc = belt.findOrAddSlots(accountName, charName);
        sc.content = new String[slots.size()];
        int i = 0;
        for (Indir<Resource> slot : slots) {
            Resource res = (slot != null) ? slot.get() : null;
            sc.content[i++] = (res != null) ? res.name : null;
        }
        save();
    }

    private static Belt findBelt(String name) {
        for (Belt belt : belts) {
            if (belt.name.equals(name))
                return belt;
        }
        return null;
    }

    private static Belt findOrAddBelt(String name) {
        Belt belt = findBelt(name);
        if (belt == null) {
            belt = new Belt();
            belt.name = name;
            belt.slots = new ArrayList<SlotCollection>();
            belts.add(belt);
        }
        return belt;
    }

    private static Belt[] load() {
        File file = new File(BELTS_JSON);
        if (file.exists()) {
            try {
                String json = FileUtils.readFileToString(file);
                if (json != null) {
                    Gson gson = new GsonBuilder().create();
                    Type collectionType = new TypeToken<Belt[]>() {}.getType();
                    return gson.fromJson(json, collectionType);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static void save() {
        try {
            Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
            File file = new File(BELTS_JSON);
            FileUtils.write(file, gson.toJson(belts));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class Belt {
        @SerializedName("name")
        public String name;

        @SerializedName("x")
        public int x;

        @SerializedName("y")
        public int y;

        @SerializedName("or")
        public int orientation;

        @SerializedName("slots")
        public ArrayList<SlotCollection> slots;

        public SlotCollection findSlots(String accountName, String charName) {
            if (slots != null) {
                for (SlotCollection sc : slots) {
                    if (sc.accountName.equals(accountName) && sc.charName.equals(charName))
                        return sc;
                }
            }
            return null;
        }

        public SlotCollection findOrAddSlots(String accountName, String charName) {
            SlotCollection sc = findSlots(accountName, charName);
            if (sc == null) {
                if (slots == null)
                    slots = new ArrayList<SlotCollection>();
                sc = new SlotCollection();
                sc.accountName = accountName;
                sc.charName = charName;
                slots.add(sc);
            }
            return sc;
        }
    }

    private static class SlotCollection {
        @SerializedName("accountName")
        public String accountName;

        @SerializedName("charName")
        public String charName;

        @SerializedName("content")
        public String[] content;
    }
}
