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
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.thesurix.gesturerecycler.GestureAdapter;
import com.thesurix.gesturerecycler.GestureViewHolder;

import projekt.substratum.BuildConfig;
import projekt.substratum.R;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;

import static android.text.Html.FROM_HTML_MODE_LEGACY;

public class PriorityAdapter extends GestureAdapter<PrioritiesInterface, GestureViewHolder> {

    private final Context mContext;
    private final int mItemResId;

    public PriorityAdapter(final Context context, @LayoutRes final int itemResId) {
        super();
        this.mContext = context;
        this.mItemResId = itemResId;
    }

    @Override
    public GestureViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        if (viewType == PrioritiesInterface.PrioritiesItemType.CONTENT.ordinal()) {
            final View itemView = LayoutInflater.from(
                    parent.getContext()).inflate(this.mItemResId, parent, false);
            return new PriorityObjectAdapter(itemView);
        } else {
            final View itemView = LayoutInflater.from(
                    parent.getContext()).inflate(R.layout.priority_item, parent, false);
            return new HeaderViewAdapter(itemView);
        }
    }

    @Override
    public void onBindViewHolder(final GestureViewHolder holder, final int position) {
        super.onBindViewHolder(holder, position);
        final PrioritiesInterface prioritiesInterface = this.getData().get(position);

        if (prioritiesInterface.getType() == PrioritiesInterface.PrioritiesItemType.CONTENT) {
            final PriorityObjectAdapter priorityObjectAdapter = (PriorityObjectAdapter) holder;
            final PrioritiesItem prioritiesItem = (PrioritiesItem) prioritiesInterface;

            try {
                // Keep this value but do not display it to the user, instead, parse it
                final ApplicationInfo applicationInfo = this.mContext.getPackageManager()
                        .getApplicationInfo(prioritiesItem.getName(), PackageManager.GET_META_DATA);
                final String packageTitle = this.mContext.getPackageManager()
                        .getApplicationLabel(applicationInfo).toString();
                final String targetPackage = Packages.getOverlayTarget(
                        this.mContext, prioritiesItem.getName());
                final String title;
                if (packageTitle.startsWith("com.android.systemui.headers")) {
                    title = this.mContext.getString(R.string.systemui_headers);
                } else if (packageTitle.startsWith("com.android.systemui.navbars")) {
                    title = this.mContext.getString(R.string.systemui_navigation);
                } else if (packageTitle.startsWith("com.android.systemui.statusbars")) {
                    title = this.mContext.getString(R.string.systemui_statusbar);
                } else if (packageTitle.startsWith("com.android.systemui.tiles")) {
                    title = this.mContext.getString(R.string.systemui_qs_tiles);
                } else if (packageTitle.startsWith("com.android.settings.icons")) {
                    title = this.mContext.getString(R.string.settings_icons);
                } else {
                    title = Packages.getPackageName(this.mContext, targetPackage);
                }

                if ((title != null) && !title.isEmpty()) {
                    priorityObjectAdapter.mCardText.setText(title);
                } else {
                    priorityObjectAdapter.mCardText.setText(R.string.reboot_awaiting_manager_title);
                }

                if (applicationInfo.metaData != null) {
                    if (applicationInfo.metaData.getString("Substratum_Device") != null) {
                        final int version = Packages.getOverlaySubstratumVersion(
                                this.mContext,
                                packageTitle,
                                References.metadataOverlayVersion);

                        if (prioritiesItem.getType1a() == null) {
                            String metadata = Packages.getOverlayMetadata(
                                    this.mContext,
                                    packageTitle,
                                    References.metadataOverlayType1a);
                            if ((metadata != null) && !metadata.isEmpty()) {
                                metadata = metadata.replace("_", " ");
                                final String textView =
                                        "<b>" + this.mContext.getString(R.string.manager_type1a) +
                                                "</b> " + metadata;
                                priorityObjectAdapter.type1a.setVisibility(View.VISIBLE);
                                prioritiesItem.setType1a(textView);
                                priorityObjectAdapter.type1a.setText(Html.fromHtml(textView,
                                        FROM_HTML_MODE_LEGACY));
                            } else {
                                priorityObjectAdapter.type1a.setVisibility(View.GONE);
                            }
                        } else {
                            priorityObjectAdapter.type1a.setVisibility(View.VISIBLE);
                            priorityObjectAdapter.type1a.setText(
                                    Html.fromHtml(prioritiesItem.getType1a(),
                                            FROM_HTML_MODE_LEGACY));
                        }

                        if (prioritiesItem.getType1b() == null) {
                            String metadata = Packages.getOverlayMetadata(
                                    this.mContext,
                                    packageTitle,
                                    References.metadataOverlayType1b);
                            if ((metadata != null) && !metadata.isEmpty()) {
                                metadata = metadata.replace("_", " ");
                                final String textView =
                                        "<b>" + this.mContext.getString(R.string.manager_type1b) +
                                                "</b> " + metadata;
                                priorityObjectAdapter.type1b.setVisibility(View.VISIBLE);
                                prioritiesItem.setType1b(textView);
                                priorityObjectAdapter.type1b.setText(Html.fromHtml(textView,
                                        FROM_HTML_MODE_LEGACY));
                            } else {
                                priorityObjectAdapter.type1b.setVisibility(View.GONE);
                            }
                        } else {
                            priorityObjectAdapter.type1b.setVisibility(View.VISIBLE);
                            priorityObjectAdapter.type1b.setText(
                                    Html.fromHtml(prioritiesItem.getType1b(),
                                            FROM_HTML_MODE_LEGACY));
                        }

                        if (prioritiesItem.getType1c() == null) {
                            String metadata = Packages.getOverlayMetadata(
                                    this.mContext,
                                    packageTitle,
                                    References.metadataOverlayType1c);
                            if ((metadata != null) && !metadata.isEmpty()) {
                                metadata = metadata.replace("_", " ");
                                final String textView =
                                        "<b>" + this.mContext.getString(R.string.manager_type1c) +
                                                "</b> " + metadata;
                                priorityObjectAdapter.type1c.setVisibility(View.VISIBLE);
                                prioritiesItem.setType1c(textView);
                                priorityObjectAdapter.type1c.setText(Html.fromHtml(textView,
                                        FROM_HTML_MODE_LEGACY));
                            } else {
                                priorityObjectAdapter.type1c.setVisibility(View.GONE);
                            }
                        } else {
                            priorityObjectAdapter.type1c.setVisibility(View.VISIBLE);
                            priorityObjectAdapter.type1c.setText(Html.fromHtml(
                                    prioritiesItem.getType1c(), FROM_HTML_MODE_LEGACY));
                        }

                        if (prioritiesItem.getType2() == null) {
                            String metadata = Packages.getOverlayMetadata(
                                    this.mContext,
                                    packageTitle,
                                    References.metadataOverlayType2);
                            if ((metadata != null) && !metadata.isEmpty()) {
                                metadata = metadata.replace("_", " ");
                                final String textView =
                                        "<b>" + this.mContext.getString(R.string.manager_type2) +
                                                "</b> " + metadata;
                                priorityObjectAdapter.type2.setVisibility(View.VISIBLE);
                                prioritiesItem.setType2(textView);
                                priorityObjectAdapter.type2.setText(Html.fromHtml(textView,
                                        FROM_HTML_MODE_LEGACY));
                            } else {
                                priorityObjectAdapter.type2.setVisibility(View.GONE);
                            }
                        } else {
                            priorityObjectAdapter.type2.setVisibility(View.VISIBLE);
                            priorityObjectAdapter.type2.setText(Html.fromHtml(
                                    prioritiesItem.getType2(), FROM_HTML_MODE_LEGACY));
                        }

                        if (prioritiesItem.getType3() == null) {
                            String metadata = Packages.getOverlayMetadata(
                                    this.mContext,
                                    packageTitle,
                                    References.metadataOverlayType3);
                            if ((metadata != null) && !metadata.isEmpty()) {
                                metadata = metadata.replace("_", " ");
                                final String textView =
                                        "<b>" + this.mContext.getString(R.string.manager_type3) +
                                                "</b> " + metadata;
                                priorityObjectAdapter.type3.setVisibility(View.VISIBLE);
                                prioritiesItem.setType3(textView);
                                priorityObjectAdapter.type3.setText(Html.fromHtml(textView,
                                        FROM_HTML_MODE_LEGACY));
                            } else {
                                priorityObjectAdapter.type3.setVisibility(View.GONE);
                            }
                        } else {
                            priorityObjectAdapter.type3.setVisibility(View.VISIBLE);
                            priorityObjectAdapter.type3.setText(
                                    Html.fromHtml(prioritiesItem.getType3(),
                                            FROM_HTML_MODE_LEGACY));
                        }

                        if (prioritiesItem.getThemeName() == null) {
                            final String metadata = Packages.getOverlayMetadata(
                                    this.mContext,
                                    packageTitle,
                                    References.metadataOverlayParent);
                            final Boolean newUpdate = (version != 0) && (version <= BuildConfig.VERSION_CODE);
                            if ((metadata != null) && !metadata.isEmpty() && newUpdate) {
                                final String pName = "<b>" +
                                        this.mContext.getString(R.string.manager_theme_name) + "</b> " +
                                        Packages.getPackageName(this.mContext, metadata);
                                priorityObjectAdapter.tvDesc.setVisibility(View.VISIBLE);
                                prioritiesItem.setThemeName(pName);
                                priorityObjectAdapter.tvDesc.setText(Html.fromHtml(pName,
                                        FROM_HTML_MODE_LEGACY));
                            } else {
                                prioritiesItem.setThemeName("");
                                priorityObjectAdapter.tvDesc.setText(packageTitle);
                            }
                        } else if (prioritiesItem.getThemeName().isEmpty()) {
                            priorityObjectAdapter.tvDesc.setText(packageTitle);
                        } else {
                            priorityObjectAdapter.tvDesc.setText(
                                    Html.fromHtml(prioritiesItem.getThemeName(),
                                            FROM_HTML_MODE_LEGACY));
                        }
                    } else {
                        priorityObjectAdapter.mCardText.setText(
                                String.format("%s (%s)", packageTitle, prioritiesItem.getName()));
                    }
                } else {
                    priorityObjectAdapter.mCardText.setText(
                            String.format("%s (%s)", packageTitle, prioritiesItem.getName()));
                }

                priorityObjectAdapter.mAppIcon.setImageDrawable(prioritiesItem.getDrawableId());
            } catch (final Exception e) {
                e.printStackTrace();
                // Suppress warning
            }
        } else {
            final HeaderViewAdapter headerViewAdapter = (HeaderViewAdapter) holder;
            final PrioritiesHeader prioritiesHeader = (PrioritiesHeader) prioritiesInterface;
            headerViewAdapter.mHeaderText.setText(prioritiesHeader.getName());
        }
    }

    @Override
    public int getItemViewType(final int position) {
        return this.getData().get(position).getType().ordinal();
    }

    private class PrioritiesHeader implements PrioritiesInterface {

        private final String mName;

        public PrioritiesHeader(final String name) {
            super();
            this.mName = name;
        }

        public CharSequence getName() {
            return this.mName;
        }

        @Override
        public PrioritiesItemType getType() {
            return PrioritiesItemType.HEADER;
        }
    }

    private class PriorityObjectAdapter extends GestureViewHolder {

        final TextView mCardText;
        final ImageView mAppIcon;
        final TextView tvDesc;
        final TextView type1a;
        final TextView type1b;
        final TextView type1c;
        final TextView type2;
        final TextView type3;
        final View view;
        private final ImageView mItemDrag;

        PriorityObjectAdapter(final View view) {
            super(view);
            this.view = view;
            this.mCardText = view.findViewById(R.id.card_text);
            this.mItemDrag = view.findViewById(R.id.card_drag);
            this.mAppIcon = view.findViewById(R.id.app_icon);
            this.tvDesc = view.findViewById(R.id.desc);
            this.type1a = view.findViewById(R.id.type1a);
            this.type1b = view.findViewById(R.id.type1b);
            this.type1c = view.findViewById(R.id.type1c);
            this.type2 = view.findViewById(R.id.type2);
            this.type3 = view.findViewById(R.id.type3);
        }

        @Nullable
        @Override
        public View getDraggableView() {
            return this.mItemDrag;
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

        final TextView mHeaderText;

        HeaderViewAdapter(final View view) {
            super(view);
            this.mHeaderText = view.findViewById(R.id.header_text);
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