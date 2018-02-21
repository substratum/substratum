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
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import jp.co.recruit_lifestyle.android.floatingview.FloatingViewListener;
import jp.co.recruit_lifestyle.android.floatingview.FloatingViewManager;
import projekt.substratum.R;
import projekt.substratum.adapters.fragments.manager.ManagerAdapter;
import projekt.substratum.adapters.fragments.manager.ManagerItem;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.services.notification.FloatUiButtonReceiver;

public class SubstratumFloatInterface extends Service implements FloatingViewListener {

    private static final int NOTIFICATION_ID = 92781162;
    private FloatingViewManager floatingViewManager;
    private List<ManagerItem> finalCheck;
    private SharedPreferences prefs;
    private boolean triggerServiceRestart, triggerSystemuiRestart;
    private ManagerAdapter adapter;

    private String foregroundedApp() {
        @SuppressLint("WrongConstant") UsageStatsManager usageStatsManager =
                (UsageStatsManager) getSystemService("usagestats");
        long time = System.currentTimeMillis();
        List<UsageStats> stats = null;
        if (usageStatsManager != null) {
            stats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY, time - (long) (1000 * 1000), time);
        }
        String foregroundApp = "";
        if ((stats != null) && !stats.isEmpty()) {
            SortedMap<Long, UsageStats> mySortedMap = new TreeMap<>();
            for (UsageStats usageStats : stats) {
                mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
            }
            if (!mySortedMap.isEmpty()) {
                foregroundApp = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
            }
        }
        UsageEvents usageEvents = null;
        if (usageStatsManager != null) {
            usageEvents = usageStatsManager.queryEvents(time - (long) (1000 * 1000), time);
        }
        UsageEvents.Event event = new UsageEvents.Event();
        // Get the last event in the doubly linked list
        if (usageEvents != null) {
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event);
            }
        }
        if (foregroundApp.equals(event.getPackageName()) &&
                (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND)) {
            return foregroundApp;
        }
        return foregroundApp;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (floatingViewManager != null) {
            return START_STICKY;
        }
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (windowManager != null) {
            windowManager.getDefaultDisplay().getMetrics(metrics);
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        @SuppressLint("InflateParams") ImageView iconView = (ImageView)
                inflater.inflate(R.layout.floatui_head, null, false);
        iconView.setOnClickListener(v -> {
            String packageName =
                    Packages.getPackageName(getApplicationContext(), foregroundedApp());
            ArrayList<String> enabledOverlaysForForegroundPackage = new ArrayList<>(
                    ThemeManager.listEnabledOverlaysForTarget(getApplicationContext(),
                            foregroundedApp()));
            Collection<String> disabledOverlaysForForegroundPackage = new ArrayList<>(
                    ThemeManager.listDisabledOverlaysForTarget(getApplicationContext(),
                            foregroundedApp()));

            if (prefs.getBoolean("floatui_show_android_system_overlays", true)) {
                enabledOverlaysForForegroundPackage.addAll(ThemeManager
                        .listEnabledOverlaysForTarget(getApplicationContext(), "android"));
                disabledOverlaysForForegroundPackage.addAll(ThemeManager
                        .listDisabledOverlaysForTarget(getApplicationContext(), "android"));
            }

            List<String> toBeShown = new ArrayList<>();
            toBeShown.addAll(enabledOverlaysForForegroundPackage);
            toBeShown.addAll(disabledOverlaysForForegroundPackage);
            Collections.sort(toBeShown);

            if (toBeShown.isEmpty()) {
                String format = String.format(
                        getString(R.string.per_app_toast_no_overlays), packageName);
                Toast.makeText(getApplicationContext(), format, Toast.LENGTH_SHORT).show();
            } else {
                finalCheck = new ArrayList<>();
                for (int j = 0; j < toBeShown.size(); j++) {
                    boolean isEnabled = enabledOverlaysForForegroundPackage
                            .contains(toBeShown.get(j));
                    ManagerItem managerItem = new ManagerItem(
                            getApplicationContext(), toBeShown.get(j), isEnabled);
                    if (isEnabled) {
                        managerItem.setSelected(true);
                    }
                    finalCheck.add(managerItem);
                }

                adapter = new ManagerAdapter(finalCheck);

                // Initialize the AlertDialog Builder
                AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.FloatUiDialog);
                builder.setPositiveButton(R.string.per_app_apply, (dialog, which) -> {
                    triggerServiceRestart = false;
                    triggerSystemuiRestart = false;
                    ArrayList<String> toEnable = new ArrayList<>();
                    ArrayList<String> toDisable = new ArrayList<>();

                    for (int i = 0; i < finalCheck.size(); i++) {
                        if (adapter.getOverlayManagerList().get(i).isSelected()) {
                            // Check if enabled
                            if (!enabledOverlaysForForegroundPackage
                                    .contains(finalCheck.get(i).getName())) {
                                // It is not enabled, append it to the list
                                String packageName1 = finalCheck.get(i).getName();
                                toEnable.add(packageName1);
                                if (packageName1.startsWith("android.") ||
                                        packageName1.startsWith(getPackageName() + '.') ||
                                        packageName1.startsWith("com.android.systemui"))
                                    triggerServiceRestart = true;
                            }
                        } else if (!disabledOverlaysForForegroundPackage
                                .contains(finalCheck.get(i).getName())) {
                            // It is disabled, append it to the list
                            String packageName2 = finalCheck.get(i).getName();
                            toDisable.add(packageName2);
                            if (packageName2.startsWith("android.") ||
                                    packageName2.startsWith(getPackageName() + '.') ||
                                    packageName2.startsWith("com.android.systemui"))
                                triggerServiceRestart = true;
                        }
                    }
                    // Dismiss the dialog so that we don't have issues with configuration changes
                    // when a dialog is open.
                    dialog.dismiss();

                    // Run the overlay management after a 0.1 second delay
                    new Handler().postDelayed(() -> {
                        if (!toEnable.isEmpty())
                            ThemeManager.enableOverlay(getApplicationContext(), toEnable);
                        if (!toDisable.isEmpty())
                            ThemeManager.disableOverlay(getApplicationContext(), toDisable);

                        if (triggerSystemuiRestart) {
                            ThemeManager.restartSystemUI(getApplicationContext());
                        }
                        if (triggerServiceRestart) {
                            Handler handler2 = new Handler();
                            handler2.postDelayed(() -> {
                                getApplicationContext().stopService(
                                        new Intent(
                                                getApplicationContext(),
                                                SubstratumFloatInterface.class));
                                getApplicationContext().startService(
                                        new Intent(
                                                getApplicationContext(),
                                                SubstratumFloatInterface.class));
                            }, 300L);
                        }
                    }, 100L);
                });
                builder.setNegativeButton(android.R.string.cancel,
                        (dialog, which) -> dialog.cancel());

                LayoutInflater inflate = LayoutInflater.from(getApplicationContext());
                @SuppressLint("InflateParams")
                View content = inflate.inflate(R.layout.floatui_dialog, null);
                builder.setView(content);

                RecyclerView recyclerView = content.findViewById(R.id.recycler_view);
                recyclerView.setAdapter(adapter);

                recyclerView.setHasFixedSize(true);
                recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

                AlertDialog alertDialog = builder.create();
                Window window = alertDialog.getWindow();
                if (window != null) {
                    window.setBackgroundDrawable(
                            getDrawable(R.drawable.dialog_background));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        window.setType(LayoutParams.TYPE_APPLICATION_OVERLAY);
                    } else {
                        window.setType(LayoutParams.TYPE_SYSTEM_ALERT);
                    }

                    WindowManager.LayoutParams windowParams = window.getAttributes();
                    windowParams.copyFrom(window.getAttributes());
                    windowParams.width = LayoutParams.MATCH_PARENT;
                    windowParams.height = LayoutParams.WRAP_CONTENT;
                    windowParams.gravity = Gravity.BOTTOM;
                    windowParams.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
                    window.setAttributes(windowParams);
                }
                alertDialog.show();
            }
        });
        floatingViewManager = new FloatingViewManager(this, this);
        floatingViewManager.setFixedTrashIconImage(R.drawable.floating_trash_cross);
        floatingViewManager.setActionTrashIconImage(R.drawable.floating_trash_base);
        FloatingViewManager.Options options = new FloatingViewManager.Options();
        options.overMargin = (int) (16.0F * metrics.density);
        floatingViewManager.addViewToWindow(iconView, options);

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
        if (floatingViewManager != null) {
            floatingViewManager.removeAllViewToWindow();
            floatingViewManager = null;
        }
    }

    private Notification createNotification() {
        // Create an Intent for the BroadcastReceiver
        Intent buttonIntent = new Intent(getApplicationContext(),
                FloatUiButtonReceiver.class);

        // Create the PendingIntent
        PendingIntent btPendingIntent = PendingIntent.getBroadcast(
                getApplicationContext(), 0, buttonIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,
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