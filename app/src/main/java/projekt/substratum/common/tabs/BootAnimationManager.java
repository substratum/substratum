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

package projekt.substratum.common.tabs;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import projekt.substratum.common.References;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeInterfacerService;

import static projekt.substratum.common.References.EXTERNAL_STORAGE_CACHE;
import static projekt.substratum.common.References.checkThemeInterfacer;
import static projekt.substratum.common.References.getDeviceEncryptionStatus;

public class BootAnimationManager {

    public static void setBootAnimation(Context context, String themeDirectory) {
        String location = Environment.getExternalStorageDirectory().getAbsolutePath() +
                EXTERNAL_STORAGE_CACHE + "bootanimation.zip";
        // Check to see if device is decrypted with theme interface
        if (getDeviceEncryptionStatus(context) <= 1 && checkThemeInterfacer(context)) {
            Log.d("BootAnimationUtils",
                    "No-root option has been enabled with the inclusion of theme interfacer...");
            ThemeInterfacerService.setBootAnimation(context, location);
            // Otherwise, fall back to rooted operations
        } else {
            // We will mount system, make our directory, copy the bootanimation
            // zip into place, set proper permissions, then unmount
            Log.d("BootAnimationUtils", "Root option has been enabled");
            FileOperations.mountRW();
            FileOperations.mountRWData();
            FileOperations.setPermissions(755, themeDirectory);
            FileOperations.move(context, location, themeDirectory + "/bootanimation.zip");
            FileOperations.setPermissions(644, themeDirectory + "/bootanimation.zip");
            FileOperations.setContext(themeDirectory);
            FileOperations.mountROData();
            FileOperations.mountRO();
        }
    }

    public static void clearBootAnimation(Context context) {
        if (getDeviceEncryptionStatus(context) <= 1 && checkThemeInterfacer(context)) {
            // OMS with theme interface
            ThemeInterfacerService.clearBootAnimation(context);
        } else if (getDeviceEncryptionStatus(context) <= 1 && !References.checkOMS(context)) {
            // Legacy decrypted
            FileOperations.delete(context, "/data/system/theme/bootanimation.zip");
        } else {
            // Encrypted OMS and legacy
            FileOperations.mountRW();
            FileOperations.move(context, "/system/media/bootanimation-backup.zip",
                    "/system/media/bootanimation.zip");
            FileOperations.delete(context, "/system/addon.d/81-subsboot.sh");
        }
    }
}