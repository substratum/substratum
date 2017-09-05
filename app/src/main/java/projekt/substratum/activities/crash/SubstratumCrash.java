package projekt.substratum.activities.crash;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.PorterDuff;
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
import projekt.substratum.common.References;

import static projekt.substratum.common.Resources.SUBSTRATUM_OVERLAY_FAULT_EXCEPTIONS;

public class SubstratumCrash extends Activity {

    boolean shouldPulsate = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.crash_activity);

        String stacktrace = CustomActivityOnCrash.getAllErrorDetailsFromIntent(
                getApplicationContext(), getIntent());
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

        if (!References.isSamsungDevice(getApplicationContext())) {
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
                                copyErrorToClipboard(stacktrace);
                                Toast.makeText(this,
                                        R.string.customactivityoncrash_error_activity_error_details_copied,
                                        Toast.LENGTH_SHORT).show();
                            })
                    .show();
        });
    }

    private void copyErrorToClipboard(String stacktrace) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(
                getString(R.string
                        .customactivityoncrash_error_activity_error_details_clipboard_label),
                stacktrace);
        assert clipboard != null;
        clipboard.setPrimaryClip(clip);
    }
}
