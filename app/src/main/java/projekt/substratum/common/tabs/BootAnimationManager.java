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
import android.util.Log;

import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeInterfacerService;

import static projekt.substratum.common.References.EXTERNAL_STORAGE_CACHE;
import static projekt.substratum.common.References.checkThemeInterfacer;
import static projekt.substratum.common.References.getDeviceEncryptionStatus;

public class BootAnimationManager {

    public static void setBootAnimation(Context context, String themeDirectory,
                                        Boolean shutdownAnimation) {
        String fileName = (shutdownAnimation ? "shutdownanimation" : "bootanimation");
        String location = EXTERNAL_STORAGE_CACHE + fileName + ".zip";
        // Check to see if device is decrypted with theme interface
        if ((getDeviceEncryptionStatus(context) <= 1 || shutdownAnimation) &&
                checkThemeInterfacer(context)) {
            Log.d("BootAnimationUtils",
                    "No-root option has been enabled with the inclusion of theme interfacer...");
            if (shutdownAnimation) {
                ThemeInterfacerService.setShutdownAnimation(context, location);
            } else {
                ThemeInterfacerService.setBootAnimation(context, location);
            }
        } else {
            // We will mount system, make our directory, copy the bootanimation
            // zip into place, set proper permissions, then unmount
            Log.d("BootAnimationUtils", "Root option has been enabled");
            FileOperations.mountRW();
            FileOperations.mountRWData();
            FileOperations.setPermissions(755, themeDirectory);
            FileOperations.move(context, location, themeDirectory + "/" + fileName + ".zip");
            FileOperations.setPermissions(644, themeDirectory + "/" + fileName + ".zip");
            FileOperations.setContext(themeDirectory);
            FileOperations.mountROData();
            FileOperations.mountRO();
        }
    }

    public static void clearBootAnimation(Context context, Boolean shutdownAnimation) {
        if (getDeviceEncryptionStatus(context) <= 1) {
            // OMS with theme interface
            if (checkThemeInterfacer(context)) {
                if (shutdownAnimation) {
                    ThemeInterfacerService.clearShutdownAnimation(context);
                } else {
                    ThemeInterfacerService.clearBootAnimation(context);
                }
            } else {
                if (shutdownAnimation) {
                    FileOperations.delete(context, "/data/system/theme/shutdownanimation.zip");
                } else {
                    FileOperations.delete(context, "/data/system/theme/bootanimation.zip");
                }
            }
        } else {
            // Encrypted OMS and legacy
            if (!shutdownAnimation) {
                FileOperations.mountRW();
                FileOperations.move(context,
                        "/system/media/bootanimation-backup.zip",
                        "/system/media/bootanimation.zip");
                FileOperations.delete(context, "/system/addon.d/81-subsboot.sh");
                FileOperations.mountRO();
            } else {
                FileOperations.mountRWData();
                FileOperations.delete(context, "/data/system/theme/shutdownanimation.zip");
                FileOperations.mountROData();
            }
        }
    }
}
