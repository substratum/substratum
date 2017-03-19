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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.flaviofaria.kenburnsview.KenBurnsView;
import com.theartofdev.edmodo.cropper.CropImage;

import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import projekt.substratum.adapters.InformationTabsAdapter;
import projekt.substratum.config.ElevatedCommands;
import projekt.substratum.config.FileOperations;
import projekt.substratum.config.FirebaseAnalytics;
import projekt.substratum.config.References;
import projekt.substratum.config.ThemeManager;
import projekt.substratum.config.WallpaperManager;
import projekt.substratum.util.SheetDialog;

public class InformationActivity extends AppCompatActivity {

    private static final int THEME_INFORMATION_REQUEST_CODE = 1;
    public static String theme_name, theme_pid, theme_mode;
    private static List<String> tab_checker;
    private static String wallpaperUrl;
    private Boolean refresh_mode = false;
    private Boolean uninstalled = false;
    private KenBurnsView kenBurnsView;
    private byte[] byteArray;
    private Bitmap heroImageBitmap;
    private SharedPreferences prefs;
    private AppBarLayout appBarLayout;
    private CollapsingToolbarLayout collapsingToolbarLayout;
    private View gradientView;
    private TabLayout tabLayout;
    private ProgressDialog mProgressDialog;
    private MenuItem favorite;
    private boolean shouldDarken;

    public static String getThemeName() {
        return theme_name;
    }

    public static String getThemePID() {
        return theme_pid;
    }

    public static String getWallpaperUrl() {
        return wallpaperUrl;
    }

    private static int getDominantColor(Bitmap bitmap) {
        try {
            return bitmap.getPixel(0, 0);
        } catch (IllegalArgumentException iae) {
            return Color.BLACK;
        }
    }

    private static void setOverflowButtonColor(final Activity activity, final Boolean dark_mode) {
        @SuppressLint("PrivateResource") final String overflowDescription =
                activity.getString(R.string.abc_action_menu_overflow_description);
        final ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        final ViewTreeObserver viewTreeObserver = decorView.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(() -> {
            final ArrayList<View> outViews = new ArrayList<>();
            decorView.findViewsWithText(outViews, overflowDescription,
                    View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
            if (outViews.isEmpty()) {
                return;
            }
            AppCompatImageView overflow = (AppCompatImageView) outViews.get(0);
            if (dark_mode) {
                overflow.setImageResource(
                        activity.getResources().getIdentifier(
                                "information_activity_overflow_dark",
                                "drawable",
                                activity.getPackageName()));
            } else {
                overflow.setImageResource(
                        activity.getResources().getIdentifier(
                                "information_activity_overflow_light",
                                "drawable",
                                activity.getPackageName()));
            }
        });
    }

    private boolean checkColorDarkness(int color) {
        double darkness =
                1 - (0.299 * Color.red(color) +
                        0.587 * Color.green(color) +
                        0.114 * Color.blue(color)) / 255;
        return darkness < 0.5;
    }

    private Drawable grabPackageHeroImage(String package_name) {
        Resources res;
        Drawable hero = null;
        try {
            res = getPackageManager().getResourcesForApplication(package_name);
            int resourceId = res.getIdentifier(package_name + ":drawable/heroimage", null, null);
            if (0 != resourceId) {
                hero = getPackageManager().getDrawable(package_name, resourceId, null);
            }
            return hero;
        } catch (Exception e) {
            // Exception
        }
        return null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Image Cropper Request Capture
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                SharedPreferences.Editor editor = prefs.edit();
                Uri resultUri = result.getUri();
                if (resultUri.toString().contains("homescreen_wallpaper")) {
                    try {
                        WallpaperManager.setWallpaper(getApplicationContext(),
                                resultUri.toString().substring(7), "home");
                        editor.putString("home_wallpaper_applied", theme_pid);
                        Snackbar.make(findViewById(android.R.id.content),
                                getString(R.string.wallpaper_homescreen_success),
                                Snackbar.LENGTH_LONG)
                                .show();
                    } catch (IOException e) {
                        Snackbar.make(findViewById(android.R.id.content),
                                getString(R.string.wallpaper_homescreen_error),
                                Snackbar.LENGTH_LONG)
                                .show();
                        e.printStackTrace();
                    }
                } else if (resultUri.toString().contains("lockscreen_wallpaper")) {
                    try {
                        WallpaperManager.setWallpaper(getApplicationContext(),
                                resultUri.toString().substring(7), "lock");
                        editor.putString("lock_wallpaper_applied", theme_pid);
                        Snackbar.make(findViewById(android.R.id.content),
                                getString(R.string.wallpaper_lockscreen_success),
                                Snackbar.LENGTH_LONG)
                                .show();
                    } catch (IOException e) {
                        Snackbar.make(findViewById(android.R.id.content),
                                getString(R.string.wallpaper_lockscreen_error),
                                Snackbar.LENGTH_LONG)
                                .show();
                        e.printStackTrace();
                    }
                } else if (resultUri.toString().contains("all_wallpaper")) {
                    try {
                        WallpaperManager.setWallpaper(getApplicationContext(),
                                resultUri.toString().substring(7), "all");
                        editor.putString("home_wallpaper_applied", theme_pid);
                        editor.putString("lock_wallpaper_applied", theme_pid);
                        Snackbar.make(findViewById(android.R.id.content),
                                getString(R.string.wallpaper_allscreen_success),
                                Snackbar.LENGTH_LONG)
                                .show();
                    } catch (IOException e) {
                        Snackbar.make(findViewById(android.R.id.content),
                                getString(R.string.wallpaper_allscreen_error),
                                Snackbar.LENGTH_LONG)
                                .show();
                        e.printStackTrace();
                    }
                }
                editor.apply();
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Log.e("SubstratumImageCropper",
                        "There has been an error processing the image...");
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.information_activity);

        prefs = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());

