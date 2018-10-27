/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.adapters.tabs.sounds;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;
import projekt.substratum.R;
import projekt.substratum.databinding.TabSoundsItemBinding;

import java.util.List;

public class SoundsAdapter extends RecyclerView.Adapter<SoundsAdapter.ViewHolder> {

    private final List<SoundsItem> soundsList;

    public SoundsAdapter(List<SoundsItem> soundsList) {
        super();
        this.soundsList = soundsList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(
                parent.getContext()).inflate(R.layout.tab_sounds_item, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder,
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