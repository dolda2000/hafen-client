package haven.minimap;

import org.w3c.dom.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CustomIconGroup {
    public String name;
    public Color color;
    public boolean show;
    public final List<CustomIconMatch> matches = new ArrayList<CustomIconMatch>();

    public CustomIconGroup(Element el) {
        name = el.getAttribute("name");
        color = Color.decode(el.getAttribute("color"));
        show = XmlUtils.getBool(el, "show", true);

        NodeList matchNodes = el.getElementsByTagName("match");
        for (int i = 0; i < matchNodes.getLength(); i++)
            matches.add(CustomIconMatch.parse((Element) matchNodes.item(i)));
    }

    public void write(Element el) {
        Document doc = el.getOwnerDocument();
        el.setAttribute("name", name);
        el.setAttribute("color", String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()));
        el.setAttribute("show", Boolean.toString(show));
        for (CustomIconMatch match : matches) {
            Element mel = doc.createElement("match");
            match.write(mel);
            el.appendChild(mel);
        }
    }
}
