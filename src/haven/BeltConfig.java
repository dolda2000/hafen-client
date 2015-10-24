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
    public static final String BELTS_JSON = "config/belt.config";

    private static final List<Belt> belts;

    static {
        Belt[] loaded = load();
        belts = (loaded != null) ? Arrays.asList(loaded) : new ArrayList<Belt>();

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
    }
}
