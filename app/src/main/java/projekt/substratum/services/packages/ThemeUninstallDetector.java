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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import projekt.substratum.MainActivity;
import projekt.substratum.common.Broadcasts;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.tabs.BootAnimationsManager;
import projekt.substratum.tabs.FontsManager;
import projekt.substratum.tabs.SoundsManager;
import projekt.substratum.tabs.WallpapersManager;

import static projekt.substratum.common.Internal.BOOT_ANIMATION_APPLIED;
import static projekt.substratum.common.Internal.SHUTDOWN_ANIMATION_APPLIED;
import static projekt.substratum.common.Internal.SOUNDS_APPLIED;
import static projekt.substratum.common.References.PACKAGE_FULLY_REMOVED;
import static projekt.substratum.common.References.SST_ADDON_PACKAGE;
import static projekt.substratum.common.References.metadataOverlayParent;

public class ThemeUninstallDetector extends BroadcastReceiver {

    private static final String TAG = "ThemeUninstallDetector";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (PACKAGE_FULLY_REMOVED.equals(intent.getAction())) {
            Uri packageName = intent.getData();
            String packageName1;
            if (packageName != null) {
                packageName1 = packageName.toString().substring(8);
            } else {
                return;
            }

            if (packageName1.equals(SST_ADDON_PACKAGE)) {
                SharedPreferences prefs =
                        context.getSharedPreferences("substratum_state", Context.MODE_PRIVATE);
                prefs.edit().clear().apply();
                Broadcasts.sendKillMessage(context);
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if (prefs.contains("installed_themes")) {
                Set installedThemes = prefs.getStringSet("installed_themes", null);
                if ((installedThemes != null) && installedThemes.contains(packageName1)) {
                    Broadcasts.sendRefreshMessage(context);
                    // Get all installed overlays for this package
                    List<String> stateAll = ThemeManager.listAllOverlays(context);

                    ArrayList<String> allOverlays = new ArrayList<>();
                    for (String overlay : stateAll) {
                        try {
                            ApplicationInfo appInfo =
                                    context.getPackageManager().getApplicationInfo(
                                            overlay, PackageManager.GET_META_DATA);
                            if ((appInfo.metaData != null) &&
                                    (appInfo.metaData.getString(metadataOverlayParent) != null)) {
                                String parent =
                                        appInfo.metaData.getString(metadataOverlayParent);
                                if ((parent != null) && parent.equals(packageName1)) {
                                    allOverlays.add(overlay);
                                }
                            }
                        } catch (Exception e) {
                            // NameNotFound
                        }
                    }

                    // Uninstall all overlays for this package
                    ThemeManager.uninstallOverlay(context, allOverlays);

                    SharedPreferences.Editor editor = prefs.edit();
                    if (prefs.getString(SOUNDS_APPLIED, "").equals(packageName1)) {
                        SoundsManager.clearSounds(context);
                        editor.remove(SOUNDS_APPLIED);
                    }
                    if (prefs.getString("fonts_applied", "").equals(packageName1)) {
                        FontsManager.clearFonts(context);
                        editor.remove("fonts_applied");
                    }
                    if (prefs.getString(BOOT_ANIMATION_APPLIED, "").equals(packageName1)) {
                        BootAnimationsManager.clearBootAnimation(context, false);
                        editor.remove(BOOT_ANIMATION_APPLIED);
                    }
                    if (prefs.getString(SHUTDOWN_ANIMATION_APPLIED, "").equals(packageName1)) {
                        BootAnimationsManager.clearBootAnimation(context, true);
                        editor.remove(SHUTDOWN_ANIMATION_APPLIED);
                    }
                    if (prefs.getString("home_wallpaper_applied", "").equals(packageName1)) {
                        try {
                            WallpapersManager.clearWallpaper(context, "home");
                            editor.remove("home_wallpaper_applied");
                        } catch (IOException e) {
                            Log.e(TAG, "Failed to restore home screen wallpaper!");
                        }
                    }
                    if (prefs.getString("lock_wallpaper_applied", "").equals(packageName1)) {
                        try {
                            WallpapersManager.clearWallpaper(context, "lock");
                            editor.remove("lock_wallpaper_applied");
                        } catch (IOException e) {
                            Log.e(TAG, "Failed to restore lock screen wallpaper!");
                        }
                    }
                    if (prefs.getString("app_shortcut_theme", "").equals(packageName1)) {
                        References.clearShortcut(context);
                        editor.remove("app_shortcut_theme");
                    }

                    // Clear off the old preserved list of themes with the new batch
                    Set<String> installed = new TreeSet<>();
                    List<ResolveInfo> themes = Packages.getThemes(context);
                    for (ResolveInfo theme : themes) {
                        installed.add(theme.activityInfo.packageName);
                    }
                    editor.putStringSet("installed_themes", installed);
                    editor.apply();
                }
            }

            new MainActivity.DoCleanUp(context).execute();
        }
    }
}