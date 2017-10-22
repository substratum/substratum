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
import projekt.substratum.common.tabs.BootAnimationManager;
import projekt.substratum.common.tabs.FontManager;
import projekt.substratum.common.tabs.SoundManager;
import projekt.substratum.common.tabs.WallpaperManager;

import static projekt.substratum.common.References.PACKAGE_FULLY_REMOVED;
import static projekt.substratum.common.References.SST_ADDON_PACKAGE;
import static projekt.substratum.common.References.metadataOverlayParent;

public class ThemeUninstallDetector extends BroadcastReceiver {

    private static final String TAG = "ThemeUninstallDetector";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (PACKAGE_FULLY_REMOVED.equals(intent.getAction())) {
            final Uri packageName = intent.getData();
            final String package_name;
            if (packageName != null) {
                package_name = packageName.toString().substring(8);
            } else {
                return;
            }

            if (package_name.equals(SST_ADDON_PACKAGE)) {
                final SharedPreferences prefs =
                        context.getSharedPreferences("substratum_state", Context.MODE_PRIVATE);
                prefs.edit().clear().apply();
                Broadcasts.sendKillMessage(context);
            }

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if (prefs.contains("installed_themes")) {
                final Set installed_themes = prefs.getStringSet("installed_themes", null);
                if (installed_themes != null && installed_themes.contains(package_name)) {
                    Broadcasts.sendRefreshMessage(context);
                    // Get all installed overlays for this package
                    final List<String> stateAll = ThemeManager.listAllOverlays(context);

                    final ArrayList<String> all_overlays = new ArrayList<>();
                    for (int j = 0; j < stateAll.size(); j++) {
                        try {
                            final String current = stateAll.get(j);
                            final ApplicationInfo appInfo =
                                    context.getPackageManager().getApplicationInfo(
                                            current, PackageManager.GET_META_DATA);
                            if (appInfo.metaData != null &&
                                    appInfo.metaData.getString(metadataOverlayParent) != null) {
                                final String parent =
                                        appInfo.metaData.getString(metadataOverlayParent);
                                if (parent != null && parent.equals(package_name)) {
                                    all_overlays.add(current);
                                }
                            }
                        } catch (final Exception e) {
                            // NameNotFound
                        }
                    }

                    // Uninstall all overlays for this package
                    ThemeManager.uninstallOverlay(context, all_overlays);

                    final SharedPreferences.Editor editor = prefs.edit();
                    if (prefs.getString("sounds_applied", "").equals(package_name)) {
                        SoundManager.clearSounds(context);
                        editor.remove("sounds_applied");
                    }
                    if (prefs.getString("fonts_applied", "").equals(package_name)) {
                        FontManager.clearFonts(context);
                        editor.remove("fonts_applied");
                    }
                    if (prefs.getString("bootanimation_applied", "").equals(package_name)) {
                        BootAnimationManager.clearBootAnimation(context, false);
                        editor.remove("bootanimation_applied");
                    }
                    if (prefs.getString("shutdownanimation_applied", "").equals(package_name)) {
                        BootAnimationManager.clearBootAnimation(context, true);
                        editor.remove("shutdownanimation_applied");
                    }
                    if (prefs.getString("home_wallpaper_applied", "").equals(package_name)) {
                        try {
                            WallpaperManager.clearWallpaper(context, "home");
                            editor.remove("home_wallpaper_applied");
                        } catch (final IOException e) {
                            Log.e(TAG, "Failed to restore home screen wallpaper!");
                        }
                    }
                    if (prefs.getString("lock_wallpaper_applied", "").equals(package_name)) {
                        try {
                            WallpaperManager.clearWallpaper(context, "lock");
                            editor.remove("lock_wallpaper_applied");
                        } catch (final IOException e) {
                            Log.e(TAG, "Failed to restore lock screen wallpaper!");
                        }
                    }
                    if (prefs.getString("app_shortcut_theme", "").equals(package_name)) {
                        References.clearShortcut(context);
                        editor.remove("app_shortcut_theme");
                    }

                    // Clear off the old preserved list of themes with the new batch
                    final Set<String> installed = new TreeSet<>();
                    final List<ResolveInfo> all_themes = Packages.getThemes(context);
                    for (int i = 0; i < all_themes.size(); i++) {
                        installed.add(all_themes.get(i).activityInfo.packageName);
                    }
                    editor.putStringSet("installed_themes", installed);
                    editor.apply();
                }
            }

            new MainActivity.DoCleanUp(context).execute();
        }
    }
}