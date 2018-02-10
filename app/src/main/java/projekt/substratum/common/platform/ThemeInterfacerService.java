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

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.om.OverlayInfo;
import android.os.RemoteException;
import android.support.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import projekt.substratum.IInterfacerInterface;
import projekt.substratum.common.Systems;
import projekt.substratum.services.binder.InterfacerBinderService;

import static projekt.substratum.common.References.INTERFACER_PACKAGE;
import static projekt.substratum.common.References.INTERFACER_SERVICE;

public enum ThemeInterfacerService {
    ;

    public static final String PRIMARY_COMMAND_KEY = "primary_command_key";
    private static final String INTERFACER_TOKEN = "interfacer_token";
    private static final String JOB_TIME_KEY = "job_time_key";
    private static final String INSTALL_LIST_KEY = "install_list";
    private static final String UNINSTALL_LIST_KEY = "uninstall_list";
    private static final String WITH_RESTART_UI_KEY = "with_restart_ui";
    private static final String BOOTANIMATION_FILE_NAME = "bootanimation_file_name";
    private static final String AUDIO_PID = "audio_pid";
    private static final String AUDIO_FILENAME = "audio_filename";
    private static final String FONTS_FILENAME = "fonts_filename";
    private static final String FONTS_PID = "fonts_pid";
    private static final String ENABLE_LIST_KEY = "enable_list";
    private static final String DISABLE_LIST_KEY = "disable_list";
    private static final String PRIORITY_LIST_KEY = "priority_list";
    private static final String SOURCE_FILE_KEY = "source_file";
    private static final String DESTINATION_FILE_KEY = "destination_file";
    private static final String WITH_DELETE_PARENT_KEY = "with_delete_parent";
    private static final String PROFILE_NAME_KEY = "profile_name";
    private static final String COMMAND_VALUE_INSTALL = "install";
    private static final String COMMAND_VALUE_UNINSTALL = "uninstall";
    private static final String COMMAND_VALUE_RESTART_UI = "restart_ui";
    private static final String COMMAND_VALUE_BOOTANIMATION = "bootanimation";
    private static final String COMMAND_VALUE_FONTS = "fonts";
    private static final String COMMAND_VALUE_AUDIO = "audio";
    private static final String COMMAND_VALUE_ENABLE = "enable";
    private static final String COMMAND_VALUE_DISABLE = "disable";
    private static final String COMMAND_VALUE_PRIORITY = "priority";
    private static final String COMMAND_VALUE_COPY = "copy";
    private static final String COMMAND_VALUE_MOVE = "move";
    private static final String COMMAND_VALUE_DELETE = "delete";
    private static final String COMMAND_VALUE_PROFILE = "profile";
    private static final String COMMAND_VALUE_MKDIR = "mkdir";
    private static final IInterfacerInterface interfacerInterface =
            InterfacerBinderService.getInstance().getInterfacerInterface();

    /**
     * Obtain the Theme Interfacer's correctly built intent
     *
     * @param context Context
     * @return Correctly built Theme Interfacer intent
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    private static Intent getThemeInterfacer(Context context) {
        Intent intent = new Intent();
        intent.setComponent(
                new ComponentName(
                        INTERFACER_PACKAGE,
                        INTERFACER_SERVICE));
        PendingIntent pending = PendingIntent.getActivity(context, 0, new Intent(), 0);
        intent.putExtra(INTERFACER_TOKEN, pending);
        intent.putExtra(JOB_TIME_KEY, System.currentTimeMillis());
        return intent;
    }

    /**
     * Obtain the Theme Interfacer's correctly built intent
     *
     * @param context Context
     * @return Correctly built Theme Interfacer intent
     */
    @Deprecated
    public static Intent getInterfacer(Context context) {
        Intent intent = new Intent();
        PendingIntent pending = PendingIntent.getActivity(context, 0, new Intent(), 0);
        intent.putExtra(INTERFACER_TOKEN, pending);
        intent.putExtra(JOB_TIME_KEY, System.currentTimeMillis());
        return intent;
    }

