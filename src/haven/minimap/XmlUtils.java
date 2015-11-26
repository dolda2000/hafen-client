package haven.minimap;

import org.w3c.dom.Element;

class XmlUtils {
    public static boolean getBool(Element el, String attrName, boolean defaultValue) {
        if (el.hasAttribute(attrName))
            return Boolean.parseBoolean(el.getAttribute(attrName));
        return defaultValue;
    }
}
