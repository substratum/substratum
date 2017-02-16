package projekt.substratum.services;

import android.app.Notification;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import jp.co.recruit_lifestyle.android.floatingview.FloatingViewListener;
import jp.co.recruit_lifestyle.android.floatingview.FloatingViewManager;
import projekt.substratum.R;
import projekt.substratum.config.References;

public class SubstratumFloatInterface extends Service implements FloatingViewListener {

    private static final String TAG = "SubstratumFloat";
    private static final int NOTIFICATION_ID = 92781162;
    private FloatingViewManager mFloatingViewManager;

    @SuppressWarnings("WrongConstant")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mFloatingViewManager != null) {
            return START_STICKY;
        }
        final DisplayMetrics metrics = new DisplayMetrics();
        final WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);
        final LayoutInflater inflater = LayoutInflater.from(this);
        final ImageView iconView = (ImageView)
                inflater.inflate(R.layout.floating_head_layout, null, false);
        iconView.setOnClickListener(v -> {
            UsageStatsManager usm = (UsageStatsManager) getSystemService("usagestats");
            long time = System.currentTimeMillis();
            List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,
                    time - 1000 * 1000, time);
            if (appList != null && appList.size() > 0) {
                SortedMap<Long, UsageStats> mySortedMap = new TreeMap<>();
                for (UsageStats usageStats : appList) {
                    mySortedMap.put(usageStats.getLastTimeUsed(),
                            usageStats);
                }
                if (mySortedMap != null && !mySortedMap.isEmpty()) {
                    String currentApp = mySortedMap.get(
                            mySortedMap.lastKey()).getPackageName();
                    String packageName =
                            References.grabPackageName(getApplicationContext(), currentApp);

                    String format = String.format(getString(R.string.per_app_toast),
                            packageName);
                    Toast toast = Toast.makeText(
                            getApplicationContext(),
                            format,
                            Toast.LENGTH_LONG);
                    toast.show();
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
        Toast.makeText(
                getApplicationContext(),
                getString(R.string.per_app_removed),
                Toast.LENGTH_LONG).show();
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
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setWhen(System.currentTimeMillis());
        builder.setSmallIcon(R.drawable.notification_floatui);
        builder.setContentTitle(getString(R.string.app_name));
        builder.setContentText(getString(R.string.per_app_notification_summary));
        builder.setOngoing(true);
        builder.setPriority(NotificationCompat.PRIORITY_MIN);
        builder.setCategory(NotificationCompat.CATEGORY_SERVICE);

        return builder.build();
    }
}