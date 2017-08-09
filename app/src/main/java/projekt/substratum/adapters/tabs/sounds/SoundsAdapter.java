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

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import projekt.substratum.R;

public class SoundsAdapter extends RecyclerView.Adapter<SoundsAdapter.MyViewHolder> {

    private List<SoundsInfo> soundsList;

    public SoundsAdapter(List<SoundsInfo> soundsList) {
        this.soundsList = soundsList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(
                parent.getContext()).inflate(R.layout.sounds_list_row, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        SoundsInfo sounds = soundsList.get(position);
        Context context = sounds.getContext();
        String current_sound = sounds.getTitle().substring(0, sounds.getTitle().length() - 4);
        switch (current_sound) {
            case "alarm":
                holder.title.setText(context.getString(R.string.sounds_alarm));
                break;
            case "notification":
                holder.title.setText(context.getString(R.string.sounds_notification));
                break;
            case "ringtone":
                holder.title.setText(context.getString(R.string.sounds_ringtone));
                break;
            case "Effect_Tick":
                holder.title.setText(context.getString(R.string.sounds_effect_tick));
                break;
            case "Lock":
                holder.title.setText(context.getString(R.string.sounds_lock_sound));
                break;
            case "Unlock":
                holder.title.setText(context.getString(R.string.sounds_unlock_sound));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return soundsList.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView title;

        MyViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.title);
        }
    }
}