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

public class ReadCloudShowcaseFile {

    public static Map main(String argv[]) {

        Map<String, String> map = new TreeMap<>();
        Map<String, String> emptyMap = new TreeMap<>();

        try {
            File fXmlFile = new File(argv[0]);

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("theme");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;

                    String addon_download_name = eElement.getAttribute("id");
                    String addon_download_link = eElement.getElementsByTagName("link").item(0).
                            getTextContent();
                    String addon_author = eElement.getElementsByTagName("author").item(0).
                            getTextContent();
                    String addon_image = "";
                    try {
                        // Try to see if the entry has an image override tag <image>
                        addon_image = eElement.getElementsByTagName("image").item(0).
                                getTextContent();
                    } catch (Exception e) {
                        // There is no image override tag
                    }

                    String addon_backgroundimage = "";
                    try {
                        // Try to see if the entry has an image override tag <backgroundimage>
                        addon_backgroundimage = eElement.getElementsByTagName("backgroundimage")
                                .item(0).
                                        getTextContent();
                    } catch (Exception e) {
                        // There is no image override tag
                    }
                    String addon_pricing = eElement.getElementsByTagName("pricing").item(0).
                            getTextContent();
                    String addon_support = eElement.getElementsByTagName("support").item(0).
                            getTextContent();

                    if (addon_image.length() > 0) {
                        String[] finalArray = {addon_download_name, addon_download_link,
                                addon_author, addon_pricing, addon_image, addon_backgroundimage,
                                addon_support};
                        map.put(finalArray[0], finalArray[1]);
                        map.put(finalArray[0] + "-author", finalArray[2]);
                        map.put(finalArray[0] + "-pricing", finalArray[3]);
                        map.put(finalArray[0] + "-image-override", finalArray[4]);
                        map.put(finalArray[0] + "-support", finalArray[5]);
                    } else {
                        String[] finalArray = {addon_download_name, addon_download_link,
                                addon_author, addon_pricing, addon_support};
                        map.put(finalArray[0], finalArray[1]);
                        map.put(finalArray[0] + "-author", finalArray[2]);
                        map.put(finalArray[0] + "-pricing", finalArray[3]);
                        map.put(finalArray[0] + "-support", finalArray[4]);
                    }
                }
            }
            return map;
        } catch (Exception e) {
            return emptyMap;
        }
    }
}