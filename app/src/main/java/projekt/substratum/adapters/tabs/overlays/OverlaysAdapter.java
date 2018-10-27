/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.adapters.tabs.overlays;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;
import projekt.substratum.R;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.databinding.TabOverlaysItemBinding;
import projekt.substratum.util.views.SheetDialog;

import java.io.File;
import java.util.List;

import static projekt.substratum.common.Packages.isPackageInstalled;
import static projekt.substratum.common.References.LEGACY_NEXUS_DIR;
import static projekt.substratum.common.References.PIXEL_NEXUS_DIR;
import static projekt.substratum.common.Systems.checkOMS;
import static projekt.substratum.common.Systems.isSamsungDevice;

public class OverlaysAdapter extends RecyclerView.Adapter<OverlaysAdapter.ViewHolder> {

    private static final String INSTALLED_ENABLED = "INSTALLED_ENABLED";
    private static final String INSTALLED_UNKNOWN = "INSTALLED_UNKNOWN";
    private static final String INSTALLED_DISABLED = "INSTALLED_DISABLED";
    private static final String NOT_INSTALLED = "NOT_INSTALLED";
    private final List<OverlaysItem> overlayList;
    private List<String> overlayStateList;

    public OverlaysAdapter(List<OverlaysItem> overlayInfo, Context context) {
        super();
        overlayList = overlayInfo;
        refreshOverlayStateList(context);
    }

    /**
     * The main function that was consolidated from zeroIndex and commitChanges, that mainly focuses
     * on the updating of colors and statuses of the overlay when selected in InformationActivity
     *
     * @param context          Context
     * @param overlaysItem     Object of the overlay
     * @param overlayStateList Cached state of the overlay
     * @param viewBinding      View binding
     * @param packageName      Package name, nullable, due to consolidation of state, if null, then
     *                         it will run on index 0, else, it will run on indexes 1 and greater.
     */
    private static void changeVisibleOptions(Context context,
                                             OverlaysItem overlaysItem,
                                             List<String> overlayStateList,
                                             TabOverlaysItemBinding viewBinding,
                                             @Nullable String packageName) {
        if (checkOMS(context)) {
            // This includes everything from custom ROMs to stock Oreo devices...
            String packageToCheck;
            if (packageName != null) {
                packageToCheck = getThemeVariantPackageName(overlaysItem, packageName);
            } else {
                packageToCheck = overlaysItem.getFullOverlayParameters();
            }
            if (isPackageInstalled(context, packageToCheck)) {
                viewBinding.overlayState.setVisibility(View.VISIBLE);
                if (overlaysItem.isOverlayEnabled()) {
                    changeOverlayTargetPackageNameTint(viewBinding, context, INSTALLED_ENABLED);
                } else if (!overlayStateList.contains(packageToCheck)) {
                    changeOverlayTargetPackageNameTint(viewBinding, context, INSTALLED_UNKNOWN);
                } else {
                    changeOverlayTargetPackageNameTint(viewBinding, context, INSTALLED_DISABLED);
                }
            } else {
                if (overlaysItem.getColorState() == 0) {
                    overlaysItem.setColorState(
                            context.getColor(R.color.overlay_not_installed_list_entry));
                    changeOverlayTargetPackageNameTint(
                            viewBinding, context, NOT_INSTALLED);
                } else {
                    changeOverlayTargetPackageNameTint(
                            viewBinding, context, String.valueOf(overlaysItem.getColorState()));
                }
                viewBinding.overlayState.setVisibility(View.GONE);
            }
        } else if (isSamsungDevice(context)) {
            // Nougat based Samsung check
            changeOverlayTargetPackageNameTint(viewBinding, context,
                    (overlaysItem.isOverlayEnabled() ? INSTALLED_ENABLED : NOT_INSTALLED));
            viewBinding.overlayState.setVisibility(
                    overlaysItem.isOverlayEnabled() ? View.VISIBLE : View.GONE);
        } else {
            // Nougat based RRO/Legacy check
            File file = new File(PIXEL_NEXUS_DIR);
            File file2 = new File(LEGACY_NEXUS_DIR);
            if (file.exists() || file2.exists()) {
                String directoryAppend =
                        '/' + overlaysItem.getPackageName() +
                                '.' + overlaysItem.getThemeName() + ".apk";
                File filer1 = new File(file.getAbsolutePath() + directoryAppend);
                File filer2 = new File(file2.getAbsolutePath() + directoryAppend);
                changeOverlayTargetPackageNameTint(viewBinding, context,
                        (filer1.exists() || filer2.exists() ? INSTALLED_ENABLED : NOT_INSTALLED));
                viewBinding.overlayState.setVisibility(
                        overlaysItem.isOverlayEnabled() ? View.VISIBLE : View.GONE);
            }
        }
        // Now let's check if the state needs changing...
        if (viewBinding.overlayState.getVisibility() == View.VISIBLE) {
            changeOverlayState(
                    viewBinding,
                    context,
                    overlaysItem,
                    ((packageName == null) ?
                            overlaysItem.compareInstalledOverlay() :
                            !overlaysItem.compareInstalledVariantOverlay(packageName))
            );
        }
    }

