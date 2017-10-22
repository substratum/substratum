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

package projekt.substratum.activities.launch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;

import java.io.Serializable;
import java.util.concurrent.ThreadLocalRandom;

import projekt.substratum.InformationActivity;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;

import static android.content.pm.PackageManager.GET_META_DATA;

public class ThemeLaunchActivity extends Activity {

    private String package_name;
    private String theme_mode;
    private Boolean legacyTheme = false;

    private static Intent launchThemeActivity(final Context context,
                                              final String theme_name,
                                              final String theme_author,
                                              final String theme_pid,
                                              final String theme_mode,
                                              final Serializable theme_hash,
                                              final Serializable theme_launch_type,
                                              final Serializable theme_debug,
                                              final Serializable theme_piracy_check,
                                              final byte[] encryption_key,
                                              final byte[] iv_encrypt_key,
                                              final Serializable theme_legacy) {

        final Intent intent = new Intent(context, InformationActivity.class);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("theme_name", theme_name);
        intent.putExtra("theme_author", theme_author);
        intent.putExtra("theme_pid", theme_pid);
        intent.putExtra("theme_mode", theme_mode);
        intent.putExtra("theme_hash", theme_hash);
        intent.putExtra("theme_launch_type", theme_launch_type);
        intent.putExtra("theme_debug", theme_debug);
        intent.putExtra("theme_piracy_check", theme_piracy_check);
        intent.putExtra("theme_legacy", theme_legacy);
        intent.putExtra("encryption_key", encryption_key);
        intent.putExtra("iv_encrypt_key", iv_encrypt_key);
        try {
            final ApplicationInfo ai =
                    context.getPackageManager().getApplicationInfo(theme_pid, GET_META_DATA);
            final String plugin = ai.metaData.getString("Substratum_Plugin");
            intent.putExtra("plugin_version", plugin);
        } catch (final Exception e) {
            // Suppress warning
        }
        return intent;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final int intent_id = (int) ThreadLocalRandom.current().nextLong(0, 9999);

        final Intent activityExtras = this.getIntent();
        this.package_name = activityExtras.getStringExtra("package_name");
        final Boolean omsCheck = activityExtras.getBooleanExtra("oms_check", false);
        this.theme_mode = activityExtras.getStringExtra("theme_mode");
        final Integer hash_passthrough = activityExtras.getIntExtra("hash_passthrough", 0);
        final Boolean certified = activityExtras.getBooleanExtra("certified", true);
        final String action = activityExtras.getAction();
        final String packageName = activityExtras.getPackage();

        final Intent myIntent = new Intent();
        myIntent.putExtra("certified", certified);
        myIntent.putExtra("hash_passthrough", hash_passthrough);
        myIntent.putExtra("theme_legacy", omsCheck);
        myIntent.putExtra("theme_mode", this.theme_mode);
        myIntent.setAction(action);
        myIntent.setPackage(packageName);
        myIntent.setClassName(this.package_name, this.package_name + ".SubstratumLauncher");

        try {
            assert action != null;
            this.startActivityForResult(myIntent,
                    (action.equals(References.TEMPLATE_GET_KEYS) ? 10000 : intent_id));
        } catch (final Exception e) {
            try {
                this.legacyTheme = true;
                this.startActivityForResult(myIntent,
                        (action.equals(References.TEMPLATE_GET_KEYS) ? 10000 : intent_id));
            } catch (final Exception e2) {
                // Suppress warning
            }
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        // Check which request we're responding to
        if ((data != null) && (requestCode != 10000)) {
            final Bundle intent = data.getExtras();
            if (intent != null) {
                final String theme_name = intent.getString("theme_name");
                final String theme_author = intent.getString("theme_author");
                final String theme_pid = intent.getString("theme_pid");
                final String theme_mode = intent.getString("theme_mode");

                final Integer theme_hash = intent.getInt("theme_hash");
                final Boolean theme_launch_type = intent.getBoolean("theme_launch_type");
                final Boolean theme_debug = intent.getBoolean("theme_debug");
                final Boolean theme_piracy_check = intent.getBoolean("theme_piracy_check");

                final byte[] encryption_key = intent.getByteArray("encryption_key");
                final byte[] iv_encrypt_key = intent.getByteArray("iv_encrypt_key");

                this.startActivity(
                        launchThemeActivity(
                                this.getApplicationContext(),
                                theme_name,
                                theme_author,
                                theme_pid,
                                theme_mode,
                                theme_hash,
                                theme_launch_type,
                                theme_debug,
                                theme_piracy_check,
                                encryption_key,
                                iv_encrypt_key,
                                Systems.checkOMS(this.getApplicationContext())
                        ));
            }
        } else if (this.legacyTheme && (requestCode != 10000)) {
            this.startActivity(
                    launchThemeActivity(
                            this.getApplicationContext(),
                            Packages.getPackageName(this.getApplicationContext(), this.package_name),
                            null,
                            this.package_name,
                            this.theme_mode,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            Systems.checkOMS(this.getApplicationContext())
                    ));
        }
        this.legacyTheme = false;
        this.finish();
    }
}