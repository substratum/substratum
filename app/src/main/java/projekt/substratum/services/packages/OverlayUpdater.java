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

package projekt.substratum.services.packages;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.util.List;

import projekt.substratum.common.References;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.util.compilers.SubstratumBuilder;

import static projekt.substratum.common.References.PACKAGE_ADDED;
import static projekt.substratum.common.References.SUBSTRATUM_BUILDER_CACHE;
import static projekt.substratum.common.References.metadataOverlayParent;
import static projekt.substratum.common.References.metadataOverlayType1a;
import static projekt.substratum.common.References.metadataOverlayType1b;
import static projekt.substratum.common.References.metadataOverlayType1c;
import static projekt.substratum.common.References.metadataOverlayType2;
import static projekt.substratum.common.References.metadataOverlayType3;

public class OverlayUpdater extends BroadcastReceiver {

    private final static String TAG = "OverlayUpdater";
    private static final String overlaysDir = "overlays";
    private SharedPreferences prefs;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onReceive(Context context, Intent intent) {
        if (PACKAGE_ADDED.equals(intent.getAction())) {
            Uri packageName = intent.getData();
            String package_name = packageName.toString().substring(8);

            // If it is an overlay, stop!
            if (ThemeManager.isOverlay(package_name)) {
                return;
            }

            // We do not support caching mode at this time
            if (References.isCachingEnabled(context)) {
                return;
            }

            // When the package is being updated, continue.
            Boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);

            if (replacing) {
                List<String> installed_overlays = ThemeManager.listOverlaysForTarget(package_name);
                if (installed_overlays.size() > 0) {
                    Log.d(TAG, "'" + package_name +
                            "' was just updated with overlays present, updating...");
                    for (int i = 0; i < installed_overlays.size(); i++) {
                        Log.d(TAG, "Current overlay found in stash: " + installed_overlays.get(i));

                        String theme = References.getOverlayMetadata(context,
                                installed_overlays.get(i), metadataOverlayParent);

                        AssetManager themeAssetManager;
                        Resources themeResources = null;
                        try {
                            themeResources = context.getPackageManager()
                                    .getResourcesForApplication(theme);
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }
                        assert themeResources != null;
                        themeAssetManager = themeResources.getAssets();

                        String type1a = References.getOverlayMetadata(
                                context, installed_overlays.get(i), metadataOverlayType1a);
                        String type1b = References.getOverlayMetadata(
                                context, installed_overlays.get(i), metadataOverlayType1b);
                        String type1c = References.getOverlayMetadata(
                                context, installed_overlays.get(i), metadataOverlayType1c);
                        String type2 = References.getOverlayMetadata(
                                context, installed_overlays.get(i), metadataOverlayType2);
                        String type3 = References.getOverlayMetadata(
                                context, installed_overlays.get(i), metadataOverlayType3);

                        String additional_variant = ((type2 != null && type2.length() > 0) ?
                                type2.split("/")[2].substring(6) : null);
                        String base_variant = ((type3 != null && type3.length() > 0) ?
                                type3.split("/")[2].substring(6) : null);

                        // Prenotions
                        String suffix = ((type3 != null && type3.length() != 0) ?
                                "/" + type3 : "/res");
                        String workingDirectory = context.getCacheDir().getAbsolutePath() +
                                SUBSTRATUM_BUILDER_CACHE.substring(0,
                                        SUBSTRATUM_BUILDER_CACHE.length() - 1);
                        File created = new File(workingDirectory);
                        if (created.exists()) {
                            FileOperations.delete(context, created.getAbsolutePath());
                            FileOperations.createNewFolder(context, created.getAbsolutePath());
                        } else {
                            FileOperations.createNewFolder(context, created.getAbsolutePath());
                        }

                        // Handle the type1s
                        if (type1a != null && type1a.length() > 0) {
                            FileOperations.copyFileOrDir(
                                    themeAssetManager,
                                    type1a,
                                    workingDirectory + suffix + "/values/type1a.xml",
                                    type1a);
                        }
                        if (type1b != null && type1b.length() > 0) {
                            FileOperations.copyFileOrDir(
                                    themeAssetManager,
                                    type1b,
                                    workingDirectory + suffix + "/values/type1b.xml",
                                    type1b);
                        }
                        if (type1c != null && type1c.length() > 0) {
                            FileOperations.copyFileOrDir(
                                    themeAssetManager,
                                    type1c,
                                    workingDirectory + suffix + "/values/type1c.xml",
                                    type1c);
                        }


                        // Handle the resource folder
                        String listDir = overlaysDir + "/" + package_name + suffix;
                        if (!listDir.endsWith("/res")) type3 = listDir;
                        FileOperations.copyFileOrDir(
                                themeAssetManager,
                                listDir,
                                workingDirectory + suffix,
                                listDir);

                        File workDir = new File(context.getCacheDir().getAbsolutePath() +
                                SUBSTRATUM_BUILDER_CACHE);
                        if (!workDir.exists() && !workDir.mkdirs())
                            Log.e(TAG, "Could not make cache directory...");

                        SubstratumBuilder sb = new SubstratumBuilder();
                        sb.beginAction(
                                context,
                                theme,
                                package_name,
                                References.grabPackageName(context, theme),
                                package_name,
                                additional_variant,
                                base_variant,
                                References.grabAppVersion(context, installed_overlays.get(i)),
                                References.checkOMS(context),
                                theme,
                                suffix,
                                type1a,
                                type1b,
                                type1c,
                                type2,
                                type3,
                                installed_overlays.get(i)
                        );
                    }
                }
            }
        }
    }
}