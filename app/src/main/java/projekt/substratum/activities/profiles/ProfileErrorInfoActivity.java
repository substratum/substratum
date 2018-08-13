/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.activities.profiles;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import projekt.substratum.R;

import static projekt.substratum.common.References.SUBSTRATUM_PACKAGE;

public class ProfileErrorInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.restore_dialog_title))
                .setMessage(getIntent().getStringExtra("dialog_message"))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.dialog_ok), (dialogInterface, i) -> {
                    PackageManager manager = getPackageManager();
                    Intent intent = manager.getLaunchIntentForPackage(SUBSTRATUM_PACKAGE);
                    if (intent != null) {
                        intent.addCategory(Intent.CATEGORY_LAUNCHER);
                        startActivity(intent);
                    }
                    finish();
                })
                .setOnCancelListener(dialogInterface -> finish())
                .setCancelable(true)
                .create()
                .show();
    }
}