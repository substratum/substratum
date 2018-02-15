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

package projekt.substratum.ui.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.databinding.BindingAdapter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import projekt.substratum.R;
import projekt.substratum.common.Internal;
import projekt.substratum.util.views.Lunchbar;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;
import static com.bumptech.glide.request.RequestOptions.centerCropTransform;

public class ViewBindingHelpers {

    @BindingAdapter("imageUrl")
    public static void imageUrl(ImageView imageView, String url) {
        Glide.with(imageView.getContext())
                .load(url)
                .apply(centerCropTransform())
                .transition(withCrossFade())
                .into(imageView);
    }

    @BindingAdapter("drawable")
    public static void setDrawable(ImageView imageView, Drawable drawable) {
        Glide.with(imageView.getContext())
                .load(drawable)
                .apply(centerCropTransform())
                .transition(withCrossFade())
                .into(imageView);
    }

    @BindingAdapter("playUrl")
    public static void playUrl(View view, String packageName) {
        Context context = view.getContext();
        view.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(Internal.PLAY_URL_PREFIX + packageName));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (ActivityNotFoundException exc) {
                Lunchbar.make(v,
                        context.getString(R.string.activity_missing_toast),
                        Snackbar.LENGTH_LONG).show();
            }
        });

    }
}