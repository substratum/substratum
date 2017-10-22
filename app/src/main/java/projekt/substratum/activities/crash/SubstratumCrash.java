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
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.crash_activity);

        final String stacktrace = this.createErrorReport(this.getIntent());
        final CaocConfig caocConfig = CustomActivityOnCrash.getConfigFromIntent(this.getIntent());

        final Button restartButton = this.findViewById(R.id.restart);
        restartButton.setOnClickListener(view -> {
            final Intent intent = new Intent(SubstratumCrash.this, SplashScreenActivity.class);
            CustomActivityOnCrash.restartApplicationWithIntent(
                    SubstratumCrash.this,
                    intent,
                    caocConfig);
        });

        final Button rescueMeButton = this.findViewById(R.id.rescue_me);
        rescueMeButton.setOnClickListener(view -> {
            final Intent intent = new Intent(SubstratumCrash.this, RescueActivity.class);
            this.startActivity(intent);
            this.finish();
        });


        final Boolean isSubstratumOverlayFault = References.stringContainsItemFromList(stacktrace,
                SUBSTRATUM_OVERLAY_FAULT_EXCEPTIONS);

        if (!Systems.isSamsungDevice(this.getApplicationContext())) {
            if (isSubstratumOverlayFault) {
                // Pulsate the Rescue Me button
                new Timer().scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        if (SubstratumCrash.this.shouldPulsate) {
                            SubstratumCrash.this.runOnUiThread(() ->
                                    rescueMeButton.getBackground().setColorFilter(
                                            SubstratumCrash.this.getColor(R.color.do_not_theme_this_color_button_pulse),
                                            PorterDuff.Mode.SRC_ATOP));
                        } else {
                            SubstratumCrash.this.runOnUiThread(() ->
                                    rescueMeButton.getBackground().setColorFilter(
                                            SubstratumCrash.this.getColor(R.color.do_not_theme_this_color_buttons),
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
                        this.startActivity(intent);
                    })
                    .show();
        }

        final Button stacktraceButton = this.findViewById(R.id.logcat);
        stacktraceButton.setOnClickListener(view -> {
            final TextView showText = new TextView(this);
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
                                References.copyToClipboard(this.getApplicationContext(),
                                        this.getString(R.string
                                                .customactivityoncrash_error_activity_error_details_clipboard_label),
                                        stacktrace);
                                Toast.makeText(this,
                                        R.string.customactivityoncrash_error_activity_error_details_copied,
                                        Toast.LENGTH_SHORT).show();
                            })
                    .show();
        });
    }

    private String createErrorReport(final Intent intent) {
        final String versionName = Packages.getAppVersion(this, this.getPackageName());
        String details = "";

        details += "Build version: " + versionName + "\n";
        details += "Device: " + Build.MODEL + " (" + Build.DEVICE + ") " + "[" + Build.FINGERPRINT +
                "]";

        final String xposed = References.checkXposedVersion();
        if (!xposed.isEmpty()) details += " {" + xposed + "}";
        details += "\n";

        final String rom = Systems.checkFirmwareSupport(this, this.getString(R.string.supported_roms_url),
                "supported_roms.xml");
        final String romVersion = Build.VERSION.RELEASE + " - " +
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

        final String activityLog = CustomActivityOnCrash.getActivityLogFromIntent(intent);
        if (activityLog != null) {
            details += "\n\nUser actions:\n";
            details += activityLog;
        }

        return details;
    }
}
