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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class ReadShowcaseTabsFile {

    /**
     * Function to read the cloud showcase tabs file
     *
     * @param location File location
     * @return Return a map for the showcase tab entries
     */
    public static Map<String, String> read(String location) {
        try {
            File fXmlFile = new File(location);

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();
            NodeList nodeList = doc.getElementsByTagName("tab");

            Map<String, String> map = new LinkedHashMap<>();
            for (int temp = 0; temp < nodeList.getLength(); temp++) {
                Node node = nodeList.item(temp);
                if ((int) node.getNodeType() == (int) Node.ELEMENT_NODE) {
                    Element element = (Element) node;

                    String addonDownloadName = element.getAttribute("id");
                    String addonDownloadLink =
                            element.getElementsByTagName("link").item(0).getTextContent();

                    String[] finalArray = {
                            addonDownloadName,
                            addonDownloadLink
                    };

                    map.put(finalArray[0], finalArray[1]);
                }
            }
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            return new TreeMap<>();
        }
    }
}