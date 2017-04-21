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

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import projekt.substratum.R;
import projekt.substratum.common.References;

public class ManagerAdapter extends
        RecyclerView.Adapter<ManagerAdapter.ViewHolder> {

    private List<ManagerItem> overlayList;

    public ManagerAdapter(List<ManagerItem> overlays) {
        this.overlayList = overlays;
    }

    @Override
    public ManagerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemLayoutView = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.overlay_manager_row, parent, false);
        return new ViewHolder(itemLayoutView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int position) {
        final int position_fixed = position;
        viewHolder.tvName.setText(
                References.grabPackageName(
                        overlayList.get(position_fixed).getContext(),
                        References.grabOverlayTarget(
                                overlayList.get(position_fixed).getContext(),
                                overlayList.get(position_fixed).getName())));
        viewHolder.tvDesc.setText(overlayList.get(position_fixed).getName());
        viewHolder.tvName.setTextColor(overlayList.get(position_fixed).getActivationValue());
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
        viewHolder.appIcon.setImageDrawable(References.grabAppIcon(
                overlayList.get(position_fixed).getContext(),
                References.grabOverlayParent(
                        overlayList.get(position_fixed).getContext(),
                        overlayList.get(position_fixed).getName())));
        viewHolder.appIconTarget.setImageDrawable(References.grabAppIcon(
                overlayList.get(position_fixed).getContext(),
                References.grabOverlayTarget(
                        overlayList.get(position_fixed).getContext(),
                        overlayList.get(position_fixed).getName())));
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

        ViewHolder(View itemLayoutView) {
            super(itemLayoutView);
            tvName = (TextView) itemLayoutView.findViewById(R.id.tvName);
            tvDesc = (TextView) itemLayoutView.findViewById(R.id.tvDesc);
            card = (CardView) itemLayoutView.findViewById(R.id.overlayCard);
            chkSelected = (CheckBox) itemLayoutView.findViewById(R.id.chkSelected);
            appIcon = (ImageView) itemLayoutView.findViewById(R.id.app_icon);
            appIconTarget = (ImageView) itemLayoutView.findViewById(R.id.app_icon_sub);
        }
    }
}