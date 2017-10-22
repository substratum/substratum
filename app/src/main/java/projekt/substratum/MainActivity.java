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

public class MainActivity extends SubstratumActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback, SearchView.OnQueryTextListener {

    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private static final int PERMISSIONS_REQUEST_DRAW_OVER_OTHER_APPS = 2;
    private static final int PERMISSIONS_REQUEST_USAGE_ACCESS_SETTINGS = 3;
    private static final int UNINSTALL_REQUEST_CODE = 12675;
    private static final String SELECTED_DRAWER_ITEM = "selected_drawer_item";
    public static String userInput = "";
    private static MenuItem searchItem;
    public static ArrayList<String> queuedUninstall;
    private static ActionBar supportActionBar;
    private TextView actionbar_title;
    public TextView actionbar_content;
    public SearchView searchView;
    private Drawer drawer;
    private int permissionCheck = PackageManager.PERMISSION_DENIED;
    private Dialog mProgressDialog;
    private SharedPreferences prefs;
    private boolean hideBundle, hideRestartUi;
    private LocalBroadcastManager localBroadcastManager;
    private KillReceiver killReceiver;
    private AndromedaReceiver andromedaReceiver;
    private Context mContext;

    private static boolean checkIfOverlaysOutdated(final Context context) {
        final List<String> overlays = ThemeManager.listAllOverlays(context);
        for (int i = 0; i < overlays.size(); i++) {
            final int current_version = Packages.getOverlaySubstratumVersion(
                    context,
                    overlays.get(i));
            if ((current_version <= OVERLAY_UPDATE_RANGE) && (current_version != 0)) {
                Log.d("OverlayOutdatedCheck",
                        "An overlay is returning " + current_version +
                                " as Substratum's version, " +
                                "this overlay is out of date, please uninstall and reinstall!");
                return true;
            }
        }
        return false;
    }

    public static void uninstallMultipleAPKS(final Activity activity) {
        if (!MainActivity.queuedUninstall.isEmpty()) {
            final Uri packageURI = Uri.parse("package:" + MainActivity.queuedUninstall.get(0));
            final Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
            activity.startActivityForResult(uninstallIntent, UNINSTALL_REQUEST_CODE);
        }
    }

    public void switchToCustomToolbar(final CharSequence title, final CharSequence content) {
        if (supportActionBar != null) supportActionBar.setTitle("");
        if (this.actionbar_content != null) this.actionbar_content.setVisibility(View.VISIBLE);
        if (this.actionbar_title != null) this.actionbar_title.setVisibility(View.VISIBLE);
        if (this.actionbar_title != null) this.actionbar_title.setText(title);
        if (this.actionbar_content != null) this.actionbar_content.setText(content);
    }

    public void switchToStockToolbar(final CharSequence title) {
        if (this.actionbar_content != null) this.actionbar_content.setVisibility(View.GONE);
        if (this.actionbar_title != null) this.actionbar_title.setVisibility(View.GONE);
        if (supportActionBar != null) supportActionBar.setTitle(title);
    }

    private void switchFragment(final String title, final String fragment) {
        if ((this.searchView != null) && !this.searchView.isIconified()) {
            this.searchView.setIconified(true);
        }
        this.switchToStockToolbar(title);
        final FragmentTransaction tx = this.getSupportFragmentManager().beginTransaction();
        tx.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        tx.replace(R.id.main, Fragment.instantiate(
                MainActivity.this,
                fragment));
        tx.commit();
        this.hideBundle = !title.equals(this.getString(R.string.nav_overlay_manager));
        this.hideRestartUi = !title.equals(this.getString(R.string.nav_overlay_manager));
        this.supportInvalidateOptionsMenu();
    }

    private void switchThemeFragment(final String title, final String home_type) {
        if ((this.searchView != null) && !this.searchView.isIconified()) {
            this.searchView.setIconified(true);
        }
        final Fragment fragment = new ThemeFragment();
        final Bundle bundle = new Bundle();
        bundle.putString("home_type", home_type);
        bundle.putString("title", title);
        fragment.setArguments(bundle);

        this.switchToStockToolbar(title);
        final FragmentTransaction tx = this.getSupportFragmentManager().beginTransaction();
        tx.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        tx.replace(R.id.main, fragment);
        tx.commit();
        this.hideBundle = false;
        this.hideRestartUi = true;
        this.supportInvalidateOptionsMenu();
    }

    private void switchFragmentToLicenses(final CharSequence title, final LibsSupportFragment fragment) {
        if ((this.searchView != null) && !this.searchView.isIconified()) {
            this.searchView.setIconified(true);
        }
        this.switchToStockToolbar(title);
        final FragmentTransaction tx = this.getSupportFragmentManager().beginTransaction();
        tx.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        tx.replace(R.id.main, fragment);
        tx.commit();
        this.hideBundle = true;
        this.hideRestartUi = true;
        this.supportInvalidateOptionsMenu();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            this.localBroadcastManager.unregisterReceiver(this.killReceiver);
        } catch (final Exception e) {
            // Unregistered already
        }

