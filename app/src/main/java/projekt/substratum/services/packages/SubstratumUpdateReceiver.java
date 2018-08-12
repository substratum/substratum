/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.services.packages;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import projekt.substratum.Substratum;
import projekt.substratum.common.Systems;
import projekt.substratum.common.platform.AndromedaService;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.common.systems.ProfileManager;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static projekt.substratum.common.platform.ThemeManager.listEnabledOverlaysForTarget;
import static projekt.substratum.common.systems.ProfileManager.SCHEDULED_PROFILE_ENABLED;

public class SubstratumUpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ((intent.getAction() != null) &&
                !intent.getAction().equals(Intent.ACTION_MY_PACKAGE_REPLACED)) return;
        SharedPreferences prefs = Substratum.getPreferences();
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