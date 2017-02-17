package projekt.substratum;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.aboutlibraries.LibsConfiguration;
import com.mikepenz.aboutlibraries.ui.LibsSupportFragment;
import com.mikepenz.itemanimators.SlideDownAlphaAnimator;
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

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;
import projekt.substratum.config.ElevatedCommands;
import projekt.substratum.config.FileOperations;
import projekt.substratum.config.References;
import projekt.substratum.fragments.ThemeFragment;
import projekt.substratum.services.SubstratumFloatInterface;
import projekt.substratum.services.ThemeService;
import projekt.substratum.util.Root;

import static projekt.substratum.config.References.ENABLE_ROOT_CHECK;
import static projekt.substratum.config.References.SUBSTRATUM_LOG;

public class MainActivity extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback, SearchView.OnQueryTextListener {

    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private static final int PERMISSIONS_REQUEST_GET_ACCOUNTS = 2;
    private static final int PERMISSIONS_REQUEST_DRAW_OVER_OTHER_APPS = 3;
    private static final int PERMISSIONS_REQUEST_USAGE_ACCESS_SETTINGS = 4;

    @SuppressLint("StaticFieldLeak")
    public static TextView actionbar_title, actionbar_content;
    public static String userInput = "";
    @SuppressLint("StaticFieldLeak")
    public static SearchView searchView;
    private static ActionBar supportActionBar;
    private Drawer drawer;
    private int permissionCheck, permissionCheck2;
    private ProgressDialog mProgressDialog;
    private SharedPreferences prefs;
    private boolean hideBundle;

    public static void switchToCustomToolbar(String title, String content) {
        if (supportActionBar != null) supportActionBar.setTitle("");
        actionbar_content.setVisibility(View.VISIBLE);
        actionbar_title.setVisibility(View.VISIBLE);
        actionbar_title.setText(title);
        actionbar_content.setText(content);
    }

    public static void switchToStockToolbar(String title) {
        actionbar_content.setVisibility(View.GONE);
        actionbar_title.setVisibility(View.GONE);
        if (supportActionBar != null) supportActionBar.setTitle(title);
    }

    private void switchFragment(String title, String fragment) {
        if (searchView != null) {
            if (!searchView.isIconified()) {
                searchView.setIconified(true);
            }
        }
        switchToStockToolbar(title);
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        tx.replace(R.id.main, Fragment.instantiate(MainActivity.this, "projekt.substratum" +
                ".fragments." + fragment));
        tx.commit();
        hideBundle = true;
        supportInvalidateOptionsMenu();
    }

    private void switchThemeFragment(String title, String home_type) {
        if (searchView != null && !searchView.isIconified()) {
            searchView.setIconified(true);
        }
        Fragment fragment = new ThemeFragment();
        Bundle bundle = new Bundle();
        bundle.putString("home_type", home_type);
        bundle.putString("title", title);
        fragment.setArguments(bundle);

        switchToStockToolbar(title);
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        tx.replace(R.id.main, fragment);
        tx.commit();
        hideBundle = false;
        supportInvalidateOptionsMenu();
    }

    private void switchFragmentToLicenses(String title, LibsSupportFragment fragment) {
        if (searchView != null && !searchView.isIconified()) {
            searchView.setIconified(true);
        }
        switchToStockToolbar(title);
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        tx.replace(R.id.main, fragment);
        tx.commit();
        hideBundle = true;
        supportInvalidateOptionsMenu();
    }

    private void printFCMtoken() {
        String token = FirebaseInstanceId.getInstance().getToken();
        Log.d(References.SUBSTRATUM_LOG, "FCM Registration Token: " + token);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());

        boolean languageCheck = prefs.getBoolean("force_english", false);
        if (languageCheck) {
            References.forceEnglishLocale(getApplicationContext());
        } else {
            References.forceSystemLocale(getApplicationContext());
        }

        actionbar_title = (TextView) findViewById(R.id.activity_title);
        actionbar_content = (TextView) findViewById(R.id.theme_count);

