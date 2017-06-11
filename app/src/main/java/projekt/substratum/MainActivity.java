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
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.support.design.widget.Lunchbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.aboutlibraries.ui.LibsSupportFragment;
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
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;
import projekt.substratum.activities.base.SubstratumActivity;
import projekt.substratum.activities.showcase.ShowcaseActivity;
import projekt.substratum.activities.studio.StudioSelectorActivity;
import projekt.substratum.common.References;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.fragments.ManagerFragment;
import projekt.substratum.fragments.PriorityListFragment;
import projekt.substratum.fragments.PriorityLoaderFragment;
import projekt.substratum.fragments.ProfileFragment;
import projekt.substratum.fragments.RecoveryFragment;
import projekt.substratum.fragments.SettingsFragment;
import projekt.substratum.fragments.TeamFragment;
import projekt.substratum.fragments.ThemeFragment;
import projekt.substratum.fragments.TroubleshootingFragment;
import projekt.substratum.services.floatui.SubstratumFloatInterface;
import projekt.substratum.services.tiles.FloatUiTile;
import projekt.substratum.util.files.Root;
import projekt.substratum.util.helpers.ContextWrapper;
import projekt.substratum.util.injectors.AOPTCheck;
import projekt.substratum.util.views.SheetDialog;

import static android.content.om.OverlayInfo.STATE_APPROVED_DISABLED;
import static android.content.om.OverlayInfo.STATE_APPROVED_ENABLED;
import static android.content.om.OverlayInfo.STATE_NOT_APPROVED_MISSING_TARGET;
import static projekt.substratum.common.References.BYPASS_ALL_VERSION_CHECKS;
import static projekt.substratum.common.References.ENABLE_ROOT_CHECK;
import static projekt.substratum.common.References.EXTERNAL_STORAGE_CACHE;
import static projekt.substratum.common.References.SUBSTRATUM_BUILDER_CACHE;
import static projekt.substratum.common.References.SUBSTRATUM_LOG;
import static projekt.substratum.common.References.checkUsagePermissions;
import static projekt.substratum.common.References.isSamsung;

public class MainActivity extends SubstratumActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback, SearchView.OnQueryTextListener {

    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private static final int PERMISSIONS_REQUEST_DRAW_OVER_OTHER_APPS = 2;
    private static final int PERMISSIONS_REQUEST_USAGE_ACCESS_SETTINGS = 3;
    private static final String SELECTED_DRAWER_ITEM = "selected_drawer_item";
    public static String userInput = "";
    public static MenuItem searchItem;
    private static ActionBar supportActionBar;
    public TextView actionbar_title, actionbar_content;
    public SearchView searchView;
    private Drawer drawer;
    private int permissionCheck;
    private ProgressDialog mProgressDialog;
    private SharedPreferences prefs;
    private boolean hideBundle, hideRestartUi;

    public void switchToCustomToolbar(String title, String content) {
        if (supportActionBar != null) supportActionBar.setTitle("");
        if (actionbar_content != null) actionbar_content.setVisibility(View.VISIBLE);
        if (actionbar_title != null) actionbar_title.setVisibility(View.VISIBLE);
        if (actionbar_title != null) actionbar_title.setText(title);
        if (actionbar_content != null) actionbar_content.setText(content);
    }

