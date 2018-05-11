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

package projekt.substratum.adapters.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;
import java.util.Locale;

import projekt.substratum.R;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.databinding.ShowcaseItemBinding;

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
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(showcaseItem.getContext());
        if (!prefs.getBoolean("lite_mode", false)) {
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