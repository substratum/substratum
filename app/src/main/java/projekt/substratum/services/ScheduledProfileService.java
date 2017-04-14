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

package projekt.substratum.services;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import projekt.substratum.ProfileErrorInfoActivity;
import projekt.substratum.R;
import projekt.substratum.Substratum;
import projekt.substratum.config.FileOperations;
import projekt.substratum.config.References;
import projekt.substratum.config.ThemeInterfacerService;
import projekt.substratum.config.ThemeManager;
import projekt.substratum.config.WallpaperManager;
import projekt.substratum.util.ReadOverlaysFile;

import static projekt.substratum.fragments.ProfileFragment.DAY_PROFILE;
import static projekt.substratum.fragments.ProfileFragment.DAY_PROFILE_HOUR;
import static projekt.substratum.fragments.ProfileFragment.DAY_PROFILE_MINUTE;
import static projekt.substratum.fragments.ProfileFragment.NIGHT;
import static projekt.substratum.fragments.ProfileFragment.NIGHT_PROFILE;
import static projekt.substratum.fragments.ProfileFragment.NIGHT_PROFILE_HOUR;
import static projekt.substratum.fragments.ProfileFragment.NIGHT_PROFILE_MINUTE;
import static projekt.substratum.fragments.ProfileFragment.SCHEDULED_PROFILE_CURRENT_PROFILE;
import static projekt.substratum.fragments.ProfileFragment.SCHEDULED_PROFILE_TYPE_EXTRA;

public class ScheduledProfileService extends JobService {

    private final int NOTIFICATION_ID = 1023;
    private Context mContext;
    private SharedPreferences prefs;
    private String extra;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;

