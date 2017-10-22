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

package projekt.substratum.adapters.showcase;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.design.widget.Lunchbar;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import projekt.substratum.R;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;
import static com.bumptech.glide.request.RequestOptions.centerCropTransform;

public class ShowcaseItemAdapter extends RecyclerView.Adapter<ShowcaseItemAdapter.ViewHolder> {
    private final List<ShowcaseItem> information;

    public ShowcaseItemAdapter(final List<ShowcaseItem> information) {
        super();
        this.information = information;
    }

    @Override
    public ShowcaseItemAdapter.ViewHolder onCreateViewHolder(final ViewGroup viewGroup, final int
            i) {
        final View view = LayoutInflater.from(
                viewGroup.getContext()).inflate(R.layout.showcase_entry_card, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int pos) {
        final ShowcaseItem showcaseItem = this.information.get(pos);
        final Context context = showcaseItem.getContext();

        Glide.with(context)
                .load(showcaseItem.getThemeIcon())
                .apply(centerCropTransform())
                .transition(withCrossFade())
                .into(viewHolder.imageView);

        Glide.with(context)
                .load(showcaseItem.getThemeBackgroundImage())
                .apply(centerCropTransform())
                .transition(withCrossFade())
                .into(viewHolder.backgroundImageView);

        viewHolder.themeName.setText(showcaseItem.getThemeName());
        viewHolder.themeAuthor.setText(showcaseItem.getThemeAuthor());

        if (showcaseItem.getThemePricing().toLowerCase(Locale.US).equals(References.paidTheme)) {
            viewHolder.themePricing.setVisibility(View.VISIBLE);
        } else {
            viewHolder.themePricing.setVisibility(View.GONE);
        }

        if (Packages.isPackageInstalled(context, showcaseItem.getThemePackage())) {
            viewHolder.installedOrNot.setVisibility(View.VISIBLE);
        } else {
            viewHolder.installedOrNot.setVisibility(View.GONE);
        }

        final String[] supported = showcaseItem.getThemeSupport().split("\\|");
        final List supported_array = Arrays.asList(supported);
        if (supported_array.contains(References.showcaseWallpapers)) {
            viewHolder.wallpaper.setAlpha(1.0f);
        } else {
            viewHolder.wallpaper.setAlpha(0.2f);
        }
        if (supported_array.contains(References.showcaseSounds)) {
            viewHolder.sounds.setAlpha(1.0f);
        } else {
            viewHolder.sounds.setAlpha(0.2f);
        }
        if (supported_array.contains(References.showcaseFonts)) {
            viewHolder.fonts.setAlpha(1.0f);
        } else {
            viewHolder.fonts.setAlpha(0.2f);
        }
        if (supported_array.contains(References.showcaseBootanimations)) {
            viewHolder.bootanimations.setAlpha(1.0f);
        } else {
            viewHolder.bootanimations.setAlpha(0.2f);
        }
        if (supported_array.contains(References.showcaseOverlays)) {
            viewHolder.overlays.setAlpha(1.0f);
        } else {
            viewHolder.overlays.setAlpha(0.2f);
        }

        viewHolder.cardView.setOnClickListener(view -> {
            try {
                final Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(showcaseItem.getThemeLink()));
                context.startActivity(intent);
            } catch (final Exception e) {
                e.printStackTrace();
                Lunchbar.make(view,
                        context.getString(R.string.activity_missing_toast),
                        Lunchbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return this.information.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final CardView cardView;
        final TextView themeName;
        final TextView themeAuthor;
        final TextView installedOrNot;
        final ImageView themePricing;
        final ImageView imageView;
        final ImageView backgroundImageView;
        final ImageView wallpaper;
        final ImageView sounds;
        final ImageView fonts;
        final ImageView bootanimations;
        final ImageView overlays;

        ViewHolder(final View view) {
            super(view);
            this.cardView = view.findViewById(R.id.theme_card);
            this.themeName = view.findViewById(R.id.theme_name);
            this.themeAuthor = view.findViewById(R.id.theme_author);
            this.themePricing = view.findViewById(R.id.theme_pricing);
            this.imageView = view.findViewById(R.id.theme_icon);
            this.backgroundImageView = view.findViewById(R.id.background_image);
            this.wallpaper = view.findViewById(R.id.theme_wallpapers);
            this.sounds = view.findViewById(R.id.theme_sounds);
            this.fonts = view.findViewById(R.id.theme_fonts);
            this.bootanimations = view.findViewById(R.id.theme_bootanimations);
            this.overlays = view.findViewById(R.id.theme_overlays);
            this.installedOrNot = view.findViewById(R.id.themeinstalled);
        }
    }
}