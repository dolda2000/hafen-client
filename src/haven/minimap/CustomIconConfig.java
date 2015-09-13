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
    private static final String FILE_PATH = "./config/minimap-icons.config";
    private static final String RESOURCE_PATH = "/minimap-icons.config";
    private static final String MATCH_EXACT = "exact";
    private static final String MATCH_REGEX = "regex";
    private static final String MATCH_STARTS_WITH = "startsWith";
    private static final String MATCH_CONTAINS = "contains";

    private final Map<String, Optional<CustomIcon>> cache = new WeakHashMap<String, Optional<CustomIcon>>();
    private final CustomIconFactory factory;
    private final File file;
    private final List<Group> groups = new ArrayList<Group>();
    private boolean enabled;

    public CustomIconConfig() {
        this.file = new File(FILE_PATH);
        this.factory = new CustomIconFactory();
        this.enabled = Config.getCustomIconsEnabled();
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
        cache.clear();
    }

    private CustomIcon match(String resName) {
        for (Group g : groups)
            for (Match m : g.matches) {
                if (m.type.equals(MATCH_EXACT)) {
                    if (resName.equals(m.value))
                        return m.show ? factory.text(m.title.toUpperCase(), g.color) : null;
                } else if (m.type.equals(MATCH_STARTS_WITH)) {
                    if (resName.startsWith(m.value))
                        return m.show ? factory.text(m.title.toUpperCase(), g.color) : null;
                } else if (m.type.equals(MATCH_CONTAINS)) {
                    if (resName.contains(m.value))
                        return m.show ? factory.text(m.title.toUpperCase(), g.color) : null;
                } else if (m.type.equals(MATCH_REGEX)) {
                    if (resName.matches(m.value))
                        return m.show ? factory.text(m.title.toUpperCase(), g.color) : null;
                }
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

    private static class Group {
        public String name;
        public Color color;
        public final List<Match> matches = new ArrayList<Match>();

        public Group(Element el) {
            name = el.getAttribute("name");
            color = Color.decode(el.getAttribute("color"));
            NodeList matchNodes = el.getElementsByTagName("match");
            for (int i = 0; i < matchNodes.getLength(); i++)
                matches.add(new Match((Element)matchNodes.item(i)));
        }
    }

    private static class Match {
        public String type;
        public String value;
        public String title;
        public boolean show;

        public Match(Element el) {
            if (el.hasAttribute(MATCH_EXACT)) {
                parseType(MATCH_EXACT, el);
            } else if (el.hasAttribute(MATCH_REGEX)) {
                parseType(MATCH_REGEX, el);
            } else if (el.hasAttribute(MATCH_CONTAINS)) {
                parseType(MATCH_CONTAINS, el);
            } else if (el.hasAttribute(MATCH_STARTS_WITH)) {
                parseType(MATCH_STARTS_WITH, el);
            } else {
                throw new UnsupportedOperationException("Unknown match type");
            }
            this.title = el.getAttribute("title");
            this.show = Boolean.parseBoolean(el.hasAttribute("show") ? el.getAttribute("show") : "true");
        }

        private void parseType(String type, Element el) {
            this.type = type;
            this.value = el.getAttribute(type);
        }
    }
}
