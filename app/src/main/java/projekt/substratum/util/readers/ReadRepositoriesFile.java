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
import projekt.substratum.Substratum;
import projekt.substratum.adapters.fragments.settings.Repository;
import projekt.substratum.common.References;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ReadRepositoriesFile {

    /**
     * Function to read the cloud repository file
     *
     * @param location File location
     * @return Return a map for the repository entries
     */
    public static List<Repository> read(String location) {
        try {
            File fXmlFile = new File(location);

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();
            NodeList nodeList = doc.getElementsByTagName("repo");

            List<Repository> list = new ArrayList<>();
            for (int temp = 0; temp < nodeList.getLength(); temp++) {
                Node node = nodeList.item(temp);
                if ((int) node.getNodeType() == (int) Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    Repository current = new Repository(element.getAttribute("name"));
                    Substratum.log(References.SUBSTRATUM_VALIDATOR,
                            "Pulling live resources from '" + current.getPackageName() + "'!");
                    try {
                        String bools =
                                element.getElementsByTagName("bools").item(0).getTextContent();
                        current.setBools(bools);
                    } catch (Exception ignored) {
                    }
                    try {
                        String colors =
                                element.getElementsByTagName("colors").item(0).getTextContent();
                        current.setColors(colors);
                    } catch (Exception ignored) {
                    }
                    try {
                        String dimens =
                                element.getElementsByTagName("dimens").item(0).getTextContent();
                        current.setDimens(dimens);
                    } catch (Exception ignored) {
                    }
                    try {
                        String styles =
                                element.getElementsByTagName("styles").item(0).getTextContent();
                        current.setStyles(styles);
                    } catch (Exception ignored) {
                    }
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