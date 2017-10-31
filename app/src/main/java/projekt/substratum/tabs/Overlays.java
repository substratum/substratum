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

package projekt.substratum.tabs;


import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.design.widget.Lunchbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import butterknife.BindView;
import butterknife.ButterKnife;
import projekt.substratum.BuildConfig;
import projekt.substratum.InformationActivity;
import projekt.substratum.R;
import projekt.substratum.adapters.tabs.overlays.OverlaysAdapter;
import projekt.substratum.adapters.tabs.overlays.OverlaysItem;
import projekt.substratum.adapters.tabs.overlays.VariantAdapter;
import projekt.substratum.adapters.tabs.overlays.VariantItem;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.util.compilers.SubstratumBuilder;
import projekt.substratum.util.files.MapUtils;
import projekt.substratum.util.files.Root;
import projekt.substratum.util.helpers.OverlaysCallback;
import projekt.substratum.util.views.SheetDialog;

import static android.content.Context.ACTIVITY_SERVICE;
import static projekt.substratum.InformationActivity.currentShownLunchBar;
import static projekt.substratum.common.Internal.CIPHER_ALGORITHM;
import static projekt.substratum.common.Internal.COMPILE_ENABLE;
import static projekt.substratum.common.Internal.COMPILE_UPDATE;
import static projekt.substratum.common.Internal.DISABLE;
import static projekt.substratum.common.Internal.DISABLE_MODE;
import static projekt.substratum.common.Internal.ENABLE;
import static projekt.substratum.common.Internal.ENABLE_DISABLE;
import static projekt.substratum.common.Internal.ENABLE_MODE;
import static projekt.substratum.common.Internal.ENCRYPTED_FILE_EXTENSION;
import static projekt.substratum.common.Internal.ENCRYPTION_KEY_EXTRA;
import static projekt.substratum.common.Internal.IV_ENCRYPTION_KEY_EXTRA;
import static projekt.substratum.common.Internal.MAIL_TYPE;
import static projekt.substratum.common.Internal.MIX_AND_MATCH;
import static projekt.substratum.common.Internal.MIX_AND_MATCH_IA_TO_OVERLAYS;
import static projekt.substratum.common.Internal.OVERLAYS_DIR;
import static projekt.substratum.common.Internal.OVERLAY_REFRESH;
import static projekt.substratum.common.Internal.PACKAGE_INSTALL_URI;
import static projekt.substratum.common.Internal.SECRET_KEY_SPEC;
import static projekt.substratum.common.Internal.SHEET_COMMAND;
import static projekt.substratum.common.Internal.START_JOB_ACTION;
import static projekt.substratum.common.Internal.SUPPORTED_ROMS_FILE;
import static projekt.substratum.common.Internal.SWAP_MODE;
import static projekt.substratum.common.Internal.THEME_NAME;
import static projekt.substratum.common.Internal.THEME_PID;
import static projekt.substratum.common.References.DEFAULT_NOTIFICATION_CHANNEL_ID;
import static projekt.substratum.common.References.ENABLE_PACKAGE_LOGGING;
import static projekt.substratum.common.References.EXTERNAL_STORAGE_CACHE;
import static projekt.substratum.common.References.SUBSTRATUM_BUILDER;
import static projekt.substratum.common.References.SUBSTRATUM_PACKAGE;
import static projekt.substratum.common.References.metadataEmail;
import static projekt.substratum.common.References.metadataEncryption;
import static projekt.substratum.common.References.metadataEncryptionValue;
import static projekt.substratum.common.Resources.FRAMEWORK;
import static projekt.substratum.common.Resources.LG_FRAMEWORK;
import static projekt.substratum.common.Resources.SAMSUNG_FRAMEWORK;
import static projekt.substratum.common.Resources.SETTINGS_ICONS;
import static projekt.substratum.common.Resources.SYSTEMUI_HEADERS;
import static projekt.substratum.common.Resources.SYSTEMUI_NAVBARS;
import static projekt.substratum.common.Resources.SYSTEMUI_QSTILES;
import static projekt.substratum.common.Resources.SYSTEMUI_STATUSBARS;
import static projekt.substratum.tabs.OverlaysManager.legacyDisable;
import static projekt.substratum.tabs.OverlaysManager.selectCompileMode;
import static projekt.substratum.tabs.OverlaysManager.selectEnabledDisabled;

public class Overlays extends Fragment {

    public static final String TAG = SUBSTRATUM_BUILDER;
    public static final int THREAD_WAIT_DURATION = 500;
    public ImageView loader_image;
    public TextView loader_string;
    public SheetDialog mCompileDialog;
    public SubstratumBuilder sb;
    public List<OverlaysItem> overlaysLists;
    public List<OverlaysItem> checkedOverlays;
    public OverlaysAdapter mAdapter;
    public String theme_name;
    public String theme_pid;
    public String versionName;
    public NotificationManager mNotifyManager;
    public NotificationCompat.Builder mBuilder;
    public boolean has_failed = false;
    public int fail_count;
    public StringBuilder failed_packages;
    public SharedPreferences prefs;
    public List<String> final_runner;
    public List<String> late_install;
    public boolean mixAndMatchMode = false;
    public boolean enable_mode = false;
    public boolean disable_mode = false;
    public boolean compile_enable_mode = false;
    public boolean enable_disable_mode = false;
    public Boolean is_overlay_active = false;
    public StringBuilder error_logs;
    public double current_amount;
    public double total_amount;
    public String current_dialog_overlay;
    public ProgressBar dialogProgress;
    public ArrayList<String> final_command;
    public AssetManager themeAssetManager;
    public Boolean missingType3 = false;
    public String type1a = "";
    public String type1b = "";
    public String type1c = "";
    public String type2 = "";
    public String type3 = "";
    public String type4 = "";
    public Boolean encrypted = false;
    public Cipher cipher;
    public int overlaysWaiting;
    public ActivityManager am;
    @BindView(R.id.header_loading_bar)
    public ProgressBar progressBar;
    @BindView(R.id.toggle_all_overlays)
    public Switch toggle_all;
    @BindView(R.id.type3_spinner)
    public Spinner base_spinner;
    Context mContext;
    @BindView(R.id.overlayRecyclerView)
    RecyclerView mRecyclerView;
    @BindView(R.id.toggle_all_overlays_text)
    TextView toggle_all_overlays_text;
    @BindView(R.id.toggle_zone)
    RelativeLayout toggleZone;
    @BindView(R.id.swipeRefreshLayout)
    SwipeRefreshLayout swipeRefreshLayout;
    private ArrayList<OverlaysItem> values2;
    private JobReceiver jobReceiver;
    private LocalBroadcastManager localBroadcastManager;
    private RefreshReceiver refreshReceiver;
    private boolean decryptedAssetsExceptionReached;
    private int currentPosition;
    private boolean firstBoot = false;