    /**
     * Helper function to set the text and text color with one call
     *
     * @param binding      View binding
     * @param context      Context
     * @param overlaysItem Object of the overlay
     * @param update       State of the overlay (to update, or it's up to date)
     */
    private static void changeOverlayState(TabOverlaysItemBinding binding,
                                           Context context,
                                           OverlaysItem overlaysItem,
                                           boolean update) {
        binding.overlayState.setText(
                String.format(
                        context.getString(update ?
                                R.string.overlays_update_available :
                                R.string.overlays_up_to_date),
                        overlaysItem.versionName)
        );
        binding.overlayState.setTextColor(
                context.getColor((update ?
                        R.color.overlay_update_available :
                        R.color.overlay_update_not_needed)
                )
        );
    }

    /**
     * Helper function to set the color of the package name based on the state
     *
     * @param binding View binding
     * @param context Context
     * @param state   What state the overlay should reflect
     */
    private static void changeOverlayTargetPackageNameTint(TabOverlaysItemBinding binding,
                                                           Context context,
                                                           String state) {
        switch (state) {
            case INSTALLED_ENABLED:
                binding.overlayTargetPackageName.setTextColor(
                        context.getColor(R.color.overlay_installed_list_entry));
                break;
            case INSTALLED_DISABLED:
                binding.overlayTargetPackageName.setTextColor(
                        context.getColor(R.color.overlay_not_enabled_list_entry));
                break;
            case INSTALLED_UNKNOWN:
                binding.overlayTargetPackageName.setTextColor(
                        context.getColor(R.color.overlay_installed_not_active));
                break;
            case NOT_INSTALLED:
                binding.overlayTargetPackageName.setTextColor(
                        context.getColor(R.color.overlay_not_installed_list_entry));
                break;
            default:
                binding.overlayTargetPackageName.setTextColor(Integer.valueOf(state));
                break;
        }
    }

