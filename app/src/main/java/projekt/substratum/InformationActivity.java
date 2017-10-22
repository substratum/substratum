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
import android.widget.Toast;

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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import projekt.substratum.activities.base.SubstratumActivity;
import projekt.substratum.adapters.tabs.InformationTabsAdapter;
import projekt.substratum.common.Broadcasts;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Resources;
import projekt.substratum.common.Systems;
import projekt.substratum.common.Theming;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.common.tabs.WallpaperManager;
import projekt.substratum.tabs.BootAnimations;
import projekt.substratum.util.files.Root;
import projekt.substratum.util.helpers.ContextWrapper;
import projekt.substratum.util.views.FloatingActionMenu;
import projekt.substratum.util.views.SheetDialog;

import static projekt.substratum.common.Packages.getOverlayMetadata;
import static projekt.substratum.common.Packages.getPackageHeroImage;
import static projekt.substratum.common.References.ACTIVITY_FINISHER;
import static projekt.substratum.common.References.BYPASS_SUBSTRATUM_BUILDER_DELETION;
import static projekt.substratum.common.References.MANAGER_REFRESH;
import static projekt.substratum.common.References.SUBSTRATUM_LOG;
import static projekt.substratum.common.References.bootAnimationsFragment;
import static projekt.substratum.common.References.fontsFragment;
import static projekt.substratum.common.References.metadataHeroOverride;
import static projekt.substratum.common.References.metadataOverlayParent;
import static projekt.substratum.common.References.metadataWallpapers;
import static projekt.substratum.common.References.overlaysFragment;
import static projekt.substratum.common.References.shutdownAnimationsFragment;
import static projekt.substratum.common.References.soundsFragment;
import static projekt.substratum.common.References.wallpaperFragment;
import static projekt.substratum.common.Systems.isSamsung;

public class InformationActivity extends SubstratumActivity {

    private static final int LUNCHBAR_DISMISS_FAB_CLICK_DELAY = 200;
    public static Lunchbar currentShownLunchBar;
    public static Boolean compilingProcess;
    public static Boolean shouldRestartActivity = false;
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
    private BroadcastReceiver refreshReceiver;
    private AndromedaReceiver andromedaReceiver;
    private ActivityFinisher activityFinisher;
    private int dominantColor;
    private Context mContext;

