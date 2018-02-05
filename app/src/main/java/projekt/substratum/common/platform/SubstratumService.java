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

import android.content.substratum.ISubstratumService;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import projekt.substratum.platform.SubstratumServiceBridge;

public enum SubstratumService {
    ;
    private static final String TAG = "SubstratumService";
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
}