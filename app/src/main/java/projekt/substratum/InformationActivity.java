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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.Lunchbar;
import android.support.design.widget.TabLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.flaviofaria.kenburnsview.KenBurnsView;
import com.gordonwong.materialsheetfab.MaterialSheetFab;
import com.gordonwong.materialsheetfab.MaterialSheetFabEventListener;
import com.theartofdev.edmodo.cropper.CropImage;

import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import projekt.substratum.activities.base.SubstratumActivity;
import projekt.substratum.adapters.tabs.InformationTabsAdapter;
import projekt.substratum.common.Broadcasts;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.common.tabs.WallpaperManager;
import projekt.substratum.services.system.SamsungPackageService;
import projekt.substratum.tabs.BootAnimations;
import projekt.substratum.util.files.Root;
import projekt.substratum.util.views.FloatingActionMenu;
import projekt.substratum.util.views.SheetDialog;

import static projekt.substratum.common.Packages.getOverlayMetadata;
import static projekt.substratum.common.Packages.getPackageHeroImage;
import static projekt.substratum.common.References.BYPASS_SUBSTRATUM_BUILDER_DELETION;
import static projekt.substratum.common.References.MANAGER_REFRESH;
import static projekt.substratum.common.References.bootAnimationsFragment;
import static projekt.substratum.common.References.fontsFragment;
import static projekt.substratum.common.References.metadataHeroOverride;
import static projekt.substratum.common.References.metadataOverlayParent;
import static projekt.substratum.common.References.metadataWallpapers;
import static projekt.substratum.common.References.overlaysFragment;
import static projekt.substratum.common.References.shutdownAnimationsFragment;
import static projekt.substratum.common.References.soundsFragment;
import static projekt.substratum.common.References.wallpaperFragment;
import static projekt.substratum.common.Systems.checkAndromeda;
import static projekt.substratum.common.Systems.isSamsung;

public class InformationActivity extends SubstratumActivity {

    private static final int LUNCHBAR_DISMISS_FAB_CLICK_DELAY = 200;
    public static Lunchbar currentShownLunchBar;
    private static List<String> tab_checker;
    public String theme_name;
    public String theme_pid;
    public String theme_mode;
    public byte[] encryption_key;
    public byte[] iv_encrypt_key;
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
    private MaterialSheetFab materialSheetFab;
    private int tabPosition;
    private LocalBroadcastManager localBroadcastManager;
    private LocalBroadcastManager localBroadcastManager2;
    private BroadcastReceiver refreshReceiver;
    private AndromedaReceiver andromedaReceiver;
    private int dominantColor;
    private Context mContext;

    private static int getDominantColor(Bitmap bitmap) {
        try {
            Palette palette = Palette.from(bitmap).generate();
            return palette.getDominantColor(Color.TRANSPARENT);
        } catch (IllegalArgumentException iae) {
            // Suppress warning
        }
        return Color.TRANSPARENT;
    }

    private static void setOverflowButtonColor(final Activity activity, final Boolean dark_mode) {
        @SuppressLint("PrivateResource") final String overflowDescription =
                activity.getString(R.string.abc_action_menu_overflow_description);
        final ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        final ViewTreeObserver viewTreeObserver = decorView.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(() -> {
            final ArrayList<View> outViews = new ArrayList<>();
            decorView.findViewsWithText(
                    outViews,
                    overflowDescription,
                    View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
            if (outViews.isEmpty()) {
                return;
            }
            AppCompatImageView overflow = (AppCompatImageView) outViews.get(0);
            overflow.setImageResource(dark_mode ? R.drawable.information_activity_overflow_dark :
                    R.drawable.information_activity_overflow_light);
        });
    }

    private void autoSetToolbarIcons(boolean dynamicActionBarColors) {
        if (collapsingToolbarLayout != null && checkColorDarkness(dominantColor) &&
                dynamicActionBarColors && prefs.getBoolean("dynamic_actionbar", true)) {
            setDarkToolbarIcons();
        } else if (collapsingToolbarLayout != null) {
            setLightToolbarIcons();
        }
    }

    private void setDarkToolbarIcons() {
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
        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeAsUpIndicator(upArrow);
        }
        setOverflowButtonColor(this, true);
    }

