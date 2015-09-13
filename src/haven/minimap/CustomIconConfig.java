package haven.minimap;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class CustomIconConfig {
    public final List<CustomIconGroup> groups = new ArrayList<CustomIconGroup>();
    private final File file;
    private final DocumentBuilderFactory documentBuilderFactory;

    public CustomIconConfig(File file) {
        this.file = file;
        this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
        reload();
    }

    public void reload() {
        try {
            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            Document doc = builder.parse(file);
            groups.clear();
            NodeList groupNodes = doc.getElementsByTagName("group");
            for (int i = 0; i < groupNodes.getLength(); i++)
                groups.add(new CustomIconGroup((Element)groupNodes.item(i)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save() {
        OutputStream out = null;
        try {
            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            Document doc = builder.newDocument();

            // construct XML
            Element root = doc.createElement("icons");
            doc.appendChild(root);
            for (CustomIconGroup group : groups) {
                Element el = doc.createElement("group");
                group.write(el);
                root.appendChild(el);
            }

            // write XML
            out = new FileOutputStream(file, false);
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            DOMSource source = new DOMSource(doc);
            StreamResult console = new StreamResult(out);
            transformer.transform(source, console);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(out);
        }
    }
}
