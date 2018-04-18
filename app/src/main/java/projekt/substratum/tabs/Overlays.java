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
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import projekt.substratum.BuildConfig;
import projekt.substratum.InformationActivity;
import projekt.substratum.R;
import projekt.substratum.adapters.tabs.overlays.OverlaysAdapter;
import projekt.substratum.adapters.tabs.overlays.OverlaysItem;
import projekt.substratum.adapters.tabs.overlays.VariantAdapter;
import projekt.substratum.adapters.tabs.overlays.VariantItem;
import projekt.substratum.common.Internal;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.databinding.TabOverlaysBinding;
import projekt.substratum.util.compilers.SubstratumBuilder;
import projekt.substratum.util.helpers.Root;
import projekt.substratum.util.views.Lunchbar;
import projekt.substratum.util.views.SheetDialog;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.Context.NOTIFICATION_SERVICE;
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
import static projekt.substratum.common.Internal.THEME_NAME;
import static projekt.substratum.common.Internal.THEME_PID;
import static projekt.substratum.common.Internal.TYPE1A_PREFIX;
import static projekt.substratum.common.Internal.TYPE1B_PREFIX;
import static projekt.substratum.common.Internal.TYPE1C_PREFIX;
import static projekt.substratum.common.Internal.TYPE2_PREFIX;
import static projekt.substratum.common.Internal.TYPE4_PREFIX;
import static projekt.substratum.common.Internal.XML_EXTENSION;
import static projekt.substratum.common.Packages.isPackageInstalled;
import static projekt.substratum.common.References.DEFAULT_NOTIFICATION_CHANNEL_ID;
import static projekt.substratum.common.References.EXTERNAL_STORAGE_CACHE;
import static projekt.substratum.common.References.SUBSTRATUM_BUILDER;
import static projekt.substratum.common.References.SUBSTRATUM_LOG;
import static projekt.substratum.common.References.metadataEmail;
import static projekt.substratum.common.References.metadataEncryption;
import static projekt.substratum.common.References.metadataEncryptionValue;
import static projekt.substratum.common.Resources.LG_FRAMEWORK;
import static projekt.substratum.common.Resources.SAMSUNG_FRAMEWORK;
import static projekt.substratum.common.Resources.SETTINGS_ICONS;
import static projekt.substratum.common.Resources.SYSTEMUI_HEADERS;
import static projekt.substratum.common.Resources.SYSTEMUI_NAVBARS;
import static projekt.substratum.common.Resources.SYSTEMUI_QSTILES;
import static projekt.substratum.common.Resources.SYSTEMUI_STATUSBARS;
import static projekt.substratum.common.Resources.allowedAppOverlay;
import static projekt.substratum.common.Resources.allowedFrameworkOverlay;
import static projekt.substratum.common.Resources.allowedSettingsOverlay;
import static projekt.substratum.common.Resources.allowedSystemUIOverlay;
import static projekt.substratum.tabs.OverlaysManager.legacyDisable;
import static projekt.substratum.tabs.OverlaysManager.selectEnabledDisabled;
import static projekt.substratum.tabs.OverlaysManager.selectStateMode;
import static projekt.substratum.util.helpers.MapUtils.sortMapByValues;

public class Overlays extends Fragment {

    public static AsyncTask mainLoader = null;
    public static RecyclerView recyclerView;
    // Theme instance based variables, used globally amongst all Overlays* files
    public String themeName;
    public String themePid;
    public String themeVersion;
    public Cipher themeCipher;
    public boolean encrypted = false;
    public boolean mixAndMatchMode = false;
    public OverlaysInstance currentInstance = OverlaysInstance.getInstance();
    public SubstratumBuilder compileInstance;
    public List<OverlaysItem> overlayItemList;
    public List<String> currentInstanceOverlays;
    // Begin functional variables with no theme-related information
    public SheetDialog compileDialog;
    public SharedPreferences prefs;
    public OverlaysAdapter overlaysAdapter;
    public NotificationManager notifyManager;
    public NotificationCompat.Builder builder;
    public ProgressBar dialogProgress;
    public AssetManager themeAssetManager;
    public ActivityManager activityManager;
    public ProgressBar progressBar;
    public Switch toggleAll;
    public Spinner baseSpinner;
    Context context;
    TextView toggleAllOverlaysText;
    RelativeLayout toggleZone;
    SwipeRefreshLayout swipeRefreshLayout;
    // RecyclerView temporaries
    private Integer recyclerViewPosition;
    // Broadcast receivers
    private JobReceiver jobReceiver;
    private LocalBroadcastManager localBroadcastManager;
    private RefreshReceiver refreshReceiver;
    private boolean firstStart = true;

    /**
     * Get the activity's view through a fragment for LunchBar invokes
     *
     * @return Returns masterView of {@link InformationActivity}
     */
    public View getActivityView() {
        InformationActivity informationActivity = ((InformationActivity) getActivity());
        if (informationActivity != null) {
            View coordinatorLayout = References.getCoordinatorLayoutView(informationActivity);
            if (coordinatorLayout != null) {
                return coordinatorLayout;
            } else {
                return References.getView(informationActivity);
            }
        }
        return null;
    }

