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

import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.SpinnerAdapter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import projekt.substratum.common.Packages;
import projekt.substratum.common.Systems;
import projekt.substratum.common.platform.ThemeManager;

public class OverlaysItem implements Serializable {

    public boolean is_variant_chosen = false;
    public boolean is_variant_chosen1 = false;
    public boolean is_variant_chosen2 = false;
    public boolean is_variant_chosen3 = false;
    public boolean is_variant_chosen4 = false;
    public boolean is_variant_chosen5 = false;
    public String versionName;
    boolean variant_mode = false;
    private final String theme_name;
    private String name;
    private final String package_name;
    private final VariantAdapter array;
    private final VariantAdapter array2;
    private final VariantAdapter array3;
    private final VariantAdapter array4;
    private final VariantAdapter array5;
    private boolean isSelected;
    private int spinnerSelection = 0;
    private int spinnerSelection2 = 0;
    private int spinnerSelection3 = 0;
    private int spinnerSelection4 = 0;
    private int spinnerSelection5 = 0;
    private String variantSelected = "";
    private String variantSelected2 = "";
    private String variantSelected3 = "";
    private String variantSelected4 = "";
    private String variantSelected5 = "";
    private String baseResources = "";
    private final Context context;
    private List<Object> enabledOverlays;
    private final Drawable app_icon;
    private final Boolean theme_oms;
    private final View activityView;

