package haven;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.io.IOUtils;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GobPathConfig {
    private static final Entry DEFAULT = new Entry(Color.WHITE, true, "Unknown");
    private static final Gson gson;

    static {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        builder.registerTypeAdapter(Entry.class, new Adapter().nullSafe());
        gson = builder.create();
    }

    private final Map<String, Entry> entries = new HashMap<String, Entry>();

    public GobPathConfig() {
        load(GobPathConfig.class.getResourceAsStream("gob-paths.config"));
    }

    public Entry get(String resName) {
        if (resName == null)
            return DEFAULT;
        Entry entry = entries.get(resName);
        if (entry == null) {
            Set<String> keys = entries.keySet();
            for (String pattern : keys) {
                if (resName.contains(pattern)) {
                    entry = entries.get(pattern);
                    break;
                }
            }
            if (entry == null)
                entry = DEFAULT;
            entries.put(resName, entry);
        }
        return entry;
    }

    private void load(InputStream stream) {
        entries.clear();
        try {
            String json = IOUtils.toString(stream);
            if(json != null) {
                try {
                    Type collectionType = new TypeToken<HashMap<String, Entry>>() {}.getType();
                    HashMap<String, Entry> jsonEntries = gson.fromJson(json, collectionType);
                    for (Map.Entry<String, Entry> entry : jsonEntries.entrySet()) {
                        entries.put(entry.getKey(), entry.getValue());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class Entry {
        public final Color color;
        public final boolean show;
        public final String name;

        private Entry(Color color, boolean show, String name) {
            this.color = color;
            this.show = show;
            this.name = name;
        }
    }

    private static class Adapter extends TypeAdapter<Entry> {
        @Override
        public void write(JsonWriter writer, Entry entry) throws IOException {
            if (entry == DEFAULT) {
                writer.nullValue();
                return;
            }
            writer.beginObject();
            writer.name("show").value(entry.show);
            String color = Utils.color2hex(entry.color);
            if(color != null) {
                writer.name("color").value(color);
            }
            if(entry.name != null) {
                writer.name("name").value(entry.name);
            }
            writer.endObject();
        }

        @Override
        public Entry read(JsonReader reader) throws IOException {
            boolean show = false;
            Color color = Color.WHITE;
            String entryName = "";
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if ("show".equals(name)) {
                    show = reader.nextBoolean();
                } else if ("color".equals(name)) {
                    color = Utils.hex2color(reader.nextString(), null);
                } else if ("name".equals(name)) {
                    entryName = reader.nextString();
                }
            }
            reader.endObject();
            return new Entry(color, show, entryName);
        }
    }
}
