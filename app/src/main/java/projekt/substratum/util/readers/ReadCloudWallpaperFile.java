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

public enum ReadCloudWallpaperFile {
    ;

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