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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import projekt.substratum.activities.base.SubstratumActivity;
import projekt.substratum.activities.showcase.ShowcaseActivity;
import projekt.substratum.common.Broadcasts;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Resources;
import projekt.substratum.common.Systems;
import projekt.substratum.common.Theming;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.AndromedaService;
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
import projekt.substratum.util.injectors.CheckBinaries;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.O;
import static projekt.substratum.common.Activities.launchActivityUrl;
import static projekt.substratum.common.Activities.launchExternalActivity;
import static projekt.substratum.common.Activities.launchInternalActivity;
import static projekt.substratum.common.References.ANDROMEDA_PACKAGE;
import static projekt.substratum.common.References.BYPASS_ALL_VERSION_CHECKS;
import static projekt.substratum.common.References.ENABLE_ROOT_CHECK;
import static projekt.substratum.common.References.EXTERNAL_STORAGE_CACHE;
import static projekt.substratum.common.References.LOGCHAR_DIR;
import static projekt.substratum.common.References.NO_THEME_ENGINE;
import static projekt.substratum.common.References.OVERLAY_MANAGER_SERVICE_N_UNROOTED;
import static projekt.substratum.common.References.OVERLAY_MANAGER_SERVICE_O_ANDROMEDA;
import static projekt.substratum.common.References.OVERLAY_MANAGER_SERVICE_O_UNROOTED;
import static projekt.substratum.common.References.OVERLAY_UPDATE_RANGE;
import static projekt.substratum.common.References.SAMSUNG_THEME_ENGINE_N;
import static projekt.substratum.common.References.SST_ADDON_PACKAGE;
import static projekt.substratum.common.References.SUBSTRATUM_BUILDER_CACHE;
import static projekt.substratum.common.References.SUBSTRATUM_LOG;
import static projekt.substratum.common.Systems.checkThemeSystemModule;
import static projekt.substratum.common.Systems.checkUsagePermissions;
import static projekt.substratum.common.Systems.isSamsung;
import static projekt.substratum.common.commands.FileOperations.delete;
import static projekt.substratum.common.platform.ThemeManager.STATE_MISSING_TARGET_N;
import static projekt.substratum.common.platform.ThemeManager.STATE_MISSING_TARGET_O;

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
    private int permissionCheck = PackageManager.PERMISSION_DENIED;
    private Dialog mProgressDialog;
    private SharedPreferences prefs;
    private boolean hideBundle, hideRestartUi;
    private LocalBroadcastManager localBroadcastManager;
    private LocalBroadcastManager localBroadcastManager2;
    private KillReceiver killReceiver;
    private AndromedaReceiver andromedaReceiver;
    private Context mContext;

    private static boolean checkIfOverlaysOutdated(Context context) {
        List<String> overlays = ThemeManager.listAllOverlays(context);
        for (int i = 0; i < overlays.size(); i++) {
            int current_version = Packages.getOverlaySubstratumVersion(
                    context,
                    overlays.get(i));
            if (current_version <= OVERLAY_UPDATE_RANGE && current_version != 0) {
                Log.d("OverlayOutdatedCheck",
                        "An overlay is returning " + current_version +
                                " as Substratum's version, " +
                                "this overlay is out of date, please uninstall and reinstall!");
                return true;
            }
        }
        return false;
    }

    public void switchToCustomToolbar(String title, String content) {
        if (supportActionBar != null) supportActionBar.setTitle("");
        if (actionbar_content != null) actionbar_content.setVisibility(View.VISIBLE);
        if (actionbar_title != null) actionbar_title.setVisibility(View.VISIBLE);
        if (actionbar_title != null) actionbar_title.setText(title);
        if (actionbar_content != null) actionbar_content.setText(content);
    }

    public void switchToStockToolbar(String title) {
        if (actionbar_content != null) actionbar_content.setVisibility(View.GONE);
        if (actionbar_title != null) actionbar_title.setVisibility(View.GONE);
        if (supportActionBar != null) supportActionBar.setTitle(title);
    }

    private void switchFragment(String title, String fragment) {
        if (searchView != null && !searchView.isIconified()) {
            searchView.setIconified(true);
        }
        switchToStockToolbar(title);
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        tx.replace(R.id.main, Fragment.instantiate(
                MainActivity.this,
                fragment));
        tx.commit();
        hideBundle = !title.equals(getString(R.string.nav_overlay_manager));
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
    public void onDestroy() {
        super.onDestroy();
        try {
            localBroadcastManager.unregisterReceiver(killReceiver);
        } catch (Exception e) {
            // Unregistered already
        }

        if (Systems.isAndromedaDevice(mContext)) {
            try {
                localBroadcastManager2.unregisterReceiver(andromedaReceiver);
            } catch (Exception e) {
                // Unregistered already
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();
        mProgressDialog = new Dialog(this, R.style.SubstratumBuilder_ActivityTheme);
        mProgressDialog.setCancelable(false);

        if (BuildConfig.DEBUG && !Systems.isSamsung(mContext)) {
            Log.d(SUBSTRATUM_LOG, "Substratum launched with debug mode signatures.");
            if (LeakCanary.isInAnalyzerProcess(this)) return;
            installLeakCanary();
            Log.d(SUBSTRATUM_LOG,
                    "LeakCanary has been initialized to actively monitor memory leaks.");
        }
        setContentView(R.layout.main_activity);
        cleanLogCharReportsIfNecessary();
        Theming.refreshInstalledThemesPref(mContext);

        int selectedDrawer = 1;
        if (savedInstanceState != null) {
            selectedDrawer = savedInstanceState.getInt(SELECTED_DRAWER_ITEM);
        }

        // Register the main app receiver to auto kill the activity
        killReceiver = new KillReceiver();
        IntentFilter filter = new IntentFilter("MainActivity.KILL");
        localBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        localBroadcastManager.registerReceiver(killReceiver, filter);

        if (Systems.isAndromedaDevice(mContext)) {
            andromedaReceiver = new AndromedaReceiver();
            IntentFilter filter2 = new IntentFilter("AndromedaReceiver.KILL");
            localBroadcastManager2 = LocalBroadcastManager.getInstance(mContext);
            localBroadcastManager2.registerReceiver(andromedaReceiver, filter2);
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        actionbar_title = findViewById(R.id.activity_title);
        actionbar_content = findViewById(R.id.theme_count);

        Systems.setROMVersion(mContext, false);
        Systems.setAndCheckOMS(mContext);

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
                        new SecondaryDrawerItem().withName(R.string.nav_drawer_reddit)
                                .withLevel(2).withIcon(R.drawable.nav_reddit)
                                .withSelectable(false)
                                .withIdentifier(101),
                        new SecondaryDrawerItem().withName(R.string.nav_drawer_telegram)
                                .withLevel(2).withIcon(R.drawable.nav_drawer_telegram)
                                .withSelectable(false)
                                .withIdentifier(102),
                        new SecondaryDrawerItem().withName(R.string.nav_drawer_xda)
                                .withLevel(2).withIcon(R.drawable.nav_drawer_xda)
                                .withSelectable(false)
                                .withIdentifier(103));

        // Split the featured content out for easy adapting
        ExpandableDrawerItem featured = new ExpandableDrawerItem()
                .withName(R.string.nav_drawer_featured)
                .withIcon(R.drawable.nav_drawer_featured)
                .withSelectable(false).withSubItems(
                        new SecondaryDrawerItem().withName(R.string.nav_drawer_rawad)
                                .withLevel(2).withIcon(R.drawable.nav_drawer_youtube)
                                .withSelectable(false)
                                .withIdentifier(104),
                        new SecondaryDrawerItem().withName(R.string.nav_drawer_xda_portal)
                                .withLevel(2).withIcon(R.drawable.nav_drawer_xda_portal)
                                .withSelectable(false)
                                .withIdentifier(105));

        // Split the resources out for easy adapting
        ExpandableDrawerItem resources = new ExpandableDrawerItem()
                .withName(R.string.nav_drawer_resources)
                .withIcon(R.drawable.nav_drawer_resources)
                .withSelectable(false).withSubItems(
                        new SecondaryDrawerItem().withName(R.string.nav_drawer_homepage)
                                .withLevel(2).withIcon(R.drawable.nav_drawer_homepage)
                                .withSelectable(false)
                                .withIdentifier(106),
                        new SecondaryDrawerItem().withName(R.string.nav_drawer_template)
                                .withLevel(2).withIcon(R.drawable.nav_drawer_template)
                                .withSelectable(false)
                                .withIdentifier(107),
                        new SecondaryDrawerItem().withName(R.string.nav_drawer_gerrit)
                                .withLevel(2).withIcon(R.drawable.nav_drawer_gerrit)
                                .withSelectable(false)
                                .withIdentifier(108),
                        new SecondaryDrawerItem().withName(R.string.nav_drawer_github)
                                .withLevel(2).withIcon(R.drawable.nav_drawer_github)
                                .withSelectable(false)
                                .withIdentifier(109),
                        new SecondaryDrawerItem().withName(R.string.nav_drawer_jira)
                                .withLevel(2).withIcon(R.drawable.nav_drawer_jira)
                                .withSelectable(false)
                                .withIdentifier(110));

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
        if (Resources.isBootAnimationSupported(mContext))
            drawerBuilder.addDrawerItems(
                    new PrimaryDrawerItem()
                            .withName(R.string.nav_bootanim)
                            .withIcon(R.drawable.nav_bootanim)
                            .withIdentifier(3));
        if (Resources.isShutdownAnimationSupported())
            drawerBuilder.addDrawerItems(
                    new PrimaryDrawerItem()
                            .withName(R.string.nav_shutdownanim)
                            .withIcon(R.drawable.nav_shutdownanim)
                            .withIdentifier(4));
        if (Resources.isFontsSupported())
            drawerBuilder.addDrawerItems(
                    new PrimaryDrawerItem()
                            .withName(R.string.nav_fonts)
                            .withIcon(R.drawable.nav_fonts)
                            .withIdentifier(5));
        if (Resources.isSoundsSupported(mContext))
            drawerBuilder.addDrawerItems(
                    new PrimaryDrawerItem()
                            .withName(R.string.nav_sounds)
                            .withIcon(R.drawable.nav_sounds)
                            .withIdentifier(6));
        drawerBuilder.addDrawerItems(
                new PrimaryDrawerItem()
                        .withName(R.string.nav_wallpapers)
                        .withIcon(R.drawable.nav_wallpapers)
                        .withIdentifier(7));
        drawerBuilder.addDrawerItems(
                new SectionDrawerItem()
                        .withName(R.string.nav_section_header_utilities));
        drawerBuilder.addDrawerItems(
                new PrimaryDrawerItem()
                        .withName(R.string.nav_overlay_manager)
                        .withIcon(R.drawable.nav_overlay_manager)
                        .withIdentifier(8));
        if (Systems.checkOMS(mContext) && !isSamsung(mContext))
            drawerBuilder.addDrawerItems(
                    new PrimaryDrawerItem()
                            .withName(R.string.nav_priorities)
                            .withIcon(R.drawable.nav_drawer_priorities)
                            .withIdentifier(9));
        if (Resources.isProfilesSupported(mContext))
            drawerBuilder.addDrawerItems(
                    new PrimaryDrawerItem()
                            .withName(R.string.nav_backup_restore)
                            .withIcon(R.drawable.nav_drawer_profiles)
                            .withIdentifier(10)
                            .withBadge(getString(R.string.beta_tag)));
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
                        switchThemeFragment(((Systems.checkOMS(
                                mContext) ?
                                        getString(R.string.app_name) :
                                        (Systems.isSamsung(mContext) ?
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
                        switchThemeFragment(getString(R.string.nav_shutdownanim),
                                References.shutdownAnimationsFragment);
                        break;
                    case 5:
                        switchThemeFragment(getString(R.string.nav_fonts),
                                References.fontsFragment);
                        break;
                    case 6:
                        switchThemeFragment(getString(R.string.nav_sounds),
                                References.soundsFragment);
                        break;
                    case 7:
                        switchThemeFragment(getString(R.string.nav_wallpapers),
                                References.wallpaperFragment);
                        break;
                    case 8:
                        switchFragment(getString(R.string.nav_overlay_manager),
                                ManagerFragment.class.getCanonicalName());
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
                        launchActivityUrl(mContext, R.string.googleplus_link);
                        break;
                    case 101:
                        launchActivityUrl(mContext, R.string.reddit_link);
                        break;
                    case 102:
                        launchActivityUrl(mContext, R.string.telegram_link);
                        break;
                    case 103:
                        int sourceURL;
                        if (Systems.isSamsung(this)) {
                            sourceURL = R.string.xda_sungstratum_link;
                        } else {
                            sourceURL = R.string.xda_link;
                        }
                        launchActivityUrl(mContext, sourceURL);
                        break;
                    case 104:
                        launchActivityUrl(mContext, R.string.rawad_youtube_url);
                        break;
                    case 105:
                        launchActivityUrl(mContext, R.string.xda_portal_link);
                        break;
                    case 106:
                        launchActivityUrl(mContext, R.string.homepage_link);
                        break;
                    case 107:
                        launchActivityUrl(mContext, R.string.template_link);
                        break;
                    case 108:
                        launchActivityUrl(mContext, R.string.gerrit_link);
                        break;
                    case 109:
                        launchActivityUrl(mContext, R.string.github_link);
                        break;
                    case 110:
                        launchActivityUrl(mContext, R.string.jira_link);
                        break;
                }
            }
            return false;
        });
        drawer = drawerBuilder.build();
        if (getIntent() != null && getIntent().getBooleanExtra("launch_manager_fragment", false)) {
            switchFragment(getString(R.string.nav_overlay_manager),
                    ManagerFragment.class.getCanonicalName());
            drawer.setSelection(8);
        } else {
            drawer.setSelection(selectedDrawer, true);
        }

        new RootRequester(this).execute();
    }

    private void cleanLogCharReportsIfNecessary() {
        Date currentDate = new Date(System.currentTimeMillis());
        if (prefs.getLong("previous_logchar_cleanup", 0) == 0) {
            prefs.edit().putLong("previous_logchar_cleanup", currentDate.getTime()).apply();
            return;
        }
        long lastCleanupDate = prefs.getLong("previous_logchar_cleanup", 0);
        long diff = currentDate.getTime() - lastCleanupDate;
        if (TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) >= 15) {
            prefs.edit().putLong("previous_logchar_cleanup", currentDate.getTime()).apply();
            new ClearLogs(this).execute();
            Log.d(SUBSTRATUM_LOG, "LogChar reports were wiped from the storage");
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

        boolean isOMS = Systems.checkOMS(mContext);
        if (isOMS || isSamsung(mContext)) {
            menu.findItem(R.id.reboot_device).setVisible(false);
            menu.findItem(R.id.soft_reboot).setVisible(false);
        }
        if (!isOMS) menu.findItem(R.id.per_app).setVisible(false);

        searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(this);
        searchItem.setVisible(!hideBundle);
        MenuItem restartUi = menu.findItem(R.id.restart_systemui);
        restartUi.setVisible(!hideRestartUi &&
                !Systems.checkAndromeda(mContext) &&
                (isOMS || Root.checkRootAccess()));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SharedPreferences prefs = mContext.getSharedPreferences(
                "substratum_state", Context.MODE_PRIVATE);
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.search:
                launchInternalActivity(this, ShowcaseActivity.class);
                return true;

            // Begin OMS based options
            case R.id.per_app:
                if (!References.isServiceRunning(SubstratumFloatInterface.class,
                        mContext)) {
                    if (Settings.canDrawOverlays(mContext) &&
                            checkUsagePermissions(mContext)) {
                        showFloatingHead();
                    } else if (!Settings.canDrawOverlays(mContext)) {
                        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    Intent draw_over_apps = new Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:" + mContext
                                                    .getPackageName()));
                                    startActivityForResult(draw_over_apps,
                                            PERMISSIONS_REQUEST_DRAW_OVER_OTHER_APPS);
                                    break;
                                case DialogInterface.BUTTON_NEGATIVE:
                                    dialog.dismiss();
                                    break;
                            }
                        };
                        new AlertDialog.Builder(this)
                                .setTitle(R.string.per_app_request_title)
                                .setMessage(R.string.per_app_draw_over_other_apps_request)
                                .setPositiveButton(R.string.dialog_ok, dialogClickListener)
                                .setNegativeButton(R.string.dialog_cancel, dialogClickListener)
                                .show();
                    } else if (!checkUsagePermissions(mContext)) {
                        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    Intent usage = new Intent(Settings
                                            .ACTION_USAGE_ACCESS_SETTINGS);
                                    startActivityForResult(usage,
                                            PERMISSIONS_REQUEST_USAGE_ACCESS_SETTINGS);
                                    break;
                                case DialogInterface.BUTTON_NEGATIVE:
                                    dialog.dismiss();
                                    break;
                            }
                        };
                        new AlertDialog.Builder(this)
                                .setTitle(R.string.per_app_request_title)
                                .setMessage(R.string.per_app_usage_stats_request)
                                .setPositiveButton(R.string.dialog_ok, dialogClickListener)
                                .setNegativeButton(R.string.dialog_cancel, dialogClickListener)
                                .show();
                    }
                } else {
                    hideFloatingHead();
                }
                return true;

            case R.id.restart_systemui:
                DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            ThemeManager.restartSystemUI(mContext);
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
                        getString(R.string.dialog_cancel), dialogClickListener);
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
                        getString(R.string.dialog_cancel), dialogClickListener);
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
                        getString(R.string.dialog_cancel), dialogClickListener);
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
                mContext);
        prefs.edit().putInt("float_tile", Tile.STATE_ACTIVE).apply();
        FloatUiTile.requestListeningState(mContext,
                new ComponentName(mContext, FloatUiTile.class));
        mContext.startService(new Intent(mContext,
                SubstratumFloatInterface.class));
        PackageManager packageManager = getPackageManager();
        ComponentName componentName =
                new ComponentName(mContext, FloatUiTile.class);
        packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
        startService(new Intent(this, FloatUiTile.class));
    }

    private void hideFloatingHead() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                mContext);
        prefs.edit().putInt("float_tile", Tile.STATE_INACTIVE).apply();
        FloatUiTile.requestListeningState(mContext,
                new ComponentName(mContext, FloatUiTile.class));
        stopService(new Intent(mContext,
                SubstratumFloatInterface.class));
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_DRAW_OVER_OTHER_APPS:
                if (!checkUsagePermissions(mContext)) {
                    Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                    startActivityForResult(intent, PERMISSIONS_REQUEST_USAGE_ACCESS_SETTINGS);
                } else {
                    if (Settings.canDrawOverlays(mContext) &&
                            checkUsagePermissions(mContext)) {
                        showFloatingHead();
                    }
                }
                break;
            case PERMISSIONS_REQUEST_USAGE_ACCESS_SETTINGS:
                if (Settings.canDrawOverlays(mContext) &&
                        checkUsagePermissions(mContext)) {
                    showFloatingHead();
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null) {
            setIntent(intent);
            if (getIntent().getBooleanExtra("launch_manager_fragment", false)) {
                switchFragment(getString(R.string.nav_overlay_manager),
                        ManagerFragment.class.getCanonicalName());
                drawer.setSelection(8);
            }
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
                    File directory = new File(EXTERNAL_STORAGE_CACHE);
                    if (directory.exists()) {
                        Boolean deleted = directory.delete();
                        if (!deleted) Log.e(References.SUBSTRATUM_LOG,
                                "Unable to delete directory");
                    } else {
                        Log.d(References.SUBSTRATUM_LOG, "Deleting old cache dir: " + directory);
                    }
                    if (!directory.exists()) {
                        Boolean made = directory.mkdirs();
                        if (!made) Log.e(References.SUBSTRATUM_LOG,
                                "Unable to create directory");
                    } else {
                        References.injectRescueArchives(mContext);
                        Log.d(References.SUBSTRATUM_LOG, "Successfully made dir: " + directory);
                    }
                    File cacheDirectory = new File(getCacheDir(),
                            SUBSTRATUM_BUILDER_CACHE);
                    if (!cacheDirectory.exists()) {
                        Boolean made = cacheDirectory.mkdirs();
                        if (!made) Log.e(References.SUBSTRATUM_LOG,
                                "Unable to create cache directory");
                    }
                    File[] fileList = new File(getCacheDir().getAbsolutePath() +
                            SUBSTRATUM_BUILDER_CACHE).listFiles();
                    for (File file : fileList) {
                        FileOperations.delete(mContext, getCacheDir()
                                .getAbsolutePath() +
                                SUBSTRATUM_BUILDER_CACHE + file.getName());
                    }
                    Log.d("SubstratumBuilder", "The cache has been flushed!");
                    References.injectRescueArchives(mContext);
                } else {
                    // permission was not granted, show closing dialog
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.permission_not_granted_dialog_title)
                            .setMessage(R.string.permission_not_granted_dialog_message1)
                            .setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
                                if (shouldShowRequestPermissionRationale(
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

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    private static class RootRequester extends AsyncTask<Void, Void, Boolean> {
        boolean isRunning = true;
        private WeakReference<MainActivity> ref;

        private RootRequester(MainActivity activity) {
            ref = new WeakReference<>(activity);
        }

        private void permissionCheck() {
            MainActivity activity = ref.get();
            if (activity != null) {
                Context context = activity.mContext;
                activity.permissionCheck = ContextCompat.checkSelfPermission(
                        context,
                        WRITE_EXTERNAL_STORAGE);

                if (activity.permissionCheck != PackageManager.PERMISSION_GRANTED) {
                    new AlertDialog.Builder(activity)
                            .setCancelable(false)
                            .setTitle(R.string.permission_explanation_title)
                            .setMessage(R.string.permission_explanation_text)
                            .setPositiveButton(R.string.accept, (dialog, i) -> {
                                dialog.cancel();

                                if (activity.permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                    // permission already granted,
                                    // allow the program to continue running
                                    File directory = new File(EXTERNAL_STORAGE_CACHE);
                                    if (!directory.exists()) {
                                        Boolean made = directory.mkdirs();
                                        if (!made) Log.e(References.SUBSTRATUM_LOG,
                                                "Unable to create directory");
                                    }
                                    File cacheDirectory = new File(activity.getCacheDir(),
                                            SUBSTRATUM_BUILDER_CACHE);
                                    if (!cacheDirectory.exists()) {
                                        Boolean made = cacheDirectory.mkdirs();
                                        if (!made) Log.e(References.SUBSTRATUM_LOG,
                                                "Unable to create cache directory");
                                    }
                                    References.injectRescueArchives(context);
                                } else {
                                    ActivityCompat.requestPermissions(activity,
                                            new String[]{
                                                    WRITE_EXTERNAL_STORAGE},
                                            PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                                }

                                if (!Systems.checkROMVersion(context)) {
                                    activity.prefs.edit().remove("oms_state").apply();
                                    activity.prefs.edit().remove("oms_version").apply();
                                    Systems.setROMVersion(context, true);
                                    Systems.setAndCheckOMS(context);
                                    activity.recreate();
                                }

                                if (!Systems.checkOMS(context) &&
                                        !activity.prefs.contains("legacy_dismissal")) {
                                    AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                                    alert.setTitle(R.string.warning_title);
                                    if (Systems.isSamsung(context)) {
                                        alert.setMessage(R.string.samsung_warning_content);
                                    } else {
                                        alert.setMessage(R.string.legacy_warning_content);
                                    }
                                    alert.setPositiveButton(R.string.dialog_ok,
                                            (dialog2, i2) -> dialog2.cancel());
                                    alert.setNeutralButton(R.string.dialog_do_not_show_again,
                                            (dialog3, i3) -> {
                                                activity.prefs.edit().putBoolean(
                                                        "legacy_dismissal", true).apply();
                                                dialog3.cancel();
                                            });
                                    alert.show();
                                }
                            })
                            .setNegativeButton(R.string.deny,
                                    (dialog, i) -> {
                                        dialog.cancel();
                                        activity.finish();
                                    })
                            .show();
                } else {
                    if (!Systems.checkROMVersion(context)) {
                        Systems.setROMVersion(context, true);
                        activity.prefs.edit().remove("oms_state").apply();
                        activity.prefs.edit().remove("oms_version").apply();
                        Systems.setAndCheckOMS(context);
                        activity.recreate();
                    }

                    if (!Systems.checkOMS(context) &&
                            !activity.prefs.contains("legacy_dismissal")) {
                        AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                        alert.setTitle(R.string.warning_title);
                        if (Systems.isSamsung(context)) {
                            alert.setMessage(R.string.samsung_warning_content);
                        } else {
                            alert.setMessage(R.string.legacy_warning_content);
                        }
                        alert.setPositiveButton(R.string.dialog_ok, (dialog2, i2) ->
                                dialog2.cancel());
                        alert.setNeutralButton(R.string.dialog_do_not_show_again,
                                (dialog3, i3) -> {
                                    activity.prefs.edit().putBoolean(
                                            "legacy_dismissal", true).apply();
                                    dialog3.cancel();
                                });
                        alert.show();
                    }

                    Broadcasts.startKeyRetrievalReceiver(context);
                    if (!activity.prefs.contains("complexion")) {
                        activity.prefs.edit().putBoolean("complexion", true).apply();
                        new References.Markdown(context, activity.prefs);
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean dialogReturnBool) {
            dialogReturnBool &= ENABLE_ROOT_CHECK & !BYPASS_ALL_VERSION_CHECKS;

            super.onPostExecute(dialogReturnBool);
            MainActivity activity = ref.get();
            if (activity != null) {
                Context context = activity.mContext;
                showDialogOrNot(dialogReturnBool);
                if (!dialogReturnBool) permissionCheck();
                if (checkIfOverlaysOutdated(context)) {
                    new AlertDialog.Builder(activity)
                            .setTitle(R.string.overlays_outdated)
                            .setMessage(R.string.overlays_outdated_message)
                            .setPositiveButton(R.string.dialog_ok, (dialogInterface, i) -> {
                            })
                            .show();
                }
            }
        }

        private void showDialogOrNot(Boolean passthrough) {
            MainActivity activity = ref.get();
            isRunning = false;
            if (activity != null) {
                Context context = activity.mContext;
                if (passthrough) {
                    activity.mProgressDialog.show();
                    activity.mProgressDialog.setContentView(R.layout.root_rejected_loader);

                    TextView titleView = activity.mProgressDialog.findViewById(R.id.title);
                    TextView textView =
                            activity.mProgressDialog.findViewById(R.id.root_rejected_text);
                    if (Systems.isSamsungDevice(context)) {
                        TextView samsungTitle = activity.mProgressDialog.findViewById(
                                R.id.sungstratum_title);
                        samsungTitle.setVisibility(View.VISIBLE);
                        Button samsungButton = activity.mProgressDialog.findViewById(
                                R.id.sungstratum_button);
                        samsungButton.setVisibility(View.VISIBLE);
                        samsungButton.setOnClickListener(view ->
                                launchActivityUrl(context, R.string.sungstratum_url));
                        textView.setVisibility(View.GONE);
                        titleView.setVisibility(View.GONE);
                    } else if (Systems.isAndromedaDevice(context) &&
                            !AndromedaService.checkServerActivity()) {
                        TextView andromedaTitle = activity.mProgressDialog.findViewById(
                                R.id.andromeda_title);
                        andromedaTitle.setText(R.string.andromeda_disconnected);
                        andromedaTitle.setVisibility(View.VISIBLE);
                        Button andromedaButton = activity.mProgressDialog.findViewById(
                                R.id.andromeda_button);
                        andromedaButton.setText(R.string.andromeda_check_status);
                        andromedaButton.setVisibility(View.VISIBLE);
                        andromedaButton.setOnClickListener(view ->
                                launchExternalActivity(context, ANDROMEDA_PACKAGE, "InfoActivity"));
                        textView.setVisibility(View.GONE);
                        titleView.setVisibility(View.GONE);
                    } else if (Systems.checkOreo() &&
                            !Packages.isPackageInstalled(context, ANDROMEDA_PACKAGE)) {
                        TextView andromedaTitle = activity.mProgressDialog.findViewById(
                                R.id.andromeda_title);
                        andromedaTitle.setVisibility(View.VISIBLE);
                        Button andromedaButton = activity.mProgressDialog.findViewById(
                                R.id.andromeda_button);
                        andromedaButton.setVisibility(View.VISIBLE);
                        andromedaButton.setOnClickListener(view ->
                                launchActivityUrl(context, R.string.andromeda_url));
                        textView.setVisibility(View.GONE);
                        titleView.setVisibility(View.GONE);
                    }
                } else {
                    CheckBinaries.install(activity.mContext, false);
                    if (Systems.checkOMS(context)) new DoCleanUp(context).execute();
                }
            }
        }

        @Override
        protected Boolean doInBackground(Void... sUrl) {
            MainActivity activity = ref.get();
            if (activity != null) {
                Context context = activity.mContext;
                int themeSystemModule = checkThemeSystemModule(context, true);

                // Samsung mode, but what if package is not installed?
                boolean samsungCheck = themeSystemModule == SAMSUNG_THEME_ENGINE_N;
                if (samsungCheck) {
                    // Throw the dialog when sungstratum addon is not installed
                    return !Packages.isPackageInstalled(context, SST_ADDON_PACKAGE);
                }

                // Check if the system is Andromeda mode
                boolean andromeda_check =
                        themeSystemModule == OVERLAY_MANAGER_SERVICE_O_ANDROMEDA;
                if (andromeda_check) {
                    Log.d("lmao", "here1");
                    // Throw the dialog when checkServerActivity() isn't working
                    return !AndromedaService.checkServerActivity();
                }

                // Check if the system is legacy
                boolean legacyCheck =
                        themeSystemModule == NO_THEME_ENGINE;
                if (legacyCheck) {
                    // Throw the dialog, after checking for root
                    return !Root.requestRootAccess();
                }

                // Check for OMS
                boolean omsCheck = Systems.checkOMS(context);
                if (omsCheck) {
                    return themeSystemModule != OVERLAY_MANAGER_SERVICE_O_UNROOTED &&
                            themeSystemModule != OVERLAY_MANAGER_SERVICE_N_UNROOTED &&
                            !Root.requestRootAccess();
                }
            }
            return false;
        }
    }

    public static class DoCleanUp extends AsyncTask<Void, Void, Void> {
        private WeakReference<Context> ref;

        public DoCleanUp(Context context) {
            ref = new WeakReference<>(context);
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }

        @Override
        protected Void doInBackground(Void... sUrl) {
            Context context = ref.get();
            if (context != null) {
                ArrayList<String> removeList = new ArrayList<>();
                // Overlays with non-existent targets
                List<String> state1 = ThemeManager.listOverlays(
                        context, SDK_INT >= O ? STATE_MISSING_TARGET_O : STATE_MISSING_TARGET_N);
                // Uninstall overlays when the main theme is not present,
                // regardless if enabled/disabled

                List<String> stateAll = ThemeManager.listAllOverlays(context);
                // We need the null check because listOverlays never returns null, but empty
                if (state1.size() > 0 && state1.get(0) != null) {
                    for (int i = 0; i < state1.size(); i++) {
                        Log.e("OverlayCleaner",
                                "Target APK not found for \"" + state1.get(i) +
                                        "\" and will be removed.");
                        removeList.add(state1.get(i));
                    }
                }

                for (int i = 0; i < stateAll.size(); i++) {
                    String parent = Packages.getOverlayParent(context, stateAll.get(i));
                    if (parent != null) {
                        if (!Packages.isPackageInstalled(context, parent)) {
                            Log.e("OverlayCleaner",
                                    "Parent APK not found for \"" + stateAll.get(i) +
                                            "\" and will be removed.");
                            removeList.add(stateAll.get(i));
                        }
                    }
                }

                if (removeList.size() > 0)
                    ThemeManager.uninstallOverlay(context, removeList);
            }
            return null;
        }
    }

    public static class ClearLogs extends AsyncTask<Void, Void, Void> {
        private WeakReference<MainActivity> ref;

        ClearLogs(MainActivity activity) {
            ref = new WeakReference<>(activity);
        }

        @Override
        protected Void doInBackground(Void... params) {
            MainActivity activity = ref.get();
            if (activity != null) {
                Context context = activity.mContext;
                delete(context, new File(LOGCHAR_DIR).getAbsolutePath());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            MainActivity activity = ref.get();
            if (activity != null) {
                Context context = activity.mContext;
                Toast.makeText(context, context.getString(R.string.cleaned_logchar_reports),
                        Toast.LENGTH_SHORT).show();
                Intent intent = activity.getIntent();
                activity.finishAffinity();
                context.startActivity(intent);
            }
        }

    }

    class KillReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    }

    class AndromedaReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            RootRequester rootRequester = new RootRequester(MainActivity.this);
            rootRequester.execute();
        }
    }
}
