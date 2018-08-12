/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.adapters.fragments.settings;

import java.util.List;

public class ValidatorFilter {
    private final String packageName;
    private List<String> filter;

    public ValidatorFilter(String packageName) {
        super();
        this.packageName = packageName;
    }

    public List<String> getFilter() {
        return this.filter;
    }

    public void setFilter(List<String> filter) {
        this.filter = filter;
    }

    public String getPackageName() {
        return this.packageName;
    }
}