    /**
     * Install a list of overlays
     *
     * @param context  Context
     * @param overlays List of overlays
     */
    static void installOverlays(Context context,
                                ArrayList<String> overlays) {
        if (Systems.isBinderInterfacer(context)) {
            try {
                interfacerInterface.installPackage(overlays);
            } catch (RemoteException e) {
                // Suppress warning
            }
        } else {
            Intent intent = getThemeInterfacer(context);
            intent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_INSTALL);
            intent.putExtra(INSTALL_LIST_KEY, overlays);
            context.startService(intent);
        }
    }

    /**
     * Uninstall a list of overlays
     *
     * @param context   Context
     * @param overlays  List of overlays
     * @param restartUi Whether to restart SystemUI or not
     */
    @SuppressWarnings("SameParameterValue")
    public static void uninstallOverlays(Context context,
                                         ArrayList<String> overlays,
                                         boolean restartUi) {
        if (Systems.isBinderInterfacer(context)) {
            try {
                interfacerInterface.uninstallPackage(overlays, restartUi);
            } catch (RemoteException e) {
                // Suppress warning
            }
        } else {
            Intent intent = getThemeInterfacer(context);
            intent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_UNINSTALL);
            intent.putExtra(UNINSTALL_LIST_KEY, overlays);
            // Only need to set if true, will restart SystemUI when done processing packages
            intent.putExtra(WITH_RESTART_UI_KEY, restartUi);
            context.startService(intent);
        }
    }

    /**
     * Enable a list of overlays
     *
     * @param context  Context
     * @param overlays List of overlays
     */
    static void enableOverlays(Context context,
                               ArrayList<String> overlays,
                               boolean restartUi) {
        if (Systems.isBinderInterfacer(context)) {
            try {
                interfacerInterface.enableOverlay(overlays, restartUi);
            } catch (RemoteException e) {
                // Suppress warning
            }
        } else {
            Intent intent = getThemeInterfacer(context);
            intent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_ENABLE);
            intent.putExtra(ENABLE_LIST_KEY, overlays);
            intent.putExtra(WITH_RESTART_UI_KEY, restartUi);
            context.startService(intent);
        }
    }

    /**
     * Disable a list of overlays
     *
     * @param context   Context
     * @param overlays  List of overlays
     * @param restartUi Whether to restart SystemUI or not
     */
    static void disableOverlays(Context context,
                                ArrayList<String> overlays,
                                boolean restartUi) {
        if (Systems.isBinderInterfacer(context)) {
            try {
                interfacerInterface.disableOverlay(overlays, restartUi);
            } catch (RemoteException e) {
                // Suppress warning
            }
        } else {
            Intent intent = getThemeInterfacer(context);
            intent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_DISABLE);
            intent.putExtra(DISABLE_LIST_KEY, overlays);
            intent.putExtra(WITH_RESTART_UI_KEY, restartUi);
            context.startService(intent);
        }
    }

    /**
     * Restart the SystemUI
     *
     * @param context Context
     */
    public static void restartSystemUI(Context context) {
        if (Systems.isBinderInterfacer(context)) {
            try {
                interfacerInterface.restartSystemUI();
            } catch (RemoteException e) {
                // Suppress warning
            }
        } else {
            Intent intent = getThemeInterfacer(context);
            intent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_RESTART_UI);
            context.startService(intent);
        }
    }

    /**
     * Set a boot animation
     *
     * @param context                Context
     * @param bootanimation_location Boot animation location
     */
    public static void setBootAnimation(Context context,
                                        String bootanimation_location) {
        if (Systems.isBinderInterfacer(context)) {
            try {
                interfacerInterface.applyBootanimation(bootanimation_location);
            } catch (RemoteException e) {
                // Suppress warning
            }
        } else {
            Intent intent = getThemeInterfacer(context);
            intent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_BOOTANIMATION);
            intent.putExtra(BOOTANIMATION_FILE_NAME, bootanimation_location);
            context.startService(intent);
        }
    }

    /**
     * Clear the applied boot animation
     *
     * @param context Context
     */
    public static void clearBootAnimation(Context context) {
        if (Systems.isBinderInterfacer(context)) {
            try {
                interfacerInterface.applyBootanimation(null);
            } catch (RemoteException e) {
                // Suppress warning
            }
        } else {
            Intent intent = getThemeInterfacer(context);
            intent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_BOOTANIMATION);
            context.startService((intent));
        }
    }

    /**
     * Set a font pack
     *
     * @param context Context
     * @param pid     Package name
     * @param name    Name of font
     */
    public static void setFonts(Context context,
                                String pid,
                                String name) {
        if (Systems.isBinderInterfacer(context)) {
            try {
                interfacerInterface.applyFonts(pid, name);
            } catch (RemoteException e) {
                // Suppress warning
            }
        } else {
            Intent intent = getThemeInterfacer(context);
            intent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_FONTS);
            intent.putExtra(FONTS_FILENAME, name);
            intent.putExtra(FONTS_PID, pid);
            context.startService(intent);
        }
    }

    /**
     * Clear applied font pack
     *
     * @param context Context
     */
    public static void clearFonts(Context context) {
        if (Systems.isBinderInterfacer(context)) {
            try {
                interfacerInterface.applyFonts(null, null);
            } catch (RemoteException e) {
                // Suppress warning
            }
        } else {
            Intent intent = getThemeInterfacer(context);
            intent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_FONTS);
            context.startService(intent);
        }
    }

    /**
     * Set a sound pack
     *
     * @param context Context
     * @param pid     Package name
     * @param name    Name of sounds
     */
    public static void setThemedSounds(Context context,
                                       String pid,
                                       String name) {
        if (Systems.isBinderInterfacer(context)) {
            try {
                interfacerInterface.applyAudio(pid, name);
            } catch (RemoteException e) {
                // Suppress warning
            }
        } else {
            Intent intent = getThemeInterfacer(context);
            intent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_AUDIO);
            intent.putExtra(AUDIO_PID, pid);
            intent.putExtra(AUDIO_FILENAME, name);
            context.startService(intent);
        }
    }

    /**
     * Clear applied sound pack
     *
     * @param context Context
     */
    public static void clearThemedSounds(Context context) {
        if (Systems.isBinderInterfacer(context)) {
            try {
                interfacerInterface.applyAudio(null,
                        null);
            } catch (RemoteException e) {
                // Suppress warning
            }
        } else {
            Intent intent = getThemeInterfacer(context);
            intent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_AUDIO);
            context.startService(intent);
        }
    }

    /**
     * Set priority of overlays
     *
     * @param context   Context
     * @param overlays  List of overlays
     * @param restartUi Whether to restart SystemUI or not
     */
    static void setPriority(Context context,
                            ArrayList<String> overlays,
                            boolean restartUi) {
        if (Systems.isBinderInterfacer(context)) {
            try {
                interfacerInterface.changePriority(overlays, restartUi);
            } catch (RemoteException e) {
                // Suppress warning
            }
        } else {
            Intent intent = getThemeInterfacer(context);
            intent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_PRIORITY);
            intent.putExtra(PRIORITY_LIST_KEY, overlays);
            intent.putExtra(WITH_RESTART_UI_KEY, restartUi);
            context.startService(intent);
        }
    }

    /**
     * Copy function
     * <p>
     * This does not copy sensitive information in or out user directories
     *
     * @param context     Context
     * @param source      Source
     * @param destination Destination
     */
    public static void copy(Context context,
                            String source,
                            String destination) {
        if (Systems.isBinderInterfacer(context)) {
            try {
                interfacerInterface.copy(source, destination);
            } catch (RemoteException e) {
                // Suppress warning
            }
        } else {
            Intent intent = getThemeInterfacer(context);
            intent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_COPY);
            intent.putExtra(SOURCE_FILE_KEY, source);
            intent.putExtra(DESTINATION_FILE_KEY, destination);
            context.startService(intent);
        }
    }

    /**
     * Move function
     * <p>
     * This does not move sensitive information in or out user directories
     *
     * @param context     Context
     * @param source      Source
     * @param destination Destination
     */
    public static void move(Context context,
                            String source,
                            String destination) {
        if (Systems.isBinderInterfacer(context)) {
            try {
                interfacerInterface.move(source, destination);
            } catch (RemoteException e) {
                // Suppress warning
            }
        } else {
            Intent intent = getThemeInterfacer(context);
            intent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_MOVE);
            intent.putExtra(SOURCE_FILE_KEY, source);
            intent.putExtra(DESTINATION_FILE_KEY, destination);
            context.startService(intent);
        }
    }

    /**
     * Delete function
     * <p>
     * This does not delete sensitive information in any user directories
     *
     * @param context      Context
     * @param directory    Directory
     * @param deleteParent Whether to delete the parent folder (input)
     */
    public static void delete(Context context,
                              String directory,
                              boolean deleteParent) {
        if (Systems.isBinderInterfacer(context)) {
            try {
                interfacerInterface.deleteDirectory(directory, deleteParent);
            } catch (RemoteException e) {
                // Suppress warning
            }
        } else {
            Intent intent = getThemeInterfacer(context);
            intent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_DELETE);
            intent.putExtra(SOURCE_FILE_KEY, directory);
            intent.putExtra(WITH_DELETE_PARENT_KEY, deleteParent);
            context.startService(intent);
        }
    }

    /**
     * Apply a profile
     *
     * @param context      Context
     * @param name         Name of profile
     * @param toBeDisabled Overlays to be disabled
     * @param toBeEnabled  Overlays to be enabled
     * @param restartUi    Whether to restart SystemUI or not
     */
    public static void applyProfile(Context context,
                                    String name,
                                    ArrayList<String> toBeDisabled,
                                    ArrayList<String> toBeEnabled,
                                    boolean restartUi) {
        if (Systems.isBinderInterfacer(context)) {
            try {
                interfacerInterface.applyProfile(toBeEnabled, toBeDisabled, name, restartUi);
            } catch (RemoteException e) {
                // Suppress warning
            }
        } else {
            Intent intent = getThemeInterfacer(context);
            intent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_PROFILE);
            intent.putExtra(PROFILE_NAME_KEY, name);
            intent.putExtra(DISABLE_LIST_KEY, toBeDisabled);
            intent.putExtra(ENABLE_LIST_KEY, toBeEnabled);
            intent.putExtra(WITH_RESTART_UI_KEY, restartUi);
            context.startService(intent);
        }
    }

    /**
     * Create a new folder
     * <p>
     * This does not create folders in any user directories
     *
     * @param context     Context
     * @param destination Destination
     */
    public static void createNewFolder(Context context,
                                       String destination) {
        if (Systems.isBinderInterfacer(context)) {
            try {
                interfacerInterface.mkdir(destination);
            } catch (RemoteException e) {
                // Suppress warning
            }
        } else {
            Intent intent = getThemeInterfacer(context);
            intent.putExtra(PRIMARY_COMMAND_KEY, COMMAND_VALUE_MKDIR);
            intent.putExtra(DESTINATION_FILE_KEY, destination);
            context.startService(intent);
        }
    }

    /**
     * Get all overlays on the device
     *
     * @param context Context
     * @return Returns a map of overlays on the device
     */
    static Map<String, List<OverlayInfo>> getAllOverlays(Context context) {
        if (Systems.isBinderInterfacer(context)) {
            try {
                //noinspection unchecked
                return interfacerInterface.getAllOverlays();
            } catch (RemoteException e) {
                // Suppress warning
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Set shutdown animation
     *
     * @param context                   Context
     * @param shutdownAnimationLocation Location of shutdown animation
     */
    public static void setShutdownAnimation(Context context, String
            shutdownAnimationLocation) {
        if (Systems.isBinderInterfacer(context)) {
            try {
                interfacerInterface.applyShutdownAnimation(shutdownAnimationLocation);
            } catch (RemoteException e) {
                // Suppress warning
            }
        }
    }

    /**
     * Clear applied shutdown animation
     *
     * @param context Context
     */
    public static void clearShutdownAnimation(Context context) {
        if (Systems.isBinderInterfacer(context)) {
            try {
                interfacerInterface.applyShutdownAnimation(null);
            } catch (RemoteException e) {
                // Suppress warning
            }
        }
    }
}