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
import android.databinding.DataBindingUtil;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import projekt.substratum.InformationActivity;
import projekt.substratum.R;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.databinding.TabOverlaysItemBinding;
import projekt.substratum.util.views.Lunchbar;
import projekt.substratum.util.views.SheetDialog;

import static projekt.substratum.InformationActivity.currentShownLunchBar;
import static projekt.substratum.common.Packages.isPackageInstalled;
import static projekt.substratum.common.References.LEGACY_NEXUS_DIR;
import static projekt.substratum.common.References.PIXEL_NEXUS_DIR;
import static projekt.substratum.common.Resources.SETTINGS;
import static projekt.substratum.common.Resources.SETTINGS_ICONS;
import static projekt.substratum.common.Resources.SYSTEMUI;
import static projekt.substratum.common.Resources.SYSTEMUI_HEADERS;
import static projekt.substratum.common.Resources.SYSTEMUI_NAVBARS;
import static projekt.substratum.common.Resources.SYSTEMUI_QSTILES;
import static projekt.substratum.common.Resources.SYSTEMUI_STATUSBARS;

public class OverlaysAdapter extends RecyclerView.Adapter<OverlaysAdapter.ViewHolder> {

    private List<OverlaysItem> overlayList;
    private List<String> overlayStateList;

    public OverlaysAdapter(List<OverlaysItem> overlayInfo, Context context) {
        super();
        overlayList = overlayInfo;
        overlayStateList = ThemeManager.listAllOverlays(context);
    }

    // Magical easy reset checking for the adapter
    // Function that runs when a user picks a spinner dropdown item that is index 0
    private static void zeroIndex(Context context,
                                  OverlaysItem currentObject,
                                  List<String> overlayStateList,
                                  TabOverlaysItemBinding viewBinding) {
        if (isPackageInstalled(context, currentObject.getFullOverlayParameters())) {
            viewBinding.overlayState.setVisibility(View.VISIBLE);
            // Check whether currently installed overlay is up to date with
            // themePid's themeVersion
            if (currentObject.compareInstalledOverlay()) {
                String format = String.format(context.getString(R.string
                                .overlays_update_available),
                        currentObject.versionName);
                viewBinding.overlayState.setText(format);
                viewBinding.overlayState.setTextColor(
                        context.getColor(R.color.overlay_update_available));
            } else {
                String format = String.format(context.getString(R.string.overlays_up_to_date),
                        currentObject.versionName);
                viewBinding.overlayState.setText(format);
                viewBinding.overlayState.setTextColor(
                        context.getColor(R.color.overlay_update_not_needed));
            }
        } else if (viewBinding.overlayState.getVisibility() == View.VISIBLE) {
            viewBinding.overlayState.setText(
                    context.getString(R.string.overlays_overlay_not_installed));
            viewBinding.overlayState.setTextColor(
                    context.getColor(R.color.overlay_not_approved_list_entry));
        } else {
            viewBinding.overlayState.setVisibility(View.GONE);
        }
        if (currentObject.isOverlayEnabled()) {
            viewBinding.overlayTargetPackageName.setTextColor(
                    context.getColor(R.color.overlay_installed_list_entry));
        } else if (isPackageInstalled(context, currentObject.getFullOverlayParameters())) {
            if (Systems.isNewSamsungDevice()) {
                if (!overlayStateList.contains(currentObject.getFullOverlayParameters())) {
                    viewBinding.overlayTargetPackageName.setTextColor(
                            context.getColor(R.color.overlay_installed_not_active));
                } else {
                    viewBinding.overlayTargetPackageName.setTextColor(
                            context.getColor(R.color.overlay_not_enabled_list_entry));
                }
            } else {
                viewBinding.overlayTargetPackageName.setTextColor(
                        context.getColor(R.color.overlay_not_enabled_list_entry));
            }
        } else {
            viewBinding.overlayTargetPackageName.setTextColor(
                    context.getColor(R.color.overlay_not_installed_list_entry));
        }
    }

