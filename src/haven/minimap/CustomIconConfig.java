package haven.minimap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.*;

public class CustomIconConfig {
    public final List<CustomIconGroup> groups = new ArrayList<CustomIconGroup>();
    private final File file;

    public CustomIconConfig(File file) {
        this.file = file;
        reload();
    }

    public void reload() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            groups.clear();
            NodeList groupNodes = doc.getElementsByTagName("group");
            for (int i = 0; i < groupNodes.getLength(); i++)
                groups.add(new CustomIconGroup((Element)groupNodes.item(i)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
