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

    public boolean is_variant_chosen;
    public boolean is_variant_chosen1;
    public boolean is_variant_chosen2;
    public boolean is_variant_chosen3;
    public boolean is_variant_chosen4;
    public boolean is_variant_chosen5;
    public final String versionName;
    final boolean variant_mode;
    private final String theme_name;
    private String name;
    private final String package_name;
    private final VariantAdapter array;
    private final VariantAdapter array2;
    private final VariantAdapter array3;
    private final VariantAdapter array4;
    private final VariantAdapter array5;
    private boolean isSelected;
    private int spinnerSelection;
    private int spinnerSelection2;
    private int spinnerSelection3;
    private int spinnerSelection4;
    private int spinnerSelection5;
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

    public OverlaysItem(final String theme_name,
                        final String name,
                        final String packageName,
                        final boolean isSelected,
                        final VariantAdapter adapter,
                        final VariantAdapter adapter2,
                        final VariantAdapter adapter3,
                        final VariantAdapter adapter4,
                        final VariantAdapter adapter5,
                        final Context context,
                        final String versionName,
                        final String baseResources,
                        final Collection enabledOverlays,
                        final Boolean theme_oms,
                        final View activityView) {
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

    public void setName(final String name) {
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

    public void setSelected(final boolean isSelected) {
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

    void setSelectedVariant(final int position) {
        if (position != 0) {
            this.is_variant_chosen = true;
            this.is_variant_chosen1 = true;
        } else {
            this.is_variant_chosen = false;
            this.is_variant_chosen1 = false;
        }
        this.spinnerSelection = position;
    }

    public void updateEnabledOverlays(final Collection<String> enabledOverlays) {
        this.enabledOverlays = new ArrayList<>();
        this.enabledOverlays.addAll(enabledOverlays);
    }

    int getSelectedVariant2() {
        return this.spinnerSelection2;
    }

    void setSelectedVariant2(final int position) {
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

    void setSelectedVariant3(final int position) {
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

    void setSelectedVariant4(final int position) {
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

    void setSelectedVariant5(final int position) {
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

    void setSelectedVariantName(final String variantName) {
        this.variantSelected = variantName;
    }

    public String getSelectedVariantName2() {
        return this.variantSelected2.replaceAll("\\s+", "");
    }

    void setSelectedVariantName2(final String variantName) {
        this.variantSelected2 = variantName;
    }

    public String getSelectedVariantName3() {
        return this.variantSelected3.replaceAll("\\s+", "");
    }

    void setSelectedVariantName3(final String variantName) {
        this.variantSelected3 = variantName;
    }

    public String getSelectedVariantName4() {
        return this.variantSelected4.replaceAll("\\s+", "");
    }

    void setSelectedVariantName4(final String variantName) {
        this.variantSelected4 = variantName;
    }

    public String getSelectedVariantName5() {
        return this.variantSelected5.replaceAll("\\s+", "");
    }

    void setSelectedVariantName5(final String variantName) {
        this.variantSelected5 = variantName;
    }

    Context getInheritedContext() {
        return this.context;
    }

    boolean compareInstalledOverlay() {
        try {
            final PackageInfo pinfo =
                    this.context.getPackageManager().getPackageInfo(this.getFullOverlayParameters(), 0);
            return pinfo.versionName.equals(this.versionName);
        } catch (final Exception e) {
            // Suppress warning
        }
        return false;
    }

    boolean compareInstalledVariantOverlay(final String varianted) {
        String variant = varianted;
        if (!".".equals(variant.substring(0, 1))) variant = '.' + variant;
        String base = this.baseResources;
        if (!this.baseResources.isEmpty() && !".".equals(this.baseResources.substring(0, 1))) {
            base = '.' + base;
        }
        try {
            final PackageInfo pinfo =
                    this.context.getPackageManager().getPackageInfo(
                            this.package_name + '.' + this.theme_name + variant + base, 0);
            return pinfo.versionName.equals(this.versionName);
        } catch (final Exception e) {
            // Suppress warning
        }
        return false;
    }

    public String getFullOverlayParameters() {
        return this.package_name + '.' + this.theme_name +
                (((this.spinnerSelection == 0 &&
                        this.spinnerSelection2 == 0 &&
                        this.spinnerSelection3 == 0 &&
                        this.spinnerSelection4 == 0) &&
                        this.spinnerSelection5 == 0) ? "" : ".") +
                (((this.spinnerSelection == 0) ? "" : this.getSelectedVariantName()) +
                        ((this.spinnerSelection2 == 0) ? "" : this.getSelectedVariantName2()) +
                        ((this.spinnerSelection3 == 0) ? "" : this.getSelectedVariantName3()) +
                        ((this.spinnerSelection4 == 0) ? "" : this.getSelectedVariantName4()) +
                        ((this.spinnerSelection5 == 0) ? "" : this.getSelectedVariantName5()))
                        .replaceAll("\\s", "").replaceAll("[^a-zA-Z0-9]+", "") +
                ((this.baseResources.isEmpty()) ? "" : '.' + this.baseResources);
    }

    public boolean isOverlayEnabled() {
        final boolean installed = Packages.isPackageInstalled(this.context, this.getFullOverlayParameters());
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