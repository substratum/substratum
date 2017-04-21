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

package projekt.substratum.common;

public class Resources {
    // Predetermined list of new Nexus/Pixel Devices
    public static final String[] NEXUS_FILTER = new String[]{
            "angler", // Nexus 6P
            "bullhead", // Nexus 5X
            "flounder", // Nexus 9
            "dragon", // Pixel C
            "marlin", // Pixel
            "sailfish" // Pixel XL
    };

    // Filter to adjust SystemUI elements
    public static final String[] ALLOWED_SYSTEMUI_ELEMENTS = {
            "com.android.systemui.headers",
            "com.android.systemui.navbars",
            "com.android.systemui.statusbars",
            "com.android.systemui.tiles"
    };

    // Filter to adjust Settings elements
    public static final String[] ALLOWED_SETTINGS_ELEMENTS = {
            "com.android.settings.icons",
    };

    // Filter to adjust UI sounds
    public static final String[] ALLOWED_UI_THEMABLE_SOUNDS = {
            "lock_sound",
            "unlock_sound",
            "low_battery_sound"};

    // Legacy Asset Folder Check
    public static final String[] ALLOWED_LEGACY_ASSETS = {
            "overlays",
            "overlays_legacy",
            "sounds"};

    // Do not theme these packages on icon pack studio
    public static final String[] BLACKLIST_STUDIO_TARGET_APPS = {
            "com.keramidas.TitaniumBackup"};
}