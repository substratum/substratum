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
import android.content.Intent;
import android.os.Bundle;

import java.util.concurrent.ThreadLocalRandom;

import projekt.substratum.Substratum;
import projekt.substratum.adapters.fragments.themes.SecurityItem;
import projekt.substratum.common.References;

import static projekt.substratum.common.Broadcasts.sendKeySentMessage;
import static projekt.substratum.common.Internal.ENCRYPTION_KEY_EXTRA;
import static projekt.substratum.common.Internal.IV_ENCRYPTION_KEY_EXTRA;
import static projekt.substratum.common.Internal.THEME_CALLER;
import static projekt.substratum.common.Internal.THEME_CERTIFIED;
import static projekt.substratum.common.Internal.THEME_DEBUG;
import static projekt.substratum.common.Internal.THEME_HASH;
import static projekt.substratum.common.Internal.THEME_HASHPASSTHROUGH;
import static projekt.substratum.common.Internal.THEME_LAUNCH_TYPE;
import static projekt.substratum.common.Internal.THEME_LEGACY;
import static projekt.substratum.common.Internal.THEME_MODE;
import static projekt.substratum.common.Internal.THEME_OMS;
import static projekt.substratum.common.Internal.THEME_PACKAGE;
import static projekt.substratum.common.Internal.THEME_PID;
import static projekt.substratum.common.Internal.THEME_PIRACY_CHECK;
import static projekt.substratum.common.References.SUBSTRATUM_LAUNCHER_CLASS;

public class KeyExchangeActivity extends Activity {

    /**
     * The consolidated method to obtain keys from a theme, used in TLA and KEA
     *
     * @param activity         Activity with support of onActivityResult
     * @param certified        Certified
     * @param hash_passthrough Hash Passthrough
     * @param omsCheck         OMS Check
     * @param themeCaller      Theme Caller
     * @param theme_mode       Theme Mode
     * @param action           Action
     * @param packageName      Calling package name
     * @param package_name     To open theme - package name
     * @param intent_id        Intent ID
     */
    public static void consolidatedIntentParser(Activity activity,
                                                Boolean certified,
                                                Integer hash_passthrough,
                                                Boolean omsCheck,
                                                String themeCaller,
                                                String theme_mode,
                                                String action,
                                                String packageName,
                                                String package_name,
                                                int intent_id) {
        Intent myIntent = new Intent();
        myIntent.putExtra(THEME_CERTIFIED, certified);
        myIntent.putExtra(THEME_HASHPASSTHROUGH, hash_passthrough);
        myIntent.putExtra(THEME_LEGACY, omsCheck);
        myIntent.putExtra(THEME_CALLER, themeCaller);
        myIntent.putExtra(THEME_MODE, theme_mode);
        myIntent.setAction(action);
        myIntent.setPackage(packageName);
        myIntent.setClassName(package_name, package_name + SUBSTRATUM_LAUNCHER_CLASS);

        try {
            if (action != null) {
                activity.startActivityForResult(myIntent,
                        (action.equals(References.TEMPLATE_GET_KEYS) ? 10000 : intent_id));
            }
        } catch (Exception e) {
            // Suppress warning
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int intent_id = (int) ThreadLocalRandom.current().nextLong(0L, 9999L);
        Intent activityExtras = getIntent();

        consolidatedIntentParser(this,
                activityExtras.getBooleanExtra(THEME_CERTIFIED, true),
                activityExtras.getIntExtra(THEME_HASHPASSTHROUGH, 0),
                activityExtras.getBooleanExtra(THEME_OMS, false),
                activityExtras.getStringExtra(THEME_CALLER),
                activityExtras.getStringExtra(THEME_MODE),
                activityExtras.getAction(),
                activityExtras.getPackage(),
                activityExtras.getStringExtra(THEME_PACKAGE),
                intent_id
        );
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data) {
        if (data != null && requestCode != 10000) {
            Bundle intent = data.getExtras();
            if (intent != null) {
                String theme_pid = intent.getString(THEME_PID);
                Substratum.currentThemeSecurity = new SecurityItem(theme_pid);
                Substratum.currentThemeSecurity.setHash(intent.getInt(THEME_HASH));
                Substratum.currentThemeSecurity.setLaunchType(intent.getBoolean(THEME_LAUNCH_TYPE));
                Substratum.currentThemeSecurity.setDebug(intent.getBoolean(THEME_DEBUG));
                Substratum.currentThemeSecurity.setPiracyCheck(
                        intent.getBoolean(THEME_PIRACY_CHECK));
                Substratum.currentThemeSecurity.setEncryptionKey(
                        intent.getByteArray(ENCRYPTION_KEY_EXTRA));
                Substratum.currentThemeSecurity.setIVEncryptKey(
                        intent.getByteArray(IV_ENCRYPTION_KEY_EXTRA));
            }
        } else if (data == null && requestCode != 10000) {
            Substratum.currentThemeSecurity = new SecurityItem(null);
        }
        sendKeySentMessage(getApplicationContext());
        finish();
    }
}