    private void setLightToolbarIcons() {
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
        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeAsUpIndicator(upArrow);
        }
        setOverflowButtonColor(this, false);
    }

    private boolean closeAllLunchBars() {
        if (currentShownLunchBar != null) {
            currentShownLunchBar.dismiss();
            currentShownLunchBar = null;
            return true;
        } else {
            return false;
        }
    }

    private View getView() {
        return ((ViewGroup) this.findViewById(android.R.id.content)).getChildAt(0);
    }

    private boolean checkColorDarkness(int color) {
        double darkness =
                1 - (0.299 * Color.red(color) +
                        0.587 * Color.green(color) +
                        0.114 * Color.blue(color)) / 255;
        return darkness < 0.5;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Image Cropper Request Capture
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                SharedPreferences.Editor editor = prefs.edit();
                Uri resultUri = result.getUri();
                if (resultUri.toString().contains("homescreen_wallpaper")) {
                    try {
                        WallpaperManager.setWallpaper(
                                mContext,
                                resultUri.toString().substring(7),
                                "home");
                        editor.putString("home_wallpaper_applied", theme_pid);
                        currentShownLunchBar = Lunchbar.make(getView(),
                                getString(R.string.wallpaper_homescreen_success),
                                Lunchbar.LENGTH_LONG);
                        currentShownLunchBar.show();
                    } catch (IOException e) {
                        currentShownLunchBar = Lunchbar.make(getView(),
                                getString(R.string.wallpaper_homescreen_error),
                                Lunchbar.LENGTH_LONG);
                        currentShownLunchBar.show();
                        e.printStackTrace();
                    }
                } else if (resultUri.toString().contains("lockscreen_wallpaper")) {
                    try {
                        WallpaperManager.setWallpaper(
                                mContext,
                                resultUri.toString().substring(7),
                                "lock");
                        editor.putString("lock_wallpaper_applied", theme_pid);
                        currentShownLunchBar = Lunchbar.make(getView(),
                                getString(R.string.wallpaper_lockscreen_success),
                                Lunchbar.LENGTH_LONG);
                        currentShownLunchBar.show();
                    } catch (IOException e) {
                        currentShownLunchBar = Lunchbar.make(getView(),
                                getString(R.string.wallpaper_lockscreen_error),
                                Lunchbar.LENGTH_LONG);
                        currentShownLunchBar.show();
                        e.printStackTrace();
                    }
                } else if (resultUri.toString().contains("all_wallpaper")) {
                    try {
                        WallpaperManager.setWallpaper(
                                mContext,
                                resultUri.toString().substring(7),
                                "all");
                        editor.putString("home_wallpaper_applied", theme_pid);
                        editor.putString("lock_wallpaper_applied", theme_pid);
                        currentShownLunchBar = Lunchbar.make(getView(),
                                getString(R.string.wallpaper_allscreen_success),
                                Lunchbar.LENGTH_LONG);
                        currentShownLunchBar.show();
                    } catch (IOException e) {
                        currentShownLunchBar = Lunchbar.make(getView(),
                                getString(R.string.wallpaper_allscreen_error),
                                Lunchbar.LENGTH_LONG);
                        currentShownLunchBar.show();
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

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.information_activity);

        mContext = getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        // Register the theme install receiver to auto refresh the fragment
        refreshReceiver = new RefreshReceiver();
        IntentFilter if1 = new IntentFilter(MANAGER_REFRESH);
        localBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        localBroadcastManager.registerReceiver(refreshReceiver, if1);

        if (Systems.isAndromedaDevice(mContext)) {
            andromedaReceiver = new InformationActivity.AndromedaReceiver();
            IntentFilter filter2 = new IntentFilter("AndromedaReceiver.KILL");
            localBroadcastManager2 = LocalBroadcastManager.getInstance(mContext);
            localBroadcastManager2.registerReceiver(andromedaReceiver, filter2);
        }

        boolean dynamicActionBarColors = getResources().getBoolean(R.bool.dynamicActionBarColors);
        boolean dynamicNavBarColors = getResources().getBoolean(R.bool.dynamicNavigationBarColors);

        Intent currentIntent = getIntent();
        theme_name = currentIntent.getStringExtra("theme_name");

        theme_pid = currentIntent.getStringExtra("theme_pid");
        theme_mode = currentIntent.getStringExtra("theme_mode");
        encryption_key = currentIntent.getByteArrayExtra("encryption_key");
        iv_encrypt_key = currentIntent.getByteArrayExtra("iv_encrypt_key");
        String wallpaperUrl = getOverlayMetadata(mContext, theme_pid,
                metadataWallpapers);

        Bundle bundle = new Bundle();
        bundle.putString("theme_name", theme_name);
        bundle.putString("theme_pid", theme_pid);
        bundle.putString("theme_mode", theme_mode);
        bundle.putByteArray("encryption_key", encryption_key);
        bundle.putByteArray("iv_encrypt_key", iv_encrypt_key);
        bundle.putString("wallpaperUrl", wallpaperUrl);

        if (theme_mode == null) {
            theme_mode = "";
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) toolbar.setTitle(theme_name);

        gradientView = findViewById(R.id.gradientView);
        collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar_tabbed_layout);
        if (collapsingToolbarLayout != null) collapsingToolbarLayout.setTitle(theme_name);

        final ViewPager viewPager = findViewById(R.id.viewpager);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        if (toolbar != null) toolbar.setNavigationOnClickListener(v -> onBackPressed());

        Drawable heroImage = getPackageHeroImage(getApplicationContext(), theme_pid, false);
        if (heroImage != null) heroImageBitmap = ((BitmapDrawable) heroImage).getBitmap();
        if (heroImageBitmap == null) {
            dominantColor = Color.TRANSPARENT;
        } else {
            dominantColor = getDominantColor(heroImageBitmap);
        }

        appBarLayout = findViewById(R.id.appbar);
        appBarLayout.setBackgroundColor(dominantColor);

        if (collapsingToolbarLayout != null &&
                dynamicActionBarColors &&
                prefs.getBoolean("dynamic_actionbar", true)) {
            collapsingToolbarLayout.setStatusBarScrimColor(dominantColor);
            collapsingToolbarLayout.setContentScrimColor(dominantColor);
        }

        if (dynamicNavBarColors && prefs.getBoolean("dynamic_navbar", true)) {
            getWindow().setNavigationBarColor(dominantColor);
            if (checkColorDarkness(dominantColor)) {
                getWindow().setNavigationBarColor(getColor(R.color.theme_information_background));
            }
        }

        View sheetView = findViewById(R.id.fab_sheet);
        View overlay = findViewById(R.id.overlay);
        int sheetColor = mContext.getColor(R.color.fab_menu_background_card);
        int fabColor = mContext.getColor(R.color.fab_background_color);

        final FloatingActionMenu floatingActionButton = findViewById(R.id.apply_fab);
        floatingActionButton.show();

        // Create material sheet FAB
        if (sheetView != null && overlay != null) {
            materialSheetFab = new MaterialSheetFab<>(
                    floatingActionButton,
                    sheetView,
                    overlay,
                    sheetColor,
                    fabColor);
        }

        new LayoutLoader(this).execute("");
        tabLayout = findViewById(R.id.tabs);
        if (tabLayout != null) {
            // First, take account for whether the theme was launched normally
            if (theme_mode.equals("")) {
                try {
                    Context otherContext = mContext.createPackageContext
                            (theme_pid, 0);
                    AssetManager am = otherContext.getAssets();
                    List found_folders = Arrays.asList(am.list(""));
                    tab_checker = new ArrayList<>();
                    if (!Systems.checkOMS(mContext)) {
                        for (int i = 0; i < found_folders.size(); i++) {
                            if (projekt.substratum.common.Resources.allowedForLegacy
                                    (found_folders.get(i).toString())) {
                                tab_checker.add(found_folders.get(i).toString());
                            }
                        }
                    } else {
                        tab_checker = Arrays.asList(am.list(""));
                    }
                    boolean isWallpaperOnly = true;
                    if (tab_checker.contains(overlaysFragment)) {
                        isWallpaperOnly = false;
                        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                                .theme_information_tab_one)));
                    }
                    if (tab_checker.contains(bootAnimationsFragment) &&
                            !checkAndromeda(mContext) &&
                            !isSamsung(mContext)) {
                        isWallpaperOnly = false;
                        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                                .theme_information_tab_two)));
                    }
                    if (tab_checker.contains(shutdownAnimationsFragment) &&
                            projekt.substratum.common.Resources.isShutdownAnimationSupported()) {
                        isWallpaperOnly = false;
                        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                                .theme_information_tab_six)));
                    }
                    if (tab_checker.contains(fontsFragment) && projekt.substratum.common
                            .Resources.isFontsSupported()) {
                        isWallpaperOnly = false;
                        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                                .theme_information_tab_three)));
                    }
                    if (tab_checker.contains(soundsFragment) &&
                            !checkAndromeda(mContext) &&
                            !isSamsung(mContext)) {
                        isWallpaperOnly = false;
                        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                                .theme_information_tab_four)));
                    }
                    if (wallpaperUrl != null && wallpaperUrl.length() > 0) {
                        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string
                                .theme_information_tab_five)));
                    }
                    if (isWallpaperOnly && wallpaperUrl != null && wallpaperUrl.length() > 0) {
                        final Handler handler = new Handler();
                        handler.postDelayed(() ->
                                runOnUiThread(floatingActionButton::hide), 500);
                    }
                } catch (Exception e) {
                    Log.e(References.SUBSTRATUM_LOG, "Could not refresh list of asset folders.");
                }
            } else {
                // At this point, theme was launched in their tab specific sections
                switch (theme_mode) {
                    case overlaysFragment:
                        tabLayout.addTab(tabLayout.newTab().setText(
                                getString(R.string.theme_information_tab_one)));
                        break;
                    case bootAnimationsFragment:
                        tabLayout.addTab(tabLayout.newTab().setText(
                                getString(R.string.theme_information_tab_two)));
                        break;
                    case shutdownAnimationsFragment:
                        tabLayout.addTab(tabLayout.newTab().setText(
                                getString(R.string.theme_information_tab_six)));
                        break;
                    case fontsFragment:
                        tabLayout.addTab(tabLayout.newTab().setText(
                                getString(R.string.theme_information_tab_three)));
                        break;
                    case soundsFragment:
                        tabLayout.addTab(tabLayout.newTab().setText(
                                getString(R.string.theme_information_tab_four)));
                        break;
                    case wallpaperFragment:
                        tabLayout.addTab(tabLayout.newTab().setText(
                                getString(R.string.theme_information_tab_five)));
                        break;
                }
            }

            tabLayout.setTabGravity(TabLayout.MODE_SCROLLABLE);
            if (dynamicActionBarColors && prefs.getBoolean("dynamic_actionbar", true))
                tabLayout.setBackgroundColor(dominantColor);

            String toOverrideHero =
                    Packages.getOverlayMetadata(
                            getApplicationContext(),
                            theme_pid,
                            metadataHeroOverride);
            if (toOverrideHero != null) {
                switch (toOverrideHero) {
                    case "dark":
                        setDarkToolbarIcons();
                        break;
                    case "light":
                        setLightToolbarIcons();
                        break;
                    default:
                        autoSetToolbarIcons(dynamicActionBarColors);
                        break;
                }
            } else {
                autoSetToolbarIcons(dynamicActionBarColors);
            }
        }

        boolean[] extra_hidden_tabs = new boolean[4];
        extra_hidden_tabs[0] = !Systems.checkAndromeda(mContext) &&
                !Systems.isSamsungDevice(mContext);
        extra_hidden_tabs[1] = Systems.checkOreo() &&
                Systems.isBinderInterfacer(mContext);
        extra_hidden_tabs[2] = projekt.substratum.common.Resources.isFontsSupported();
        extra_hidden_tabs[3] = !Systems.checkAndromeda(mContext);

        final InformationTabsAdapter adapter = new InformationTabsAdapter
                (getSupportFragmentManager(), (tabLayout != null) ? tabLayout.getTabCount() : 0,
                        theme_mode, tab_checker, wallpaperUrl, extra_hidden_tabs, bundle);

        if (viewPager != null) {
            viewPager.setOffscreenPageLimit((tabLayout != null) ? tabLayout.getTabCount() : 0);
            viewPager.setAdapter(adapter);
            viewPager.addOnPageChangeListener(
                    new TabLayout.TabLayoutOnPageChangeListener(tabLayout) {
                        @Override
                        public void onPageSelected(int position) {
                            tabPosition = position;
                            switch (viewPager.getAdapter().instantiateItem(viewPager, tabPosition)
                                    .getClass().getSimpleName()) {
                                case "Overlays":
                                    floatingActionButton.show();
                                    floatingActionButton.setImageResource(
                                            R.drawable.floating_action_button_icon);
                                    break;
                                case "BootAnimations":
                                case "Fonts":
                                case "Sounds":
                                    floatingActionButton.show();
                                    floatingActionButton.setImageResource(
                                            R.drawable.floating_action_button_icon_check);
                                    break;
                                case "Wallpapers":
                                    floatingActionButton.hide();
                                    break;
                            }
                        }
                    });
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
                            viewPager.setCurrentItem(tab.getPosition());
                        }
                    });

            PagerAdapter adapt = viewPager.getAdapter();
            LocalBroadcastManager localBroadcastManager =
                    LocalBroadcastManager.getInstance(mContext);
            floatingActionButton.setOnClickListener(v -> {
                try {
                    boolean isLunchbarOpen = closeAllLunchBars();
                    final Handler handler = new Handler();
                    handler.postDelayed(() -> {
                        Intent intent;
                        Object obj = adapt.instantiateItem(viewPager, tabPosition);
                        switch (obj.getClass().getSimpleName()) {
                            case "Overlays":
                                materialSheetFab.showSheet();
                                break;
                            case "BootAnimations":
                                boolean isShutdownTab = ((BootAnimations) obj).isShutdownTab();
                                intent = new Intent((isShutdownTab ? "ShutdownAnimations" :
                                        "BootAnimations") + ".START_JOB");
                                localBroadcastManager.sendBroadcast(intent);
                                break;
                            case "Fonts":
                                intent = new Intent("Fonts.START_JOB");
                                localBroadcastManager.sendBroadcast(intent);
                                break;
                            case "Sounds":
                                intent = new Intent("Sounds.START_JOB");
                                localBroadcastManager.sendBroadcast(intent);
                                break;
                        }
                    }, isLunchbarOpen ? LUNCHBAR_DISMISS_FAB_CLICK_DELAY : 0);
                } catch (NullPointerException npe) {
                    // Suppress warning
                }
            });

            Intent intent = new Intent("Overlays.START_JOB");
            Switch enable_swap = findViewById(R.id.enable_swap);
            if (!Systems.checkOMS(this) && !Systems.isSamsung(mContext)) {
                enable_swap.setText(getString(R.string.fab_menu_swap_toggle_legacy));
            } else if (Systems.isSamsung(mContext)) {
                View fab_menu_divider = findViewById(R.id.fab_menu_divider);
                fab_menu_divider.setVisibility(View.GONE);
                enable_swap.setVisibility(View.GONE);
            }
            if (enable_swap != null) {
                boolean enabled = prefs.getBoolean("enable_swapping_overlays", true);
                intent.putExtra("command", "MixAndMatchMode");
                intent.putExtra("newValue", enabled);
                localBroadcastManager.sendBroadcast(intent);
                enable_swap.setChecked(enabled);

                enable_swap.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    prefs.edit().putBoolean("enable_swapping_overlays", isChecked).apply();
                    intent.putExtra("command", "MixAndMatchMode");
                    intent.putExtra("newValue", isChecked);
                    localBroadcastManager.sendBroadcast(intent);
                });
            }

            final TextView compile_enable_selected = findViewById(R.id.compile_enable_selected);
            if (!Systems.checkOMS(this)) compile_enable_selected.setVisibility(View.GONE);
            if (compile_enable_selected != null) {
                compile_enable_selected.setOnClickListener(v -> {
                    materialSheetFab.setEventListener(new MaterialSheetFabEventListener() {
                        @Override
                        public void onSheetHidden() {
                            super.onSheetHidden();
                            intent.putExtra("command", "CompileEnable");
                            localBroadcastManager.sendBroadcast(intent);
                            materialSheetFab.setEventListener(null);
                        }
                    });
                    materialSheetFab.hideSheet();
                });
            }

            TextView compile_update_selected = findViewById(R.id.compile_update_selected);
            if (!Systems.checkOMS(this)) {
                compile_update_selected.setText(getString(R.string.fab_menu_compile_install));
            }
            if (compile_update_selected != null) {
                compile_update_selected.setOnClickListener(v -> {
                    materialSheetFab.setEventListener(new MaterialSheetFabEventListener() {
                        @Override
                        public void onSheetHidden() {
                            super.onSheetHidden();
                            intent.putExtra("command", "CompileUpdate");
                            localBroadcastManager.sendBroadcast(intent);
                            materialSheetFab.setEventListener(null);
                        }
                    });
                    materialSheetFab.hideSheet();
                });
            }

            TextView disable_selected = findViewById(R.id.disable_selected);
            if (!Systems.checkOMS(this)) {
                disable_selected.setText(getString(R.string.fab_menu_uninstall));
            }
            if (disable_selected != null) {
                disable_selected.setOnClickListener(v -> {
                    materialSheetFab.setEventListener(new MaterialSheetFabEventListener() {
                        @Override
                        public void onSheetHidden() {
                            super.onSheetHidden();
                            intent.putExtra("command", "Disable");
                            localBroadcastManager.sendBroadcast(intent);
                            materialSheetFab.setEventListener(null);
                        }
                    });
                    materialSheetFab.hideSheet();
                });
            }

            LinearLayout enable_zone = findViewById(R.id.enable);
            if (!Systems.checkOMS(this)) enable_zone.setVisibility(View.GONE);
            TextView enable_selected = findViewById(R.id.enable_selected);
            if (enable_selected != null) {
                enable_selected.setOnClickListener(v -> {
                    materialSheetFab.setEventListener(new MaterialSheetFabEventListener() {
                        @Override
                        public void onSheetHidden() {
                            super.onSheetHidden();
                            intent.putExtra("command", "Enable");
                            localBroadcastManager.sendBroadcast(intent);
                            materialSheetFab.setEventListener(null);
                        }
                    });
                    materialSheetFab.hideSheet();
                });
            }

            TextView enable_disable_selected =
                    findViewById(R.id.enable_disable_selected);
            if (!Systems.checkOMS(this))
                enable_disable_selected.setVisibility(View.GONE);
            if (enable_disable_selected != null) {
                enable_disable_selected.setOnClickListener(v -> {
                    materialSheetFab.setEventListener(new MaterialSheetFabEventListener() {
                        @Override
                        public void onSheetHidden() {
                            super.onSheetHidden();
                            intent.putExtra("command", "EnableDisable");
                            localBroadcastManager.sendBroadcast(intent);
                            materialSheetFab.setEventListener(null);
                        }
                    });
                    materialSheetFab.hideSheet();
                });
            }

            Boolean shouldShowSamsungWarning =
                    !prefs.getBoolean("show_dangerous_samsung_overlays", false);
            if (Systems.isSamsung(mContext) &&
                    !Packages.isSamsungTheme(mContext, theme_pid) &&
                    shouldShowSamsungWarning) {
                currentShownLunchBar = Lunchbar.make(
                        getView(),
                        R.string.toast_samsung_prototype_alert,
                        Lunchbar.LENGTH_SHORT);
                currentShownLunchBar.show();
            }
            if (Systems.isSamsung(mContext)) {
                startService(new Intent(getBaseContext(), SamsungPackageService.class));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.theme_information_menu, menu);

        // Start formalizing a check for dark icons
        boolean dynamicActionBarColors = getResources().getBoolean(R.bool.dynamicActionBarColors);
        shouldDarken = collapsingToolbarLayout != null &&
                checkColorDarkness(dominantColor) &&
                dynamicActionBarColors && prefs.getBoolean("dynamic_actionbar", true);

        // Start dynamically showing menu items
        boolean isOMS = Systems.checkOMS(mContext);
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
        if (Packages.getThemeChangelog(mContext, theme_pid) != null) {
            MenuItem changelog = menu.findItem(R.id.changelog);
            changelog.setVisible(true);
            if (shouldDarken) changelog.getIcon().setColorFilter(
                    getColor(R.color.information_activity_dark_icon_mode),
                    PorterDuff.Mode.SRC_ATOP);
        }

        if (Systems.checkAndromeda(mContext) ||
                (!isOMS && !Root.checkRootAccess())) {
            menu.findItem(R.id.restart_systemui).setVisible(false);
        }
        if (!isOMS) {
            menu.findItem(R.id.disable).setVisible(false);
            menu.findItem(R.id.enable).setVisible(false);
        }
        if (isOMS || isSamsung(mContext)) {
            if (isSamsung(mContext)) menu.findItem(R.id.clean).setVisible(false);
            menu.findItem(R.id.reboot_device).setVisible(false);
            menu.findItem(R.id.soft_reboot).setVisible(false);
            menu.findItem(R.id.uninstall).setVisible(false);
        }

        if (!Packages.isUserApp(mContext, theme_pid)) {
            menu.findItem(R.id.uninstall).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.favorite:
                if (prefs.contains("app_shortcut_theme")) {
                    if (prefs.getString("app_shortcut_theme", "").equals(theme_pid)) {
                        new AppShortcutClearer(this).execute("");
                    } else {
                        new AppShortcutCreator(this).execute("favorite");
                    }
                } else {
                    new AppShortcutCreator(this).execute("favorite");
                }
                return true;
            case R.id.changelog:
                SheetDialog sheetDialog = new SheetDialog(this);
                @SuppressLint("InflateParams")
                View sheetView = getLayoutInflater().inflate(R.layout.changelog_sheet_dialog, null);

                LinearLayout titleBox = sheetView.findViewById(R.id.title_box);
                TextView title = titleBox.findViewById(R.id.title);
                String format_me = String.format(getString(R.string.changelog_title), theme_name);
                title.setText(format_me);

                LinearLayout textBox = sheetView.findViewById(R.id.text_box);
                TextView text = textBox.findViewById(R.id.text);

                String[] changelog_parsing =
                        Packages.getThemeChangelog(mContext, theme_pid);
                StringBuilder to_show = new StringBuilder();
                if (changelog_parsing != null) {
                    for (String aChangelog_parsing : changelog_parsing) {
                        to_show.append("\u2022 ").append(aChangelog_parsing).append("\n");
                    }
                }
                text.setText(to_show.toString());
                sheetDialog.setCanceledOnTouchOutside(true);
                sheetDialog.setContentView(sheetView);
                sheetDialog.show();
                return true;
            case R.id.clean:
                AlertDialog.Builder builder1 = new AlertDialog.Builder(InformationActivity.this);
                builder1.setTitle(theme_name);
                builder1.setIcon(Packages.getAppIcon(mContext, theme_pid));
                builder1.setMessage(R.string.clean_dialog_body)
                        .setPositiveButton(R.string.dialog_ok, (dialog, id18) -> {
                            // Dismiss the dialog
                            dialog.dismiss();
                            // Get all installed overlays
                            List<String> stateAll = ThemeManager.listAllOverlays(
                                    mContext);

                            ArrayList<String> all_overlays = new ArrayList<>();
                            for (int j = 0; j < stateAll.size(); j++) {
                                try {
                                    String current = stateAll.get(j);
                                    ApplicationInfo appInfo = mContext
                                            .getPackageManager().getApplicationInfo(
                                                    current, PackageManager.GET_META_DATA);
                                    if (appInfo.metaData != null &&
                                            appInfo.metaData.getString(
                                                    metadataOverlayParent) != null) {
                                        String parent =
                                                appInfo.metaData.getString(metadataOverlayParent);
                                        if (parent != null && parent.equals(theme_pid)) {
                                            all_overlays.add(current);
                                        }
                                    }
                                } catch (Exception e) {
                                    // NameNotFound
                                }
                            }

                            // Begin uninstalling overlays for this package
                            ThemeManager.uninstallOverlay(
                                    mContext,
                                    all_overlays
                            );
                        })
                        .setNegativeButton(R.string.dialog_cancel, (dialog, id19) -> {
                            // User cancelled the dialog
                        });
                // Create the AlertDialog object and return it
                builder1.create();
                builder1.show();
                return true;
            case R.id.disable:
                AlertDialog.Builder builder3 = new AlertDialog.Builder(InformationActivity.this);
                builder3.setTitle(theme_name);
                builder3.setIcon(Packages.getAppIcon(mContext, theme_pid));
                builder3.setMessage(R.string.disable_dialog_body)
                        .setPositiveButton(R.string.dialog_ok, (dialog, id16) -> {
                            // Dismiss the dialog
                            dialog.dismiss();
                            // Get all enabled overlays
                            List<String> stateAll = ThemeManager.listOverlays(
                                    mContext, ThemeManager.STATE_ENABLED);

                            ArrayList<String> all_overlays = new ArrayList<>();
                            for (int j = 0; j < stateAll.size(); j++) {
                                try {
                                    String current = stateAll.get(j);
                                    ApplicationInfo appInfo = mContext
                                            .getPackageManager().getApplicationInfo(
                                                    current, PackageManager.GET_META_DATA);
                                    if (appInfo.metaData != null &&
                                            appInfo.metaData.getString(
                                                    metadataOverlayParent) != null) {
                                        String parent =
                                                appInfo.metaData.getString(metadataOverlayParent);
                                        if (parent != null && parent.equals(theme_pid)) {
                                            all_overlays.add(current);
                                        }
                                    }
                                } catch (Exception e) {
                                    // NameNotFound
                                }
                            }
                            currentShownLunchBar = Lunchbar.make(getView(),
                                    getString(R.string.disable_completion),
                                    Lunchbar.LENGTH_LONG);
                            currentShownLunchBar.show();
                            // Begin disabling overlays
                            ThemeManager.disableOverlay(mContext, all_overlays);
                        })
                        .setNegativeButton(R.string.dialog_cancel, (dialog, id15) -> {
                            // User cancelled the dialog
                        });
                // Create the AlertDialog object and return it
                builder3.create();
                builder3.show();
                return true;
            case R.id.enable:
                AlertDialog.Builder builder4 = new AlertDialog.Builder(InformationActivity.this);
                builder4.setTitle(theme_name);
                builder4.setIcon(Packages.getAppIcon(mContext, theme_pid));
                builder4.setMessage(R.string.enable_dialog_body)
                        .setPositiveButton(R.string.dialog_ok, (dialog, id14) -> {
                            // Dismiss the dialog
                            dialog.dismiss();
                            // Get all disabled overlays
                            List<String> stateAll = ThemeManager.listOverlays(
                                    mContext, ThemeManager.STATE_DISABLED);

                            ArrayList<String> all_overlays = new ArrayList<>();
                            for (int j = 0; j < stateAll.size(); j++) {
                                try {
                                    String current = stateAll.get(j);
                                    ApplicationInfo appInfo = mContext
                                            .getPackageManager().getApplicationInfo(
                                                    current, PackageManager.GET_META_DATA);
                                    if (appInfo.metaData != null &&
                                            appInfo.metaData.getString(
                                                    metadataOverlayParent) != null) {
                                        String parent =
                                                appInfo.metaData.getString(metadataOverlayParent);
                                        if (parent != null && parent.equals(theme_pid)) {
                                            all_overlays.add(current);
                                        }
                                    }
                                } catch (Exception e) {
                                    // NameNotFound
                                }
                            }
                            currentShownLunchBar = Lunchbar.make(getView(),
                                    getString(R.string.enable_completion),
                                    Lunchbar.LENGTH_LONG);
                            currentShownLunchBar.show();

                            // Begin enabling overlays
                            ThemeManager.enableOverlay(mContext, all_overlays);
                        })
                        .setNegativeButton(R.string.dialog_cancel, (dialog, id13) ->
                                dialog
                                        .cancel());
                // Create the AlertDialog object and return it
                builder4.create();
                builder4.show();
                return true;
            case R.id.uninstall:
                AlertDialog.Builder builder5 = new AlertDialog.Builder(InformationActivity.this);
                builder5.setTitle(theme_name);
                builder5.setIcon(Packages.getAppIcon(mContext, theme_pid));
                builder5.setMessage(R.string.uninstall_dialog_text)
                        .setPositiveButton(R.string.dialog_ok, (dialog, id12) -> {
                            // Dismiss the dialog
                            dialog.dismiss();
                            new uninstallTheme(this).execute("");
                        })
                        .setNegativeButton(R.string.dialog_cancel, (dialog, id1) -> {
                            // User cancelled the dialog
                        });
                // Create the AlertDialog object and return it
                builder5.create();
                builder5.show();
                return true;
            case R.id.restart_systemui:
                ThemeManager.restartSystemUI(mContext);
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
        if (materialSheetFab.isSheetVisible()) {
            materialSheetFab.hideSheet();
        } else {
            if (uninstalled)
                Broadcasts.sendRefreshMessage(mContext);
            finish();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (Systems.isSamsung(mContext)) {
            stopService(new Intent(getBaseContext(), SamsungPackageService.class));
        }

        try {
            localBroadcastManager.unregisterReceiver(refreshReceiver);
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

        if (!BYPASS_SUBSTRATUM_BUILDER_DELETION) {
            String workingDirectory =
                    mContext.getCacheDir().getAbsolutePath();
            File deleted = new File(workingDirectory);
            FileOperations.delete(mContext, deleted.getAbsolutePath());
            if (!deleted.exists()) Log.d(References.SUBSTRATUM_BUILDER,
                    "Successfully cleared Substratum cache!");
        }
    }

    private static class LayoutLoader extends AsyncTask<String, Integer, String> {
        private WeakReference<InformationActivity> ref;

        LayoutLoader(InformationActivity informationActivity) {
            ref = new WeakReference<>(informationActivity);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            InformationActivity informationActivity = ref.get();
            if (informationActivity != null) {
                if (!informationActivity.prefs.getBoolean("complexion", false)) {
                    if (informationActivity.gradientView != null)
                        informationActivity.gradientView.setVisibility(View.GONE);
                    if (informationActivity.kenBurnsView != null)
                        informationActivity.kenBurnsView.
                                setBackgroundColor(Color.parseColor("#ffff00"));
                    if (informationActivity.collapsingToolbarLayout != null) {
                        informationActivity.collapsingToolbarLayout.
                                setStatusBarScrimColor(Color.parseColor("#ffff00"));
                        informationActivity.collapsingToolbarLayout.
                                setContentScrimColor(Color.parseColor("#ffff00"));
                    }
                    if (informationActivity.appBarLayout != null)
                        informationActivity.appBarLayout.
                                setBackgroundColor(Color.parseColor("#ffff00"));
                    if (informationActivity.tabLayout != null)
                        informationActivity.tabLayout.
                                setBackgroundColor(Color.parseColor("#ffff00"));
                    informationActivity.getWindow().
                            setNavigationBarColor(Color.parseColor("#ffff00"));
                } else if (informationActivity.kenBurnsView != null) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(
                            informationActivity.byteArray, 0, informationActivity.byteArray.length);
                    informationActivity.kenBurnsView.setImageBitmap(bitmap);
                }
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InformationActivity informationActivity = ref.get();
            if (informationActivity != null) {
                informationActivity.kenBurnsView =
                        informationActivity.findViewById(R.id.kenburnsView);
                if (informationActivity.heroImageBitmap != null) {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    informationActivity.heroImageBitmap.compress(
                            Bitmap.CompressFormat.JPEG, 100, stream);
                    informationActivity.byteArray = stream.toByteArray();
                }
            }
            return null;
        }
    }

    private static class AppShortcutCreator extends AsyncTask<String, Integer, String> {
        private WeakReference<InformationActivity> ref;

        AppShortcutCreator(InformationActivity informationActivity) {
            ref = new WeakReference<>(informationActivity);
        }

        @Override
        protected void onPostExecute(String result) {
            InformationActivity informationActivity = ref.get();
            if (informationActivity != null) {
                informationActivity.prefs.edit().
                        putString("app_shortcut_theme", informationActivity.theme_pid).apply();
                informationActivity.favorite.setIcon(
                        informationActivity.getDrawable(R.drawable.toolbar_favorite));
                if (informationActivity.shouldDarken)
                    informationActivity.favorite.getIcon().setColorFilter(
                            informationActivity.getColor(R.color
                                    .information_activity_dark_icon_mode),
                            PorterDuff.Mode.SRC_ATOP);
                String format = String.format(
                        informationActivity.getString(R.string.menu_favorite_snackbar), result);
                currentShownLunchBar = Lunchbar.make(
                        informationActivity.getView(),
                        format,
                        Lunchbar.LENGTH_LONG);
                currentShownLunchBar.show();
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InformationActivity informationActivity = ref.get();
            if (informationActivity != null) {
                References.createShortcut(
                        informationActivity.mContext,
                        informationActivity.theme_pid,
                        informationActivity.theme_name);
                return informationActivity.theme_name;
            }
            return null;
        }
    }

    private static class AppShortcutClearer extends AsyncTask<String, Integer, String> {
        private WeakReference<InformationActivity> ref;

        AppShortcutClearer(InformationActivity informationActivity) {
            ref = new WeakReference<>(informationActivity);
        }

        @Override
        protected void onPostExecute(String result) {
            InformationActivity informationActivity = ref.get();
            if (informationActivity != null) {
                informationActivity.prefs.edit().remove("app_shortcut_theme").apply();
                informationActivity.favorite.setIcon(
                        informationActivity.getDrawable(R.drawable.toolbar_not_favorite));
                if (informationActivity.shouldDarken)
                    informationActivity.favorite.getIcon().setColorFilter(
                            informationActivity.getColor(R.color
                                    .information_activity_dark_icon_mode),
                            PorterDuff.Mode.SRC_ATOP);
                String format = String.format(
                        informationActivity.getString(R.string.menu_favorite_snackbar_cleared),
                        result);
                currentShownLunchBar = Lunchbar.make(
                        informationActivity.getView(),
                        format,
                        Lunchbar.LENGTH_LONG);
                currentShownLunchBar.show();
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InformationActivity informationActivity = ref.get();
            if (informationActivity != null) {
                References.clearShortcut(informationActivity.mContext);
                return informationActivity.theme_name;
            }
            return null;
        }
    }

    private static class uninstallTheme extends AsyncTask<String, Integer, String> {
        private WeakReference<InformationActivity> ref;

        uninstallTheme(InformationActivity informationActivity) {
            ref = new WeakReference<>(informationActivity);
        }

        @Override
        protected void onPreExecute() {
            InformationActivity informationActivity = ref.get();
            if (informationActivity != null) {
                String parseMe = String.format(
                        informationActivity.getString(R.string.adapter_uninstalling),
                        informationActivity.theme_name);

                informationActivity.mProgressDialog = new ProgressDialog(informationActivity);
                informationActivity.mProgressDialog.setMessage(parseMe);
                informationActivity.mProgressDialog.setIndeterminate(true);
                informationActivity.mProgressDialog.setCancelable(false);
                informationActivity.mProgressDialog.show();
                // Clear the notification of building theme if shown
                NotificationManager manager = (NotificationManager)
                        informationActivity.mContext
                                .getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager != null) {
                    manager.cancel(References.notification_id);
                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
            InformationActivity informationActivity = ref.get();
            if (informationActivity != null) {
                informationActivity.mProgressDialog.cancel();
                informationActivity.uninstalled = true;
                informationActivity.onBackPressed();
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InformationActivity informationActivity = ref.get();
            if (informationActivity != null) {
                Packages.uninstallPackage(
                        informationActivity.mContext,
                        informationActivity.theme_pid);
            }
            return null;
        }
    }

    class RefreshReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!Packages.isPackageInstalled(context, theme_pid)) {
                Log.d("ThemeUninstaller",
                        "The theme was uninstalled, so the activity is now closing!");
                Broadcasts.sendRefreshMessage(context);
                finish();
            }
        }
    }

    class AndromedaReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    }
}