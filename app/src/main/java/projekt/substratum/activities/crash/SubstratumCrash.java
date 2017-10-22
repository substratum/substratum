package projekt.substratum.activities.crash;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

import cat.ereza.customactivityoncrash.CustomActivityOnCrash;
import cat.ereza.customactivityoncrash.config.CaocConfig;
import projekt.substratum.R;
import projekt.substratum.activities.launch.RescueActivity;
import projekt.substratum.activities.launch.SplashScreenActivity;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;

import static projekt.substratum.common.References.NO_THEME_ENGINE;
import static projekt.substratum.common.References.OVERLAY_MANAGER_SERVICE_N_UNROOTED;
import static projekt.substratum.common.References.OVERLAY_MANAGER_SERVICE_O_ANDROMEDA;
import static projekt.substratum.common.References.OVERLAY_MANAGER_SERVICE_O_ROOTED;
import static projekt.substratum.common.References.OVERLAY_MANAGER_SERVICE_O_UNROOTED;
import static projekt.substratum.common.References.RUNTIME_RESOURCE_OVERLAY_N_ROOTED;
import static projekt.substratum.common.References.SAMSUNG_THEME_ENGINE_N;
import static projekt.substratum.common.Resources.SUBSTRATUM_OVERLAY_FAULT_EXCEPTIONS;

public class SubstratumCrash extends Activity {

    boolean shouldPulsate = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.crash_activity);

        String stacktrace = createErrorReport(getIntent());
        CaocConfig caocConfig = CustomActivityOnCrash.getConfigFromIntent(getIntent());

        Button restartButton = findViewById(R.id.restart);
        restartButton.setOnClickListener(view -> {
            Intent intent = new Intent(SubstratumCrash.this, SplashScreenActivity.class);
            CustomActivityOnCrash.restartApplicationWithIntent(
                    SubstratumCrash.this,
                    intent,
                    caocConfig);
        });

        Button rescueMeButton = findViewById(R.id.rescue_me);
        rescueMeButton.setOnClickListener(view -> {
            Intent intent = new Intent(SubstratumCrash.this, RescueActivity.class);
            startActivity(intent);
            finish();
        });


        Boolean isSubstratumOverlayFault = References.stringContainsItemFromList(stacktrace,
                SUBSTRATUM_OVERLAY_FAULT_EXCEPTIONS);

        if (!Systems.isSamsungDevice(getApplicationContext())) {
            if (isSubstratumOverlayFault) {
                // Pulsate the Rescue Me button
                new Timer().scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        if (SubstratumCrash.this.shouldPulsate) {
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
                        SubstratumCrash.this.shouldPulsate = !SubstratumCrash.this.shouldPulsate;
                    }
                }, 0, 400);
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

        Button stacktraceButton = findViewById(R.id.logcat);
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

    private String createErrorReport(Intent intent) {
        String versionName = Packages.getAppVersion(this, getPackageName());
        String details = "";

        details += "Build version: " + versionName + "\n";
        details += "Device: " + Build.MODEL + " (" + Build.DEVICE + ") " + "[" + Build.FINGERPRINT +
                "]";

        String xposed = References.checkXposedVersion();
        if (!xposed.isEmpty()) details += " {" + xposed + "}";
        details += "\n";

        String rom = Systems.checkFirmwareSupport(this, getString(R.string.supported_roms_url),
                "supported_roms.xml");
        String romVersion = Build.VERSION.RELEASE + " - " +
                (!rom.isEmpty() ? rom : "Unknown");
        details += "ROM: " + romVersion + "\n";
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
