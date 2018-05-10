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

package projekt.substratum.services.system;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.commands.SamsungOverlayCacher;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.common.systems.ProfileManager;

import static projekt.substratum.common.References.BOOT_COMPLETED;
import static projekt.substratum.common.References.EXTERNAL_STORAGE_CACHE;
import static projekt.substratum.common.References.SUBSTRATUM_PACKAGE;
import static projekt.substratum.common.Resources.FRAMEWORK;
import static projekt.substratum.common.Resources.SAMSUNG_FRAMEWORK;
import static projekt.substratum.common.Resources.SYSTEMUI;
import static projekt.substratum.common.systems.ProfileManager.SCHEDULED_PROFILE_ENABLED;

public class BootCompletedDetector extends BroadcastReceiver {

    private static final String TAG = "SubstratumBoot";
    private Context context;

    private static void clearSubstratumCompileFolder(Context context) {
        File deleted = new File(
                EXTERNAL_STORAGE_CACHE);
        FileOperations.delete(context, deleted.getAbsolutePath());
        if (!deleted.exists())
            Log.d(TAG,
                    "Successfully cleared the temporary compilation folder on " +
                            "the external storage.");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BOOT_COMPLETED.equals(intent.getAction())) {
            this.context = context;
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if (prefs.getBoolean(SCHEDULED_PROFILE_ENABLED, false)) {
                ProfileManager.updateScheduledProfile(context);
            }
            BootCompletedDetector.clearSubstratumCompileFolder(context);
            new GlideClear().execute();
            new References.Markdown(context);
            if (Systems.isSamsungDevice(context) || Systems.isNewSamsungDevice()) {
                checkSamsungMigration();
            }
            if (Systems.isNewSamsungDevice() || Systems.isNewSamsungDeviceAndromeda(context)) {
                SamsungOverlayCacher samsungOverlayCacher = new SamsungOverlayCacher(context);
                samsungOverlayCacher.getOverlays(true);
            }
        }
    }

    /**
     * A helper function that allows for Samsung devices to survive the upgrade of Nougat to Oreo
     */
    private void checkSamsungMigration() {
        SharedPreferences prefs =
                android.preference.PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.contains("samsung_migration_key")) {
            prefs.edit().putInt("samsung_migration_key", Build.VERSION.SDK_INT).apply();
        } else {
            int comparison_value = prefs.getInt("samsung_migration_key", 0);
            if (comparison_value < Build.VERSION.SDK_INT) {
                List<String> android = ThemeManager.listOverlaysForTarget(context, FRAMEWORK);
                List<String> fwk = ThemeManager.listOverlaysForTarget(context, SAMSUNG_FRAMEWORK);
                List<String> systemui = ThemeManager.listOverlaysForTarget(context, SYSTEMUI);
                List<String> subs = ThemeManager.listOverlaysForTarget(context, SUBSTRATUM_PACKAGE);
                ArrayList<String> overlaysToBeUninstalled = new ArrayList<>();
                overlaysToBeUninstalled.addAll(android);
                overlaysToBeUninstalled.addAll(fwk);
                overlaysToBeUninstalled.addAll(systemui);
                overlaysToBeUninstalled.addAll(subs);
                ThemeManager.uninstallOverlay(context, overlaysToBeUninstalled);
                prefs.edit().putInt("samsung_migration_key", Build.VERSION.SDK_INT).apply();
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    public class GlideClear extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            Glide.get(context).clearMemory();
        }

        @Override
        protected Void doInBackground(Void... sUrl) {
            Glide.get(context).clearDiskCache();
            return null;
        }
    }
}