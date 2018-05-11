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