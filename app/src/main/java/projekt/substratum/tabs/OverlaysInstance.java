/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.tabs;

import projekt.substratum.adapters.tabs.overlays.OverlaysItem;

import java.util.ArrayList;
import java.util.List;

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