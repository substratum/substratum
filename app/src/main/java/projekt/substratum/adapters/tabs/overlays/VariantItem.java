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
    private String variant_name;
    private String variant_hex;
    private boolean forceHidden;
    private int color = 0;

    public VariantItem(String variant_name, String variant_hex) {
        this.variant_name = variant_name;
        if (variant_hex == null) {
            forceHidden = true;
        } else {
            forceHidden = false;
            this.variant_hex = variant_hex;
        }
    }

    String getVariantName() {
        return variant_name;
    }

    String getVariantHex() {
        return variant_hex;
    }

    @Override
    public String toString() {
        return getVariantName();
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
