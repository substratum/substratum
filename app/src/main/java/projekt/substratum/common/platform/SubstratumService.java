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

import java.util.List;

import projekt.substratum.platform.SubstratumServiceBridge;

enum SubstratumService {
    ;

    static void installOverlay(List<String> paths) {
        try {
            SubstratumServiceBridge.get().installOverlay(paths);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    static void uninstallOverlay(List<String> packages, boolean restartUi) {
        try {
            SubstratumServiceBridge.get().uninstallOverlay(packages, restartUi);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    static void switchOverlay(List<String> packages, boolean enable, boolean restartUi) {
        try {
            SubstratumServiceBridge.get().switchOverlay(packages, enable, restartUi);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    static void changePriority(List<String> packages, boolean restartUi) {
        try {
            SubstratumServiceBridge.get().changePriority(packages, restartUi);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}