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
import android.graphics.Typeface;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
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
import projekt.substratum.util.files.StringUtils;

import static projekt.substratum.common.References.metadataOverlayDevice;
import static projekt.substratum.common.Resources.LG_FRAMEWORK;
import static projekt.substratum.common.Resources.SAMSUNG_FRAMEWORK;
import static projekt.substratum.common.Resources.SETTINGS_ICONS;
import static projekt.substratum.common.Resources.SYSTEMUI_HEADERS;
import static projekt.substratum.common.Resources.SYSTEMUI_NAVBARS;
import static projekt.substratum.common.Resources.SYSTEMUI_QSTILES;
import static projekt.substratum.common.Resources.SYSTEMUI_STATUSBARS;

public class PriorityAdapter extends GestureAdapter<PrioritiesInterface, GestureViewHolder> {

    private final Context context;
    private final int mItemResId;

    public PriorityAdapter(Context context,
                           @LayoutRes int itemResId) {
        super();
        this.context = context;
        mItemResId = itemResId;
    }

    @Override
    public GestureViewHolder onCreateViewHolder(ViewGroup parent,
                                                int viewType) {
        if (viewType == PrioritiesInterface.PrioritiesItemType.CONTENT.ordinal()) {
            View itemView = LayoutInflater.from(
                    parent.getContext()).inflate(mItemResId, parent, false);
            return new PriorityObjectAdapter(itemView);
        } else {
            View itemView = LayoutInflater.from(
                    parent.getContext()).inflate(R.layout.priority_item, parent, false);
            return new HeaderViewAdapter(itemView);
        }
    }

