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