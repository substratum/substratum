/*
 * Copyright (c) 2016-2019 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.util.readers;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class ReadVariantPrioritizedColor {

    /**
     * Function to read the variant's prioritized (themer's FIRST in file) color
     *
     * @param fileName File name
     * @return Returns the first line of the XML file for the color
     */
    public static String read(InputStream fileName) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fileName);
            doc.getDocumentElement().normalize();
            NodeList nodeList = doc.getElementsByTagName("color");
            Node node = nodeList.item(0);
            return node.getAttributes().item(0).getNodeValue();
        } catch (Exception ignored) {
            // At this point, the file does not exist!
        }
        return null;
    }
}