    private static int getDominantColor(final Bitmap bitmap) {
        try {
            final Palette palette = Palette.from(bitmap).generate();
            return palette.getDominantColor(Color.TRANSPARENT);
        } catch (final IllegalArgumentException ignored) {
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
            for (final View view : outViews) {
                if (view instanceof AppCompatImageView) {
                    final AppCompatImageView overflow = (AppCompatImageView) view;
                    overflow.setImageResource(dark_mode ? R.drawable
                            .information_activity_overflow_dark :
                            R.drawable.information_activity_overflow_light);
                }
            }
        });
    }

    private void autoSetToolbarIcons(final boolean dynamicActionBarColors) {
        if (this.collapsingToolbarLayout != null && this.checkColorDarkness(this.dominantColor) &&
                dynamicActionBarColors) {
            this.setDarkToolbarIcons();
        } else if (this.collapsingToolbarLayout != null) {
            this.setLightToolbarIcons();
        }
    }

    private void setDarkToolbarIcons() {
        this.collapsingToolbarLayout.setCollapsedTitleTextColor(
                this.getColor(R.color.information_activity_dark_icon_mode));
        this.collapsingToolbarLayout.setExpandedTitleColor(
                this.getColor(R.color.information_activity_dark_icon_mode));
        this.tabLayout.setTabTextColors(
                this.getColor(R.color.information_activity_dark_text_mode),
                this.getColor(R.color.information_activity_dark_text_mode));

        final Drawable upArrow = this.getDrawable(R.drawable.information_activity_back_dark);
        if (upArrow != null)
            upArrow.setColorFilter(this.getColor(R.color.information_activity_dark_icon_mode),
                    PorterDuff.Mode.SRC_ATOP);
        if (this.getSupportActionBar() != null) {
            this.getSupportActionBar().setHomeAsUpIndicator(upArrow);
        }
        setOverflowButtonColor(this, true);
    }

    private void setLightToolbarIcons() {
        this.collapsingToolbarLayout.setCollapsedTitleTextColor(
                this.getColor(R.color.information_activity_light_icon_mode));
        this.collapsingToolbarLayout.setExpandedTitleColor(
                this.getColor(R.color.information_activity_light_icon_mode));
        this.tabLayout.setTabTextColors(
                this.getColor(R.color.information_activity_light_text_mode),
                this.getColor(R.color.information_activity_light_text_mode));

        final Drawable upArrow = this.getDrawable(R.drawable.information_activity_back_light);
        if (upArrow != null)
            upArrow.setColorFilter(this.getColor(R.color.information_activity_light_icon_mode),
                    PorterDuff.Mode.SRC_ATOP);
        if (this.getSupportActionBar() != null) {
            this.getSupportActionBar().setHomeAsUpIndicator(upArrow);
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

    private boolean checkColorDarkness(final int color) {
        final double darkness =
                1 - (0.299 * Color.red(color) +
                        0.587 * Color.green(color) +
                        0.114 * Color.blue(color)) / 255;
        return darkness < 0.5;
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Image Cropper Request Capture
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            final CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                final SharedPreferences.Editor editor = this.prefs.edit();
                final Uri resultUri = result.getUri();
                if (resultUri.toString().contains("homescreen_wallpaper")) {
                    try {
                        WallpaperManager.setWallpaper(
                                this.mContext,
                                resultUri.toString().substring(7),
                                "home");
                        editor.putString("home_wallpaper_applied", this.theme_pid);
                        currentShownLunchBar = Lunchbar.make(this.getView(),
                                this.getString(R.string.wallpaper_homescreen_success),
                                Lunchbar.LENGTH_LONG);
                        currentShownLunchBar.show();
                    } catch (final IOException e) {
                        currentShownLunchBar = Lunchbar.make(this.getView(),
                                this.getString(R.string.wallpaper_homescreen_error),
                                Lunchbar.LENGTH_LONG);
                        currentShownLunchBar.show();
                        e.printStackTrace();
                    }
                } else if (resultUri.toString().contains("lockscreen_wallpaper")) {
                    try {
                        WallpaperManager.setWallpaper(
                                this.mContext,
                                resultUri.toString().substring(7),
                                "lock");
                        editor.putString("lock_wallpaper_applied", this.theme_pid);
                        currentShownLunchBar = Lunchbar.make(this.getView(),
                                this.getString(R.string.wallpaper_lockscreen_success),
                                Lunchbar.LENGTH_LONG);
                        currentShownLunchBar.show();
                    } catch (final IOException e) {
                        currentShownLunchBar = Lunchbar.make(this.getView(),
                                this.getString(R.string.wallpaper_lockscreen_error),
                                Lunchbar.LENGTH_LONG);
                        currentShownLunchBar.show();
                        e.printStackTrace();
                    }
                } else if (resultUri.toString().contains("all_wallpaper")) {
                    try {
                        WallpaperManager.setWallpaper(
                                this.mContext,
                                resultUri.toString().substring(7),
                                "all");
                        editor.putString("home_wallpaper_applied", this.theme_pid);
                        editor.putString("lock_wallpaper_applied", this.theme_pid);
                        currentShownLunchBar = Lunchbar.make(this.getView(),
                                this.getString(R.string.wallpaper_allscreen_success),
                                Lunchbar.LENGTH_LONG);
                        currentShownLunchBar.show();
                    } catch (final IOException e) {
                        currentShownLunchBar = Lunchbar.make(this.getView(),
                                this.getString(R.string.wallpaper_allscreen_error),
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
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.information_activity);

        this.mContext = this.getApplicationContext();
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        this.localBroadcastManager = LocalBroadcastManager.getInstance(this.mContext);

        // Register the theme install receiver to auto refresh the fragment
        this.refreshReceiver = new RefreshReceiver();
        this.localBroadcastManager.registerReceiver(this.refreshReceiver,
                new IntentFilter(MANAGER_REFRESH));

        this.activityFinisher = new ActivityFinisher();
        this.localBroadcastManager.registerReceiver(this.activityFinisher,
                new IntentFilter(ACTIVITY_FINISHER));

        if (Systems.isAndromedaDevice(this.mContext)) {
            this.andromedaReceiver = new InformationActivity.AndromedaReceiver();
            this.localBroadcastManager.registerReceiver(this.andromedaReceiver,
                    new IntentFilter("AndromedaReceiver.KILL"));
        }

        final boolean dynamicActionBarColors = this.getResources().getBoolean(R.bool.dynamicActionBarColors);
        final boolean dynamicNavBarColors = this.getResources().getBoolean(R.bool.dynamicNavigationBarColors);

        final Intent currentIntent = this.getIntent();
        this.theme_name = currentIntent.getStringExtra("theme_name");

        this.theme_pid = currentIntent.getStringExtra("theme_pid");
        this.theme_mode = currentIntent.getStringExtra("theme_mode");
        this.encryption_key = currentIntent.getByteArrayExtra("encryption_key");
        this.iv_encrypt_key = currentIntent.getByteArrayExtra("iv_encrypt_key");
        final String wallpaperUrl = getOverlayMetadata(this.mContext, this.theme_pid,
                metadataWallpapers);

        final Bundle bundle = new Bundle();
        bundle.putString("theme_name", this.theme_name);
        bundle.putString("theme_pid", this.theme_pid);
        bundle.putString("theme_mode", this.theme_mode);
        bundle.putByteArray("encryption_key", this.encryption_key);
        bundle.putByteArray("iv_encrypt_key", this.iv_encrypt_key);
        bundle.putString("wallpaperUrl", wallpaperUrl);

        if (this.theme_mode == null) this.theme_mode = "";

        final Toolbar toolbar = this.findViewById(R.id.toolbar);
        if (toolbar != null) toolbar.setTitle(this.theme_name);

        this.gradientView = this.findViewById(R.id.gradientView);
        this.collapsingToolbarLayout = this.findViewById(R.id.collapsing_toolbar_tabbed_layout);
        if (this.collapsingToolbarLayout != null) this.collapsingToolbarLayout.setTitle(this.theme_name);

        final ViewPager viewPager = this.findViewById(R.id.viewpager);

        this.setSupportActionBar(toolbar);
        if (this.getSupportActionBar() != null) {
            this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            this.getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        if (toolbar != null) toolbar.setNavigationOnClickListener(v -> this.onBackPressed());

        final Drawable heroImage = getPackageHeroImage(this.getApplicationContext(), this.theme_pid, false);
        if (heroImage != null) this.heroImageBitmap = ((BitmapDrawable) heroImage).getBitmap();
        if (this.heroImageBitmap == null) {
            this.dominantColor = Color.TRANSPARENT;
        } else {
            this.dominantColor = getDominantColor(this.heroImageBitmap);
        }

        this.appBarLayout = this.findViewById(R.id.appbar);
        if (this.appBarLayout != null) this.appBarLayout.setBackgroundColor(this.dominantColor);

        if (this.collapsingToolbarLayout != null &&
                dynamicActionBarColors) {
            this.collapsingToolbarLayout.setStatusBarScrimColor(this.dominantColor);
            this.collapsingToolbarLayout.setContentScrimColor(this.dominantColor);
        }

        if (dynamicNavBarColors) {
            this.getWindow().setNavigationBarColor(this.dominantColor);
            if (this.checkColorDarkness(this.dominantColor)) {
                this.getWindow().setNavigationBarColor(this.getColor(R.color.theme_information_background));
            }
        }

        final View sheetView = this.findViewById(R.id.fab_sheet);
        final View overlay = this.findViewById(R.id.overlay);
        final int sheetColor = this.mContext.getColor(R.color.fab_menu_background_card);
        final int fabColor = this.mContext.getColor(R.color.fab_background_color);

        final FloatingActionMenu floatingActionButton = this.findViewById(R.id.apply_fab);
        floatingActionButton.show();

        // Create material sheet FAB
        if (sheetView != null && overlay != null) {
            this.materialSheetFab = new MaterialSheetFab<>(
                    floatingActionButton,
                    sheetView,
                    overlay,
                    sheetColor,
                    fabColor);
        }

        new LayoutLoader(this).execute("");
        this.tabLayout = this.findViewById(R.id.tabs);
        if (this.tabLayout != null) {
            // First, take account for whether the theme was launched normally
            if ("".equals(this.theme_mode)) {
                try {
                    final Context otherContext = this.mContext.createPackageContext
                            (this.theme_pid, 0);
                    final AssetManager am = otherContext.getAssets();
                    final List found_folders = Arrays.asList(am.list(""));
                    tab_checker = new ArrayList<>();
                    if (!Systems.checkOMS(this.mContext)) {
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
                        this.tabLayout.addTab(this.tabLayout.newTab().setText(this.getString(R.string
                                .theme_information_tab_one)));
                    }
                    if (tab_checker.contains(bootAnimationsFragment) &&
                            Resources.isBootAnimationSupported(this.mContext)) {
                        isWallpaperOnly = false;
                        this.tabLayout.addTab(this.tabLayout.newTab().setText(this.getString(R.string
                                .theme_information_tab_two)));
                    }
                    if (tab_checker.contains(shutdownAnimationsFragment) &&
                            Resources.isShutdownAnimationSupported()) {
                        isWallpaperOnly = false;
                        this.tabLayout.addTab(this.tabLayout.newTab().setText(this.getString(R.string
                                .theme_information_tab_six)));
                    }
                    if (tab_checker.contains(fontsFragment) && Resources.isFontsSupported()) {
                        isWallpaperOnly = false;
                        this.tabLayout.addTab(this.tabLayout.newTab().setText(this.getString(R.string
                                .theme_information_tab_three)));
                    }
                    if (tab_checker.contains(soundsFragment) &&
                            Resources.isSoundsSupported(this.mContext)) {
                        isWallpaperOnly = false;
                        this.tabLayout.addTab(this.tabLayout.newTab().setText(this.getString(R.string
                                .theme_information_tab_four)));
                    }
                    if (wallpaperUrl != null && !wallpaperUrl.isEmpty()) {
                        this.tabLayout.addTab(this.tabLayout.newTab().setText(this.getString(R.string
                                .theme_information_tab_five)));
                    }
                    if (isWallpaperOnly && wallpaperUrl != null && !wallpaperUrl.isEmpty()) {
                        final Handler handler = new Handler();
                        handler.postDelayed(() ->
                                this.runOnUiThread(floatingActionButton::hide), 500);
                    }
                } catch (final Exception e) {
                    Log.e(References.SUBSTRATUM_LOG, "Could not refresh list of asset folders.");
                }
            } else {
                // At this point, theme was launched in their tab specific sections
                switch (this.theme_mode) {
                    case overlaysFragment:
                        this.tabLayout.addTab(this.tabLayout.newTab().setText(
                                this.getString(R.string.theme_information_tab_one)));
                        break;
                    case bootAnimationsFragment:
                        this.tabLayout.addTab(this.tabLayout.newTab().setText(
                                this.getString(R.string.theme_information_tab_two)));
                        break;
                    case shutdownAnimationsFragment:
                        this.tabLayout.addTab(this.tabLayout.newTab().setText(
                                this.getString(R.string.theme_information_tab_six)));
                        break;
                    case fontsFragment:
                        this.tabLayout.addTab(this.tabLayout.newTab().setText(
                                this.getString(R.string.theme_information_tab_three)));
                        break;
                    case soundsFragment:
                        this.tabLayout.addTab(this.tabLayout.newTab().setText(
                                this.getString(R.string.theme_information_tab_four)));
                        break;
                    case wallpaperFragment:
                        this.tabLayout.addTab(this.tabLayout.newTab().setText(
                                this.getString(R.string.theme_information_tab_five)));
                        break;
                }
            }

            this.tabLayout.setTabGravity(TabLayout.MODE_SCROLLABLE);
            if (dynamicActionBarColors)
                this.tabLayout.setBackgroundColor(this.dominantColor);

            final String toOverrideHero =
                    Packages.getOverlayMetadata(
                            this.getApplicationContext(),
                            this.theme_pid,
                            metadataHeroOverride);
            if (toOverrideHero != null) {
                switch (toOverrideHero) {
                    case "dark":
                        this.setDarkToolbarIcons();
                        break;
                    case "light":
                        this.setLightToolbarIcons();
                        break;
                    default:
                        this.autoSetToolbarIcons(dynamicActionBarColors);
                        break;
                }
            } else {
                this.autoSetToolbarIcons(dynamicActionBarColors);
            }
        }

        final HashMap<String, Boolean> extra_hidden_tabs = new HashMap<>();
        // Boot animation visibility
        extra_hidden_tabs.put(bootAnimationsFragment, !Systems.checkAndromeda(this.mContext) &&
                !Systems.isSamsungDevice(this.mContext));
        // Shutdown animation visibility
        extra_hidden_tabs.put(shutdownAnimationsFragment, Systems.checkOreo() &&
                Systems.isBinderInterfacer(this.mContext));
        // Fonts visibility
        extra_hidden_tabs.put(fontsFragment, Resources.isFontsSupported());
        // Sounds visibility
        extra_hidden_tabs.put(soundsFragment, Systems.isBinderInterfacer(this.mContext));

        final InformationTabsAdapter adapter = new InformationTabsAdapter
                (this.getSupportFragmentManager(), (this.tabLayout != null) ? this.tabLayout.getTabCount() : 0,
                        this.theme_mode, tab_checker, wallpaperUrl, extra_hidden_tabs, bundle);

        if (viewPager != null) {
            viewPager.setOffscreenPageLimit((this.tabLayout != null) ? this.tabLayout.getTabCount() : 0);
            viewPager.setAdapter(adapter);
            viewPager.addOnPageChangeListener(
                    new TabLayout.TabLayoutOnPageChangeListener(InformationActivity.this.tabLayout) {
                        @Override
                        public void onPageSelected(final int position) {
                            InformationActivity.this.tabPosition = position;
                            switch (viewPager.getAdapter().instantiateItem(viewPager, InformationActivity.this.tabPosition)
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
            if (this.tabLayout != null) this.tabLayout.addOnTabSelectedListener(
                    new TabLayout.OnTabSelectedListener() {
                        @Override
                        public void onTabSelected(final TabLayout.Tab tab) {
                            viewPager.setCurrentItem(tab.getPosition());
                        }

                        @Override
                        public void onTabUnselected(final TabLayout.Tab tab) {
                        }

                        @Override
                        public void onTabReselected(final TabLayout.Tab tab) {
                            viewPager.setCurrentItem(tab.getPosition());
                        }
                    });

            final PagerAdapter adapt = viewPager.getAdapter();
            final LocalBroadcastManager localBroadcastManager =
                    LocalBroadcastManager.getInstance(this.mContext);
            floatingActionButton.setOnClickListener(v -> {
                try {
                    final boolean isLunchbarOpen = this.closeAllLunchBars();
                    final Handler handler = new Handler();
                    handler.postDelayed(() -> {
                        final Intent intent;
                        final Object obj = adapt.instantiateItem(viewPager, this.tabPosition);
                        switch (obj.getClass().getSimpleName()) {
                            case "Overlays":
                                this.materialSheetFab.showSheet();
                                break;
                            case "BootAnimations":
                                final boolean isShutdownTab = ((BootAnimations) obj).isShutdownTab();
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
                } catch (final NullPointerException npe) {
                    // Suppress warning
                }
            });

            final Intent intent = new Intent("Overlays.START_JOB");
            final Switch enable_swap = this.findViewById(R.id.enable_swap);
            if (!Systems.checkOMS(this) && !Systems.isSamsung(this.mContext)) {
                enable_swap.setText(this.getString(R.string.fab_menu_swap_toggle_legacy));
            } else if (Systems.isSamsung(this.mContext)) {
                final View fab_menu_divider = this.findViewById(R.id.fab_menu_divider);
                fab_menu_divider.setVisibility(View.GONE);
                enable_swap.setVisibility(View.GONE);
            }
            if (enable_swap != null) {
                final boolean enabled = this.prefs.getBoolean("enable_swapping_overlays", true);
                intent.putExtra("command", "MixAndMatchMode");
                intent.putExtra("newValue", enabled);
                localBroadcastManager.sendBroadcast(intent);
                enable_swap.setChecked(enabled);

                enable_swap.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    this.prefs.edit().putBoolean("enable_swapping_overlays", isChecked).apply();
                    intent.putExtra("command", "MixAndMatchMode");
                    intent.putExtra("newValue", isChecked);
                    localBroadcastManager.sendBroadcast(intent);
                });
            }

            final TextView compile_enable_selected = this.findViewById(R.id.compile_enable_selected);
            if (!Systems.checkOMS(this)) compile_enable_selected.setVisibility(View.GONE);
            if (compile_enable_selected != null) {
                compile_enable_selected.setOnClickListener(v -> {
                    this.materialSheetFab.setEventListener(new MaterialSheetFabEventListener() {
                        @Override
                        public void onSheetHidden() {
                            super.onSheetHidden();
                            intent.putExtra("command", "CompileEnable");
                            localBroadcastManager.sendBroadcast(intent);
                            InformationActivity.this.materialSheetFab.setEventListener(null);
                        }
                    });
                    this.materialSheetFab.hideSheet();
                });
            }

            final TextView compile_update_selected = this.findViewById(R.id.compile_update_selected);
            if (!Systems.checkOMS(this)) {
                compile_update_selected.setText(this.getString(R.string.fab_menu_compile_install));
            }
            if (compile_update_selected != null) {
                compile_update_selected.setOnClickListener(v -> {
                    this.materialSheetFab.setEventListener(new MaterialSheetFabEventListener() {
                        @Override
                        public void onSheetHidden() {
                            super.onSheetHidden();
                            intent.putExtra("command", "CompileUpdate");
                            localBroadcastManager.sendBroadcast(intent);
                            InformationActivity.this.materialSheetFab.setEventListener(null);
                        }
                    });
                    this.materialSheetFab.hideSheet();
                });
            }

            final TextView disable_selected = this.findViewById(R.id.disable_selected);
            if (!Systems.checkOMS(this)) {
                disable_selected.setText(this.getString(R.string.fab_menu_uninstall));
            }
            if (disable_selected != null) {
                disable_selected.setOnClickListener(v -> {
                    this.materialSheetFab.setEventListener(new MaterialSheetFabEventListener() {
                        @Override
                        public void onSheetHidden() {
                            super.onSheetHidden();
                            intent.putExtra("command", "Disable");
                            localBroadcastManager.sendBroadcast(intent);
                            InformationActivity.this.materialSheetFab.setEventListener(null);
                        }
                    });
                    this.materialSheetFab.hideSheet();
                });
            }

            final LinearLayout enable_zone = this.findViewById(R.id.enable);
            if (!Systems.checkOMS(this)) enable_zone.setVisibility(View.GONE);
            final TextView enable_selected = this.findViewById(R.id.enable_selected);
            if (enable_selected != null) {
                enable_selected.setOnClickListener(v -> {
                    this.materialSheetFab.setEventListener(new MaterialSheetFabEventListener() {
                        @Override
                        public void onSheetHidden() {
                            super.onSheetHidden();
                            intent.putExtra("command", "Enable");
                            localBroadcastManager.sendBroadcast(intent);
                            InformationActivity.this.materialSheetFab.setEventListener(null);
                        }
                    });
                    this.materialSheetFab.hideSheet();
                });
            }

            final TextView enable_disable_selected =
                    this.findViewById(R.id.enable_disable_selected);
            if (!Systems.checkOMS(this))
                enable_disable_selected.setVisibility(View.GONE);
            if (enable_disable_selected != null) {
                enable_disable_selected.setOnClickListener(v -> {
                    this.materialSheetFab.setEventListener(new MaterialSheetFabEventListener() {
                        @Override
                        public void onSheetHidden() {
                            super.onSheetHidden();
                            intent.putExtra("command", "EnableDisable");
                            localBroadcastManager.sendBroadcast(intent);
                            InformationActivity.this.materialSheetFab.setEventListener(null);
                        }
                    });
                    this.materialSheetFab.hideSheet();
                });
            }

            final Boolean shouldShowSamsungWarning =
                    !this.prefs.getBoolean("show_dangerous_samsung_overlays", false);
            if (Systems.isSamsung(this.mContext) &&
                    !Packages.isSamsungTheme(this.mContext, this.theme_pid) &&
                    shouldShowSamsungWarning) {
                currentShownLunchBar = Lunchbar.make(
                        this.getView(),
                        R.string.toast_samsung_prototype_alert,
                        Lunchbar.LENGTH_SHORT);
                currentShownLunchBar.show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        this.getMenuInflater().inflate(R.menu.theme_information_menu, menu);

        // Start formalizing a check for dark icons
        final boolean dynamicActionBarColors = this.getResources().getBoolean(R.bool.dynamicActionBarColors);
        this.shouldDarken = this.collapsingToolbarLayout != null &&
                this.checkColorDarkness(this.dominantColor) &&
                dynamicActionBarColors;

        // Start dynamically showing menu items
        final boolean isOMS = Systems.checkOMS(this.mContext);
        final boolean isMR1orHigher = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1;

        if (!isMR1orHigher) menu.findItem(R.id.favorite).setVisible(false);
        if (isMR1orHigher) {
            this.favorite = menu.findItem(R.id.favorite);
            if (this.prefs.contains("app_shortcut_theme")) {
                if (this.prefs.getString("app_shortcut_theme", "").equals(this.theme_pid)) {
                    this.favorite.setIcon(this.getDrawable(R.drawable.toolbar_favorite));
                } else {
                    this.favorite.setIcon(this.getDrawable(R.drawable.toolbar_not_favorite));
                }
            } else {
                this.favorite.setIcon(this.getDrawable(R.drawable.toolbar_not_favorite));
            }
            if (this.shouldDarken) this.favorite.getIcon().setColorFilter(
                    this.getColor(R.color.information_activity_dark_icon_mode),
                    PorterDuff.Mode.SRC_ATOP);
        }
        if (Packages.getThemeChangelog(this.mContext, this.theme_pid) != null) {
            final MenuItem changelog = menu.findItem(R.id.changelog);
            changelog.setVisible(true);
            if (this.shouldDarken) changelog.getIcon().setColorFilter(
                    this.getColor(R.color.information_activity_dark_icon_mode),
                    PorterDuff.Mode.SRC_ATOP);
        }

        if (Systems.checkAndromeda(this.mContext) ||
                (!isOMS && !Root.checkRootAccess())) {
            menu.findItem(R.id.restart_systemui).setVisible(false);
        }
        if (!isOMS) {
            menu.findItem(R.id.disable).setVisible(false);
            menu.findItem(R.id.enable).setVisible(false);
        }
        if (isOMS || isSamsung(this.mContext)) {
            if (isSamsung(this.mContext)) menu.findItem(R.id.clean).setVisible(false);
            menu.findItem(R.id.reboot_device).setVisible(false);
            menu.findItem(R.id.soft_reboot).setVisible(false);
            menu.findItem(R.id.uninstall).setVisible(false);
        }

        if (!Packages.isUserApp(this.mContext, this.theme_pid)) {
            menu.findItem(R.id.uninstall).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();

        switch (id) {
            case R.id.favorite:
                if (this.prefs.contains("app_shortcut_theme")) {
                    if (this.prefs.getString("app_shortcut_theme", "").equals(this.theme_pid)) {
                        new AppShortcutClearer(this).execute("");
                    } else {
                        new AppShortcutCreator(this).execute("favorite");
                    }
                } else {
                    new AppShortcutCreator(this).execute("favorite");
                }
                return true;
            case R.id.changelog:
                final SheetDialog sheetDialog = new SheetDialog(this);
                @SuppressLint("InflateParams") final View sheetView = this.getLayoutInflater().inflate(R.layout.changelog_sheet_dialog, null);

                final LinearLayout titleBox = sheetView.findViewById(R.id.title_box);
                final TextView title = titleBox.findViewById(R.id.title);
                final String format_me = String.format(this.getString(R.string.changelog_title), this.theme_name);
                title.setText(format_me);

                final LinearLayout textBox = sheetView.findViewById(R.id.text_box);
                final TextView text = textBox.findViewById(R.id.text);

                final String[] changelog_parsing =
                        Packages.getThemeChangelog(this.mContext, this.theme_pid);
                final StringBuilder to_show = new StringBuilder();
                if (changelog_parsing != null) {
                    for (final String aChangelog_parsing : changelog_parsing) {
                        to_show.append("\u2022 ").append(aChangelog_parsing).append("\n");
                    }
                }
                text.setText(to_show.toString());
                sheetDialog.setCanceledOnTouchOutside(true);
                sheetDialog.setContentView(sheetView);
                sheetDialog.show();
                return true;
            case R.id.clean:
                final AlertDialog.Builder builder1 = new AlertDialog.Builder(InformationActivity.this);
                builder1.setTitle(this.theme_name);
                builder1.setIcon(Packages.getAppIcon(this.mContext, this.theme_pid));
                builder1.setMessage(R.string.clean_dialog_body)
                        .setPositiveButton(R.string.dialog_ok, (dialog, id18) -> {
                            // Dismiss the dialog
                            dialog.dismiss();
                            // Get all installed overlays
                            List<String> stateAll = ThemeManager.listAllOverlays(
                                    this.mContext);

                            ArrayList<String> all_overlays = new ArrayList<>();
                            for (int j = 0; j < stateAll.size(); j++) {
                                try {
                                    String current = stateAll.get(j);
                                    ApplicationInfo appInfo = this.mContext
                                            .getPackageManager().getApplicationInfo(
                                                    current, PackageManager.GET_META_DATA);
                                    if (appInfo.metaData != null &&
                                            appInfo.metaData.getString(
                                                    metadataOverlayParent) != null) {
                                        String parent =
                                                appInfo.metaData.getString(metadataOverlayParent);
                                        if (parent != null && parent.equals(this.theme_pid)) {
                                            all_overlays.add(current);
                                        }
                                    }
                                } catch (Exception e) {
                                    // NameNotFound
                                }
                            }

                            // Begin uninstalling overlays for this package
                            ThemeManager.uninstallOverlay(
                                    this.mContext,
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
                final AlertDialog.Builder builder3 = new AlertDialog.Builder(InformationActivity.this);
                builder3.setTitle(this.theme_name);
                builder3.setIcon(Packages.getAppIcon(this.mContext, this.theme_pid));
                builder3.setMessage(R.string.disable_dialog_body)
                        .setPositiveButton(R.string.dialog_ok, (dialog, id16) -> {
                            // Dismiss the dialog
                            dialog.dismiss();
                            // Get all enabled overlays
                            List<String> stateAll = ThemeManager.listOverlays(
                                    this.mContext, ThemeManager.STATE_ENABLED);

                            ArrayList<String> all_overlays = new ArrayList<>();
                            for (int j = 0; j < stateAll.size(); j++) {
                                try {
                                    String current = stateAll.get(j);
                                    ApplicationInfo appInfo = this.mContext
                                            .getPackageManager().getApplicationInfo(
                                                    current, PackageManager.GET_META_DATA);
                                    if (appInfo.metaData != null &&
                                            appInfo.metaData.getString(
                                                    metadataOverlayParent) != null) {
                                        String parent =
                                                appInfo.metaData.getString(metadataOverlayParent);
                                        if (parent != null && parent.equals(this.theme_pid)) {
                                            all_overlays.add(current);
                                        }
                                    }
                                } catch (Exception e) {
                                    // NameNotFound
                                }
                            }
                            currentShownLunchBar = Lunchbar.make(this.getView(),
                                    this.getString(R.string.disable_completion),
                                    Lunchbar.LENGTH_LONG);
                            currentShownLunchBar.show();
                            // Begin disabling overlays
                            ThemeManager.disableOverlay(this.mContext, all_overlays);
                        })
                        .setNegativeButton(R.string.dialog_cancel, (dialog, id15) -> {
                            // User cancelled the dialog
                        });
                // Create the AlertDialog object and return it
                builder3.create();
                builder3.show();
                return true;
            case R.id.enable:
                final AlertDialog.Builder builder4 = new AlertDialog.Builder(InformationActivity.this);
                builder4.setTitle(this.theme_name);
                builder4.setIcon(Packages.getAppIcon(this.mContext, this.theme_pid));
                builder4.setMessage(R.string.enable_dialog_body)
                        .setPositiveButton(R.string.dialog_ok, (dialog, id14) -> {
                            // Dismiss the dialog
                            dialog.dismiss();
                            // Get all disabled overlays
                            List<String> stateAll = ThemeManager.listOverlays(
                                    this.mContext, ThemeManager.STATE_DISABLED);

                            ArrayList<String> all_overlays = new ArrayList<>();
                            for (int j = 0; j < stateAll.size(); j++) {
                                try {
                                    String current = stateAll.get(j);
                                    ApplicationInfo appInfo = this.mContext
                                            .getPackageManager().getApplicationInfo(
                                                    current, PackageManager.GET_META_DATA);
                                    if (appInfo.metaData != null &&
                                            appInfo.metaData.getString(
                                                    metadataOverlayParent) != null) {
                                        String parent =
                                                appInfo.metaData.getString(metadataOverlayParent);
                                        if (parent != null && parent.equals(this.theme_pid)) {
                                            all_overlays.add(current);
                                        }
                                    }
                                } catch (Exception e) {
                                    // NameNotFound
                                }
                            }
                            currentShownLunchBar = Lunchbar.make(this.getView(),
                                    this.getString(R.string.enable_completion),
                                    Lunchbar.LENGTH_LONG);
                            currentShownLunchBar.show();

                            // Begin enabling overlays
                            ThemeManager.enableOverlay(this.mContext, all_overlays);
                        })
                        .setNegativeButton(R.string.dialog_cancel, (dialog, id13) ->
                                dialog
                                        .cancel());
                // Create the AlertDialog object and return it
                builder4.create();
                builder4.show();
                return true;
            case R.id.uninstall:
                final AlertDialog.Builder builder5 = new AlertDialog.Builder(InformationActivity.this);
                builder5.setTitle(this.theme_name);
                builder5.setIcon(Packages.getAppIcon(this.mContext, this.theme_pid));
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
                ThemeManager.restartSystemUI(this.mContext);
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
        if (this.materialSheetFab.isSheetVisible()) {
            this.materialSheetFab.hideSheet();
        } else {
            if (this.uninstalled)
                Broadcasts.sendRefreshMessage(this.mContext);
            this.finish();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Close the active compiling notification if the app was closed from recents
        final NotificationManager manager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(References.notification_id_compiler);
        }

        try {
            this.localBroadcastManager.unregisterReceiver(this.refreshReceiver);
        } catch (final Exception e) {
            // Unregistered already
        }

        try {
            this.localBroadcastManager.unregisterReceiver(this.activityFinisher);
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

        if (!BYPASS_SUBSTRATUM_BUILDER_DELETION) {
            final String workingDirectory =
                    this.mContext.getCacheDir().getAbsolutePath();
            final File deleted = new File(workingDirectory);
            FileOperations.delete(this.mContext, deleted.getAbsolutePath());
            if (!deleted.exists()) Log.d(References.SUBSTRATUM_BUILDER,
                    "Successfully cleared Substratum cache!");
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

    private static class LayoutLoader extends AsyncTask<String, Integer, String> {
        private final WeakReference<InformationActivity> ref;

        LayoutLoader(final InformationActivity informationActivity) {
            super();
            this.ref = new WeakReference<>(informationActivity);
        }

        @Override
        protected void onPostExecute(final String result) {
            super.onPostExecute(result);
            final InformationActivity informationActivity = this.ref.get();
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
                    final Bitmap bitmap = BitmapFactory.decodeByteArray(
                            informationActivity.byteArray, 0, informationActivity.byteArray.length);
                    informationActivity.kenBurnsView.setImageBitmap(bitmap);
                }
            }
        }

        @Override
        protected String doInBackground(final String... sUrl) {
            final InformationActivity informationActivity = this.ref.get();
            if (informationActivity != null) {
                informationActivity.kenBurnsView =
                        informationActivity.findViewById(R.id.kenburnsView);
                if (informationActivity.heroImageBitmap != null) {
                    final ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    informationActivity.heroImageBitmap.compress(
                            Bitmap.CompressFormat.JPEG, 100, stream);
                    informationActivity.byteArray = stream.toByteArray();
                }
            }
            return null;
        }
    }

    private static class AppShortcutCreator extends AsyncTask<String, Integer, String> {
        private final WeakReference<InformationActivity> ref;

        AppShortcutCreator(final InformationActivity informationActivity) {
            super();
            this.ref = new WeakReference<>(informationActivity);
        }

        @Override
        protected void onPostExecute(final String result) {
            final InformationActivity informationActivity = this.ref.get();
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
                final String format = String.format(
                        informationActivity.getString(R.string.menu_favorite_snackbar), result);
                currentShownLunchBar = Lunchbar.make(
                        informationActivity.getView(),
                        format,
                        Lunchbar.LENGTH_LONG);
                currentShownLunchBar.show();
            }
        }

        @Override
        protected String doInBackground(final String... sUrl) {
            final InformationActivity informationActivity = this.ref.get();
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
        private final WeakReference<InformationActivity> ref;

        AppShortcutClearer(final InformationActivity informationActivity) {
            super();
            this.ref = new WeakReference<>(informationActivity);
        }

        @Override
        protected void onPostExecute(final String result) {
            final InformationActivity informationActivity = this.ref.get();
            if (informationActivity != null) {
                informationActivity.prefs.edit().remove("app_shortcut_theme").apply();
                informationActivity.favorite.setIcon(
                        informationActivity.getDrawable(R.drawable.toolbar_not_favorite));
                if (informationActivity.shouldDarken)
                    informationActivity.favorite.getIcon().setColorFilter(
                            informationActivity.getColor(R.color
                                    .information_activity_dark_icon_mode),
                            PorterDuff.Mode.SRC_ATOP);
                final String format = String.format(
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
        protected String doInBackground(final String... sUrl) {
            final InformationActivity informationActivity = this.ref.get();
            if (informationActivity != null) {
                References.clearShortcut(informationActivity.mContext);
                return informationActivity.theme_name;
            }
            return null;
        }
    }

    private static class uninstallTheme extends AsyncTask<String, Integer, String> {
        private final WeakReference<InformationActivity> ref;

        uninstallTheme(final InformationActivity informationActivity) {
            super();
            this.ref = new WeakReference<>(informationActivity);
        }

        @Override
        protected void onPreExecute() {
            final InformationActivity informationActivity = this.ref.get();
            if (informationActivity != null) {
                final String parseMe = String.format(
                        informationActivity.getString(R.string.adapter_uninstalling),
                        informationActivity.theme_name);

                informationActivity.mProgressDialog = new ProgressDialog(informationActivity);
                informationActivity.mProgressDialog.setMessage(parseMe);
                informationActivity.mProgressDialog.setIndeterminate(true);
                informationActivity.mProgressDialog.setCancelable(false);
                informationActivity.mProgressDialog.show();
                // Clear the notification of building theme if shown
                final NotificationManager manager = (NotificationManager)
                        informationActivity.mContext
                                .getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager != null) {
                    manager.cancel(References.notification_id_compiler);
                }
            }
        }

        @Override
        protected void onPostExecute(final String result) {
            final InformationActivity informationActivity = this.ref.get();
            if (informationActivity != null) {
                informationActivity.mProgressDialog.cancel();
                informationActivity.uninstalled = true;
                informationActivity.onBackPressed();
            }
        }

        @Override
        protected String doInBackground(final String... sUrl) {
            final InformationActivity informationActivity = this.ref.get();
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
        public void onReceive(final Context context, final Intent intent) {
            if (!Packages.isPackageInstalled(context, InformationActivity.this.theme_pid)) {
                Log.d("ThemeUninstaller",
                        "The theme was uninstalled, so the activity is now closing!");
                Broadcasts.sendRefreshMessage(context);
                InformationActivity.this.finish();
            }
        }
    }

    class AndromedaReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            InformationActivity.this.finish();
        }
    }

    class ActivityFinisher extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (intent != null && !compilingProcess) {
                final String package_name = intent.getStringExtra("theme_pid");
                if (package_name != null && package_name.equals(InformationActivity.this.theme_pid)) {
                    final String to_format = String.format(InformationActivity.this.getString(R.string.toast_activity_finished),
                            InformationActivity.this.theme_name);
                    Log.d(SUBSTRATUM_LOG,
                            InformationActivity.this.theme_name + " was just updated, now closing InformationActivity...");
                    InformationActivity.this.createToast(to_format, Toast.LENGTH_LONG);
                    InformationActivity.this.finish();
                    final Handler handler = new Handler();
                    handler.postDelayed(() ->
                            Theming.launchTheme(InformationActivity.this.mContext, InformationActivity.this.theme_pid, InformationActivity.this.theme_mode), 500);
                }
            } else if (compilingProcess) {
                Log.d(SUBSTRATUM_LOG,
                        "Tried to restart activity but theme was compiling, delaying...");
                shouldRestartActivity = true;
            }
        }
    }
}