    /**
     * Display all of the type resources if this is called
     */
    void logTypes() {
        if (ENABLE_PACKAGE_LOGGING) {
            Log.d("Theme Type1a Resource", type1a);
            Log.d("Theme Type1b Resource", type1b);
            Log.d("Theme Type1c Resource", type1c);
            Log.d("Theme Type2  Resource", type2);
            Log.d("Theme Type3  Resource", type3);
            Log.d("Theme Type4  Resource", type4);
        }
    }

    /**
     * We use flags to unify a whole function in OverlaysManager, call this to reset it all!
     * By resetting all, it means that you have finished ALL functions to-be-served for the user
     */
    void resetCompileFlags() {
        is_overlay_active = false;
        compile_enable_mode = false;
        enable_mode = false;
        disable_mode = false;
        enable_disable_mode = false;
    }

    /**
     * Get the activity's view through a fragment for LunchBar invokes
     *
     * @return Returns masterView of {@link InformationActivity}
     */
    public View getActivityView() {
        InformationActivity informationActivity = ((InformationActivity) getActivity());
        if (informationActivity != null) {
            return References.getView(informationActivity);
        }
        return null;
    }

    /**
     * Utilize the shared compile + * mode, but enable the overlays selected afterwards
     */
    private void startCompileEnableMode() {
        if (!is_overlay_active) {
            resetCompileFlags();
            is_overlay_active = true;
            compile_enable_mode = true;
            selectCompileMode(this);
        }
    }

    /**
     * Utilize the shared compile + * mode and do not enable the overlays selected afterwards
     */
    private void startCompileUpdateMode() {
        if (!is_overlay_active) {
            resetCompileFlags();
            is_overlay_active = true;
            selectCompileMode(this);
        }
    }

    /**
     * Disable the selected overlays
     */
    private void startDisable() {
        if (!is_overlay_active) {
            selectEnabledDisabled(this, DISABLE_MODE);
            if (!Systems.checkOMS(mContext)) {
                legacyDisable(this);
            }
        }
    }

    /**
     * Enable the selected overlays
     */
    private void startEnable() {
        if (!is_overlay_active) {
            selectEnabledDisabled(this, ENABLE_MODE);
        }
    }

    /**
     * Swap state mode
     */
    private void startEnableDisable() {
        if (!is_overlay_active) {
            selectEnabledDisabled(this, SWAP_MODE);
        }
    }

    /**
     * Allow InformationActivity to easily set the mix and match mode through the fab menu
     *
     * @param newValue Sets whether to enable mix and match mode (the toggle)
     */
    private void setMixAndMatchMode(boolean newValue) {
        mixAndMatchMode = newValue;
        prefs.edit().putBoolean("enable_swapping_overlays", mixAndMatchMode).apply();
        updateEnabledOverlays();
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_overlays, container, false);
        ButterKnife.bind(this, view);

        mContext = getContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        am = (ActivityManager) mContext.getSystemService(ACTIVITY_SERVICE);

        // Register the theme install receiver to auto refresh the fragment
        refreshReceiver = new RefreshReceiver();
        localBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        localBroadcastManager.registerReceiver(refreshReceiver,
                new IntentFilter(OVERLAY_REFRESH));

        // Configuration changes for overlays are uncaught, so to ensure a graceful reload, add
        // crucial resets here
        mAdapter = null;

        if (getArguments() != null) {
            theme_name = getArguments().getString(THEME_NAME);
            theme_pid = getArguments().getString(THEME_PID);
            String encrypt_check =
                    Packages.getOverlayMetadata(mContext, theme_pid, metadataEncryption);

            if ((encrypt_check != null) && encrypt_check.equals(metadataEncryptionValue)) {
                byte[] encryption_key = getArguments().getByteArray(ENCRYPTION_KEY_EXTRA);
                byte[] iv_encrypt_key = getArguments().getByteArray(IV_ENCRYPTION_KEY_EXTRA);
                try {
                    cipher = Cipher.getInstance(CIPHER_ALGORITHM);
                    cipher.init(
                            Cipher.DECRYPT_MODE,
                            new SecretKeySpec(encryption_key, SECRET_KEY_SPEC),
                            new IvParameterSpec(iv_encrypt_key)
                    );
                    Log.d(TAG, "Loading substratum theme in encrypted assets mode.");
                    encrypted = true;
                } catch (Exception e) {
                    Log.d(TAG,
                            "Loading substratum theme in decrypted assets mode due to an exception.");
                    decryptedAssetsExceptionReached = true;
                }
            } else {
                Log.d(TAG, "Loading substratum theme in decrypted assets mode.");
            }

            if (decryptedAssetsExceptionReached) {
                currentShownLunchBar = Lunchbar.make(
                        getActivityView(),
                        R.string.error_loading_theme_close_text,
                        Lunchbar.LENGTH_INDEFINITE);
                currentShownLunchBar.setAction(getString(R.string.error_loading_theme_close),
                        v -> {
                            currentShownLunchBar.dismiss();
                            assert getActivity() != null;
                            getActivity().finish();
                        });
                currentShownLunchBar.show();
            }
        }

        mixAndMatchMode = prefs.getBoolean("enable_swapping_overlays", false);
        progressBar.setVisibility(View.GONE);

        // Pre-initialize the adapter first so that it won't complain for skipping layout on logs
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        ArrayList<OverlaysItem> empty_array = new ArrayList<>();
        RecyclerView.Adapter empty_adapter = new OverlaysAdapter(empty_array);
        mRecyclerView.setAdapter(empty_adapter);
        toggle_all_overlays_text.setVisibility(View.VISIBLE);

        File work_area = new File(EXTERNAL_STORAGE_CACHE);
        if (!work_area.exists() && work_area.mkdir()) {
            Log.d(TAG, "Updating the internal storage with proper file directories...");
        }

