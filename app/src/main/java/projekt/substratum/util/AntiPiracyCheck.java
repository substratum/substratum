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

package projekt.substratum.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import projekt.substratum.config.FileOperations;
import projekt.substratum.config.References;
import projekt.substratum.config.ThemeManager;

public class AntiPiracyCheck {

    private Context mContext;
    private List<String> installed_themes;
    private ArrayList<String> unauthorized_packages;
    private SharedPreferences prefs;

    public void execute(Context context) {
        this.mContext = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(
                mContext);
        new AntiPiracyChecker().execute("");
    }

    private class AntiPiracyChecker extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
            installed_themes = new ArrayList<>();
            unauthorized_packages = new ArrayList<>();
        }

        @Override
        protected void onPostExecute(String result) {
            if (unauthorized_packages.size() > 0) {
                PurgeUnauthorizedOverlays purgeUnauthorizedOverlays = new
                        PurgeUnauthorizedOverlays();
                purgeUnauthorizedOverlays.execute("");
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            PackageManager packageManager = mContext.getPackageManager();
            List<ApplicationInfo> list = packageManager.getInstalledApplications(PackageManager
                    .GET_META_DATA);

            for (ApplicationInfo packageInfo : list) {
                getSubstratumPackages(mContext, packageInfo.packageName);
            }

            try {
                for (ApplicationInfo packageInfo : list) {
                    checkOverlayIntegrity(mContext, packageInfo.packageName);
                }
            } catch (Exception e) {
                // Exception
            }
            Set<String> installed = new HashSet<>();
            installed.addAll(installed_themes);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putStringSet("installed_themes", installed);
            edit.apply();
            return null;
        }

        private void getSubstratumPackages(Context context, String package_name) {
            // Simulate the Layers Plugin feature by filtering all installed apps and their metadata
            try {
                ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                        package_name, PackageManager.GET_META_DATA);
                if (appInfo.metaData != null) {
                    if (appInfo.metaData.getString(References.metadataName) != null) {
                        if (appInfo.metaData.getString(References.metadataAuthor) != null) {
                            installed_themes.add(package_name);
                        }
                    }
                }
            } catch (Exception e) {
                // Exception
            }
        }

        private void checkOverlayIntegrity(Context context, String package_name) {
            // Check whether all overlay packages installed matches the current device's information
            try {
                ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                        package_name, PackageManager.GET_META_DATA);
                if (appInfo.metaData != null &&
                        appInfo.metaData.getString("Substratum_Device") != null) {
                    String deviceID = appInfo.metaData.getString("Substratum_Device");
                    String actualDeviceID = References.getDeviceID(context);
                    if (deviceID != null && deviceID.equals(actualDeviceID)) {
                        if (appInfo.metaData.getString("Substratum_Parent") != null
                                && !References.isPackageInstalled(context,
                                appInfo.metaData.getString("Substratum_Parent"))) {
                            Log.d("OverlayVerification", package_name + " " +
                                    "unauthorized to be used on this device.");
                            unauthorized_packages.add(package_name);
                        }
                    } else {
                        Log.d("OverlayVerification", package_name + " " +
                                "unauthorized to be used on this device.");
                        unauthorized_packages.add(package_name);
                    }
                }
            } catch (Exception e) {
                // Exception
            }
        }

        private class PurgeUnauthorizedOverlays extends AsyncTask<String, Integer, String> {

            @Override
            protected String doInBackground(String... sUrl) {
                ArrayList<String> final_commands_array = new ArrayList<>();
                if (unauthorized_packages.size() > 0) {
                    for (int i = 0; i < unauthorized_packages.size(); i++) {
                        final_commands_array.add(unauthorized_packages.get(i));
                    }
                    if (References.checkOMS(mContext)) {
                        ThemeManager.uninstallOverlay(mContext, final_commands_array);
                    } else {
                        FileOperations.mountRW();
                        for (int i = 0; i < final_commands_array.size(); i++) {
                            FileOperations.delete(mContext, References.getInstalledDirectory
                                    (mContext,
                                            final_commands_array.get(i)));
                        }
                        FileOperations.mountRO();
                    }
                }
                return null;
            }
        }
    }
}