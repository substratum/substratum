package projekt.substratum.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class ReadCloudWallpaperFile {

    public static Map main(String argv[]) {

        Map<String, String> map = new TreeMap<>();
        Map<String, String> emptyMap = new TreeMap<>();

        try {
            File fXmlFile = new File(argv[0]);

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("wallpaper");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;

                    String addon_download_name = eElement.getAttribute("id");
                    String addon_download_link = eElement.getElementsByTagName("link").item(0).
                            getTextContent();
                    String addon_preview_link = eElement.getElementsByTagName("preview").item(0).
                            getTextContent();

                    String[] finalArray = {addon_download_name, addon_download_link,
                            addon_preview_link};

                    map.put(finalArray[0], finalArray[1]);
                    map.put(finalArray[0] + "-preview", finalArray[2]);
                }
            }
            return map;
        } catch (Exception e) {
            return emptyMap;
        }
    }
}