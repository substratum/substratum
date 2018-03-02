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

import android.os.RemoteException;

import java.util.ArrayList;

import projekt.substratum.IInterfacerInterface;
import projekt.substratum.services.binder.InterfacerBinderService;

public enum ThemeInterfacerService {
    ;

    private static final IInterfacerInterface interfacerInterface =
            InterfacerBinderService.getInstance().getInterfacerInterface();

    /**
     * Install a list of overlays
     *
     * @param overlays List of overlays
     */
    static void installOverlays(ArrayList<String> overlays) {
        try {
            interfacerInterface.installPackage(overlays);
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Uninstall a list of overlays
     *
     * @param overlays List of overlays
     */
    public static void uninstallOverlays(ArrayList<String> overlays) {
        try {
            interfacerInterface.uninstallPackage(overlays, false);
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Enable a list of overlays
     *
     * @param overlays List of overlays
     */
    static void enableOverlays(ArrayList<String> overlays,
                               boolean restartUi) {
        try {
            interfacerInterface.enableOverlay(overlays, restartUi);
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Disable a list of overlays
     *
     * @param overlays  List of overlays
     * @param restartUi Whether to restart SystemUI or not
     */
    static void disableOverlays(ArrayList<String> overlays,
                                boolean restartUi) {
        try {
            interfacerInterface.disableOverlay(overlays, restartUi);
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Restart the SystemUI
     */
    public static void restartSystemUI() {
        try {
            interfacerInterface.restartSystemUI();
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Set a boot animation
     *
     * @param bootanimationLocation Boot animation location
     */
    public static void setBootAnimation(String bootanimationLocation) {
        try {
            interfacerInterface.applyBootanimation(bootanimationLocation);
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Clear the applied boot animation
     */
    public static void clearBootAnimation() {
        try {
            interfacerInterface.applyBootanimation(null);
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Set a font pack
     *
     * @param pid  Package name
     * @param name Name of font
     */
    public static void setFonts(String pid, String name) {
        try {
            interfacerInterface.applyFonts(pid, name);
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Clear applied font pack
     */
    public static void clearFonts() {
        try {
            interfacerInterface.applyFonts(null, null);
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Set a sound pack
     *
     * @param pid  Package name
     * @param name Name of sounds
     */
    public static void setThemedSounds(String pid, String name) {
        try {
            interfacerInterface.applyAudio(pid, name);
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Clear applied sound pack
     */
    public static void clearThemedSounds() {
        try {
            interfacerInterface.applyAudio(null,
                    null);
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Set priority of overlays
     *
     * @param overlays  List of overlays
     * @param restartUi Whether to restart SystemUI or not
     */
    static void setPriority(ArrayList<String> overlays,
                            boolean restartUi) {
        try {
            interfacerInterface.changePriority(overlays, restartUi);
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Copy function
     * <p>
     * This does not copy sensitive information in or out user directories
     *
     * @param source      Source
     * @param destination Destination
     */
    public static void copy(String source,
                            String destination) {
        try {
            interfacerInterface.copy(source, destination);
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Move function
     * <p>
     * This does not move sensitive information in or out user directories
     *
     * @param source      Source
     * @param destination Destination
     */
    public static void move(String source,
                            String destination) {
        try {
            interfacerInterface.move(source, destination);
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Delete function
     * <p>
     * This does not delete sensitive information in any user directories
     *
     * @param directory    Directory
     * @param deleteParent Whether to delete the parent folder (input)
     */
    public static void delete(String directory,
                              boolean deleteParent) {
        try {
            interfacerInterface.deleteDirectory(directory, deleteParent);
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Apply a profile
     *
     * @param name         Name of profile
     * @param toBeDisabled Overlays to be disabled
     * @param toBeEnabled  Overlays to be enabled
     * @param restartUi    Whether to restart SystemUI or not
     */
    public static void applyProfile(String name,
                                    ArrayList<String> toBeDisabled,
                                    ArrayList<String> toBeEnabled,
                                    boolean restartUi) {
        try {
            interfacerInterface.applyProfile(toBeEnabled, toBeDisabled, name, restartUi);
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Create a new folder
     * <p>
     * This does not create folders in any user directories
     *
     * @param destination Destination
     */
    public static void createNewFolder(String destination) {
        try {
            interfacerInterface.mkdir(destination);
        } catch (RemoteException ignored) {
        }
    }
}