        boolean dynamicActionBarColors = getResources().getBoolean(R.bool.dynamicActionBarColors);
        boolean dynamicNavBarColors = getResources().getBoolean(R.bool.dynamicNavigationBarColors);

        Intent currentIntent = getIntent();
        theme_name = currentIntent.getStringExtra("theme_name");
        theme_pid = currentIntent.getStringExtra("theme_pid");
        theme_mode = currentIntent.getStringExtra("theme_mode");
        Boolean theme_legacy = currentIntent.getBooleanExtra("theme_legacy", false);
        refresh_mode = currentIntent.getBooleanExtra("refresh_mode", false);
        wallpaperUrl = null;
        String theme_author = References.grabThemeAuthor(getApplicationContext(), theme_pid);

        try {
            ApplicationInfo appInfo = getApplicationContext()
                    .getPackageManager().getApplicationInfo(
                            theme_pid, PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                if (appInfo.metaData.getString("Substratum_Wallpapers") != null) {
                    wallpaperUrl = appInfo.metaData.getString("Substratum_Wallpapers");
                }
            }
        } catch (Exception e) {
            // NameNotFound
        }

        if (theme_mode == null) {
            theme_mode = "";
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) toolbar.setTitle(theme_name);

        gradientView = findViewById(R.id.gradientView);
        collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById
                (R.id.collapsing_toolbar_tabbed_layout);
        if (collapsingToolbarLayout != null) collapsingToolbarLayout.setTitle(theme_name);

        final ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        if (toolbar != null) toolbar.setNavigationOnClickListener(v -> onBackPressed());

        Drawable heroImage = grabPackageHeroImage(theme_pid);
        if (heroImage != null) heroImageBitmap = ((BitmapDrawable) heroImage).getBitmap();
        int dominantColor = getDominantColor(heroImageBitmap);

        appBarLayout = (AppBarLayout) findViewById(R.id.appbar);
        appBarLayout.setBackgroundColor(dominantColor);

        if (collapsingToolbarLayout != null && dynamicActionBarColors &&
                prefs.getBoolean("dynamic_actionbar", true)) {
            collapsingToolbarLayout.setStatusBarScrimColor(dominantColor);
            collapsingToolbarLayout.setContentScrimColor(dominantColor);
        }

        if (dynamicNavBarColors && prefs.getBoolean("dynamic_navbar", true)) {
            getWindow().setNavigationBarColor(dominantColor);
            if (checkColorDarkness(dominantColor)) {
                getWindow().setNavigationBarColor(
                        getColor(R.color.theme_information_background));
            }
        }

