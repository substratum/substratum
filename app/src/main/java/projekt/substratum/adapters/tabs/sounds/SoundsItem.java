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

import projekt.substratum.R;

import static projekt.substratum.common.Internal.ALARM;
import static projekt.substratum.common.Internal.EFFECT_TICK;
import static projekt.substratum.common.Internal.LOCK;
import static projekt.substratum.common.Internal.NOTIFICATION;
import static projekt.substratum.common.Internal.RINGTONE;
import static projekt.substratum.common.Internal.UNLOCK;

public class SoundsItem {
    private final String absolutePath;
    private final Context context;
    private final String title;

    public SoundsItem(Context context,
                      String title,
                      String absolutePath) {
        super();
        this.context = context;
        this.title = title;
        this.absolutePath = absolutePath;
    }

    public String getAbsolutePath() {
        return this.absolutePath;
    }

    public String getWorkingTitle() {
        String current_sound = title.substring(0, title.length() - 4);
        switch (current_sound) {
            case ALARM:
                return context.getString(R.string.sounds_alarm);
            case NOTIFICATION:
                return context.getString(R.string.sounds_notification);
            case RINGTONE:
                return context.getString(R.string.sounds_ringtone);
            case EFFECT_TICK:
                return context.getString(R.string.sounds_effect_tick);
            case LOCK:
                return context.getString(R.string.sounds_lock_sound);
            case UNLOCK:
                return context.getString(R.string.sounds_unlock_sound);
            default:
                return current_sound;
        }
    }
}