    @Override
    public boolean onStartJob(JobParameters params) {
        mContext = this;
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mNotifyManager = (NotificationManager) mContext.getSystemService(
                Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(mContext);
        extra = params.getExtras().getString(SCHEDULED_PROFILE_TYPE_EXTRA);

        if (extra != null && !extra.isEmpty()) {
            new ApplyProfile(this).execute(params);
            return true;
        } else {
            mBuilder.setContentTitle(getString(R.string.scheduled_night_profile))
                    .setSmallIcon(R.drawable.ic_substratum)
                    .setPriority(Notification.PRIORITY_DEFAULT)
                    .setContentText(getString(R.string.profile_failed_notification));
            mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
            return false;
        }
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    private class ApplyProfile extends AsyncTask<JobParameters, Void, JobParameters> {
        private JobService jobService;

        ApplyProfile(JobService _jobService) {
            jobService = _jobService;
        }

        @Override
        protected void onPreExecute() {
            // Make sure binder service is alive
            Substratum.getInstance().startBinderService();

            String profile_name = "";
            switch (extra) {
                case "day":
                    profile_name = getString(R.string.profile_notification_title_day);
                    break;
                case "night":
                    profile_name = getString(R.string.profile_notification_title_night);
                    break;
            }

            Log.d("ScheduledProfile", "Processing...");
            String title_parse = String.format(getString(R.string.profile_notification_title),
                    profile_name);
            mNotifyManager.cancel(NOTIFICATION_ID);
            mBuilder.setContentTitle(title_parse)
                    .setOngoing(false)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setSmallIcon(R.drawable.ic_substratum)
                    .setContentText(getString(R.string.profile_success_notification));
        }

        @Override
        protected JobParameters doInBackground(JobParameters... params) {
            String type;
            if (extra.equals(NIGHT)) {
                type = NIGHT_PROFILE;
            } else {
                type = DAY_PROFILE;
            }

            String processed = prefs.getString(type, "");
            File overlays = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/substratum/profiles/" + processed + "/overlays.xml");
            ArrayList<String> to_be_run = new ArrayList<>();
            List<List<String>> cannot_run_overlays = new ArrayList<>();
            List<String> system = new ArrayList<>();
            String dialog_message = "";
            if (overlays.exists()) {
                String[] commandsSystem4 = {"/data/system/overlays.xml", "4"};
                String[] commandsSystem5 = {"/data/system/overlays.xml", "5"};
                String[] commands = {overlays.getAbsolutePath(), "5"};

                List<List<String>> profile = ReadOverlaysFile.withTargetPackage(
                        mContext, commands);
                system = ReadOverlaysFile.main(mContext, commandsSystem4);
                system.addAll(ReadOverlaysFile.main(mContext, commandsSystem5));

                // Now process the overlays to be enabled
                for (int i = 0, size = profile.size(); i < size; i++) {
                    String packageName = profile.get(i).get(0);
                    String targetPackage = profile.get(i).get(1);
                    if (References.isPackageInstalled(mContext, targetPackage)) {
                        if (!packageName.endsWith(".icon")) {
                            if (system.contains(packageName)) {
                                to_be_run.add(packageName);
                            } else {
                                cannot_run_overlays.add(profile.get(i));
                            }
                        }
                    }
                }

                // Parse non-exist profile overlay packages
                for (int i = 0; i < cannot_run_overlays.size(); i++) {
                    String packageName = cannot_run_overlays.get(i).get(0);
                    String targetPackage = cannot_run_overlays.get(i).get(1);
                    String packageDetail = packageName.replace(targetPackage + ".", "");
                    String detailSplit = Arrays.toString(packageDetail.split("\\."))
                            .replace("[", "")
                            .replace("]", "")
                            .replace(",", " ");

                    if (dialog_message.length() == 0) {
                        dialog_message = dialog_message + "\u2022 " + targetPackage + " (" +
                                detailSplit + ")";
                    } else {
                        dialog_message = dialog_message + "\n" + "\u2022 " + targetPackage
                                + " (" + detailSplit + ")";
                    }
                }
            }

            if (cannot_run_overlays.size() == 0) {
                File theme = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/substratum/profiles/" + processed + "/theme");

                // Encrypted devices boot Animation
                File bootanimation = new File(theme, "bootanimation.zip");
                if (bootanimation.exists() &&
                        References.getDeviceEncryptionStatus(mContext) > 1) {
                    FileOperations.mountRW();
                    FileOperations.move(mContext, "/system/media/bootanimation.zip",
                            "/system/madia/bootanimation-backup.zip");
                    FileOperations.copy(mContext, bootanimation.getAbsolutePath(),
                            "/system/media/bootanimation.zip");
                    FileOperations.setPermissions(644, "/system/media/bootanimation.zip");
                    FileOperations.mountRO();
                }

                if (References.checkThemeInterfacer(mContext)) {
                    ArrayList<String> toBeDisabled = new ArrayList<>(system);
                    boolean shouldRestartUi = ThemeManager.shouldRestartUI(mContext, toBeDisabled)
                            || ThemeManager.shouldRestartUI(mContext, to_be_run);
                    ThemeInterfacerService.applyProfile(mContext, processed, new ArrayList<>
                                    (system),
                            to_be_run, shouldRestartUi);
                } else {
                    // Restore the whole backed up profile back to /data/system/theme/
                    if (theme.exists()) {
                        FileOperations.delete(mContext, "/data/system/theme", false);
                        FileOperations.copyDir(mContext, theme.getAbsolutePath(),
                                "/data/system/theme");
                        FileOperations.setPermissionsRecursively(644, "/data/system/theme/audio");
                        FileOperations.setPermissions(755, "/data/system/theme/audio");
                        FileOperations.setPermissions(755, "/data/system/theme/audio/alarms");
                        FileOperations.setPermissions(755,
                                "/data/system/theme/audio/notifications");
                        FileOperations.setPermissions(755, "/data/system/theme/audio/ringtones");
                        FileOperations.setPermissions(755, "/data/system/theme/audio/ringtones");
                        FileOperations.setPermissionsRecursively(644, "/data/system/theme/fonts/");
                        FileOperations.setPermissions(755, "/data/system/theme/fonts/");
                        FileOperations.setContext("/data/system/theme");

                        ThemeManager.disableAll(mContext);
                        ThemeManager.enableOverlay(mContext, to_be_run);
                        ThemeManager.restartSystemUI(mContext);
                    }
                }

                // Restore wallpapers
                String homeWallPath = Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/substratum/profiles/" + processed + "/wallpaper.png";
                String lockWallPath = Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/substratum/profiles/" + processed + "/wallpaper_lock.png";
                File homeWall = new File(homeWallPath);
                File lockWall = new File(lockWallPath);
                if (homeWall.exists() || lockWall.exists()) {
                    try {
                        WallpaperManager.setWallpaper(mContext, homeWallPath, "home");
                        WallpaperManager.setWallpaper(mContext, lockWallPath, "lock");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                Intent notifyIntent = new Intent(mContext, ProfileErrorInfoActivity.class);
                notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                notifyIntent.putExtra("dialog_message", dialog_message);
                PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0,
                        notifyIntent, 0);

                mBuilder.setContentTitle(getString(R.string.profile_failed_notification))
                        .setContentText(getString(R.string.profile_failed_info_notification))
                        .setContentIntent(contentIntent);
            }
            return null;
        }

        @Override
        protected void onPostExecute(JobParameters params) {
            //create new alarm
            boolean isNight = extra.equals(NIGHT);
            int hour = isNight ? prefs.getInt(NIGHT_PROFILE_HOUR, 0) :
                    prefs.getInt(DAY_PROFILE_HOUR, 0);
            int minute = isNight ? prefs.getInt(NIGHT_PROFILE_MINUTE, 0) :
                    prefs.getInt(DAY_PROFILE_MINUTE, 0);

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.add(Calendar.DAY_OF_YEAR, 1);

            Intent i = new Intent(mContext, ScheduledProfileReceiver.class);
            i.putExtra(SCHEDULED_PROFILE_TYPE_EXTRA, extra);
            PendingIntent newIntent = PendingIntent.getBroadcast(mContext, isNight ? 0 : 1, i,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            AlarmManager alarmMgr = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
            alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                    newIntent);

            //save current profile
            prefs.edit().putString(SCHEDULED_PROFILE_CURRENT_PROFILE, extra).apply();

            //all set, notify user the output
            mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());

            //TODO: this thing cause problem(s), is this really needed?
            //jobService.jobFinished(params, false);
        }
    }
}