    @Override
    public void onBindViewHolder(GestureViewHolder holder,
                                 int position) {
        super.onBindViewHolder(holder, position);
        PrioritiesInterface prioritiesInterface = getData().get(position);

        if (prioritiesInterface.getType() == PrioritiesInterface.PrioritiesItemType.CONTENT) {
            PriorityObjectAdapter priorityObjectAdapter = (PriorityObjectAdapter) holder;
            PrioritiesItem prioritiesItem = (PrioritiesItem) prioritiesInterface;

            try {
                // Keep this value but do not display it to the user, instead, parse it
                ApplicationInfo applicationInfo = context.getPackageManager()
                        .getApplicationInfo(prioritiesItem.getName(), PackageManager.GET_META_DATA);
                String packageTitle = context.getPackageManager()
                        .getApplicationLabel(applicationInfo).toString();
                String targetPackage = Packages.getOverlayTarget(
                        context, prioritiesItem.getName());
                String title;
                if (packageTitle.startsWith(SYSTEMUI_HEADERS)) {
                    title = context.getString(R.string.systemui_headers);
                } else if (packageTitle.startsWith(SYSTEMUI_NAVBARS)) {
                    title = context.getString(R.string.systemui_navigation);
                } else if (packageTitle.startsWith(SYSTEMUI_STATUSBARS)) {
                    title = context.getString(R.string.systemui_statusbar);
                } else if (packageTitle.startsWith(SYSTEMUI_QSTILES)) {
                    title = context.getString(R.string.systemui_qs_tiles);
                } else if (packageTitle.startsWith(SETTINGS_ICONS)) {
                    title = context.getString(R.string.settings_icons);
                } else if (packageTitle.startsWith(SAMSUNG_FRAMEWORK)) {
                    title = context.getString(R.string.samsung_framework);
                } else if (packageTitle.startsWith(LG_FRAMEWORK)) {
                    title = context.getString(R.string.lg_framework);
                } else {
                    title = Packages.getPackageName(context, targetPackage);
                }

                if ((title != null) && !title.isEmpty()) {
                    priorityObjectAdapter.mCardText.setText(title);
                } else {
                    priorityObjectAdapter.mCardText.setText(R.string.reboot_awaiting_manager_title);
                }

                if (applicationInfo.metaData != null) {
                    if (applicationInfo.metaData.getString(metadataOverlayDevice) != null) {
                        int version = Packages.getOverlaySubstratumVersion(
                                context,
                                packageTitle);

                        if (prioritiesItem.getType1a() == null) {
                            String metadata = Packages.getOverlayMetadata(
                                    context,
                                    packageTitle,
                                    References.metadataOverlayType1a);
                            if ((metadata != null) && !metadata.isEmpty()) {
                                metadata = metadata.replace("_", " ");
                                SpannableStringBuilder type1a = StringUtils.format(context.getString(
                                        R.string.manager_type1a), metadata, Typeface.BOLD);
                                priorityObjectAdapter.type1a.setVisibility(View.VISIBLE);
                                prioritiesItem.setType1a(type1a.toString());
                                priorityObjectAdapter.type1a.setText(type1a);
                            } else {
                                priorityObjectAdapter.type1a.setVisibility(View.GONE);
                            }
                        } else {
                            priorityObjectAdapter.type1a.setVisibility(View.VISIBLE);
                            priorityObjectAdapter.type1a.setText((prioritiesItem.getType1a()));
                        }

                        if (prioritiesItem.getType1b() == null) {
                            String metadata = Packages.getOverlayMetadata(
                                    context,
                                    packageTitle,
                                    References.metadataOverlayType1b);
                            if ((metadata != null) && !metadata.isEmpty()) {
                                metadata = metadata.replace("_", " ");
                                SpannableStringBuilder type1b = StringUtils.format(context.getString(
                                        R.string.manager_type1b), metadata, Typeface.BOLD);
                                priorityObjectAdapter.type1b.setVisibility(View.VISIBLE);
                                prioritiesItem.setType1b(type1b.toString());
                                priorityObjectAdapter.type1b.setText(type1b);
                            } else {
                                priorityObjectAdapter.type1b.setVisibility(View.GONE);
                            }
                        } else {
                            priorityObjectAdapter.type1b.setVisibility(View.VISIBLE);
                            priorityObjectAdapter.type1b.setText(prioritiesItem.getType1b());
                        }

                        if (prioritiesItem.getType1c() == null) {
                            String metadata = Packages.getOverlayMetadata(
                                    context,
                                    packageTitle,
                                    References.metadataOverlayType1c);
                            if ((metadata != null) && !metadata.isEmpty()) {
                                metadata = metadata.replace("_", " ");
                                SpannableStringBuilder type1c = StringUtils.format(context.getString(
                                        R.string.manager_type1c), metadata, Typeface.BOLD);
                                priorityObjectAdapter.type1c.setVisibility(View.VISIBLE);
                                prioritiesItem.setType1c(type1c.toString());
                                priorityObjectAdapter.type1c.setText(type1c);
                            } else {
                                priorityObjectAdapter.type1c.setVisibility(View.GONE);
                            }
                        } else {
                            priorityObjectAdapter.type1c.setVisibility(View.VISIBLE);
                            priorityObjectAdapter.type1c.setText(prioritiesItem.getType1c());
                        }

                        if (prioritiesItem.getType2() == null) {
                            String metadata = Packages.getOverlayMetadata(
                                    context,
                                    packageTitle,
                                    References.metadataOverlayType2);
                            if ((metadata != null) && !metadata.isEmpty()) {
                                metadata = metadata.replace("_", " ");
                                SpannableStringBuilder type2 = StringUtils.format(context.getString(
                                        R.string.manager_type2), metadata, Typeface.BOLD);
                                priorityObjectAdapter.type2.setVisibility(View.VISIBLE);
                                prioritiesItem.setType2(type2.toString());
                                priorityObjectAdapter.type2.setText(type2);
                            } else {
                                priorityObjectAdapter.type2.setVisibility(View.GONE);
                            }
                        } else {
                            priorityObjectAdapter.type2.setVisibility(View.VISIBLE);
                            priorityObjectAdapter.type2.setText(prioritiesItem.getType2());
                        }

                        if (prioritiesItem.getType3() == null) {
                            String metadata = Packages.getOverlayMetadata(
                                    context,
                                    packageTitle,
                                    References.metadataOverlayType3);
                            if ((metadata != null) && !metadata.isEmpty()) {
                                metadata = metadata.replace("_", " ");
                                SpannableStringBuilder type3 = StringUtils.format(context.getString(
                                        R.string.manager_type3), metadata, Typeface.BOLD);
                                priorityObjectAdapter.type3.setVisibility(View.VISIBLE);
                                prioritiesItem.setType3(type3.toString());
                                priorityObjectAdapter.type3.setText(type3);
                            } else {
                                priorityObjectAdapter.type3.setVisibility(View.GONE);
                            }
                        } else {
                            priorityObjectAdapter.type3.setVisibility(View.VISIBLE);
                            priorityObjectAdapter.type3.setText(prioritiesItem.getType3());
                        }

                        if (prioritiesItem.getThemeName() == null) {
                            String metadata = Packages.getOverlayMetadata(
                                    context,
                                    packageTitle,
                                    References.metadataOverlayParent);
                            Boolean newUpdate = (version != 0) && (version <= BuildConfig
                                    .VERSION_CODE);
                            if ((metadata != null) && !metadata.isEmpty() && newUpdate) {
                                SpannableStringBuilder themeName = StringUtils.format(context.getString(
                                        R.string.manager_theme_name), metadata, Typeface.BOLD);
                                priorityObjectAdapter.tvDesc.setVisibility(View.VISIBLE);
                                prioritiesItem.setThemeName(themeName.toString());
                                priorityObjectAdapter.tvDesc.setText(themeName);
                            } else {
                                prioritiesItem.setThemeName("");
                                priorityObjectAdapter.tvDesc.setText(packageTitle);
                            }
                        } else if (prioritiesItem.getThemeName().isEmpty()) {
                            priorityObjectAdapter.tvDesc.setText(packageTitle);
                        } else {
                            priorityObjectAdapter.tvDesc.setText(prioritiesItem.getThemeName());
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
            } catch (Exception e) {
                e.printStackTrace();
                // Suppress warning
            }
        } else {
            HeaderViewAdapter headerViewAdapter = (HeaderViewAdapter) holder;
            PrioritiesHeader prioritiesHeader = (PrioritiesHeader) prioritiesInterface;
            headerViewAdapter.mHeaderText.setText(prioritiesHeader.getName());
        }
    }

    @Override
    public int getItemViewType(int position) {
        return getData().get(position).getType().ordinal();
    }

    private static class PrioritiesHeader implements PrioritiesInterface {

        private final String mName;

        public PrioritiesHeader(String name) {
            super();
            mName = name;
        }

        public CharSequence getName() {
            return mName;
        }

        @Override
        public PrioritiesItemType getType() {
            return PrioritiesItemType.HEADER;
        }
    }

    private static class PriorityObjectAdapter extends GestureViewHolder {

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

        PriorityObjectAdapter(View view) {
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

    private static class HeaderViewAdapter extends GestureViewHolder {

        final TextView mHeaderText;

        HeaderViewAdapter(View view) {
            super(view);
            mHeaderText = view.findViewById(R.id.header_text);
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