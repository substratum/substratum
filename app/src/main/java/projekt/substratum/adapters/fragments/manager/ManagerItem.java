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
import android.graphics.drawable.Drawable;

import java.io.Serializable;

import projekt.substratum.BuildConfig;
import projekt.substratum.R;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;

public class ManagerItem implements Serializable {

    private String name;
    private String type1a;
    private String type1b;
    private String type1c;
    private String type2;
    private String type3;
    private String type4;
    private String themeName;
    private String labelName;
    private boolean isSelected;
    private int activationValue;
    private Context mContext;
    private Drawable mDrawable;
    private Drawable mTargetDrawable;

    public ManagerItem(Context context, String name, boolean isActivated) {
        this.mContext = context;
        this.name = name;
        this.isSelected = false;

        int version = Packages.getOverlaySubstratumVersion(
                context,
                this.name,
                References.metadataOverlayVersion);
        Boolean newUpdate = (version != 0) && BuildConfig.VERSION_CODE >= version;
        String metadata = Packages.getOverlayMetadata(
                context,
                this.name,
                References.metadataOverlayParent);
        if (metadata != null && metadata.length() > 0 && newUpdate) {
            this.themeName = "<b>" + context.getString(R.string.manager_theme_name) + "</b> " +
                    Packages.getPackageName(context, metadata);
        } else {
            this.themeName = "";
        }
        this.updateEnabledOverlays(isActivated);
        setLabelName(context);
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

    public String getType1a() {
        return type1a;
    }

    public void setType1a(String name) {
        this.type1a = name;
    }

    public String getType1b() {
        return type1b;
    }

    public void setType1b(String name) {
        this.type1b = name;
    }

    public String getType1c() {
        return type1c;
    }

    public void setType1c(String name) {
        this.type1c = name;
    }

    public String getType2() {
        return type2;
    }

    public void setType2(String name) {
        this.type2 = name;
    }

    public String getType3() {
        return type3;
    }

    public void setType3(String name) {
        this.type3 = name;
    }

    public String getType4() {
        return type4;
    }

    public void setType4(String name) {
        this.type4 = name;
    }

    public String getThemeName() {
        if (themeName == null) {
            themeName = getContext().getString(R.string.reboot_awaiting_manager_title);
        }
        return themeName;
    }

    public void setThemeName(String name) {
        this.themeName = name;
    }

    public String getLabelName() {
        if (labelName == null) {
            labelName = getContext().getString(R.string.reboot_awaiting_manager_title);
        }
        return labelName;
    }

    private void setLabelName(Context context) {
        String packageName = getName();
        String targetPackage = Packages.getOverlayTarget(context, packageName);
        if (packageName.startsWith("com.android.systemui.headers")) {
            labelName = context.getString(R.string.systemui_headers);
        } else if (packageName.startsWith("com.android.systemui.navbars")) {
            labelName = context.getString(R.string.systemui_navigation);
        } else if (packageName.startsWith("com.android.systemui.statusbars")) {
            labelName = context.getString(R.string.systemui_statusbar);
        } else if (packageName.startsWith("com.android.systemui.tiles")) {
            labelName = context.getString(R.string.systemui_qs_tiles);
        } else if (packageName.startsWith("com.android.settings.icons")) {
            labelName = context.getString(R.string.settings_icons);
        } else {
            labelName = Packages.getPackageName(context, targetPackage);
        }
    }

    public Drawable getDrawable() {
        return mDrawable;
    }

    public void setDrawable(Drawable drawable) {
        this.mDrawable = drawable;
    }

    Drawable getTargetDrawable() {
        return mTargetDrawable;
    }

    void setTargetDrawable(Drawable drawable) {
        this.mTargetDrawable = drawable;
    }
}