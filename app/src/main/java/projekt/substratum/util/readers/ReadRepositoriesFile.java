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

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import projekt.substratum.adapters.fragments.settings.Repository;
import projekt.substratum.common.References;

public enum ReadRepositoriesFile {
    ;

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
                    Log.d(References.SUBSTRATUM_VALIDATOR,
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