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

import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import projekt.substratum.BuildConfig;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;

import static projekt.substratum.common.References.ENABLE_AOPT_OUTPUT;
import static projekt.substratum.common.References.permissionSamsungOverlay;
import static projekt.substratum.common.Systems.getDeviceID;

public class CompilerCommands {

    public static String createOverlayManifest(Context context,
                                               String overlayPackage,
                                               String parse2_themeName,
                                               String parse2_variantName,
                                               String parse2_baseName,
                                               String versionName,
                                               String targetPackage,
                                               String themeParent,
                                               boolean themeOms,
                                               int legacyPriority,
                                               boolean baseVariantNull,
                                               String type1a,
                                               String type1b,
                                               String type1c,
                                               String type2,
                                               String type3,
                                               String type4,
                                               String packageNameOverride) {

        String packageName;
        if (baseVariantNull) {
            packageName = overlayPackage + "." + parse2_themeName;
        } else {
            packageName = overlayPackage + "." + parse2_themeName +
                    parse2_variantName + parse2_baseName;
        }
        if (isNotNullOrEmpty(packageNameOverride)) {
            packageName = packageNameOverride;
        }

        boolean showOverlayInSamsungSettings =
                Systems.isSamsung(context) && References.toggleShowSamsungOverlayInSettings;
        try {

            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

            // root elements
            Document document = documentBuilder.newDocument();
            Element rootElement = document.createElement("manifest");
            rootElement.setAttribute("xmlns:android",
                    "http://schemas.android.com/apk/res/android");
            rootElement.setAttribute("package", packageName);
            rootElement.setAttribute("android:versionName", versionName);

            Element overlayElement = document.createElement("overlay");
            if (!themeOms)
                overlayElement.setAttribute("android:priority", String.valueOf(legacyPriority));
            overlayElement.setAttribute("android:targetPackage", targetPackage);
            if (showOverlayInSamsungSettings) {
                Element samsungPermissionElement = document.createElement("uses-permission");
                samsungPermissionElement.setAttribute("android:name", permissionSamsungOverlay);
                overlayElement.appendChild(samsungPermissionElement);
            }
            rootElement.appendChild(overlayElement);

            Element applicationElement = document.createElement("application");
            applicationElement.setAttribute("android:label", packageName);
            applicationElement.setAttribute("allowBackup", "false");
            applicationElement.setAttribute("android:hasCode", "false");

            Element metadataOverlayDevice = document.createElement("meta-data");
            metadataOverlayDevice.setAttribute("android:name", References.metadataOverlayDevice);
            metadataOverlayDevice.setAttribute("android:value", getDeviceID(context));
            applicationElement.appendChild(metadataOverlayDevice);

            Element metadataOverlayParent = document.createElement("meta-data");
            metadataOverlayParent.setAttribute("android:name", References.metadataOverlayParent);
            metadataOverlayParent.setAttribute("android:value", themeParent);
            applicationElement.appendChild(metadataOverlayParent);

            Element metadataOverlayTarget = document.createElement("meta-data");
            metadataOverlayTarget.setAttribute("android:name", References.metadataOverlayTarget);
            metadataOverlayTarget.setAttribute("android:value", targetPackage);
            applicationElement.appendChild(metadataOverlayTarget);

            Element metadataOverlayType1a = document.createElement("meta-data");
            metadataOverlayType1a.setAttribute("android:name", References.metadataOverlayType1a);
            metadataOverlayType1a.setAttribute("android:value", type1a);
            applicationElement.appendChild(metadataOverlayType1a);

            Element metadataOverlayType1b = document.createElement("meta-data");
            metadataOverlayType1b.setAttribute("android:name", References.metadataOverlayType1b);
            metadataOverlayType1b.setAttribute("android:value", type1b);
            applicationElement.appendChild(metadataOverlayType1b);

            Element metadataOverlayType1c = document.createElement("meta-data");
            metadataOverlayType1c.setAttribute("android:name", References.metadataOverlayType1c);
            metadataOverlayType1c.setAttribute("android:value", type1c);
            applicationElement.appendChild(metadataOverlayType1c);

            Element metadataOverlayType2 = document.createElement("meta-data");
            metadataOverlayType2.setAttribute("android:name", References.metadataOverlayType2);
            metadataOverlayType2.setAttribute("android:value", type2);
            applicationElement.appendChild(metadataOverlayType2);

            Element metadataOverlayType3 = document.createElement("meta-data");
            metadataOverlayType3.setAttribute("android:name", References.metadataOverlayType3);
            metadataOverlayType3.setAttribute("android:value", type3);
            applicationElement.appendChild(metadataOverlayType3);

            Element metadataOverlayType4 = document.createElement("meta-data");
            metadataOverlayType4.setAttribute("android:name", References.metadataOverlayType4);
            metadataOverlayType4.setAttribute("android:value", type4);
            applicationElement.appendChild(metadataOverlayType4);

            Element metadataOverlayVersion = document.createElement("meta-data");
            metadataOverlayVersion.setAttribute("android:name", References.metadataOverlayVersion);
            metadataOverlayVersion.setAttribute("android:value", String.valueOf(BuildConfig
                    .VERSION_CODE));
            applicationElement.appendChild(metadataOverlayVersion);

            rootElement.appendChild(applicationElement);
            document.appendChild(rootElement);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(document);
            StringWriter outWriter = new StringWriter();
            StreamResult streamResult = new StreamResult(outWriter);
            transformer.transform(domSource, streamResult);

            return outWriter.getBuffer().toString();

        } catch (ParserConfigurationException | TransformerException e) {
            e.printStackTrace();

        }

        return "";
    }

