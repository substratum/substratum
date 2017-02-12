package projekt.substratum.services;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.preference.PreferenceManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import projekt.substratum.ProfileErrorInfoActivity;
import projekt.substratum.R;
import projekt.substratum.config.MasqueradeService;
import projekt.substratum.config.References;
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

public class ScheduledProfileService extends IntentService {

    private final int NOTIFICATION_ID = 1023;
    private Context mContext;
    private SharedPreferences prefs;
    private String extra;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;

    public ScheduledProfileService() {
        super("ScheduledProfileService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mContext = this;
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mNotifyManager = (NotificationManager) mContext.getSystemService(
                Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(mContext);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onHandleIntent(Intent intent) {
        extra = intent.getStringExtra(SCHEDULED_PROFILE_TYPE_EXTRA);

        if (extra != null && !extra.isEmpty()) {
            String title_parse = String.format(getString(R.string.profile_notification_title), extra);
            mBuilder.setContentTitle(title_parse)
                    .setSmallIcon(R.drawable.ic_substratum)
                    .setPriority(Notification.PRIORITY_DEFAULT)
                    .setContentText(getString(R.string.profile_pending_notification))
                    .setOngoing(true);
            mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());

            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            while (powerManager.isInteractive()) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            applyScheduledProfile(intent);
        } else {
            mBuilder.setContentTitle(getString(R.string.scheduled_night_profile))
                    .setSmallIcon(R.drawable.ic_substratum)
                    .setPriority(Notification.PRIORITY_DEFAULT)
                    .setContentText(getString(R.string.profile_failed_notification));
            mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
            ScheduledProfileReceiver.completeWakefulIntent(intent);
        }
    }

    private void applyScheduledProfile(Intent intent) {
        String type;

        // Cancel ongoing notification
        mNotifyManager.cancel(NOTIFICATION_ID);
        mBuilder.setOngoing(false)
                .setPriority(Notification.PRIORITY_MAX)
                .setSmallIcon(R.drawable.ic_substratum);
        mBuilder.setContentText(getString(R.string.profile_success_notification));

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
                References.mountRW();
                References.move(mContext, "/system/media/bootanimation.zip",
                        "/system/madia/bootanimation-backup.zip");
                References.copy(mContext, bootanimation.getAbsolutePath(),
                        "/system/media/bootanimation.zip");
                References.setPermissions(644, "/system/media/bootanimation.zip");
                References.mountRO();
            }

            if (References.checkMasquerade(mContext) >= 22) {
                MasqueradeService.applyProfile(mContext, processed, new ArrayList<>(system),
                        to_be_run);
            } else {
                // Restore the whole backed up profile back to /data/system/theme/
                if (theme.exists()) {
                    References.delete(mContext, "/data/system/theme", false);
                    References.copyDir(mContext, theme.getAbsolutePath(), "/data/system/theme");
                    References.setPermissionsRecursively(644, "/data/system/theme/audio");
                    References.setPermissions(755, "/data/system/theme/audio");
                    References.setPermissions(755, "/data/system/theme/audio/alarms");
                    References.setPermissions(755, "/data/system/theme/audio/notifications");
                    References.setPermissions(755, "/data/system/theme/audio/ringtones");
                    References.setPermissions(755, "/data/system/theme/audio/ringtones");
                    References.setPermissionsRecursively(644, "/data/system/theme/fonts/");
                    References.setPermissions(755, "/data/system/theme/fonts/");
                    References.setContext("/data/system/theme");

                    References.disableAll(mContext);
                    References.enableOverlay(mContext, to_be_run);
                    References.restartSystemUI(mContext);
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
                    References.setWallpaper(mContext, homeWallPath, "home");
                    References.setWallpaper(mContext, lockWallPath, "lock");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            Intent notifyIntent = new Intent(mContext, ProfileErrorInfoActivity.class);
            notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            notifyIntent.putExtra("dialog_message", dialog_message);
            PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, notifyIntent, 0);

            mBuilder.setContentTitle(getString(R.string.profile_failed_notification))
                    .setContentText(getString(R.string.profile_failed_info_notification))
                    .setContentIntent(contentIntent);
        }

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
        ScheduledProfileReceiver.completeWakefulIntent(intent);
    }
}
