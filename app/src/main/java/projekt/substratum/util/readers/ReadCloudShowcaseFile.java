/*
 * Copyright (c) 2016-2017 Projekt Substratum
 * This file is part of Substratum.
 *
 * Substratum is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Substratum is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Substratum.  If not, see <http://www.gnu.org/licenses/>.
 */

package projekt.substratum.util.readers;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public enum ReadCloudShowcaseFile {
    ;

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