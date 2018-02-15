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

package projekt.substratum.adapters.tabs.sounds;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import projekt.substratum.R;
import projekt.substratum.databinding.TabSoundsItemBinding;

public class SoundsAdapter extends RecyclerView.Adapter<SoundsAdapter.ViewHolder> {

    private List<SoundsItem> soundsList;

    public SoundsAdapter(List<SoundsItem> soundsList) {
        super();
        this.soundsList = soundsList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(
                parent.getContext()).inflate(R.layout.tab_sounds_item, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder,
                                 int position) {
        final SoundsItem sounds = soundsList.get(position);
        TabSoundsItemBinding viewHolderBinding = holder.getBinding();
        viewHolderBinding.setSounds(sounds);
        viewHolderBinding.executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return this.soundsList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TabSoundsItemBinding binding;

        ViewHolder(View itemLayoutView) {
            super(itemLayoutView);
            binding = DataBindingUtil.bind(itemLayoutView);
        }

        TabSoundsItemBinding getBinding() {
            return binding;
        }
    }
}