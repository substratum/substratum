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

import java.util.ArrayList;
import java.util.List;

import projekt.substratum.platform.SubstratumServiceBridge;

public enum SubstratumService {
    ;

    static void installOverlay(List<String> paths) {
        try {
            SubstratumServiceBridge.get().installOverlay(paths);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void uninstallOverlay(List<String> packages, boolean restartUi) {
        try {
            SubstratumServiceBridge.get().uninstallOverlay(packages, restartUi);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void switchOverlay(List<String> packages, boolean enable, boolean restartUi) {
        try {
            SubstratumServiceBridge.get().switchOverlay(packages, enable, restartUi);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void setPriority(List<String> packages, boolean restartUi) {
        try {
            SubstratumServiceBridge.get().setPriority(packages, restartUi);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void restartSystemUi() {
        try {
            SubstratumServiceBridge.get().restartSystemUI();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void copy(String source, String destination) {
        try {
            SubstratumServiceBridge.get().copy(source, destination);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void move(String source, String destination) {
        try {
            SubstratumServiceBridge.get().move(source, destination);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void createNewFolder(String destination) {
        try {
            SubstratumServiceBridge.get().mkdir(destination);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void delete(String destination, boolean deleteParent) {
        try {
            SubstratumServiceBridge.get().deleteDirectory(destination, deleteParent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setBootAnimation(String location) {
        try {
            SubstratumServiceBridge.get().applyBootanimation(location);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void clearBootAnimation() {
        try {
            SubstratumServiceBridge.get().applyBootanimation(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setShutdownAnimation(String location) {
        try {
            SubstratumServiceBridge.get().applyShutdownAnimation(location);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void clearShutdownAnimation() {
        try {
            SubstratumServiceBridge.get().applyShutdownAnimation(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setFonts(String pid, String name) {
        try {
            SubstratumServiceBridge.get().applyFonts(pid, name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void clearFonts() {
        try {
            SubstratumServiceBridge.get().applyFonts(null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setSounds(String pid, String name) {
        try {
            SubstratumServiceBridge.get().applySounds(pid, name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void clearSounds() {
        try {
            SubstratumServiceBridge.get().applySounds(null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void applyProfile(String name, ArrayList<String> toBeDisabled,
                                    ArrayList<String> toBeEnabled, boolean restartUi) {
        try {
            SubstratumServiceBridge.get().applyProfile(toBeEnabled, toBeDisabled, name, restartUi);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}