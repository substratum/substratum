package projekt.substratum.model;

/**
 * @author Nicholas Chum (nicholaschum)
 */

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class OverlaysInfo implements Serializable {

    public boolean is_variant_chosen = false;
    public boolean is_variant_chosen1 = false;
    public boolean is_variant_chosen2 = false;
    public boolean is_variant_chosen3 = false;
    public boolean is_variant_chosen4 = false;
    public boolean variant_mode = false;
    public String versionName;
    private String theme_name;
    private String name;
    private String package_name;
    private ArrayAdapter<String> array, array2, array3, array4;
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

    public OverlaysInfo(String theme_name, String name, String packageName, boolean isSelected,
                        ArrayAdapter<String>
                                adapter, ArrayAdapter<String>
                                adapter2, ArrayAdapter<String>
                                adapter3, ArrayAdapter<String>
                                adapter4, Context context, String versionName,
                        String baseResources, List enabledOverlays) {

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
        if (baseResources != null) this.baseResources = baseResources.replaceAll("\\s+", "")
                .replaceAll("[^a-zA-Z0-9]+", "");
        if (adapter != null) variant_mode = true;
        this.enabledOverlays = new ArrayList<>();
        for (int i = 0; i < enabledOverlays.size(); i++) {
            this.enabledOverlays.add(enabledOverlays.get(i));
        }
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

    public String getPackageName() {
        return package_name;
    }

    public String getBaseResources() {
        return baseResources;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }

    public ArrayAdapter<String> getSpinnerArray() {
        return array;
    }

    public ArrayAdapter<String> getSpinnerArray2() {
        return array2;
    }

    public ArrayAdapter<String> getSpinnerArray3() {
        return array3;
    }

    public ArrayAdapter<String> getSpinnerArray4() {
        return array4;
    }

    public int getSelectedVariant() {
        return spinnerSelection;
    }

    public void setSelectedVariant(int position) {
        if (position != 0) {
            is_variant_chosen = true;
            is_variant_chosen1 = true;
        } else {
            is_variant_chosen = false;
            is_variant_chosen1 = false;
        }
        spinnerSelection = position;
    }

    public int getSelectedVariant2() {
        return spinnerSelection2;
    }

    public void setSelectedVariant2(int position) {
        if (position != 0) {
            is_variant_chosen = true;
            is_variant_chosen2 = true;
        } else {
            is_variant_chosen = false;
            is_variant_chosen2 = false;
        }
        spinnerSelection2 = position;
    }

    public int getSelectedVariant3() {
        return spinnerSelection3;
    }

    public void setSelectedVariant3(int position) {
        if (position != 0) {
            is_variant_chosen = true;
            is_variant_chosen3 = true;
        } else {
            is_variant_chosen = false;
            is_variant_chosen3 = false;
        }
        spinnerSelection3 = position;
    }

    public int getSelectedVariant4() {
        return spinnerSelection4;
    }

    public void setSelectedVariant4(int position) {
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

    public void setSelectedVariantName(String variantName) {
        this.variantSelected = variantName;
    }

    public String getSelectedVariantName2() {
        return variantSelected2.replaceAll("\\s+", "");
    }

    public void setSelectedVariantName2(String variantName) {
        this.variantSelected2 = variantName;
    }

    public String getSelectedVariantName3() {
        return variantSelected3.replaceAll("\\s+", "");
    }

    public void setSelectedVariantName3(String variantName) {
        this.variantSelected3 = variantName;
    }

    public String getSelectedVariantName4() {
        return variantSelected4.replaceAll("\\s+", "");
    }

    public void setSelectedVariantName4(String variantName) {
        this.variantSelected4 = variantName;
    }

    public Context getInheritedContext() {
        return context;
    }

    public boolean compareInstalledOverlay() {
        try {
            PackageInfo pinfo = context.getPackageManager().getPackageInfo(
                    getPackageName() + "." + theme_name, 0);
            return pinfo.versionName.equals(versionName);
        } catch (PackageManager.NameNotFoundException nnfe) {
            nnfe.printStackTrace();
            Log.e("SubstratumLogger", "Could not find explicit package identifier in " +
                    "package manager list.");
        }
        return false;
    }

    public boolean compareInstalledVariantOverlay(String variant) {
        if (!variant.substring(0, 1).equals(".")) variant = "." + variant;
        String base = baseResources;
        if (!baseResources.substring(0, 1).equals(".")) {
            base = "." + base;
        }
        try {
            PackageInfo pinfo = context.getPackageManager().getPackageInfo(
                    getPackageName() + "." + theme_name + variant + base, 0);
            return pinfo.versionName.equals(versionName);
        } catch (PackageManager.NameNotFoundException nnfe) {
            nnfe.printStackTrace();
            Log.e("SubstratumLogger", "Could not find explicit package identifier in " +
                    "package manager list.");
        }
        return false;
    }

    public boolean isPackageInstalled(String package_name) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(package_name, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public String getFullOverlayParameters() {
        return getPackageName() + "." + getThemeName() +
                (((getSelectedVariant() == 0 && getSelectedVariant2() == 0 && getSelectedVariant3
                        () == 0 &&
                        getSelectedVariant4() == 0)) ? "" : ".") +
                ((getSelectedVariant() == 0) ? "" : getSelectedVariantName()) +
                ((getSelectedVariant2() == 0) ? "" : getSelectedVariantName2()) +
                ((getSelectedVariant3() == 0) ? "" : getSelectedVariantName3()) +
                ((getSelectedVariant4() == 0) ? "" : getSelectedVariantName4()) +
                ((baseResources.length() == 0) ? "" : "." + baseResources);
    }

    public boolean isOverlayEnabled() {
        String package_name = getPackageName() + "." + getThemeName() +
                (((getSelectedVariant() == 0 && getSelectedVariant2() == 0 && getSelectedVariant3
                        () == 0 &&
                        getSelectedVariant4() == 0)) ? "" : ".") +
                ((getSelectedVariant() == 0) ? "" : getSelectedVariantName()) +
                ((getSelectedVariant2() == 0) ? "" : getSelectedVariantName2()) +
                ((getSelectedVariant3() == 0) ? "" : getSelectedVariantName3()) +
                ((getSelectedVariant4() == 0) ? "" : getSelectedVariantName4()) +
                ((baseResources.length() == 0) ? "" : "." + baseResources);
        if (isPackageInstalled(package_name)) {
            if (enabledOverlays.contains(package_name)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

}