        References.setAndCheckOMS(getApplicationContext());
        startService(new Intent(this, ThemeService.class));

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setHomeButtonEnabled(false);
                getSupportActionBar().setTitle("");
            }
        }
        supportActionBar = getSupportActionBar();
        switchToStockToolbar(getString(R.string.app_name));

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

        LibsConfiguration.getInstance().setItemAnimator(new SlideDownAlphaAnimator());
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
        drawerBuilder.addDrawerItems(
                new PrimaryDrawerItem()
                        .withName(R.string.nav_backup_restore)
                        .withIcon(R.drawable.nav_drawer_profiles)
                        .withIdentifier(11));
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

        permissionCheck = ContextCompat.checkSelfPermission(
                getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissionCheck2 = ContextCompat.checkSelfPermission(
                getApplicationContext(),
                Manifest.permission.GET_ACCOUNTS);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED ||
                permissionCheck2 != PackageManager.PERMISSION_GRANTED) {
            prefs.edit().remove("permissions_ungranted").apply();
        }

        if (prefs.getBoolean("permissions_ungranted", true)) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.permission_explanation_title)
                    .setMessage(R.string.permission_explanation_text)
                    .setPositiveButton(R.string.accept, (dialog, i) -> {
                        dialog.cancel();

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
                            References.injectRescueArchives(getApplicationContext());
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
                        new RootRequester().execute("");
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
        MenuItem searchItem = menu.findItem(R.id.action_search);
        MenuItem showcase = menu.findItem(R.id.search);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(this);
        searchItem.setVisible(!hideBundle);
        showcase.setVisible(!hideBundle);
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
            case R.id.search:
                Intent intent = new Intent(this, ShowcaseActivity.class);
                startActivity(intent);
                return true;

            // Begin OMS based options
            case R.id.per_app:
                if (!References.isServiceRunning(SubstratumFloatInterface.class,
                        getApplicationContext())) {
                    if (Settings.canDrawOverlays(getApplicationContext()) &&
                            checkUsagePermissions()) {
                        showFloatingHead();
                    } else if (!Settings.canDrawOverlays(getApplicationContext())) {
                        Intent draw_over_apps = new Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getApplicationContext()
                                        .getPackageName()));
                        startActivityForResult(draw_over_apps,
                                PERMISSIONS_REQUEST_DRAW_OVER_OTHER_APPS);
                        Toast toast = Toast.makeText(
                                getApplicationContext(),
                                getString(R.string.per_app_draw_over_other_apps_request),
                                Toast.LENGTH_LONG);
                        toast.show();
                    } else if (!checkUsagePermissions()) {
                        Intent usage = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                        startActivityForResult(usage,
                                PERMISSIONS_REQUEST_USAGE_ACCESS_SETTINGS);
                        Toast toast = Toast.makeText(
                                getApplicationContext(),
                                getString(R.string.per_app_usage_stats_request),
                                Toast.LENGTH_LONG);
                        toast.show();
                    }
                } else {
                    hideFloatingHead();
                }
                return true;

            // Begin RRO based options
            case R.id.reboot_device:
                prefs.edit().clear().apply();
                ElevatedCommands.reboot();
                return true;
            case R.id.soft_reboot:
                prefs.edit().clear().apply();
                ElevatedCommands.softReboot();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (!searchView.isIconified()) {
            searchView.setIconified(true);
        } else {
            if (drawer != null && drawer.isDrawerOpen()) {
                drawer.closeDrawer();
            } else if (drawer != null && drawer.getCurrentSelectedPosition() > 1) {
                drawer.setSelectionAtPosition(1);
            } else if (drawer != null && drawer.getCurrentSelectedPosition() == 1) {
                this.finish();
            }
        }
    }

    public void showFloatingHead() {
        Toast.makeText(
                getApplicationContext(),
                getString(R.string.per_app_introduced),
                Toast.LENGTH_LONG).show();
        getApplicationContext().startService(new Intent(getApplicationContext(),
                SubstratumFloatInterface.class));
    }

    private void hideFloatingHead() {
        Toast.makeText(
                getApplicationContext(),
                getString(R.string.per_app_removed),
                Toast.LENGTH_LONG).show();
        stopService(new Intent(getApplicationContext(),
                SubstratumFloatInterface.class));
    }

    private boolean checkUsagePermissions() {
        try {
            PackageManager packageManager = getApplicationContext().getPackageManager();
            ApplicationInfo applicationInfo =
                    packageManager.getApplicationInfo(getApplicationContext().getPackageName(), 0);
            AppOpsManager appOpsManager = (AppOpsManager)
                    getApplicationContext().getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOpsManager.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    applicationInfo.uid,
                    applicationInfo.packageName);
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_DRAW_OVER_OTHER_APPS:
                if (!checkUsagePermissions()) {
                    Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                    startActivityForResult(intent, PERMISSIONS_REQUEST_USAGE_ACCESS_SETTINGS);
                } else {
                    showFloatingHead();
                }
                break;
            case PERMISSIONS_REQUEST_USAGE_ACCESS_SETTINGS:
                showFloatingHead();
                break;
            default:
                break;
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
                                "Unable to create directory");
                    }
                    File cacheDirectory = new File(getCacheDir(),
                            "/SubstratumBuilder/");
                    if (!cacheDirectory.exists()) {
                        Boolean made = cacheDirectory.mkdirs();
                        if (!made) Log.e(References.SUBSTRATUM_LOG,
                                "Unable to create cache directory");
                    }
                    References.injectRescueArchives(getApplicationContext());
                    File[] fileList = new File(getCacheDir().getAbsolutePath() +
                            "/SubstratumBuilder/").listFiles();
                    for (File file : fileList) {
                        FileOperations.delete(getApplicationContext(), getCacheDir()
                                .getAbsolutePath() +
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
                            .setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
                                if (shouldShowRequestPermissionRationale(Manifest.permission.
                                        WRITE_EXTERNAL_STORAGE)) {
                                    MainActivity.this.finish();
                                } else {
                                    // User choose not to show request again
                                    final Intent i = new Intent();
                                    i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    i.addCategory(Intent.CATEGORY_DEFAULT);
                                    i.setData(Uri.parse("package:" + this.getPackageName()));
                                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                    i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                                    this.startActivity(i);
                                    this.finish();
                                }
                            })
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
                            .setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
                                if (shouldShowRequestPermissionRationale(Manifest.permission.
                                        GET_ACCOUNTS)) {
                                    MainActivity.this.finish();
                                } else {
                                    // User choose not to show request again
                                    final Intent i = new Intent();
                                    i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    i.addCategory(Intent.CATEGORY_DEFAULT);
                                    i.setData(Uri.parse("package:" + this.getPackageName()));
                                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                    i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                                    this.startActivity(i);
                                    this.finish();
                                }
                            })
                            .show();
                    return;
                }
                break;
            }
        }
    }

    @Override
    public boolean onQueryTextChange(String query) {
        if (!userInput.equals(query)) {
            userInput = query;
            Fragment f = getSupportFragmentManager().findFragmentById(R.id.main);
            getSupportFragmentManager()
                    .beginTransaction()
                    .detach(f)
                    .commitNowAllowingStateLoss();
            getSupportFragmentManager()
                    .beginTransaction()
                    .attach(f)
                    .commitAllowingStateLoss();
        }
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    private class RootRequester extends AsyncTask<String, Integer, Boolean> {

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result && ENABLE_ROOT_CHECK &&
                    !References.checkMasqueradeJobService(getApplicationContext())) {
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
            }
            super.onPostExecute(result);
        }

        @Override
        protected Boolean doInBackground(String... sUrl) {
            if (!References.checkMasqueradeJobService(getApplicationContext())) {
                Boolean receivedRoot = Root.requestRootAccess();
                if (receivedRoot) Log.d(SUBSTRATUM_LOG, "Substratum has loaded in rooted mode.");
                References.injectRescueArchives(getApplicationContext());
                return receivedRoot;
            } else {
                Log.d(SUBSTRATUM_LOG, "Substratum has loaded in rootless masquerade mode.");
                return false;
            }
        }
    }
}