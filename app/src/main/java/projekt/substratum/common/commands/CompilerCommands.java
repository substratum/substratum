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

import projekt.substratum.common.References;

import static projekt.substratum.common.References.ENABLE_AOPT_OUTPUT;
import static projekt.substratum.common.References.getDeviceID;
import static projekt.substratum.common.References.metadataOverlayParent;

public class CompilerCommands {

    public static String createOverlayManifest(Context context, String overlay_package,
                                               String parse2_themeName, String parse2_variantName,
                                               String parse2_baseName, String versionName,
                                               String targetPackage, String theme_parent,
                                               String varianter, Boolean theme_oms,
                                               int legacy_priority, boolean base_variant_null) {
        String package_name;
        if (base_variant_null) {
            package_name = overlay_package + "." + parse2_themeName;
        } else {
            package_name = overlay_package + "." + parse2_themeName +
                    parse2_variantName + parse2_baseName;
        }
        return "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>\n" +

                "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" " +
                "package=\"" + package_name + "\"\n" +

                // Version of this overlay should match the version of the theme
                "        android:versionName=\"" + versionName + "\"> \n" +

                // Begin overlay parameters - include legacy as well (OMS ignored)
                "    <overlay " + ((!theme_oms) ? "android:priority=\"" +
                legacy_priority + "\" " : "") +
                "android:targetPackage=\"" + targetPackage + "\"/>\n" +

                // Our current overlay label is set to be its own package name
                "    <application android:label=\"" + package_name + "\">\n" +

                // Ensure that this overlay was specifically made for this device only
                "        <meta-data android:name=\"Substratum_Device\" " +
                "android:value=\"" + getDeviceID(context) + "\"/>\n" +

                // We can easily track what the overlay parents are without any parsing this way
                "        <meta-data android:name=\"" + metadataOverlayParent + "\" " +
                "android:value=\"" + theme_parent + "\"/>\n" +

                // As we cannot read the overlay tag, we must log our target for this overlay
                "        <meta-data android:name=\"Substratum_Target\" " +
                "android:value=\"" + targetPackage + "\"/>\n" +

                // Check if the overlay is a variant from the theme, if so, log it
                ((!base_variant_null) ?
                        "        <meta-data android:name=\"Substratum_Variant\" " +
                                "android:value=\"" + varianter + "\"/>\n" : "") +

                "    </application>\n" +
                "</manifest>\n";
    }

    public static String createIconOverlayManifest(Context context, String overlay_package,
                                                   String theme_pack, String versionName,
                                                   String parsedIconName, Boolean theme_oms,
                                                   int legacy_priority) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>\n" +

                "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" " +
                "package=\"" + overlay_package + ".icon" + "\"\n" +

                // Version of this overlay should match the version of the theme
                "        android:versionName=\"" + theme_pack + " (" + versionName + "\"> \n" +

                // Begin overlay parameters - include legacy as well (OMS ignored)
                "    <overlay " + ((!theme_oms) ? "android:priority=\"" +
                legacy_priority + "\" " : "") +
                "android:targetPackage=\"" + overlay_package + "\"/>\n" +

                // Our current overlay label is set to be its own package name
                "    <application android:label=\"" + parsedIconName + "\">\n" +

                // Ensure that this overlay was specifically made for this device only
                "        <meta-data android:name=\"Substratum_Device\" " +
                "android:value=\"" + getDeviceID(context) + "\"/>\n" +

                // We can easily track what the icon pack parents are without any parsing this way
                "        <meta-data android:name=\"Substratum_IconPack\" " +
                "android:value=\"" + theme_pack + "\"/>\n" +

                "    </application>\n" +
                "</manifest>\n";
    }

    @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
    public static String createAOPTShellCommands(String work_area, String targetPkg,
                                                 String overlay_package, String theme_name,
                                                 boolean legacySwitch, String additional_variant,
                                                 Context context, String noCacheDir) {
        StringBuilder sb = new StringBuilder();
        // Initialize the AOPT command
        sb.append(context.getFilesDir().getAbsolutePath() + "/aopt p ");
        // Compile with specified manifest
        sb.append("-M " + work_area + "/AndroidManifest.xml ");
        // If the user picked a variant (type2), compile multiple directories
        sb.append(((additional_variant != null) ?
                "-S " + work_area + "/" + "type2_" + additional_variant + "/ " : ""));
        // We will compile a volatile directory where we make temporary changes to
        sb.append("-S " + work_area + (References.isCachingEnabled(context) ?
                "/workdir/ " : noCacheDir + "/ "));
        // Build upon the system's Android framework
        sb.append("-I " + "/system/framework/framework-res.apk ");
        // If running on the AppCompat commits (first run), it will build upon the app too
        sb.append((legacySwitch) ? "" : "-I " + targetPkg + " ");
        // Specify the file output directory
        sb.append("-F " + work_area + "/" + overlay_package + "." + theme_name + "-unsigned.apk ");
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
}