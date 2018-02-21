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

package projekt.substratum.tabs;

import android.content.Context;

import projekt.substratum.common.platform.SubstratumService;
import projekt.substratum.common.platform.ThemeInterfacerService;

import static projekt.substratum.common.Systems.checkSubstratumService;
import static projekt.substratum.common.Systems.checkThemeInterfacer;

public enum SoundsManager {
    ;

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