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

package projekt.substratum.activities.profiles;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import projekt.substratum.R;
import projekt.substratum.activities.base.SubstratumActivity;

public class ProfileErrorInfoActivity extends SubstratumActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.restore_dialog_title))
                .setMessage(getIntent().getStringExtra("dialog_message"))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.dialog_ok), (dialogInterface, i) -> {
                    PackageManager manager = this.getPackageManager();
                    Intent intent = manager.getLaunchIntentForPackage("projekt.substratum");
                    intent.addCategory(Intent.CATEGORY_LAUNCHER);
                    startActivity(intent);
                    finish();
                })
                .setOnCancelListener(dialogInterface -> finish())
                .setCancelable(true)
                .create()
                .show();
    }
}