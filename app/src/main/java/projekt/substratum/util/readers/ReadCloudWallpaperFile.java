/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.util.readers;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.Map;
import java.util.TreeMap;

public class ReadCloudWallpaperFile {

    /**
     * Function to read the cloud wallpaper file
     *
     * @param location File location
     * @return Return a map for the wallpaper entries
     */
    public static Map read(String location) {
        try {
            File fXmlFile = new File(location);

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();
            NodeList nodeList = doc.getElementsByTagName("wallpaper");

            Map<String, String> map = new TreeMap<>();
            for (int temp = 0; temp < nodeList.getLength(); temp++) {
                Node node = nodeList.item(temp);
                if ((int) node.getNodeType() == (int) Node.ELEMENT_NODE) {
                    Element element = (Element) node;

                    // Replace all spaces with a tilde first, as tilde "~" is lower priority than
                    // "-", we have to put this first.
                    String addonDownloadName = element.getAttribute("id")
                            .replaceAll("\\s+", "~");
                    String addonDownloadLink =
                            element.getElementsByTagName("link").item(0).getTextContent();
                    String addonPreviewLink =
                            element.getElementsByTagName("preview").item(0).getTextContent();

                    String[] finalArray = {
                            addonDownloadName,
                            addonDownloadLink,
                            addonPreviewLink
                    };

                    map.put(finalArray[0], finalArray[1]);
                    map.put(finalArray[0] + "-preview", finalArray[2]);
                }
            }
            return map;
        } catch (Exception ignored) {
            return new TreeMap<String, String>();
        }
    }
}