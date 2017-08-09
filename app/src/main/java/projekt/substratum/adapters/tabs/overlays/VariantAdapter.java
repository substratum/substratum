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
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import projekt.substratum.R;
import projekt.substratum.common.References;

public class VariantAdapter extends ArrayAdapter<VariantItem> {

    public VariantAdapter(Context context, ArrayList<VariantItem> variantItemArrayList) {
        super(context, R.layout.preview_spinner, R.id.variant_name, variantItemArrayList);
    }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    private View getCustomView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(
                    this.getContext()).inflate(R.layout.preview_spinner, parent, false);

            holder = new ViewHolder();
            holder.variantName = (TextView) convertView.findViewById(R.id.variant_name);
            holder.variantHex = (ImageView) convertView.findViewById(R.id.variant_hex);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        VariantItem item = getItem(position);
        if (item != null) {
            try {
                // First check if our model contains a saved color value
                if (item.isDefaultOption()) {
                    if (item.getVariantName() != null) {
                        holder.variantName.setText(item.getVariantName().replace("_", " "));
                        holder.variantHex.setVisibility(View.GONE);
                    }
                } else if (item.getColor() == 0) {
                    if (item.getVariantName() != null) {
                        holder.variantName.setText(item.getVariantName().replace("_", " "));
                        if (item.getVariantHex().contains(":color")) {
                            // Uh oh, we hit a package dependent resource pointer!
                            String working_value = item.getVariantHex();
                            // First, we have to strip out the reference pointers (public/private)
                            if (working_value.startsWith("@*")) {
                                working_value = working_value.substring(2);
                            } else if (working_value.startsWith("@")) {
                                working_value = working_value.substring(1);
                            }
                            // Now check the package name
                            String working_package = working_value.split(":")[0];
                            String working_color = working_value.split("/")[1];
                            int color = References.grabColorResource(
                                    getContext(), working_package, working_color);
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
                                holder.variantHex.setImageTintList(csl);
                                holder.variantHex.setVisibility(View.VISIBLE);
                            } else {
                                holder.variantName.setText(item.getVariantName());
                                holder.variantHex.setVisibility(View.GONE);
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
                            holder.variantHex.setImageTintList(csl);
                            holder.variantHex.setVisibility(View.VISIBLE);
                        }
                    } else {
                        holder.variantHex.setVisibility(View.INVISIBLE);
                    }
                } else {
                    if (item.getVariantName() != null) {
                        holder.variantName.setText(item.getVariantName().replace("_", " "));
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
                    holder.variantHex.setImageTintList(csl);
                    holder.variantHex.setVisibility(View.VISIBLE);
                }
            } catch (IllegalArgumentException iae) {
                holder.variantHex.setVisibility(View.INVISIBLE);
            }
        } else {
            holder.variantHex.setVisibility(View.INVISIBLE);
        }
        return convertView;
    }

    private class ViewHolder {
        TextView variantName;
        ImageView variantHex;
    }
}