        if (Systems.isAndromedaDevice(this.mContext)) {
            try {
                this.localBroadcastManager.unregisterReceiver(this.andromedaReceiver);
            } catch (final Exception e) {
                // Unregistered already
            }
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mContext = this.getApplicationContext();
        this.mProgressDialog = new Dialog(this, R.style.SubstratumBuilder_ActivityTheme);
        this.mProgressDialog.setCancelable(false);

        if (BuildConfig.DEBUG && !Systems.isSamsung(this.mContext)) {
            Log.d(SUBSTRATUM_LOG, "Substratum launched with debug mode signatures.");
        }
        this.setContentView(R.layout.main_activity);
        this.cleanLogCharReportsIfNecessary();
        Theming.refreshInstalledThemesPref(this.mContext);

        int selectedDrawer = 1;
        if (savedInstanceState != null) {
            selectedDrawer = savedInstanceState.getInt(SELECTED_DRAWER_ITEM);
        }

        // Register the main app receiver to auto kill the activity
        this.killReceiver = new KillReceiver();
        this.localBroadcastManager = LocalBroadcastManager.getInstance(this.mContext);
        this.localBroadcastManager.registerReceiver(this.killReceiver, new IntentFilter("MainActivity.KILL"));

        if (Systems.isAndromedaDevice(this.mContext)) {
            this.andromedaReceiver = new AndromedaReceiver();
            this.localBroadcastManager.registerReceiver(this.andromedaReceiver,
                    new IntentFilter("AndromedaReceiver.KILL"));
        }

        this.prefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);

        this.actionbar_title = this.findViewById(R.id.activity_title);
        this.actionbar_content = this.findViewById(R.id.theme_count);

        Systems.setROMVersion(this.mContext, false);
        Systems.setAndCheckOMS(this.mContext);

        final Toolbar toolbar = this.findViewById(R.id.toolbar);
        if (toolbar != null) {
            this.setSupportActionBar(toolbar);
            if (this.getSupportActionBar() != null) {
                this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                this.getSupportActionBar().setHomeButtonEnabled(false);
                this.getSupportActionBar().setTitle("");
            }
        }
        supportActionBar = this.getSupportActionBar();
        this.switchToStockToolbar(this.getString(R.string.app_name));

        String versionName = BuildConfig.VERSION_NAME;
        if (BuildConfig.DEBUG) {
            versionName = versionName + " - " + BuildConfig.GIT_HASH;
        }

