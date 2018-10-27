/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.common.platform;

import android.os.RemoteException;
import projekt.substratum.IInterfacerInterface;
import projekt.substratum.services.binder.InterfacerBinderService;

import java.util.ArrayList;

public class ThemeInterfacerService {

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