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

public class ReadCloudShowcaseFile {

    /**
     * Function to read the cloud showcase file
     *
     * @param location File location
     * @return Return a map for the showcase entries
     */
    public static Map read(String location) {
        try {
            File fXmlFile = new File(location);

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();
            NodeList nodeList = doc.getElementsByTagName("theme");

            Map<String, String> map = new TreeMap<>();
            for (int temp = 0; temp < nodeList.getLength(); temp++) {
                Node node = nodeList.item(temp);
                if ((int) node.getNodeType() == (int) Node.ELEMENT_NODE) {
                    Element element = (Element) node;

                    String addonDownloadName = element.getAttribute("id");

                    String addonAuthor = element.getElementsByTagName("author").item(0).
                            getTextContent();

                    String addonBackgroundimage = "";
                    try {
                        // Try to see if the entry has an image override tag <backgroundimage>
                        addonBackgroundimage = element.getElementsByTagName("backgroundimage")
                                .item(0).getTextContent();
                    } catch (Exception ignored) {
                        // There is no image override tag
                    }

                    String addonPackageName = "";
                    try {
                        // Try to see if the entry has an image override tag <backgroundimage>
                        addonPackageName = element.getElementsByTagName("package")
                                .item(0).getTextContent();
                    } catch (Exception ignored) {
                        // There is no image override tag
                    }

                    String addonPricing = element.getElementsByTagName("pricing").item(0)
                            .getTextContent();

                    if (addonBackgroundimage.isEmpty()) {
                        String[] finalArray = {
                                addonDownloadName,
                                addonAuthor,
                                addonPricing,
                                addonPackageName
                        };
                        map.put(finalArray[0], finalArray[1]);
                        map.put(finalArray[0] + "-author", finalArray[1]);
                        map.put(finalArray[0] + "-pricing", finalArray[2]);
                        map.put(finalArray[0] + "-package-name", finalArray[3]);
                    } else {
                        String[] finalArray = {
                                addonDownloadName,
                                addonAuthor,
                                addonPricing,
                                addonBackgroundimage,
                                addonPackageName
                        };
                        map.put(finalArray[0], finalArray[1]);
                        map.put(finalArray[0] + "-author", finalArray[1]);
                        map.put(finalArray[0] + "-pricing", finalArray[2]);
                        map.put(finalArray[0] + "-feature-image", finalArray[3]);
                        map.put(finalArray[0] + "-package-name", finalArray[4]);
                    }
                }
            }
            return map;
        } catch (Exception ignored) {
            return new TreeMap<String, String>();
        }
    }
}