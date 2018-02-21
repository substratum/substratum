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

public enum FontsManager {
    ;

    /**
     * Set a font pack
     *
     * @param context  Context
     * @param themePid Theme package name
     * @param name     Name of font
     */
    public static void setFonts(
            Context context,
            String themePid,
            String name) {
        if (checkSubstratumService(context)) {
            SubstratumService.setFonts(themePid, name);
        } else if (checkThemeInterfacer(context)) {
            ThemeInterfacerService.setFonts(themePid, name);
        }
    }

    /**
     * Clear an applied font pack
     *
     * @param context Context
     */
    public static void clearFonts(Context context) {
        if (checkSubstratumService(context)) {
            SubstratumService.clearFonts();
        } else if (checkThemeInterfacer(context)) {
            ThemeInterfacerService.clearFonts();
        }
    }
}