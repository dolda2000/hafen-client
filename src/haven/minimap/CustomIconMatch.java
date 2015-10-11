package haven.minimap;

import haven.Coord;
import org.w3c.dom.Element;

public class CustomIconMatch {
    private static final String TYPE_EXACT = "exact";
    private static final String TYPE_REGEX = "regex";
    private static final String TYPE_STARTS_WITH = "startsWith";
    private static final String TYPE_CONTAINS = "contains";

    public String type;
    public String value;
    public String name;
    private String text;
    public boolean show;
    public String image;
    public Coord size;
    public boolean useDefaultIcon;

    private CustomIconMatch(String type, Element el) {
        this.type = type;
        this.value = el.getAttribute(type);
        this.name = el.getAttribute("name");
        this.text = el.getAttribute("text");
        this.show = XmlUtils.getBool(el, "show", true);;
        this.image = el.hasAttribute("image") ? el.getAttribute("image") : null;
        this.useDefaultIcon = XmlUtils.getBool(el, "defaultIcon", false);
        if (el.hasAttribute("size"))
            this.size = parseSize(el.getAttribute("size"));
    }

    public static CustomIconMatch parse(Element el) {
        if (el.hasAttribute(TYPE_EXACT))
            return new CustomIconMatch(TYPE_EXACT, el);
        if (el.hasAttribute(TYPE_REGEX))
            return new CustomIconMatch(TYPE_REGEX, el);
        if (el.hasAttribute(TYPE_CONTAINS))
            return new CustomIconMatch(TYPE_CONTAINS, el);
        if (el.hasAttribute(TYPE_STARTS_WITH))
            return new CustomIconMatch(TYPE_STARTS_WITH, el);
        throw new UnsupportedOperationException("Unknown match type");
    }

    public boolean matches(String str) {
        if (type.equals(TYPE_EXACT))
            return str.equals(value);
        if (type.equals(TYPE_STARTS_WITH))
            return str.startsWith(value);
        if (type.equals(TYPE_CONTAINS))
            return str.contains(value);
        if (type.equals(TYPE_REGEX))
            return str.matches(value);
        return false;
    }

    public String text() {
        return (text != null && !text.isEmpty()) ? text : name;
    }

    public void write(Element el) {
        el.setAttribute(type, value);
        el.setAttribute("name", name);
        el.setAttribute("show", Boolean.toString(show));
        if (useDefaultIcon)
            el.setAttribute("defaultIcon", Boolean.toString(useDefaultIcon));
        if (text != null && !text.isEmpty())
            el.setAttribute("text", text);
        if (image != null)
            el.setAttribute("image", image);
        if (size != null)
            el.setAttribute("size", formatSize(size));
    }

    private static Coord parseSize(String str) {
        String[] parts = str.split("x");
        try {
            if (parts.length == 2) {
                int w = Integer.parseInt(parts[0]);
                int h = Integer.parseInt(parts[1]);
                return new Coord(w, h);
            } else if (parts.length == 1) {
                int n = Integer.parseInt(parts[0]);
                return new Coord(n, n);
            }
            return null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String formatSize(Coord coord) {
        return String.format("%dx%d", coord.x, coord.y);
    }
}
