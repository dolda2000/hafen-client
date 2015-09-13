package haven.minimap;

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

    private CustomIconMatch(String type, Element el) {
        this.type = type;
        this.value = el.getAttribute(type);
        this.name = el.getAttribute("name");
        this.text = el.getAttribute("text");
        this.show = Boolean.parseBoolean(el.hasAttribute("show") ? el.getAttribute("show") : "true");
        this.image = el.hasAttribute("image") ? el.getAttribute("image") : null;
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
}
