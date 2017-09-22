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

package projekt.substratum.adapters.fragments.manager;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import projekt.substratum.R;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;

import static android.text.Html.FROM_HTML_MODE_LEGACY;

public class ManagerAdapter extends
        RecyclerView.Adapter<ManagerAdapter.ViewHolder> {

    private List<ManagerItem> overlayList;
    private Boolean floatui;

    public ManagerAdapter(List<ManagerItem> overlays, Boolean floatui) {
        this.floatui = floatui;
        this.overlayList = overlays;
    }

    @Override
    public ManagerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemLayoutView;
        if (floatui) {
            itemLayoutView = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.floatui_row, parent, false);
        } else {
            itemLayoutView = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.manager_row, parent, false);
        }
        return new ViewHolder(itemLayoutView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int position) {
        final int position_fixed = position;
        Context context = overlayList.get(position_fixed).getContext();
        String packageName = overlayList.get(position_fixed).getName();
        String targetPackage = Packages.getOverlayTarget(context, packageName);

        String title;
        if (packageName.startsWith("com.android.systemui.headers")) {
            title = context.getString(R.string.systemui_headers);
        } else if (packageName.startsWith("com.android.systemui.navbars")) {
            title = context.getString(R.string.systemui_navigation);
        } else if (packageName.startsWith("com.android.systemui.statusbars")) {
            title = context.getString(R.string.systemui_statusbar);
        } else if (packageName.startsWith("com.android.systemui.tiles")) {
            title = context.getString(R.string.systemui_qs_tiles);
        } else if (packageName.startsWith("com.android.settings.icons")) {
            title = context.getString(R.string.settings_icons);
        } else {
            title = Packages.getPackageName(context, targetPackage);
        }

        if (title != null && title.length() > 0) {
            viewHolder.tvName.setText(title);
        } else {
            viewHolder.tvName.setText(R.string.reboot_awaiting_manager_title);
        }

        viewHolder.tvName.setTextColor(overlayList.get(position_fixed).getActivationValue());

        if (overlayList.get(position_fixed).getType1a() == null) {
            String metadata = Packages.getOverlayMetadata(
                    context,
                    packageName,
                    References.metadataOverlayType1a);

            if (metadata != null && metadata.length() > 0) {
                metadata = metadata.replace("_", " ");
                String textView = "<b>" + context.getString(R.string.manager_type1a) +
                        "</b> " + metadata;
                viewHolder.type1a.setVisibility(View.VISIBLE);
                overlayList.get(position_fixed).setType1a(textView);
                viewHolder.type1a.setText(Html.fromHtml(textView, FROM_HTML_MODE_LEGACY));
            } else {
                viewHolder.type1a.setVisibility(View.GONE);
            }
        } else {
            viewHolder.type1a.setVisibility(View.VISIBLE);
            viewHolder.type1a.setText(Html.fromHtml(overlayList.get(position_fixed).getType1a(),
                    FROM_HTML_MODE_LEGACY));
        }

        if (overlayList.get(position_fixed).getType1b() == null) {
            String metadata = Packages.getOverlayMetadata(
                    context,
                    packageName,
                    References.metadataOverlayType1b);
            if (metadata != null && metadata.length() > 0) {
                metadata = metadata.replace("_", " ");
                String textView = "<b>" + context.getString(R.string.manager_type1b) +
                        "</b> " + metadata;
                viewHolder.type1b.setVisibility(View.VISIBLE);
                overlayList.get(position_fixed).setType1b(textView);
                viewHolder.type1b.setText(Html.fromHtml(textView, FROM_HTML_MODE_LEGACY));
            } else {
                viewHolder.type1b.setVisibility(View.GONE);
            }
        } else {
            viewHolder.type1b.setVisibility(View.VISIBLE);
            viewHolder.type1b.setText(Html.fromHtml(overlayList.get(position_fixed).getType1b(),
                    FROM_HTML_MODE_LEGACY));
        }

        if (overlayList.get(position_fixed).getType1c() == null) {
            String metadata = Packages.getOverlayMetadata(
                    context,
                    packageName,
                    References.metadataOverlayType1c);
            if (metadata != null && metadata.length() > 0) {
                metadata = metadata.replace("_", " ");
                String textView = "<b>" + context.getString(R.string.manager_type1c) +
                        "</b> " + metadata;
                viewHolder.type1c.setVisibility(View.VISIBLE);
                overlayList.get(position_fixed).setType1c(textView);
                viewHolder.type1c.setText(Html.fromHtml(textView, FROM_HTML_MODE_LEGACY));
            } else {
                viewHolder.type1c.setVisibility(View.GONE);
            }
        } else {
            viewHolder.type1c.setVisibility(View.VISIBLE);
            viewHolder.type1c.setText(Html.fromHtml(overlayList.get(position_fixed).getType1c(),
                    FROM_HTML_MODE_LEGACY));
        }

        if (overlayList.get(position_fixed).getType2() == null) {
            String metadata = Packages.getOverlayMetadata(
                    context,
                    packageName,
                    References.metadataOverlayType2);
            if (metadata != null && metadata.length() > 0) {
                metadata = metadata.replace("_", " ");
                String textView = "<b>" + context.getString(R.string.manager_type2) +
                        "</b> " + metadata;
                viewHolder.type2.setVisibility(View.VISIBLE);
                overlayList.get(position_fixed).setType2(textView);
                viewHolder.type2.setText(Html.fromHtml(textView, FROM_HTML_MODE_LEGACY));
            } else {
                viewHolder.type2.setVisibility(View.GONE);
            }
        } else {
            viewHolder.type2.setVisibility(View.VISIBLE);
            viewHolder.type2.setText(Html.fromHtml(overlayList.get(position_fixed).getType2(),
                    FROM_HTML_MODE_LEGACY));
        }

        if (overlayList.get(position_fixed).getType3() == null) {
            String metadata = Packages.getOverlayMetadata(
                    context,
                    packageName,
                    References.metadataOverlayType3);
            if (metadata != null && metadata.length() > 0) {
                metadata = metadata.replace("_", " ");
                String textView = "<b>" + context.getString(R.string.manager_type3) +
                        "</b> " + metadata;
                viewHolder.type3.setVisibility(View.VISIBLE);
                overlayList.get(position_fixed).setType3(textView);
                viewHolder.type3.setText(Html.fromHtml(textView, FROM_HTML_MODE_LEGACY));
            } else {
                viewHolder.type3.setVisibility(View.GONE);
            }
        } else {
            viewHolder.type3.setVisibility(View.VISIBLE);
            viewHolder.type3.setText(Html.fromHtml(overlayList.get(position_fixed).getType3(),
                    FROM_HTML_MODE_LEGACY));
        }

        if (overlayList.get(position_fixed).getType4() == null) {
            String metadata = Packages.getOverlayMetadata(
                    context,
                    packageName,
                    References.metadataOverlayType4);
            if (metadata != null && metadata.length() > 0) {
                metadata = metadata.replace("_", " ");
                String textView = "<b>" + context.getString(R.string.manager_type4) +
                        "</b> " + metadata;
                viewHolder.type4.setVisibility(View.VISIBLE);
                overlayList.get(position_fixed).setType4(textView);
                viewHolder.type4.setText(Html.fromHtml(textView, FROM_HTML_MODE_LEGACY));
            } else {
                viewHolder.type4.setVisibility(View.GONE);
            }
        } else {
            viewHolder.type4.setVisibility(View.VISIBLE);
            viewHolder.type4.setText(Html.fromHtml(overlayList.get(position_fixed).getType4(),
                    FROM_HTML_MODE_LEGACY));
        }

        String textView = "<b>" + context.getString(R.string.manager_version) +
                "</b> " +
                String.valueOf(
                        Packages.getOverlaySubstratumVersion(
                                context,
                                overlayList.get(position_fixed)
                                        .getName()));
        viewHolder.version.setText(Html.fromHtml(textView, FROM_HTML_MODE_LEGACY));

        if (overlayList.get(position_fixed).getThemeName().length() == 0) {
            viewHolder.tvDesc.setText(packageName);
        } else {
            viewHolder.tvDesc.setText(
                    Html.fromHtml(overlayList.get(position_fixed).getThemeName(),
                            FROM_HTML_MODE_LEGACY));
        }

        viewHolder.chkSelected.setChecked(overlayList.get(position_fixed).isSelected());
        viewHolder.chkSelected.setTag(overlayList.get(position_fixed));
        viewHolder.chkSelected.setOnClickListener(view -> {
            CheckBox checkBox = (CheckBox) view;
            ManagerItem contact = (ManagerItem) checkBox.getTag();

            contact.setSelected(checkBox.isChecked());
            overlayList.get(position_fixed).setSelected(checkBox.isChecked());
        });
        viewHolder.card.setOnClickListener(view -> {
            viewHolder.chkSelected.setChecked(!viewHolder.chkSelected.isChecked());

            CheckBox cb = viewHolder.chkSelected;
            ManagerItem contact = (ManagerItem) cb.getTag();

            contact.setSelected(cb.isChecked());
            contact.setSelected(cb.isChecked());
        });
        if (overlayList.get(position_fixed).getDrawable() == null) {
            Drawable app_icon = Packages.getAppIcon(
                    overlayList.get(position_fixed).getContext(),
                    Packages.getOverlayParent(
                            overlayList.get(position_fixed).getContext(),
                            overlayList.get(position_fixed).getName()));
            overlayList.get(position_fixed).setDrawable(app_icon);
            viewHolder.appIcon.setImageDrawable(app_icon);
        } else {
            viewHolder.appIcon.setImageDrawable(overlayList.get(position_fixed).getDrawable());
        }
        if (overlayList.get(position_fixed).getTargetDrawable() == null) {
            Drawable app_icon = Packages.getAppIcon(
                    overlayList.get(position_fixed).getContext(),
                    Packages.getOverlayTarget(
                            overlayList.get(position_fixed).getContext(),
                            overlayList.get(position_fixed).getName()));
            overlayList.get(position_fixed).setTargetDrawable(app_icon);
            viewHolder.appIconTarget.setImageDrawable(app_icon);
        } else {
            viewHolder.appIconTarget.setImageDrawable(
                    overlayList.get(position_fixed).getTargetDrawable());
        }
    }

    @Override
    public int getItemCount() {
        return overlayList.size();
    }

    public List<ManagerItem> getOverlayManagerList() {
        return overlayList;
    }

    public void setOverlayManagerList(List<ManagerItem> overlayList) {
        this.overlayList = overlayList;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvDesc;
        CheckBox chkSelected;
        CardView card;
        ImageView appIcon;
        ImageView appIconTarget;
        TextView type1a;
        TextView type1b;
        TextView type1c;
        TextView type2;
        TextView type3;
        TextView type4;
        TextView version;

        ViewHolder(View itemLayoutView) {
            super(itemLayoutView);
            tvName = itemLayoutView.findViewById(R.id.tvName);
            tvDesc = itemLayoutView.findViewById(R.id.desc);
            card = itemLayoutView.findViewById(R.id.overlayCard);
            chkSelected = itemLayoutView.findViewById(R.id.chkSelected);
            appIcon = itemLayoutView.findViewById(R.id.app_icon);
            appIconTarget = itemLayoutView.findViewById(R.id.app_icon_sub);
            type1a = itemLayoutView.findViewById(R.id.type1a);
            type1b = itemLayoutView.findViewById(R.id.type1b);
            type1c = itemLayoutView.findViewById(R.id.type1c);
            type2 = itemLayoutView.findViewById(R.id.type2);
            type3 = itemLayoutView.findViewById(R.id.type3);
            type4 = itemLayoutView.findViewById(R.id.type4);
            version = itemLayoutView.findViewById(R.id.version);
        }
    }
}