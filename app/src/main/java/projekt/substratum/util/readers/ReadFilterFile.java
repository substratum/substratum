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
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import projekt.substratum.adapters.fragments.settings.ValidatorFilter;

public enum ReadFilterFile {
    ;

    public static List<ValidatorFilter> main(final String file) {

        try {
            final File fXmlFile = new File(file);

            final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            final Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();
            final NodeList nList = doc.getElementsByTagName("repo");

            final List<ValidatorFilter> list = new ArrayList<>();
            for (int temp = 0; temp < nList.getLength(); temp++) {
                final Node nNode = nList.item(temp);
                if ((int) nNode.getNodeType() == (int) Node.ELEMENT_NODE) {
                    final Element eElement = (Element) nNode;
                    final ValidatorFilter current = new ValidatorFilter(eElement.getAttribute
                            ("name"));
                    final List<String> filtered = new ArrayList<>();
                    boolean unknown = false;
                    int counter = 1;
                    while (!unknown) {
                        try {
                            final String filterObject = eElement.getElementsByTagName("filter" +
                                    counter)
                                    .item(0).getTextContent();
                            filtered.add(filterObject);
                            counter++;
                        } catch (final Exception e) {
                            unknown = true;
                        }
                    }
                    current.setFilter(filtered);
                    list.add(current);
                }
            }
            return list;
        } catch (final Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}