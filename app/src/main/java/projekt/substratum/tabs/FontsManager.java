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

public class FontsManager {

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