    // Function that runs when a user picks a spinner dropdown item that is index >= 1
    private static void commitChanges(Context context,
                                      OverlaysItem overlaysItem,
                                      List<String> overlayStateList,
                                      TabOverlaysItemBinding viewBinding,
                                      String packageName) {
        if (isPackageInstalled(context,
                getThemeVariantPackageName(overlaysItem, packageName))) {
            viewBinding.overlayState.setVisibility(View.VISIBLE);
            // Check whether currently installed overlay is up to date with
            // themePid's themeVersion
            if (!overlaysItem.compareInstalledVariantOverlay(
                    packageName)) {
                viewBinding.overlayState.setText(String.format(
                        context.getString(R.string.overlays_update_available),
                        overlaysItem.versionName));
                viewBinding.overlayState.setTextColor(
                        context.getColor(R.color.overlay_update_available));
            } else {
                viewBinding.overlayState.setText(String.format(
                        context.getString(R.string.overlays_up_to_date),
                        overlaysItem.versionName));
                viewBinding.overlayState.setTextColor(
                        context.getColor(R.color.overlay_update_not_needed));
            }
            if (overlaysItem.isOverlayEnabled()) {
                viewBinding.overlayTargetPackageName.setTextColor(
                        context.getColor(R.color.overlay_installed_list_entry));
            } else if (isPackageInstalled(context,
                    overlaysItem.getFullOverlayParameters())) {
                if (Systems.isNewSamsungDevice()) {
                    if (!overlayStateList.contains(overlaysItem.getFullOverlayParameters())) {
                        viewBinding.overlayTargetPackageName.setTextColor(
                                context.getColor(R.color.overlay_installed_not_active));
                    } else {
                        viewBinding.overlayTargetPackageName.setTextColor(
                                context.getColor(R.color.overlay_not_enabled_list_entry));
                    }
                } else {
                    viewBinding.overlayTargetPackageName.setTextColor(
                            context.getColor(R.color.overlay_not_enabled_list_entry));
                }
            } else {
                viewBinding.overlayTargetPackageName.setTextColor(
                        context.getColor(R.color.overlay_not_installed_list_entry));
            }
        } else if (viewBinding.overlayState.getVisibility() == View.VISIBLE) {
            viewBinding.overlayState.setText(
                    context.getString(R.string.overlays_overlay_not_installed));
            viewBinding.overlayState.setTextColor(
                    context.getColor(R.color.overlay_not_approved_list_entry));
            if (overlaysItem.isOverlayEnabled()) {
                viewBinding.overlayTargetPackageName.setTextColor(
                        context.getColor(R.color.overlay_installed_list_entry));
            } else if (isPackageInstalled(context,
                    overlaysItem.getFullOverlayParameters())) {
                if (Systems.isNewSamsungDevice()) {
                    if (!overlayStateList.contains(overlaysItem.getFullOverlayParameters())) {
                        viewBinding.overlayTargetPackageName.setTextColor(
                                context.getColor(R.color.overlay_installed_not_active));
                    } else {
                        viewBinding.overlayTargetPackageName.setTextColor(
                                context.getColor(R.color.overlay_not_enabled_list_entry));
                    }
                } else {
                    viewBinding.overlayTargetPackageName.setTextColor(
                            context.getColor(R.color.overlay_not_enabled_list_entry));
                }
            } else {
                viewBinding.overlayTargetPackageName.setTextColor(
                        context.getColor(R.color.overlay_not_installed_list_entry));
            }
        } else {
            viewBinding.overlayState.setVisibility(View.GONE);
        }
    }

