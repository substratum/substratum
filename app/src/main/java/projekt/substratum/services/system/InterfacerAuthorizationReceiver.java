/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.services.system;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import projekt.substratum.R;

public class InterfacerAuthorizationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getBooleanExtra("isCallerAuthorized", false)) {
            Toast.makeText(context,
                    context.getString(R.string.interfacer_not_authorized_toast),
                    Toast.LENGTH_LONG).show();
        }
    }
}