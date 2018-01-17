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

import static projekt.substratum.common.Internal.ALARM;
import static projekt.substratum.common.Internal.EFFECT_TICK;
import static projekt.substratum.common.Internal.LOCK;
import static projekt.substratum.common.Internal.NOTIFICATION;
import static projekt.substratum.common.Internal.RINGTONE;
import static projekt.substratum.common.Internal.UNLOCK;

public class SoundsAdapter extends RecyclerView.Adapter<SoundsAdapter.ViewHolder> {

    private final List<SoundsInfo> soundsList;

    public SoundsAdapter(List<SoundsInfo> soundsList) {
        super();
        this.soundsList = soundsList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(
                parent.getContext()).inflate(R.layout.sounds_list_row, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder,
                                 int position) {
        SoundsInfo sounds = this.soundsList.get(position);
        Context context = sounds.getContext();
        String current_sound = sounds.getTitle().substring(0, sounds.getTitle().length() - 4);
        switch (current_sound) {
            case ALARM:
                holder.title.setText(context.getString(R.string.sounds_alarm));
                break;
            case NOTIFICATION:
                holder.title.setText(context.getString(R.string.sounds_notification));
                break;
            case RINGTONE:
                holder.title.setText(context.getString(R.string.sounds_ringtone));
                break;
            case EFFECT_TICK:
                holder.title.setText(context.getString(R.string.sounds_effect_tick));
                break;
            case LOCK:
                holder.title.setText(context.getString(R.string.sounds_lock_sound));
                break;
            case UNLOCK:
                holder.title.setText(context.getString(R.string.sounds_unlock_sound));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return this.soundsList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView title;

        ViewHolder(View view) {
            super(view);
            this.title = view.findViewById(R.id.title);
        }
    }
}