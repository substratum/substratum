/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.adapters.tabs.overlays;

import java.io.Serializable;

public class VariantItem implements Serializable {
    private final String variantName;
    private final boolean forceHidden;
    private String variantHex;
    private int color;

    public VariantItem(String variantName,
                       String variantHex) {
        super();
        this.variantName = variantName;
        if (variantHex == null) {
            this.forceHidden = true;
        } else {
            this.forceHidden = false;
            this.variantHex = variantHex;
        }
    }

    String getVariantName() {
        return this.variantName;
    }

    String getVariantHex() {
        return this.variantHex;
    }

    @Override
    public String toString() {
        return this.variantName;
    }

    public int getColor() {
        return this.color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    boolean isDefaultOption() {
        return this.forceHidden;
    }
}