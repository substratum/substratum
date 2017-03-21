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

package projekt.substratum;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import android.service.quicksettings.Tile;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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
import android.widget.LinearLayout;
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
import com.mikepenz.materialdrawer.model.ExpandableDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

import java.io.File;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;
import projekt.substratum.config.ElevatedCommands;
import projekt.substratum.config.FileOperations;
import projekt.substratum.config.References;
import projekt.substratum.config.ThemeManager;
import projekt.substratum.fragments.PriorityListFragment;
import projekt.substratum.fragments.PriorityLoaderFragment;
import projekt.substratum.fragments.ThemeFragment;
import projekt.substratum.services.FloatUiTile;
import projekt.substratum.services.InterfaceAuthorizationReceiver;
import projekt.substratum.services.SubstratumFloatInterface;
import projekt.substratum.util.Root;
import projekt.substratum.util.SheetDialog;

import static projekt.substratum.config.References.ENABLE_ROOT_CHECK;
import static projekt.substratum.config.References.INTERFACER_PACKAGE;
import static projekt.substratum.config.References.SUBSTRATUM_LOG;
import static projekt.substratum.config.References.checkUsagePermissions;

public class MainActivity extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback, SearchView.OnQueryTextListener {

    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private static final int PERMISSIONS_REQUEST_GET_ACCOUNTS = 2;
    private static final int PERMISSIONS_REQUEST_DRAW_OVER_OTHER_APPS = 3;
    private static final int PERMISSIONS_REQUEST_USAGE_ACCESS_SETTINGS = 4;
    private static final String SELECTED_DRAWER_ITEM = "selected_drawer_item";

    @SuppressLint("StaticFieldLeak")
    public static TextView actionbar_title, actionbar_content;
    public static String userInput = "";
    @SuppressLint("StaticFieldLeak")
    public static SearchView searchView;
    public static MenuItem searchItem;
    private static ActionBar supportActionBar;
    private Drawer drawer;
    private int permissionCheck, permissionCheck2;
    private ProgressDialog mProgressDialog;
    private SharedPreferences prefs;
    private boolean hideBundle, hideRestartUi;
    private BroadcastReceiver authorizationReceiver;

    public static void switchToCustomToolbar(String title, String content) {
        if (supportActionBar != null) supportActionBar.setTitle("");
        if (actionbar_content != null) actionbar_content.setVisibility(View.VISIBLE);
        if (actionbar_title != null) actionbar_title.setVisibility(View.VISIBLE);
        if (actionbar_title != null) actionbar_title.setText(title);
        if (actionbar_content != null) actionbar_content.setText(content);
    }

    public static void switchToStockToolbar(String title) {
        try {
            actionbar_content.setVisibility(View.GONE);
            actionbar_title.setVisibility(View.GONE);
            if (supportActionBar != null) supportActionBar.setTitle(title);
        } catch (NullPointerException npe) {
            // At this point, the activity is closing!
        }
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
        hideRestartUi = !title.equals(getString(R.string.nav_overlay_manager));
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
        hideRestartUi = true;
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
        hideRestartUi = true;
        supportInvalidateOptionsMenu();
    }

    private void printFCMtoken() {
        String token = FirebaseInstanceId.getInstance().getToken();
        Log.d(References.SUBSTRATUM_LOG, "FCM Registration Token: " + token);
    }

