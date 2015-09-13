package haven.minimap;

import haven.*;
import haven.util.Optional;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.xml.parsers.*;

public class CustomIconConfig {
    private static final String FILE_PATH = "./config/custom-icons.config";
    private static final String RESOURCE_PATH = "/custom-icons.config";
    private static final String MATCH_EXACT = "exact";
    private static final String MATCH_REGEX = "regex";
    private static final String MATCH_STARTS_WITH = "startsWith";
    private static final String MATCH_CONTAINS = "contains";

    public final List<Group> groups = new ArrayList<Group>();
    private final Map<String, Optional<CustomIcon>> cache = new WeakHashMap<String, Optional<CustomIcon>>();
    private final CustomIconFactory factory;
    private final Glob glob;
    private final File file;
    private boolean enabled;

    public CustomIconConfig(Glob glob) {
        this.file = new File(FILE_PATH);
        this.factory = new CustomIconFactory();
        this.enabled = Config.getCustomIconsEnabled();
        this.glob = glob;
        reload();
    }

    public boolean enabled() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
        Config.setCustomIconsEnabled(enabled);
    }

    public CustomIcon getIcon(String resName) {
        Optional<CustomIcon> icon = cache.get(resName);
        if (icon == null) {
            icon = Optional.of(match(resName));
            cache.put(resName, icon);
        }
        return icon.hasValue() ? icon.getValue() : null;
    }

    public void reload() {
        try {
            if (!file.exists())
                copyDefaultConfig();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            groups.clear();
            NodeList groupNodes = doc.getElementsByTagName("group");
            for (int i = 0; i < groupNodes.getLength(); i++)
                groups.add(new Group((Element)groupNodes.item(i)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        reset();
    }

    public void reset() {
        cache.clear();
        synchronized(glob.oc) {
            for(Gob gob : glob.oc)
                gob.delattr(CustomGobIcon.class);
        }
    }

    private CustomIcon match(String resname) {
        for (Group g : groups)
            for (Match m : g.matches)
                if (m.matches(resname)) {
                    if (g.show && m.show) {
                        return (m.image != null)
                            ? factory.res(Resource.remote().load(m.image), g.color)
                            : factory.text(m.title.toUpperCase(), g.color);
                    } else
                        return null;
                }
        return null;
    }

    private void copyDefaultConfig() throws IOException {
        InputStream in = null;
        OutputStream out = null;
        try {
            file.getParentFile().mkdirs();
            in = getClass().getResourceAsStream(RESOURCE_PATH);
            out = new FileOutputStream(file, false);
            IOUtils.copy(in, out);
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
    }

    public static class Group {
        public String name;
        public Color color;
        public boolean show;
        public final List<Match> matches = new ArrayList<Match>();

        public Group(Element el) {
            name = el.getAttribute("name");
            color = Color.decode(el.getAttribute("color"));
            show = Boolean.parseBoolean(el.hasAttribute("show") ? el.getAttribute("show") : "true");

            NodeList matchNodes = el.getElementsByTagName("match");
            for (int i = 0; i < matchNodes.getLength(); i++)
                matches.add(Match.parse((Element)matchNodes.item(i)));
        }
    }

    public static class Match {
        public String type;
        public String value;
        public String title;
        public boolean show;
        public String image;

        private Match(String type, Element el) {
            this.type = type;
            this.value = el.getAttribute(type);
            this.title = el.getAttribute("title");
            this.show = Boolean.parseBoolean(el.hasAttribute("show") ? el.getAttribute("show") : "true");
            this.image = el.hasAttribute("image") ? el.getAttribute("image") : null;
        }

        public static Match parse(Element el) {
            if (el.hasAttribute(MATCH_EXACT))
                return new Match(MATCH_EXACT, el);
            if (el.hasAttribute(MATCH_REGEX))
                return new Match(MATCH_REGEX, el);
            if (el.hasAttribute(MATCH_CONTAINS))
                return new Match(MATCH_CONTAINS, el);
            if (el.hasAttribute(MATCH_STARTS_WITH))
                return new Match(MATCH_STARTS_WITH, el);
            throw new UnsupportedOperationException("Unknown match type");
        }

        public boolean matches(String str) {
            if (type.equals(MATCH_EXACT))
                return str.equals(value);
            if (type.equals(MATCH_STARTS_WITH))
                return str.startsWith(value);
            if (type.equals(MATCH_CONTAINS))
                return str.contains(value);
            if (type.equals(MATCH_REGEX))
                return str.matches(value);
            return false;
        }
    }
}
