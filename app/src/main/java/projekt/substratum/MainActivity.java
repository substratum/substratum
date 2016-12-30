package projekt.substratum;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.aboutlibraries.ui.LibsSupportFragment;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.holder.DimenHolder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;
import projekt.substrate.LetsGetStarted;
import projekt.substratum.config.References;
import projekt.substratum.fragments.ThemeFragment;
import projekt.substratum.services.ThemeService;
import projekt.substratum.util.Root;

public class MainActivity extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private static final int PERMISSIONS_REQUEST_GET_ACCOUNTS = 2;
    private Drawer drawer;
    private int permissionCheck, permissionCheck2;
    private ProgressDialog mProgressDialog;
    private SharedPreferences prefs;

    private void switchFragment(String title, String fragment) {
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(title);
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        tx.replace(R.id.main, Fragment.instantiate(MainActivity.this, "projekt.substratum" +
                ".fragments." + fragment));
        tx.commit();
    }

    private void switchThemeFragment(String title, String home_type) {
        Fragment fragment = new ThemeFragment();
        Bundle bundle = new Bundle();
        bundle.putString("home_type", home_type);
        fragment.setArguments(bundle);

        if (getSupportActionBar() != null) getSupportActionBar().setTitle(title);
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        tx.replace(R.id.main, fragment);
        tx.commit();
    }

    private void switchFragmentToLicenses(String title, LibsSupportFragment fragment) {
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(title);
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        tx.replace(R.id.main, fragment);
        tx.commit();
    }

    private void printFCMtoken() {
        String token = FirebaseInstanceId.getInstance().getToken();
        Log.d(References.SUBSTRATUM_LOG, "FCM Registration Token: " + token);
    }

    private boolean copyRescueFile(Context context, String sourceFileName, String destFileName) {
        AssetManager assetManager = context.getAssets();

        File destFile = new File(destFileName);
        File destParentDir = destFile.getParentFile();
        if (!destParentDir.exists()) {
            Boolean made = destParentDir.mkdir();
            if (!made) Log.e(References.SUBSTRATUM_LOG,
                    "Unable to create directories for rescue archive dumps.");
        }

        InputStream in;
        OutputStream out;
        try {
            in = assetManager.open(sourceFileName);
            out = new FileOutputStream(destFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.flush();
            out.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());

        References.setAndCheckOMS(getApplicationContext());
        startService(new Intent(this, ThemeService.class));

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setHomeButtonEnabled(false);
            }
        }

        AccountHeader header = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.material_drawer_header_background)
                .withProfileImagesVisible(false)
                .withSelectionListEnabledForSingleProfile(false)
                .addProfiles(
                        new ProfileDrawerItem().withName(getString(R.string.drawer_name)).withEmail
                                (BuildConfig.VERSION_NAME))
                .withCurrentProfileHiddenInList(true)
                .build();

        final LibsSupportFragment fragment = new LibsBuilder().supportFragment();

        DrawerBuilder drawerBuilder = new DrawerBuilder();
        drawerBuilder.withActivity(this);

        if (toolbar != null) drawerBuilder.withToolbar(toolbar);
        drawerBuilder.withSavedInstance(savedInstanceState);
        drawerBuilder.withActionBarDrawerToggleAnimated(true);
        if (prefs.getBoolean("alternate_drawer_design", false)) {
            drawerBuilder.withRootView(R.id.drawer_container);
            drawerBuilder.withHeaderHeight(DimenHolder.fromDp(0));
        }
        drawerBuilder.withAccountHeader(header);
        Boolean fonts_allowed = false;
        try {
            Class<?> cls = Class.forName("android.graphics.Typeface");
            cls.getDeclaredMethod("getSystemFontDirLocation");
            cls.getDeclaredMethod("getThemeFontConfigLocation");
            cls.getDeclaredMethod("getThemeFontDirLocation");
            Log.d(References.SUBSTRATUM_LOG, "This system fully supports font hotswapping.");
            fonts_allowed = true;
        } catch (Exception ex) {
            // Suppress Fonts
        }

        // Begin initializing the navigation drawer
        drawerBuilder.addDrawerItems(
                new PrimaryDrawerItem()
                        .withName(R.string.nav_home)
                        .withIcon(R.drawable.nav_theme_packs)
                        .withIdentifier(1));
        drawerBuilder.addDrawerItems(
                new PrimaryDrawerItem()
                        .withName(R.string.nav_overlays)
                        .withIcon(R.drawable.nav_overlays)
                        .withIdentifier(2));
        drawerBuilder.addDrawerItems(
                new PrimaryDrawerItem()
                        .withName(R.string.nav_bootanim)
                        .withIcon(R.drawable.nav_bootanim)
                        .withIdentifier(3));
        if (fonts_allowed) drawerBuilder.addDrawerItems(
                new PrimaryDrawerItem()
                        .withName(R.string.nav_fonts)
                        .withIcon(R.drawable.nav_fonts)
                        .withIdentifier(4));
        drawerBuilder.addDrawerItems(
                new PrimaryDrawerItem()
                        .withName(R.string.nav_sounds)
                        .withIcon(R.drawable.nav_sounds)
                        .withIdentifier(5));
        drawerBuilder.addDrawerItems(
                new PrimaryDrawerItem()
                        .withName(R.string.nav_wallpapers)
                        .withIcon(R.drawable.nav_wallpapers)
                        .withIdentifier(6));
        drawerBuilder.addDrawerItems(
                new SectionDrawerItem()
                        .withName(R.string.nav_section_header_utilities));
        drawerBuilder.addDrawerItems(
                new PrimaryDrawerItem()
                        .withName(R.string.nav_overlay_manager)
                        .withIcon(R.drawable.nav_overlay_manager)
                        .withIdentifier(7));
        drawerBuilder.addDrawerItems(
                new PrimaryDrawerItem()
                        .withName(R.string.nav_manage)
                        .withIcon(R.drawable.nav_manage)
                        .withIdentifier(8));
        if (References.checkMasquerade(getApplicationContext()) >= 20 &&
                BuildConfig.VERSION_NAME.contains("-")) drawerBuilder.addDrawerItems(
                new PrimaryDrawerItem()
                        .withName(R.string.nav_studio)
                        .withIcon(R.drawable.nav_drawer_studio)
                        .withSelectable(false)
                        .withIdentifier(9));
        if (References.checkOMS(getApplicationContext())) drawerBuilder.addDrawerItems(
                new PrimaryDrawerItem()
                        .withName(R.string.nav_priorities)
                        .withIcon(R.drawable.nav_drawer_priorities)
                        .withIdentifier(10));
        if (BuildConfig.VERSION_NAME.contains("-")) {
            drawerBuilder.addDrawerItems(
                    new PrimaryDrawerItem()
                            .withName(R.string.nav_backup_restore)
                            .withIcon(R.drawable.nav_drawer_profiles)
                            .withIdentifier(11));
        }
        drawerBuilder.addDrawerItems(
                new SectionDrawerItem()
                        .withName(R.string.nav_section_header_more));
        drawerBuilder.addDrawerItems(
                new SecondaryDrawerItem()
                        .withName(R.string.nav_troubleshooting)
                        .withIcon(R.drawable.nav_troubleshooting)
                        .withIdentifier(12));
        drawerBuilder.addDrawerItems(
                new SecondaryDrawerItem()
                        .withName(R.string.nav_team)
                        .withIcon(R.drawable.nav_drawer_team)
                        .withIdentifier(13));
        drawerBuilder.addDrawerItems(
                new SecondaryDrawerItem()
                        .withName(getString(R.string.nav_opensource))
                        .withIcon(R.drawable.nav_drawer_licenses)
                        .withIdentifier(14));
        drawerBuilder.addDrawerItems(
                new SecondaryDrawerItem()
                        .withName(R.string.nav_settings)
                        .withIcon(R.drawable.nav_drawer_settings)
                        .withIdentifier(15));
        drawerBuilder.withOnDrawerItemClickListener((view, position, drawerItem) -> {
            if (drawerItem != null) {
                switch ((int) drawerItem.getIdentifier()) {
                    case 1:
                        switchThemeFragment(((References.checkOMS(
                                getApplicationContext())) ?
                                        getString(R.string.app_name) :
                                        getString(R.string.legacy_app_name)),
                                References.homeFragment);
                        break;
                    case 2:
                        switchThemeFragment(getString(R.string.nav_overlays),
                                References.overlaysFragment);
                        break;
                    case 3:
                        switchThemeFragment(getString(R.string.nav_bootanim),
                                References.bootAnimationsFragment);
                        break;
                    case 4:
                        switchThemeFragment(getString(R.string.nav_fonts),
                                References.fontsFragment);
                        break;
                    case 5:
                        switchThemeFragment(getString(R.string.nav_sounds),
                                References.soundsFragment);
                        break;
                    case 6:
                        switchThemeFragment(getString(R.string.nav_wallpapers),
                                References.wallpaperFragment);
                        break;
                    case 7:
                        switchFragment(getString(R.string.nav_overlay_manager),
                                "AdvancedManagerFragment");
                        break;
                    case 8:
                        switchFragment(getString(R.string.nav_manage),
                                "ManageFragment");
                        break;
                    case 9:
                        Intent intent = new Intent(getApplicationContext(),
                                StudioSelectorActivity.class);
                        startActivity(intent);
                        break;
                    case 10:
                        switchFragment(getString(R.string.nav_priorities),
                                "PriorityLoaderFragment");
                        break;
                    case 11:
                        switchFragment(getString(R.string.nav_backup_restore),
                                "ProfileFragment");
                        break;
                    case 12:
                        switchFragment(getString(R.string.nav_troubleshooting),
                                "TroubleshootingFragment");
                        break;
                    case 13:
                        switchFragment(getString(R.string.nav_team),
                                "TeamFragment");
                        break;
                    case 14:
                        switchFragmentToLicenses(getString(R.string.nav_opensource),
                                fragment);
                        break;
                    case 15:
                        switchFragment(getString(R.string.nav_settings),
                                "SettingsFragment");
                        break;
                }
            }
            return false;
        });
        drawerBuilder.withSelectedItem(1);
        drawerBuilder.withSelectedItemByPosition(1);
        drawer = drawerBuilder.build();

        if (prefs.getBoolean("permissions_ungranted", true)) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.permission_explanation_title)
                    .setMessage(R.string.permission_explanation_text)
                    .setPositiveButton(R.string.accept, (dialog, i) -> {
                        dialog.cancel();

                        permissionCheck = ContextCompat.checkSelfPermission(
                                getApplicationContext(),
                                Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        permissionCheck2 = ContextCompat.checkSelfPermission(
                                getApplicationContext(),
                                Manifest.permission.GET_ACCOUNTS);

                        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                            // permission already granted, allow the program to continue running
                            File directory = new File(Environment.getExternalStorageDirectory(),
                                    "/.substratum/");
                            if (!directory.exists()) {
                                Boolean made = directory.mkdirs();
                                if (!made) Log.e(References.SUBSTRATUM_LOG,
                                        "Unable to create directory");
                            }
                            File cacheDirectory = new File(getCacheDir(),
                                    "/SubstratumBuilder/");
                            if (!cacheDirectory.exists()) {
                                Boolean made = cacheDirectory.mkdirs();
                                if (!made) Log.e(References.SUBSTRATUM_LOG,
                                        "Unable to create cache directory");
                            }
                            File rescueFile = new File(
                                    Environment.getExternalStorageDirectory() +
                                            File.separator + "substratum" +
                                            File.separator + "SubstratumRescue.zip");
                            File rescueFileLegacy = new File(
                                    Environment.getExternalStorageDirectory() +
                                            File.separator + "substratum" +
                                            File.separator + "SubstratumRescue_Legacy.zip");
                            if (!rescueFile.exists()) {
                                copyRescueFile(getApplicationContext(), "rescue.dat",
                                        Environment.getExternalStorageDirectory() +
                                                java.io.File.separator + "substratum" +
                                                java.io.File.separator + "SubstratumRescue.zip");
                            }
                            if (!rescueFileLegacy.exists()) {
                                copyRescueFile(getApplicationContext(), "rescue_legacy.dat",
                                        Environment.getExternalStorageDirectory() +
                                                java.io.File.separator + "substratum" +
                                                java.io.File.separator +
                                                "SubstratumRescue_Legacy.zip");
                            }
                            if (permissionCheck2 == PackageManager.PERMISSION_GRANTED) {
                                // permission already granted, allow the program to continue
                                // Set the first option to start at app boot
                                if (!prefs.contains("permissions_ungranted")) {
                                    prefs.edit()
                                            .putBoolean("permissions_ungranted", false).apply();
                                }
                                drawer.setSelectionAtPosition(1);
                                mProgressDialog = new ProgressDialog(this,
                                        R.style.SubstratumBuilder_BlurView);
                                new RootRequester().execute("");
                            } else {
                                ActivityCompat.requestPermissions(this,
                                        new String[]{Manifest.permission.GET_ACCOUNTS},
                                        PERMISSIONS_REQUEST_GET_ACCOUNTS);
                            }
                        } else {
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                        }
                        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
                        printFCMtoken();

                        if (!References.checkOMS(getApplicationContext()) &&
                                !prefs.contains("legacy_dismissal")) {
                            new AlertDialog.Builder(this)
                                    .setTitle(R.string.warning_title)
                                    .setMessage(R.string.legacy_warning_content)
                                    .setPositiveButton(R.string.dialog_ok, (dialog2, i2) ->
                                            dialog2.cancel())
                                    .setNeutralButton(R.string.dialog_do_not_show_again,
                                            (dialog3, i3) -> {
                                                prefs.edit().putBoolean(
                                                        "legacy_dismissal", true).apply();
                                                dialog3.cancel();
                                            })
                                    .show();
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !References.checkOMS(
                                getApplicationContext()) && References.isIncompatibleFirmware()) {
                            new AlertDialog.Builder(this)
                                    .setTitle(R.string.warning_title)
                                    .setMessage(R.string.dangerous_warning_content)
                                    .setPositiveButton(R.string.dialog_ok, (dialog4, which4) ->
                                            dialog4.cancel())
                                    .show();
                        }
                    })
                    .setNegativeButton(R.string.deny,
                            (dialog, i) -> {
                                dialog.cancel();
                                this.finish();
                            })
                    .show();
        } else {
            drawer.setSelectionAtPosition(1);
            try {
                FirebaseDatabase.getInstance().setPersistenceEnabled(true);
            } catch (RuntimeException re1) {
                try {
                    FirebaseDatabase.getInstance().setPersistenceEnabled(true);
                } catch (RuntimeException re2) {
                    // Suppress warning
                }
            }
            printFCMtoken();

            if (!References.checkOMS(getApplicationContext()) &&
                    !prefs.contains("legacy_dismissal")) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.warning_title)
                        .setMessage(R.string.legacy_warning_content)
                        .setPositiveButton(R.string.dialog_ok, (dialog, i) -> dialog.cancel())
                        .setNeutralButton(R.string.dialog_do_not_show_again,
                                (dialog, i) -> {
                                    prefs.edit().putBoolean("legacy_dismissal", true).apply();
                                    dialog.cancel();
                                })
                        .show();
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !References.checkOMS(
                    getApplicationContext()) && References.isIncompatibleFirmware()) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.warning_title)
                        .setMessage(R.string.dangerous_warning_content)
                        .setPositiveButton(R.string.dialog_ok, (dialog, which) -> dialog.cancel())
                        .show();
            }

            mProgressDialog = new ProgressDialog(this, R.style.SubstratumBuilder_BlurView);
            new RootRequester().execute("");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (permissionCheck == PackageManager.PERMISSION_GRANTED &&
                permissionCheck2 == PackageManager.PERMISSION_GRANTED) {
            //add the values which need to be saved from the drawer to the bundle
            outState = drawer.saveInstanceState(outState);
            super.onSaveInstanceState(outState);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (References.checkOMS(getApplicationContext())) {
            getMenuInflater().inflate(R.menu.activity_menu, menu);
        } else {
            getMenuInflater().inflate(R.menu.activity_menu_legacy, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(
                "substratum_state", Context.MODE_PRIVATE);
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.refresh:
                Fragment f = getSupportFragmentManager().findFragmentById(R.id.main);
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                if (f != null)
                    ft.detach(f).attach(f).commit();
                return true;
            case R.id.search:
                Intent intent = new Intent(this, ShowcaseActivity.class);
                startActivity(intent);
                return true;

            // Begin OMS based options
            case R.id.restart_systemui:
                prefs.edit().clear().apply();
                References.restartSystemUI();
                return true;

            // Begin RRO based options
            case R.id.reboot_device:
                prefs.edit().clear().apply();
                References.reboot();
                return true;
            case R.id.soft_reboot:
                prefs.edit().clear().apply();
                References.softReboot();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawer != null && drawer.isDrawerOpen()) {
            drawer.closeDrawer();
        } else if (drawer != null && drawer.getCurrentSelectedPosition() > 1) {
            drawer.setSelectionAtPosition(1);
        } else if (drawer != null && drawer.getCurrentSelectedPosition() == 1) {
            this.finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission already granted, allow the program to continue running
                    File directory = new File(Environment.getExternalStorageDirectory(),
                            "/.substratum/");
                    if (!directory.exists()) {
                        Boolean made = directory.mkdirs();
                        if (!made) Log.e(References.SUBSTRATUM_LOG,
                                "Could not make internal substratum directory.");
                    }
                    File cacheDirectory = new File(getCacheDir(),
                            "/SubstratumBuilder/");
                    if (!cacheDirectory.exists()) {
                        Boolean made = cacheDirectory.mkdirs();
                        if (!made)
                            Log.e(References.SUBSTRATUM_LOG, "Could not create cache directory.");
                    }
                    File[] fileList = new File(getCacheDir().getAbsolutePath() +
                            "/SubstratumBuilder/").listFiles();
                    for (File file : fileList) {
                        References.delete(getCacheDir().getAbsolutePath() +
                                "/SubstratumBuilder/" + file.getName());
                    }
                    Log.d("SubstratumBuilder", "The cache has been flushed!");
                    if (permissionCheck2 == PackageManager.PERMISSION_GRANTED) {
                        // permission already granted, allow the program to continue running
                        // Set the first option to start at app boot
                        if (!prefs.contains("permissions_ungranted")) {
                            prefs.edit().putBoolean("permissions_ungranted", false).apply();
                        }
                        drawer.setSelectionAtPosition(1);
                        if (References.spreadYourWingsAndFly(getApplicationContext())) {
                            LetsGetStarted.kissMe();
                        }
                    } else {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.GET_ACCOUNTS},
                                PERMISSIONS_REQUEST_GET_ACCOUNTS);
                    }
                } else {
                    // permission was not granted, show closing dialog
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.permission_not_granted_dialog_title)
                            .setMessage(R.string.permission_not_granted_dialog_message1)
                            .setPositiveButton(R.string.dialog_ok, (dialog, which) -> MainActivity.this.finish())
                            .show();
                    return;
                }
                break;
            }
            case PERMISSIONS_REQUEST_GET_ACCOUNTS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission already granted, allow the program to continue running
                    // Set the first option to start at app boot
                    if (!prefs.contains("permissions_ungranted")) {
                        prefs.edit().putBoolean("permissions_ungranted", false).apply();
                    }
                    drawer.setSelectionAtPosition(1);
                } else {
                    // permission was not granted, show closing dialog
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.permission_not_granted_dialog_title)
                            .setMessage(R.string.permission_not_granted_dialog_message3)
                            .setPositiveButton(R.string.dialog_ok, (dialog, which) ->
                                    MainActivity.this.finish())
                            .show();
                    return;
                }
                break;
            }
        }
    }

    private class RootRequester extends AsyncTask<String, Integer, Boolean> {

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();
                mProgressDialog.setContentView(R.layout.root_rejected_loader);

                final float radius = 5;
                final View decorView = getWindow().getDecorView();
                final View rootView = decorView.findViewById(android.R.id.content);
                final Drawable windowBackground = decorView.getBackground();

                BlurView blurView = (BlurView) mProgressDialog.findViewById(R.id.blurView);

                blurView.setupWith(rootView)
                        .windowBackground(windowBackground)
                        .blurAlgorithm(new RenderScriptBlur(getApplicationContext(), true))
                        .blurRadius(radius);
                final TextView textView = (TextView) mProgressDialog.findViewById(R.id.timer);
                if (References.isPackageInstalled(
                        getApplicationContext(), "eu.chainfire.supersu")) {
                    CountDownTimer Count = new CountDownTimer(5000, 1000) {
                        public void onTick(long millisUntilFinished) {
                            if ((millisUntilFinished / 1000) > 1) {
                                textView.setText(String.format(
                                        getString(R.string.root_rejected_timer_plural),
                                        (millisUntilFinished / 1000) + ""));
                            } else {
                                textView.setText(String.format(
                                        getString(R.string.root_rejected_timer_singular),
                                        (millisUntilFinished / 1000) + ""));
                            }
                        }

                        public void onFinish() {
                            mProgressDialog.dismiss();
                            finish();
                        }
                    };
                    Count.start();
                } else {
                    textView.setText(getString(R.string.root_rejected_text_cm_phh));
                }
            } else {
                if (References.spreadYourWingsAndFly(getApplicationContext())) {
                    LetsGetStarted.kissMe();
                }
            }
            super.onPostExecute(result);
        }

        @Override
        protected Boolean doInBackground(String... sUrl) {
            Boolean receivedRoot = Root.requestRootAccess();
            if (receivedRoot) {
                if (References.checkOMS(getApplicationContext())) {
                    if (!prefs.getBoolean("substratum_oms", true)) {
                        if (!new File(Environment.getExternalStorageDirectory()
                                .getAbsolutePath() + "/.substratum/").exists()) {
                            References.delete(Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() + "/.substratum/");
                        }
                        if (!new File(Environment.getExternalStorageDirectory()
                                .getAbsolutePath() + "/substratum/").exists()) {
                            References.delete(Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() + "/substratum/");
                        }
                        File directory = new File(Environment.getExternalStorageDirectory(),
                                "/.substratum/");
                        if (!directory.exists()) {
                            Boolean made = directory.mkdirs();
                            if (!made) Log.e(References.SUBSTRATUM_LOG,
                                    "Could not make substratum directory on internal storage.");
                        }
                    }
                }
            }
            return receivedRoot;
        }
    }
}