    protected RefWatcher installLeakCanary() {
        LeakCanary.enableDisplayLeakActivity(this);
        RefWatcher refWatcher = LeakCanary.refWatcher(this).build();
        getApplication().registerActivityLifecycleCallbacks(
                new Application.ActivityLifecycleCallbacks() {

                    @Override
                    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                    }

                    @Override
                    public void onActivityStarted(Activity activity) {
                    }

                    @Override
                    public void onActivityResumed(Activity activity) {
                    }

                    @Override
                    public void onActivityPaused(Activity activity) {
                    }

                    @Override
                    public void onActivityStopped(Activity activity) {
                    }

                    @Override
                    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                    }

                    @Override
                    public void onActivityDestroyed(Activity activity) {
                        if (activity instanceof MainActivity) {
                            return;
                        } else if (activity instanceof InformationActivity) {
                            return;
                        }
                        refWatcher.watch(activity);
                    }
                });
        return refWatcher;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (BuildConfig.DEBUG) {
            Log.d(SUBSTRATUM_LOG, "Substratum launched with debug mode signatures.");
            if (LeakCanary.isInAnalyzerProcess(this)) return;
            installLeakCanary();
            Log.d(SUBSTRATUM_LOG,
                    "LeakCanary has been initialized to actively monitor memory leaks.");
        }
        setContentView(R.layout.main_activity);

        int selectedDrawer = 1;
        if (savedInstanceState != null) {
            selectedDrawer = savedInstanceState.getInt(SELECTED_DRAWER_ITEM);
        }

        authorizationReceiver = new InterfaceAuthorizationReceiver();
        IntentFilter filter = new IntentFilter(INTERFACER_PACKAGE + ".CALLER_AUTHORIZED");
        getApplicationContext().registerReceiver(authorizationReceiver, filter);

        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        boolean languageCheck = prefs.getBoolean("force_english", false);
        if (languageCheck) {
            References.forceEnglishLocale(getApplicationContext());
        } else {
            References.forceSystemLocale(getApplicationContext());
        }

        actionbar_title = (TextView) findViewById(R.id.activity_title);
        actionbar_content = (TextView) findViewById(R.id.theme_count);

        References.setROMVersion(getApplicationContext(), false);
        References.setAndCheckOMS(getApplicationContext());

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

        // Split the community chats out for easy adapting
        ExpandableDrawerItem social = new ExpandableDrawerItem()
                .withName(R.string.nav_drawer_community)
                .withIcon(R.drawable.nav_drawer_community)
                .withSelectable(false).withSubItems(
                        new SecondaryDrawerItem().withName(R.string.nav_drawer_googleplus)
                                .withLevel(2).withIcon(R.drawable.nav_drawer_googleplus)
                                .withSelectable(false)
                                .withIdentifier(100),
                        new SecondaryDrawerItem().withName(R.string.nav_drawer_telegram)
                                .withLevel(2).withIcon(R.drawable.nav_drawer_telegram)
                                .withSelectable(false)
                                .withIdentifier(101),
                        new SecondaryDrawerItem().withName(R.string.nav_drawer_xda)
                                .withLevel(2).withIcon(R.drawable.nav_drawer_xda)
                                .withSelectable(false)
                                .withIdentifier(102));

        // Split the featured content out for easy adapting
        ExpandableDrawerItem featured = new ExpandableDrawerItem()
                .withName(R.string.nav_drawer_featured)
                .withIcon(R.drawable.nav_drawer_featured)
                .withSelectable(false).withSubItems(
                        new SecondaryDrawerItem().withName(R.string.nav_drawer_tcf)
                                .withLevel(2).withIcon(R.drawable.nav_drawer_tcf)
                                .withSelectable(false)
                                .withIdentifier(103),
                        new SecondaryDrawerItem().withName(R.string.nav_drawer_xda_portal)
                                .withLevel(2).withIcon(R.drawable.nav_drawer_xda_portal)
                                .withSelectable(false)
                                .withIdentifier(104));

        // Split the resources out for easy adapting
        ExpandableDrawerItem resources = new ExpandableDrawerItem()
                .withName(R.string.nav_drawer_resources)
                .withIcon(R.drawable.nav_drawer_resources)
                .withSelectable(false).withSubItems(
                        new SecondaryDrawerItem().withName(R.string.nav_drawer_homepage)
                                .withLevel(2).withIcon(R.drawable.nav_drawer_homepage)
                                .withSelectable(false)
                                .withIdentifier(105),
                        new SecondaryDrawerItem().withName(R.string.nav_drawer_template)
                                .withLevel(2).withIcon(R.drawable.nav_drawer_template)
                                .withSelectable(false)
                                .withIdentifier(106),
                        new SecondaryDrawerItem().withName(R.string.nav_drawer_gerrit)
                                .withLevel(2).withIcon(R.drawable.nav_drawer_gerrit)
                                .withSelectable(false)
                                .withIdentifier(107),
                        new SecondaryDrawerItem().withName(R.string.nav_drawer_github)
                                .withLevel(2).withIcon(R.drawable.nav_drawer_github)
                                .withSelectable(false)
                                .withIdentifier(108));

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
        if (References.checkThemeInterfacer(getApplicationContext()) &&
                BuildConfig.VERSION_NAME.contains("-")) drawerBuilder.addDrawerItems(
                new PrimaryDrawerItem()
                        .withName(R.string.nav_studio)
                        .withIcon(R.drawable.nav_drawer_studio)
                        .withSelectable(false)
                        .withIdentifier(8));
        if (References.checkOMS(getApplicationContext())) drawerBuilder.addDrawerItems(
                new PrimaryDrawerItem()
                        .withName(R.string.nav_priorities)
                        .withIcon(R.drawable.nav_drawer_priorities)
                        .withIdentifier(9));
        drawerBuilder.addDrawerItems(
                new PrimaryDrawerItem()
                        .withName(R.string.nav_backup_restore)
                        .withIcon(R.drawable.nav_drawer_profiles)
                        .withIdentifier(10));
        drawerBuilder.addDrawerItems(
                new PrimaryDrawerItem()
                        .withName(R.string.nav_manage)
                        .withIcon(R.drawable.nav_manage)
                        .withIdentifier(11));
        drawerBuilder.addDrawerItems(
                new SectionDrawerItem()
                        .withName(R.string.nav_section_header_get_involved));
        drawerBuilder.addDrawerItems(social);
        drawerBuilder.addDrawerItems(featured);
        drawerBuilder.addDrawerItems(resources);
        drawerBuilder.addDrawerItems(
                new PrimaryDrawerItem()
                        .withName(R.string.nav_troubleshooting)
                        .withIcon(R.drawable.nav_troubleshooting)
                        .withIdentifier(12));
        drawerBuilder.addDrawerItems(
                new SectionDrawerItem()
                        .withName(R.string.nav_section_header_more));
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
                        Intent intent = new Intent(getApplicationContext(),
                                StudioSelectorActivity.class);
                        startActivity(intent);
                        break;
                    case 9:
                        switchFragment(getString(R.string.nav_priorities),
                                "PriorityLoaderFragment");
                        break;
                    case 10:
                        switchFragment(getString(R.string.nav_backup_restore),
                                "ProfileFragment");
                        break;
                    case 11:
                        switchFragment(getString(R.string.nav_manage),
                                "RecoveryFragment");
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
                    case 100:
                        try {
                            String sourceURL = getString(R.string.googleplus_link);
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(sourceURL));
                            startActivity(i);
                        } catch (Exception e) {
                            Snackbar.make(findViewById(android.R.id.content),
                                    getString(R.string.activity_missing_toast),
                                    Snackbar.LENGTH_LONG)
                                    .show();
                        }
                        break;
                    case 101:
                        try {
                            String sourceURL = getString(R.string.telegram_link);
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(sourceURL));
                            startActivity(i);
                        } catch (Exception e) {
                            Snackbar.make(findViewById(android.R.id.content),
                                    getString(R.string.activity_missing_toast),
                                    Snackbar.LENGTH_LONG)
                                    .show();
                        }
                        break;
                    case 102:
                        try {
                            String sourceURL = getString(R.string.xda_link);
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(sourceURL));
                            startActivity(i);
                        } catch (Exception e) {
                            Snackbar.make(findViewById(android.R.id.content),
                                    getString(R.string.activity_missing_toast),
                                    Snackbar.LENGTH_LONG)
                                    .show();
                        }
                        break;
                    case 103:
                        try {
                            String sourceURL = getString(R.string.tcf_link);
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(sourceURL));
                            startActivity(i);
                        } catch (Exception e) {
                            Snackbar.make(findViewById(android.R.id.content),
                                    getString(R.string.activity_missing_toast),
                                    Snackbar.LENGTH_LONG)
                                    .show();
                        }
                        break;
                    case 104:
                        try {
                            String sourceURL = getString(R.string.xda_portal_link);
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(sourceURL));
                            startActivity(i);
                        } catch (Exception e) {
                            Snackbar.make(findViewById(android.R.id.content),
                                    getString(R.string.activity_missing_toast),
                                    Snackbar.LENGTH_LONG)
                                    .show();
                        }
                        break;
                    case 105:
                        try {
                            String sourceURL = getString(R.string.homepage_link);
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(sourceURL));
                            startActivity(i);
                        } catch (Exception e) {
                            Snackbar.make(findViewById(android.R.id.content),
                                    getString(R.string.activity_missing_toast),
                                    Snackbar.LENGTH_LONG)
                                    .show();
                        }
                        break;
                    case 106:
                        try {
                            String sourceURL = getString(R.string.template_link);
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(sourceURL));
                            startActivity(i);
                        } catch (Exception e) {
                            Snackbar.make(findViewById(android.R.id.content),
                                    getString(R.string.activity_missing_toast),
                                    Snackbar.LENGTH_LONG)
                                    .show();
                        }
                        break;
                    case 107:
                        try {
                            String sourceURL = getString(R.string.gerrit_link);
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(sourceURL));
                            startActivity(i);
                        } catch (Exception e) {
                            Snackbar.make(findViewById(android.R.id.content),
                                    getString(R.string.activity_missing_toast),
                                    Snackbar.LENGTH_LONG)
                                    .show();
                        }
                        break;
                    case 108:
                        try {
                            String sourceURL = getString(R.string.github_link);
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(sourceURL));
                            startActivity(i);
                        } catch (Exception e) {
                            Snackbar.make(findViewById(android.R.id.content),
                                    getString(R.string.activity_missing_toast),
                                    Snackbar.LENGTH_LONG)
                                    .show();
                        }
                        break;
                }
            }
            return false;
        });
        drawer = drawerBuilder.build();
        drawer.setSelection(selectedDrawer, true);

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

                        if (!References.checkROMVersion(getApplicationContext())) {
                            new AlertDialog.Builder(this)
                                    .setTitle(R.string.warning_title)
                                    .setMessage(R.string.dirty_flash_detected)
                                    .setPositiveButton(R.string.dialog_ok, (dialog2, i2) -> {
                                        dialog2.cancel();
                                        prefs.edit().remove("oms_state").apply();
                                        prefs.edit().remove("oms_version").apply();
                                        References.setROMVersion(getApplicationContext(),
                                                true);
                                        References.setAndCheckOMS(getApplicationContext());
                                        this.recreate();
                                    })
                                    .show();
                        }

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

            if (!References.checkROMVersion(getApplicationContext())) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.warning_title)
                        .setMessage(R.string.dirty_flash_detected)
                        .setPositiveButton(R.string.dialog_ok, (dialog2, i2) -> {
                            dialog2.cancel();
                            References.setROMVersion(getApplicationContext(), true);
                            prefs.edit().remove("oms_state").apply();
                            prefs.edit().remove("oms_version").apply();
                            References.setAndCheckOMS(getApplicationContext());
                            this.recreate();
                        })
                        .show();
            }

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
    protected void onDestroy() {
        super.onDestroy();
        try {
            getApplicationContext().unregisterReceiver(authorizationReceiver);
        } catch (IllegalArgumentException e) {
            // Already unregistered
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (permissionCheck == PackageManager.PERMISSION_GRANTED &&
                permissionCheck2 == PackageManager.PERMISSION_GRANTED) {
            //add the values which need to be saved from the drawer to the bundle
            outState = drawer.saveInstanceState(outState);
            outState.putInt(SELECTED_DRAWER_ITEM, (int) drawer.getCurrentSelection());
            super.onSaveInstanceState(outState);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_menu, menu);

        boolean isOMS = References.checkOMS(getApplicationContext());
        if (isOMS) menu.findItem(R.id.reboot_device).setVisible(false);
        if (isOMS) menu.findItem(R.id.soft_reboot).setVisible(false);
        if (!isOMS) menu.findItem(R.id.per_app).setVisible(false);

        searchItem = menu.findItem(R.id.action_search);
        MenuItem showcase = menu.findItem(R.id.search);
        MenuItem restartUi = menu.findItem(R.id.restart_systemui);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(this);
        searchItem.setVisible(!hideBundle);
        showcase.setVisible(!hideBundle);
        restartUi.setVisible(!hideRestartUi && isOMS);
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
                            checkUsagePermissions(getApplicationContext())) {
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
                    } else if (!checkUsagePermissions(getApplicationContext())) {
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
            case R.id.restart_systemui:
                ThemeManager.restartSystemUI(getApplicationContext());
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
            }
            Fragment f = getSupportFragmentManager().findFragmentById(R.id.main);
            if (f instanceof PriorityListFragment) {
                Fragment fragment = new PriorityLoaderFragment();
                FragmentManager fm = getSupportFragmentManager();
                FragmentTransaction transaction = fm.beginTransaction();
                transaction.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim
                        .slide_out_right);
                transaction.replace(R.id.main, fragment);
                transaction.commit();
            } else if (drawer != null && drawer.getCurrentSelectedPosition() > 1) {
                drawer.setSelectionAtPosition(1);
            } else if (drawer != null && drawer.getCurrentSelectedPosition() == 1) {
                this.finish();
            }
        }
    }

    public void showFloatingHead() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());
        prefs.edit().putInt("float_tile", Tile.STATE_ACTIVE).apply();
        FloatUiTile.requestListeningState(getApplicationContext(),
                new ComponentName(getApplicationContext(), FloatUiTile.class));
        getApplicationContext().startService(new Intent(getApplicationContext(),
                SubstratumFloatInterface.class));
    }

    private void hideFloatingHead() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());
        prefs.edit().putInt("float_tile", Tile.STATE_INACTIVE).apply();
        FloatUiTile.requestListeningState(getApplicationContext(),
                new ComponentName(getApplicationContext(), FloatUiTile.class));
        stopService(new Intent(getApplicationContext(),
                SubstratumFloatInterface.class));
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_DRAW_OVER_OTHER_APPS:
                if (!checkUsagePermissions(getApplicationContext())) {
                    Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                    startActivityForResult(intent, PERMISSIONS_REQUEST_USAGE_ACCESS_SETTINGS);
                } else {
                    if (Settings.canDrawOverlays(getApplicationContext()) &&
                            checkUsagePermissions(getApplicationContext())) {
                        showFloatingHead();
                    }
                }
                break;
            case PERMISSIONS_REQUEST_USAGE_ACCESS_SETTINGS:
                if (Settings.canDrawOverlays(getApplicationContext()) &&
                        checkUsagePermissions(getApplicationContext())) {
                    showFloatingHead();
                }
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
                        showOutdatedRequestDialog();
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
                    showOutdatedRequestDialog();
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

    public void showOutdatedRequestDialog() {
        boolean show_outdated_themes = prefs.contains("display_old_themes");
        if (!show_outdated_themes) {
            SheetDialog sheetDialog = new SheetDialog(this);
            @SuppressLint("InflateParams")
            View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_dialog, null);
            LinearLayout hide = (LinearLayout) sheetView.findViewById(R.id.hide_outdated_themes);
            LinearLayout show = (LinearLayout) sheetView.findViewById(R.id.show_outdated_themes);
            hide.setOnClickListener(v -> {
                prefs.edit().putBoolean("display_old_themes", false).apply();
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            });
            show.setOnClickListener(v -> {
                prefs.edit().putBoolean("display_old_themes", true).apply();
                sheetDialog.hide();
            });
            sheetDialog.setCanceledOnTouchOutside(false);
            sheetDialog.setContentView(sheetView);
            sheetDialog.show();
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    private class RootRequester extends AsyncTask<String, Integer, Boolean> {

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result && ENABLE_ROOT_CHECK &&
                    !References.checkThemeInterfacer(getApplicationContext())) {
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
                showOutdatedRequestDialog();
            }
            super.onPostExecute(result);
        }

        @Override
        protected Boolean doInBackground(String... sUrl) {
            prefs.edit().putBoolean("complexion",
                    !References.spreadYourWingsAndFly(getApplicationContext())).apply();
            if (!References.checkThemeInterfacer(getApplicationContext())) {
                Boolean receivedRoot = Root.requestRootAccess();
                if (receivedRoot) Log.d(SUBSTRATUM_LOG, "Substratum has loaded in rooted mode.");
                References.injectRescueArchives(getApplicationContext());
                return receivedRoot;
            } else {
                Log.d(SUBSTRATUM_LOG, "Substratum has loaded in rootless mode.");
                return false;
            }
        }
    }
}