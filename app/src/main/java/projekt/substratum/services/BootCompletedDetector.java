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

package projekt.substratum.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Environment;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import java.io.File;

import projekt.substratum.config.FileOperations;
import projekt.substratum.config.ProfileManager;
import projekt.substratum.config.References;

import static projekt.substratum.config.ProfileManager.SCHEDULED_PROFILE_ENABLED;

public class BootCompletedDetector extends BroadcastReceiver {

    private static final String APP_CRASHED = "projekt.substratum.APP_CRASHED";
    private static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
    private static final String PACKAGE_ADDED = "android.intent.action.PACKAGE_ADDED";

    private boolean registerBroadcastReceivers(Context context) {
        try {
            IntentFilter intentAppCrashed = new IntentFilter(APP_CRASHED);
            IntentFilter intentPackageAdded = new IntentFilter(PACKAGE_ADDED);
            context.getApplicationContext().registerReceiver(
                    new AppCrashReceiver(), intentAppCrashed);
            context.getApplicationContext().registerReceiver(
                    new PackageModificationDetector(), intentPackageAdded);
            return true;
        } catch (Exception e) {
            // Suppress warning
        }
        return false;
    }

    private boolean clearSubstratumCompileFolder(Context context) {
        File deleted = new File(
                Environment.getExternalStorageDirectory().getAbsolutePath() + "/.substratum");
        FileOperations.delete(context, deleted.getAbsolutePath());
        if (!deleted.exists())
            Log.d(References.SUBSTRATUM_LOG,
                    "Successfully cleared the temporary compilation folder on " +
                            "the external storage.");
        return !deleted.exists();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BOOT_COMPLETED.equals(intent.getAction())) {
            boolean registered = registerBroadcastReceivers(context);
            if (registered) {
                Log.d(this.getClass().getSimpleName(),
                        "Successfully registered broadcast receivers " +
                                "for Substratum functionality!");
            } else {
                Log.e(this.getClass().getSimpleName(),
                        "Failed to register broadcast receivers for Substratum functionality...");
            }
            clearSubstratumCompileFolder(context);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if (prefs.getBoolean(SCHEDULED_PROFILE_ENABLED, false)) {
                ProfileManager.updateScheduledProfile(context);
            }
        }
    }
}