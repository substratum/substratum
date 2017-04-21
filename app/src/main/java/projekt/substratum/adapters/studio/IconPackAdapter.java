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

package projekt.substratum.adapters.studio;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import projekt.substratum.R;
import projekt.substratum.common.References;

public class IconPackAdapter extends RecyclerView.Adapter<IconEntry> {

    private ArrayList<IconInfo> itemList;
    private Context mContext;

    public IconPackAdapter(Context mContext, ArrayList<IconInfo> itemList) {
        this.mContext = mContext;
        this.itemList = itemList;
    }

    @Override
    public IconEntry onCreateViewHolder(ViewGroup parent, int viewType) {
        @SuppressLint("InflateParams") View layoutView = LayoutInflater.from(
                parent.getContext()).inflate(R.layout.icon_entry_card, null);
        return new IconEntry(mContext, layoutView);
    }

    @Override
    public void onBindViewHolder(IconEntry holder, int position) {
        // Get the Resources first
        if (itemList.get(position).getDrawable() != null) {
            holder.iconName.setText(itemList.get(position).getParsedName());
            Glide.with(itemList.get(position).getContext())
                    .load(itemList.get(position).getDrawable())
                    .centerCrop()
                    .into(holder.iconDrawable);
        } else {
            try {
                if (itemList.get(position).getThemePackage() == null) {
                    // Package name on the RecyclerView item
                    String packageName = References.grabPackageName(
                            itemList.get(position).getContext(),
                            itemList.get(position).getPackageName());
                    itemList.get(position).setParsedName(packageName);
                    holder.iconName.setText(packageName);

                    // Load the newly added icon into the RecyclerView item
                    Drawable drawable_icon = itemList.get(position).getContext().getPackageManager()
                            .getApplicationIcon(itemList.get(position).getPackageName());
                    Bitmap bitmap = ((BitmapDrawable) drawable_icon).getBitmap();
                    try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        byte[] bitmapData = stream.toByteArray();
                        itemList.get(position).setDrawable(bitmapData);

                        Glide.with(itemList.get(position).getContext())
                                .load(bitmapData)
                                .centerCrop()
                                .into(holder.iconDrawable);
                    }
                } else {
                    Context context = itemList.get(position).getContext().createPackageContext(
                            itemList.get(position).getThemePackage(), 0);
                    Resources resources = context.getResources();
                    int drawable = resources.getIdentifier(
                            itemList.get(position).getPackageDrawable(), // Drawable name
                            "drawable",
                            itemList.get(position).getThemePackage()); // Icon Pack

                    // Package name on the RecyclerView item
                    String packageName = References.grabPackageName(
                            itemList.get(position).getContext(),
                            itemList.get(position).getPackageName());
                    itemList.get(position).setParsedName(packageName);
                    holder.iconName.setText(packageName);

                    if (drawable != 0) {
                        // Load the newly added icon into the RecyclerView item
                        Drawable drawable_icon = context.getDrawable(drawable);
                        if (drawable_icon != null) {
                            Bitmap bitmap = ((BitmapDrawable) drawable_icon).getBitmap();
                            try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                                bitmap.compress(Bitmap.CompressFormat.PNG, 70, stream);
                                byte[] bitmapData = stream.toByteArray();
                                itemList.get(position).setDrawable(bitmapData);

                                Glide.with(itemList.get(position).getContext())
                                        .load(bitmapData)
                                        .centerCrop()
                                        .into(holder.iconDrawable);
                            }
                        }
                    } else {
                        // Load the newly added icon into the RecyclerView item
                        Drawable drawable_icon = itemList.get(position).getContext()
                                .getPackageManager().getApplicationIcon(
                                        itemList.get(position).getPackageName());
                        Bitmap bitmap = ((BitmapDrawable) drawable_icon).getBitmap();
                        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 70, stream);
                            byte[] bitmapData = stream.toByteArray();
                            itemList.get(position).setDrawable(bitmapData);

                            Glide.with(itemList.get(position).getContext())
                                    .load(bitmapData)
                                    .centerCrop()
                                    .into(holder.iconDrawable);
                        }
                    }
                }
            } catch (Exception e) {
                // Suppress warning
            }
        }
        if (itemList.get(position).getPackageDrawable().equals("null")) {
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(0);
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
            holder.iconDrawable.setColorFilter(filter);
            holder.setDisabled(true);
        }
    }

    @Override
    public int getItemCount() {
        return this.itemList.size();
    }
}