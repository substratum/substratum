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

package projekt.substratum.services.profiles;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import projekt.substratum.R;
import projekt.substratum.Substratum;
import projekt.substratum.activities.profiles.ProfileErrorInfoActivity;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeInterfacerService;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.common.systems.ProfileManager;
import projekt.substratum.common.tabs.WallpaperManager;
import projekt.substratum.services.binder.AndromedaBinderService;
import projekt.substratum.services.binder.InterfacerBinderService;

import static projekt.substratum.common.Systems.isAndromedaDevice;
import static projekt.substratum.common.Systems.isBinderInterfacer;
import static projekt.substratum.common.systems.ProfileManager.DAY_PROFILE;
import static projekt.substratum.common.systems.ProfileManager.DAY_PROFILE_HOUR;
import static projekt.substratum.common.systems.ProfileManager.DAY_PROFILE_MINUTE;
import static projekt.substratum.common.systems.ProfileManager.NIGHT;
import static projekt.substratum.common.systems.ProfileManager.NIGHT_PROFILE;
import static projekt.substratum.common.systems.ProfileManager.NIGHT_PROFILE_HOUR;
import static projekt.substratum.common.systems.ProfileManager.NIGHT_PROFILE_MINUTE;
import static projekt.substratum.common.systems.ProfileManager.SCHEDULED_PROFILE_CURRENT_PROFILE;
import static projekt.substratum.common.systems.ProfileManager.SCHEDULED_PROFILE_TYPE_EXTRA;

public class ScheduledProfileService extends JobService {

    private static final int NOTIFICATION_ID = 1023;
    private static final String TAG = "ScheduledProfile";
    private Context context;
    private SharedPreferences prefs;
    private String extra;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private JobParameters jobParameters;

