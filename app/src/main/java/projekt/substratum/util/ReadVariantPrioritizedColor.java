package projekt.substratum.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class ReadVariantPrioritizedColor {

    public static String main(String fileName) {
        try {
            File fXmlFile = new File(fileName);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("color");
            Node nNode = nList.item(0);
            Element eElement = (Element) nNode;
            return eElement.getAttributes().item(0).getNodeValue();
        } catch (Exception e) {
            // At this point, the file does not exist!
        }
        return null;
    }
}