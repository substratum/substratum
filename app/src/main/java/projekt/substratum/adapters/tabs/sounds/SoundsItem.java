/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
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