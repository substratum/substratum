package projekt.substratum.common;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import projekt.substratum.R;
import projekt.substratum.activities.launch.ThemeLaunchActivity;
import projekt.substratum.util.compilers.CacheCreator;

import static projekt.substratum.common.References.KEY_RETRIEVAL;
import static projekt.substratum.common.References.SUBSTRATUM_LAUNCHER_CLASS;
import static projekt.substratum.common.References.SUBSTRATUM_LAUNCHER_CLASS_PATH;
import static projekt.substratum.common.References.TEMPLATE_GET_KEYS;
import static projekt.substratum.common.References.TEMPLATE_THEME_MODE;
import static projekt.substratum.common.References.hashPassthrough;
import static projekt.substratum.common.References.metadataEncryptionValue;
import static projekt.substratum.common.References.spreadYourWingsAndFly;

public class Theming {
    public static boolean isCachingEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("caching_enabled", false);
    }

    public static void refreshInstalledThemesPref(Context context) {
        SharedPreferences.Editor editor =
                PreferenceManager.getDefaultSharedPreferences(context).edit();

        // Initial parse of what is installed on the device
        Set<String> installed_themes = new TreeSet<>();
        List<ResolveInfo> all_themes = Packages.getThemes(context);
        for (int i = 0; i < all_themes.size(); i++) {
            installed_themes.add(all_themes.get(i).activityInfo.packageName);
        }
        editor.putStringSet("installed_themes", installed_themes);
        editor.apply();
    }

    // Locate the proper launch intent for the themes
    @SuppressWarnings("SameParameterValue")
    public static Intent sendLaunchIntent(Context mContext, String currentTheme,
                                          boolean theme_legacy, String theme_mode,
                                          Boolean notification) {
        Intent originalIntent = new Intent(Intent.ACTION_MAIN);
        if (theme_legacy)
            originalIntent.putExtra("theme_legacy", true);
        if (theme_mode != null) {
            originalIntent.putExtra("theme_mode", theme_mode);
        }
        if (notification) {
            originalIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
        originalIntent.putExtra("hash_passthrough", hashPassthrough(mContext));
        originalIntent.putExtra("certified", !spreadYourWingsAndFly(mContext));
        try {
            PackageManager pm = mContext.getPackageManager();
            PackageInfo info = pm.getPackageInfo(currentTheme, PackageManager.GET_ACTIVITIES);
            ActivityInfo[] list = info.activities;
            for (ActivityInfo aList : list) {
                // We need to look for what the themer assigned the class to be! This is a dynamic
                // function that only launches the correct SubstratumLauncher class. Having it
                // hardcoded is bad.
                if (aList.name.equals(currentTheme + SUBSTRATUM_LAUNCHER_CLASS)) {
                    originalIntent.setComponent(
                            new ComponentName(
                                    currentTheme, currentTheme + SUBSTRATUM_LAUNCHER_CLASS));
                    return originalIntent;
                } else if (aList.name.equals(SUBSTRATUM_LAUNCHER_CLASS_PATH)) {
                    originalIntent.setComponent(
                            new ComponentName(
                                    currentTheme, SUBSTRATUM_LAUNCHER_CLASS_PATH));
                    return originalIntent;
                }
            }
        } catch (Exception e) {
            // Suppress warning
        }
        return null;
    }

    // Launch intent for a theme
    public static void launchTheme(Context mContext,
                                   String package_name,
                                   String theme_mode) {
        Intent theme_intent = themeIntent(
                mContext,
                package_name,
                theme_mode,
                TEMPLATE_THEME_MODE);
        mContext.startActivity(theme_intent);
    }

    // Key return of a theme
    public static void getThemeKeys(Context mContext, String package_name) {
        Intent theme_intent = themeIntent(
                mContext,
                package_name,
                null,
                TEMPLATE_GET_KEYS);
        try {
            mContext.startActivity(theme_intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Intent themeIntent(Context mContext,
                                     String package_name,
                                     String theme_mode,
                                     String actionIntent) {
        boolean should_debug = projekt.substratum.BuildConfig.DEBUG;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        if (should_debug) Log.d("ThemeLauncher", "Creating new intent...");
        Intent intentActivity;
        if (actionIntent.equals(TEMPLATE_GET_KEYS)) {
            intentActivity = new Intent();
        } else {
            intentActivity = new Intent(mContext, ThemeLaunchActivity.class);
        }
        intentActivity.putExtra("package_name", package_name);
        if (should_debug) Log.d("ThemeLauncher", "Assigning action to intent...");
        intentActivity.setAction(actionIntent);
        if (should_debug) Log.d("ThemeLauncher", "Assigning package name to intent...");
        intentActivity.setPackage(package_name);
        intentActivity.putExtra("calling_package_name", mContext.getPackageName());
        if (should_debug) Log.d("ThemeLauncher", "Checking for theme system type...");
        intentActivity.putExtra("oms_check", !Systems.checkOMS(mContext));
        intentActivity.putExtra("theme_mode", theme_mode);
        intentActivity.putExtra("notification", false);
        if (should_debug) Log.d("ThemeLauncher", "Obtaining APK signature hash...");
        intentActivity.putExtra("hash_passthrough", hashPassthrough(mContext));
        if (should_debug) Log.d("ThemeLauncher", "Checking for certification...");
        intentActivity.putExtra("certified", prefs.getBoolean("complexion", true));
        if (should_debug) Log.d("ThemeLauncher", "Starting Activity...");
        return intentActivity;
    }

    // This class serves to update the theme's cache on demand
    public static class SubstratumThemeUpdate extends AsyncTask<Void, Integer, String> {
        private final String TAG = "SubstratumThemeUpdate";
        private ProgressDialog progress;
        private String theme_name, theme_package, theme_mode;
        private Boolean launch = false;
        private Boolean cacheable = false;
        private Context mContext;
        private LocalBroadcastManager localBroadcastManager;
        private KeyRetrieval keyRetrieval;
        private Intent securityIntent;
        private Cipher cipher;
        private Handler handler = new Handler();
        private Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Waiting for encryption key handshake approval...");
                if (securityIntent != null) {
                    Log.d(TAG, "Encryption key handshake approved!");
                    handler.removeCallbacks(runnable);
                } else {
                    Log.d(TAG, "Encryption key still null...");
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    handler.postDelayed(this, 100);
                }
            }
        };

        public SubstratumThemeUpdate(Context mContext, String theme_package, String theme_name,
                                     String theme_mode) {
            this.mContext = mContext;
            this.theme_package = theme_package;
            this.theme_name = theme_name;
            this.theme_mode = theme_mode;
            this.cacheable = Packages.isPackageDebuggable(mContext, theme_package);
        }

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(mContext, R.style.AppTheme_DialogAlert);

            String parse = String.format(mContext.getString(R.string.on_demand_updating_text),
                    theme_name);

            progress.setTitle(mContext.getString(R.string.on_demand_updating_title));
            progress.setMessage(parse);
            progress.setIndeterminate(false);
            progress.setCancelable(false);
            if (cacheable) progress.show();
        }

        @Override
        protected void onPostExecute(String result) {
            if (progress != null) {
                progress.dismiss();
            }
            if (launch) {
                Toast.makeText(mContext, mContext.getString(R.string
                                .background_updated_toast),
                        Toast.LENGTH_SHORT).show();
                // At this point, we can safely assume that the theme has successfully extracted
                launchTheme(mContext, theme_package, theme_mode);
            } else if (!cacheable) {
                Toast.makeText(mContext, mContext.getString(R.string.
                                background_updated_toast_rejected),
                        Toast.LENGTH_SHORT).show();
                // Just in case.
                new CacheCreator().wipeCache(mContext, theme_package);
            } else {
                Toast.makeText(mContext, mContext.getString(R.string
                                .background_updated_toast_cancel),
                        Toast.LENGTH_SHORT).show();
                // We don't want this cache anymore, delete it from the system completely
                new CacheCreator().wipeCache(mContext, theme_package);
            }
        }

        @Override
        protected String doInBackground(Void... Params) {
            if (!cacheable) return null;

            String encrypt_check =
                    Packages.getOverlayMetadata(mContext, theme_package, References
                            .metadataEncryption);

            if (encrypt_check != null && encrypt_check.equals(metadataEncryptionValue)) {
                Log.d(TAG, "This overlay for " +
                        Packages.getPackageName(mContext, theme_package) +
                        " is encrypted, passing handshake to the theme package...");

                getThemeKeys(mContext, theme_package);

                keyRetrieval = new KeyRetrieval();
                IntentFilter if1 = new IntentFilter(KEY_RETRIEVAL);
                localBroadcastManager = LocalBroadcastManager.getInstance(mContext);
                localBroadcastManager.registerReceiver(keyRetrieval, if1);

                int counter = 0;
                handler.postDelayed(runnable, 100);
                while (securityIntent == null && counter < 5) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    counter++;
                }
                if (counter > 5) {
                    Log.e(TAG, "Could not receive handshake in time...");
                    return null;
                }

                if (securityIntent != null) {
                    try {
                        byte[] encryption_key =
                                securityIntent.getByteArrayExtra("encryption_key");
                        byte[] iv_encrypt_key =
                                securityIntent.getByteArrayExtra("iv_encrypt_key");

                        cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                        cipher.init(
                                Cipher.DECRYPT_MODE,
                                new SecretKeySpec(encryption_key, "AES"),
                                new IvParameterSpec(iv_encrypt_key)
                        );
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            }

            launch = new CacheCreator().initializeCache(mContext, theme_package, cipher);
            return null;
        }

        class KeyRetrieval extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {
                securityIntent = intent;
            }
        }
    }
}
