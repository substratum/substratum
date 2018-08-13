/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.activities.crash;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import cat.ereza.customactivityoncrash.CustomActivityOnCrash;
import cat.ereza.customactivityoncrash.config.CaocConfig;
import projekt.substratum.R;
import projekt.substratum.activities.launch.SplashScreenActivity;
import projekt.substratum.activities.shortcuts.RescueActivity;
import projekt.substratum.common.Activities;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.databinding.CrashActivityBinding;

import java.util.Timer;
import java.util.TimerTask;

import static projekt.substratum.common.Internal.SUPPORTED_ROMS_FILE;
import static projekt.substratum.common.References.NO_THEME_ENGINE;
import static projekt.substratum.common.References.OVERLAY_MANAGER_SERVICE_N_UNROOTED;
import static projekt.substratum.common.References.OVERLAY_MANAGER_SERVICE_O_ANDROMEDA;
import static projekt.substratum.common.References.OVERLAY_MANAGER_SERVICE_O_ROOTED;
import static projekt.substratum.common.References.OVERLAY_MANAGER_SERVICE_O_UNROOTED;
import static projekt.substratum.common.References.RUNTIME_RESOURCE_OVERLAY_N_ROOTED;
import static projekt.substratum.common.References.SAMSUNG_THEME_ENGINE_N;
import static projekt.substratum.common.Resources.SUBSTRATUM_OVERLAY_FAULT_EXCEPTIONS;
import static projekt.substratum.common.Resources.SYSTEM_FAULT_EXCEPTIONS;

public class SubstratumCrash extends Activity {

    private Button rescueMeButton;
    private boolean shouldPulsate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String stacktrace = createErrorReport(getIntent());
        CaocConfig caocConfig = CustomActivityOnCrash.getConfigFromIntent(getIntent());

        boolean isSystemFault = References.stringContainsItemFromList(
                stacktrace,
                SYSTEM_FAULT_EXCEPTIONS);
        boolean isRomBuilderFault = References.stringContainsItemFromList(
                stacktrace,
                new String[]{
                        "Attempt to invoke interface method " +
                                "'boolean android.content.om.IOverlayManager."
                }
        );

        if (isSystemFault && !isRomBuilderFault) {
            Activities.launchInternalActivity(this, SystemCrash.class);
            finishAffinity();
        }

        // We should have the theme dynamically change depending on the nature of the crash
        setTheme(R.style.DoNotThemeThisStyle);

        CrashActivityBinding binding =
                DataBindingUtil.setContentView(this, R.layout.crash_activity);

        Button restartButton = binding.restart;
        rescueMeButton = binding.rescueMe;
        Button stacktraceButton = binding.logcat;

        restartButton.setOnClickListener(view -> {
            Intent intent = new Intent(SubstratumCrash.this, SplashScreenActivity.class);
            CustomActivityOnCrash.restartApplicationWithIntent(
                    SubstratumCrash.this,
                    intent,
                    (caocConfig == null ? new CaocConfig() : caocConfig));
        });

        rescueMeButton.setOnClickListener(view -> {
            Intent intent = new Intent(SubstratumCrash.this, RescueActivity.class);
            startActivity(intent);
            finish();
        });


        boolean isSubstratumOverlayFault = References.stringContainsItemFromList(
                stacktrace,
                SUBSTRATUM_OVERLAY_FAULT_EXCEPTIONS);

