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

import java.util.concurrent.ThreadLocalRandom;

import projekt.substratum.InformationActivity;
import projekt.substratum.common.References;

import static android.content.pm.PackageManager.GET_META_DATA;

public class ThemeLaunchActivity extends Activity {

    private String package_name;
    private String theme_mode;
    private Boolean legacyTheme = false;

    public static Intent launchThemeActivity(Context context,
                                             String theme_name,
                                             String theme_author,
                                             String theme_pid,
                                             String theme_mode,
                                             Integer theme_hash,
                                             Boolean theme_launch_type,
                                             Boolean theme_debug,
                                             Boolean theme_piracy_check,
                                             byte[] encryption_key,
                                             byte[] iv_encrypt_key,
                                             Boolean theme_legacy) {

        Intent intent = new Intent(context, InformationActivity.class);
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
            ApplicationInfo ai =
                    context.getPackageManager().getApplicationInfo(theme_pid, GET_META_DATA);
            String plugin = ai.metaData.getString("Substratum_Plugin");
            intent.putExtra("plugin_version", plugin);
        } catch (Exception e) {
            // Suppress warning
        }
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int intent_id = (int) ThreadLocalRandom.current().nextLong(0, 9999);

        Intent activityExtras = getIntent();
        package_name = activityExtras.getStringExtra("package_name");
        Boolean omsCheck = activityExtras.getBooleanExtra("oms_check", false);
        theme_mode = activityExtras.getStringExtra("theme_mode");
        Integer hash_passthrough = activityExtras.getIntExtra("hash_passthrough", 0);
        Boolean certified = activityExtras.getBooleanExtra("certified", true);
        String action = activityExtras.getAction();
        String packageName = activityExtras.getPackage();

        Intent myIntent = new Intent();
        myIntent.putExtra("certified", certified);
        myIntent.putExtra("hash_passthrough", hash_passthrough);
        myIntent.putExtra("theme_legacy", omsCheck);
        myIntent.putExtra("theme_mode", theme_mode);
        myIntent.setAction(action);
        myIntent.setPackage(packageName);
        myIntent.setClassName(package_name, package_name + ".SubstratumLauncher");

        try {
            assert action != null;
            startActivityForResult(myIntent,
                    (action.equals(References.TEMPLATE_GET_KEYS) ? 10000 : intent_id));
        } catch (Exception e) {
            try {
                legacyTheme = true;
                startActivityForResult(myIntent,
                        (action.equals(References.TEMPLATE_GET_KEYS) ? 10000 : intent_id));
            } catch (Exception e2) {
                // Suppress warning
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (data != null && requestCode != 10000) {
            Bundle intent = data.getExtras();
            if (intent != null) {
                String theme_name = intent.getString("theme_name");
                String theme_author = intent.getString("theme_author");
                String theme_pid = intent.getString("theme_pid");
                String theme_mode = intent.getString("theme_mode");

                Integer theme_hash = intent.getInt("theme_hash");
                Boolean theme_launch_type = intent.getBoolean("theme_launch_type");
                Boolean theme_debug = intent.getBoolean("theme_debug");
                Boolean theme_piracy_check = intent.getBoolean("theme_piracy_check");

                byte[] encryption_key = intent.getByteArray("encryption_key");
                byte[] iv_encrypt_key = intent.getByteArray("iv_encrypt_key");

                startActivity(
                        launchThemeActivity(
                                getApplicationContext(),
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
                                References.checkOMS(getApplicationContext())
                        ));
            }
        } else if (legacyTheme && requestCode != 10000) {
            startActivity(
                    launchThemeActivity(
                            getApplicationContext(),
                            References.grabPackageName(getApplicationContext(), package_name),
                            null,
                            package_name,
                            theme_mode,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            References.checkOMS(getApplicationContext())
                    ));
        }
        legacyTheme = false;
        finish();
    }
}