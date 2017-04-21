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
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
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
import projekt.substratum.common.References;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.services.notification.FloatUiButtonReceiver;

import static android.content.om.OverlayInfo.STATE_APPROVED_DISABLED;
import static android.content.om.OverlayInfo.STATE_APPROVED_ENABLED;

public class SubstratumFloatInterface extends Service implements FloatingViewListener {

    private static final String TAG = "SubstratumFloat";
    private static final int NOTIFICATION_ID = 92781162;
    private FloatingViewManager mFloatingViewManager;
    private ListView alertDialogListView;
    private String[] final_check;
    private SharedPreferences prefs;
    private boolean trigger_service_restart, trigger_systemui_restart;

    @SuppressWarnings("WrongConstant")
    public String foregroundedApp() {
        UsageStatsManager mUsageStatsManager = (UsageStatsManager) getSystemService("usagestats");
        long time = System.currentTimeMillis();
        List<UsageStats> stats = mUsageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, time - 1000 * 1000, time);
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
        UsageEvents usageEvents = mUsageStatsManager.queryEvents(time - 1000 * 1000, time);
        UsageEvents.Event event = new UsageEvents.Event();
        // Get the last event in the doubly linked list
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event);
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
        windowManager.getDefaultDisplay().getMetrics(metrics);
        final LayoutInflater inflater = LayoutInflater.from(this);
        @SuppressLint("InflateParams") final ImageView iconView = (ImageView)
                inflater.inflate(R.layout.floating_head_layout, null, false);
        iconView.setOnClickListener(v -> {
            String packageName =
                    References.grabPackageName(getApplicationContext(), foregroundedApp());
            String dialogTitle = String.format(getString(R.string.per_app_dialog_title),
                    packageName);

            List<String> state4 = ThemeManager.listOverlays(STATE_APPROVED_DISABLED);
            List<String> state5 = ThemeManager.listOverlays(STATE_APPROVED_ENABLED);
            ArrayList<String> disabled = new ArrayList<>(state4);
            ArrayList<String> enabled = new ArrayList<>(state5);
            ArrayList<String> all_overlays = new ArrayList<>();
            ArrayList<String> to_be_shown = new ArrayList<>();
            all_overlays.addAll(state4);
            all_overlays.addAll(state5);
            boolean show_android_overlays =
                    prefs.getBoolean("floatui_show_android_system_overlays", true);
            for (int i = 0; i < all_overlays.size(); i++) {
                if (all_overlays.get(i).startsWith(foregroundedApp() + ".")) {
                    to_be_shown.add(all_overlays.get(i));
                } else if (show_android_overlays && all_overlays.get(i).startsWith("android.")) {
                    to_be_shown.add(all_overlays.get(i));
                }
            }
            Collections.sort(to_be_shown);

            if (to_be_shown.size() == 0) {
                String format = String.format(
                        getString(R.string.per_app_toast_no_overlays), packageName);
                Toast.makeText(getApplicationContext(), format, Toast.LENGTH_SHORT).show();
            } else {
                final_check = new String[to_be_shown.size()];
                for (int j = 0; j < to_be_shown.size(); j++) {
                    final_check[j] = to_be_shown.get(j);
                }

                ListAdapter itemsAdapter =
                        new ArrayAdapter<>(this, R.layout.multiple_choice_list_entry,
                                final_check);

                TextView title = new TextView(this);
                title.setText(dialogTitle);
                title.setBackgroundColor(getColor(R.color.floatui_dialog_header_background));
                title.setPadding(20, 40, 20, 40);
                title.setGravity(Gravity.CENTER);
                title.setTextColor(getColor(R.color.floatui_dialog_title_color));
                title.setTextSize(20);
                title.setAllCaps(true);

                AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.FloatUiDialog);
                builder.setCustomTitle(title);
                builder.setAdapter(itemsAdapter, (dialog, which) -> {
                });
                builder.setPositiveButton(R.string.per_app_apply, (dialog, which) -> {
                    int locations = alertDialogListView.getCheckedItemCount();
                    trigger_service_restart = false;
                    trigger_systemui_restart = false;
                    ArrayList<String> to_enable = new ArrayList<>();
                    ArrayList<String> to_disable = new ArrayList<>();

                    for (int i = 0; i < final_check.length; i++) {
                        if (alertDialogListView.getCheckedItemPositions().get(i)) {
                            // Check if enabled
                            if (!enabled.contains(final_check[i])) {
                                // It is not enabled, append it to the list
                                to_enable.add(final_check[i]);
                                if (final_check[i].startsWith("android.") ||
                                        final_check[i].startsWith(getPackageName() + "."))
                                    trigger_service_restart = true;
                                if (final_check[i].startsWith("com.android.systemui."))
                                    trigger_systemui_restart = true;
                            }
                        } else {
                            // Check if disabled
                            if (!disabled.contains(final_check[i])) {
                                // It is enabled, append it to the list
                                to_disable.add(final_check[i]);
                                if (final_check[i].startsWith("android.") ||
                                        final_check[i].startsWith(getPackageName() + "."))
                                    trigger_service_restart = true;
                                if (final_check[i].startsWith("com.android.systemui."))
                                    trigger_systemui_restart = true;
                            }
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

                AlertDialog alertDialog = builder.create();
                //noinspection ConstantConditions
                alertDialog.getWindow().setBackgroundDrawable(
                        getDrawable(R.drawable.dialog_background));
                alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                alertDialogListView = alertDialog.getListView();
                alertDialogListView.setItemsCanFocus(false);
                alertDialogListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                alertDialogListView.setOnItemClickListener((parent, view, position, id) -> {
                });
                alertDialog.show();

                for (int i = 0; i < final_check.length; i++) {
                    if (enabled.contains(final_check[i])) {
                        alertDialogListView.setItemChecked(i, true);
                    }
                }
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
        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                getApplicationContext(), 0, new Intent(), 0);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
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