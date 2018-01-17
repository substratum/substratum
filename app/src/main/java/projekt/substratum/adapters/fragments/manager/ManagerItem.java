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

import static projekt.substratum.common.Resources.LG_FRAMEWORK;
import static projekt.substratum.common.Resources.SAMSUNG_FRAMEWORK;
import static projekt.substratum.common.Resources.SETTINGS_ICONS;
import static projekt.substratum.common.Resources.SYSTEMUI_HEADERS;
import static projekt.substratum.common.Resources.SYSTEMUI_NAVBARS;
import static projekt.substratum.common.Resources.SYSTEMUI_QSTILES;
import static projekt.substratum.common.Resources.SYSTEMUI_STATUSBARS;

public class ManagerItem implements Serializable {

    private final Context context;
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
    private Drawable mDrawable;
    private Drawable mTargetDrawable;

    public ManagerItem(Context context,
                       String name,
                       Boolean isActivated) {
        super();
        this.context = context;
        this.name = name;
        this.isSelected = false;

        int version = Packages.getOverlaySubstratumVersion(
                context,
                this.name);
        Boolean newUpdate = (version != 0) && (version <= BuildConfig.VERSION_CODE);
        String metadata = Packages.getOverlayMetadata(
                context,
                this.name,
                References.metadataOverlayParent);
        if ((metadata != null) && !metadata.isEmpty() && newUpdate) {
            this.themeName = Packages.getPackageName(context, metadata);
        } else {
            this.themeName = "";
        }
        this.updateEnabledOverlays(isActivated);
        this.setLabelName(context);
    }

    int getActivationValue() {
        return this.activationValue;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSelected() {
        return this.isSelected;
    }

    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }

    public Context getContext() {
        return this.context;
    }

    public void updateEnabledOverlays(boolean isActivated) {
        this.activationValue =
                ((isActivated) ? this.context.getColor(R.color.overlay_installed_list_entry) :
                        this.context.getColor(R.color.overlay_not_enabled_list_entry));
    }

    public String getType1a() {
        return this.type1a;
    }

    public void setType1a(String name) {
        this.type1a = name;
    }

    public String getType1b() {
        return this.type1b;
    }

    public void setType1b(String name) {
        this.type1b = name;
    }

    public String getType1c() {
        return this.type1c;
    }

    public void setType1c(String name) {
        this.type1c = name;
    }

    public String getType2() {
        return this.type2;
    }

    public void setType2(String name) {
        this.type2 = name;
    }

    public String getType3() {
        return this.type3;
    }

    public void setType3(String name) {
        this.type3 = name;
    }

    public String getType4() {
        return this.type4;
    }

    public void setType4(String name) {
        this.type4 = name;
    }

    public String getThemeName() {
        if (this.themeName == null) {
            this.themeName = this.context.getString(R.string.reboot_awaiting_manager_title);
        }
        return this.themeName;
    }

    public void setThemeName(String name) {
        this.themeName = name;
    }

    public String getLabelName() {
        if (this.labelName == null) {
            this.labelName = this.context.getString(R.string.reboot_awaiting_manager_title);
        }
        return this.labelName;
    }

    private void setLabelName(Context context) {
        String packageName = this.name;
        String targetPackage = Packages.getOverlayTarget(context, packageName);
        if (packageName.startsWith(SYSTEMUI_HEADERS)) {
            this.labelName = context.getString(R.string.systemui_headers);
        } else if (packageName.startsWith(SYSTEMUI_NAVBARS)) {
            this.labelName = context.getString(R.string.systemui_navigation);
        } else if (packageName.startsWith(SYSTEMUI_STATUSBARS)) {
            this.labelName = context.getString(R.string.systemui_statusbar);
        } else if (packageName.startsWith(SYSTEMUI_QSTILES)) {
            this.labelName = context.getString(R.string.systemui_qs_tiles);
        } else if (packageName.startsWith(SETTINGS_ICONS)) {
            this.labelName = context.getString(R.string.settings_icons);
        } else if (packageName.startsWith(SAMSUNG_FRAMEWORK)) {
            this.labelName = context.getString(R.string.samsung_framework);
        } else if (packageName.startsWith(LG_FRAMEWORK)) {
            this.labelName = context.getString(R.string.lg_framework);
        } else {
            this.labelName = Packages.getPackageName(context, targetPackage);
        }
    }

    public Drawable getDrawable() {
        return this.mDrawable;
    }

    public void setDrawable(Drawable drawable) {
        this.mDrawable = drawable;
    }

    Drawable getTargetDrawable() {
        return this.mTargetDrawable;
    }

    void setTargetDrawable(Drawable drawable) {
        this.mTargetDrawable = drawable;
    }
}