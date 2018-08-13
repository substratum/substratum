/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
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
import androidx.core.app.NotificationCompat;
import projekt.substratum.R;
import projekt.substratum.Substratum;
import projekt.substratum.activities.profiles.ProfileErrorInfoActivity;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.SubstratumService;
import projekt.substratum.common.platform.ThemeInterfacerService;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.common.systems.ProfileManager;
import projekt.substratum.services.binder.AndromedaBinderService;
import projekt.substratum.services.binder.InterfacerBinderService;
import projekt.substratum.tabs.WallpapersManager;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import static projekt.substratum.common.Systems.checkSubstratumService;
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
    private static SharedPreferences prefs = Substratum.getPreferences();
    private Context context;
    private String extra;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder builder;
    private JobParameters jobParameters;

    @Override
    public boolean onStartJob(JobParameters params) {
        context = this;
        jobParameters = params;
        notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        builder = new NotificationCompat.Builder(context, References
                .DEFAULT_NOTIFICATION_CHANNEL_ID);
        extra = params.getExtras().getString(SCHEDULED_PROFILE_TYPE_EXTRA);

        if ((extra != null) && !extra.isEmpty()) {
            new ApplyProfile(this).execute();
            return true;
        } else {
            builder.setContentTitle(getString(R.string.scheduled_night_profile))
                    .setSmallIcon(R.drawable.ic_substratum)
                    .setPriority(Notification.PRIORITY_DEFAULT)
                    .setContentText(getString(R.string.profile_failed_notification));
            notificationManager.notify(NOTIFICATION_ID, builder.build());
            return false;
        }
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    private static class ApplyProfile extends AsyncTask<Void, Void, Void> {
        private final WeakReference<ScheduledProfileService> ref;

        ApplyProfile(ScheduledProfileService service) {
            super();
            ref = new WeakReference<>(service);
        }

        @Override
        protected void onPreExecute() {
            ScheduledProfileService service = ref.get();

            if (service != null) {
                // Make sure binder service is alive
                if (isAndromedaDevice(service)) {
                    Substratum.getInstance().startBinderService(AndromedaBinderService.class);
                } else if (isBinderInterfacer(service)) {
                    Substratum.getInstance().startBinderService(InterfacerBinderService.class);
                }

                String profileName = "";
                switch (service.extra) {
                    case "day":
                        profileName = service.getString(R.string.profile_notification_title_day);
                        break;
                    case "night":
                        profileName = service.getString(R.string.profile_notification_title_night);
                        break;
                }

                Substratum.log(TAG, "Processing...");
                String titleParse = String.format(
                        service.getString(R.string.profile_notification_title),
                        profileName);
                service.notificationManager.cancel(NOTIFICATION_ID);
                service.builder.setContentTitle(titleParse)
                        .setOngoing(false)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setSmallIcon(R.drawable.ic_substratum)
                        .setContentText(service.getString(R.string.profile_success_notification));
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            ScheduledProfileService service = ref.get();
            if (service != null) {
                Context context = service.context;

                String type;
                if (service.extra.equals(NIGHT)) {
                    type = NIGHT_PROFILE;
                } else {
                    type = DAY_PROFILE;
                }

                String processed = prefs.getString(type, "");
                File overlays = new File(Environment.getExternalStorageDirectory()
                        .getAbsolutePath()
                        + "/substratum/profiles/" + processed + "/overlay_state.xml");
                ArrayList<String> toBeRun = new ArrayList<>();
                List<List<String>> cannotRunOverlays = new ArrayList<>();
                List<String> system = new ArrayList<>();
                StringBuilder dialogMessage = new StringBuilder();
                if (overlays.exists()) {
                    List<List<String>> profiles =
                            ProfileManager.readProfileStatePackageWithTargetPackage(processed, 5);
                    system = ProfileManager.readProfileStatePackage(processed, 4);
                    system.addAll(ProfileManager.readProfileStatePackage(processed, 5));

                    // Now process the overlays to be enabled
                    for (List<String> profile : profiles) {
                        String packageName = profile.get(0);
                        String targetPackage = profile.get(1);
                        if (Packages.isPackageInstalled(context, targetPackage)) {
                            if (system.contains(packageName)) {
                                toBeRun.add(packageName);
                            } else {
                                cannotRunOverlays.add(profile);
                            }
                        }
                    }

                    // Parse non-exist profile overlay packages
                    for (List<String> cannotRunOverlay : cannotRunOverlays) {
                        String packageName = cannotRunOverlay.get(0);
                        String targetPackage = cannotRunOverlay.get(1);
                        String packageDetail = packageName.replace(targetPackage + '.', "");
                        String detailSplit = Arrays.toString(packageDetail.split("\\."))
                                .replace("[", "")
                                .replace("]", "")
                                .replace(",", " ");

                        if (dialogMessage.length() == 0) {
                            dialogMessage.append("\u2022 ")
                                    .append(targetPackage).append(" (")
                                    .append(detailSplit).append(')');
                        } else {
                            dialogMessage.append('\n' + "\u2022 ")
                                    .append(targetPackage).append(" (")
                                    .append(detailSplit).append(')');
                        }
                    }
                }

                if (cannotRunOverlays.isEmpty()) {
                    File theme = new File(
                            Environment.getExternalStorageDirectory().getAbsolutePath() +
                                    "/substratum/profiles/" + processed + "/theme");

                    // Encrypted devices boot Animation
                    File bootanimation = new File(theme, "bootanimation.zip");
                    if (bootanimation.exists() &&
                            (Systems.getDeviceEncryptionStatus(context) > 1)) {
                        FileOperations.mountRW();
                        FileOperations.move(context, "/system/media/bootanimation.zip",
                                "/system/madia/bootanimation-backup.zip");
                        FileOperations.copy(context, bootanimation.getAbsolutePath(),
                                "/system/media/bootanimation.zip");
                        FileOperations.setPermissions(644, "/system/media/bootanimation.zip");
                        FileOperations.mountRO();
                    }

                    Iterable<String> toBeDisabled = new ArrayList<>(system);
                    boolean shouldRestartUi = ThemeManager.shouldRestartUI(context,
                            toBeDisabled)
                            || ThemeManager.shouldRestartUI(context, toBeRun);
                    if (isBinderInterfacer(context)) {
                        ThemeInterfacerService.applyProfile(
                                processed,
                                new ArrayList<>(system),
                                toBeRun,
                                shouldRestartUi);
                    } else if (checkSubstratumService(context)) {
                        SubstratumService.applyProfile(
                                processed,
                                new ArrayList<>(system),
                                toBeRun,
                                shouldRestartUi);
                    }

                    // Restore wallpapers
                    String homeWallPath = Environment.getExternalStorageDirectory()
                            .getAbsolutePath() +
                            "/substratum/profiles/" + processed + "/wallpaper.png";
                    String lockWallPath = Environment.getExternalStorageDirectory()
                            .getAbsolutePath() +
                            "/substratum/profiles/" + processed + "/wallpaper_lock.png";
                    File homeWall = new File(homeWallPath);
                    File lockWall = new File(lockWallPath);
                    if (homeWall.exists() || lockWall.exists()) {
                        try {
                            WallpapersManager.setWallpaper(context, homeWallPath, "home");
                            WallpapersManager.setWallpaper(context, lockWallPath, "lock");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    Intent notifyIntent = new Intent(context, ProfileErrorInfoActivity.class);
                    notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    notifyIntent.putExtra("dialogMessage", dialogMessage.toString());
                    PendingIntent contentIntent =
                            PendingIntent.getActivity(context, 0, notifyIntent, 0);

                    service.builder.setContentTitle(
                            service.getString(R.string.profile_failed_notification))
                            .setContentText(service.getString(
                                    R.string.profile_failed_info_notification))
                            .setContentIntent(contentIntent);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void params) {
            ScheduledProfileService service = ref.get();
            if (service != null) {
                Context context = service.context;

                // Create new alarm
                boolean isNight = service.extra.equals(NIGHT);
                int hour = isNight ?
                        prefs.getInt(NIGHT_PROFILE_HOUR, 0) :
                        prefs.getInt(DAY_PROFILE_HOUR, 0);
                int minute = isNight ?
                        prefs.getInt(NIGHT_PROFILE_MINUTE, 0) :
                        prefs.getInt(DAY_PROFILE_MINUTE, 0);

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);
                calendar.add(Calendar.DAY_OF_YEAR, 1);

                Intent i = new Intent(context, ScheduledProfileReceiver.class);
                i.putExtra(SCHEDULED_PROFILE_TYPE_EXTRA, service.extra);
                PendingIntent newIntent = PendingIntent.getBroadcast(
                        context,
                        isNight ? 0 : 1, i,
                        PendingIntent.FLAG_UPDATE_CURRENT);

                AlarmManager alarmMgr = (AlarmManager)
                        context.getSystemService(Context.ALARM_SERVICE);

                if (alarmMgr != null) {
                    alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            newIntent);
                }

                //save current profile
                prefs.edit().putString(SCHEDULED_PROFILE_CURRENT_PROFILE, service.extra).apply();

                //all set, notify user the output
                service.notificationManager.notify(NOTIFICATION_ID, service.builder.build());
                service.jobFinished(service.jobParameters, false);
            }
        }
    }
}