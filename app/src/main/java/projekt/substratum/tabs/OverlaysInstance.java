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

package projekt.substratum.tabs;

import java.util.ArrayList;
import java.util.List;

import projekt.substratum.adapters.tabs.overlays.OverlaysItem;

class OverlaysInstance {

    private static final OverlaysInstance ourInstance = new OverlaysInstance();
    boolean hasFailed;
    Integer failCount;
    StringBuilder failedPackages;
    StringBuilder errorLogs;
    boolean missingType3;
    List<String> finalRunner;
    List<String> lateInstall;
    ArrayList<String> finalCommand;
    List<OverlaysItem> checkedOverlays;
    double currentAmount;
    double totalAmount;
    int overlaysWaiting;

    /**
     * Obtain the current instance
     *
     * @return Returns the current instance
     */
    static OverlaysInstance getInstance() {
        ourInstance.reset();
        return ourInstance;
    }

    /**
     * Resets the singleton instance and its values
     */
    void reset() {
        hasFailed = false;
        failCount = 0;
        failedPackages = new StringBuilder();
        errorLogs = new StringBuilder();
        missingType3 = false;
        finalRunner = new ArrayList<>();
        lateInstall = new ArrayList<>();
        finalCommand = new ArrayList<>();
        checkedOverlays = new ArrayList<>();
        currentAmount = 0;
        overlaysWaiting = 0;
    }
}