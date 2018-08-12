/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.adapters.fragments.settings;

public class Repository {
    private final String packageName;
    private String bools;
    private String colors;
    private String dimens;
    private String styles;

    public Repository(String packageName) {
        super();
        this.packageName = packageName;
    }

    public String getPackageName() {
        return this.packageName;
    }

    public String getBools() {
        return this.bools;
    }

    public void setBools(String bools) {
        this.bools = bools;
    }

    public String getColors() {
        return this.colors;
    }

    public void setColors(String colors) {
        this.colors = colors;
    }

    public String getDimens() {
        return this.dimens;
    }

    public void setDimens(String dimens) {
        this.dimens = dimens;
    }

    public String getStyles() {
        return this.styles;
    }

    public void setStyles(String styles) {
        this.styles = styles;
    }
}