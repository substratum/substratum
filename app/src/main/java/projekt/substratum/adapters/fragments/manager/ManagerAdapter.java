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

import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import java.util.List;

import projekt.substratum.R;
import projekt.substratum.common.Packages;
import projekt.substratum.databinding.ManagerItemBinding;

public class ManagerAdapter extends
        RecyclerView.Adapter<ManagerAdapter.ViewHolder> {

    private List<ManagerItem> overlayList;

    public ManagerAdapter(List<ManagerItem> overlays) {
        super();
        this.overlayList = overlays;
    }

    @Override
    public ManagerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                        int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(
                R.layout.manager_item, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder,
                                 int position) {
        final ManagerItem managerItem = overlayList.get(position);
        ManagerItemBinding viewHolderBinding = viewHolder.getBinding();

        viewHolderBinding.tvName.setTextColor(overlayList.get(position).getActivationValue());

        viewHolderBinding.chkSelected.setChecked(overlayList.get(position).isSelected());
        viewHolderBinding.chkSelected.setTag(overlayList.get(position));
        viewHolderBinding.chkSelected.setOnClickListener(view -> {
            CheckBox checkBox = (CheckBox) view;
            ManagerItem contact = (ManagerItem) checkBox.getTag();

            contact.setSelected(checkBox.isChecked());
            overlayList.get(position).setSelected(checkBox.isChecked());
        });
        viewHolderBinding.overlayCard.setOnClickListener(view -> {
            viewHolderBinding.chkSelected.setChecked(!viewHolderBinding.chkSelected.isChecked());

            CheckBox cb = viewHolderBinding.chkSelected;
            ManagerItem contact = (ManagerItem) cb.getTag();

            contact.setSelected(cb.isChecked());
            contact.setSelected(cb.isChecked());
        });
        if (overlayList.get(position).getDrawable() == null) {
            Drawable appIcon = Packages.getAppIcon(
                    overlayList.get(position).getContext(),
                    Packages.getOverlayParent(
                            overlayList.get(position).getContext(),
                            overlayList.get(position).getName()));
            overlayList.get(position).setDrawable(appIcon);
            viewHolderBinding.appIcon.setImageDrawable(appIcon);
        } else {
            viewHolderBinding.appIcon.setImageDrawable(overlayList.get(position).getDrawable());
        }
        if (overlayList.get(position).getTargetDrawable() == null) {
            Drawable appIcon = Packages.getAppIcon(
                    overlayList.get(position).getContext(),
                    Packages.getOverlayTarget(
                            overlayList.get(position).getContext(),
                            overlayList.get(position).getName()));
            overlayList.get(position).setTargetDrawable(appIcon);
            viewHolderBinding.appIconSub.setImageDrawable(appIcon);
        } else {
            viewHolderBinding.appIconSub.setImageDrawable(
                    overlayList.get(position).getTargetDrawable());
        }
        viewHolderBinding.setOverlay(managerItem);
        viewHolderBinding.executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return overlayList.size();
    }

    public List<ManagerItem> getOverlayManagerList() {
        return overlayList;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ManagerItemBinding binding;

        ViewHolder(View itemLayoutView) {
            super(itemLayoutView);
            binding = DataBindingUtil.bind(itemLayoutView);
        }

        ManagerItemBinding getBinding() {
            return binding;
        }
    }
}