        if (!Systems.isSamsungDevice(getApplicationContext())) {
            if (isSubstratumOverlayFault) {
                // Pulsate the Rescue Me button
                new Timer().scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        if (shouldPulsate) {
                            runOnUiThread(() ->
                                    rescueMeButton.getBackground().setColorFilter(
                                            getColor(R.color.do_not_theme_this_color_button_pulse),
                                            PorterDuff.Mode.SRC_ATOP));
                        } else {
                            runOnUiThread(() ->
                                    rescueMeButton.getBackground().setColorFilter(
                                            getColor(R.color.do_not_theme_this_color_buttons),
                                            PorterDuff.Mode.SRC_ATOP));
                        }
                        shouldPulsate = !shouldPulsate;
                    }
                }, 0L, 400L);
            }
        } else if (isSubstratumOverlayFault) {
            rescueMeButton.setVisibility(View.GONE);
            new AlertDialog.Builder(this)
                    .setTitle(R.string.error_dialog_samsung)
                    .setMessage(R.string.error_dialog_samsung_text)
                    .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                        Intent intent = new Intent(
                                Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS);
                        startActivity(intent);
                    })
                    .show();
        }

        stacktraceButton.setOnClickListener(view -> {
            TextView showText = new TextView(this);
            showText.setPadding(70, 30, 70, 30);
            showText.setText(stacktrace);
            showText.setTextIsSelectable(true);

            new AlertDialog.Builder(this)
                    .setTitle(R.string.error_activity_log_dialog_title)
                    .setView(showText)
                    .setPositiveButton(R.string
                            .customactivityoncrash_error_activity_error_details_close, null)
                    .setNeutralButton(R.string
                                    .customactivityoncrash_error_activity_error_details_copy,
                            (dialog1, which) -> {
                                References.copyToClipboard(getApplicationContext(),
                                        getString(R.string
                                                .customactivityoncrash_error_activity_error_details_clipboard_label),
                                        stacktrace);
                                Toast.makeText(this,
                                        R.string.customactivityoncrash_error_activity_error_details_copied,
                                        Toast.LENGTH_SHORT).show();
                            })
                    .show();
        });
    }

    /**
     * Create an error report
     *
     * @param intent Intent
     * @return Return string of error report
     */
    private String createErrorReport(Intent intent) {
        String versionName = Packages.getAppVersion(this, getPackageName());
        String details = "";

        details += "Build version: " + versionName + '\n';
        details += "Device: " + Build.MODEL + " (" + Build.DEVICE + ") " + '[' + Build.FINGERPRINT +
                ']';

        String xposed = References.checkXposedVersion();
        if (!xposed.isEmpty()) details += " {" + xposed + '}';
        details += "\n";

        String rom = Systems.checkFirmwareSupport(this,
                getString(R.string.supported_roms_url),
                SUPPORTED_ROMS_FILE);
        String romVersion = Build.VERSION.RELEASE + " - " +
                (!rom.isEmpty() ? rom : "Unknown");
        details += "ROM: " + romVersion + '\n';
        details += "Theme system: ";
        switch (Systems.checkThemeSystemModule(this)) {
            case NO_THEME_ENGINE:
                details += "Not detected";
                break;
            case OVERLAY_MANAGER_SERVICE_O_ROOTED:
                details += "OMS (root)";
                break;
            case OVERLAY_MANAGER_SERVICE_O_ANDROMEDA:
                details += "OMS (andromeda)";
                break;
            case OVERLAY_MANAGER_SERVICE_O_UNROOTED:
                if (Systems.checkSubstratumService(getApplicationContext())) {
                    details += "OMS (system service)";
                } else if (Systems.checkThemeInterfacer(getApplicationContext())) {
                    details += "OMS (interfacer)";
                }
                break;
            case OVERLAY_MANAGER_SERVICE_N_UNROOTED:
                details += "OMS (interfacer)";
                break;
            case SAMSUNG_THEME_ENGINE_N:
                details += "RRO (Samsung)";
                break;
            case RUNTIME_RESOURCE_OVERLAY_N_ROOTED:
                details += "RRO (Legacy)";
                break;
        }
        details += "\n\n";

        details += "Stack trace:\n";
        details += CustomActivityOnCrash.getStackTraceFromIntent(intent);

        String activityLog = CustomActivityOnCrash.getActivityLogFromIntent(intent);
        if (activityLog != null) {
            details += "\n\nUser actions:\n";
            details += activityLog;
        }

        return details;
    }
}