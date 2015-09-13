package haven.minimap;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.awt.*;
import java.util.*;

public class CustomIconGroup {
    public String name;
    public Color color;
    public boolean show;
    public final java.util.List<CustomIconMatch> matches = new ArrayList<CustomIconMatch>();

    public CustomIconGroup(Element el) {
        name = el.getAttribute("name");
        color = Color.decode(el.getAttribute("color"));
        show = Boolean.parseBoolean(el.hasAttribute("show") ? el.getAttribute("show") : "true");

        NodeList matchNodes = el.getElementsByTagName("match");
        for (int i = 0; i < matchNodes.getLength(); i++)
            matches.add(CustomIconMatch.parse((Element) matchNodes.item(i)));
    }
}
