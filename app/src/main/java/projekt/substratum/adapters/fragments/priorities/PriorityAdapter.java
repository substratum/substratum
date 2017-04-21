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

package projekt.substratum.adapters.fragments.priorities;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.thesurix.gesturerecycler.GestureAdapter;
import com.thesurix.gesturerecycler.GestureViewHolder;

import java.io.ByteArrayOutputStream;

import projekt.substratum.R;

public class PriorityAdapter extends GestureAdapter<PrioritiesInterface, GestureViewHolder> {

    private final Context mContext;
    private final int mItemResId;

    public PriorityAdapter(final Context context, @LayoutRes final int itemResId) {
        this.mContext = context;
        this.mItemResId = itemResId;
    }

    @Override
    public GestureViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        if (viewType == PrioritiesInterface.PrioritiesItemType.CONTENT.ordinal()) {
            View itemView = LayoutInflater.from(
                    parent.getContext()).inflate(mItemResId, parent, false);
            return new PriorityObjectAdapter(itemView);
        } else {
            View itemView = LayoutInflater.from(
                    parent.getContext()).inflate(R.layout.header_item, parent, false);
            return new HeaderViewAdapter(itemView);
        }
    }

    @Override
    public void onBindViewHolder(final GestureViewHolder holder, final int position) {
        super.onBindViewHolder(holder, position);
        final PrioritiesInterface prioritiesInterface = getData().get(position);

        if (prioritiesInterface.getType() == PrioritiesInterface.PrioritiesItemType.CONTENT) {
            PriorityObjectAdapter priorityObjectAdapter = (PriorityObjectAdapter) holder;
            PrioritiesItem prioritiesItem = (PrioritiesItem) prioritiesInterface;

            try {
                // Keep this value but do not display it to the user, instead, parse it
                ApplicationInfo applicationInfo = mContext.getPackageManager()
                        .getApplicationInfo
                                (prioritiesItem.getName(), PackageManager.GET_META_DATA);
                String packageTitle = mContext.getPackageManager().getApplicationLabel
                        (applicationInfo).toString();
                if (applicationInfo.metaData != null) {
                    if (applicationInfo.metaData.getString("Substratum_Device") != null) {
                        priorityObjectAdapter.mCardText.setText(prioritiesItem.getName());
                    } else {
                        priorityObjectAdapter.mCardText.setText(
                                packageTitle + " (" + prioritiesItem.getName() + ")");
                    }
                } else {
                    priorityObjectAdapter.mCardText.setText(
                            packageTitle + " (" + prioritiesItem.getName() + ")");
                }

                // Grab app icon from PackageInstaller and convert it to a BitmapDrawable in bytes
                Drawable icon = prioritiesItem.getDrawableId();
                Bitmap bitmap = ((BitmapDrawable) icon).getBitmap();
                try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] bitmapData = stream.toByteArray();

                    Glide.with(mContext)
                            .load(bitmapData)
                            .centerCrop()
                            .into(priorityObjectAdapter.mAppIcon);
                }
            } catch (Exception e) {
                // Suppress warning
            }
        } else {
            HeaderViewAdapter headerViewAdapter = (HeaderViewAdapter) holder;
            PrioritiesHeader prioritiesHeader = (PrioritiesHeader) prioritiesInterface;
            headerViewAdapter.mHeaderText.setText(prioritiesHeader.getName());
        }
    }

    @Override
    public int getItemViewType(final int position) {
        return getData().get(position).getType().ordinal();
    }

    private class PrioritiesHeader implements PrioritiesInterface {

        private String mName;

        public PrioritiesHeader(final String name) {
            mName = name;
        }

        public String getName() {
            return mName;
        }

        @Override
        public PrioritiesItemType getType() {
            return PrioritiesItemType.HEADER;
        }
    }

    private class PriorityObjectAdapter extends GestureViewHolder {

        TextView mCardText;
        ImageView mAppIcon;
        private ImageView mItemDrag;

        PriorityObjectAdapter(final View view) {
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

    private class HeaderViewAdapter extends GestureViewHolder {

        TextView mHeaderText;

        HeaderViewAdapter(final View view) {
            super(view);
            mHeaderText = (TextView) view.findViewById(R.id.header_text);
        }

        @Override
        public boolean canDrag() {
            return false;
        }

        @Override
        public boolean canSwipe() {
            return false;
        }
    }
}