    @Override
    public boolean onStartJob(final JobParameters params) {
        this.context = this;
        this.jobParameters = params;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
        this.mNotifyManager =
                (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.mBuilder = new NotificationCompat.Builder(this.context, References
                .DEFAULT_NOTIFICATION_CHANNEL_ID);
        this.extra = params.getExtras().getString(SCHEDULED_PROFILE_TYPE_EXTRA);

        if (this.extra != null && !this.extra.isEmpty()) {
            new ApplyProfile(this).execute();
            return true;
        } else {
            this.mBuilder.setContentTitle(this.getString(R.string.scheduled_night_profile))
                    .setSmallIcon(R.drawable.ic_substratum)
                    .setPriority(Notification.PRIORITY_DEFAULT)
                    .setContentText(this.getString(R.string.profile_failed_notification));
            this.mNotifyManager.notify(NOTIFICATION_ID, this.mBuilder.build());
            return false;
        }
    }

    @Override
    public boolean onStopJob(final JobParameters params) {
        return false;
    }

    private static class ApplyProfile extends AsyncTask<Void, Void, Void> {
        private final WeakReference<ScheduledProfileService> ref;

        ApplyProfile(final ScheduledProfileService service) {
            super();
            this.ref = new WeakReference<>(service);
        }

        @Override
        protected void onPreExecute() {
            final ScheduledProfileService service = this.ref.get();

            if (service != null) {
                // Make sure binder service is alive
                if (isAndromedaDevice(service)) {
                    Substratum.getInstance().startBinderService(AndromedaBinderService.class);
                } else if (isBinderInterfacer(service)) {
                    Substratum.getInstance().startBinderService(InterfacerBinderService.class);
                }

                String profile_name = "";
                switch (service.extra) {
                    case "day":
                        profile_name = service.getString(R.string.profile_notification_title_day);
                        break;
                    case "night":
                        profile_name = service.getString(R.string.profile_notification_title_night);
                        break;
                }

                Log.d(TAG, "Processing...");
                final String title_parse = String.format(
                        service.getString(R.string.profile_notification_title),
                        profile_name);
                service.mNotifyManager.cancel(NOTIFICATION_ID);
                service.mBuilder.setContentTitle(title_parse)
                        .setOngoing(false)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setSmallIcon(R.drawable.ic_substratum)
                        .setContentText(service.getString(R.string.profile_success_notification));
            }
        }

        @Override
        protected Void doInBackground(final Void... params) {
            final ScheduledProfileService service = this.ref.get();
            if (service != null) {
                final Context context = service.context;
                final SharedPreferences prefs = service.prefs;

                final String type;
                if (service.extra.equals(NIGHT)) {
                    type = NIGHT_PROFILE;
                } else {
                    type = DAY_PROFILE;
                }

                final String processed = prefs.getString(type, "");
                final File overlays = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/substratum/profiles/" + processed + "/overlay_state.xml");
                final ArrayList<String> to_be_run = new ArrayList<>();
                final List<List<String>> cannot_run_overlays = new ArrayList<>();
                List<String> system = new ArrayList<>();
                final StringBuilder dialog_message = new StringBuilder();
                if (overlays.exists()) {
                    final List<List<String>> profile =
                            ProfileManager.readProfileStatePackageWithTargetPackage(processed, 5);
                    system = ProfileManager.readProfileStatePackage(processed, 4);
                    system.addAll(ProfileManager.readProfileStatePackage(processed, 5));

                    // Now process the overlays to be enabled
                    for (int i = 0, size = profile.size(); i < size; i++) {
                        final String packageName = profile.get(i).get(0);
                        final String targetPackage = profile.get(i).get(1);
                        if (Packages.isPackageInstalled(context, targetPackage)) {
                            if (system.contains(packageName)) {
                                to_be_run.add(packageName);
                            } else {
                                cannot_run_overlays.add(profile.get(i));
                            }
                        }
                    }

                    // Parse non-exist profile overlay packages
                    for (int i = 0; i < cannot_run_overlays.size(); i++) {
                        final String packageName = cannot_run_overlays.get(i).get(0);
                        final String targetPackage = cannot_run_overlays.get(i).get(1);
                        final String packageDetail = packageName.replace(targetPackage + ".", "");
                        final String detailSplit = Arrays.toString(packageDetail.split("\\."))
                                .replace("[", "")
                                .replace("]", "")
                                .replace(",", " ");

                        if (dialog_message.length() == 0) {
                            dialog_message.append("\u2022 ")
                                    .append(targetPackage).append(" (")
                                    .append(detailSplit).append(")");
                        } else {
                            dialog_message.append("\n" + "\u2022 ")
                                    .append(targetPackage).append(" (")
                                    .append(detailSplit).append(")");
                        }
                    }
                }

                if (cannot_run_overlays.isEmpty()) {
                    final File theme = new File(
                            Environment.getExternalStorageDirectory().getAbsolutePath() +
                                    "/substratum/profiles/" + processed + "/theme");

                    // Encrypted devices boot Animation
                    final File bootanimation = new File(theme, "bootanimation.zip");
                    if (bootanimation.exists() &&
                            Systems.getDeviceEncryptionStatus(context) > 1) {
                        FileOperations.mountRW();
                        FileOperations.move(context, "/system/media/bootanimation.zip",
                                "/system/madia/bootanimation-backup.zip");
                        FileOperations.copy(context, bootanimation.getAbsolutePath(),
                                "/system/media/bootanimation.zip");
                        FileOperations.setPermissions(644, "/system/media/bootanimation.zip");
                        FileOperations.mountRO();
                    }

                    final Iterable<String> toBeDisabled = new ArrayList<>(system);
                    final boolean shouldRestartUi = ThemeManager.shouldRestartUI(context, toBeDisabled)
                            || ThemeManager.shouldRestartUI(context, to_be_run);
                    ThemeInterfacerService.applyProfile(
                            context,
                            processed,
                            new ArrayList<>(system),
                            to_be_run,
                            shouldRestartUi);

                    // Restore wallpapers
                    final String homeWallPath = Environment.getExternalStorageDirectory()
                            .getAbsolutePath() +
                            "/substratum/profiles/" + processed + "/wallpaper.png";
                    final String lockWallPath = Environment.getExternalStorageDirectory()
                            .getAbsolutePath() +
                            "/substratum/profiles/" + processed + "/wallpaper_lock.png";
                    final File homeWall = new File(homeWallPath);
                    final File lockWall = new File(lockWallPath);
                    if (homeWall.exists() || lockWall.exists()) {
                        try {
                            WallpaperManager.setWallpaper(context, homeWallPath, "home");
                            WallpaperManager.setWallpaper(context, lockWallPath, "lock");
                        } catch (final Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    final Intent notifyIntent = new Intent(context, ProfileErrorInfoActivity.class);
                    notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    notifyIntent.putExtra("dialog_message", dialog_message.toString());
                    final PendingIntent contentIntent =
                            PendingIntent.getActivity(context, 0, notifyIntent, 0);

                    service.mBuilder.setContentTitle(
                            service.getString(R.string.profile_failed_notification))
                            .setContentText(service.getString(
                                    R.string.profile_failed_info_notification))
                            .setContentIntent(contentIntent);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(final Void params) {
            final ScheduledProfileService service = this.ref.get();
            if (service != null) {
                final Context context = service.context;
                final SharedPreferences prefs = service.prefs;

                // Create new alarm
                final boolean isNight = service.extra.equals(NIGHT);
                final int hour = isNight ?
                        prefs.getInt(NIGHT_PROFILE_HOUR, 0) :
                        prefs.getInt(DAY_PROFILE_HOUR, 0);
                final int minute = isNight ?
                        prefs.getInt(NIGHT_PROFILE_MINUTE, 0) :
                        prefs.getInt(DAY_PROFILE_MINUTE, 0);

                final Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);
                calendar.add(Calendar.DAY_OF_YEAR, 1);

                final Intent i = new Intent(context, ScheduledProfileReceiver.class);
                i.putExtra(SCHEDULED_PROFILE_TYPE_EXTRA, service.extra);
                final PendingIntent newIntent = PendingIntent.getBroadcast(
                        context,
                        isNight ? 0 : 1, i,
                        PendingIntent.FLAG_UPDATE_CURRENT);

                final AlarmManager alarmMgr = (AlarmManager)
                        context.getSystemService(Context.ALARM_SERVICE);

                if (alarmMgr != null) {
                    alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            newIntent);
                }

                //save current profile
                prefs.edit().putString(SCHEDULED_PROFILE_CURRENT_PROFILE, service.extra).apply();

                //all set, notify user the output
                service.mNotifyManager.notify(NOTIFICATION_ID, service.mBuilder.build());
                service.jobFinished(service.jobParameters, false);
            }
        }
    }
}