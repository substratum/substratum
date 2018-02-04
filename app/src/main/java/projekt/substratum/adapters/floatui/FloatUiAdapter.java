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

package projekt.substratum.adapters.floatui;

import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import projekt.substratum.R;
import projekt.substratum.common.Packages;
import projekt.substratum.databinding.FloatuiRowBinding;

public class FloatUiAdapter extends RecyclerView.Adapter<FloatUiAdapter.ViewHolder> {

    private List<FloatUiItem> overlayList;

    public FloatUiAdapter(List<FloatUiItem> overlays) {
        super();
        this.overlayList = overlays;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
                                         int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(
                R.layout.floatui_row, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder,
                                 int position) {
        final FloatUiItem floatUiItem = overlayList.get(position);
        FloatuiRowBinding viewBinding = viewHolder.getBinding();
        viewBinding.setOverlay(floatUiItem);
        viewBinding.executePendingBindings();

        viewBinding.tvName.setTextColor(floatUiItem.getActivationValue());
        viewBinding.chkSelected.setChecked(floatUiItem.isSelected());
        viewBinding.chkSelected.setTag(floatUiItem);
        viewBinding.chkSelected.setOnClickListener(view ->
                floatUiItem.setSelected(!floatUiItem.isSelected()));
        viewBinding.overlayCard.setOnClickListener(view -> {
            viewBinding.chkSelected.setChecked(!viewBinding.chkSelected.isChecked());
            floatUiItem.setSelected(!floatUiItem.isSelected());
        });
        if (floatUiItem.getDrawable() == null) {
            Drawable app_icon = Packages.getAppIcon(
                    floatUiItem.getContext(),
                    Packages.getOverlayParent(
                            floatUiItem.getContext(),
                            floatUiItem.getName()));
            floatUiItem.setDrawable(app_icon);
            viewBinding.appIcon.setImageDrawable(app_icon);
        } else {
            viewBinding.appIcon.setImageDrawable(floatUiItem.getDrawable());
        }
        if (floatUiItem.getTargetDrawable() == null) {
            Drawable app_icon = Packages.getAppIcon(
                    floatUiItem.getContext(),
                    Packages.getOverlayTarget(
                            floatUiItem.getContext(),
                            floatUiItem.getName()));
            floatUiItem.setTargetDrawable(app_icon);
            viewBinding.appIconSub.setImageDrawable(app_icon);
        } else {
            viewBinding.appIconSub.setImageDrawable(floatUiItem.getTargetDrawable());
        }
    }

    @Override
    public int getItemCount() {
        return overlayList.size();
    }

    public List<FloatUiItem> getOverlayManagerList() {
        return overlayList;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final FloatuiRowBinding binding;

        ViewHolder(View itemLayoutView) {
            super(itemLayoutView);
            binding = DataBindingUtil.bind(itemLayoutView);
        }

        public FloatuiRowBinding getBinding() {
            return binding;
        }
    }
}