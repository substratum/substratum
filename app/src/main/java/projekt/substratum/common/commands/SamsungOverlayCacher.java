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

package projekt.substratum.common.commands;

import android.content.Context;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import projekt.substratum.common.Packages;

import static projekt.substratum.common.References.EXTERNAL_STORAGE_SAMSUNG_OVERLAY_CACHE;

public class SamsungOverlayCacher {

    private Context context;

    public SamsungOverlayCacher(Context context) {
        this.context = context;
        if (!checkSamsungCacheExists()) {
            createSamsungCache();
        }
    }

    private boolean checkSamsungCacheExists() {
        return new File(EXTERNAL_STORAGE_SAMSUNG_OVERLAY_CACHE).exists();
    }

    private void createSamsungCache() {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            Element rootElement = doc.createElement("overlays");
            doc.appendChild(rootElement);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(
                    new File(EXTERNAL_STORAGE_SAMSUNG_OVERLAY_CACHE));
            transformer.transform(source, result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateSamsungCache(String overlay) {
        if (!getOverlays(false).contains(overlay)) {
            ArrayList<String> overlayList = new ArrayList<>();
            overlayList.add(overlay);
            updateSamsungCache(overlayList);
        }
    }

    private void updateSamsungCache(List<String> overlays) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(new File(EXTERNAL_STORAGE_SAMSUNG_OVERLAY_CACHE));

            NodeList rootList = doc.getElementsByTagName("overlays");
            Node root = rootList.item(0);

            for (String overlay : overlays) {
                Element overlayItem = doc.createElement("overlay");
                overlayItem.setAttribute("package", overlay);
                root.appendChild(overlayItem);
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(
                    new File(EXTERNAL_STORAGE_SAMSUNG_OVERLAY_CACHE));
            transformer.transform(source, result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ArrayList<String> getOverlays(boolean shouldClean) {
        try {
            File fXmlFile = new File(EXTERNAL_STORAGE_SAMSUNG_OVERLAY_CACHE);

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();
            NodeList nodeList = doc.getElementsByTagName("overlay");

            ArrayList<String> returnOverlays = new ArrayList<>();

            for (int temp = 0; temp < nodeList.getLength(); temp++) {
                Node node = nodeList.item(temp);
                Element element = (Element) node;

                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    String overlayPackage = element.getAttribute("package");
                    if (shouldClean) {
                        if (!Packages.isPackageInstalled(context, overlayPackage)) {
                            Node parent = element.getParentNode();
                            parent.removeChild(element);
                        }
                    } else {
                        if (Packages.isPackageInstalled(context, overlayPackage)) {
                            returnOverlays.add(overlayPackage);
                        }
                    }
                }
            }
            return returnOverlays;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }
}