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

import projekt.substratum.R;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;

import static projekt.substratum.InformationActivity.currentShownLunchBar;
import static projekt.substratum.common.Packages.isPackageInstalled;
import static projekt.substratum.common.References.LEGACY_NEXUS_DIR;
import static projekt.substratum.common.References.PIXEL_NEXUS_DIR;

public class OverlaysAdapter extends RecyclerView.Adapter<OverlaysAdapter.ViewHolder> {

    private final List<OverlaysItem> overlayList;

    public OverlaysAdapter(final List<OverlaysItem> overlayInfo) {
        super();
        this.overlayList = overlayInfo;
    }

    @Override
    public OverlaysAdapter.ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final View itemLayoutView = LayoutInflater.from(
                parent.getContext()).inflate(R.layout.overlays_list_row, parent, false);
        return new ViewHolder(itemLayoutView);
    }

    private AdapterView.OnItemSelectedListener adapterViewOISL(final Context context,
                                                               final OverlaysItem current_object,
                                                               final ViewHolder viewHolder,
                                                               final int spinnerNumber) {
        return new AdapterView.OnItemSelectedListener() {

            String setPackageName(final String packageName, final AdapterView<?> arg0) {
                return packageName + arg0.getSelectedItem().toString()
                        .replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9]+", "");
            }

            @Override
            public void onItemSelected(final AdapterView<?> arg0,
                                       final View arg1,
                                       final int pos,
                                       final long id) {
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
                    OverlaysAdapter.this.zeroIndex(context, current_object, viewHolder);
                }

                if (pos >= 1) {
                    String packageName = "";
                    if (spinnerNumber == 1) {
                        packageName = this.setPackageName(packageName, arg0);
                    } else {
                        if ((viewHolder.optionsSpinner != null) && (viewHolder
                                .optionsSpinner.getVisibility() == View.VISIBLE))
                            if (viewHolder.optionsSpinner.getSelectedItemPosition() != 0)
                                packageName += viewHolder.optionsSpinner
                                        .getSelectedItem().toString()
                                        .replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9]+", "");
                    }
                    if (spinnerNumber == 2) {
                        packageName = this.setPackageName(packageName, arg0);
                    } else {
                        if ((viewHolder.optionsSpinner2 != null) && (viewHolder
                                .optionsSpinner2.getVisibility() == View.VISIBLE))
                            if (viewHolder.optionsSpinner2.getSelectedItemPosition() != 0)
                                packageName += viewHolder.optionsSpinner2
                                        .getSelectedItem().toString()
                                        .replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9]+", "");
                    }
                    if (spinnerNumber == 3) {
                        packageName = this.setPackageName(packageName, arg0);
                    } else {
                        if ((viewHolder.optionsSpinner3 != null) && (viewHolder
                                .optionsSpinner3.getVisibility() == View.VISIBLE))
                            if (viewHolder.optionsSpinner3.getSelectedItemPosition() != 0)
                                packageName += viewHolder.optionsSpinner3
                                        .getSelectedItem().toString().replaceAll("\\s+", "")
                                        .replaceAll("[^a-zA-Z0-9]+", "");
                    }
                    if (spinnerNumber == 4) {
                        packageName = this.setPackageName(packageName, arg0);
                    } else {
                        if ((viewHolder.optionsSpinner4 != null) && (viewHolder
                                .optionsSpinner4.getVisibility() == View.VISIBLE))
                            if (viewHolder.optionsSpinner4.getSelectedItemPosition() != 0)
                                packageName += viewHolder.optionsSpinner4
                                        .getSelectedItem().toString().replaceAll("\\s+", "")
                                        .replaceAll("[^a-zA-Z0-9]+", "");
                    }
                    if (spinnerNumber == 5) {
                        packageName = this.setPackageName(packageName, arg0);
                    } else {
                        if ((viewHolder.optionsSpinner5 != null) && (viewHolder
                                .optionsSpinner5.getVisibility() == View.VISIBLE))
                            if (viewHolder.optionsSpinner5.getSelectedItemPosition() != 0)
                                packageName += viewHolder.optionsSpinner5
                                        .getSelectedItem().toString().replaceAll("\\s+", "")
                                        .replaceAll("[^a-zA-Z0-9]+", "");
                    }
                    OverlaysAdapter.this.commitChanges(context, current_object, viewHolder, packageName);
                }
            }

            @Override
            public void onNothingSelected(final AdapterView<?> arg0) {
            }
        };
    }

    // Magical easy reset checking for the adapter
    // Function that runs when a user picks a spinner dropdown item that is index 0
    private void zeroIndex(final Context context, final OverlaysItem current_object, final ViewHolder viewHolder) {
        if (isPackageInstalled(context, current_object.getFullOverlayParameters())) {
            viewHolder.overlayState.setVisibility(View.VISIBLE);
            // Check whether currently installed overlay is up to date with
            // theme_pid's versionName
            if (!current_object.compareInstalledOverlay()) {
                final String format = String.format(context.getString(R.string.overlays_update_available),
                        current_object.versionName);
                viewHolder.overlayState.setText(format);
                viewHolder.overlayState.setTextColor(
                        context.getColor(R.color.overlay_update_available));
            } else {
                viewHolder.overlayState.setText(context.getString(R.string.overlays_up_to_date));
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
    private void commitChanges(final Context context, final OverlaysItem current_object,
                               final ViewHolder viewHolder, final String packageName) {
        if (isPackageInstalled(context,
                current_object.getPackageName() + "." + current_object.getThemeName() +
                        "." + packageName +
                        ((!current_object.getBaseResources().isEmpty()) ?
                                "." + current_object.getBaseResources() : ""))) {
            viewHolder.overlayState.setVisibility(View.VISIBLE);
            // Check whether currently installed overlay is up to date with
            // theme_pid's versionName
            if (!current_object.compareInstalledVariantOverlay(
                    packageName)) {
                final String format = String.format(context.getString(R.string.overlays_update_available),
                        current_object.versionName);

                viewHolder.overlayState.setText(format);
                viewHolder.overlayState.setTextColor(
                        context.getColor(R.color.overlay_update_available));
            } else {
                viewHolder.overlayState.setText(
                        context.getString(R.string.overlays_up_to_date));
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
    public void onBindViewHolder(final ViewHolder viewHolder, final int position) {

        final OverlaysItem current_object = this.overlayList.get(position);
        final Context context = current_object.getInheritedContext();

        viewHolder.app_icon.setImageDrawable(current_object.getAppIcon());

        viewHolder.overlayTargetPackageName.setText(current_object.getName());

        viewHolder.overlayTargetPackage.setText(current_object.getPackageName());

        if (isPackageInstalled(context,
                current_object.getPackageName() + "." + current_object.getThemeName() +
                        ((!current_object.getBaseResources().isEmpty()) ?
                                "." + current_object.getBaseResources() : ""))) {
            viewHolder.overlayState.setVisibility(View.VISIBLE);
            // Check whether currently installed overlay is up to date with theme_pid's versionName
            if (!current_object.compareInstalledOverlay()) {
                final String format = String.format(
                        context.getString(R.string.overlays_update_available),
                        current_object.versionName);
                viewHolder.overlayState.setText(format);
                viewHolder.overlayState.setTextColor(
                        context.getColor(R.color.overlay_update_available));
            } else {
                viewHolder.overlayState.setText(context.getString(R.string.overlays_up_to_date));
                viewHolder.overlayState.setTextColor(
                        context.getColor(R.color.overlay_update_not_needed));
            }
        } else {
            viewHolder.overlayState.setVisibility(View.GONE);
        }

        viewHolder.checkBox.setChecked(current_object.isSelected());

        viewHolder.checkBox.setTag(current_object);

        viewHolder.checkBox.setOnClickListener(v -> {
            final CheckBox cb = (CheckBox) v;
            final OverlaysItem contact = (OverlaysItem) cb.getTag();

            contact.setSelected(cb.isChecked());
            current_object.setSelected(cb.isChecked());
        });

        viewHolder.card.setOnClickListener(v -> {
            viewHolder.checkBox.setChecked(!viewHolder.checkBox.isChecked());

            final CheckBox cb = viewHolder.checkBox;
            final OverlaysItem contact = (OverlaysItem) cb.getTag();

            contact.setSelected(cb.isChecked());
            current_object.setSelected(cb.isChecked());
        });

        viewHolder.card.setOnLongClickListener(view -> {
            final String packageName = current_object.getPackageName();
            final String packageVersion;
            switch (packageName) {
                case "com.android.systemui.headers":
                case "com.android.systemui.navbars":
                case "com.android.systemui.statusbars":
                case "com.android.systemui.tiles":
                    packageVersion = Packages.getAppVersion(
                            context, "com.android.systemui");
                    break;
                case "com.android.settings.icons":
                    packageVersion = Packages.getAppVersion(
                            context, "com.android.settings");
                    break;
                default:
                    packageVersion = Packages.getAppVersion(
                            context, packageName);
            }
            if (packageVersion != null) {
                final String version = String.format(
                        context.getString(R.string.overlays_tab_package_ver_message),
                        current_object.getName(),
                        packageVersion);

                currentShownLunchBar = Lunchbar.make(
                        current_object.getActivityView(),
                        version,
                        Lunchbar.LENGTH_LONG);
                currentShownLunchBar.setAction(context.getString(android.R.string.copy),
                        view1 -> {
                            References.copyToClipboard(context, "version", version);
                            currentShownLunchBar = Lunchbar.make(
                                    current_object.getActivityView(),
                                    R.string.overlays_tab_package_ver_message_copied,
                                    Lunchbar.LENGTH_SHORT);
                            currentShownLunchBar.show();
                        });
                currentShownLunchBar.show();
            } else {
                currentShownLunchBar = Lunchbar.make(
                        current_object.getActivityView(),
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
                        this.adapterViewOISL(context, current_object, viewHolder, 1));
                viewHolder.optionsSpinner.setSelection(current_object.getSelectedVariant());
            } else {
                viewHolder.optionsSpinner.setVisibility(View.GONE);
            }
            if (current_object.getSpinnerArray2() != null) {
                viewHolder.optionsSpinner2.setVisibility(View.VISIBLE);
                viewHolder.optionsSpinner2.setAdapter(current_object.getSpinnerArray2());
                viewHolder.optionsSpinner2.setOnItemSelectedListener(
                        this.adapterViewOISL(context, current_object, viewHolder, 2));
                viewHolder.optionsSpinner2.setSelection(current_object.getSelectedVariant2());
            } else {
                viewHolder.optionsSpinner2.setVisibility(View.GONE);
            }
            if (current_object.getSpinnerArray3() != null) {
                viewHolder.optionsSpinner3.setVisibility(View.VISIBLE);
                viewHolder.optionsSpinner3.setAdapter(current_object.getSpinnerArray3());
                viewHolder.optionsSpinner3.setOnItemSelectedListener(
                        this.adapterViewOISL(context, current_object, viewHolder, 3));
                viewHolder.optionsSpinner3.setSelection(current_object.getSelectedVariant3());
            } else {
                viewHolder.optionsSpinner3.setVisibility(View.GONE);
            }
            if (current_object.getSpinnerArray4() != null) {
                viewHolder.optionsSpinner4.setVisibility(View.VISIBLE);
                viewHolder.optionsSpinner4.setAdapter(current_object.getSpinnerArray4());
                viewHolder.optionsSpinner4.setOnItemSelectedListener(
                        this.adapterViewOISL(context, current_object, viewHolder, 4));
                viewHolder.optionsSpinner4.setSelection(current_object.getSelectedVariant4());
            } else {
                viewHolder.optionsSpinner4.setVisibility(View.GONE);
            }
            if (current_object.getSpinnerArray5() != null) {
                viewHolder.optionsSpinner5.setVisibility(View.VISIBLE);
                viewHolder.optionsSpinner5.setAdapter(current_object.getSpinnerArray5());
                viewHolder.optionsSpinner5.setOnItemSelectedListener(
                        this.adapterViewOISL(context, current_object, viewHolder, 5));
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
                if (Systems.isSamsung(context)) {
                    if (current_object.isOverlayEnabled()) {
                        viewHolder.overlayTargetPackageName.setTextColor(
                                context.getColor(R.color.overlay_installed_list_entry));
                    } else {
                        viewHolder.overlayTargetPackageName.setTextColor(
                                context.getColor(R.color.overlay_not_installed_list_entry));
                    }
                } else {
                    // At this point, the object is an RRO formatted check
                    final File file = new File(PIXEL_NEXUS_DIR);
                    final File file2 = new File(LEGACY_NEXUS_DIR);
                    if (file.exists() || file2.exists()) {
                        final File filer1 = new File(
                                file.getAbsolutePath() + "/" +
                                        current_object.getPackageName() + "." +
                                        current_object.getThemeName() + ".apk");
                        final File filer2 = new File(
                                file2.getAbsolutePath() + "/" +
                                        current_object.getPackageName() + "." +
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

    @Override
    public int getItemCount() {
        return this.overlayList.size();
    }

    public List<OverlaysItem> getOverlayList() {
        return this.overlayList;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        CardView card;
        TextView overlayTargetPackageName;
        TextView overlayTargetPackage;
        TextView overlayState;
        CheckBox checkBox;
        Spinner optionsSpinner;
        Spinner optionsSpinner2;
        Spinner optionsSpinner3;
        Spinner optionsSpinner4;
        Spinner optionsSpinner5;
        ImageView app_icon;

        ViewHolder(final View itemLayoutView) {
            super(itemLayoutView);
            this.app_icon = itemLayoutView.findViewById(R.id.app_icon);
            this.card = itemLayoutView.findViewById(R.id.card);
            this.checkBox = itemLayoutView.findViewById(R.id.checkBox);
            this.overlayState = itemLayoutView.findViewById(R.id.installedState);
            this.optionsSpinner = itemLayoutView.findViewById(R.id.optionsSpinner);
            this.optionsSpinner2 = itemLayoutView.findViewById(R.id.optionsSpinner2);
            this.optionsSpinner3 = itemLayoutView.findViewById(R.id.optionsSpinner3);
            this.optionsSpinner4 = itemLayoutView.findViewById(R.id.optionsSpinner4);
            this.optionsSpinner5 = itemLayoutView.findViewById(R.id.optionsSpinner5);
            this.overlayTargetPackageName = itemLayoutView.findViewById(R.id.overlayTargetPackageName);
            this.overlayTargetPackage = itemLayoutView.findViewById(R.id.overlayTargetPackage);
        }
    }
}