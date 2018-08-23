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
import projekt.substratum.adapters.fragments.settings.ValidatorFilter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ReadFilterFile {

    /**
     * Function to read the cloud validator filter file
     *
     * @param location File location
     * @return Return a map for the validator filter entries
     */
    public static List<ValidatorFilter> read(String location) {
        try {
            File fXmlFile = new File(location);

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();
            NodeList nodeList = doc.getElementsByTagName("repo");

            List<ValidatorFilter> list = new ArrayList<>();
            for (int temp = 0; temp < nodeList.getLength(); temp++) {
                Node node = nodeList.item(temp);
                if ((int) node.getNodeType() == (int) Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    ValidatorFilter current = new ValidatorFilter(
                            element.getAttribute("name"));
                    List<String> filtered = new ArrayList<>();
                    boolean unknown = false;
                    int counter = 1;
                    while (!unknown) {
                        try {
                            String filterObject =
                                    element.getElementsByTagName(
                                            "filter" + counter).item(0).getTextContent();
                            filtered.add(filterObject);
                            counter++;
                        } catch (Exception e) {
                            unknown = true;
                        }
                    }
                    current.setFilter(filtered);
                    list.add(current);
                }
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}