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

package projekt.substratum.common.platform;

import java.util.List;

import projekt.andromeda.IAndromedaInterface;
import projekt.substratum.services.binder.AndromedaBinderService;

public enum AndromedaService {
    ;

    /**
     * Obtain the current binded Andromeda interface
     *
     * @return The interface
     */
    private static IAndromedaInterface getAndromedaInterface() {
        return AndromedaBinderService.getAndromedaInterface();
    }

    /**
     * Check whether the Andromeda server is running
     *
     * @return True, if yes
     */
    public static boolean checkServerActivity() {
        try {
            return getAndromedaInterface().checkServerActivity();
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * Enable a list of overlays
     *
     * @param overlays List of overlays
     * @return True, if succeeded.
     */
    static boolean enableOverlays(List<String> overlays) {
        try {
            return getAndromedaInterface().enableOverlay(overlays);
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * Disable a list of overlays
     *
     * @param overlays List of overlays
     * @return True, if succeeded.
     */
    static boolean disableOverlays(List<String> overlays) {
        try {
            return getAndromedaInterface().disableOverlay(overlays);
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * List all overlays
     *
     * @return True, if succeeded.
     */
    public static boolean listOverlays() {
        try {
            return getAndromedaInterface().listOverlays();
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * Install a list of overlays
     *
     * @param overlays List of overlays
     * @return True, if succeeded.
     */
    static boolean installOverlays(List<String> overlays) {
        try {
            return getAndromedaInterface().installPackage(overlays);
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * Uninstall a list of overlays
     *
     * @param overlays List of overlays
     * @return True, if succeeded.
     */
    static boolean uninstallOverlays(List<String> overlays) {
        try {
            return getAndromedaInterface().uninstallPackage(overlays);
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * Change priority of the overlays
     *
     * @param overlays List of overlays
     * @return True, if succeeded.
     */
    static boolean setPriority(List<String> overlays) {
        try {
            return getAndromedaInterface().changePriority(overlays);
        } catch (Exception ignored) {
        }
        return false;
    }
}