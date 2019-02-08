/*
 * Copyright (c) 2016-2019 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.common;

import android.os.Environment;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public final class MigrationsHandler {

    private static final String EXTERNAL_STORAGE_SAMSUNG_OVERLAY_CACHE =
            Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/.samsung_overlays.xml";

    public static ArrayList<String> getAllCachedSamsungOverlays() {
        final ArrayList<String> overlays = new ArrayList<>();
        final File fXmlFile = new File(EXTERNAL_STORAGE_SAMSUNG_OVERLAY_CACHE);
        if (!fXmlFile.exists())
            return overlays;
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();
            NodeList nodeList = doc.getElementsByTagName("overlay");

            for (int temp = 0; temp < nodeList.getLength(); temp++) {
                Node node = nodeList.item(temp);
                Element element = (Element) node;
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    overlays.add(element.getAttribute("package"));
                }
            }
            return overlays;
        } catch (Exception ignored) {
        } finally {
            fXmlFile.deleteOnExit();
        }
        return overlays;
    }

}
