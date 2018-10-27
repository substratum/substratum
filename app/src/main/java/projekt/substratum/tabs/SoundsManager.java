/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.tabs;

import android.content.Context;
import projekt.substratum.common.platform.SubstratumService;
import projekt.substratum.common.platform.ThemeInterfacerService;

import static projekt.substratum.common.Systems.checkSubstratumService;
import static projekt.substratum.common.Systems.checkThemeInterfacer;

public class SoundsManager {

    /**
     * Set sound pack
     *
     * @param context  Context
     * @param themePid Theme package name
     * @param name     Name of sound pack
     * @return Return an array of booleans
     */
    public static boolean[] setSounds(
            Context context,
            String themePid,
            String name) {
        boolean ringtone = false;

        if (checkSubstratumService(context)) {
            SubstratumService.setSounds(themePid, name);
        } else if (checkThemeInterfacer(context)) {
            ThemeInterfacerService.setThemedSounds(themePid, name);
            ringtone = true; // Always assume that the process is succeeded;
        }
        return new boolean[]{false, ringtone};
    }

    /**
     * Clear applied sound pack
     *
     * @param context Context
     */
    public static void clearSounds(Context context) {
        // ATTENTION (to developers):
        //
        // Sounds that aren't cleared (for testing purposes), but removed from the folder
        // are cleared on the next reboot. The way the ContentResolver SQL database works is that it
        // checks the file integrity of _data (file path), and if the file is missing, the database
        // entry is removed.
        if (checkSubstratumService(context)) {
            SubstratumService.clearSounds();
        } else if (checkThemeInterfacer(context)) {
            ThemeInterfacerService.clearThemedSounds();
        }
    }
}