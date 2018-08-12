/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.services.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import projekt.substratum.Substratum;
import projekt.substratum.services.floatui.SubstratumFloatInterface;

public class FloatUiButtonReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = Substratum.getPreferences();
        if (prefs.getBoolean("floatui_show_android_system_overlays", true)) {
            prefs.edit().putBoolean("floatui_show_android_system_overlays", false).apply();
        } else {
            prefs.edit().putBoolean("floatui_show_android_system_overlays", true).apply();
        }
        Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.getApplicationContext().sendBroadcast(closeDialog);
        context.stopService(new Intent(context, SubstratumFloatInterface.class));
        context.startService(new Intent(context, SubstratumFloatInterface.class));
    }
}