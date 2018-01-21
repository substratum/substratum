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