    /**
     * Helper function to clean out the text to remove all non-acceptable package name strings
     *
     * @param optionSpinnerText Text to be cleaned
     * @return Cleaned text
     */
    private static String sanitizeSpinnerText(String optionSpinnerText) {
        return optionSpinnerText.replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9]+", "");
    }

    /**
     * Helper function to obtain the theme's variant package name as a whole with all types
     *
     * @param overlaysItem Overlay object
     * @param packageName  Package name of initial theme
     * @return Theorized package name with all the variant info
     */
    private static String getThemeVariantPackageName(OverlaysItem overlaysItem,
                                                     String packageName) {
        return overlaysItem.getPackageName() + '.' + overlaysItem.getThemeName() +
                '.' + packageName + (!overlaysItem.getBaseResources().isEmpty() ?
                '.' + overlaysItem.getBaseResources() : "");
    }

    public void refreshOverlayStateList(Context context) {
        overlayStateList = ThemeManager.listAllOverlays(context);
    }

    @NonNull
    @Override
    public OverlaysAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                         int viewType) {
        View itemLayoutView = LayoutInflater.from(
                parent.getContext()).inflate(R.layout.tab_overlays_item, parent, false);
        return new ViewHolder(itemLayoutView);
    }

    /**
     * Adapter listener object that reflects to the user's spinner dropdown selection
     *
     * @param context       Context
     * @param overlaysItem  Overlay object
     * @param viewHolder    View holder
     * @param spinnerNumber Spinner selection
     * @return Listener object that dynamically changes based on user selection
     */
    private AdapterView.OnItemSelectedListener overlayAdapterListener(Context context,
                                                                      OverlaysItem overlaysItem,
                                                                      ViewHolder viewHolder,
                                                                      int spinnerNumber) {
        return new AdapterView.OnItemSelectedListener() {

            final TabOverlaysItemBinding viewHolderBinding = viewHolder.getBinding();

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
                        overlaysItem.setSelectedVariant(pos);
                        overlaysItem.setSelectedVariantName(arg0.getSelectedItem().toString());
                        break;
                    case 2:
                        overlaysItem.setSelectedVariant2(pos);
                        overlaysItem.setSelectedVariantName2(arg0.getSelectedItem().toString());
                        break;
                    case 3:
                        overlaysItem.setSelectedVariant3(pos);
                        overlaysItem.setSelectedVariantName3(arg0.getSelectedItem().toString());
                        break;
                    case 4:
                        overlaysItem.setSelectedVariant4(pos);
                        overlaysItem.setSelectedVariantName4(arg0.getSelectedItem().toString());
                        break;
                    case 5:
                        overlaysItem.setSelectedVariant5(pos);
                        overlaysItem.setSelectedVariantName5(arg0.getSelectedItem().toString());
                        break;
                }

                if (pos == 0) {
                    OverlaysAdapter.changeVisibleOptions(
                            context,
                            overlaysItem,
                            overlayStateList,
                            viewHolderBinding,
                            null
                    );
                } else if (pos >= 1) {
                    String packageName = "";
                    if (spinnerNumber == 1) {
                        packageName = setPackageName(packageName, arg0);
                    } else {
                        if ((viewHolderBinding.optionsSpinner != null) && (viewHolderBinding
                                .optionsSpinner.getVisibility() == View.VISIBLE))
                            if (viewHolderBinding.optionsSpinner.getSelectedItemPosition() != 0)
                                packageName += sanitizeSpinnerText(
                                        viewHolderBinding.optionsSpinner
                                                .getSelectedItem().toString());
                    }
                    if (spinnerNumber == 2) {
                        packageName = setPackageName(packageName, arg0);
                    } else {
                        if ((viewHolderBinding.optionsSpinner2 != null) && (viewHolderBinding
                                .optionsSpinner2.getVisibility() == View.VISIBLE))
                            if (viewHolderBinding.optionsSpinner2.getSelectedItemPosition() != 0)
                                packageName += sanitizeSpinnerText(
                                        viewHolderBinding.optionsSpinner2
                                                .getSelectedItem().toString());
                    }
                    if (spinnerNumber == 3) {
                        packageName = setPackageName(packageName, arg0);
                    } else {
                        if ((viewHolderBinding.optionsSpinner3 != null) && (viewHolderBinding
                                .optionsSpinner3.getVisibility() == View.VISIBLE))
                            if (viewHolderBinding.optionsSpinner3.getSelectedItemPosition() != 0)
                                packageName += sanitizeSpinnerText(
                                        viewHolderBinding.optionsSpinner3
                                                .getSelectedItem().toString());
                    }
                    if (spinnerNumber == 4) {
                        packageName = setPackageName(packageName, arg0);
                    } else {
                        if ((viewHolderBinding.optionsSpinner4 != null) && (viewHolderBinding
                                .optionsSpinner4.getVisibility() == View.VISIBLE))
                            if (viewHolderBinding.optionsSpinner4.getSelectedItemPosition() != 0)
                                packageName += sanitizeSpinnerText(
                                        viewHolderBinding.optionsSpinner4
                                                .getSelectedItem().toString());
                    }
                    if (spinnerNumber == 5) {
                        packageName = setPackageName(packageName, arg0);
                    } else {
                        if ((viewHolderBinding.optionsSpinner5 != null) && (viewHolderBinding
                                .optionsSpinner5.getVisibility() == View.VISIBLE))
                            if (viewHolderBinding.optionsSpinner5.getSelectedItemPosition() != 0)
                                packageName += sanitizeSpinnerText(
                                        viewHolderBinding.optionsSpinner5
                                                .getSelectedItem().toString());
                    }
                    OverlaysAdapter.changeVisibleOptions(
                            context,
                            overlaysItem,
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
    public void onBindViewHolder(@NonNull ViewHolder viewHolder,
                                 int position) {

        OverlaysItem overlaysItem = overlayList.get(position);
        TabOverlaysItemBinding viewHolderBinding = viewHolder.getBinding();
        Context context = overlaysItem.getContext();

        viewHolderBinding.appIcon.setImageDrawable(overlaysItem.getAppIcon());
        viewHolderBinding.overlayTargetPackageName.setText(overlaysItem.getName());

        OverlaysAdapter.changeVisibleOptions(
                context,
                overlaysItem,
                overlayStateList,
                viewHolderBinding,
                null
        );

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

        if (overlaysItem.variantMode) {
            if (overlaysItem.getSpinnerArray() != null) {
                viewHolderBinding.optionsSpinner.setAdapter(overlaysItem.getSpinnerArray());
                viewHolderBinding.optionsSpinner.setOnItemSelectedListener(
                        overlayAdapterListener(context, overlaysItem, viewHolder, 1));
                viewHolderBinding.optionsSpinner.setSelection(overlaysItem.getSelectedVariant());
            }
            if (overlaysItem.getSpinnerArray2() != null) {
                viewHolderBinding.optionsSpinner2.setAdapter(overlaysItem.getSpinnerArray2());
                viewHolderBinding.optionsSpinner2.setOnItemSelectedListener(
                        overlayAdapterListener(context, overlaysItem, viewHolder, 2));
                viewHolderBinding.optionsSpinner2.setSelection(overlaysItem.getSelectedVariant2());
            }
            if (overlaysItem.getSpinnerArray3() != null) {
                viewHolderBinding.optionsSpinner3.setAdapter(overlaysItem.getSpinnerArray3());
                viewHolderBinding.optionsSpinner3.setOnItemSelectedListener(
                        overlayAdapterListener(context, overlaysItem, viewHolder, 3));
                viewHolderBinding.optionsSpinner3.setSelection(overlaysItem.getSelectedVariant3());
            }
            if (overlaysItem.getSpinnerArray4() != null) {
                viewHolderBinding.optionsSpinner4.setAdapter(overlaysItem.getSpinnerArray4());
                viewHolderBinding.optionsSpinner4.setOnItemSelectedListener(
                        overlayAdapterListener(context, overlaysItem, viewHolder, 4));
                viewHolderBinding.optionsSpinner4.setSelection(overlaysItem.getSelectedVariant4());
            }
            if (overlaysItem.getSpinnerArray5() != null) {
                viewHolderBinding.optionsSpinner5.setAdapter(overlaysItem.getSpinnerArray5());
                viewHolderBinding.optionsSpinner5.setOnItemSelectedListener(
                        overlayAdapterListener(context, overlaysItem, viewHolder, 5));
                viewHolderBinding.optionsSpinner5.setSelection(overlaysItem.getSelectedVariant5());
            }
        }
        viewHolderBinding.setOverlay(overlaysItem);
        viewHolderBinding.executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return overlayList.size();
    }

    public List<OverlaysItem> getOverlayList() {
        return overlayList;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TabOverlaysItemBinding binding;

        ViewHolder(View itemLayoutView) {
            super(itemLayoutView);
            binding = DataBindingUtil.bind(itemLayoutView);
        }

        TabOverlaysItemBinding getBinding() {
            return binding;
        }
    }
}