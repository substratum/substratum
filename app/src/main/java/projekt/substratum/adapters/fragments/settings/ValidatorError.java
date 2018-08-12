/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.adapters.fragments.settings;

import java.util.ArrayList;
import java.util.List;

public class ValidatorError {
    private final List<String> bools = new ArrayList<>();
    private final List<String> colors = new ArrayList<>();
    private final List<String> dimens = new ArrayList<>();
    private final List<String> styles = new ArrayList<>();
    private final String packageName;

    public ValidatorError(String packageName) {
        super();
        this.packageName = packageName;
    }

    public void addBoolError(String bools) {
        this.bools.add(bools);
    }

    public String getPackageName() {
        return this.packageName;
    }

    List<String> getBoolErrors() {
        return this.bools;
    }

    List<String> getColorErrors() {
        return this.colors;
    }

    List<String> getDimenErrors() {
        return this.dimens;
    }

    List<String> getStyleErrors() {
        return this.styles;
    }
}