    public OverlaysItem(String theme_name,
                        String name,
                        String packageName,
                        boolean isSelected,
                        VariantAdapter adapter,
                        VariantAdapter adapter2,
                        VariantAdapter adapter3,
                        VariantAdapter adapter4,
                        VariantAdapter adapter5,
                        Context context,
                        String versionName,
                        String baseResources,
                        Collection enabledOverlays,
                        Boolean theme_oms,
                        View activityView) {
        super();

        this.theme_name = theme_name;
        this.name = name;
        this.package_name = packageName;
        this.isSelected = isSelected;
        this.array = adapter;
        this.array2 = adapter2;
        this.array3 = adapter3;
        this.array4 = adapter4;
        this.array5 = adapter5;
        this.context = context;
        this.versionName = versionName;
        this.theme_oms = theme_oms;
        if (baseResources != null)
            this.baseResources =
                    baseResources.replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9]+", "");
        this.variant_mode = true;
        this.enabledOverlays = new ArrayList<>();
        this.enabledOverlays.addAll(enabledOverlays);
        this.app_icon = Packages.getAppIcon(context, packageName);
        this.activityView = activityView;
    }

    View getActivityView() {
        return this.activityView;
    }

    boolean isDeviceOMS() {
        return this.theme_oms;
    }

    public String getThemeName() {
        return this.theme_name;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    Drawable getAppIcon() {
        return this.app_icon;
    }

    public String getPackageName() {
        return this.package_name;
    }

    String getBaseResources() {
        return this.baseResources;
    }

    public boolean isSelected() {
        return this.isSelected;
    }

    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }

    SpinnerAdapter getSpinnerArray() {
        return this.array;
    }

    SpinnerAdapter getSpinnerArray2() {
        return this.array2;
    }

    SpinnerAdapter getSpinnerArray3() {
        return this.array3;
    }

    SpinnerAdapter getSpinnerArray4() {
        return this.array4;
    }

    SpinnerAdapter getSpinnerArray5() {
        return this.array5;
    }

    int getSelectedVariant() {
        return this.spinnerSelection;
    }

    void setSelectedVariant(int position) {
        if (position != 0) {
            this.is_variant_chosen = true;
            this.is_variant_chosen1 = true;
        } else {
            this.is_variant_chosen = false;
            this.is_variant_chosen1 = false;
        }
        this.spinnerSelection = position;
    }

    public void updateEnabledOverlays(Collection<String> enabledOverlays) {
        this.enabledOverlays = new ArrayList<>();
        this.enabledOverlays.addAll(enabledOverlays);
    }

    int getSelectedVariant2() {
        return this.spinnerSelection2;
    }

    void setSelectedVariant2(int position) {
        if (position != 0) {
            this.is_variant_chosen = true;
            this.is_variant_chosen2 = true;
        } else {
            this.is_variant_chosen = false;
            this.is_variant_chosen2 = false;
        }
        this.spinnerSelection2 = position;
    }

    int getSelectedVariant3() {
        return this.spinnerSelection3;
    }

    void setSelectedVariant3(int position) {
        if (position != 0) {
            this.is_variant_chosen = true;
            this.is_variant_chosen3 = true;
        } else {
            this.is_variant_chosen = false;
            this.is_variant_chosen3 = false;
        }
        this.spinnerSelection3 = position;
    }

    int getSelectedVariant4() {
        return this.spinnerSelection4;
    }

    void setSelectedVariant4(int position) {
        if (position != 0) {
            this.is_variant_chosen = true;
            this.is_variant_chosen4 = true;
        } else {
            this.is_variant_chosen = false;
            this.is_variant_chosen4 = false;
        }
        this.spinnerSelection4 = position;
    }

    int getSelectedVariant5() {
        return this.spinnerSelection5;
    }

    void setSelectedVariant5(int position) {
        if (position != 0) {
            this.is_variant_chosen = true;
            this.is_variant_chosen5 = true;
        } else {
            this.is_variant_chosen = false;
            this.is_variant_chosen5 = false;
        }
        this.spinnerSelection5 = position;
    }

    public String getSelectedVariantName() {
        return this.variantSelected.replaceAll("\\s+", "");
    }

    void setSelectedVariantName(String variantName) {
        this.variantSelected = variantName;
    }

    public String getSelectedVariantName2() {
        return this.variantSelected2.replaceAll("\\s+", "");
    }

    void setSelectedVariantName2(String variantName) {
        this.variantSelected2 = variantName;
    }

    public String getSelectedVariantName3() {
        return this.variantSelected3.replaceAll("\\s+", "");
    }

    void setSelectedVariantName3(String variantName) {
        this.variantSelected3 = variantName;
    }

    public String getSelectedVariantName4() {
        return this.variantSelected4.replaceAll("\\s+", "");
    }

    void setSelectedVariantName4(String variantName) {
        this.variantSelected4 = variantName;
    }

    public String getSelectedVariantName5() {
        return this.variantSelected5.replaceAll("\\s+", "");
    }

    void setSelectedVariantName5(String variantName) {
        this.variantSelected5 = variantName;
    }

    Context getInheritedContext() {
        return this.context;
    }

    boolean compareInstalledOverlay() {
        try {
            PackageInfo pinfo =
                    this.context.getPackageManager().getPackageInfo(this.getFullOverlayParameters(), 0);
            return pinfo.versionName.equals(this.versionName);
        } catch (Exception e) {
            // Suppress warning
        }
        return false;
    }

    boolean compareInstalledVariantOverlay(String varianted) {
        String variant = varianted;
        if (!".".equals(variant.substring(0, 1))) variant = "." + variant;
        String base = this.baseResources;
        if (this.baseResources.length() > 0 && !".".equals(this.baseResources.substring(0, 1))) {
            base = "." + base;
        }
        try {
            PackageInfo pinfo =
                    this.context.getPackageManager().getPackageInfo(
                            this.getPackageName() + "." + this.theme_name + variant + base, 0);
            return pinfo.versionName.equals(this.versionName);
        } catch (Exception e) {
            // Suppress warning
        }
        return false;
    }

    public String getFullOverlayParameters() {
        return this.getPackageName() + "." + this.getThemeName() +
                (((this.getSelectedVariant() == 0 &&
                        this.getSelectedVariant2() == 0 &&
                        this.getSelectedVariant3() == 0 &&
                        this.getSelectedVariant4() == 0) &&
                        this.getSelectedVariant5() == 0) ? "" : ".") +
                (((this.getSelectedVariant() == 0) ? "" : this.getSelectedVariantName()) +
                        ((this.getSelectedVariant2() == 0) ? "" : this.getSelectedVariantName2()) +
                        ((this.getSelectedVariant3() == 0) ? "" : this.getSelectedVariantName3()) +
                        ((this.getSelectedVariant4() == 0) ? "" : this.getSelectedVariantName4()) +
                        ((this.getSelectedVariant5() == 0) ? "" : this.getSelectedVariantName5()))
                        .replaceAll("\\s", "").replaceAll("[^a-zA-Z0-9]+", "") +
                ((this.baseResources.length() == 0) ? "" : "." + this.baseResources);
    }

    public boolean isOverlayEnabled() {
        boolean installed = Packages.isPackageInstalled(this.context, this.getFullOverlayParameters());
        if (Systems.isSamsungDevice(this.context)) {
            return installed;
        } else {
            return installed && (
                    ThemeManager.isOverlayEnabled(this.context, this.getFullOverlayParameters()) ||
                            this.enabledOverlays.contains(this.getFullOverlayParameters())
            );
        }
    }
}