    /**
     * Utilize the shared compile + * mode, but enable the overlays selected afterwards
     */
    private void startCompileEnableMode() {
        selectStateMode(this, COMPILE_ENABLE);
    }

    /**
     * Utilize the shared compile + * mode and do not enable the overlays selected afterwards
     */
    private void startCompileUpdateMode() {
        selectStateMode(this, COMPILE_UPDATE);
    }

    /**
     * Disable the selected overlays
     */
    private void startDisable() {
        selectEnabledDisabled(this, DISABLE_MODE);
        if (!Systems.checkOMS(context)) {
            legacyDisable(this);
        }
    }

    /**
     * Enable the selected overlays
     */
    private void startEnable() {
        selectEnabledDisabled(this, ENABLE_MODE);
    }

    /**
     * Swap state mode
     */
    private void startEnableDisable() {
        selectEnabledDisabled(this, ENABLE_DISABLE);
    }

    /**
     * Allow InformationActivity to easily set the mix and match mode through the fab menu
     *
     * @param newValue Sets whether to enable mix and match mode (the toggle)
     */
    private void setMixAndMatchMode(boolean newValue) {
        mixAndMatchMode = newValue;
        prefs.edit().putBoolean("enable_swapping_overlays", mixAndMatchMode).apply();
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        TabOverlaysBinding viewBinding =
                DataBindingUtil.inflate(inflater, R.layout.tab_overlays, container, false);
        progressBar = viewBinding.headerLoadingBar;
        toggleAll = viewBinding.toggleAllOverlays;
        baseSpinner = viewBinding.type3Spinner;
        recyclerView = viewBinding.overlayRecyclerView;
        toggleAllOverlaysText = viewBinding.toggleAllOverlaysText;
        toggleZone = viewBinding.toggleZone;
        swipeRefreshLayout = viewBinding.swipeRefreshLayout;

        context = getContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        notifyManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        compileDialog = new SheetDialog(context);

        // Register the theme install receiver to auto refresh the fragment
        refreshReceiver = new RefreshReceiver();
        localBroadcastManager = LocalBroadcastManager.getInstance(context);
        try {
            localBroadcastManager.unregisterReceiver(refreshReceiver);
        } catch (IllegalArgumentException ignored) {
            // Unregistered already
        }
        localBroadcastManager.registerReceiver(refreshReceiver, new IntentFilter(OVERLAY_REFRESH));

        if (getArguments() != null) {
            themeName = getArguments().getString(THEME_NAME);
            themePid = getArguments().getString(THEME_PID);
            String encryptCheck =
                    Packages.getOverlayMetadata(context, themePid, metadataEncryption);

            boolean decryptedAssetsExceptionReached = false;
            if ((encryptCheck != null) && encryptCheck.equals(metadataEncryptionValue)) {
                byte[] encryptionKey = getArguments().getByteArray(ENCRYPTION_KEY_EXTRA);
                byte[] ivEncryptKey = getArguments().getByteArray(IV_ENCRYPTION_KEY_EXTRA);
                try {
                    themeCipher = Cipher.getInstance(CIPHER_ALGORITHM);
                    themeCipher.init(
                            Cipher.DECRYPT_MODE,
                            new SecretKeySpec(encryptionKey, SECRET_KEY_SPEC),
                            new IvParameterSpec(ivEncryptKey)
                    );
                    Log.d(SUBSTRATUM_BUILDER, "Loading substratum theme in encrypted assets mode.");
                    encrypted = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(SUBSTRATUM_BUILDER,
                            "Loading substratum theme in decrypted assets mode due to an exception.");
                    decryptedAssetsExceptionReached = true;
                }
            } else {
                Log.d(SUBSTRATUM_BUILDER, "Loading substratum theme in decrypted assets mode.");
            }

            if (decryptedAssetsExceptionReached) {
                currentShownLunchBar = Lunchbar.make(
                        getActivityView(),
                        context.getString(R.string.error_loading_theme_close_text),
                        Snackbar.LENGTH_INDEFINITE);
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
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        ArrayList<OverlaysItem> emptyArray = new ArrayList<>();
        RecyclerView.Adapter emptyAdapter = new OverlaysAdapter(emptyArray, context);
        recyclerView.setAdapter(emptyAdapter);
        toggleAllOverlaysText.setVisibility(View.VISIBLE);

        File workArea = new File(EXTERNAL_STORAGE_CACHE);
        if (!workArea.exists() && workArea.mkdir()) {
            Log.d(SUBSTRATUM_BUILDER,
                    "Updating the internal storage with proper file directories...");
        }

        // Adjust the behaviour of the mix and match toggle in the sheet
        toggleAll.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    try {
                        overlayItemList = overlaysAdapter.getOverlayList();
                        for (int i = 0; i < overlayItemList.size(); i++) {
                            OverlaysItem currentOverlay = overlayItemList.get(i);
                            currentOverlay.setSelected(isChecked);
                            overlaysAdapter.notifyDataSetChanged();
                        }
                    } catch (Exception ignored) {
                        Log.e(SUBSTRATUM_BUILDER, "Window has lost connection with the host.");
                    }
                });

        // Allow the user to toggle the select all switch by clicking on the bar above
        toggleZone.setOnClickListener(v -> {
            try {
                toggleAll.setChecked(!toggleAll.isChecked());
                overlayItemList = overlaysAdapter.getOverlayList();
                for (int i = 0; i < overlayItemList.size(); i++) {
                    OverlaysItem currentOverlay = overlayItemList.get(i);
                    currentOverlay.setSelected(toggleAll.isChecked());
                    overlaysAdapter.notifyDataSetChanged();
                }
            } catch (Exception ignored) {
                Log.e(SUBSTRATUM_BUILDER, "Window has lost connection with the host.");
            }
        });

        // Allow the user to swipe down to refresh the overlay list
        swipeRefreshLayout.setOnRefreshListener(this::refreshList);
        swipeRefreshLayout.setEnabled(false);

        /*
          PLUGIN TYPE 3: Parse each overlay folder to see if they have folder options
         */
        SharedPreferences prefs2 =
                context.getSharedPreferences("base_variant", Context.MODE_PRIVATE);
        baseSpinner.post(() -> baseSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> arg0,
                                               View arg1,
                                               int pos,
                                               long id) {
                        if (!firstStart) {
                            prefs2.edit().putInt(themePid, pos).apply();
                            refreshList();
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                    }
                }));
        baseSpinner.setEnabled(false);

        try {
            Resources themeResources = context.getPackageManager()
                    .getResourcesForApplication(themePid);
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
                            themeCipher);
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
                    Log.e(SUBSTRATUM_BUILDER, "There was an error parsing asset file!");
                    type3.add(new VariantItem(getString(R.string
                            .overlays_variant_default_3), null));
                }
                inputStream.close();
            } else {
                type3.add(new VariantItem(getString(R.string.overlays_variant_default_3), null));
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
                    toggleAllOverlaysText.setVisibility(View.GONE);
                    baseSpinner.setVisibility(View.VISIBLE);
                    baseSpinner.setAdapter(adapter1);
                    try {
                        Log.d(SUBSTRATUM_BUILDER,
                                "Assigning the spinner position: " +
                                        prefs2.getInt(themePid, 0));
                        if (prefs2.getInt(themePid, 0) <= type3.size() - 1) {
                            baseSpinner.setSelection(prefs2.getInt(themePid, 0));
                        } else {
                            throw new Exception();
                        }
                    } catch (Exception e) {
                        // Should be OutOfBounds, but let's catch everything
                        Log.d(SUBSTRATUM_BUILDER, "Falling back to default spinner position due to an error...");
                        prefs2.edit().putInt(themePid, 0).apply();
                        baseSpinner.setSelection(0);
                    }
                } else {
                    toggleAllOverlaysText.setVisibility(View.VISIBLE);
                    baseSpinner.setVisibility(View.INVISIBLE);
                }
            } else {
                toggleAllOverlaysText.setVisibility(View.VISIBLE);
                baseSpinner.setVisibility(View.INVISIBLE);
            }
        } catch (Exception e) {
            if (baseSpinner.getVisibility() == View.VISIBLE) {
                toggleAllOverlaysText.setVisibility(View.VISIBLE);
                baseSpinner.setVisibility(View.INVISIBLE);
            }
            e.printStackTrace();
            Log.e(SUBSTRATUM_BUILDER, "Could not parse list of base options for this theme!");
        }

        // Enable job listener
        jobReceiver = new JobReceiver();
        IntentFilter intentFilter =
                new IntentFilter(getClass().getSimpleName() + START_JOB_ACTION);
        localBroadcastManager.registerReceiver(jobReceiver, intentFilter);

        // Enable the instance to be retained for LogChar invoke after configuration change
        setRetainInstance(true);
        if (currentInstance != null &&
                currentInstance.errorLogs != null &&
                currentInstance.errorLogs.length() > 0) {
            invokeLogCharLunchBar(context);
        }
        return viewBinding.getRoot();
    }

    /**
     * Updates the current instance's list of enabled overlays
     */
    void getCurrentOverlays() {
        currentInstanceOverlays =
                new ArrayList<>(ThemeManager.listOverlays(context, ThemeManager.STATE_ENABLED));
    }

    /**
     * We need to be able to check the active notifications before throwing a new one
     *
     * @return If true, then there's an existing Substratum notification needed to be cleared
     */
    boolean checkActiveNotifications() {
        StatusBarNotification[] activeNotifications = notifyManager.getActiveNotifications();
        for (StatusBarNotification statusBarNotification : activeNotifications) {
            if (statusBarNotification.getPackageName().equals(context.getPackageName())) {
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
        notificationIntent.putExtra(Internal.THEME_NAME, themeName);
        notificationIntent.putExtra(Internal.THEME_PID, themePid);
        notificationIntent.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent =
                PendingIntent.getActivity(context, 0, notificationIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT);

        // Closing off the persistent notification
        notifyManager.cancel(References.NOTIFICATION_ID_COMPILER);
        builder = new NotificationCompat.Builder(context, DEFAULT_NOTIFICATION_CHANNEL_ID);
        builder.setAutoCancel(true);
        builder.setProgress(0, 0, false);
        builder.setOngoing(false);
        builder.setContentIntent(intent);
        builder.setSmallIcon(R.drawable.notification_warning_icon);
        builder.setContentTitle(context.getString(R.string.notification_done_title));
        builder.setContentText(context.getString(R.string.notification_some_errors_found));
        if (prefs.getBoolean("vibrate_on_compiled", false)) {
            builder.setVibrate(new long[]{200L, 400L, 200L, 1000L});
        }
        notifyManager.notify(References.NOTIFICATION_ID_COMPILER, builder.build());

        Toast.makeText(
                context,
                context.getString(R.string.toast_compiled_updated_with_errors),
                Toast.LENGTH_LONG).show();

        if (prefs.getBoolean("autosave_logchar", true)) {
            new SendErrorReport(
                    context,
                    themePid,
                    currentInstance.errorLogs.toString(),
                    currentInstance.failedPackages.toString(),
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
        CharSequence errorLogCopy = new StringBuilder(currentInstance.errorLogs);
        currentInstance.errorLogs = new StringBuilder();
        currentShownLunchBar = Lunchbar.make(
                getActivityView(),
                context.getString(R.string.logcat_snackbar_text),
                Snackbar.LENGTH_INDEFINITE);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
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
                                    context.getString(R.string.logcat_dialog_copy_success),
                                    Snackbar.LENGTH_LONG);
                            currentShownLunchBar.show();
                        });

        if (Packages.getOverlayMetadata(context, themePid, metadataEmail) != null) {
            builder.setPositiveButton(getString(R.string.logcat_send), (dialogInterface, i) ->
                    new SendErrorReport(
                            context,
                            themePid,
                            logs.toString(),
                            currentInstance.failedPackages.toString(),
                            false).execute());
        }
        builder.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        recyclerView.invalidate();
        recyclerView = null;

        try {
            localBroadcastManager.unregisterReceiver(refreshReceiver);
        } catch (IllegalArgumentException ignored) {
            // Unregistered already
        }
        try {
            localBroadcastManager.unregisterReceiver(jobReceiver);
        } catch (IllegalArgumentException ignored) {
            // Unregistered already
        }
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
                        themeCipher);
            } else {
                inputStream = themeAssetManager.open(
                        OVERLAYS_DIR + '/' + packageName + "/type1" + variantName);
            }
        } catch (IOException ignored) {
        }

        // Parse current default types on type3 base resource folders
        String parsedVariant = "";
        try {
            if (baseSpinner.getSelectedItemPosition() != 0) {
                parsedVariant = baseSpinner.getSelectedItem().toString().replaceAll("\\s+", "");
            }
        } catch (NullPointerException ignored) {
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
                        themeCipher)) {
                    hex = Packages.getOverlayResource(name);
                } catch (IOException ignored) {
                }
            } else {
                try (InputStream name = themeAssetManager.open(
                        OVERLAYS_DIR + '/' + packageName + suffix +
                                "/values/type1" + variantName + ".xml")) {
                    hex = Packages.getOverlayResource(name);
                } catch (IOException ignored) {
                }
            }
            return new VariantItem(formatter, hex);
        } catch (Exception e) {
            // When erroring out, put the default spinner text
            Log.d(SUBSTRATUM_BUILDER, "Falling back to default base variant text...");
            String hex = null;
            if (encrypted) {
                try (InputStream input = FileOperations.getInputStream(
                        themeAssetManager,
                        OVERLAYS_DIR + '/' + packageName +
                                suffix + "/values/type1" + variantName + ".xml" +
                                ENCRYPTED_FILE_EXTENSION,
                        themeCipher)) {
                    hex = Packages.getOverlayResource(input);
                } catch (IOException ignored) {
                }
            } else {
                try (InputStream input = themeAssetManager.open(OVERLAYS_DIR +
                        '/' + packageName + suffix + "/values/type1" + variantName + ".xml")) {
                    hex = Packages.getOverlayResource(input);
                } catch (IOException ignored) {
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
            Log.d(SUBSTRATUM_BUILDER, "Falling back to default base variant text...");
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
                    OVERLAYS_DIR + "/" + packageName + '/' + currentTypeOneObject, themeCipher)) {
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
                FileOperations.delete(context,
                        new File(currentInstance.lateInstall.get(0)).getAbsolutePath());
                if ((currentInstance.lateInstall != null) &&
                        !currentInstance.lateInstall.isEmpty())
                    currentInstance.lateInstall.remove(0);
                if (!currentInstance.lateInstall.isEmpty()) {
                    installMultipleAPKs();
                } else {
                    refreshList();
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
                context,
                context.getApplicationContext().getPackageName() + ".provider",
                new File(currentInstance.lateInstall.get(0)));
        intent.setDataAndType(
                uri,
                PACKAGE_INSTALL_URI);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, 2486);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Systems.checkOMS(context) && !toggleAll.isChecked()) {
            refreshList();
        } else {
            if (firstStart) refreshList();
        }
    }

    /**
     * Simple function to call when requesting to reload the list. This should contain method calls
     * to refresh the list gracefully including disabling certain views and re-enabling them in the
     * subsequent calls
     */
    private void refreshList() {
        if (!compileDialog.isShowing()) {
            recyclerViewPosition = ((LinearLayoutManager)
                    recyclerView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
            toggleAll.setChecked(false);
            if (baseSpinner.getSelectedItemPosition() > 0) {
                mainLoader =
                        new LoadOverlays(this).execute(baseSpinner.getSelectedItem().toString());
            } else {
                mainLoader = new LoadOverlays(this).execute("");
            }
        } else {
            Log.d(SUBSTRATUM_LOG,
                    "Overlay compilation in progress, will skip refreshing layout until the end.");
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
        private boolean autosaveInstance;
        private ProgressDialog progressDialog;

        SendErrorReport(Context context,
                        String themePid,
                        String errorLog,
                        String failedPackages,
                        boolean autosaveInstance) {
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
                String themeVersion = Packages.getAppVersion(context, themePid);
                String romVersion = Build.VERSION.RELEASE + " - " +
                        (!rom.isEmpty() ? rom : "Unknown");

                String device = Build.MODEL + " (" + Build.DEVICE + ") " +
                        '[' + Build.FINGERPRINT + ']';
                String xposed = References.checkXposedVersion();
                if (!xposed.isEmpty()) device += " {" + xposed + '}';

                String attachment = String.format(
                        context.getString(R.string.logcat_attachment_body),
                        themeName,
                        device,
                        romVersion,
                        String.valueOf(BuildConfig.VERSION_CODE),
                        themeVersion,
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
        private String parsedThemeName;
        private ArrayList<OverlaysItem> adapterList;
        private List<String> currentOverlays;

        LoadOverlays(Overlays fragment) {
            super();
            ref = new WeakReference<>(fragment);
        }

        static void setViews(Overlays fragment, boolean state) {
            fragment.swipeRefreshLayout.setRefreshing(!state);
            fragment.recyclerView.setEnabled(state);
            fragment.toggleAll.setEnabled(state);
            if (!state) fragment.toggleAll.setChecked(false);
            fragment.baseSpinner.setEnabled(state);
        }

        /**
         * Step 1
         * Assign global variables and do all the prenotions
         *
         * @param overlays Overlays
         * @return True, if successful
         */
        private static boolean assignVariables(Overlays overlays, LoadOverlays loadOverlays) {
            try {
                loadOverlays.adapterList = new ArrayList<>();
                Resources themeResources = overlays.context.getPackageManager()
                        .getResourcesForApplication(overlays.themePid);
                overlays.themeAssetManager = themeResources.getAssets();
                overlays.themeVersion = Packages.getAppVersion(
                        overlays.context, overlays.themePid);
                String initialParse = overlays.themeName.replaceAll("\\s+", "");
                loadOverlays.parsedThemeName = initialParse.replaceAll("[^a-zA-Z0-9]+", "");
                overlays.getCurrentOverlays();
                loadOverlays.currentOverlays = overlays.currentInstanceOverlays;
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        /**
         * Step 2
         * Create a buffered list of strings that provide the function with overlays that are
         * parsable by {@link #obtainUnsortedMap(Overlays, List)}
         *
         * @param overlays Overlays
         * @return Returns a list of strings to be mapped
         */
        private static List<String> buffer(Overlays overlays) {
            Context context = overlays.context;
            Collection<String> overlaysFolder = new ArrayList<>();

            try {
                // First, we list the overlays available in the theme, then add them all to workList
                String[] overlayList = overlays.themeAssetManager.list(OVERLAYS_DIR);
                Collections.addAll(
                        overlaysFolder,
                        overlayList
                );
            } catch (IOException ioe) {
                ioe.printStackTrace();
                return null;
            }

            // This is for Samsung internal use only!
            boolean showDangerous =
                    !overlays.prefs.getBoolean("show_dangerous_samsung_overlays", false);

            // Now, we have to sort what packages are going to be seen by the user
            List<String> list = new ArrayList<>();
            for (String packageName : overlaysFolder) {
                if ((isPackageInstalled(context, packageName) ||
                        allowedSystemUIOverlay(packageName) ||
                        allowedSettingsOverlay(packageName) ||
                        allowedFrameworkOverlay(packageName)
                ) && (!showDangerous ||
                        !ThemeManager.blacklisted(
                                packageName,
                                Systems.isSamsungDevice(context) &&
                                        Packages.isSamsungTheme(
                                                context,
                                                overlays.themePid
                                        )
                        )
                )) {
                    list.add(packageName);
                }
            }
            if (list.size() > 0) {
                return new ArrayList<>(list);
            } else {
                return null;
            }
        }

        /**
         * Step 3
         * Obtain a map in the format of {package_name,package_identifier} for the asset overlays
         *
         * @param overlays Overlays
         * @param values   {@link #buffer(Overlays)}
         * @return Returns a Map, for sorting
         */
        private static Map<String, String> obtainUnsortedMap(Overlays overlays,
                                                             List<String> values) {
            // Create the map for {package name: package identifier}
            Map<String, String> unsortedMap = new HashMap<>();

            // Then let's convert all the package names to their app names
            for (int i = 0; i < values.size(); i++) {
                try {
                    String target = values.get(i);
                    String packageName = "";
                    boolean succeeded = false;

                    if (allowedSystemUIOverlay(target)) {
                        // Check if the overlay matches one of the custom packages from SystemUI
                        switch (target) {
                            case SYSTEMUI_HEADERS:
                                packageName = overlays.getString(R.string.systemui_headers);
                                succeeded = true;
                                break;
                            case SYSTEMUI_NAVBARS:
                                packageName = overlays.getString(R.string.systemui_navigation);
                                succeeded = true;
                                break;
                            case SYSTEMUI_STATUSBARS:
                                packageName = overlays.getString(R.string.systemui_statusbar);
                                succeeded = true;
                                break;
                            case SYSTEMUI_QSTILES:
                                packageName = overlays.getString(R.string.systemui_qs_tiles);
                                succeeded = true;
                                break;
                        }
                    } else if (allowedSettingsOverlay(target)) {
                        // If not SystemUI, check if it is part of the Settings
                        switch (target) {
                            case SETTINGS_ICONS:
                                packageName = overlays.getString(R.string.settings_icons);
                                succeeded = true;
                                break;
                        }
                    } else if (allowedFrameworkOverlay(target)) {
                        // Finally, if not Settings, check if it is part of the Android Framework
                        switch (target) {
                            case SAMSUNG_FRAMEWORK:
                                packageName = overlays.getString(R.string.samsung_framework);
                                succeeded = true;
                                break;
                            case LG_FRAMEWORK:
                                packageName = overlays.getString(R.string.lg_framework);
                                succeeded = true;
                                break;
                        }
                    } else if (allowedAppOverlay(target)) {
                        // The filter passes, just toss in the app into the list
                        packageName = Packages.getPackageName(overlays.context, target);
                        succeeded = true;
                    }
                    if (succeeded) unsortedMap.put(target, packageName);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
            return unsortedMap;
        }

        /**
         * Step 4
         * Completely iron out everything as there is no way we need to fail at this point
         *
         * @param overlays     Overlays
         * @param loadOverlays LoadOverlays
         * @param sortedMap    Sorted map
         * @param argument     Argument fed into the AsynchronousTask
         */
        private static void parseDirectoryStructure(Overlays overlays,
                                                    LoadOverlays loadOverlays,
                                                    List<Pair<String, String>> sortedMap,
                                                    String argument) {
            for (Pair<String, String> entry : sortedMap) {
                String packageName = entry.second;
                String packageIdentifier = entry.first;

                try {
                    List<String> typeArray = new ArrayList<>();
                    Object typeArrayRaw = overlays.themeAssetManager.list(
                            OVERLAYS_DIR + '/' + packageIdentifier);

                    // Sort the typeArray so that the types are asciibetical
                    Collections.addAll(typeArray, (String[]) typeArrayRaw);
                    Collections.sort(typeArray);

                    // Let's start adding the type xmls to be parsed into the spinners
                    ArrayList<VariantItem> type1a = new ArrayList<>();
                    ArrayList<VariantItem> type1b = new ArrayList<>();
                    ArrayList<VariantItem> type1c = new ArrayList<>();
                    ArrayList<VariantItem> type2 = new ArrayList<>();
                    ArrayList<VariantItem> type4 = new ArrayList<>();

                    // Load up type1a's
                    if (typeArray.contains("type1a") ||
                            typeArray.contains("type1a" + ENCRYPTED_FILE_EXTENSION)) {
                        type1a.add(overlays.setTypeOneSpinners(packageIdentifier, "a"));
                    }

                    // Load up type1b's
                    if (typeArray.contains("type1b") ||
                            typeArray.contains("type1b" + ENCRYPTED_FILE_EXTENSION)) {
                        type1b.add(overlays.setTypeOneSpinners(packageIdentifier, "b"));
                    }

                    // Load up type1c's
                    if (typeArray.contains("type1c") ||
                            typeArray.contains("type1c" + ENCRYPTED_FILE_EXTENSION)) {
                        type1c.add(overlays.setTypeOneSpinners(packageIdentifier, "c"));
                    }

                    // Are there any type2's in the overlay folder?
                    boolean type2checker = false;
                    for (int i = 0; i < typeArray.size(); i++) {
                        String type = typeArray.get(i);
                        if (type != null && type.startsWith("type2_")) {
                            type2checker = true;
                            break;
                        }
                    }

                    // Type2's are present, let's parse the name of the type2 spinner if present
                    if (type2checker) {
                        InputStreamReader inputStreamReader = null;
                        try {
                            inputStreamReader = new InputStreamReader(
                                    FileOperations.getInputStream(
                                            overlays.themeAssetManager,
                                            OVERLAYS_DIR + '/' + packageIdentifier +
                                                    (overlays.encrypted ?
                                                            "/type2" + ENCRYPTED_FILE_EXTENSION :
                                                            "/type2"
                                                    ),
                                            (overlays.encrypted ? overlays.themeCipher : null)));
                        } catch (Exception ignored) {
                        }
                        type2.add(overlays.setTypeTwoFourSpinners(inputStreamReader, 2));
                    }

                    // Are there any type4's in the overlay folder?
                    boolean type4checker = false;
                    for (int i = 0; i < typeArray.size(); i++) {
                        String type = typeArray.get(i);
                        if (type != null && type.startsWith("type4_")) {
                            type4checker = true;
                            break;
                        }
                    }

                    // Type4's are present, let's parse the name of the type2 spinner if present
                    if (type4checker) {
                        InputStreamReader inputStreamReader = null;
                        try {
                            inputStreamReader = new InputStreamReader(
                                    FileOperations.getInputStream(
                                            overlays.themeAssetManager,
                                            OVERLAYS_DIR + '/' + packageIdentifier +
                                                    (overlays.encrypted ?
                                                            "/type4" + ENCRYPTED_FILE_EXTENSION :
                                                            "/type4"
                                                    ),
                                            (overlays.encrypted ? overlays.themeCipher : null)));
                        } catch (Exception ignored) {
                        }
                        type4.add(overlays.setTypeTwoFourSpinners(inputStreamReader, 4));
                    }

                    // Are there any attention files in the overlay folder?
                    boolean attentionPresent = false;
                    for (int i = 0; i < typeArray.size(); i++) {
                        String type = typeArray.get(i);
                        if (type != null && (type.equals("attention") ||
                                type.equals("attention" + ENCRYPTED_FILE_EXTENSION))) {
                            attentionPresent = true;
                            break;
                        }
                    }
                    // attention file present, let's parse the name of the type2 spinner if present
                    StringBuilder attentionFile = new StringBuilder();
                    if (attentionPresent) {
                        InputStreamReader inputStreamReader;
                        try {
                            inputStreamReader = new InputStreamReader(
                                    FileOperations.getInputStream(
                                            overlays.themeAssetManager,
                                            OVERLAYS_DIR + '/' + packageIdentifier +
                                                    (overlays.encrypted ?
                                                            "/attention" +
                                                                    ENCRYPTED_FILE_EXTENSION :
                                                            "/attention"
                                                    ),
                                            (overlays.encrypted ? overlays.themeCipher : null)));
                            BufferedReader reader = new BufferedReader(inputStreamReader);
                            attentionFile.append(reader.readLine());
                        } catch (Exception ignored) {
                        }
                    }

                    // Finally, check if the assets/overlays folder actually has anything inside
                    if (typeArray.size() > 1) {
                        for (int i = 0; i < typeArray.size(); i++) {
                            String current = typeArray.get(i);

                            // Filter out the assets/overlays/overlay_name/res
                            if (current.contains(XML_EXTENSION)) {
                                // We need to find out whether the themer decided to add
                                // injection-based type1 variants, and if so, add to the
                                // variants
                                switch (current.substring(0, 7)) {
                                    case TYPE1A_PREFIX:
                                        type1a.add(overlays.setTypeOneHexAndSpinner(
                                                current, packageIdentifier));
                                        break;
                                    case TYPE1B_PREFIX:
                                        type1b.add(overlays.setTypeOneHexAndSpinner(
                                                current, packageIdentifier));
                                        break;
                                    case TYPE1C_PREFIX:
                                        type1c.add(overlays.setTypeOneHexAndSpinner(
                                                current, packageIdentifier));
                                        break;
                                }
                            } else if (current.length() >= 6) {
                                // Now we filter out directories that we specifically whitelisted
                                String starting = current.substring(0, 6);
                                switch (starting) {
                                    case TYPE2_PREFIX:
                                        type2.add(new VariantItem(current.substring(6), null));
                                        break;
                                    case TYPE4_PREFIX:
                                        type4.add(new VariantItem(current.substring(6), null));
                                        break;
                                }
                            }
                        }
                        VariantAdapter[] adapters = new VariantAdapter[]{
                                new VariantAdapter(overlays.context, type1a),
                                new VariantAdapter(overlays.context, type1b),
                                new VariantAdapter(overlays.context, type1c),
                                new VariantAdapter(overlays.context, type2),
                                new VariantAdapter(overlays.context, type4)
                        };
                        Boolean[] checker = new Boolean[]{
                                !type1a.isEmpty(),
                                !type1b.isEmpty(),
                                !type1c.isEmpty(),
                                !type2.isEmpty(),
                                !type4.isEmpty()
                        };
                        createOverlaysItem(
                                overlays,
                                loadOverlays,
                                packageName,
                                packageIdentifier,
                                checker,
                                adapters,
                                attentionFile.toString(),
                                argument);
                    } else {
                        // At this point, there is no spinner adapter, so it should be null
                        createOverlaysItem(
                                overlays,
                                loadOverlays,
                                packageName,
                                packageIdentifier,
                                null,
                                null,
                                attentionFile.toString(),
                                argument);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Step 4.5
         * Create each individual overlay item
         *
         * @param overlays          Overlays
         * @param loadOverlays      LoadOverlays
         * @param packageName       Name of of overlay
         * @param packageIdentifier Package name
         * @param checker           Array of booleans denoting whether each type is available
         * @param adapters          Array of VariantAdapters denoting the data
         * @param argument          Argument of the AsynchronousTask
         */
        private static void createOverlaysItem(Overlays overlays,
                                               LoadOverlays loadOverlays,
                                               String packageName,
                                               String packageIdentifier,
                                               Boolean[] checker,
                                               VariantAdapter[] adapters,
                                               String attention,
                                               String argument) {
            try {
                OverlaysItem overlaysItem =
                        new OverlaysItem(
                                loadOverlays.parsedThemeName,
                                packageName,
                                packageIdentifier,
                                false,
                                (checker != null ? (checker[0] ? adapters[0] : null) : null),
                                (checker != null ? (checker[1] ? adapters[1] : null) : null),
                                (checker != null ? (checker[2] ? adapters[2] : null) : null),
                                (checker != null ? (checker[3] ? adapters[3] : null) : null),
                                (checker != null ? (checker[4] ? adapters[4] : null) : null),
                                overlays.context,
                                overlays.themeVersion,
                                argument,
                                loadOverlays.currentOverlays,
                                attention);
                loadOverlays.adapterList.add(overlaysItem);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onPreExecute() {
            Overlays fragment = ref.get();
            if (fragment != null) setViews(fragment, false);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Overlays fragment = ref.get();
            if (fragment != null) {
                setViews(fragment, true);
                fragment.overlaysAdapter = new OverlaysAdapter(adapterList, fragment.context);
                fragment.recyclerView.setAdapter(fragment.overlaysAdapter);
                fragment.recyclerView.getLayoutManager().scrollToPosition(
                        fragment.recyclerViewPosition);
                fragment.overlaysAdapter.notifyDataSetChanged();
                if (!fragment.recyclerView.isShown())
                    fragment.recyclerView.setVisibility(View.VISIBLE);
                if (fragment.firstStart) fragment.firstStart = false;
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            Overlays fragment = ref.get();
            if (fragment != null) {
                // Modularizing the compile process to make it easier to track errors
                boolean assigned = assignVariables(fragment, this);
                if (assigned) {
                    List<String> values = buffer(fragment);
                    if (values != null) {
                        Map<String, String> unsortedMap = obtainUnsortedMap(fragment, values);
                        if (unsortedMap != null) {
                            List<Pair<String, String>> sortedMap = sortMapByValues(unsortedMap);
                            if (sortedMap != null) {
                                parseDirectoryStructure(fragment, this, sortedMap, sUrl[0]);
                            }
                        }
                    }
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
                    if (overlaysAdapter != null && !compileDialog.isShowing())
                        startCompileEnableMode();
                    break;
                case COMPILE_UPDATE:
                    if (overlaysAdapter != null && !compileDialog.isShowing())
                        startCompileUpdateMode();
                    break;
                case DISABLE:
                    if (overlaysAdapter != null && !compileDialog.isShowing())
                        startDisable();
                    break;
                case ENABLE:
                    if (overlaysAdapter != null && !compileDialog.isShowing())
                        startEnable();
                    break;
                case ENABLE_DISABLE:
                    if (overlaysAdapter != null && !compileDialog.isShowing())
                        startEnableDisable();
                    break;
                case MIX_AND_MATCH:
                    if (overlaysAdapter != null) {
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
            if (Systems.isSamsungDevice(context) && Root.checkRootAccess()) {
                if (currentInstance.overlaysWaiting > 0) {
                    --currentInstance.overlaysWaiting;
                } else {
                    progressBar.setVisibility(View.GONE);
                    refreshList();
                }
            } else if (!compileDialog.isShowing()) {
                refreshList();
            } else {
                Log.d(SUBSTRATUM_BUILDER,
                        "Refresh overlays has been cancelled during compilation phase...");
            }
        }
    }
}