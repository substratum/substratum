/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.util.views;

import android.support.design.R;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.TextView;

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