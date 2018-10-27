/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.common.platform;

import android.content.substratum.ISubstratumService;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import projekt.substratum.platform.SubstratumServiceBridge;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SubstratumService {

    private static final String TAG = "SubstratumService";
    private static final String[] EXPECTED_METHODS = {
            "installOverlay",
            "uninstallOverlay",
            "switchOverlay",
            "setPriority",
            "restartSystemUI",
            "copy",
            "move",
            "mkdir",
            "deleteDirectory",
            "applyBootanimation",
            "applyFonts",
            "applyProfile",
            "applyShutdownAnimation",
            "getAllOverlays"
    };
    private static final ArrayList<String> expectedMethods = new ArrayList<>(Arrays.asList(EXPECTED_METHODS));
    private static final int uid = Process.myUid() / 100000;
    private static final ISubstratumService service = SubstratumServiceBridge.get();

    static void installOverlay(List<String> paths) {
        try {
            service.installOverlay(paths);
        } catch (Exception e) {
            Log.e(TAG, "There was an exception when trying to install overlay", e);
        }
    }

    public static void uninstallOverlay(List<String> packages, boolean restartUi) {
        try {
            service.uninstallOverlay(packages, restartUi);
        } catch (Exception e) {
            Log.e(TAG, "There was an exception when trying to uninstall overlay", e);
        }
    }

    public static Map getAllOverlays() {
        try {
            return service.getAllOverlays(uid);
        } catch (RemoteException e) {
            Log.e(TAG, "There was an exception when trying to get all overlay", e);
            return null;
        }
    }

    public static boolean setEnabled(String packageName, boolean enable, int userId) {
        try {
            return service.setEnabled(packageName, enable, userId);
        } catch (RemoteException e) {
            Log.e(TAG, "There was an error enabling overlays", e);
            return false;
        }
    }

    static void switchOverlay(List<String> packages, boolean enable, boolean restartUi) {
        try {
            service.switchOverlay(packages, enable, restartUi);
        } catch (Exception e) {
            Log.e(TAG, "There was an exception when trying to switch overlay", e);
        }
    }

    static void setPriority(List<String> packages, boolean restartUi) {
        try {
            service.setPriority(packages, restartUi);
        } catch (Exception e) {
            Log.e(TAG, "There was an exception when trying to set overlay priority", e);
        }
    }

    static void restartSystemUi() {
        try {
            service.restartSystemUI();
        } catch (Exception e) {
            Log.e(TAG, "There was an exception when trying to restart SystemUI", e);
        }
    }

    public static void copy(String source, String destination) {
        try {
            service.copy(source, destination);
        } catch (Exception e) {
            Log.e(TAG, "There was an exception when trying to copy", e);
        }
    }

    public static void move(String source, String destination) {
        try {
            service.move(source, destination);
        } catch (Exception e) {
            Log.e(TAG, "There was an exception when trying to move", e);
        }
    }

    public static void createNewFolder(String destination) {
        try {
            service.mkdir(destination);
        } catch (Exception e) {
            Log.e(TAG, "There was an exception when trying to create new folder", e);
        }
    }

    public static void delete(String destination, boolean deleteParent) {
        try {
            service.deleteDirectory(destination, deleteParent);
        } catch (Exception e) {
            Log.e(TAG, "There was an exception when trying to delete", e);
        }
    }

    public static void setBootAnimation(String location) {
        try {
            service.applyBootanimation(location);
        } catch (Exception e) {
            Log.e(TAG, "There was an exception when trying to set boot animation", e);
        }
    }

    public static void clearBootAnimation() {
        try {
            service.applyBootanimation(null);
        } catch (Exception e) {
            Log.e(TAG, "There was an exception when trying to clear boot animation", e);
        }
    }

    public static void setShutdownAnimation(String location) {
        try {
            service.applyShutdownAnimation(location);
        } catch (Exception e) {
            Log.e(TAG, "There was an exception when trying to set shutdown animation", e);
        }
    }

    public static void clearShutdownAnimation() {
        try {
            service.applyShutdownAnimation(null);
        } catch (Exception e) {
            Log.e(TAG, "There was an exception when trying to clear shutdown animation", e);
        }
    }

    public static void setFonts(String pid, String name) {
        try {
            service.applyFonts(pid, name);
        } catch (Exception e) {
            Log.e(TAG, "There was an exception when trying to set fonts", e);
        }
    }

    public static void clearFonts() {
        try {
            service.applyFonts(null, null);
        } catch (Exception e) {
            Log.e(TAG, "There was an exception when trying to clear fonts", e);
        }
    }

    public static void setSounds(String pid, String name) {
        try {
            service.applySounds(pid, name);
        } catch (Exception e) {
            Log.e(TAG, "There was an exception when trying to set sounds", e);
        }
    }

    public static void clearSounds() {
        try {
            service.applySounds(null, null);
        } catch (Exception e) {
            Log.e(TAG, "There was an exception when trying to clear sounds", e);
        }
    }

    public static void applyProfile(String name, ArrayList<String> toBeDisabled,
                                    ArrayList<String> toBeEnabled, boolean restartUi) {
        try {
            service.applyProfile(toBeEnabled, toBeDisabled, name, restartUi);
        } catch (Exception e) {
            Log.e(TAG, "There was an exception when trying to apply profile", e);
        }
    }

    public static boolean checkApi() {
        try {
            final Method[] methods = ISubstratumService.class.getMethods();
            /*
            if (Systems.IS_PIE)
                expectedMethods.add("setEnabled");
            */
            for (String expectedMethod : expectedMethods) {
                boolean methodFound = false;
                for (Method method : methods) {
                    if (expectedMethod.equals(method.getName())) {
                        methodFound = true;
                        break;
                    }
                }
                if (!methodFound) {
                    Log.wtf(TAG, "Expected method not found: " + expectedMethod);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}