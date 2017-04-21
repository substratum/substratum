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

import android.content.Context;
import android.support.design.widget.Lunchbar;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import projekt.substratum.R;

class IconEntry extends RecyclerView.ViewHolder implements View.OnClickListener {

    public Context mContext;
    TextView iconName;
    ImageView iconDrawable;
    private Boolean willBeModified = false;

    IconEntry(Context mContext, View itemView) {
        super(itemView);
        this.mContext = mContext;
        itemView.setOnClickListener(this);
        iconName = (TextView) itemView.findViewById(R.id.icon_pack_package);
        iconDrawable = (ImageView) itemView.findViewById(R.id.icon_pack_icon);
    }

    public void setDisabled(Boolean bool) {
        this.willBeModified = bool;
    }

    @Override
    public void onClick(View view) {
        if (willBeModified) {
            Lunchbar.make(view,
                    mContext.getString(R.string.studio_toast_mask),
                    Lunchbar.LENGTH_LONG)
                    .show();
        }
    }
}