        final AccountHeader header = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.material_drawer_header_background)
                .withProfileImagesVisible(false)
                .withSelectionListEnabledForSingleProfile(false)
                .addProfiles(
                        new ProfileDrawerItem()
                                .withName(this.getString(R.string.drawer_name))
                                .withEmail(versionName))
                .withCurrentProfileHiddenInList(true)
                .build();

        //LibsConfiguration.getInstance().setItemAnimator(new SlideDownAlphaAnimator());
        final LibsSupportFragment fragment = new LibsBuilder().supportFragment();

        final DrawerBuilder drawerBuilder = new DrawerBuilder();
        drawerBuilder.withActivity(this);

        if (toolbar != null) drawerBuilder.withToolbar(toolbar);
        drawerBuilder.withSavedInstance(savedInstanceState);
        drawerBuilder.withActionBarDrawerToggleAnimated(true);
        if (this.prefs.getBoolean("alternate_drawer_design", false)) {
            drawerBuilder.withRootView(R.id.drawer_container);
            drawerBuilder.withHeaderHeight(DimenHolder.fromDp(0));
        }
        drawerBuilder.withAccountHeader(header);


        // Split the community chats out for easy adapting
        final ExpandableDrawerItem social = new ExpandableDrawerItem()
                .withName(R.string.nav_drawer_community)
                .withIcon(R.drawable.nav_drawer_community)
                .withSelectable(false).withSubItems(
                        new SecondaryDrawerItem().withName(R.string.nav_drawer_googleplus)
                                .withLevel(2).withIcon(R.drawable.nav_drawer_googleplus)
                                .withSelectable(false)
                                .withIdentifier(100L),
                        new SecondaryDrawerItem().withName(R.string.nav_drawer_reddit)
                                .withLevel(2).withIcon(R.drawable.nav_reddit)
                                .withSelectable(false)
                                .withIdentifier(101L),
                        new SecondaryDrawerItem().withName(R.string.nav_drawer_telegram)
                                .withLevel(2).withIcon(R.drawable.nav_drawer_telegram)
                                .withSelectable(false)
                                .withIdentifier(102L),
                        new SecondaryDrawerItem().withName(R.string.nav_drawer_xda)
                                .withLevel(2).withIcon(R.drawable.nav_drawer_xda)
                                .withSelectable(false)
                                .withIdentifier(103L));

        // Split the featured content out for easy adapting
        final ExpandableDrawerItem featured = new ExpandableDrawerItem()
                .withName(R.string.nav_drawer_featured)
                .withIcon(R.drawable.nav_drawer_featured)
                .withSelectable(false).withSubItems(
                        new SecondaryDrawerItem().withName(R.string.nav_drawer_rawad)
                                .withLevel(2).withIcon(R.drawable.nav_drawer_youtube)
                                .withSelectable(false)
                                .withIdentifier(104L),
                        new SecondaryDrawerItem().withName(R.string.nav_drawer_xda_portal)
                                .withLevel(2).withIcon(R.drawable.nav_drawer_xda_portal)
                                .withSelectable(false)
                                .withIdentifier(105L));

        // Split the resources out for easy adapting
        final ExpandableDrawerItem resources = new ExpandableDrawerItem()
                .withName(R.string.nav_drawer_resources)
                .withIcon(R.drawable.nav_drawer_resources)
                .withSelectable(false).withSubItems(
                        new SecondaryDrawerItem().withName(R.string.nav_drawer_homepage)
                                .withLevel(2).withIcon(R.drawable.nav_drawer_homepage)
                                .withSelectable(false)
                                .withIdentifier(106L),
                        new SecondaryDrawerItem().withName(R.string.nav_drawer_template)
                                .withLevel(2).withIcon(R.drawable.nav_drawer_template)
                                .withSelectable(false)
                                .withIdentifier(107L),
                        new SecondaryDrawerItem().withName(R.string.nav_drawer_gerrit)
                                .withLevel(2).withIcon(R.drawable.nav_drawer_gerrit)
                                .withSelectable(false)
                                .withIdentifier(108L),
                        new SecondaryDrawerItem().withName(R.string.nav_drawer_github)
                                .withLevel(2).withIcon(R.drawable.nav_drawer_github)
                                .withSelectable(false)
                                .withIdentifier(109L),
                        new SecondaryDrawerItem().withName(R.string.nav_drawer_jira)
                                .withLevel(2).withIcon(R.drawable.nav_drawer_jira)
                                .withSelectable(false)
                                .withIdentifier(110L));

        // Begin initializing the navigation drawer
        drawerBuilder.addDrawerItems(
                new PrimaryDrawerItem()
                        .withName(R.string.nav_home)
                        .withIcon(R.drawable.nav_theme_packs)
                        .withIdentifier(1L));
        drawerBuilder.addDrawerItems(
                new PrimaryDrawerItem()
                        .withName(R.string.nav_overlays)
                        .withIcon(R.drawable.nav_overlays)
                        .withIdentifier(2L));
        if (Resources.isBootAnimationSupported(this.mContext))
            drawerBuilder.addDrawerItems(
                    new PrimaryDrawerItem()
                            .withName(R.string.nav_bootanim)
                            .withIcon(R.drawable.nav_bootanim)
                            .withIdentifier(3L));
        if (Resources.isShutdownAnimationSupported())
            drawerBuilder.addDrawerItems(
                    new PrimaryDrawerItem()
                            .withName(R.string.nav_shutdownanim)
                            .withIcon(R.drawable.nav_shutdownanim)
                            .withIdentifier(4L));
        if (Resources.isFontsSupported())
            drawerBuilder.addDrawerItems(
                    new PrimaryDrawerItem()
                            .withName(R.string.nav_fonts)
                            .withIcon(R.drawable.nav_fonts)
                            .withIdentifier(5L));
        if (Resources.isSoundsSupported(this.mContext))
            drawerBuilder.addDrawerItems(
                    new PrimaryDrawerItem()
                            .withName(R.string.nav_sounds)
                            .withIcon(R.drawable.nav_sounds)
                            .withIdentifier(6L));
        drawerBuilder.addDrawerItems(
                new PrimaryDrawerItem()
                        .withName(R.string.nav_wallpapers)
                        .withIcon(R.drawable.nav_wallpapers)
                        .withIdentifier(7L));
        drawerBuilder.addDrawerItems(
                new SectionDrawerItem()
                        .withName(R.string.nav_section_header_utilities));
        drawerBuilder.addDrawerItems(
                new PrimaryDrawerItem()
                        .withName(R.string.nav_overlay_manager)
                        .withIcon(R.drawable.nav_overlay_manager)
                        .withIdentifier(8L));
        if (Systems.checkOMS(this.mContext) && !isSamsung(this.mContext))
            drawerBuilder.addDrawerItems(
                    new PrimaryDrawerItem()
                            .withName(R.string.nav_priorities)
                            .withIcon(R.drawable.nav_drawer_priorities)
                            .withIdentifier(9L));
        if (Resources.isProfilesSupported(this.mContext))
            drawerBuilder.addDrawerItems(
                    new PrimaryDrawerItem()
                            .withName(R.string.nav_backup_restore)
                            .withIcon(R.drawable.nav_drawer_profiles)
                            .withIdentifier(10L)
                            .withBadge(this.getString(R.string.beta_tag)));
        drawerBuilder.addDrawerItems(
                new PrimaryDrawerItem()
                        .withName(R.string.nav_manage)
                        .withIcon(R.drawable.nav_manage)
                        .withIdentifier(11L));
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
                        .withIdentifier(12L));
        drawerBuilder.addDrawerItems(
                new SectionDrawerItem()
                        .withName(R.string.nav_section_header_more));
        drawerBuilder.addDrawerItems(
                new SecondaryDrawerItem()
                        .withName(R.string.nav_team_contributors)
                        .withIcon(R.drawable.nav_drawer_team)
                        .withIdentifier(13L));
        drawerBuilder.addDrawerItems(
                new SecondaryDrawerItem()
                        .withName(this.getString(R.string.nav_opensource))
                        .withIcon(R.drawable.nav_drawer_licenses)
                        .withIdentifier(14L));
        drawerBuilder.addDrawerItems(
                new SecondaryDrawerItem()
                        .withName(R.string.nav_settings)
                        .withIcon(R.drawable.nav_drawer_settings)
                        .withIdentifier(15L));
        drawerBuilder.withOnDrawerItemClickListener((view, position, drawerItem) -> {
            if (drawerItem != null) {
                switch ((int) drawerItem.getIdentifier()) {
                    case 1:
                        this.switchThemeFragment(((Systems.checkOMS(
                                this.mContext) ?
                                        this.getString(R.string.app_name) :
                                        (Systems.isSamsung(this.mContext) ?
                                                this.getString(R.string.samsung_app_name) :
                                                this.getString(R.string.legacy_app_name)))
                                ),
                                References.homeFragment);
                        break;
                    case 2:
                        this.switchThemeFragment(this.getString(R.string.nav_overlays),
                                References.overlaysFragment);
                        break;
                    case 3:
                        this.switchThemeFragment(this.getString(R.string.nav_bootanim),
                                References.bootAnimationsFragment);
                        break;
                    case 4:
                        this.switchThemeFragment(this.getString(R.string.nav_shutdownanim),
                                References.shutdownAnimationsFragment);
                        break;
                    case 5:
                        this.switchThemeFragment(this.getString(R.string.nav_fonts),
                                References.fontsFragment);
                        break;
                    case 6:
                        this.switchThemeFragment(this.getString(R.string.nav_sounds),
                                References.soundsFragment);
                        break;
                    case 7:
                        this.switchThemeFragment(this.getString(R.string.nav_wallpapers),
                                References.wallpaperFragment);
                        break;
                    case 8:
                        this.switchFragment(this.getString(R.string.nav_overlay_manager),
                                ManagerFragment.class.getCanonicalName());
                        break;
                    case 9:
                        this.switchFragment(this.getString(R.string.nav_priorities),
                                PriorityLoaderFragment.class.getCanonicalName());
                        break;
                    case 10:
                        this.switchFragment(this.getString(R.string.nav_backup_restore),
                                ProfileFragment.class.getCanonicalName());
                        break;
                    case 11:
                        this.switchFragment(this.getString(R.string.nav_manage),
                                RecoveryFragment.class.getCanonicalName());
                        break;
                    case 12:
                        this.switchFragment(this.getString(R.string.nav_troubleshooting),
                                TroubleshootingFragment.class.getCanonicalName());
                        break;
                    case 13:
                        this.switchFragment(this.getString(R.string.nav_team_contributors),
                                TeamFragment.class.getCanonicalName());
                        break;
                    case 14:
                        this.switchFragmentToLicenses(this.getString(R.string.nav_opensource),
                                fragment);
                        break;
                    case 15:
                        this.switchFragment(this.getString(R.string.nav_settings),
                                SettingsFragment.class.getCanonicalName());
                        break;
                    case 100:
                        launchActivityUrl(this.mContext, R.string.googleplus_link);
                        break;
                    case 101:
                        launchActivityUrl(this.mContext, R.string.reddit_link);
                        break;
                    case 102:
                        launchActivityUrl(this.mContext, R.string.telegram_link);
                        break;
                    case 103:
                        final int sourceURL;
                        if (Systems.isSamsung(this)) {
                            sourceURL = R.string.xda_sungstratum_link;
                        } else {
                            sourceURL = R.string.xda_link;
                        }
                        launchActivityUrl(this.mContext, sourceURL);
                        break;
                    case 104:
                        launchActivityUrl(this.mContext, R.string.rawad_youtube_url);
                        break;
                    case 105:
                        launchActivityUrl(this.mContext, R.string.xda_portal_link);
                        break;
                    case 106:
                        launchActivityUrl(this.mContext, R.string.homepage_link);
                        break;
                    case 107:
                        launchActivityUrl(this.mContext, R.string.template_link);
                        break;
                    case 108:
                        launchActivityUrl(this.mContext, R.string.gerrit_link);
                        break;
                    case 109:
                        launchActivityUrl(this.mContext, R.string.github_link);
                        break;
                    case 110:
                        launchActivityUrl(this.mContext, R.string.jira_link);
                        break;
                }
            }
            return false;
        });
        this.drawer = drawerBuilder.build();
        if ((this.getIntent() != null) && this.getIntent().getBooleanExtra("launch_manager_fragment", false)) {
            this.switchFragment(this.getString(R.string.nav_overlay_manager),
                    ManagerFragment.class.getCanonicalName());
            this.drawer.setSelection(8L);
        } else {
            this.drawer.setSelection((long) selectedDrawer, true);
        }

        new RootRequester(this).execute();
    }

    private void cleanLogCharReportsIfNecessary() {
        final Date currentDate = new Date(System.currentTimeMillis());
        if (this.prefs.getLong("previous_logchar_cleanup", 0L) == 0L) {
            this.prefs.edit().putLong("previous_logchar_cleanup", currentDate.getTime()).apply();
            return;
        }
        final long lastCleanupDate = this.prefs.getLong("previous_logchar_cleanup", 0L);
        final long diff = currentDate.getTime() - lastCleanupDate;
        if (TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) >= 15L) {
            this.prefs.edit().putLong("previous_logchar_cleanup", currentDate.getTime()).apply();
            new ClearLogs(this).execute();
            Log.d(SUBSTRATUM_LOG, "LogChar reports were wiped from the storage");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (this.permissionCheck == PackageManager.PERMISSION_GRANTED) {
            //add the values which need to be saved from the drawer to the bundle
            outState = this.drawer.saveInstanceState(outState);
            outState.putInt(SELECTED_DRAWER_ITEM, (int) this.drawer.getCurrentSelection());
            super.onSaveInstanceState(outState);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        this.getMenuInflater().inflate(R.menu.activity_menu, menu);

        final boolean isOMS = Systems.checkOMS(this.mContext);
        if (isOMS || isSamsung(this.mContext)) {
            menu.findItem(R.id.reboot_device).setVisible(false);
            menu.findItem(R.id.soft_reboot).setVisible(false);
        }
        if (!isOMS) menu.findItem(R.id.per_app).setVisible(false);

        searchItem = menu.findItem(R.id.action_search);
        this.searchView = (SearchView) searchItem.getActionView();
        this.searchView.setOnQueryTextListener(this);
        searchItem.setVisible(!this.hideBundle);
        final MenuItem restartUi = menu.findItem(R.id.restart_systemui);
        restartUi.setVisible(!this.hideRestartUi &&
                !Systems.checkAndromeda(this.mContext) &&
                (isOMS || Root.checkRootAccess()));
        return true;
    }

    @SuppressWarnings("LocalCanBeFinal")
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final SharedPreferences prefs = this.mContext.getSharedPreferences(
                "substratum_state", Context.MODE_PRIVATE);
        switch (item.getItemId()) {
            case android.R.id.home:
                this.onBackPressed();
                return true;
            case R.id.search:
                launchInternalActivity(this, ShowcaseActivity.class);
                return true;

            // Begin OMS based options
            case R.id.per_app:
                if (!References.isServiceRunning(SubstratumFloatInterface.class,
                        this.mContext)) {
                    if (Settings.canDrawOverlays(this.mContext) &&
                            checkUsagePermissions(this.mContext)) {
                        this.showFloatingHead();
                    } else if (!Settings.canDrawOverlays(this.mContext)) {
                        final DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    final Intent draw_over_apps = new Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:" + this.mContext
                                                    .getPackageName()));
                                    this.startActivityForResult(draw_over_apps,
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
                    } else if (!checkUsagePermissions(this.mContext)) {
                        final DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    final Intent usage = new Intent(Settings
                                            .ACTION_USAGE_ACCESS_SETTINGS);
                                    this.startActivityForResult(usage,
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
                    this.hideFloatingHead();
                }
                return true;

            case R.id.restart_systemui:
                DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            ThemeManager.restartSystemUI(this.mContext);
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(this.getString(R.string.dialog_restart_systemui_title));
                builder.setMessage(this.getString(R.string.dialog_restart_systemui_content));
                builder.setPositiveButton(
                        this.getString(R.string.restore_dialog_okay), dialogClickListener);
                builder.setNegativeButton(
                        this.getString(R.string.dialog_cancel), dialogClickListener);
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
                builder.setTitle(this.getString(R.string.dialog_restart_reboot_title));
                builder.setMessage(this.getString(R.string.dialog_restart_reboot_content));
                builder.setPositiveButton(
                        this.getString(R.string.restore_dialog_okay), dialogClickListener);
                builder.setNegativeButton(
                        this.getString(R.string.dialog_cancel), dialogClickListener);
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
                builder.setTitle(this.getString(R.string.dialog_restart_soft_reboot_title));
                builder.setMessage(this.getString(R.string.dialog_restart_soft_reboot_content));
                builder.setPositiveButton(
                        this.getString(R.string.restore_dialog_okay), dialogClickListener);
                builder.setNegativeButton(
                        this.getString(R.string.dialog_cancel), dialogClickListener);
                builder.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (!this.searchView.isIconified()) {
            this.searchView.setIconified(true);
        } else if ((this.drawer != null) && this.drawer.isDrawerOpen()) {
            this.drawer.closeDrawer();
        } else {
            final Fragment f = this.getSupportFragmentManager().findFragmentById(R.id.main);
            if (f instanceof PriorityListFragment) {
                final Fragment fragment = new PriorityLoaderFragment();
                final FragmentManager fm = this.getSupportFragmentManager();
                final FragmentTransaction transaction = fm.beginTransaction();
                transaction.setCustomAnimations(
                        android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                transaction.replace(R.id.main, fragment);
                transaction.commit();
            } else if ((this.drawer != null) && (this.drawer.getCurrentSelectedPosition() > 1)) {
                this.drawer.setSelectionAtPosition(1);
            } else if ((this.drawer != null) && (this.drawer.getCurrentSelectedPosition() == 1)) {
                this.finish();
            }
        }
    }

    @Override
    protected void attachBaseContext(final Context base) {
        Context newBase = base;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(base);
        final boolean languageCheck = this.prefs.getBoolean("force_english", false);
        if (languageCheck) {
            final Locale newLocale = new Locale(Locale.ENGLISH.getLanguage());
            newBase = ContextWrapper.wrapNewLocale(base, newLocale);
        }
        super.attachBaseContext(newBase);
    }

    private void showFloatingHead() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                this.mContext);
        prefs.edit().putInt("float_tile", Tile.STATE_ACTIVE).apply();
        FloatUiTile.requestListeningState(this.mContext,
                new ComponentName(this.mContext, FloatUiTile.class));
        this.mContext.startService(new Intent(this.mContext,
                SubstratumFloatInterface.class));
        final PackageManager packageManager = this.getPackageManager();
        final ComponentName componentName =
                new ComponentName(this.mContext, FloatUiTile.class);
        packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
        this.startService(new Intent(this, FloatUiTile.class));
    }

    private void hideFloatingHead() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                this.mContext);
        prefs.edit().putInt("float_tile", Tile.STATE_INACTIVE).apply();
        FloatUiTile.requestListeningState(this.mContext,
                new ComponentName(this.mContext, FloatUiTile.class));
        this.stopService(new Intent(this.mContext,
                SubstratumFloatInterface.class));
    }

    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_DRAW_OVER_OTHER_APPS:
                if (!checkUsagePermissions(this.mContext)) {
                    final Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                    this.startActivityForResult(intent, PERMISSIONS_REQUEST_USAGE_ACCESS_SETTINGS);
                } else {
                    if (Settings.canDrawOverlays(this.mContext) &&
                            checkUsagePermissions(this.mContext)) {
                        this.showFloatingHead();
                    }
                }
                break;
            case PERMISSIONS_REQUEST_USAGE_ACCESS_SETTINGS:
                if (Settings.canDrawOverlays(this.mContext) &&
                        checkUsagePermissions(this.mContext)) {
                    this.showFloatingHead();
                }
                break;
            case UNINSTALL_REQUEST_CODE:
                if ((queuedUninstall != null) && !queuedUninstall.isEmpty()) {
                    queuedUninstall.remove(0);
                    uninstallMultipleAPKS(this);
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        if (intent != null) {
            this.setIntent(intent);
            if (this.getIntent().getBooleanExtra("launch_manager_fragment", false)) {
                this.switchFragment(this.getString(R.string.nav_overlay_manager),
                        ManagerFragment.class.getCanonicalName());
                this.drawer.setSelection(8L);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions,
                                           @NonNull final int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                if ((grantResults.length > 0) &&
                        (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission already granted, allow the program to continue running
                    final File directory = new File(EXTERNAL_STORAGE_CACHE);
                    if (directory.exists()) {
                        final Boolean deleted = directory.delete();
                        if (!deleted) Log.e(References.SUBSTRATUM_LOG,
                                "Unable to delete directory");
                    } else {
                        Log.d(References.SUBSTRATUM_LOG, "Deleting old cache dir: " + directory);
                    }
                    if (!directory.exists()) {
                        final Boolean made = directory.mkdirs();
                        if (!made) Log.e(References.SUBSTRATUM_LOG,
                                "Unable to create directory");
                    } else {
                        References.injectRescueArchives(this.mContext);
                        Log.d(References.SUBSTRATUM_LOG, "Successfully made dir: " + directory);
                    }
                    final File cacheDirectory = new File(this.getCacheDir(),
                            SUBSTRATUM_BUILDER_CACHE);
                    if (!cacheDirectory.exists()) {
                        final Boolean made = cacheDirectory.mkdirs();
                        if (!made) Log.e(References.SUBSTRATUM_LOG,
                                "Unable to create cache directory");
                    }
                    final File[] fileList = new File(this.getCacheDir().getAbsolutePath() +
                            SUBSTRATUM_BUILDER_CACHE).listFiles();
                    for (final File file : fileList) {
                        FileOperations.delete(this.mContext, this.getCacheDir()
                                .getAbsolutePath() +
                                SUBSTRATUM_BUILDER_CACHE + file.getName());
                    }
                    Log.d("SubstratumBuilder", "The cache has been flushed!");
                    References.injectRescueArchives(this.mContext);
                } else {
                    // permission was not granted, show closing dialog
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.permission_not_granted_dialog_title)
                            .setMessage(R.string.permission_not_granted_dialog_message1)
                            .setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
                                if (this.shouldShowRequestPermissionRationale(
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
    public boolean onQueryTextChange(final String query) {
        if (!userInput.equals(query)) {
            userInput = query;
            final Fragment f = this.getSupportFragmentManager().findFragmentById(R.id.main);
            this.getSupportFragmentManager()
                    .beginTransaction()
                    .detach(f)
                    .commitNowAllowingStateLoss();
            this.getSupportFragmentManager()
                    .beginTransaction()
                    .attach(f)
                    .commitAllowingStateLoss();
        }
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(final String query) {
        return false;
    }

    private static final class RootRequester extends AsyncTask<Void, Void, Boolean> {
        boolean isRunning = true;
        private final WeakReference<MainActivity> ref;

        private RootRequester(final MainActivity activity) {
            super();
            this.ref = new WeakReference<>(activity);
        }

        private void permissionCheck() {
            final MainActivity activity = this.ref.get();
            if (activity != null) {
                final Context context = activity.mContext;
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
                        final AlertDialog.Builder alert = new AlertDialog.Builder(activity);
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
            final MainActivity activity = this.ref.get();
            if (activity != null) {
                final Context context = activity.mContext;
                this.showDialogOrNot(dialogReturnBool);
                if (!dialogReturnBool) this.permissionCheck();
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

        private void showDialogOrNot(final Boolean passthrough) {
            final MainActivity activity = this.ref.get();
            this.isRunning = false;
            if (activity != null) {
                final Context context = activity.mContext;
                if (passthrough) {
                    activity.mProgressDialog.show();
                    activity.mProgressDialog.setContentView(R.layout.root_rejected_loader);

                    final TextView titleView = activity.mProgressDialog.findViewById(R.id.title);
                    final TextView textView =
                            activity.mProgressDialog.findViewById(R.id.root_rejected_text);
                    if (Systems.isSamsungDevice(context)) {
                        final TextView samsungTitle = activity.mProgressDialog.findViewById(
                                R.id.sungstratum_title);
                        samsungTitle.setVisibility(View.VISIBLE);
                        final Button samsungButton = activity.mProgressDialog.findViewById(
                                R.id.sungstratum_button);
                        samsungButton.setVisibility(View.VISIBLE);
                        samsungButton.setOnClickListener(view ->
                                launchActivityUrl(context, R.string.sungstratum_url));
                        textView.setVisibility(View.GONE);
                        titleView.setVisibility(View.GONE);
                    } else if (Systems.isAndromedaDevice(context) &&
                            !AndromedaService.checkServerActivity()) {
                        final TextView andromedaTitle = activity.mProgressDialog.findViewById(
                                R.id.andromeda_title);
                        andromedaTitle.setText(R.string.andromeda_disconnected);
                        andromedaTitle.setVisibility(View.VISIBLE);
                        final Button andromedaButton = activity.mProgressDialog.findViewById(
                                R.id.andromeda_button);
                        andromedaButton.setText(R.string.andromeda_check_status);
                        andromedaButton.setVisibility(View.VISIBLE);
                        andromedaButton.setOnClickListener(view ->
                                launchExternalActivity(context, ANDROMEDA_PACKAGE, "InfoActivity"));
                        textView.setVisibility(View.GONE);
                        titleView.setVisibility(View.GONE);
                    } else if (Systems.checkOreo() &&
                            !Packages.isPackageInstalled(context, ANDROMEDA_PACKAGE)) {
                        final TextView andromedaTitle = activity.mProgressDialog.findViewById(
                                R.id.andromeda_title);
                        andromedaTitle.setVisibility(View.VISIBLE);
                        final Button andromedaButton = activity.mProgressDialog.findViewById(
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
        protected Boolean doInBackground(final Void... sUrl) {
            final MainActivity activity = this.ref.get();
            if (activity != null) {
                final Context context = activity.mContext;
                final int themeSystemModule = checkThemeSystemModule(context, true);

                // Samsung mode, but what if package is not installed?
                final boolean samsungCheck = themeSystemModule == SAMSUNG_THEME_ENGINE_N;
                if (samsungCheck) {
                    // Throw the dialog when sungstratum addon is not installed
                    return !Packages.isPackageInstalled(context, SST_ADDON_PACKAGE);
                }

                // Check if the system is Andromeda mode
                final boolean andromeda_check =
                        themeSystemModule == OVERLAY_MANAGER_SERVICE_O_ANDROMEDA;
                if (andromeda_check) {
                    Log.d("lmao", "here1");
                    // Throw the dialog when checkServerActivity() isn't working
                    return !AndromedaService.checkServerActivity();
                }

                // Check if the system is legacy
                final boolean legacyCheck =
                        themeSystemModule == NO_THEME_ENGINE;
                if (legacyCheck) {
                    // Throw the dialog, after checking for root
                    return !Root.requestRootAccess();
                }

                // Check for OMS
                final boolean omsCheck = Systems.checkOMS(context);
                if (omsCheck) {
                    return (themeSystemModule != OVERLAY_MANAGER_SERVICE_O_UNROOTED) &&
                            (themeSystemModule != OVERLAY_MANAGER_SERVICE_N_UNROOTED) &&
                            !Root.requestRootAccess();
                }
            }
            return false;
        }
    }

    public static class DoCleanUp extends AsyncTask<Void, Void, Void> {
        private final WeakReference<Context> ref;

        public DoCleanUp(final Context context) {
            super();
            this.ref = new WeakReference<>(context);
        }

        @Override
        protected void onPostExecute(final Void result) {
            super.onPostExecute(result);
        }

        @Override
        protected Void doInBackground(final Void... sUrl) {
            final Context context = this.ref.get();
            if (context != null) {
                final ArrayList<String> removeList = new ArrayList<>();
                // Overlays with non-existent targets
                final List<String> state1 = ThemeManager.listOverlays(
                        context, ThemeManager.STATE_MISSING_TARGET);
                // Uninstall overlays when the main theme is not present,
                // regardless if enabled/disabled

                final List<String> stateAll = ThemeManager.listAllOverlays(context);
                // We need the null check because listOverlays never returns null, but empty
                if (!state1.isEmpty() && (state1.get(0) != null)) {
                    for (int i = 0; i < state1.size(); i++) {
                        Log.e("OverlayCleaner",
                                "Target APK not found for \"" + state1.get(i) +
                                        "\" and will be removed.");
                        removeList.add(state1.get(i));
                    }
                }

                for (int i = 0; i < stateAll.size(); i++) {
                    final String parent = Packages.getOverlayParent(context, stateAll.get(i));
                    if (parent != null) {
                        if (!Packages.isPackageInstalled(context, parent)) {
                            Log.e("OverlayCleaner",
                                    "Parent APK not found for \"" + stateAll.get(i) +
                                            "\" and will be removed.");
                            removeList.add(stateAll.get(i));
                        }
                    }
                }

                if (!removeList.isEmpty())
                    ThemeManager.uninstallOverlay(context, removeList);
            }
            return null;
        }
    }

    public static class ClearLogs extends AsyncTask<Void, Void, Void> {
        private final WeakReference<MainActivity> ref;

        ClearLogs(final MainActivity activity) {
            super();
            this.ref = new WeakReference<>(activity);
        }

        @Override
        protected Void doInBackground(final Void... params) {
            final MainActivity activity = this.ref.get();
            if (activity != null) {
                final Context context = activity.mContext;
                delete(context, new File(LOGCHAR_DIR).getAbsolutePath());
            }
            return null;
        }

        @Override
        protected void onPostExecute(final Void result) {
            final MainActivity activity = this.ref.get();
            if (activity != null) {
                final Context context = activity.mContext;
                Toast.makeText(context, context.getString(R.string.cleaned_logchar_reports),
                        Toast.LENGTH_SHORT).show();
                final Intent intent = activity.getIntent();
                activity.finishAffinity();
                context.startActivity(intent);
            }
        }

    }

    class KillReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            MainActivity.this.finish();
        }
    }

    class AndromedaReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            final RootRequester rootRequester = new RootRequester(MainActivity.this);
            rootRequester.execute();
        }
    }
}
