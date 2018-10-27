/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.services.system;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import com.bumptech.glide.Glide;
import projekt.substratum.Substratum;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.commands.SamsungOverlayCacher;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.common.systems.ProfileManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
    private SharedPreferences prefs = Substratum.getPreferences();

    private static void clearSubstratumCompileFolder(Context context) {
        File deleted = new File(
                EXTERNAL_STORAGE_CACHE);
        FileOperations.delete(context, deleted.getAbsolutePath());
        if (!deleted.exists())
            Substratum.log(TAG,
                    "Successfully cleared the temporary compilation folder on " +
                            "the external storage.");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BOOT_COMPLETED.equals(intent.getAction())) {
            this.context = context;
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
        if (!prefs.contains("samsung_migration_key")) {
            prefs.edit().putInt("samsung_migration_key", Build.VERSION.SDK_INT).apply();
        } else {
            int comparisonValue = prefs.getInt("samsung_migration_key", 0);
            if (comparisonValue < Build.VERSION.SDK_INT) {
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
    class GlideClear extends AsyncTask<Void, Void, Void> {

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