        if (References.isOffensive(theme_name)) {
            FirebaseAnalytics.backupDebuggableStatistics(
                    getApplicationContext(),
                    "bannable-offence",
                    References.getDeviceID(getApplicationContext()),
                    theme_name);
        } else if (
                References.isOffensive(theme_author)) {
            FirebaseAnalytics.backupDebuggableStatistics(
                    getApplicationContext(),
                    "bannable-offence",
                    References.getDeviceID(getApplicationContext()),
                    theme_author);
        }

        new LayoutLoader().execute("");

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        if (tabLayout != null) {
            if (theme_mode.equals("")) {
                try {
                    Context otherContext = getApplicationContext().createPackageContext
                            (theme_pid, 0);
                    AssetManager am = otherContext.getAssets();
                    List found_folders = Arrays.asList(am.list(""));
                    tab_checker = new ArrayList<>();
                    if (!References.checkOMS(getApplicationContext())) {
                        for (int i = 0; i < found_folders.size(); i++) {
                            if (References.allowedForLegacy(found_folders.get(i).toString())) {
                                tab_checker.add(found_folders.get(i).toString());
                            }
                        }
                    } else {
                        tab_checker = Arrays.asList(am.list(""));
                    }
                    if (tab_checker.contains("overlays") ||
                            tab_checker.contains("overlays_legacy")) {
                        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                                .theme_information_tab_one)));
                    }
                    if (tab_checker.contains("bootanimation")) {
                        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                                .theme_information_tab_two)));
                    }
                    if (tab_checker.contains("fonts")) {
                        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                                .theme_information_tab_three)));
                    }
                    if (tab_checker.contains("audio")) {
                        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                                .theme_information_tab_four)));
                    }
                    if (wallpaperUrl != null) {
                        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                                .theme_information_tab_five)));
                    }
                } catch (Exception e) {
                    Log.e(References.SUBSTRATUM_LOG, "Could not refresh list of asset folders.");
                }
            } else {
                if (theme_mode.equals("overlays")) {
                    tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                            .theme_information_tab_one)));
                } else {
                    if (theme_mode.equals("bootanimation")) {
                        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                                .theme_information_tab_two)));
                    } else {
                        if (theme_mode.equals("fonts")) {
                            tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                                    .theme_information_tab_three)));
                        } else {
                            if (theme_mode.equals("audio")) {
                                tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                                        .theme_information_tab_four)));
                            } else {
                                if (theme_mode.equals("wallpapers")) {
                                    tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                                            .theme_information_tab_five)));
                                }
                            }
                        }
                    }
                }
            }

            tabLayout.setTabGravity(TabLayout.MODE_SCROLLABLE);
            if (dynamicActionBarColors && prefs.getBoolean("dynamic_actionbar", true))
                tabLayout.setBackgroundColor(dominantColor);

            if (collapsingToolbarLayout != null && checkColorDarkness(dominantColor) &&
                    dynamicActionBarColors && prefs.getBoolean("dynamic_actionbar", true)) {
                collapsingToolbarLayout.setCollapsedTitleTextColor(
                        getColor(R.color.information_activity_dark_icon_mode));
                collapsingToolbarLayout.setExpandedTitleColor(
                        getColor(R.color.information_activity_dark_icon_mode));
                tabLayout.setTabTextColors(
                        getColor(R.color.information_activity_dark_text_mode),
                        getColor(R.color.information_activity_dark_text_mode));

                Drawable upArrow = getDrawable(R.drawable.information_activity_back_dark);
                if (upArrow != null)
                    upArrow.setColorFilter(getColor(R.color.information_activity_dark_icon_mode),
                            PorterDuff.Mode.SRC_ATOP);
                getSupportActionBar().setHomeAsUpIndicator(upArrow);
                setOverflowButtonColor(this, true);
            } else if (collapsingToolbarLayout != null) {
                collapsingToolbarLayout.setCollapsedTitleTextColor(
                        getColor(R.color.information_activity_light_icon_mode));
                collapsingToolbarLayout.setExpandedTitleColor(
                        getColor(R.color.information_activity_light_icon_mode));
                tabLayout.setTabTextColors(
                        getColor(R.color.information_activity_light_text_mode),
                        getColor(R.color.information_activity_light_text_mode));

                Drawable upArrow = getDrawable(R.drawable.information_activity_back_light);
                if (upArrow != null)
                    upArrow.setColorFilter(getColor(R.color.information_activity_light_icon_mode),
                            PorterDuff.Mode.SRC_ATOP);
                getSupportActionBar().setHomeAsUpIndicator(upArrow);
                setOverflowButtonColor(this, false);
            }
        }
        final InformationTabsAdapter adapter = new InformationTabsAdapter
                (getSupportFragmentManager(), (tabLayout != null) ? tabLayout.getTabCount() : 0,
                        theme_mode, tab_checker, wallpaperUrl);

        if (viewPager != null) {
            viewPager.setOffscreenPageLimit((tabLayout != null) ? tabLayout.getTabCount() : 0);
            viewPager.setAdapter(adapter);
            viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener
                    (tabLayout));
            if (tabLayout != null) tabLayout.addOnTabSelectedListener(
                    new TabLayout.OnTabSelectedListener() {
                        @Override
                        public void onTabSelected(TabLayout.Tab tab) {
                            viewPager.setCurrentItem(tab.getPosition());
                        }

                        @Override
                        public void onTabUnselected(TabLayout.Tab tab) {
                        }

                        @Override
                        public void onTabReselected(TabLayout.Tab tab) {
                        }
                    });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.theme_information_menu, menu);

        // Start formalizing a check for dark icons
        Drawable heroImage = grabPackageHeroImage(theme_pid);
        if (heroImage != null) heroImageBitmap = ((BitmapDrawable) heroImage).getBitmap();
        int dominantColor = getDominantColor(heroImageBitmap);
        boolean dynamicActionBarColors = getResources().getBoolean(R.bool.dynamicActionBarColors);
        shouldDarken = collapsingToolbarLayout != null &&
                checkColorDarkness(dominantColor) &&
                dynamicActionBarColors && prefs.getBoolean("dynamic_actionbar", true);

        // Start dynamically showing menu items
        boolean isOMS = References.checkOMS(getApplicationContext());
        boolean isMR1orHigher = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1;

        if (!isMR1orHigher) menu.findItem(R.id.favorite).setVisible(false);
        if (isMR1orHigher) {
            favorite = menu.findItem(R.id.favorite);
            if (prefs.contains("app_shortcut_theme")) {
                if (prefs.getString("app_shortcut_theme", "").equals(theme_pid)) {
                    favorite.setIcon(getDrawable(R.drawable.toolbar_favorite));
                } else {
                    favorite.setIcon(getDrawable(R.drawable.toolbar_not_favorite));
                }
            } else {
                favorite.setIcon(getDrawable(R.drawable.toolbar_not_favorite));
            }
            if (shouldDarken) favorite.getIcon().setColorFilter(
                    getColor(R.color.information_activity_dark_icon_mode),
                    PorterDuff.Mode.SRC_ATOP);
        }
        if (References.grabThemeChangelog(getApplicationContext(), theme_pid) != null) {
            MenuItem changelog = menu.findItem(R.id.changelog);
            changelog.setVisible(true);
            if (shouldDarken) changelog.getIcon().setColorFilter(
                    getColor(R.color.information_activity_dark_icon_mode),
                    PorterDuff.Mode.SRC_ATOP);
        }

        if (!isOMS) menu.findItem(R.id.disable).setVisible(false);
        if (!isOMS) menu.findItem(R.id.enable).setVisible(false);
        if (!isOMS) menu.findItem(R.id.restart_systemui).setVisible(false);
        if (isOMS) menu.findItem(R.id.reboot_device).setVisible(false);
        if (isOMS) menu.findItem(R.id.soft_reboot).setVisible(false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.favorite:
                if (prefs.contains("app_shortcut_theme")) {
                    if (prefs.getString("app_shortcut_theme", "").equals(theme_pid)) {
                        new AppShortcutClearer().execute("");
                    } else {
                        new AppShortcutCreator().execute("favorite");
                    }
                } else {
                    new AppShortcutCreator().execute("favorite");
                }
                return true;
            case R.id.changelog:
                SheetDialog sheetDialog = new SheetDialog(this);
                @SuppressLint("InflateParams")
                View sheetView = getLayoutInflater().inflate(R.layout.changelog_sheet_dialog, null);

                LinearLayout titleBox = (LinearLayout) sheetView.findViewById(R.id.title_box);
                TextView title = (TextView) titleBox.findViewById(R.id.title);
                String format_me = String.format(getString(R.string.changelog_title), theme_name);
                title.setText(format_me);

                LinearLayout textBox = (LinearLayout) sheetView.findViewById(R.id.text_box);
                TextView text = (TextView) textBox.findViewById(R.id.text);

                String[] changelog_parsing =
                        References.grabThemeChangelog(getApplicationContext(), theme_pid);
                String to_show = "";
                if (changelog_parsing != null) {
                    for (String aChangelog_parsing : changelog_parsing) {
                        to_show += "\u2022 " + aChangelog_parsing + "\n";
                    }
                }
                text.setText(to_show);
                sheetDialog.setCanceledOnTouchOutside(true);
                sheetDialog.setContentView(sheetView);
                sheetDialog.show();
                return true;
            case R.id.clean:
                AlertDialog.Builder builder1 = new AlertDialog.Builder(InformationActivity.this);
                builder1.setTitle(theme_name);
                builder1.setIcon(References.grabAppIcon(getApplicationContext(), theme_pid));
                builder1.setMessage(R.string.clean_dialog_body)
                        .setPositiveButton(R.string.uninstall_dialog_okay, (dialog, id18) -> {
                            // Get all installed overlays
                            List<String> stateAll = ThemeManager.listOverlays(4);
                            stateAll.addAll(ThemeManager.listOverlays(5));

                            ArrayList<String> all_overlays = new ArrayList<>();
                            for (int j = 0; j < stateAll.size(); j++) {
                                try {
                                    String current = stateAll.get(j);
                                    ApplicationInfo appInfo = getApplicationContext()
                                            .getPackageManager().getApplicationInfo(
                                                    current, PackageManager.GET_META_DATA);
                                    if (appInfo.metaData != null &&
                                            appInfo.metaData.getString("Substratum_Parent") !=
                                                    null) {
                                        String parent = appInfo.metaData.getString
                                                ("Substratum_Parent");
                                        if (parent != null && parent.equals(theme_pid)) {
                                            all_overlays.add(current);
                                        }
                                    }
                                } catch (Exception e) {
                                    // NameNotFound
                                }
                            }

                            Toast toast = Toast.makeText(getApplicationContext(),
                                    getString(R.string
                                            .clean_completion),
                                    Toast.LENGTH_LONG);
                            toast.show();

                            // Begin uninstalling overlays for this package
                            ThemeManager.uninstallOverlay(getApplicationContext(), all_overlays);
                        })
                        .setNegativeButton(R.string.uninstall_dialog_cancel, (dialog, id19) -> {
                            // User cancelled the dialog
                        });
                // Create the AlertDialog object and return it
                builder1.create();
                builder1.show();
                return true;
            case R.id.clean_cache:
                AlertDialog.Builder builder2 = new AlertDialog.Builder(InformationActivity.this);
                builder2.setTitle(theme_name);
                builder2.setIcon(References.grabAppIcon(getApplicationContext(), theme_pid));
                builder2.setMessage(R.string.clean_cache_dialog_body)
                        .setPositiveButton(R.string.uninstall_dialog_okay, (dialog, id110) -> {
                            FileOperations.delete(getApplicationContext(), getCacheDir()
                                    .getAbsolutePath() +
                                    "/SubstratumBuilder/" + theme_pid + "/");
                            String format =
                                    String.format(
                                            getString(R.string.cache_clear_completion), theme_name);
                            Toast toast = Toast.makeText(getApplicationContext(), format,
                                    Toast.LENGTH_LONG);
                            toast.show();
                            finish();
                        })
                        .setNegativeButton(R.string.uninstall_dialog_cancel, (dialog, id17) ->
                                dialog
                                        .cancel());
                // Create the AlertDialog object and return it
                builder2.create();
                builder2.show();
                return true;
            case R.id.disable:
                AlertDialog.Builder builder3 = new AlertDialog.Builder(InformationActivity.this);
                builder3.setTitle(theme_name);
                builder3.setIcon(References.grabAppIcon(getApplicationContext(), theme_pid));
                builder3.setMessage(R.string.disable_dialog_body)
                        .setPositiveButton(R.string.uninstall_dialog_okay, (dialog, id16) -> {
                            // Get all enabled overlays
                            List<String> stateAll = ThemeManager.listOverlays(5);

                            ArrayList<String> all_overlays = new ArrayList<>();
                            for (int j = 0; j < stateAll.size(); j++) {
                                try {
                                    String current = stateAll.get(j);
                                    ApplicationInfo appInfo = getApplicationContext()
                                            .getPackageManager().getApplicationInfo(
                                                    current, PackageManager.GET_META_DATA);
                                    if (appInfo.metaData != null &&
                                            appInfo.metaData.getString("Substratum_Parent") !=
                                                    null) {
                                        String parent = appInfo.metaData.getString
                                                ("Substratum_Parent");
                                        if (parent != null && parent.equals(theme_pid)) {
                                            all_overlays.add(current);
                                        }
                                    }
                                } catch (Exception e) {
                                    // NameNotFound
                                }
                            }

                            Toast toast = Toast.makeText(getApplicationContext(),
                                    getString(R.string
                                            .disable_completion),
                                    Toast.LENGTH_LONG);
                            toast.show();

                            // Begin disabling overlays
                            ThemeManager.disableOverlay(getApplicationContext(), all_overlays);
                        })
                        .setNegativeButton(R.string.uninstall_dialog_cancel, (dialog, id15) -> {
                            // User cancelled the dialog
                        });
                // Create the AlertDialog object and return it
                builder3.create();
                builder3.show();
                return true;
            case R.id.enable:
                AlertDialog.Builder builder4 = new AlertDialog.Builder(InformationActivity.this);
                builder4.setTitle(theme_name);
                builder4.setIcon(References.grabAppIcon(getApplicationContext(), theme_pid));
                builder4.setMessage(R.string.enable_dialog_body)
                        .setPositiveButton(R.string.uninstall_dialog_okay, (dialog, id14) -> {
                            // Get all disabled overlays
                            List<String> stateAll = ThemeManager.listOverlays(4);

                            ArrayList<String> all_overlays = new ArrayList<>();
                            for (int j = 0; j < stateAll.size(); j++) {
                                try {
                                    String current = stateAll.get(j);
                                    ApplicationInfo appInfo = getApplicationContext()
                                            .getPackageManager().getApplicationInfo(
                                                    current, PackageManager.GET_META_DATA);
                                    if (appInfo.metaData != null &&
                                            appInfo.metaData.getString("Substratum_Parent") !=
                                                    null) {
                                        String parent = appInfo.metaData.getString
                                                ("Substratum_Parent");
                                        if (parent != null && parent.equals(theme_pid)) {
                                            all_overlays.add(current);
                                        }
                                    }
                                } catch (Exception e) {
                                    // NameNotFound
                                }
                            }

                            Toast toast = Toast.makeText(getApplicationContext(),
                                    getString(R.string
                                            .enable_completion),
                                    Toast.LENGTH_LONG);
                            toast.show();

                            // Begin enabling overlays
                            ThemeManager.enableOverlay(getApplicationContext(), all_overlays);
                        })
                        .setNegativeButton(R.string.uninstall_dialog_cancel, (dialog, id13) ->
                                dialog
                                        .cancel());
                // Create the AlertDialog object and return it
                builder4.create();
                builder4.show();
                return true;
            case R.id.rate:
                try {
                    String playURL = "https://play.google.com/store/apps/details?id=" + theme_pid;
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(playURL));
                    startActivity(i);
                } catch (ActivityNotFoundException activityNotFoundException) {
                    Snackbar.make(findViewById(android.R.id.content),
                            getString(R.string.activity_missing_toast),
                            Snackbar.LENGTH_LONG)
                            .show();
                }
                return true;
            case R.id.uninstall:
                AlertDialog.Builder builder5 = new AlertDialog.Builder(InformationActivity.this);
                builder5.setTitle(theme_name);
                builder5.setIcon(References.grabAppIcon(getApplicationContext(), theme_pid));
                builder5.setMessage(R.string.uninstall_dialog_text)
                        .setPositiveButton(R.string.uninstall_dialog_okay, (dialog, id12) -> new
                                uninstallTheme().execute(""))
                        .setNegativeButton(R.string.uninstall_dialog_cancel, (dialog, id1) -> {
                            // User cancelled the dialog
                        });
                // Create the AlertDialog object and return it
                builder5.create();
                builder5.show();
                return true;
            case R.id.restart_systemui:
                ThemeManager.restartSystemUI(getApplicationContext());
                return true;
            case R.id.reboot_device:
                ElevatedCommands.reboot();
                return true;
            case R.id.soft_reboot:
                ElevatedCommands.softReboot();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (uninstalled || refresh_mode) {
            prefs.edit().putInt("uninstalled", THEME_INFORMATION_REQUEST_CODE).apply();
        } else {
            prefs.edit().putInt("uninstalled", 0).apply();
        }
    }

    private class LayoutLoader extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (References.spreadYourWingsAndFly(getApplicationContext())) {
                gradientView.setVisibility(View.GONE);
                kenBurnsView.setBackgroundColor(Color.parseColor("#ffff00"));
                collapsingToolbarLayout.setStatusBarScrimColor(Color.parseColor("#ffff00"));
                collapsingToolbarLayout.setContentScrimColor(Color.parseColor("#ffff00"));
                appBarLayout.setBackgroundColor(Color.parseColor("#ffff00"));
                tabLayout.setBackgroundColor(Color.parseColor("#ffff00"));
                getWindow().setNavigationBarColor(Color.parseColor("#ffff00"));
            } else {
                Glide.with(getApplicationContext()).load(byteArray).centerCrop().into(kenBurnsView);
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            kenBurnsView = (KenBurnsView) findViewById(R.id.kenburnsView);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            heroImageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byteArray = stream.toByteArray();
            return null;
        }
    }

    private class AppShortcutCreator extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPostExecute(String result) {
            prefs.edit().putString("app_shortcut_theme", theme_pid).apply();
            favorite.setIcon(getDrawable(R.drawable.toolbar_favorite));
            if (shouldDarken) favorite.getIcon().setColorFilter(
                    getColor(R.color.information_activity_dark_icon_mode),
                    PorterDuff.Mode.SRC_ATOP);
            String format = String.format(getString(R.string.menu_favorite_snackbar), result);
            Snackbar.make(findViewById(android.R.id.content),
                    format,
                    Snackbar.LENGTH_LONG)
                    .show();
        }

        @Override
        protected String doInBackground(String... sUrl) {
            References.createShortcut(getApplicationContext(), theme_pid, theme_name);
            return theme_name;
        }
    }

    private class AppShortcutClearer extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPostExecute(String result) {
            prefs.edit().remove("app_shortcut_theme").apply();
            favorite.setIcon(getDrawable(R.drawable.toolbar_not_favorite));
            if (shouldDarken) favorite.getIcon().setColorFilter(
                    getColor(R.color.information_activity_dark_icon_mode),
                    PorterDuff.Mode.SRC_ATOP);
            String format = String.format(
                    getString(R.string.menu_favorite_snackbar_cleared), result);
            Snackbar.make(findViewById(android.R.id.content),
                    format,
                    Snackbar.LENGTH_LONG)
                    .show();
        }

        @Override
        protected String doInBackground(String... sUrl) {
            References.clearShortcut(getApplicationContext());
            return theme_name;
        }
    }

    private class uninstallTheme extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
            String parseMe = String.format(getString(R.string.adapter_uninstalling),
                    theme_name);
            mProgressDialog = new ProgressDialog(InformationActivity.this);
            mProgressDialog.setMessage(parseMe);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
            // Clear the notification of building theme if shown
            NotificationManager manager = (NotificationManager)
                    getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancel(References.notification_id);
        }

        @Override
        protected void onPostExecute(String result) {
            mProgressDialog.cancel();
            Toast toast = Toast.makeText(getApplicationContext(),
                    getString(R.string.clean_completion),
                    Toast.LENGTH_LONG);
            toast.show();
            uninstalled = true;
            onBackPressed();
        }

        @Override
        protected String doInBackground(String... sUrl) {
            References.uninstallPackage(getApplicationContext(), theme_pid);
            return null;
        }
    }
}