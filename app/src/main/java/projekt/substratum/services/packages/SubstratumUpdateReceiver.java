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

package projekt.substratum.services.packages;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import projekt.substratum.common.Systems;
import projekt.substratum.common.platform.AndromedaService;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.common.systems.ProfileManager;

import static projekt.substratum.common.platform.ThemeManager.listEnabledOverlaysForTarget;
import static projekt.substratum.common.systems.ProfileManager.SCHEDULED_PROFILE_ENABLED;

public class SubstratumUpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ((intent.getAction() != null) &&
                !intent.getAction().equals(Intent.ACTION_MY_PACKAGE_REPLACED)) return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean scheduleProfileEnabled = prefs.getBoolean(SCHEDULED_PROFILE_ENABLED, false);
        if (scheduleProfileEnabled) {
            ProfileManager.updateScheduledProfile(context);
        }

        List<String> overlays = listEnabledOverlaysForTarget(context, context.getPackageName());
        ThemeManager.disableOverlay(context, new ArrayList<>(overlays));
        if (Systems.checkAndromeda(context)) {
            if (!AndromedaService.checkServerActivity()) {
                prefs.edit().putStringSet("to_be_disabled_overlays", new TreeSet<>(overlays))
                        .apply();
            }
        }
    }
}