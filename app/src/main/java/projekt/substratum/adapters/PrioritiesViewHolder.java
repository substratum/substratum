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

package projekt.substratum.adapters;

import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.thesurix.gesturerecycler.GestureViewHolder;

import projekt.substratum.R;


class PrioritiesViewHolder extends GestureViewHolder {

    TextView mCardText;
    ImageView mAppIcon;
    private ImageView mItemDrag;

    PrioritiesViewHolder(final View view) {
        super(view);
        mCardText = (TextView) view.findViewById(R.id.card_text);
        mItemDrag = (ImageView) view.findViewById(R.id.card_drag);
        mAppIcon = (ImageView) view.findViewById(R.id.app_icon);
    }

    @Nullable
    @Override
    public View getDraggableView() {
        return mItemDrag;
    }

    @Override
    public boolean canDrag() {
        return true;
    }

    @Override
    public boolean canSwipe() {
        return true;
    }
}