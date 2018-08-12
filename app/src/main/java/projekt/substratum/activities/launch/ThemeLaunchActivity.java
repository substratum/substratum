/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.activities.launch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import projekt.substratum.InformationActivity;
import projekt.substratum.Substratum;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;

import java.io.Serializable;
import java.util.concurrent.ThreadLocalRandom;

import static projekt.substratum.common.Internal.ENCRYPTION_KEY_EXTRA;
import static projekt.substratum.common.Internal.IV_ENCRYPTION_KEY_EXTRA;
import static projekt.substratum.common.Internal.THEME_AUTHOR;
import static projekt.substratum.common.Internal.THEME_CALLER;
import static projekt.substratum.common.Internal.THEME_CERTIFIED;
import static projekt.substratum.common.Internal.THEME_DEBUG;
import static projekt.substratum.common.Internal.THEME_HASH;
import static projekt.substratum.common.Internal.THEME_HASHPASSTHROUGH;
import static projekt.substratum.common.Internal.THEME_LAUNCH_TYPE;
import static projekt.substratum.common.Internal.THEME_LEGACY;
import static projekt.substratum.common.Internal.THEME_NAME;
import static projekt.substratum.common.Internal.THEME_OMS;
import static projekt.substratum.common.Internal.THEME_PACKAGE;
import static projekt.substratum.common.Internal.THEME_PID;
import static projekt.substratum.common.Internal.THEME_PIRACY_CHECK;
import static projekt.substratum.common.References.SUBSTRATUM_LAUNCHER_CLASS;

public class ThemeLaunchActivity extends Activity {

    private String packageName;
    private boolean legacyTheme;

    /**
     * Launch the theme
     *
     * @param context          Context
     * @param themeName        Theme name
     * @param themeAuthor      Theme author
     * @param themePid         Theme package name
     * @param themeHash        Theme hash
     * @param themeLaunchType  Theme launch type
     * @param themeDebug       Theme debug
     * @param themePiracyCheck Theme piracy check
     * @param encryptionKey    Encryption key
     * @param ivEncryptKey     IV encryption key
     * @param themeLegacy      Legacy support
     * @return Returns an intent that allows for launching of the theme directly
     */
    private static Intent launchThemeActivity(Context context,
                                              String themeName,
                                              String themeAuthor,
                                              String themePid,
                                              Serializable themeHash,
                                              Serializable themeLaunchType,
                                              Serializable themeDebug,
                                              Serializable themePiracyCheck,
                                              byte[] encryptionKey,
                                              byte[] ivEncryptKey,
                                              Serializable themeLegacy) {

        Intent intent = new Intent(context, InformationActivity.class);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(THEME_NAME, themeName);
        intent.putExtra(THEME_AUTHOR, themeAuthor);
        intent.putExtra(THEME_PID, themePid);
        intent.putExtra(THEME_HASH, themeHash);
        intent.putExtra(THEME_LAUNCH_TYPE, themeLaunchType);
        intent.putExtra(THEME_DEBUG, themeDebug);
        intent.putExtra(THEME_PIRACY_CHECK, themePiracyCheck);
        intent.putExtra(THEME_LEGACY, themeLegacy);
        intent.putExtra(ENCRYPTION_KEY_EXTRA, encryptionKey);
        intent.putExtra(IV_ENCRYPTION_KEY_EXTRA, ivEncryptKey);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int intentId = (int) ThreadLocalRandom.current().nextLong(0L, 9999L);

        Intent activityExtras = getIntent();
        packageName = activityExtras.getStringExtra(THEME_PACKAGE);
        Boolean omsCheck = activityExtras.getBooleanExtra(THEME_OMS, false);
        Integer hashPassthrough = activityExtras.getIntExtra(THEME_HASHPASSTHROUGH, 0);
        Boolean certified = activityExtras.getBooleanExtra(THEME_CERTIFIED, true);
        String action = activityExtras.getAction();
        String packageName = activityExtras.getPackage();
        String themeCaller = activityExtras.getStringExtra(THEME_CALLER);

        Intent myIntent = new Intent();
        myIntent.putExtra(THEME_CERTIFIED, certified);
        myIntent.putExtra(THEME_HASHPASSTHROUGH, hashPassthrough);
        myIntent.putExtra(THEME_LEGACY, omsCheck);
        myIntent.putExtra(THEME_CALLER, themeCaller);
        myIntent.setAction(action);
        myIntent.setPackage(packageName);
        myIntent.setClassName(this.packageName, this.packageName + SUBSTRATUM_LAUNCHER_CLASS);

        try {
            assert action != null;
            startActivityForResult(myIntent,
                    (action.equals(References.TEMPLATE_GET_KEYS) ? 10000 : intentId));
        } catch (Exception e) {
            try {
                legacyTheme = true;
                startActivityForResult(myIntent,
                        (action.equals(References.TEMPLATE_GET_KEYS) ? 10000 : intentId));
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data) {
        // Check which request we're responding to
        if ((data != null) && (requestCode != 10000)) {
            Bundle intent = data.getExtras();
            if (intent != null) {
                String themeName = intent.getString(THEME_NAME);
                String themeAuthor = intent.getString(THEME_AUTHOR);
                String themePid = intent.getString(THEME_PID);
                Integer themeHash = intent.getInt(THEME_HASH);
                boolean themeLaunchType = intent.getBoolean(THEME_LAUNCH_TYPE);
                boolean themeDebug = intent.getBoolean(THEME_DEBUG);
                boolean themePiracyCheck = intent.getBoolean(THEME_PIRACY_CHECK);
                byte[] encryptionKey = intent.getByteArray(ENCRYPTION_KEY_EXTRA);
                byte[] ivEncryptKey = intent.getByteArray(IV_ENCRYPTION_KEY_EXTRA);

                startActivity(
                        launchThemeActivity(
                                Substratum.getInstance(),
                                themeName,
                                themeAuthor,
                                themePid,
                                themeHash,
                                themeLaunchType,
                                themeDebug,
                                themePiracyCheck,
                                encryptionKey,
                                ivEncryptKey,
                                Systems.checkOMS(Substratum.getInstance())
                        ));
            }
        } else if (legacyTheme && (requestCode != 10000)) {
            startActivity(
                    launchThemeActivity(
                            Substratum.getInstance(),
                            Packages.getPackageName(Substratum.getInstance(), packageName),
                            null,
                            packageName,
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