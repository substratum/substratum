/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.tabs;

import android.content.Context;
import projekt.substratum.Substratum;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.SubstratumService;
import projekt.substratum.common.platform.ThemeInterfacerService;

import static projekt.substratum.common.Internal.BOOTANIMATION;
import static projekt.substratum.common.Internal.BOOTANIMATION_BU_LOCATION;
import static projekt.substratum.common.Internal.BOOTANIMATION_LOCATION;
import static projekt.substratum.common.Internal.BOOT_ANIMATION;
import static projekt.substratum.common.Internal.SHUTDOWNANIMATION;
import static projekt.substratum.common.Internal.SHUTDOWN_ANIMATION;
import static projekt.substratum.common.Internal.SUBSBOOT_ADDON;
import static projekt.substratum.common.Internal.THEME_644;
import static projekt.substratum.common.Internal.THEME_755;
import static projekt.substratum.common.Internal.THEME_DIRECTORY;
import static projekt.substratum.common.References.EXTERNAL_STORAGE_CACHE;
import static projekt.substratum.common.Systems.checkSubstratumService;
import static projekt.substratum.common.Systems.checkThemeInterfacer;
import static projekt.substratum.common.Systems.getDeviceEncryptionStatus;

public class BootAnimationsManager {

    private static final String TAG = "BootAnimationsManager";

    /**
     * Set a boot animation
     *
     * @param context           Context
     * @param themeDirectory    Theme directory
     * @param shutdownAnimation Whether to apply it as a shutdown animation or not
     */
    public static void setBootAnimation(Context context,
                                        String themeDirectory,
                                        boolean shutdownAnimation) {
        String fileName = (shutdownAnimation ? SHUTDOWN_ANIMATION : BOOT_ANIMATION);
        String location = EXTERNAL_STORAGE_CACHE + fileName + ".zip";
        // Check to see if device is decrypted with theme interface
        if (getDeviceEncryptionStatus(context) <= 1) {
            if (checkSubstratumService(context)) {
                Substratum.log("BootAnimationUtils",
                        "No-root option has been enabled with the inclusion of substratum service...");
                if (shutdownAnimation) {
                    SubstratumService.setShutdownAnimation(location);
                } else {
                    SubstratumService.setBootAnimation(location);
                }
            } else if (checkThemeInterfacer(context)) {
                Substratum.log(TAG, "No-root option has been enabled with the inclusion of theme interfacer...");
                ThemeInterfacerService.setBootAnimation(location);
            }
        } else {
            // We will mount system, make our directory, copy the bootanimation
            // zip into place, set proper permissions, then unmount
            Substratum.log(TAG, "Root option has been enabled");
            FileOperations.mountSystemRW();
            FileOperations.mountRWData();
            FileOperations.setPermissions(THEME_755, themeDirectory);
            FileOperations.move(context, location, themeDirectory + '/' + fileName + ".zip");
            FileOperations.setPermissions(THEME_644, themeDirectory + '/' + fileName + ".zip");
            FileOperations.setSystemFileContext(themeDirectory);
            FileOperations.mountROData();
            FileOperations.mountSystemRO();
        }
    }

    /**
     * Clear an applied boot animation
     *
     * @param context           Context
     * @param shutdownAnimation Whether to clear the shutdown animation or not
     */
    public static void clearBootAnimation(Context context,
                                          boolean shutdownAnimation) {
        // Shutdown animation is working on encrypted devices
        if (shutdownAnimation || getDeviceEncryptionStatus(context) <= 1) {
            // OMS with theme interface
            if (checkSubstratumService(context)) {
                if (shutdownAnimation) {
                    SubstratumService.clearShutdownAnimation();
                } else {
                    SubstratumService.clearBootAnimation();
                }
            } else if (checkThemeInterfacer(context)) {
                ThemeInterfacerService.clearBootAnimation();
            } else {
                if (shutdownAnimation) {
                    FileOperations.delete(context, THEME_DIRECTORY + SHUTDOWNANIMATION);
                } else {
                    FileOperations.delete(context, THEME_DIRECTORY + BOOTANIMATION);
                }
            }
        } else {
            // Encrypted OMS and legacy
            FileOperations.mountSystemRW();
            FileOperations.move(context,
                    BOOTANIMATION_BU_LOCATION,
                    BOOTANIMATION_LOCATION);
            FileOperations.delete(context, SUBSBOOT_ADDON);
            FileOperations.mountSystemRO();
        }
    }
}