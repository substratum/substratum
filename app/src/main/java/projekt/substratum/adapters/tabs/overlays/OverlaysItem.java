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
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class OverlaysItem implements Serializable {

    public boolean is_variant_chosen = false;
    public boolean is_variant_chosen1 = false;
    public boolean is_variant_chosen2 = false;
    public boolean is_variant_chosen3 = false;
    public boolean is_variant_chosen4 = false;
    public String versionName;
    boolean variant_mode = false;
    private String theme_name;
    private String name;
    private String package_name;
    private VariantAdapter array;
    private VariantAdapter array2;
    private VariantAdapter array3;
    private VariantAdapter array4;
    private boolean isSelected;
    private int spinnerSelection = 0;
    private int spinnerSelection2 = 0;
    private int spinnerSelection3 = 0;
    private int spinnerSelection4 = 0;
    private String variantSelected = "";
    private String variantSelected2 = "";
    private String variantSelected3 = "";
    private String variantSelected4 = "";
    private String baseResources = "";
    private Context context;
    private ArrayList<Object> enabledOverlays;
    private Drawable app_icon;
    private Boolean theme_oms;

    public OverlaysItem(String theme_name,
                        String name,
                        String packageName,
                        boolean isSelected,
                        VariantAdapter adapter,
                        VariantAdapter adapter2,
                        VariantAdapter adapter3,
                        VariantAdapter adapter4,
                        Context context,
                        String versionName,
                        String baseResources,
                        List enabledOverlays,
                        Boolean theme_oms) {

        this.theme_name = theme_name;
        this.name = name;
        this.package_name = packageName;
        this.isSelected = isSelected;
        this.array = adapter;
        this.array2 = adapter2;
        this.array3 = adapter3;
        this.array4 = adapter4;
        this.context = context;
        this.versionName = versionName;
        this.theme_oms = theme_oms;
        if (baseResources != null)
            this.baseResources =
                    baseResources.replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9]+", "");
        variant_mode = true;
        this.enabledOverlays = new ArrayList<>();
        for (int i = 0; i < enabledOverlays.size(); i++) {
            this.enabledOverlays.add(enabledOverlays.get(i));
        }
    }

    boolean isDeviceOMS() {
        return theme_oms;
    }

    public String getThemeName() {
        return theme_name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    Drawable getAppIcon() {
        return app_icon;
    }

    void setAppIcon(Drawable drawable) {
        this.app_icon = drawable;
    }

    public String getPackageName() {
        return package_name;
    }

    String getBaseResources() {
        return baseResources;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }

    VariantAdapter getSpinnerArray() {
        return array;
    }

    VariantAdapter getSpinnerArray2() {
        return array2;
    }

    VariantAdapter getSpinnerArray3() {
        return array3;
    }

    VariantAdapter getSpinnerArray4() {
        return array4;
    }

    int getSelectedVariant() {
        return spinnerSelection;
    }

    void setSelectedVariant(int position) {
        if (position != 0) {
            is_variant_chosen = true;
            is_variant_chosen1 = true;
        } else {
            is_variant_chosen = false;
            is_variant_chosen1 = false;
        }
        spinnerSelection = position;
    }

    public void updateEnabledOverlays(List<String> enabledOverlays) {
        this.enabledOverlays = new ArrayList<>();
        for (int i = 0; i < enabledOverlays.size(); i++) {
            this.enabledOverlays.add(enabledOverlays.get(i));
        }
    }

    int getSelectedVariant2() {
        return spinnerSelection2;
    }

    void setSelectedVariant2(int position) {
        if (position != 0) {
            is_variant_chosen = true;
            is_variant_chosen2 = true;
        } else {
            is_variant_chosen = false;
            is_variant_chosen2 = false;
        }
        spinnerSelection2 = position;
    }

    int getSelectedVariant3() {
        return spinnerSelection3;
    }

    void setSelectedVariant3(int position) {
        if (position != 0) {
            is_variant_chosen = true;
            is_variant_chosen3 = true;
        } else {
            is_variant_chosen = false;
            is_variant_chosen3 = false;
        }
        spinnerSelection3 = position;
    }

    int getSelectedVariant4() {
        return spinnerSelection4;
    }

    void setSelectedVariant4(int position) {
        if (position != 0) {
            is_variant_chosen = true;
            is_variant_chosen4 = true;
        } else {
            is_variant_chosen = false;
            is_variant_chosen4 = false;
        }
        spinnerSelection4 = position;
    }

    public String getSelectedVariantName() {
        return variantSelected.replaceAll("\\s+", "");
    }

    void setSelectedVariantName(String variantName) {
        this.variantSelected = variantName;
    }

    public String getSelectedVariantName2() {
        return variantSelected2.replaceAll("\\s+", "");
    }

    void setSelectedVariantName2(String variantName) {
        this.variantSelected2 = variantName;
    }

    public String getSelectedVariantName3() {
        return variantSelected3.replaceAll("\\s+", "");
    }

    void setSelectedVariantName3(String variantName) {
        this.variantSelected3 = variantName;
    }

    public String getSelectedVariantName4() {
        return variantSelected4.replaceAll("\\s+", "");
    }

    void setSelectedVariantName4(String variantName) {
        this.variantSelected4 = variantName;
    }

    Context getInheritedContext() {
        return context;
    }

    boolean compareInstalledOverlay() {
        try {
            PackageInfo pinfo =
                    context.getPackageManager().getPackageInfo(getFullOverlayParameters(), 0);
            return pinfo.versionName.equals(versionName);
        } catch (Exception e) {
            // Suppress warning
        }
        return false;
    }

    boolean compareInstalledVariantOverlay(String varianted) {
        String variant = varianted;
        if (!variant.substring(0, 1).equals(".")) variant = "." + variant;
        String base = baseResources;
        if (baseResources.length() > 0 && !baseResources.substring(0, 1).equals(".")) {
            base = "." + base;
        }
        try {
            PackageInfo pinfo =
                    context.getPackageManager().getPackageInfo(
                            getPackageName() + "." + theme_name + variant + base, 0);
            return pinfo.versionName.equals(versionName);
        } catch (Exception e) {
            // Suppress warning
        }
        return false;
    }

    public boolean isPackageInstalled(String package_name) {
        try {
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo(package_name, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (Exception e) {
            // Suppress warning
        }
        return false;
    }

    public String getFullOverlayParameters() {
        return getPackageName() + "." + getThemeName() +
                (((getSelectedVariant() == 0 &&
                        getSelectedVariant2() == 0 &&
                        getSelectedVariant3() == 0 &&
                        getSelectedVariant4() == 0)) ? "" : ".") +
                (((getSelectedVariant() == 0) ? "" : getSelectedVariantName()) +
                        ((getSelectedVariant2() == 0) ? "" : getSelectedVariantName2()) +
                        ((getSelectedVariant3() == 0) ? "" : getSelectedVariantName3()) +
                        ((getSelectedVariant4() == 0) ? "" : getSelectedVariantName4()))
                        .replaceAll("\\s", "").replaceAll("[^a-zA-Z0-9]+", "") +
                ((baseResources.length() == 0) ? "" : "." + baseResources);
    }

    public boolean isOverlayEnabled() {
        return isPackageInstalled(getFullOverlayParameters()) &&
                enabledOverlays.contains(getFullOverlayParameters());
    }
}