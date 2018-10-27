/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.common.platform;

import projekt.andromeda.IAndromedaInterface;
import projekt.substratum.services.binder.AndromedaBinderService;

import java.util.List;

public class AndromedaService {

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
    public static boolean disableOverlays(List<String> overlays) {
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
    public static void listOverlays() {
        try {
            getAndromedaInterface().listOverlays();
        } catch (Exception ignored) {
        }
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