    public void switchToStockToolbar(String title) {
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
        tx.replace(R.id.main, Fragment.instantiate(
                MainActivity.this,
                fragment));
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

    protected void installLeakCanary() {
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

        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        actionbar_title = findViewById(R.id.activity_title);
        actionbar_content = findViewById(R.id.theme_count);

        References.setROMVersion(getApplicationContext(), false);
        References.setAndCheckOMS(getApplicationContext());

        Toolbar toolbar = findViewById(R.id.toolbar);
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

        String versionName = BuildConfig.VERSION_NAME;
        if (BuildConfig.DEBUG) {
            versionName = versionName + " - " + BuildConfig.GIT_HASH;
        }

        AccountHeader header = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.material_drawer_header_background)
                .withProfileImagesVisible(false)
                .withSelectionListEnabledForSingleProfile(false)
                .addProfiles(
                        new ProfileDrawerItem()
                                .withName(getString(R.string.drawer_name))
                                .withEmail(versionName))
                .withCurrentProfileHiddenInList(true)
                .build();

        //LibsConfiguration.getInstance().setItemAnimator(new SlideDownAlphaAnimator());
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
        Boolean checkSamsungStatus = isSamsung(getApplicationContext());
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
        if (!checkSamsungStatus) drawerBuilder.addDrawerItems(
                new PrimaryDrawerItem()
                        .withName(R.string.nav_bootanim)
                        .withIcon(R.drawable.nav_bootanim)
                        .withIdentifier(3));
        if (References.isFontsSupported() && !checkSamsungStatus)
            drawerBuilder.addDrawerItems(
                    new PrimaryDrawerItem()
                            .withName(R.string.nav_fonts)
                            .withIcon(R.drawable.nav_fonts)
                            .withIdentifier(4));
        if (!checkSamsungStatus) drawerBuilder.addDrawerItems(
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
                References.isAuthorizedDebugger(getApplicationContext()) &&
                !checkSamsungStatus)
            drawerBuilder.addDrawerItems(
                    new PrimaryDrawerItem()
                            .withName(R.string.nav_studio)
                            .withIcon(R.drawable.nav_drawer_studio)
                            .withSelectable(false)
                            .withIdentifier(8));
        if (References.checkOMS(getApplicationContext()) && !checkSamsungStatus)
            drawerBuilder.addDrawerItems(
                    new PrimaryDrawerItem()
                            .withName(R.string.nav_priorities)
                            .withIcon(R.drawable.nav_drawer_priorities)
                            .withIdentifier(9));
        if (!checkSamsungStatus) drawerBuilder.addDrawerItems(
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
                        .withName(R.string.nav_team_contributors)
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
                                getApplicationContext()) ?
                                        getString(R.string.app_name) :
                                        (References.isSamsung(getApplicationContext()) ?
                                                getString(R.string.samsung_app_name) :
                                                getString(R.string.legacy_app_name)))
                                ),
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
                                ManagerFragment.class.getCanonicalName());
                        break;
                    case 8:
                        Intent intent = new Intent(getApplicationContext(),
                                StudioSelectorActivity.class);
                        startActivity(intent);
                        break;
                    case 9:
                        switchFragment(getString(R.string.nav_priorities),
                                PriorityLoaderFragment.class.getCanonicalName());
                        break;
                    case 10:
                        switchFragment(getString(R.string.nav_backup_restore),
                                ProfileFragment.class.getCanonicalName());
                        break;
                    case 11:
                        switchFragment(getString(R.string.nav_manage),
                                RecoveryFragment.class.getCanonicalName());
                        break;
                    case 12:
                        switchFragment(getString(R.string.nav_troubleshooting),
                                TroubleshootingFragment.class.getCanonicalName());
                        break;
                    case 13:
                        switchFragment(getString(R.string.nav_team_contributors),
                                TeamFragment.class.getCanonicalName());
                        break;
                    case 14:
                        switchFragmentToLicenses(getString(R.string.nav_opensource),
                                fragment);
                        break;
                    case 15:
                        switchFragment(getString(R.string.nav_settings),
                                SettingsFragment.class.getCanonicalName());
                        break;
                    case 100:
                        try {
                            String sourceURL = getString(R.string.googleplus_link);
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(sourceURL));
                            startActivity(i);
                        } catch (Exception e) {
                            Lunchbar.make(findViewById(android.R.id.content),
                                    getString(R.string.activity_missing_toast),
                                    Lunchbar.LENGTH_LONG)
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
                            Lunchbar.make(findViewById(android.R.id.content),
                                    getString(R.string.activity_missing_toast),
                                    Lunchbar.LENGTH_LONG)
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
                            Lunchbar.make(findViewById(android.R.id.content),
                                    getString(R.string.activity_missing_toast),
                                    Lunchbar.LENGTH_LONG)
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
                            Lunchbar.make(findViewById(android.R.id.content),
                                    getString(R.string.activity_missing_toast),
                                    Lunchbar.LENGTH_LONG)
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
                            Lunchbar.make(findViewById(android.R.id.content),
                                    getString(R.string.activity_missing_toast),
                                    Lunchbar.LENGTH_LONG)
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
                            Lunchbar.make(findViewById(android.R.id.content),
                                    getString(R.string.activity_missing_toast),
                                    Lunchbar.LENGTH_LONG)
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
                            Lunchbar.make(findViewById(android.R.id.content),
                                    getString(R.string.activity_missing_toast),
                                    Lunchbar.LENGTH_LONG)
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
                            Lunchbar.make(findViewById(android.R.id.content),
                                    getString(R.string.activity_missing_toast),
                                    Lunchbar.LENGTH_LONG)
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
                            Lunchbar.make(findViewById(android.R.id.content),
                                    getString(R.string.activity_missing_toast),
                                    Lunchbar.LENGTH_LONG)
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

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.permission_explanation_title)
                    .setMessage(R.string.permission_explanation_text)
                    .setPositiveButton(R.string.accept, (dialog, i) -> {
                        dialog.cancel();

                        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                            // permission already granted, allow the program to continue running
                            File directory = new File(Environment.getExternalStorageDirectory(),
                                    EXTERNAL_STORAGE_CACHE);
                            if (!directory.exists()) {
                                Boolean made = directory.mkdirs();
                                if (!made) Log.e(References.SUBSTRATUM_LOG,
                                        "Unable to create directory");
                            }
                            File cacheDirectory = new File(getCacheDir(),
                                    SUBSTRATUM_BUILDER_CACHE);
                            if (!cacheDirectory.exists()) {
                                Boolean made = cacheDirectory.mkdirs();
                                if (!made) Log.e(References.SUBSTRATUM_LOG,
                                        "Unable to create cache directory");
                            }
                            References.injectRescueArchives(getApplicationContext());
                        } else {
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                        }

                        if (!References.checkROMVersion(getApplicationContext())) {
                            prefs.edit().remove("oms_state").apply();
                            prefs.edit().remove("oms_version").apply();
                            References.setROMVersion(getApplicationContext(), true);
                            References.setAndCheckOMS(getApplicationContext());
                            this.recreate();
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

                        if (!References.checkOMS(getApplicationContext()) &&
                                References.isIncompatibleFirmware()) {
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
            if (!References.checkROMVersion(getApplicationContext())) {
                References.setROMVersion(getApplicationContext(), true);
                prefs.edit().remove("oms_state").apply();
                prefs.edit().remove("oms_version").apply();
                References.setAndCheckOMS(getApplicationContext());
                this.recreate();
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

            References.startKeyRetrievalReceiver(getApplicationContext());
            mProgressDialog = new ProgressDialog(this, R.style.SubstratumBuilder_BlurView);
            new RootRequester(this).execute();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
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
        if (isOMS || isSamsung(getApplicationContext())) {
            menu.findItem(R.id.reboot_device).setVisible(false);
            menu.findItem(R.id.soft_reboot).setVisible(false);
        }
        if (!isOMS) menu.findItem(R.id.per_app).setVisible(false);

        searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(this);
        searchItem.setVisible(!hideBundle);
        MenuItem restartUi = menu.findItem(R.id.restart_systemui);
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
                                Uri.parse("package:" + getApplicationContext().getPackageName()));
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
                DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            ThemeManager.restartSystemUI(getApplicationContext());
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.dialog_restart_systemui_title));
                builder.setMessage(getString(R.string.dialog_restart_systemui_content));
                builder.setPositiveButton(
                        getString(R.string.restore_dialog_okay), dialogClickListener);
                builder.setNegativeButton(
                        getString(R.string.restore_dialog_cancel), dialogClickListener);
                builder.show();
                return true;
            // Begin RRO based options
            case R.id.reboot_device:
                dialogClickListener = (dialog, which) -> {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            prefs.edit().clear().apply();
                            ElevatedCommands.reboot();
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                    }
                };
                builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.dialog_restart_reboot_title));
                builder.setMessage(getString(R.string.dialog_restart_reboot_content));
                builder.setPositiveButton(
                        getString(R.string.restore_dialog_okay), dialogClickListener);
                builder.setNegativeButton(
                        getString(R.string.restore_dialog_cancel), dialogClickListener);
                builder.show();
                return true;

            case R.id.soft_reboot:
                dialogClickListener = (dialog, which) -> {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            prefs.edit().clear().apply();
                            ElevatedCommands.softReboot();
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                    }
                };
                builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.dialog_restart_soft_reboot_title));
                builder.setMessage(getString(R.string.dialog_restart_soft_reboot_content));
                builder.setPositiveButton(
                        getString(R.string.restore_dialog_okay), dialogClickListener);
                builder.setNegativeButton(
                        getString(R.string.restore_dialog_cancel), dialogClickListener);
                builder.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (!searchView.isIconified()) {
            searchView.setIconified(true);
        } else if (drawer != null && drawer.isDrawerOpen()) {
            drawer.closeDrawer();
        } else {
            Fragment f = getSupportFragmentManager().findFragmentById(R.id.main);
            if (f instanceof PriorityListFragment) {
                Fragment fragment = new PriorityLoaderFragment();
                FragmentManager fm = getSupportFragmentManager();
                FragmentTransaction transaction = fm.beginTransaction();
                transaction.setCustomAnimations(
                        android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                transaction.replace(R.id.main, fragment);
                transaction.commit();
            } else if (drawer != null && drawer.getCurrentSelectedPosition() > 1) {
                drawer.setSelectionAtPosition(1);
            } else if (drawer != null && drawer.getCurrentSelectedPosition() == 1) {
                this.finish();
            }
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        Context newBase = base;
        prefs = PreferenceManager.getDefaultSharedPreferences(base);
        boolean languageCheck = prefs.getBoolean("force_english", false);
        if (languageCheck) {
            Locale newLocale = new Locale(Locale.ENGLISH.getLanguage());
            newBase = ContextWrapper.wrapNewLocale(base, newLocale);
        }
        super.attachBaseContext(newBase);
    }

    public void showFloatingHead() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());
        prefs.edit().putInt("float_tile", Tile.STATE_ACTIVE).apply();
        FloatUiTile.requestListeningState(getApplicationContext(),
                new ComponentName(getApplicationContext(), FloatUiTile.class));
        getApplicationContext().startService(new Intent(getApplicationContext(),
                SubstratumFloatInterface.class));
        PackageManager packageManager = getPackageManager();
        ComponentName componentName =
                new ComponentName(getApplicationContext(), FloatUiTile.class);
        packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
        startService(new Intent(this, FloatUiTile.class));
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
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission already granted, allow the program to continue running
                    File directory = new File(Environment.getExternalStorageDirectory(),
                            EXTERNAL_STORAGE_CACHE);
                    if (!directory.exists()) {
                        Boolean made = directory.mkdirs();
                        if (!made) Log.e(References.SUBSTRATUM_LOG,
                                "Unable to create directory");
                    }
                    File cacheDirectory = new File(getCacheDir(),
                            SUBSTRATUM_BUILDER_CACHE);
                    if (!cacheDirectory.exists()) {
                        Boolean made = cacheDirectory.mkdirs();
                        if (!made) Log.e(References.SUBSTRATUM_LOG,
                                "Unable to create cache directory");
                    }
                    References.injectRescueArchives(getApplicationContext());
                    File[] fileList = new File(getCacheDir().getAbsolutePath() +
                            SUBSTRATUM_BUILDER_CACHE).listFiles();
                    for (File file : fileList) {
                        FileOperations.delete(getApplicationContext(), getCacheDir()
                                .getAbsolutePath() +
                                SUBSTRATUM_BUILDER_CACHE + file.getName());
                    }
                    Log.d("SubstratumBuilder", "The cache has been flushed!");
                    mProgressDialog = new ProgressDialog(this, R.style.SubstratumBuilder_BlurView);
                    References.injectRescueArchives(getApplicationContext());
                    new RootRequester(this).execute();
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
            LinearLayout hide = sheetView.findViewById(R.id.hide_outdated_themes);
            LinearLayout show = sheetView.findViewById(R.id.show_outdated_themes);
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

    private static class RootRequester extends AsyncTask<Void, Void, Boolean> {
        private WeakReference<MainActivity> ref;

        private RootRequester(MainActivity activity) {
            ref = new WeakReference<>(activity);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            MainActivity activity = ref.get();
            Context context = activity.getApplicationContext();

            if (!result && !References.isSamsung(context) &&
                    ENABLE_ROOT_CHECK && !BYPASS_ALL_VERSION_CHECKS &&
                    !References.checkThemeInterfacer(context)) {
                activity.mProgressDialog.setCancelable(false);
                activity.mProgressDialog.show();
                activity.mProgressDialog.setContentView(R.layout.root_rejected_loader);

                float radius = 5;
                View decorView = activity.getWindow().getDecorView();
                ViewGroup rootView = decorView.findViewById(android.R.id.content);
                Drawable windowBackground = decorView.getBackground();

                BlurView blurView = activity.mProgressDialog.findViewById(R.id.blurView);

                blurView.setupWith(rootView)
                        .windowBackground(windowBackground)
                        .blurAlgorithm(new RenderScriptBlur(context))
                        .blurRadius(radius);
                TextView textView = activity.mProgressDialog.findViewById(R.id.timer);
                if (References.isPackageInstalled(
                        context, "eu.chainfire.supersu")) {
                    CountDownTimer Count = new CountDownTimer(5000, 1000) {
                        public void onTick(long millisUntilFinished) {
                            if ((millisUntilFinished / 1000) > 1) {
                                textView.setText(String.format(
                                        activity.getString(R.string.root_rejected_timer_plural),
                                        (millisUntilFinished / 1000) + ""));
                            } else {
                                textView.setText(String.format(
                                        activity.getString(R.string.root_rejected_timer_singular),
                                        (millisUntilFinished / 1000) + ""));
                            }
                        }

                        public void onFinish() {
                            activity.mProgressDialog.dismiss();
                            activity.finish();
                        }
                    };
                    Count.start();
                } else {
                    textView.setText(activity.getString(R.string.root_rejected_text_cm_phh));
                }
            } else {
                activity.showOutdatedRequestDialog();
                new AOPTCheck().injectAOPT(activity.getApplicationContext(), false);
                if (References.checkOMS(context)) new DoCleanUp(context).execute();
            }
        }

        @Override
        protected Boolean doInBackground(Void... sUrl) {
            MainActivity activity = ref.get();
            Context context = activity.getApplicationContext();
            activity.prefs.edit().putBoolean("complexion",
                    !References.spreadYourWingsAndFly(context) &&
                            References.hashPassthrough(context) != 0).apply();
            if (!References.checkThemeInterfacer(context) &&
                    !References.isSamsung(context)) {
                Boolean receivedRoot = Root.requestRootAccess();
                if (receivedRoot) Log.d(SUBSTRATUM_LOG, "Substratum has loaded in rooted mode.");
                References.injectRescueArchives(context);
                return receivedRoot;
            } else if (References.isSamsung(context)) {
                Log.d(SUBSTRATUM_LOG, "Substratum has loaded in Samsung mode.");
                return false;
            } else {
                Log.d(SUBSTRATUM_LOG, "Substratum has loaded in rootless mode.");
                return false;
            }
        }
    }

    private static class DoCleanUp extends AsyncTask<Void, Void, Void> {
        @SuppressLint("StaticFieldLeak")
        private Context context;

        private DoCleanUp(Context context) {
            this.context = context;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }

        @Override
        protected Void doInBackground(Void... sUrl) {
            ArrayList<String> removeList = new ArrayList<>();
            // Overlays with non-existent targets
            List<String> state1 = ThemeManager.listOverlays(
                    context, STATE_NOT_APPROVED_MISSING_TARGET);
            // Uninstall overlays when the main theme is not present, regardless if enabled/disabled
            List<String> state4 = ThemeManager.listOverlays(
                    context, STATE_APPROVED_DISABLED);
            List<String> state5 = ThemeManager.listOverlays(
                    context, STATE_APPROVED_ENABLED);
            // We need the null check because listOverlays never returns null, but empty
            if (state1.size() > 0 && state1.get(0) != null) {
                for (int i = 0; i < state1.size(); i++) {
                    Log.e("OverlayCleaner",
                            "Target APK not found for \"" + state1.get(i) +
                                    "\" and will be removed.");
                    removeList.add(state1.get(i));
                }
            }

            ArrayList<String> installed_overlays = new ArrayList<>(state4);
            installed_overlays.addAll(state5);
            for (int i = 0; i < installed_overlays.size(); i++) {
                String parent = References.grabOverlayParent(context, installed_overlays.get(i));
                String pack = References.grabIconPack(context, installed_overlays.get(i));
                if (!References.isPackageInstalled(context, parent) &&
                        !References.isPackageInstalled(context, pack)) {
                    Log.e("OverlayCleaner",
                            "Parent APK not found for \"" + installed_overlays.get(i) +
                                    "\" and will be removed.");
                    removeList.add(installed_overlays.get(i));
                }
            }

            if (removeList.size() > 0)
                ThemeManager.uninstallOverlay(context, removeList, false);
            return null;
        }
    }
}
