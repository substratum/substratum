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
        Boolean notification = activityExtras.getBooleanExtra("notification", false);
        String hash_passthrough = activityExtras.getStringExtra("hash_passthrough");
        Boolean certified = activityExtras.getBooleanExtra("certified", true);

        Intent myIntent = new Intent();
        myIntent.putExtra("certified", certified);
        myIntent.putExtra("hash_passthrough", hash_passthrough);
        myIntent.setClassName(package_name, package_name + ".SubstratumLauncher");

        Intent myIntent2 = new Intent();
        myIntent2.putExtra("certified", certified);
        myIntent2.putExtra("hash_passthrough", hash_passthrough);
        myIntent2.setClassName(package_name, package_name + ".SubstratumLauncher");

        try {
            startActivityForResult(myIntent, intent_id);
        } catch (Exception e) {
            try {
                legacyTheme = true;
                startActivityForResult(myIntent2, intent_id);
            } catch (Exception e2) {
                // Suppress warning
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (data != null) {
            Bundle intent = data.getExtras();

            String theme_name = intent.getString("theme_name");
            String theme_author = intent.getString("theme_author");
            String theme_pid = intent.getString("theme_pid");
            String theme_mode = intent.getString("theme_mode");

            Integer theme_hash = intent.getInt("theme_hash");
            Boolean theme_launch_type = intent.getBoolean("theme_launch_type");
            Boolean theme_debug = intent.getBoolean("theme_debug");
            Boolean theme_piracy_check = intent.getBoolean("theme_piracy_check");

            startActivity(launchThemeActivity(
                    getApplicationContext(),
                    theme_name,
                    theme_author,
                    theme_pid,
                    theme_mode,
                    theme_hash,
                    theme_launch_type,
                    theme_debug,
                    theme_piracy_check,
                    References.checkOMS(getApplicationContext())
            ));
        } else if (legacyTheme) {
            startActivity(launchThemeActivity(
                    getApplicationContext(),
                    References.grabPackageName(getApplicationContext(), package_name),
                    null,
                    package_name,
                    theme_mode,
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