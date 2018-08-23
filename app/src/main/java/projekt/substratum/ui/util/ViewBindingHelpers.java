/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.ui.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import androidx.databinding.BindingAdapter;
import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import projekt.substratum.R;
import projekt.substratum.common.Internal;
import projekt.substratum.util.views.Lunchbar;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;
import static com.bumptech.glide.request.RequestOptions.centerCropTransform;

public class ViewBindingHelpers {

    @BindingAdapter("imageUrl")
    public static void imageUrl(ImageView imageView, String url) {
        Glide.with(imageView.getContext())
                .load(url.replace("http://", "https://"))
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