        // Adjust the behaviour of the mix and match toggle in the sheet
        toggle_all.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    try {
                        overlaysLists = mAdapter.getOverlayList();
                        for (int i = 0; i < overlaysLists.size(); i++) {
                            OverlaysItem currentOverlay = overlaysLists.get(i);
                            currentOverlay.setSelected(isChecked);
                            mAdapter.notifyDataSetChanged();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Window has lost connection with the host.");
                    }
                });

        // Allow the user to toggle the select all switch by clicking on the bar above
        toggleZone.setOnClickListener(v -> {
            try {
                toggle_all.setChecked(!toggle_all.isChecked());
                overlaysLists = mAdapter.getOverlayList();
                for (int i = 0; i < overlaysLists.size(); i++) {
                    OverlaysItem currentOverlay = overlaysLists.get(i);
                    currentOverlay.setSelected(toggle_all.isChecked());
                    mAdapter.notifyDataSetChanged();
                }
            } catch (Exception e) {
                Log.e(TAG, "Window has lost connection with the host.");
            }
        });

        // Allow the user to swipe down to refresh the overlay list
        swipeRefreshLayout.setOnRefreshListener(this::refreshList);

        /*
          PLUGIN TYPE 3: Parse each overlay folder to see if they have folder options
         */
        SharedPreferences prefs2 =
                mContext.getSharedPreferences("base_variant", Context.MODE_PRIVATE);
        base_spinner.post(() -> base_spinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> arg0,
                                               View arg1,
                                               int pos,
                                               long id) {
                        prefs2.edit().putInt(theme_pid, pos).apply();
                        refreshList();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                    }
                }));
        base_spinner.setEnabled(false);

        try {
            Resources themeResources = mContext.getPackageManager()
                    .getResourcesForApplication(theme_pid);
            themeAssetManager = themeResources.getAssets();

            List<String> stringArray = new ArrayList<>();

            String[] listArray = themeAssetManager.list("overlays/android");
            Collections.addAll(stringArray, listArray);

            ArrayList<VariantItem> type3 = new ArrayList<>();
            if (stringArray.contains("type3") ||
                    stringArray.contains("type3" + ENCRYPTED_FILE_EXTENSION)) {
                InputStream inputStream;
                if (encrypted) {
                    inputStream = FileOperations.getInputStream(
                            themeAssetManager,
                            "overlays/android/type3" + ENCRYPTED_FILE_EXTENSION,
                            cipher);
                } else {
                    inputStream = themeAssetManager.open("overlays/android/type3");
                }

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream))) {
                    String formatter = String.format(
                            getString(R.string.overlays_variant_substitute), reader.readLine
                                    ());
                    type3.add(new VariantItem(formatter, null));
                } catch (IOException e) {
                    Log.e(TAG, "There was an error parsing asset file!");
                    type3.add(new VariantItem(getString(R.string
                            .overlays_variant_default_3), null));
                }
                inputStream.close();
            } else {
                type3.add(new VariantItem(getString(R.string.overlays_variant_default_3),
                        null));
            }

            if (stringArray.size() > 1) {
                for (int i = 0; i < stringArray.size(); i++) {
                    String current = stringArray.get(i);
                    if (!"res".equals(current) &&
                            !current.contains(".") &&
                            (current.length() >= 6) &&
                            "type3_".equals(current.substring(0, 6))) {
                        type3.add(new VariantItem(current.substring(6), null));
                    }
                }
                SpinnerAdapter adapter1 = new VariantAdapter(getActivity(), type3);
                if (type3.size() > 1) {
                    toggle_all_overlays_text.setVisibility(View.GONE);
                    base_spinner.setVisibility(View.VISIBLE);
                    base_spinner.setAdapter(adapter1);
                    try {
                        Log.d(TAG,
                                "Assigning the spinner position: " + prefs2.getInt(theme_pid, 0));
                        base_spinner.setSelection(prefs2.getInt(theme_pid, 0));
                    } catch (Exception e) {
                        // Should be OutOfBounds, but let's catch everything
                        Log.d(TAG, "Falling back to default spinner position due to an error...");
                        prefs2.edit().putInt(theme_pid, 0).apply();
                        base_spinner.setSelection(0);
                    }
                } else {
                    toggle_all_overlays_text.setVisibility(View.VISIBLE);
                    base_spinner.setVisibility(View.INVISIBLE);
                    refreshList();
                }
            } else {
                toggle_all_overlays_text.setVisibility(View.VISIBLE);
                base_spinner.setVisibility(View.INVISIBLE);
                refreshList();
            }
        } catch (Exception e) {
            if (base_spinner.getVisibility() == View.VISIBLE) {
                toggle_all_overlays_text.setVisibility(View.VISIBLE);
                base_spinner.setVisibility(View.INVISIBLE);
            }
            e.printStackTrace();
            Log.e(TAG, "Could not parse list of base options for this theme!");
        }

        // Enable job listener
        jobReceiver = new JobReceiver();
        IntentFilter intentFilter =
                new IntentFilter(getClass().getSimpleName() + START_JOB_ACTION);
        localBroadcastManager.registerReceiver(jobReceiver, intentFilter);

        // Enable the instance to be retained for LogChar invoke after configuration change
        setRetainInstance(true);
        if ((error_logs != null) && (error_logs.length() > 0)) {
            invokeLogCharLunchBar(mContext);
        }
        return view;
    }

    /**
     * Return a new list of enabled overlays
     *
     * @return new list of enabled overlays
     */
    List<String> updateEnabledOverlays() {
        return new ArrayList<>(ThemeManager.listOverlays(
                mContext,
                ThemeManager.STATE_ENABLED
        ));
    }

    /**
     * We need to be able to check the active notifications before throwing a new one
     *
     * @return If true, then there's an existing Substratum notification needed to be cleared
     */
    boolean checkActiveNotifications() {
        StatusBarNotification[] activeNotifications = mNotifyManager.getActiveNotifications();
        for (StatusBarNotification statusBarNotification : activeNotifications) {
            if (statusBarNotification.getPackageName().equals(mContext.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * When the compilation has failed, we need a way to conclude the process gracefully
     *
     * @param context Self explanatory, bud.
     */
    void failedFunction(Context context) {
        // Add dummy intent to be able to close the notification on click
        Intent notificationIntent = new Intent(context, getClass());
        notificationIntent.putExtra("theme_name", theme_name);
        notificationIntent.putExtra("theme_pid", theme_pid);
        notificationIntent.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent =
                PendingIntent.getActivity(context, 0, notificationIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT);

        // Closing off the persistent notification
        mNotifyManager.cancel(References.notification_id_compiler);
        mBuilder = new NotificationCompat.Builder(context, DEFAULT_NOTIFICATION_CHANNEL_ID);
        mBuilder.setAutoCancel(true);
        mBuilder.setProgress(0, 0, false);
        mBuilder.setOngoing(false);
        mBuilder.setContentIntent(intent);
        mBuilder.setSmallIcon(R.drawable.notification_warning_icon);
        mBuilder.setContentTitle(context.getString(R.string.notification_done_title));
        mBuilder.setContentText(context.getString(R.string.notification_some_errors_found));
        if (prefs.getBoolean("vibrate_on_compiled", false)) {
            mBuilder.setVibrate(new long[]{200L, 400L, 200L, 1000L});
        }
        mNotifyManager.notify(References.notification_id_compiler, mBuilder.build());

        Toast.makeText(
                context,
                context.getString(R.string.toast_compiled_updated_with_errors),
                Toast.LENGTH_LONG).show();

        if (prefs.getBoolean("autosave_logchar", true)) {
            new SendErrorReport(
                    context,
                    theme_pid,
                    error_logs.toString(),
                    failed_packages.toString(),
                    true
            ).execute();
        }

        invokeLogCharLunchBar(context);
    }

    /**
     * Invoke the LogChar lunch bar
     *
     * @param context Self explanatory, bud.
     */
    private void invokeLogCharLunchBar(Context context) {
        CharSequence errorLogCopy = new StringBuilder(error_logs);
        error_logs = new StringBuilder();
        currentShownLunchBar = Lunchbar.make(
                getActivityView(),
                R.string.logcat_snackbar_text,
                Lunchbar.LENGTH_INDEFINITE);
        currentShownLunchBar.setAction(getString(R.string.logcat_snackbar_button), view -> {
            currentShownLunchBar.dismiss();
            invokeLogCharDialog(context, errorLogCopy);
        });
        currentShownLunchBar.show();
    }

    /**
     * Invoke the LogChar dialog with the logs inside
     *
     * @param context Self explanatory, bud.
     * @param logs    The series of logs that needs to be thrown to the user
     */
    private void invokeLogCharDialog(Context context, CharSequence logs) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context)
                .setTitle(R.string.logcat_dialog_title)
                .setMessage("\n" + logs)
                .setNeutralButton(R.string
                        .customactivityoncrash_error_activity_error_details_close, null)
                .setNegativeButton(R.string
                                .customactivityoncrash_error_activity_error_details_copy,
                        (dialog1, which) -> {
                            References.copyToClipboard(context,
                                    "substratum_log", logs.toString());
                            currentShownLunchBar = Lunchbar.make(
                                    getActivityView(),
                                    R.string.logcat_dialog_copy_success,
                                    Lunchbar.LENGTH_LONG);
                            currentShownLunchBar.show();
                        });

        if (Packages.getOverlayMetadata(context, theme_pid, metadataEmail) != null) {
            builder.setPositiveButton(getString(R.string.logcat_send), (dialogInterface, i) ->
                    new SendErrorReport(
                            context,
                            theme_pid,
                            logs.toString(),
                            failed_packages.toString(),
                            false
                    ).execute());
        }
        builder.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            localBroadcastManager.unregisterReceiver(refreshReceiver);
        } catch (IllegalArgumentException e) {
            // Unregistered already
        }
        try {
            localBroadcastManager.unregisterReceiver(jobReceiver);
        } catch (IllegalArgumentException e) {
            // Unregistered already
        }
    }

    /**
     * Analyze whether the series of compiled overlays needs to throw out the current session
     * This is dependent on the framework and self-package names.
     *
     * @param context Self explanatory, bud.
     * @return If true, the system will automatically recreate
     */
    boolean needsRecreate(Context context) {
        for (OverlaysItem oi : checkedOverlays) {
            String packageName = oi.getPackageName();
            if (packageName.equals(FRAMEWORK) ||
                    packageName.equals(SUBSTRATUM_PACKAGE)) {
                if (!enable_mode &&
                        !disable_mode &&
                        !enable_disable_mode &&
                        ThemeManager.isOverlayEnabled(context, oi.getFullOverlayParameters())) {
                    return false;
                } else if (enable_mode ||
                        disable_mode ||
                        compile_enable_mode ||
                        enable_disable_mode) {
                    return false;
                }
            }
        }
        return Systems.checkOMS(mContext) && !has_failed;
    }

    /**
     * Type1s are the variants that reside at the root of the overlay target folder. These are
     * hotswappable files to the res/values folders
     *
     * @param packageName Package name of the target overlay
     * @param variantName Variant name of the type1 file
     * @return Returns the VariantItem object used to populate the RecyclerView
     */
    private VariantItem setTypeOneSpinners(String packageName,
                                           String variantName) {
        InputStream inputStream = null;
        try {
            if (encrypted) {
                inputStream = FileOperations.getInputStream(
                        themeAssetManager,
                        OVERLAYS_DIR + '/' + packageName + "/type1" + variantName +
                                ENCRYPTED_FILE_EXTENSION,
                        cipher);
            } else {
                inputStream = themeAssetManager.open(
                        OVERLAYS_DIR + '/' + packageName + "/type1" + variantName);
            }
        } catch (IOException ioe) {
            // Suppress warning
        }

        // Parse current default types on type3 base resource folders
        String parsedVariant = "";
        try {
            if (base_spinner.getSelectedItemPosition() != 0) {
                parsedVariant = base_spinner.getSelectedItem().toString().replaceAll("\\s+", "");
            }
        } catch (NullPointerException npe) {
            // Suppress warning
        }
        String suffix = ((!parsedVariant.isEmpty()) ? ("/type3_" + parsedVariant) : "/res");

        // Type1 Spinner Text Adjustments
        assert inputStream != null;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream))) {
            // This adjusts it so that we have the spinner text set
            String formatter = String.format(
                    getString(R.string.overlays_variant_substitute),
                    reader.readLine());
            // This is the default type1 xml hex, if present
            String hex = null;

            if (encrypted) {
                try (InputStream name = FileOperations.getInputStream(
                        themeAssetManager,
                        OVERLAYS_DIR + '/' + packageName + suffix +
                                "/values/type1" + variantName + ".xml" +
                                ENCRYPTED_FILE_EXTENSION,
                        cipher)) {
                    hex = Packages.getOverlayResource(name);
                } catch (IOException e) {
                    // Suppress warning
                }
            } else {
                try (InputStream name = themeAssetManager.open(
                        OVERLAYS_DIR + '/' + packageName + suffix +
                                "/values/type1" + variantName + ".xml")) {
                    hex = Packages.getOverlayResource(name);
                } catch (IOException e) {
                    // Suppress warning
                }
            }
            return new VariantItem(formatter, hex);
        } catch (Exception e) {
            // When erroring out, put the default spinner text
            Log.d(TAG, "Falling back to default base variant text...");
            String hex = null;
            if (encrypted) {
                try (InputStream input = FileOperations.getInputStream(
                        themeAssetManager,
                        OVERLAYS_DIR + '/' + packageName +
                                suffix + "/values/type1" + variantName + ".xml" +
                                ENCRYPTED_FILE_EXTENSION,
                        cipher)) {
                    hex = Packages.getOverlayResource(input);
                } catch (IOException ioe) {
                    // Suppress warning
                }
            } else {
                try (InputStream input = themeAssetManager.open(OVERLAYS_DIR +
                        '/' + packageName + suffix + "/values/type1" + variantName + ".xml")) {
                    hex = Packages.getOverlayResource(input);
                } catch (IOException ioe) {
                    // Suppress warning
                }
            }
            switch (variantName) {
                case "a":
                    return new VariantItem(
                            getString(R.string.overlays_variant_default_1a), hex);
                case "b":
                    return new VariantItem(
                            getString(R.string.overlays_variant_default_1b), hex);
                case "c":
                    return new VariantItem(
                            getString(R.string.overlays_variant_default_1c), hex);
                default:
                    return null;
            }
        }
    }

    /**
     * Type2 and Type4s are the variants that reside at the root of the overlay target folder.
     * These are interchangeable resource files to the res or assets folders
     *
     * @param inputStreamReader An input stream reader to check the names of the folders
     * @param typeValue         A specified number between 2 and 4 that consolidated the functions
     *                          from separate methods, so that we can use one method to house both.
     * @return Returns the VariantItem object used to populate the RecyclerView
     */
    private VariantItem setTypeTwoFourSpinners(
            InputStreamReader inputStreamReader,
            Integer typeValue) {
        try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
            return new VariantItem(String.format(
                    getString(R.string.overlays_variant_substitute), reader.readLine()), null);
        } catch (Exception e) {
            Log.d(TAG, "Falling back to default base variant text...");
            switch (typeValue) {
                case 2:
                    return new VariantItem(
                            getString(R.string.overlays_variant_default_2),
                            null);
                case 4:
                    return new VariantItem(
                            getString(R.string.overlays_variant_default_4),
                            null);
            }
        }
        return null;
    }

    /**
     * This function is so that we get the color previews for the type1 variants, which takes the
     * first object within the XML file and displays it as a circular preview
     *
     * @param currentTypeOneObject Name of the currently processing type1 object
     * @param packageName          Package name of the target overlay
     * @return Returns the VariantItem object used to populate the RecyclerView
     */
    private VariantItem setTypeOneHexAndSpinner(
            String currentTypeOneObject,
            String packageName) {
        if (encrypted) {
            try (InputStream inputStream = FileOperations.getInputStream(themeAssetManager,
                    OVERLAYS_DIR + "/" + packageName + '/' + currentTypeOneObject, cipher)) {
                String hex = Packages.getOverlayResource(inputStream);

                return new VariantItem(
                        currentTypeOneObject.substring(7, currentTypeOneObject.length() - 8), hex);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try (InputStream inputStream = themeAssetManager.open(
                    OVERLAYS_DIR + "/" + packageName + '/' + currentTypeOneObject)) {
                String hex = Packages.getOverlayResource(inputStream);

                return new VariantItem(
                        currentTypeOneObject.substring(7, currentTypeOneObject.length() - 4), hex);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Handle all the startActivityWithResult calls
     *
     * @param requestCode Code used to feed into activity start
     * @param resultCode  Code that was fed to this activity after succession
     * @param data        Result from the received startActivityWithResult
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 2486:
                FileOperations.delete(mContext,
                        new File(late_install.get(0)).getAbsolutePath());
                if ((late_install != null) && !late_install.isEmpty())
                    late_install.remove(0);
                if (!late_install.isEmpty()) {
                    installMultipleAPKs();
                }
        }
    }

    /**
     * A special function for Samsung related devices to install overlays by queue based, so that
     * it won't overwhelm the user on multiple overlays selected.
     */
    private void installMultipleAPKs() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = FileProvider.getUriForFile(
                mContext,
                mContext.getApplicationContext().getPackageName() + ".provider",
                new File(late_install.get(0)));
        intent.setDataAndType(
                uri,
                PACKAGE_INSTALL_URI);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, 2486);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!firstBoot && !toggle_all.isChecked()) {
            firstBoot = true;
            refreshList();
        }
    }

    /**
     * Simple function to call when requesting to reload the list. This should contain method calls
     * to refresh the list gracefully including disabling certain views and re-enabling them in the
     * subsequent calls
     */
    private void refreshList() {
        currentPosition = ((LinearLayoutManager)
                mRecyclerView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
        toggle_all.setChecked(false);
        if (base_spinner.getSelectedItemPosition() > 0) {
            new LoadOverlays(this).execute(base_spinner.getSelectedItem().toString());
        } else {
            new LoadOverlays(this).execute("");
        }
    }

    /**
     * A class to work in conjunction with the LogChar dialog, to send reports to themers.
     * At the current moment, this supports sending through Email, Telegram and many Android chat
     * clients that support attaching files.
     */
    private static class SendErrorReport extends AsyncTask<Void, Void, File> {
        private WeakReference<Context> ref;
        private String themePid;
        private String errorLog;
        private String themeName;
        private String themeAuthor;
        private String themeEmail;
        private String emailSubject;
        private String emailBody;
        private String failedPackages;
        private Boolean autosaveInstance;
        private ProgressDialog progressDialog;

        SendErrorReport(Context context,
                        String themePid,
                        String errorLog,
                        String failedPackages,
                        Boolean autosaveInstance) {
            super();
            this.ref = new WeakReference<>(context);
            this.themePid = themePid;
            this.errorLog = errorLog;
            this.failedPackages = failedPackages;
            this.autosaveInstance = autosaveInstance;
            this.themeName = Packages.getPackageName(context, themePid);
            this.themeAuthor = Packages.getOverlayMetadata(context, themePid,
                    References.metadataAuthor);
            this.themeEmail = Packages.getOverlayMetadata(context, themePid,
                    References.metadataEmail);
            this.emailSubject = String.format(
                    context.getString(R.string.logcat_email_subject), themeName);
            this.emailBody = String.format(
                    context.getString(R.string.logcat_email_body), themeAuthor, themeName);
        }

        @Override
        protected void onPreExecute() {
            Context context = ref.get();
            if ((context != null) && !autosaveInstance) {
                progressDialog = new ProgressDialog(context);
                progressDialog.setIndeterminate(true);
                progressDialog.setCancelable(false);
                progressDialog.setMessage(
                        context.getString(R.string.logcat_processing_dialog));
                progressDialog.show();
            }
        }

        @Override
        protected File doInBackground(Void... sUrl) {
            Context context = ref.get();
            if (context != null) {
                String rom = Systems.checkFirmwareSupport(context,
                        context.getString(R.string.supported_roms_url),
                        SUPPORTED_ROMS_FILE);
                String theme_version = Packages.getAppVersion(context, themePid);
                String rom_version = Build.VERSION.RELEASE + " - " +
                        (!rom.isEmpty() ? rom : "Unknown");

                String device = Build.MODEL + " (" + Build.DEVICE + ") " +
                        '[' + Build.FINGERPRINT + ']';
                String xposed = References.checkXposedVersion();
                if (!xposed.isEmpty()) device += " {" + xposed + '}';

                String attachment = String.format(
                        context.getString(R.string.logcat_attachment_body),
                        themeName,
                        device,
                        rom_version,
                        String.valueOf(BuildConfig.VERSION_CODE),
                        theme_version,
                        failedPackages,
                        errorLog);

                File log = null;
                if (autosaveInstance) {
                    References.writeLogCharFile(themePid, attachment);
                } else {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HH:mm",
                            Locale.US);
                    log = new File(EXTERNAL_STORAGE_CACHE +
                            "/theme_error-" + dateFormat.format(new Date()) + ".txt");
                    try (FileWriter fw = new FileWriter(log, false);
                         BufferedWriter out = new BufferedWriter(fw)) {
                        out.write(attachment);
                    } catch (IOException e) {
                        // Suppress exception
                    }
                }
                return log;
            }
            return null;
        }

        @Override
        protected void onPostExecute(File result) {
            Context context = ref.get();
            if (context != null) {
                if (!autosaveInstance && (result != null)) {
                    if (progressDialog != null) progressDialog.dismiss();

                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType(MAIL_TYPE);
                    i.putExtra(Intent.EXTRA_EMAIL, new String[]{themeEmail});
                    i.putExtra(Intent.EXTRA_SUBJECT, emailSubject);
                    i.putExtra(Intent.EXTRA_TEXT, emailBody);
                    i.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                            ref.get(),
                            ref.get().getPackageName() + ".provider",
                            result));
                    try {
                        context.startActivity(Intent.createChooser(i,
                                context.getString(R.string.logcat_email_activity)));
                    } catch (ActivityNotFoundException ex) {
                        Toast.makeText(context,
                                R.string.logcat_email_activity_error,
                                Toast.LENGTH_LONG)
                                .show();
                    }
                }
            }
        }
    }

    /**
     * The beef of the {@link #refreshList()} method, where we have the list refreshing
     * asynchronously.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    private static class LoadOverlays extends AsyncTask<String, Integer, String> {
        private WeakReference<Overlays> ref;

        LoadOverlays(Overlays fragment) {
            super();
            ref = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            Overlays fragment = ref.get();
            if (fragment != null) {
                fragment.swipeRefreshLayout.setRefreshing(true);
                fragment.mRecyclerView.setEnabled(false);
                fragment.toggle_all.setEnabled(false);
                fragment.toggle_all.setChecked(false);
                fragment.base_spinner.setEnabled(false);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Overlays fragment = ref.get();
            if (fragment != null) {
                fragment.swipeRefreshLayout.setRefreshing(false);
                fragment.mRecyclerView.setEnabled(true);
                fragment.toggle_all.setEnabled(true);
                fragment.base_spinner.setEnabled(true);
                // On the first start, when adapter is null, use the old style of refreshing RV
                if (fragment.mAdapter == null) {
                    fragment.mAdapter = new OverlaysAdapter(fragment.values2);
                    fragment.mRecyclerView.setAdapter(fragment.mAdapter);
                    fragment.mAdapter.notifyDataSetChanged();
                    fragment.mRecyclerView.setVisibility(View.VISIBLE);
                }
                // If the adapter isn't null, reload using DiffUtil
                DiffUtil.DiffResult diffResult =
                        DiffUtil.calculateDiff(
                                new OverlaysCallback(
                                        fragment.mAdapter.getList(), fragment.values2));
                fragment.mAdapter.setList(fragment.values2);
                diffResult.dispatchUpdatesTo(fragment.mAdapter);
                // Scroll to the proper position where the user was
                ((LinearLayoutManager)
                        fragment.mRecyclerView.getLayoutManager()).
                        scrollToPositionWithOffset(fragment.currentPosition, 20);
                if (fragment.mAdapter != null) fragment.mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            Overlays fragment = ref.get();
            if (fragment != null) {
                Context context = fragment.getActivity();
                // Refresh asset manager
                try {
                    assert context != null;
                    Resources themeResources = context.getPackageManager()
                            .getResourcesForApplication(fragment.theme_pid);
                    fragment.themeAssetManager = themeResources.getAssets();

                    // Get the current theme_pid's versionName so that we can version our overlays
                    fragment.versionName = Packages.getAppVersion(context, fragment.theme_pid);
                    List<String> state5overlays = fragment.updateEnabledOverlays();
                    String parse1_themeName = fragment.theme_name.replaceAll("\\s+", "");
                    fragment.values2 = new ArrayList<>();

                    // Buffer the initial values list so that we get the list of packages
                    // inside this theme
                    Collection<String> overlaysFolder = new ArrayList<>();
                    try {
                        String[] overlayList =
                                fragment.themeAssetManager.list(OVERLAYS_DIR);
                        Collections.addAll(overlaysFolder, overlayList);
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }

                    Boolean showDangerous =
                            !fragment.prefs.getBoolean("show_dangerous_samsung_overlays", false);

                    List<String> values = new ArrayList<>(overlaysFolder.stream().filter
                            (package_name -> (Packages
                                    .isPackageInstalled(context, package_name) ||
                                    projekt.substratum.common.Resources.allowedSystemUIOverlay
                                            (package_name) ||
                                    projekt.substratum.common.Resources.allowedSettingsOverlay
                                            (package_name) ||
                                    projekt.substratum.common.Resources.allowedFrameworkOverlay
                                            (package_name)) &&
                                    (!showDangerous || !ThemeManager.blacklisted(
                                            package_name,
                                            Systems.isSamsung(context) &&
                                                    !Packages.isSamsungTheme(
                                                            context, fragment.theme_pid))))
                            .collect(Collectors.toList()));

                    // Create the map for {package name: package identifier}
                    Map<String, String> unsortedMap = new HashMap<>();

                    // Then let's convert all the package names to their app names
                    for (int i = 0; i < values.size(); i++) {
                        try {
                            if (projekt.substratum.common.Resources.allowedSystemUIOverlay(values
                                    .get(i))) {
                                String package_name = "";
                                switch (values.get(i)) {
                                    case SYSTEMUI_HEADERS:
                                        package_name = context.getString(R.string.systemui_headers);
                                        break;
                                    case SYSTEMUI_NAVBARS:
                                        package_name = context.getString(R.string
                                                .systemui_navigation);
                                        break;
                                    case SYSTEMUI_STATUSBARS:
                                        package_name = context.getString(R.string
                                                .systemui_statusbar);
                                        break;
                                    case SYSTEMUI_QSTILES:
                                        package_name = context.getString(R.string
                                                .systemui_qs_tiles);
                                        break;
                                }
                                unsortedMap.put(values.get(i), package_name);
                            } else if (projekt.substratum.common.Resources.allowedSettingsOverlay
                                    (values.get(i))) {
                                String package_name = "";
                                switch (values.get(i)) {
                                    case SETTINGS_ICONS:
                                        package_name = context.getString(R.string.settings_icons);
                                        break;
                                }
                                unsortedMap.put(values.get(i), package_name);
                            } else if (projekt.substratum.common.Resources.allowedFrameworkOverlay
                                    (values.get(i))) {
                                String package_name = "";
                                switch (values.get(i)) {
                                    case SAMSUNG_FRAMEWORK:
                                        package_name = context.getString(
                                                R.string.samsung_framework);
                                        break;
                                    case LG_FRAMEWORK:
                                        package_name = context.getString(
                                                R.string.lg_framework);
                                        break;
                                }
                                unsortedMap.put(values.get(i), package_name);
                            } else if (projekt.substratum.common.Resources.allowedAppOverlay
                                    (values.get(i))) {
                                ApplicationInfo applicationInfo = context.getPackageManager()
                                        .getApplicationInfo
                                                (values.get(i), 0);
                                String packageTitle = context.getPackageManager()
                                        .getApplicationLabel
                                                (applicationInfo).toString();
                                unsortedMap.put(values.get(i), packageTitle);
                            }
                        } catch (Exception e) {
                            // Exception
                        }
                    }

                    // Sort the values list
                    List<Pair<String, String>> sortedMap =
                            MapUtils.sortMapByValues(unsortedMap);

                    // Now let's add the new information so that the adapter can recognize custom
                    // method
                    // calls
                    String parse2_themeName = parse1_themeName.replaceAll("[^a-zA-Z0-9]+",
                            "");
                    for (Pair<String, String> entry : sortedMap) {
                        String package_name = entry.second;
                        String package_identifier = entry.first;

                        try {
                            List<String> typeArray = new ArrayList<>();

                            Object typeArrayRaw = fragment.themeAssetManager.list(
                                    OVERLAYS_DIR + '/' + package_identifier);

                            // Sort the typeArray so that the types are asciibetical
                            Collections.addAll(typeArray, (String[]) typeArrayRaw);
                            Collections.sort(typeArray);

                            // Sort the typeArray so that the types are asciibetical
                            Collections.sort(typeArray);

                            // Let's start adding the type xmls to be parsed into the spinners
                            ArrayList<VariantItem> type1a = new ArrayList<>();
                            if (typeArray.contains("type1a") ||
                                    typeArray.contains("type1a" + ENCRYPTED_FILE_EXTENSION)) {
                                type1a.add(fragment.setTypeOneSpinners(
                                        package_identifier, "a"));
                            }

                            ArrayList<VariantItem> type1b = new ArrayList<>();
                            if (typeArray.contains("type1b") ||
                                    typeArray.contains("type1b" + ENCRYPTED_FILE_EXTENSION)) {
                                type1b.add(fragment.setTypeOneSpinners(
                                        package_identifier, "b"));
                            }

                            ArrayList<VariantItem> type1c = new ArrayList<>();
                            if (typeArray.contains("type1c") ||
                                    typeArray.contains("type1c" + ENCRYPTED_FILE_EXTENSION)) {
                                type1c.add(fragment.setTypeOneSpinners(
                                        package_identifier, "c"));
                            }

                            boolean type2checker = false;
                            for (int i = 0; i < typeArray.size(); i++) {
                                if (typeArray.get(i).startsWith("type2_")) {
                                    type2checker = true;
                                    break;
                                }
                            }
                            ArrayList<VariantItem> type2 = new ArrayList<>();
                            if (typeArray.contains("type2") ||
                                    typeArray.contains("type2" + ENCRYPTED_FILE_EXTENSION) ||
                                    type2checker) {
                                InputStreamReader inputStreamReader = null;
                                try {
                                    inputStreamReader =
                                            new InputStreamReader(
                                                    FileOperations.getInputStream(
                                                            fragment.themeAssetManager,
                                                            OVERLAYS_DIR + '/' +
                                                                    package_identifier +
                                                                    (fragment.encrypted ?
                                                                            "/type2" +
                                                                                    ENCRYPTED_FILE_EXTENSION :
                                                                            "/type2"),
                                                            (fragment.encrypted ?
                                                                    fragment.cipher :
                                                                    null)));
                                } catch (Exception e) {
                                    // Suppress warning
                                }
                                type2.add(fragment.setTypeTwoFourSpinners(inputStreamReader, 2));
                            }

                            // Begin Type4 initialization
                            boolean type4checker = false;
                            for (int i = 0; i < typeArray.size(); i++) {
                                if (typeArray.get(i).startsWith("type4_")) {
                                    type4checker = true;
                                    break;
                                }
                            }
                            ArrayList<VariantItem> type4 = new ArrayList<>();
                            if (typeArray.contains("type4") ||
                                    typeArray.contains("type4" + ENCRYPTED_FILE_EXTENSION) ||
                                    type4checker) {
                                InputStreamReader inputStreamReader = null;
                                try {
                                    inputStreamReader =
                                            new InputStreamReader(
                                                    FileOperations.getInputStream(
                                                            fragment.themeAssetManager,
                                                            OVERLAYS_DIR + '/' +
                                                                    package_identifier +
                                                                    (fragment.encrypted ?
                                                                            "/type4" +
                                                                                    ENCRYPTED_FILE_EXTENSION :
                                                                            "/type4"),
                                                            (fragment.encrypted ?
                                                                    fragment.cipher :
                                                                    null)));
                                } catch (Exception e) {
                                    // Suppress warning
                                }
                                type4.add(fragment.setTypeTwoFourSpinners(inputStreamReader, 4));
                            }

                            if (typeArray.size() > 1) {
                                for (int i = 0; i < typeArray.size(); i++) {
                                    String current = typeArray.get(i);
                                    if (!"res".equals(current)) {
                                        if (current.contains(".xml")) {
                                            switch (current.substring(0, 7)) {
                                                case "type1a_":
                                                    type1a.add(
                                                            fragment.setTypeOneHexAndSpinner(
                                                                    current, package_identifier));
                                                    break;
                                                case "type1b_":
                                                    type1b.add(
                                                            fragment.setTypeOneHexAndSpinner(
                                                                    current, package_identifier));
                                                    break;
                                                case "type1c_":
                                                    type1c.add(
                                                            fragment.setTypeOneHexAndSpinner(
                                                                    current, package_identifier));
                                                    break;
                                            }
                                        } else if (!current.contains(".") &&
                                                (current.length() > 5)) {
                                            if ("type2_".equals(current.substring(0, 6))) {
                                                type2.add(
                                                        new VariantItem(
                                                                current.substring(6), null));
                                            } else if ("type4_".equals(current.substring(0, 6))) {
                                                type4.add(
                                                        new VariantItem(
                                                                current.substring(6), null));
                                            }
                                        }
                                    }
                                }

                                VariantAdapter adapter1 = new VariantAdapter(context, type1a);
                                VariantAdapter adapter2 = new VariantAdapter(context, type1b);
                                VariantAdapter adapter3 = new VariantAdapter(context, type1c);
                                VariantAdapter adapter4 = new VariantAdapter(context, type2);
                                VariantAdapter adapter5 = new VariantAdapter(context, type4);

                                boolean adapterOneChecker = type1a.isEmpty();
                                boolean adapterTwoChecker = type1b.isEmpty();
                                boolean adapterThreeChecker = type1c.isEmpty();
                                boolean adapterFourChecker = type2.isEmpty();
                                boolean adapterFiveChecker = type4.isEmpty();

                                OverlaysItem overlaysItem =
                                        new OverlaysItem(
                                                parse2_themeName,
                                                package_name,
                                                package_identifier,
                                                false,
                                                (adapterOneChecker ? null : adapter1),
                                                (adapterTwoChecker ? null : adapter2),
                                                (adapterThreeChecker ? null : adapter3),
                                                (adapterFourChecker ? null : adapter4),
                                                (adapterFiveChecker ? null : adapter5),
                                                context,
                                                fragment.versionName,
                                                sUrl[0],
                                                state5overlays,
                                                Systems.checkOMS(context),
                                                fragment.getActivityView());
                                fragment.values2.add(overlaysItem);
                            } else {
                                // At this point, there is no spinner adapter, so it should be null
                                OverlaysItem overlaysItem =
                                        new OverlaysItem(
                                                parse2_themeName,
                                                package_name,
                                                package_identifier,
                                                false,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null,
                                                context,
                                                fragment.versionName,
                                                sUrl[0],
                                                state5overlays,
                                                Systems.checkOMS(context),
                                                fragment.getActivityView());
                                fragment.values2.add(overlaysItem);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    // Consume window disconnection
                }
            }
            return null;
        }
    }

    /**
     * A receiver to process the floating action menu sheet entries from InformationActivity
     * {@link projekt.substratum.InformationActivity}
     */
    class JobReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!isAdded()) return;

            String command = intent.getStringExtra(SHEET_COMMAND);
            switch (command) {
                case COMPILE_ENABLE:
                    if (mAdapter != null) startCompileEnableMode();
                    break;
                case COMPILE_UPDATE:
                    if (mAdapter != null) startCompileUpdateMode();
                    break;
                case DISABLE:
                    if (mAdapter != null) startDisable();
                    break;
                case ENABLE:
                    if (mAdapter != null) startEnable();
                    break;
                case ENABLE_DISABLE:
                    if (mAdapter != null) startEnableDisable();
                    break;
                case MIX_AND_MATCH:
                    if (mAdapter != null) {
                        setMixAndMatchMode(intent.getBooleanExtra(
                                MIX_AND_MATCH_IA_TO_OVERLAYS, false));
                    }
                    break;
            }
        }
    }

    /**
     * A receiver to refresh the list after a package has been modified
     * {@link projekt.substratum.services.packages.PackageModificationDetector}
     */
    protected class RefreshReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Systems.isSamsung(context) && Root.checkRootAccess()) {
                if (overlaysWaiting > 0) {
                    --overlaysWaiting;
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
            refreshList();
        }
    }
}