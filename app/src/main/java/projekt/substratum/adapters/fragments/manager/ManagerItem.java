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

package projekt.substratum.adapters.fragments.manager;

import android.content.Context;

import java.io.Serializable;

import projekt.substratum.R;

public class ManagerItem implements Serializable {

    private String name;

    private boolean isSelected;

    private int activationValue;

    private Context mContext;

    public ManagerItem(Context context, String name, boolean isActivated) {
        this.mContext = context;
        this.name = name;
        this.isSelected = false;
        this.updateEnabledOverlays(isActivated);
    }

    int getActivationValue() {
        return activationValue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }

    public Context getContext() {
        return mContext;
    }

    public void updateEnabledOverlays(boolean isActivated) {
        this.activationValue =
                ((isActivated) ? mContext.getColor(R.color.overlay_installed_list_entry) :
                        mContext.getColor(R.color.overlay_not_enabled_list_entry));
    }
}