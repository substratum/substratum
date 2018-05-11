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
import android.content.res.ColorStateList;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

import projekt.substratum.R;
import projekt.substratum.common.Packages;
import projekt.substratum.databinding.TabOverlaysPreviewItemBinding;

public class VariantAdapter extends ArrayAdapter<VariantItem> {

    private final Context context;

    public VariantAdapter(Context context,
                          List<VariantItem> variantItemArrayList) {
        super(context, R.layout.tab_overlays_preview_item, variantItemArrayList);
        this.context = context;
    }

    @Override
    public View getDropDownView(int position,
                                View convertView,
                                @NonNull ViewGroup parent) {
        return this.getCustomView(position);
    }

    @NonNull
    @Override
    public View getView(int position,
                        View convertView,
                        @NonNull ViewGroup parent) {
        return this.getCustomView(position);
    }

    private View getCustomView(int position) {
        LayoutInflater inflater = LayoutInflater.from(context);
        TabOverlaysPreviewItemBinding binding = DataBindingUtil.
                inflate(inflater, R.layout.tab_overlays_preview_item, null, false);

        VariantItem item = this.getItem(position);
        if (item != null) {
            try {
                // First check if our model contains a saved color value
                if (item.isDefaultOption()) {
                    if (item.getVariantName() != null) {
                        binding.variantName.setText(item.getVariantName().replace("_", " "));
                        binding.variantHex.setVisibility(View.GONE);
                    }
                } else if (item.getColor() == 0) {
                    if (item.getVariantName() != null) {
                        binding.variantName.setText(item.getVariantName().replace("_", " "));
                        if (item.getVariantHex().contains(":color")) {
                            // Uh oh, we hit a package dependent resource pointer!
                            String workingValue = item.getVariantHex();
                            // First, we have to strip out the reference pointers (public/private)
                            if (workingValue.startsWith("@*")) {
                                workingValue = workingValue.substring(2);
                            } else if (workingValue.startsWith("@")) {
                                workingValue = workingValue.substring(1);
                            }
                            // Now check the package name
                            String workingPackage = workingValue.split(":")[0];
                            String workingColor = workingValue.split("/")[1];
                            int color = Packages.getColorResource(
                                    this.getContext(), workingPackage, workingColor);
                            if (color != 0) {
                                item.setColor(color);
                                ColorStateList csl = new ColorStateList(
                                        new int[][]{
                                                new int[]{android.R.attr.state_checked},
                                                new int[]{}
                                        },
                                        new int[]{
                                                color,
                                                color
                                        }
                                );
                                binding.variantHex.setImageTintList(csl);
                                binding.variantHex.setVisibility(View.VISIBLE);
                            } else {
                                binding.variantName.setText(item.getVariantName());
                                binding.variantHex.setVisibility(View.GONE);
                            }
                        } else {
                            int color = Color.parseColor(item.getVariantHex());
                            item.setColor(color);
                            ColorStateList csl = new ColorStateList(
                                    new int[][]{
                                            new int[]{android.R.attr.state_checked},
                                            new int[]{}
                                    },
                                    new int[]{
                                            color,
                                            color
                                    }
                            );
                            binding.variantHex.setImageTintList(csl);
                            binding.variantHex.setVisibility(View.VISIBLE);
                        }
                    } else {
                        binding.variantHex.setVisibility(View.INVISIBLE);
                    }
                } else {
                    if (item.getVariantName() != null) {
                        binding.variantName.setText(item.getVariantName().replace("_", " "));
                    }
                    // We now know that the color is not 0 which is the hardcoded null set for int
                    int color = item.getColor();
                    ColorStateList csl = new ColorStateList(
                            new int[][]{
                                    new int[]{android.R.attr.state_checked},
                                    new int[]{}
                            },
                            new int[]{
                                    color,
                                    color
                            }
                    );
                    binding.variantHex.setImageTintList(csl);
                    binding.variantHex.setVisibility(View.VISIBLE);
                }
            } catch (IllegalArgumentException iae) {
                binding.variantHex.setVisibility(View.INVISIBLE);
            }
        } else {
            binding.variantHex.setVisibility(View.INVISIBLE);
        }
        binding.setOverlayColorPreviewItem(item);
        binding.executePendingBindings();
        return binding.getRoot();
    }
}