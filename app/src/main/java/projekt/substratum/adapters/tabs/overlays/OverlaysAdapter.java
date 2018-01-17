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
import android.support.design.widget.Lunchbar;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import projekt.substratum.InformationActivity;
import projekt.substratum.R;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
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

    private final List<OverlaysItem> overlayList;

    public OverlaysAdapter(List<OverlaysItem> overlayInfo) {
        super();
        overlayList = overlayInfo;
    }

    // Magical easy reset checking for the adapter
    // Function that runs when a user picks a spinner dropdown item that is index 0
    private static void zeroIndex(Context context,
                                  OverlaysItem current_object,
                                  ViewHolder viewHolder) {
        if (isPackageInstalled(context, current_object.getFullOverlayParameters())) {
            viewHolder.overlayState.setVisibility(View.VISIBLE);
            // Check whether currently installed overlay is up to date with
            // theme_pid's theme_version
            if (!current_object.compareInstalledOverlay()) {
                String format = String.format(context.getString(R.string
                                .overlays_update_available),
                        current_object.versionName);
                viewHolder.overlayState.setText(format);
                viewHolder.overlayState.setTextColor(
                        context.getColor(R.color.overlay_update_available));
            } else {
                String format = String.format(context.getString(R.string.overlays_up_to_date),
                        current_object.versionName);
                viewHolder.overlayState.setText(format);
                viewHolder.overlayState.setTextColor(
                        context.getColor(R.color.overlay_update_not_needed));
            }
        } else if (viewHolder.overlayState.getVisibility() == View.VISIBLE) {
            viewHolder.overlayState.setText(
                    context.getString(R.string.overlays_overlay_not_installed));
            viewHolder.overlayState.setTextColor(
                    context.getColor(R.color.overlay_not_approved_list_entry));
        } else {
            viewHolder.overlayState.setVisibility(View.GONE);
        }
        if (current_object.isOverlayEnabled()) {
            viewHolder.overlayTargetPackageName.setTextColor(
                    context.getColor(R.color.overlay_installed_list_entry));
        } else if (isPackageInstalled(context, current_object.getFullOverlayParameters())) {
            viewHolder.overlayTargetPackageName.setTextColor(
                    context.getColor(R.color.overlay_not_enabled_list_entry));
        } else {
            viewHolder.overlayTargetPackageName.setTextColor(
                    context.getColor(R.color.overlay_not_installed_list_entry));
        }
    }

    // Function that runs when a user picks a spinner dropdown item that is index >= 1
    private static void commitChanges(Context context,
                                      OverlaysItem current_object,
                                      ViewHolder viewHolder,
                                      String packageName) {
        if (isPackageInstalled(context,
                current_object.getPackageName() + '.' + current_object.getThemeName() +
                        '.' + packageName +
                        ((!current_object.getBaseResources().isEmpty()) ?
                                '.' + current_object.getBaseResources() : ""))) {
            viewHolder.overlayState.setVisibility(View.VISIBLE);
            // Check whether currently installed overlay is up to date with
            // theme_pid's theme_version
            if (!current_object.compareInstalledVariantOverlay(
                    packageName)) {
                String format = String.format(context.getString(R.string
                                .overlays_update_available),
                        current_object.versionName);

                viewHolder.overlayState.setText(format);
                viewHolder.overlayState.setTextColor(
                        context.getColor(R.color.overlay_update_available));
            } else {
                String format = String.format(context.getString(R.string.overlays_up_to_date),
                        current_object.versionName);
                viewHolder.overlayState.setText(format);
                viewHolder.overlayState.setTextColor(
                        context.getColor(R.color.overlay_update_not_needed));
            }
            if (current_object.isOverlayEnabled()) {
                viewHolder.overlayTargetPackageName.setTextColor(
                        context.getColor(R.color.overlay_installed_list_entry));
            } else if (isPackageInstalled(context,
                    current_object.getFullOverlayParameters())) {
                viewHolder.overlayTargetPackageName.setTextColor(
                        context.getColor(R.color.overlay_not_enabled_list_entry));
            } else {
                viewHolder.overlayTargetPackageName.setTextColor(
                        context.getColor(R.color.overlay_not_installed_list_entry));
            }
        } else if (viewHolder.overlayState.getVisibility() == View.VISIBLE) {
            viewHolder.overlayState.setText(
                    context.getString(R.string.overlays_overlay_not_installed));
            viewHolder.overlayState.setTextColor(
                    context.getColor(R.color.overlay_not_approved_list_entry));
            if (current_object.isOverlayEnabled()) {
                viewHolder.overlayTargetPackageName.setTextColor(
                        context.getColor(R.color.overlay_installed_list_entry));
            } else if (isPackageInstalled(context,
                    current_object.getFullOverlayParameters())) {
                viewHolder.overlayTargetPackageName.setTextColor(
                        context.getColor(R.color.overlay_not_enabled_list_entry));
            } else {
                viewHolder.overlayTargetPackageName.setTextColor(
                        context.getColor(R.color.overlay_not_installed_list_entry));
            }
        } else {
            viewHolder.overlayState.setVisibility(View.GONE);
        }
    }

    @Override
    public OverlaysAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                         int viewType) {
        View itemLayoutView = LayoutInflater.from(
                parent.getContext()).inflate(R.layout.overlays_list_row, parent, false);
        return new ViewHolder(itemLayoutView);
    }

    private AdapterView.OnItemSelectedListener adapterViewOISL(Context context,
                                                               OverlaysItem current_object,
                                                               ViewHolder viewHolder,
                                                               int spinnerNumber) {
        return new AdapterView.OnItemSelectedListener() {

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
                    OverlaysAdapter.zeroIndex(context, current_object, viewHolder);
                }

                if (pos >= 1) {
                    String packageName = "";
                    if (spinnerNumber == 1) {
                        packageName = setPackageName(packageName, arg0);
                    } else {
                        if ((viewHolder.optionsSpinner != null) && (viewHolder
                                .optionsSpinner.getVisibility() == View.VISIBLE))
                            if (viewHolder.optionsSpinner.getSelectedItemPosition() != 0)
                                packageName += viewHolder.optionsSpinner
                                        .getSelectedItem().toString()
                                        .replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9]+", "");
                    }
                    if (spinnerNumber == 2) {
                        packageName = setPackageName(packageName, arg0);
                    } else {
                        if ((viewHolder.optionsSpinner2 != null) && (viewHolder
                                .optionsSpinner2.getVisibility() == View.VISIBLE))
                            if (viewHolder.optionsSpinner2.getSelectedItemPosition() != 0)
                                packageName += viewHolder.optionsSpinner2
                                        .getSelectedItem().toString()
                                        .replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9]+", "");
                    }
                    if (spinnerNumber == 3) {
                        packageName = setPackageName(packageName, arg0);
                    } else {
                        if ((viewHolder.optionsSpinner3 != null) && (viewHolder
                                .optionsSpinner3.getVisibility() == View.VISIBLE))
                            if (viewHolder.optionsSpinner3.getSelectedItemPosition() != 0)
                                packageName += viewHolder.optionsSpinner3
                                        .getSelectedItem().toString().replaceAll("\\s+", "")
                                        .replaceAll("[^a-zA-Z0-9]+", "");
                    }
                    if (spinnerNumber == 4) {
                        packageName = setPackageName(packageName, arg0);
                    } else {
                        if ((viewHolder.optionsSpinner4 != null) && (viewHolder
                                .optionsSpinner4.getVisibility() == View.VISIBLE))
                            if (viewHolder.optionsSpinner4.getSelectedItemPosition() != 0)
                                packageName += viewHolder.optionsSpinner4
                                        .getSelectedItem().toString().replaceAll("\\s+", "")
                                        .replaceAll("[^a-zA-Z0-9]+", "");
                    }
                    if (spinnerNumber == 5) {
                        packageName = setPackageName(packageName, arg0);
                    } else {
                        if ((viewHolder.optionsSpinner5 != null) && (viewHolder
                                .optionsSpinner5.getVisibility() == View.VISIBLE))
                            if (viewHolder.optionsSpinner5.getSelectedItemPosition() != 0)
                                packageName += viewHolder.optionsSpinner5
                                        .getSelectedItem().toString().replaceAll("\\s+", "")
                                        .replaceAll("[^a-zA-Z0-9]+", "");
                    }
                    OverlaysAdapter.commitChanges(context, current_object, viewHolder, packageName);
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

        OverlaysItem current_object = overlayList.get(position);
        Context context = current_object.getContext();

        viewHolder.app_icon.setImageDrawable(current_object.getAppIcon());
        viewHolder.overlayTargetPackageName.setText(current_object.getName());

        String targetVersion;
        switch (current_object.getPackageName()) {
            case SYSTEMUI_HEADERS:
            case SYSTEMUI_NAVBARS:
            case SYSTEMUI_STATUSBARS:
            case SYSTEMUI_QSTILES:
                targetVersion = Packages.getAppVersion(context, SYSTEMUI);
                break;
            case SETTINGS_ICONS:
                targetVersion = Packages.getAppVersion(context, SETTINGS);
                break;
            default:
                targetVersion = Packages.getAppVersion(context, current_object.getPackageName());
        }
        String packageTargetWithVersion =
                current_object.getPackageName() + " [" + targetVersion + "]";
        viewHolder.overlayTargetPackage.setText(packageTargetWithVersion);

        if (isPackageInstalled(context,
                current_object.getPackageName() + '.' + current_object.getThemeName() +
                        ((!current_object.getBaseResources().isEmpty()) ?
                                '.' + current_object.getBaseResources() : ""))) {
            viewHolder.overlayState.setVisibility(View.VISIBLE);
            // Check whether currently installed overlay is up to date with theme_pid's theme_version
            if (!current_object.compareInstalledOverlay()) {
                String format = String.format(
                        context.getString(R.string.overlays_update_available),
                        current_object.versionName);
                viewHolder.overlayState.setText(format);
                viewHolder.overlayState.setTextColor(
                        context.getColor(R.color.overlay_update_available));
            } else {
                String format = String.format(context.getString(R.string.overlays_up_to_date),
                        current_object.versionName);
                viewHolder.overlayState.setText(format);
                viewHolder.overlayState.setTextColor(
                        context.getColor(R.color.overlay_update_not_needed));
            }
        } else {
            viewHolder.overlayState.setVisibility(View.GONE);
        }

        viewHolder.checkBox.setChecked(current_object.isSelected());

        viewHolder.checkBox.setTag(current_object);

        if (current_object.attention != null && current_object.attention.length() > 0) {
            viewHolder.attentionIcon.setVisibility(View.VISIBLE);
            viewHolder.attentionIcon.setOnClickListener(view -> {
                SheetDialog sheetDialog = new SheetDialog(context);
                View sheetView =
                        View.inflate(context, R.layout.overlays_attention_sheet_dialog, null);
                TextView attentionText = sheetView.findViewById(R.id.attention_text);
                attentionText.setText(current_object.attention.replace("\\n", "\n"));
                sheetDialog.setContentView(sheetView);
                sheetDialog.show();
            });
        } else {
            viewHolder.attentionIcon.setVisibility(View.GONE);
        }

        viewHolder.checkBox.setOnClickListener(v -> {
            CheckBox cb = (CheckBox) v;
            OverlaysItem contact = (OverlaysItem) cb.getTag();

            contact.setSelected(cb.isChecked());
            current_object.setSelected(cb.isChecked());
        });

        viewHolder.card.setOnClickListener(v -> {
            viewHolder.checkBox.setChecked(!viewHolder.checkBox.isChecked());

            CheckBox cb = viewHolder.checkBox;
            OverlaysItem contact = (OverlaysItem) cb.getTag();

            contact.setSelected(cb.isChecked());
            current_object.setSelected(cb.isChecked());
        });

        viewHolder.card.setOnLongClickListener(view -> {
            String packageName = current_object.getPackageName();
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
                        current_object.getName(),
                        packageVersion);

                currentShownLunchBar = Lunchbar.make(
                        getLunchbarView(current_object),
                        version,
                        Lunchbar.LENGTH_LONG);
                currentShownLunchBar.setAction(context.getString(android.R.string.copy),
                        view1 -> {
                            References.copyToClipboard(context, "version", version);
                            currentShownLunchBar = Lunchbar.make(
                                    getLunchbarView(current_object),
                                    R.string.overlays_tab_package_ver_message_copied,
                                    Lunchbar.LENGTH_SHORT);
                            currentShownLunchBar.show();
                        });
                currentShownLunchBar.show();
            } else {
                currentShownLunchBar = Lunchbar.make(
                        getLunchbarView(current_object),
                        R.string.overlays_tab_package_ver_failure,
                        Lunchbar.LENGTH_LONG);
                currentShownLunchBar.show();
            }
            return true;
        });

        if (current_object.variant_mode) {
            if (current_object.getSpinnerArray() != null) {
                viewHolder.optionsSpinner.setVisibility(View.VISIBLE);
                viewHolder.optionsSpinner.setAdapter(current_object.getSpinnerArray());
                viewHolder.optionsSpinner.setOnItemSelectedListener(
                        adapterViewOISL(context, current_object, viewHolder, 1));
                viewHolder.optionsSpinner.setSelection(current_object.getSelectedVariant());
            } else {
                viewHolder.optionsSpinner.setVisibility(View.GONE);
            }
            if (current_object.getSpinnerArray2() != null) {
                viewHolder.optionsSpinner2.setVisibility(View.VISIBLE);
                viewHolder.optionsSpinner2.setAdapter(current_object.getSpinnerArray2());
                viewHolder.optionsSpinner2.setOnItemSelectedListener(
                        adapterViewOISL(context, current_object, viewHolder, 2));
                viewHolder.optionsSpinner2.setSelection(current_object.getSelectedVariant2());
            } else {
                viewHolder.optionsSpinner2.setVisibility(View.GONE);
            }
            if (current_object.getSpinnerArray3() != null) {
                viewHolder.optionsSpinner3.setVisibility(View.VISIBLE);
                viewHolder.optionsSpinner3.setAdapter(current_object.getSpinnerArray3());
                viewHolder.optionsSpinner3.setOnItemSelectedListener(
                        adapterViewOISL(context, current_object, viewHolder, 3));
                viewHolder.optionsSpinner3.setSelection(current_object.getSelectedVariant3());
            } else {
                viewHolder.optionsSpinner3.setVisibility(View.GONE);
            }
            if (current_object.getSpinnerArray4() != null) {
                viewHolder.optionsSpinner4.setVisibility(View.VISIBLE);
                viewHolder.optionsSpinner4.setAdapter(current_object.getSpinnerArray4());
                viewHolder.optionsSpinner4.setOnItemSelectedListener(
                        adapterViewOISL(context, current_object, viewHolder, 4));
                viewHolder.optionsSpinner4.setSelection(current_object.getSelectedVariant4());
            } else {
                viewHolder.optionsSpinner4.setVisibility(View.GONE);
            }
            if (current_object.getSpinnerArray5() != null) {
                viewHolder.optionsSpinner5.setVisibility(View.VISIBLE);
                viewHolder.optionsSpinner5.setAdapter(current_object.getSpinnerArray5());
                viewHolder.optionsSpinner5.setOnItemSelectedListener(
                        adapterViewOISL(context, current_object, viewHolder, 5));
                viewHolder.optionsSpinner5.setSelection(current_object.getSelectedVariant5());
            } else {
                viewHolder.optionsSpinner5.setVisibility(View.GONE);
            }
            if (current_object.isDeviceOMS()) {
                if (current_object.isOverlayEnabled()) {
                    viewHolder.overlayTargetPackageName.setTextColor(
                            context.getColor(R.color.overlay_installed_list_entry));
                } else {
                    if (isPackageInstalled(context, current_object.getFullOverlayParameters())) {
                        viewHolder.overlayTargetPackageName.setTextColor(
                                context.getColor(R.color.overlay_not_enabled_list_entry));
                    } else {
                        viewHolder.overlayTargetPackageName.setTextColor(
                                context.getColor(R.color.overlay_not_installed_list_entry));
                    }
                }
            } else {
                if (Systems.isSamsungDevice(context)) {
                    if (current_object.isOverlayEnabled()) {
                        viewHolder.overlayTargetPackageName.setTextColor(
                                context.getColor(R.color.overlay_installed_list_entry));
                    } else {
                        viewHolder.overlayTargetPackageName.setTextColor(
                                context.getColor(R.color.overlay_not_installed_list_entry));
                    }
                } else {
                    // At this point, the object is an RRO formatted check
                    File file = new File(PIXEL_NEXUS_DIR);
                    File file2 = new File(LEGACY_NEXUS_DIR);
                    if (file.exists() || file2.exists()) {
                        File filer1 = new File(
                                file.getAbsolutePath() + '/' +
                                        current_object.getPackageName() + '.' +
                                        current_object.getThemeName() + ".apk");
                        File filer2 = new File(
                                file2.getAbsolutePath() + '/' +
                                        current_object.getPackageName() + '.' +
                                        current_object.getThemeName() + ".apk");
                        if (filer1.exists() || filer2.exists()) {
                            viewHolder.overlayTargetPackageName.setTextColor(
                                    context.getColor(R.color.overlay_installed_list_entry));
                        } else {
                            viewHolder.overlayTargetPackageName.setTextColor(
                                    context.getColor(R.color.overlay_not_installed_list_entry));
                        }
                    }
                }
            }
        } else {
            viewHolder.optionsSpinner.setVisibility(View.GONE);
            viewHolder.optionsSpinner2.setVisibility(View.GONE);
            viewHolder.optionsSpinner3.setVisibility(View.GONE);
            viewHolder.optionsSpinner4.setVisibility(View.GONE);
            viewHolder.optionsSpinner5.setVisibility(View.GONE);
            if (current_object.isDeviceOMS()) {
                if (current_object.isOverlayEnabled()) {
                    viewHolder.overlayTargetPackageName.setTextColor(
                            context.getColor(R.color.overlay_installed_list_entry));
                } else if (isPackageInstalled(context, current_object.getFullOverlayParameters())) {
                    viewHolder.overlayTargetPackageName.setTextColor(
                            context.getColor(R.color.overlay_not_enabled_list_entry));
                } else {
                    viewHolder.overlayTargetPackageName.setTextColor(
                            context.getColor(R.color.overlay_not_installed_list_entry));
                }
            }
        }
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

        final CardView card;
        final TextView overlayTargetPackageName;
        final TextView overlayTargetPackage;
        final TextView overlayState;
        final CheckBox checkBox;
        final Spinner optionsSpinner;
        final Spinner optionsSpinner2;
        final Spinner optionsSpinner3;
        final Spinner optionsSpinner4;
        final Spinner optionsSpinner5;
        final ImageView app_icon;
        final ImageView attentionIcon;

        ViewHolder(View itemLayoutView) {
            super(itemLayoutView);
            app_icon = itemLayoutView.findViewById(R.id.app_icon);
            attentionIcon = itemLayoutView.findViewById(R.id.feature_icon);
            card = itemLayoutView.findViewById(R.id.card);
            checkBox = itemLayoutView.findViewById(R.id.checkBox);
            overlayState = itemLayoutView.findViewById(R.id.installedState);
            optionsSpinner = itemLayoutView.findViewById(R.id.optionsSpinner);
            optionsSpinner2 = itemLayoutView.findViewById(R.id.optionsSpinner2);
            optionsSpinner3 = itemLayoutView.findViewById(R.id.optionsSpinner3);
            optionsSpinner4 = itemLayoutView.findViewById(R.id.optionsSpinner4);
            optionsSpinner5 = itemLayoutView.findViewById(R.id.optionsSpinner5);
            overlayTargetPackageName = itemLayoutView.findViewById(R.id
                    .overlayTargetPackageName);
            overlayTargetPackage = itemLayoutView.findViewById(R.id.overlayTargetPackage);
        }
    }
}