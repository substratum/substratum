package projekt.substratum.util;

import android.content.Context;

import projekt.substratum.config.References;

import static projekt.substratum.config.References.getDeviceID;

class CommandCompiler {

    static String createOverlayManifest(Context context, String overlay_package,
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
                "        <meta-data android:name=\"Substratum_Parent\" " +
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

    static String createIconOverlayManifest(Context context, String overlay_package,
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

    static String createAOPTShellCommands(String work_area, String targetPkg,
                                          String overlay_package, String theme_name,
                                          boolean legacySwitch, String additional_variant) {

        return "aopt p " +

                // Compile with manifest
                "-M " + work_area + "/AndroidManifest.xml " +

                // If the user picked a variant (type2), compile multiple directories
                ((additional_variant != null) ?
                        "-S " + work_area + "/" + "type2_" + additional_variant + "/ " : "") +

                // We will compile a volatile directory where we make temporary changes to
                "-S " + work_area + "/workdir/ " +

                // Build upon the Android framework
                "-I " + "/system/framework/framework-res.apk " +

                // If running on the AppCompat commits (first run), it will build upon the app too
                ((legacySwitch) ? "" : "-I " + targetPkg + " ") +

                // Specify the file output directory
                "-F " + work_area + "/" + overlay_package + "." + theme_name + "-unsigned.apk " +

                // Final arguments to conclude the AOPT build
                "-f --include-meta-data --auto-add-overlay" +

                ((References.ENABLE_AOPT_OUTPUT) ? " -v" : "") +

                "\n";
    }
}