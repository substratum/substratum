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
import java.util.HashMap;
import java.util.Map;

public class ReadSupportedROMsFile {

    /**
     * Function to read the cloud supported ROMs file
     *
     * @param location File location
     * @return Return a map for the supported ROMs list
     */
    public static Map<String, String> read(String location) {
        try {
            File fXmlFile = new File(location);

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();
            NodeList nodeList = doc.getElementsByTagName("rom");

            Map<String, String> hashMap = new HashMap<>();
            for (int temp = 0; temp < nodeList.getLength(); temp++) {
                Node node = nodeList.item(temp);
                if ((int) node.getNodeType() == (int) Node.ELEMENT_NODE) {
                    Element element = (Element) node;

                    String name = element.getAttribute("name");
                    String id = element.getAttribute("id");

                    hashMap.put(id, name);
                }
            }
            return hashMap;
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }
}