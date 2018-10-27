/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.adapters.activities;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;
import projekt.substratum.R;
import projekt.substratum.Substratum;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.databinding.ShowcaseItemBinding;

import java.util.List;
import java.util.Locale;

import static projekt.substratum.common.References.setRecyclerViewAnimations;

public class ShowcaseAdapter extends RecyclerView.Adapter<ShowcaseAdapter.ViewHolder> {
    private final List<ShowcaseItem> information;

    public ShowcaseAdapter(List<ShowcaseItem> information) {
        super();
        this.information = information;
    }

    @NonNull
    @Override
    public ShowcaseAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup,
                                                         int i) {
        View view = LayoutInflater.from(
                viewGroup.getContext()).inflate(R.layout.showcase_item, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder,
                                 int pos) {
        ShowcaseItem showcaseItem = this.information.get(pos);
        Context context = showcaseItem.getContext();
        ShowcaseItemBinding viewHolderBinding = viewHolder.getBinding();
        showcaseItem.setPaid(
                showcaseItem.getThemePricing().toLowerCase(Locale.US).equals(References.paidTheme));
        showcaseItem.setInstalled(
                Packages.isPackageInstalled(context, showcaseItem.getThemePackage()));
        viewHolderBinding.setShowcaseItem(showcaseItem);
        viewHolderBinding.executePendingBindings();
        if (!Substratum.getPreferences().getBoolean("lite_mode", false)) {
            setRecyclerViewAnimations(viewHolderBinding.backgroundImage);
        }
    }

    @Override
    public int getItemCount() {
        return this.information.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ShowcaseItemBinding binding;

        ViewHolder(View view) {
            super(view);
            binding = DataBindingUtil.bind(view);
        }

        ShowcaseItemBinding getBinding() {
            return binding;
        }
    }
}