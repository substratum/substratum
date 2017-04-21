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

package projekt.substratum.adapters.studio;

import android.content.Intent;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import projekt.substratum.R;
import projekt.substratum.activities.studio.StudioPreviewActivity;
import projekt.substratum.common.References;

public class PackAdapter extends RecyclerView.Adapter<PackAdapter.ViewHolder> {
    private ArrayList<PackInfo> information;

    public PackAdapter(ArrayList<PackInfo> information) {
        this.information = information;
    }

    @Override
    public PackAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout
                .icon_studio_pack_entry, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int pos) {
        final int i = pos;

        viewHolder.cardView.setOnClickListener(
                v -> {
                    Intent intent = new Intent(information.get(i).getContext(),
                            StudioPreviewActivity.class);
                    intent.putExtra("icon_pack", information.get(i).getPackageName());
                    information.get(i).getContext().startActivity(intent);
                });

        viewHolder.packName.setText(References.grabPackageName(information.get(i).getContext(),
                information.get(i).getPackageName()));

        viewHolder.packIcon.setImageDrawable(References.grabAppIcon(information.get(i).getContext(),
                information.get(i).getPackageName()));
    }

    @Override
    public int getItemCount() {
        return information.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView packName;
        ImageView packIcon;

        ViewHolder(View view) {
            super(view);
            cardView = (CardView) view.findViewById(R.id.pack_card);
            packIcon = (ImageView) view.findViewById(R.id.pack_icon);
            packName = (TextView) view.findViewById(R.id.pack_name);
        }
    }
}