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

    private Boolean floatui;
    private List<ManagerItem> overlayList;

    public ManagerAdapter(List<ManagerItem> overlays,
                          Boolean floatui) {
        super();
        this.floatui = floatui;
        this.overlayList = overlays;
    }

    @Override
    public ManagerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                        int viewType) {
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
    public void onBindViewHolder(ViewHolder viewHolder,
                                 int position) {
        Context context = overlayList.get(position).getContext();
        String packageName = overlayList.get(position).getName();

        String title = overlayList.get(position).getLabelName();

        if ((title != null) && !title.isEmpty()) {
            viewHolder.tvName.setText(title);
        } else {
            viewHolder.tvName.setText(R.string.reboot_awaiting_manager_title);
        }

        viewHolder.tvName.setTextColor(overlayList.get(position).getActivationValue());

        if (overlayList.get(position).getType1a() == null) {
            String metadata = Packages.getOverlayMetadata(
                    context,
                    packageName,
                    References.metadataOverlayType1a);

            if ((metadata != null) && !metadata.isEmpty()) {
                metadata = metadata.replace("_", " ");
                String textView = "<b>" + context.getString(R.string.manager_type1a) +
                        "</b> " + metadata;
                viewHolder.type1a.setVisibility(View.VISIBLE);
                overlayList.get(position).setType1a(textView);
                viewHolder.type1a.setText(Html.fromHtml(textView, FROM_HTML_MODE_LEGACY));
            } else {
                viewHolder.type1a.setVisibility(View.GONE);
            }
        } else {
            viewHolder.type1a.setVisibility(View.VISIBLE);
            viewHolder.type1a.setText(Html.fromHtml(overlayList.get(position).getType1a(),
                    FROM_HTML_MODE_LEGACY));
        }

        if (overlayList.get(position).getType1b() == null) {
            String metadata = Packages.getOverlayMetadata(
                    context,
                    packageName,
                    References.metadataOverlayType1b);
            if ((metadata != null) && !metadata.isEmpty()) {
                metadata = metadata.replace("_", " ");
                String textView = "<b>" + context.getString(R.string.manager_type1b) +
                        "</b> " + metadata;
                viewHolder.type1b.setVisibility(View.VISIBLE);
                overlayList.get(position).setType1b(textView);
                viewHolder.type1b.setText(Html.fromHtml(textView, FROM_HTML_MODE_LEGACY));
            } else {
                viewHolder.type1b.setVisibility(View.GONE);
            }
        } else {
            viewHolder.type1b.setVisibility(View.VISIBLE);
            viewHolder.type1b.setText(Html.fromHtml(overlayList.get(position).getType1b(),
                    FROM_HTML_MODE_LEGACY));
        }

        if (overlayList.get(position).getType1c() == null) {
            String metadata = Packages.getOverlayMetadata(
                    context,
                    packageName,
                    References.metadataOverlayType1c);
            if ((metadata != null) && !metadata.isEmpty()) {
                metadata = metadata.replace("_", " ");
                String textView = "<b>" + context.getString(R.string.manager_type1c) +
                        "</b> " + metadata;
                viewHolder.type1c.setVisibility(View.VISIBLE);
                overlayList.get(position).setType1c(textView);
                viewHolder.type1c.setText(Html.fromHtml(textView, FROM_HTML_MODE_LEGACY));
            } else {
                viewHolder.type1c.setVisibility(View.GONE);
            }
        } else {
            viewHolder.type1c.setVisibility(View.VISIBLE);
            viewHolder.type1c.setText(Html.fromHtml(overlayList.get(position).getType1c(),
                    FROM_HTML_MODE_LEGACY));
        }

        if (overlayList.get(position).getType2() == null) {
            String metadata = Packages.getOverlayMetadata(
                    context,
                    packageName,
                    References.metadataOverlayType2);
            if ((metadata != null) && !metadata.isEmpty()) {
                metadata = metadata.replace("_", " ");
                String textView = "<b>" + context.getString(R.string.manager_type2) +
                        "</b> " + metadata;
                viewHolder.type2.setVisibility(View.VISIBLE);
                overlayList.get(position).setType2(textView);
                viewHolder.type2.setText(Html.fromHtml(textView, FROM_HTML_MODE_LEGACY));
            } else {
                viewHolder.type2.setVisibility(View.GONE);
            }
        } else {
            viewHolder.type2.setVisibility(View.VISIBLE);
            viewHolder.type2.setText(Html.fromHtml(overlayList.get(position).getType2(),
                    FROM_HTML_MODE_LEGACY));
        }

        if (overlayList.get(position).getType3() == null) {
            String metadata = Packages.getOverlayMetadata(
                    context,
                    packageName,
                    References.metadataOverlayType3);
            if ((metadata != null) && !metadata.isEmpty()) {
                metadata = metadata.replace("_", " ");
                String textView = "<b>" + context.getString(R.string.manager_type3) +
                        "</b> " + metadata;
                viewHolder.type3.setVisibility(View.VISIBLE);
                overlayList.get(position).setType3(textView);
                viewHolder.type3.setText(Html.fromHtml(textView, FROM_HTML_MODE_LEGACY));
            } else {
                viewHolder.type3.setVisibility(View.GONE);
            }
        } else {
            viewHolder.type3.setVisibility(View.VISIBLE);
            viewHolder.type3.setText(Html.fromHtml(overlayList.get(position).getType3(),
                    FROM_HTML_MODE_LEGACY));
        }

        if (overlayList.get(position).getType4() == null) {
            String metadata = Packages.getOverlayMetadata(
                    context,
                    packageName,
                    References.metadataOverlayType4);
            if ((metadata != null) && !metadata.isEmpty()) {
                metadata = metadata.replace("_", " ");
                String textView = "<b>" + context.getString(R.string.manager_type4) +
                        "</b> " + metadata;
                viewHolder.type4.setVisibility(View.VISIBLE);
                overlayList.get(position).setType4(textView);
                viewHolder.type4.setText(Html.fromHtml(textView, FROM_HTML_MODE_LEGACY));
            } else {
                viewHolder.type4.setVisibility(View.GONE);
            }
        } else {
            viewHolder.type4.setVisibility(View.VISIBLE);
            viewHolder.type4.setText(Html.fromHtml(overlayList.get(position).getType4(),
                    FROM_HTML_MODE_LEGACY));
        }

        String textView = "<b>" + context.getString(R.string.manager_version) +
                "</b> " +
                String.valueOf(
                        Packages.getOverlaySubstratumVersion(
                                context,
                                overlayList.get(position)
                                        .getName()));
        viewHolder.version.setText(Html.fromHtml(textView, FROM_HTML_MODE_LEGACY));

        if (overlayList.get(position).getThemeName().isEmpty()) {
            viewHolder.tvDesc.setText(packageName);
        } else {
            viewHolder.tvDesc.setText(
                    Html.fromHtml(overlayList.get(position).getThemeName(),
                            FROM_HTML_MODE_LEGACY));
        }

        viewHolder.chkSelected.setChecked(overlayList.get(position).isSelected());
        viewHolder.chkSelected.setTag(overlayList.get(position));
        viewHolder.chkSelected.setOnClickListener(view -> {
            CheckBox checkBox = (CheckBox) view;
            ManagerItem contact = (ManagerItem) checkBox.getTag();

            contact.setSelected(checkBox.isChecked());
            overlayList.get(position).setSelected(checkBox.isChecked());
        });
        viewHolder.card.setOnClickListener(view -> {
            viewHolder.chkSelected.setChecked(!viewHolder.chkSelected.isChecked());

            CheckBox cb = viewHolder.chkSelected;
            ManagerItem contact = (ManagerItem) cb.getTag();

            contact.setSelected(cb.isChecked());
            contact.setSelected(cb.isChecked());
        });
        if (overlayList.get(position).getDrawable() == null) {
            Drawable app_icon = Packages.getAppIcon(
                    overlayList.get(position).getContext(),
                    Packages.getOverlayParent(
                            overlayList.get(position).getContext(),
                            overlayList.get(position).getName()));
            overlayList.get(position).setDrawable(app_icon);
            viewHolder.appIcon.setImageDrawable(app_icon);
        } else {
            viewHolder.appIcon.setImageDrawable(overlayList.get(position).getDrawable());
        }
        if (overlayList.get(position).getTargetDrawable() == null) {
            Drawable app_icon = Packages.getAppIcon(
                    overlayList.get(position).getContext(),
                    Packages.getOverlayTarget(
                            overlayList.get(position).getContext(),
                            overlayList.get(position).getName()));
            overlayList.get(position).setTargetDrawable(app_icon);
            viewHolder.appIconTarget.setImageDrawable(app_icon);
        } else {
            viewHolder.appIconTarget.setImageDrawable(
                    overlayList.get(position).getTargetDrawable());
        }
    }

    @Override
    public int getItemCount() {
        return overlayList.size();
    }

    public List<ManagerItem> getOverlayManagerList() {
        return overlayList;
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
            this.tvName = itemLayoutView.findViewById(R.id.tvName);
            this.tvDesc = itemLayoutView.findViewById(R.id.desc);
            this.card = itemLayoutView.findViewById(R.id.overlayCard);
            this.chkSelected = itemLayoutView.findViewById(R.id.chkSelected);
            this.appIcon = itemLayoutView.findViewById(R.id.app_icon);
            this.appIconTarget = itemLayoutView.findViewById(R.id.app_icon_sub);
            this.type1a = itemLayoutView.findViewById(R.id.type1a);
            this.type1b = itemLayoutView.findViewById(R.id.type1b);
            this.type1c = itemLayoutView.findViewById(R.id.type1c);
            this.type2 = itemLayoutView.findViewById(R.id.type2);
            this.type3 = itemLayoutView.findViewById(R.id.type3);
            this.type4 = itemLayoutView.findViewById(R.id.type4);
            this.version = itemLayoutView.findViewById(R.id.version);
        }
    }
}