/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.util.views;

import android.view.View;
import android.widget.TextView;
import com.google.android.material.R;
import com.google.android.material.snackbar.Snackbar;

public class Lunchbar {

    public static Snackbar make(final View view,
                                final String string,
                                final int duration) {
        Snackbar created = Snackbar.make(view, string, duration);
        created.getView().setBackgroundColor(view.getContext().getColor(
                projekt.substratum.R.color.lunchbar_background));
        TextView textView = created.getView().findViewById(R.id.snackbar_text);
        textView.setTextColor(view.getContext().getColor(
                projekt.substratum.R.color.lunchbar_text));
        return created;
    }
}