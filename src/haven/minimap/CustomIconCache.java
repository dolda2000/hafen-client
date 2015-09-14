package haven.minimap;

import haven.*;
import haven.util.Optional;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.Map;
import java.util.WeakHashMap;

public class CustomIconCache {
    private static final String CONFIG_FILE_PATH = "./config/custom-icons.config";
    private static final String CONFIG_RES_PATH = "/custom-icons.config";

    public final CustomIconConfig config;
    private final Map<String, Optional<CustomIcon>> cache = new WeakHashMap<String, Optional<CustomIcon>>();
    private final CustomIconFactory factory;
    private final Glob glob;
    private boolean enabled;

    public CustomIconCache(Glob glob) {
        File file = new File(CONFIG_FILE_PATH);
        try {
            if (!file.exists())
                copyDefaultConfig(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.config = new CustomIconConfig(file);
        this.factory = new CustomIconFactory();
        this.enabled = Config.getCustomIconsEnabled();
        this.glob = glob;
    }

    public boolean enabled() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
        Config.setCustomIconsEnabled(enabled);
    }

    public CustomIcon get(String resName) {
        Optional<CustomIcon> icon = cache.get(resName);
        if (icon == null) {
            icon = Optional.of(match(resName));
            cache.put(resName, icon);
        }
        return icon.hasValue() ? icon.getValue() : null;
    }

    public void reset() {
        cache.clear();
        synchronized(glob.oc) {
            for(Gob gob : glob.oc) {
                CustomGobIcon icon = gob.getattr(CustomGobIcon.class);
                if (icon != null)
                    gob.delattr(CustomGobIcon.class);
            }
        }
    }

    private CustomIcon match(String resName) {
        for (CustomIconGroup g : config.groups)
            for (CustomIconMatch m : g.matches)
                if (m.matches(resName)) {
                    if (g.show && m.show) {
                        return (m.image != null)
                            ? factory.res(Resource.remote().load(m.image))
                            : factory.text(m.text().toUpperCase(), g.color);
                    } else
                        return null;
                }
        return null;
    }

    private static void copyDefaultConfig(File file) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        try {
            file.getParentFile().mkdirs();
            in = CustomIconCache.class.getResourceAsStream(CONFIG_RES_PATH);
            out = new FileOutputStream(file, false);
            IOUtils.copy(in, out);
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
    }
}