    private static boolean isNotNullOrEmpty(String string) {
        return string != null && string.length() != 0;
    }

    @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
    public static String createAOPTShellCommands(String work_area,
                                                 String targetPkg,
                                                 String overlay_package,
                                                 String theme_name,
                                                 boolean legacySwitch,
                                                 String additional_variant,
                                                 String asset_replacement,
                                                 Context context,
                                                 String dir) {
        StringBuilder sb = new StringBuilder();
        // Initialize the AOPT command
        sb.append(context.getFilesDir().getAbsolutePath() + "/aopt p ");
        // Compile with specified manifest
        sb.append("-M " + work_area + "/AndroidManifest.xml ");
        // If the user picked a variant (type2), compile multiple directories
        sb.append(((isNotNullOrEmpty(additional_variant)) ?
                "-S " + work_area + "/" + "type2_" + additional_variant + "/ " : ""));
        // If the user picked an asset variant (type4), compile multiple directories
        sb.append(((isNotNullOrEmpty(asset_replacement)) ?
                "-A " + work_area + "/assets/ " : ""));
        // We will compile a volatile directory where we make temporary changes to
        sb.append("-S " + work_area + dir + "/ ");
        // Build upon the system's Android framework
        sb.append("-I " + "/system/framework/framework-res.apk ");
        // If running on the AppCompat commits (first run), it will build upon the app too
        sb.append((legacySwitch) ? "" : "-I " + targetPkg + " ");

        // Specify the file output directory
        sb.append("-F " + work_area + "/" + overlay_package + "." +
                theme_name + "-unsigned.apk ");
        // Final arguments to conclude the AOPT build
        if (ENABLE_AOPT_OUTPUT) {
            sb.append("-v ");
        }
        // Allow themers to append new resources
        sb.append("--include-meta-data ");
        sb.append("--auto-add-overlay ");
        // Overwrite all the files in the internal storage
        sb.append("-f ");
        sb.append("\n");

        return sb.toString();
    }

    public static String createZipAlignShellCommands(Context context,
                                                     String source,
                                                     String destination) {
        // Compiler will automatically optimize this with StringBuilder
        String ret;

        // Initialize the ZipAlign command
        ret = context.getFilesDir().getAbsolutePath() + "/zipalign 4 ";
        // Supply the source
        ret += source + " ";
        // Supply the destination
        ret += destination;

        return ret;
    }
}