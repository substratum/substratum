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
import projekt.substratum.Substratum;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;

import static android.content.pm.PackageManager.GET_META_DATA;
import static projekt.substratum.common.Internal.ENCRYPTION_KEY_EXTRA;
import static projekt.substratum.common.Internal.IV_ENCRYPTION_KEY_EXTRA;
import static projekt.substratum.common.Internal.THEME_AUTHOR;
import static projekt.substratum.common.Internal.THEME_CERTIFIED;
import static projekt.substratum.common.Internal.THEME_DEBUG;
import static projekt.substratum.common.Internal.THEME_HASH;
import static projekt.substratum.common.Internal.THEME_HASHPASSTHROUGH;
import static projekt.substratum.common.Internal.THEME_LAUNCH_TYPE;
import static projekt.substratum.common.Internal.THEME_LEGACY;
import static projekt.substratum.common.Internal.THEME_MODE;
import static projekt.substratum.common.Internal.THEME_NAME;
import static projekt.substratum.common.Internal.THEME_OMS;
import static projekt.substratum.common.Internal.THEME_PACKAGE;
import static projekt.substratum.common.Internal.THEME_PID;
import static projekt.substratum.common.Internal.THEME_PIRACY_CHECK;
import static projekt.substratum.common.References.SUBSTRATUM_LAUNCHER_CLASS;
import static projekt.substratum.common.References.metadataVersion;

public class ThemeLaunchActivity extends Activity {

    private String package_name;
    private String theme_mode;
    private Boolean legacyTheme = false;

    /**
     * Launch the theme
     *
     * @param context            Context
     * @param theme_name         Theme name
     * @param theme_author       Theme author
     * @param theme_pid          Theme package name
     * @param theme_mode         Theme mode
     * @param theme_hash         Theme hash
     * @param theme_launch_type  Theme launch type
     * @param theme_debug        Theme debug
     * @param theme_piracy_check Theme piracy check
     * @param encryption_key     Encryption key
     * @param iv_encrypt_key     IV encryption key
     * @param theme_legacy       Legacy support
     * @return Returns an intent that allows for launching of the theme directly
     */
    private static Intent launchThemeActivity(Context context,
                                              String theme_name,
                                              String theme_author,
                                              String theme_pid,
                                              String theme_mode,
                                              Serializable theme_hash,
                                              Serializable theme_launch_type,
                                              Serializable theme_debug,
                                              Serializable theme_piracy_check,
                                              byte[] encryption_key,
                                              byte[] iv_encrypt_key,
                                              Serializable theme_legacy) {

        Intent intent = new Intent(context, InformationActivity.class);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(THEME_NAME, theme_name);
        intent.putExtra(THEME_AUTHOR, theme_author);
        intent.putExtra(THEME_PID, theme_pid);
        intent.putExtra(THEME_MODE, theme_mode);
        intent.putExtra(THEME_HASH, theme_hash);
        intent.putExtra(THEME_LAUNCH_TYPE, theme_launch_type);
        intent.putExtra(THEME_DEBUG, theme_debug);
        intent.putExtra(THEME_PIRACY_CHECK, theme_piracy_check);
        intent.putExtra(THEME_LEGACY, theme_legacy);
        intent.putExtra(ENCRYPTION_KEY_EXTRA, encryption_key);
        intent.putExtra(IV_ENCRYPTION_KEY_EXTRA, iv_encrypt_key);
        try {
            ApplicationInfo ai =
                    context.getPackageManager().getApplicationInfo(theme_pid, GET_META_DATA);
            String plugin = ai.metaData.getString(metadataVersion);
            intent.putExtra("plugin_version", plugin);
        } catch (Exception e) {
            // Suppress warning
        }
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int intent_id = (int) ThreadLocalRandom.current().nextLong(0L, 9999L);

        Intent activityExtras = getIntent();
        package_name = activityExtras.getStringExtra(THEME_PACKAGE);
        Boolean omsCheck = activityExtras.getBooleanExtra(THEME_OMS, false);
        theme_mode = activityExtras.getStringExtra(THEME_MODE);
        Integer hash_passthrough = activityExtras.getIntExtra(THEME_HASHPASSTHROUGH, 0);
        Boolean certified = activityExtras.getBooleanExtra(THEME_CERTIFIED, true);
        String action = activityExtras.getAction();
        String packageName = activityExtras.getPackage();

        Intent myIntent = new Intent();
        myIntent.putExtra(THEME_CERTIFIED, certified);
        myIntent.putExtra(THEME_HASHPASSTHROUGH, hash_passthrough);
        myIntent.putExtra(THEME_LEGACY, omsCheck);
        myIntent.putExtra(THEME_MODE, theme_mode);
        myIntent.setAction(action);
        myIntent.setPackage(packageName);
        myIntent.setClassName(package_name, package_name + SUBSTRATUM_LAUNCHER_CLASS);

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
    protected void onActivityResult(int requestCode, int resultCode, Intent
            data) {
        // Check which request we're responding to
        if ((data != null) && (requestCode != 10000)) {
            Bundle intent = data.getExtras();
            if (intent != null) {
                String theme_name = intent.getString(THEME_NAME);
                String theme_author = intent.getString(THEME_AUTHOR);
                String theme_pid = intent.getString(THEME_PID);
                String theme_mode = intent.getString(THEME_MODE);
                Integer theme_hash = intent.getInt(THEME_HASH);
                Boolean theme_launch_type = intent.getBoolean(THEME_LAUNCH_TYPE);
                Boolean theme_debug = intent.getBoolean(THEME_DEBUG);
                Boolean theme_piracy_check = intent.getBoolean(THEME_PIRACY_CHECK);
                byte[] encryption_key = intent.getByteArray(ENCRYPTION_KEY_EXTRA);
                byte[] iv_encrypt_key = intent.getByteArray(IV_ENCRYPTION_KEY_EXTRA);

                startActivity(
                        launchThemeActivity(
                                Substratum.getInstance(),
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
                                Systems.checkOMS(Substratum.getInstance())
                        ));
            }
        } else if (legacyTheme && (requestCode != 10000)) {
            startActivity(
                    launchThemeActivity(
                            Substratum.getInstance(),
                            Packages.getPackageName(Substratum.getInstance(), package_name),
                            null,
                            package_name,
                            theme_mode,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            Systems.checkOMS(Substratum.getInstance())
                    ));
        }
        legacyTheme = false;
        finish();
    }
}