    private static String sanitizeOptionSpinnerText(String optionSpinnerText) {
        return optionSpinnerText.replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9]+", "");
    }

    private static String getThemeVariantPackageName(OverlaysItem overlaysItem, String packageName) {
        return overlaysItem.getPackageName() + '.' + overlaysItem.getThemeName() +
                '.' + packageName +
                ((!overlaysItem.getBaseResources().isEmpty()) ?
                        '.' + overlaysItem.getBaseResources() : "");
    }

    @Override
    public OverlaysAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                         int viewType) {
        View itemLayoutView = LayoutInflater.from(
                parent.getContext()).inflate(R.layout.tab_overlays_item, parent, false);
        return new ViewHolder(itemLayoutView);
    }

    private AdapterView.OnItemSelectedListener adapterViewOISL(Context context,
                                                               OverlaysItem current_object,
                                                               ViewHolder viewHolder,
                                                               int spinnerNumber) {
        return new AdapterView.OnItemSelectedListener() {

            TabOverlaysItemBinding viewHolderBinding = viewHolder.getBinding();

            String setPackageName(String packageName, AdapterView<?> arg0) {
                return packageName + arg0.getSelectedItem().toString()
                        .replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9]+", "");
            }

            @Override
            public void onItemSelected(AdapterView<?> arg0,
                                       View arg1,
                                       int pos,
                                       long id) {
                switch (spinnerNumber) {
                    case 1:
                        current_object.setSelectedVariant(pos);
                        current_object.setSelectedVariantName(arg0.getSelectedItem().toString());
                        break;
                    case 2:
                        current_object.setSelectedVariant2(pos);
                        current_object.setSelectedVariantName2(arg0.getSelectedItem().toString());
                        break;
                    case 3:
                        current_object.setSelectedVariant3(pos);
                        current_object.setSelectedVariantName3(arg0.getSelectedItem().toString());
                        break;
                    case 4:
                        current_object.setSelectedVariant4(pos);
                        current_object.setSelectedVariantName4(arg0.getSelectedItem().toString());
                        break;
                    case 5:
                        current_object.setSelectedVariant5(pos);
                        current_object.setSelectedVariantName5(arg0.getSelectedItem().toString());
                        break;
                }

                if (pos == 0) {
                    OverlaysAdapter.zeroIndex(
                            context,
                            current_object,
                            overlayStateList,
                            viewHolderBinding
                    );
                } else if (pos >= 1) {
                    String packageName = "";
                    if (spinnerNumber == 1) {
                        packageName = setPackageName(packageName, arg0);
                    } else {
                        if ((viewHolderBinding.optionsSpinner != null) && (viewHolderBinding
                                .optionsSpinner.getVisibility() == View.VISIBLE))
                            if (viewHolderBinding.optionsSpinner.getSelectedItemPosition() != 0)
                                packageName += sanitizeOptionSpinnerText(
                                        viewHolderBinding.optionsSpinner.getSelectedItem().toString());
                    }
                    if (spinnerNumber == 2) {
                        packageName = setPackageName(packageName, arg0);
                    } else {
                        if ((viewHolderBinding.optionsSpinner2 != null) && (viewHolderBinding
                                .optionsSpinner2.getVisibility() == View.VISIBLE))
                            if (viewHolderBinding.optionsSpinner2.getSelectedItemPosition() != 0)
                                packageName += sanitizeOptionSpinnerText(
                                        viewHolderBinding.optionsSpinner2.getSelectedItem().toString());
                    }
                    if (spinnerNumber == 3) {
                        packageName = setPackageName(packageName, arg0);
                    } else {
                        if ((viewHolderBinding.optionsSpinner3 != null) && (viewHolderBinding
                                .optionsSpinner3.getVisibility() == View.VISIBLE))
                            if (viewHolderBinding.optionsSpinner3.getSelectedItemPosition() != 0)
                                packageName += sanitizeOptionSpinnerText(
                                        viewHolderBinding.optionsSpinner3.getSelectedItem().toString());
                    }
                    if (spinnerNumber == 4) {
                        packageName = setPackageName(packageName, arg0);
                    } else {
                        if ((viewHolderBinding.optionsSpinner4 != null) && (viewHolderBinding
                                .optionsSpinner4.getVisibility() == View.VISIBLE))
                            if (viewHolderBinding.optionsSpinner4.getSelectedItemPosition() != 0)
                                packageName += sanitizeOptionSpinnerText(
                                        viewHolderBinding.optionsSpinner4.getSelectedItem().toString());
                    }
                    if (spinnerNumber == 5) {
                        packageName = setPackageName(packageName, arg0);
                    } else {
                        if ((viewHolderBinding.optionsSpinner5 != null) && (viewHolderBinding
                                .optionsSpinner5.getVisibility() == View.VISIBLE))
                            if (viewHolderBinding.optionsSpinner5.getSelectedItemPosition() != 0)
                                packageName += sanitizeOptionSpinnerText(
                                        viewHolderBinding.optionsSpinner5.getSelectedItem().toString());
                    }
                    OverlaysAdapter.commitChanges(
                            context,
                            current_object,
                            overlayStateList,
                            viewHolderBinding,
                            packageName
                    );
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        };
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder,
                                 int position) {

        OverlaysItem overlaysItem = overlayList.get(position);
        TabOverlaysItemBinding viewHolderBinding = viewHolder.getBinding();
        Context context = overlaysItem.getContext();

        viewHolderBinding.appIcon.setImageDrawable(overlaysItem.getAppIcon());
        viewHolderBinding.overlayTargetPackageName.setText(overlaysItem.getName());

        if (overlaysItem.isVariantInstalled()) {
            // Check whether currently installed overlay is up to date with themePid's themeVersion
            if (overlaysItem.compareInstalledOverlay()) {
                viewHolderBinding.overlayState.setText(
                        String.format(
                                context.getString(R.string.overlays_update_available),
                                overlaysItem.versionName));
                viewHolderBinding.overlayState.setTextColor(
                        context.getColor(R.color.overlay_update_available));
            } else {
                viewHolderBinding.overlayState.setText(
                        String.format(
                                context.getString(R.string.overlays_up_to_date),
                                overlaysItem.versionName));
                viewHolderBinding.overlayState.setTextColor(
                        context.getColor(R.color.overlay_update_not_needed));
            }
        }

        viewHolderBinding.checkBox.setTag(overlaysItem);

        viewHolderBinding.attentionIcon.setOnClickListener(view -> {
            SheetDialog sheetDialog = new SheetDialog(context);
            View sheetView =
                    View.inflate(context, R.layout.tab_overlays_attention_sheet_dialog, null);
            TextView attentionText = sheetView.findViewById(R.id.attention_text);
            attentionText.setText(overlaysItem.attention.replace("\\n", "\n"));
            sheetDialog.setContentView(sheetView);
            sheetDialog.show();
        });

        viewHolderBinding.checkBox.setOnClickListener(v -> {
            CheckBox cb = (CheckBox) v;
            OverlaysItem contact = (OverlaysItem) cb.getTag();

            contact.setSelected(cb.isChecked());
            overlaysItem.setSelected(cb.isChecked());
        });

        viewHolderBinding.card.setOnClickListener(v -> {
            viewHolderBinding.checkBox.setChecked(!viewHolderBinding.checkBox.isChecked());

            CheckBox cb = viewHolderBinding.checkBox;
            OverlaysItem contact = (OverlaysItem) cb.getTag();

            contact.setSelected(cb.isChecked());
            overlaysItem.setSelected(cb.isChecked());
        });

        viewHolderBinding.card.setOnLongClickListener(view -> {
            String packageName = overlaysItem.getPackageName();
            String packageVersion;
            switch (packageName) {
                case SYSTEMUI_HEADERS:
                case SYSTEMUI_NAVBARS:
                case SYSTEMUI_STATUSBARS:
                case SYSTEMUI_QSTILES:
                    packageVersion = Packages.getAppVersion(
                            context, SYSTEMUI);
                    break;
                case SETTINGS_ICONS:
                    packageVersion = Packages.getAppVersion(
                            context, SETTINGS);
                    break;
                default:
                    packageVersion = Packages.getAppVersion(
                            context, packageName);
            }
            if (packageVersion != null) {
                String version = String.format(
                        context.getString(R.string.overlays_tab_package_ver_message),
                        overlaysItem.getName(),
                        packageVersion);
                References.copyToClipboard(context, "version", version);
                currentShownLunchBar = Lunchbar.make(
                        getLunchbarView(overlaysItem),
                        context.getString(
                                R.string.overlays_tab_package_ver_message_copied),
                        Snackbar.LENGTH_SHORT);
                currentShownLunchBar.show();
            } else {
                currentShownLunchBar = Lunchbar.make(
                        getLunchbarView(overlaysItem),
                        context.getString(R.string.overlays_tab_package_ver_failure),
                        Snackbar.LENGTH_LONG);
                currentShownLunchBar.show();
            }
            return true;
        });

        if (overlaysItem.variantMode) {
            if (overlaysItem.getSpinnerArray() != null) {
                viewHolderBinding.optionsSpinner.setAdapter(overlaysItem.getSpinnerArray());
                viewHolderBinding.optionsSpinner.setOnItemSelectedListener(
                        adapterViewOISL(context, overlaysItem, viewHolder, 1));
                viewHolderBinding.optionsSpinner.setSelection(overlaysItem.getSelectedVariant());
            }
            if (overlaysItem.getSpinnerArray2() != null) {
                viewHolderBinding.optionsSpinner2.setAdapter(overlaysItem.getSpinnerArray2());
                viewHolderBinding.optionsSpinner2.setOnItemSelectedListener(
                        adapterViewOISL(context, overlaysItem, viewHolder, 2));
                viewHolderBinding.optionsSpinner2.setSelection(overlaysItem.getSelectedVariant2());
            }
            if (overlaysItem.getSpinnerArray3() != null) {
                viewHolderBinding.optionsSpinner3.setAdapter(overlaysItem.getSpinnerArray3());
                viewHolderBinding.optionsSpinner3.setOnItemSelectedListener(
                        adapterViewOISL(context, overlaysItem, viewHolder, 3));
                viewHolderBinding.optionsSpinner3.setSelection(overlaysItem.getSelectedVariant3());
            }
            if (overlaysItem.getSpinnerArray4() != null) {
                viewHolderBinding.optionsSpinner4.setAdapter(overlaysItem.getSpinnerArray4());
                viewHolderBinding.optionsSpinner4.setOnItemSelectedListener(
                        adapterViewOISL(context, overlaysItem, viewHolder, 4));
                viewHolderBinding.optionsSpinner4.setSelection(overlaysItem.getSelectedVariant4());
            }
            if (overlaysItem.getSpinnerArray5() != null) {
                viewHolderBinding.optionsSpinner5.setAdapter(overlaysItem.getSpinnerArray5());
                viewHolderBinding.optionsSpinner5.setOnItemSelectedListener(
                        adapterViewOISL(context, overlaysItem, viewHolder, 5));
                viewHolderBinding.optionsSpinner5.setSelection(overlaysItem.getSelectedVariant5());
            }
            if (overlaysItem.isDeviceOMS()) {
                if (overlaysItem.isOverlayEnabled()) {
                    viewHolderBinding.overlayTargetPackageName.setTextColor(
                            context.getColor(R.color.overlay_installed_list_entry));
                } else {
                    if (isPackageInstalled(context, overlaysItem.getFullOverlayParameters())) {
                        if (Systems.isNewSamsungDevice()) {
                            if (!overlayStateList.contains(
                                    overlaysItem.getFullOverlayParameters())) {
                                viewHolderBinding.overlayTargetPackageName.setTextColor(
                                        context.getColor(R.color.overlay_installed_not_active));
                            } else {
                                viewHolderBinding.overlayTargetPackageName.setTextColor(
                                        context.getColor(R.color.overlay_not_enabled_list_entry));
                            }
                        } else {
                            viewHolderBinding.overlayTargetPackageName.setTextColor(
                                    context.getColor(R.color.overlay_not_enabled_list_entry));
                        }
                    } else {
                        viewHolderBinding.overlayTargetPackageName.setTextColor(
                                context.getColor(R.color.overlay_not_installed_list_entry));
                    }
                }
            } else {
                if (Systems.isSamsungDevice(context)) {
                    if (overlaysItem.isOverlayEnabled()) {
                        viewHolderBinding.overlayTargetPackageName.setTextColor(
                                context.getColor(R.color.overlay_installed_list_entry));
                    } else {
                        viewHolderBinding.overlayTargetPackageName.setTextColor(
                                context.getColor(R.color.overlay_not_installed_list_entry));
                    }
                } else {
                    // At this point, the object is an RRO formatted check
                    File file = new File(PIXEL_NEXUS_DIR);
                    File file2 = new File(LEGACY_NEXUS_DIR);
                    if (file.exists() || file2.exists()) {
                        File filer1 = new File(
                                file.getAbsolutePath() + '/' +
                                        overlaysItem.getPackageName() + '.' +
                                        overlaysItem.getThemeName() + ".apk");
                        File filer2 = new File(
                                file2.getAbsolutePath() + '/' +
                                        overlaysItem.getPackageName() + '.' +
                                        overlaysItem.getThemeName() + ".apk");
                        if (filer1.exists() || filer2.exists()) {
                            viewHolderBinding.overlayTargetPackageName.setTextColor(
                                    context.getColor(R.color.overlay_installed_list_entry));
                        } else {
                            viewHolderBinding.overlayTargetPackageName.setTextColor(
                                    context.getColor(R.color.overlay_not_installed_list_entry));
                        }
                    }
                }
            }
        } else {
            if (overlaysItem.isDeviceOMS()) {
                if (overlaysItem.isOverlayEnabled()) {
                    viewHolderBinding.overlayTargetPackageName.setTextColor(
                            context.getColor(R.color.overlay_installed_list_entry));
                } else if (isPackageInstalled(context, overlaysItem.getFullOverlayParameters())) {
                    if (Systems.isNewSamsungDevice()) {
                        if (!overlayStateList.contains(overlaysItem.getFullOverlayParameters())) {
                            viewHolderBinding.overlayTargetPackageName.setTextColor(
                                    context.getColor(R.color.overlay_installed_not_active));
                        } else {
                            viewHolderBinding.overlayTargetPackageName.setTextColor(
                                    context.getColor(R.color.overlay_not_enabled_list_entry));
                        }
                    } else {
                        viewHolderBinding.overlayTargetPackageName.setTextColor(
                                context.getColor(R.color.overlay_not_enabled_list_entry));
                    }
                }
            }
        }
        viewHolderBinding.setOverlay(overlaysItem);
        viewHolderBinding.executePendingBindings();
    }

    private View getLunchbarView(OverlaysItem item) {
        Context context = item.getContext();
        if (context instanceof InformationActivity) {
            return References.getCoordinatorLayoutView((InformationActivity) context);
        }
        return item.getActivityView();
    }

    @Override
    public int getItemCount() {
        return overlayList.size();
    }

    public List<OverlaysItem> getOverlayList() {
        return overlayList;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TabOverlaysItemBinding binding;

        ViewHolder(View itemLayoutView) {
            super(itemLayoutView);
            binding = DataBindingUtil.bind(itemLayoutView);
        }

        TabOverlaysItemBinding getBinding() {
            return binding;
        }
    }
}