/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.activities.crash;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import projekt.substratum.LaunchActivity;
import projekt.substratum.R;

public class SystemCrash extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.crash_dialog);
        dialog.setCancelable(false);
        dialog.show();

        new Handler().postDelayed(() -> {
            dialog.dismiss();
            final Context context = getApplicationContext();
            Intent intent = new Intent(context, LaunchActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            finishAffinity();
        }, 3000);
    }
}