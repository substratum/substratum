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

package projekt.substratum.services.floatui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import jp.co.recruit_lifestyle.android.floatingview.FloatingViewListener;
import jp.co.recruit_lifestyle.android.floatingview.FloatingViewManager;
import projekt.substratum.R;
import projekt.substratum.adapters.fragments.manager.ManagerAdapter;
import projekt.substratum.adapters.fragments.manager.ManagerItem;
import projekt.substratum.common.References;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.services.notification.FloatUiButtonReceiver;

import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;

public class SubstratumFloatInterface extends Service implements FloatingViewListener {

    private static final int NOTIFICATION_ID = 92781162;
    private FloatingViewManager mFloatingViewManager;
    private List<ManagerItem> final_check;
    private SharedPreferences prefs;
    private boolean trigger_service_restart, trigger_systemui_restart;
    private ManagerAdapter mAdapter;

    @SuppressWarnings("WrongConstant")
    public String foregroundedApp() {
        UsageStatsManager mUsageStatsManager = (UsageStatsManager) getSystemService("usagestats");
        long time = System.currentTimeMillis();
        List<UsageStats> stats = null;
        if (mUsageStatsManager != null) {
            stats = mUsageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY, time - 1000 * 1000, time);
        }
        String foregroundApp = "";
        if (stats != null && stats.size() > 0) {
            SortedMap<Long, UsageStats> mySortedMap = new TreeMap<>();
            for (UsageStats usageStats : stats) {
                mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
            }
            if (!mySortedMap.isEmpty()) {
                foregroundApp = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
            }
        }
        UsageEvents usageEvents = null;
        if (mUsageStatsManager != null) {
            usageEvents = mUsageStatsManager.queryEvents(time - 1000 * 1000, time);
        }
        UsageEvents.Event event = new UsageEvents.Event();
        // Get the last event in the doubly linked list
        if (usageEvents != null) {
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event);
            }
        }
        if (foregroundApp.equals(event.getPackageName()) &&
                event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
            return foregroundApp;
        }
        return foregroundApp;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mFloatingViewManager != null) {
            return START_STICKY;
        }
        final DisplayMetrics metrics = new DisplayMetrics();
        final WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (windowManager != null) {
            windowManager.getDefaultDisplay().getMetrics(metrics);
        }
        final LayoutInflater inflater = LayoutInflater.from(this);
        @SuppressLint("InflateParams") final ImageView iconView = (ImageView)
                inflater.inflate(R.layout.floatui_head, null, false);
        iconView.setOnClickListener(v -> {
            String packageName =
                    References.grabPackageName(getApplicationContext(), foregroundedApp());
            String dialogTitle = String.format(getString(R.string.per_app_dialog_title),
                    packageName);

            ArrayList<String> all_overlays = new ArrayList<>(
                    ThemeManager.listAllOverlays(getApplicationContext()));
            ArrayList<String> enabledOverlaysForForegroundPackage = new ArrayList<>(
                    ThemeManager.listEnabledOverlaysForTarget(getApplicationContext(),
                            foregroundedApp()));
            ArrayList<String> disabledOverlaysForForegroundPackage = new ArrayList<>(
                    ThemeManager.listDisabledOverlaysForTarget(getApplicationContext(),
                            foregroundedApp()));
            boolean show_android_overlays =
                    prefs.getBoolean("floatui_show_android_system_overlays", true);
            ArrayList<String> to_be_shown = new ArrayList<>();
            to_be_shown.addAll(enabledOverlaysForForegroundPackage);
            to_be_shown.addAll(disabledOverlaysForForegroundPackage);
            if (show_android_overlays) {
                for (String overlay : all_overlays) {
                    if (overlay.startsWith("android.")) to_be_shown.add(overlay);
                }
            }
            Collections.sort(to_be_shown);

            if (to_be_shown.size() == 0) {
                String format = String.format(
                        getString(R.string.per_app_toast_no_overlays), packageName);
                Toast.makeText(getApplicationContext(), format, Toast.LENGTH_SHORT).show();
            } else {
                final_check = new ArrayList<>();
                for (int j = 0; j < to_be_shown.size(); j++) {
                    Boolean is_enabled = enabledOverlaysForForegroundPackage.contains(to_be_shown.get(j));
                    ManagerItem managerItem = new ManagerItem(
                            getApplicationContext(), to_be_shown.get(j), is_enabled);
                    if (is_enabled) {
                        managerItem.setSelected(true);
                    }
                    final_check.add(managerItem);
                }

                RecyclerView mRecyclerView;
                mAdapter = new ManagerAdapter(final_check, true);

                // Set a custom title
                TextView title = new TextView(this);
                title.setText(dialogTitle);
                title.setBackgroundColor(getColor(R.color.floatui_dialog_header_background));
                title.setPadding(20, 40, 20, 40);
                title.setGravity(Gravity.CENTER);
                title.setTextColor(getColor(R.color.floatui_dialog_title_color));
                title.setTextSize(20);
                title.setAllCaps(true);

                // Initialize the AlertDialog Builder
                AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.FloatUiDialog);
                builder.setCustomTitle(title);
                builder.setPositiveButton(R.string.per_app_apply, (dialog, which) -> {
                    trigger_service_restart = false;
                    trigger_systemui_restart = false;
                    ArrayList<String> to_enable = new ArrayList<>();
                    ArrayList<String> to_disable = new ArrayList<>();

                    for (int i = 0; i < final_check.size(); i++) {
                        if (mAdapter.getOverlayManagerList().get(i).isSelected()) {
                            // Check if enabled
                            if (!enabledOverlaysForForegroundPackage
                                    .contains(final_check.get(i).getName())) {
                                // It is not enabled, append it to the list
                                String package_name = final_check.get(i).getName();
                                to_enable.add(package_name);
                                if (package_name.startsWith("android.") ||
                                        package_name.startsWith(getPackageName() + ".") ||
                                        package_name.startsWith("com.android.systemui"))
                                    trigger_service_restart = true;
                            }
                        } else if (!disabledOverlaysForForegroundPackage
                                .contains(final_check.get(i).getName())) {
                            // It is disabled, append it to the list
                            String package_name = final_check.get(i).getName();
                            to_disable.add(package_name);
                            if (package_name.startsWith("android.") ||
                                    package_name.startsWith(getPackageName() + ".") ||
                                    package_name.startsWith("com.android.systemui"))
                                trigger_service_restart = true;
                        }
                    }
                    // Dismiss the dialog so that we don't have issues with configuration changes
                    // when a dialog is open.
                    dialog.dismiss();

                    // Run the overlay management after a 0.1 second delay
                    final Handler handler = new Handler();
                    handler.postDelayed(() -> {
                        if (to_enable.size() > 0)
                            ThemeManager.enableOverlay(getApplicationContext(), to_enable);
                        if (to_disable.size() > 0)
                            ThemeManager.disableOverlay(getApplicationContext(), to_disable);

                        if (trigger_systemui_restart) {
                            ThemeManager.restartSystemUI(getApplicationContext());
                        }
                        if (trigger_service_restart) {
                            final Handler handler2 = new Handler();
                            handler2.postDelayed(() -> {
                                getApplicationContext().stopService(
                                        new Intent(
                                                getApplicationContext(),
                                                SubstratumFloatInterface.class));
                                getApplicationContext().startService(
                                        new Intent(
                                                getApplicationContext(),
                                                SubstratumFloatInterface.class));
                            }, 300);
                        }
                    }, 100);
                });
                builder.setNegativeButton(android.R.string.cancel,
                        (dialog, which) -> dialog.cancel());

                LayoutInflater inflate = LayoutInflater.from(getApplicationContext());
                @SuppressLint("InflateParams")
                View content = inflate.inflate(R.layout.floatui_dialog, null);
                builder.setView(content);

                mRecyclerView = (RecyclerView) content.findViewById(R.id.recycler_view);
                mRecyclerView.setAdapter(mAdapter);

                mRecyclerView.setHasFixedSize(true);
                mRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

                AlertDialog alertDialog = builder.create();
                //noinspection ConstantConditions
                alertDialog.getWindow().setBackgroundDrawable(
                        getDrawable(R.drawable.dialog_background));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    alertDialog.getWindow().setType(TYPE_APPLICATION_OVERLAY);
                } else {
                    alertDialog.getWindow().setType(TYPE_SYSTEM_ALERT);
                }
                alertDialog.show();
            }
        });
        mFloatingViewManager = new FloatingViewManager(this, this);
        mFloatingViewManager.setFixedTrashIconImage(R.drawable.floating_trash_cross);
        mFloatingViewManager.setActionTrashIconImage(R.drawable.floating_trash_base);
        final FloatingViewManager.Options options = new FloatingViewManager.Options();
        options.overMargin = (int) (16 * metrics.density);
        mFloatingViewManager.addViewToWindow(iconView, options);

        startForeground(NOTIFICATION_ID, createNotification());

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        destroy();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onFinishFloatingView() {
        stopSelf();
    }

    @Override
    public void onTouchFinished(boolean isFinishing, int x, int y) {
    }

    private void destroy() {
        if (mFloatingViewManager != null) {
            mFloatingViewManager.removeAllViewToWindow();
            mFloatingViewManager = null;
        }
    }

    private Notification createNotification() {
        // Create an Intent for the BroadcastReceiver
        Intent buttonIntent = new Intent(getApplicationContext(), FloatUiButtonReceiver.class);

        // Create the PendingIntent
        PendingIntent btPendingIntent = PendingIntent.getBroadcast(
                getApplicationContext(), 0, buttonIntent, 0);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this,
                References.ONGOING_NOTIFICATION_CHANNEL_ID);
        builder.setWhen(System.currentTimeMillis());
        prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());
        if (prefs.getBoolean("floatui_show_android_system_overlays", true)) {
            builder.addAction(android.R.color.transparent, getString(R.string
                    .per_app_notification_framework_hide), btPendingIntent);
        } else {
            builder.addAction(android.R.color.transparent, getString(R.string
                    .per_app_notification_framework_show), btPendingIntent);
        }
        builder.setSmallIcon(R.drawable.notification_floatui);
        builder.setContentTitle(getString(R.string.app_name));
        builder.setContentText(getString(R.string.per_app_notification_summary));
        builder.setOngoing(true);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);
        builder.setCategory(NotificationCompat.CATEGORY_SERVICE);

        return builder.build();
    }
}