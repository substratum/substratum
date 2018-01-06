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

package projekt.substratum.adapters.fragments.themes;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import projekt.substratum.MainActivity;
import projekt.substratum.R;
import projekt.substratum.Substratum;
import projekt.substratum.activities.launch.KeyExchangeActivity;
import projekt.substratum.activities.launch.ThemeLaunchActivity;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.util.views.SheetDialog;

import static projekt.substratum.activities.launch.ThemeLaunchActivity.launchThemeActivity;
import static projekt.substratum.common.Internal.KEY_ACCEPTED_RECEIVER;
import static projekt.substratum.common.Internal.PLAY_URL_PREFIX;
import static projekt.substratum.common.Internal.THEME_READY_ALL;
import static projekt.substratum.common.Internal.THEME_READY_READY;
import static projekt.substratum.common.Internal.THEME_READY_STOCK;
import static projekt.substratum.common.References.PLAY_STORE_PACKAGE_NAME;
import static projekt.substratum.common.References.TEMPLATE_THEME_MODE;
import static projekt.substratum.common.Theming.isThemeUsingDefaultTheme;
import static projekt.substratum.common.Theming.themeIntent;


public class ThemeAdapter extends RecyclerView.Adapter<ThemeAdapter.ViewHolder> {
    private List<ThemeItem> information;
    private Context mContext;
    private LocalBroadcastManager localBroadcastManager;
    private LaunchThemeReceiver launchThemeReceiver;
    private ThemeItem themeItem;
    private Boolean isUsingDefaultTheme;
    private ActivityOptions options;

    public ThemeAdapter(List<ThemeItem> information) {
        super();
        this.information = information;
    }

    @Override
    public ThemeAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup,
                                                      int i) {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(viewGroup.getContext());
        View view;
        if (!prefs.getBoolean("advanced_ui", false)) {
            view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.special_entry_card,
                    viewGroup, false);
        } else if (prefs.getBoolean("nougat_style_cards", false)) {
            view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.theme_entry_card_n,
                    viewGroup, false);
        } else {
            view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.theme_entry_card,
                    viewGroup, false);
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder,
                                 int pos) {
        ThemeItem themeItem = this.information.get(pos);
        this.mContext = themeItem.getContext();

        ViewCompat.setTransitionName(viewHolder.imageView, themeItem.getThemePackage());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        viewHolder.theme_name.setText(themeItem.getThemeName());
        viewHolder.theme_author.setText(themeItem.getThemeAuthor());
        if (prefs.getBoolean("advanced_ui", false)) {
            if (themeItem.getPluginVersion() != null) {
                viewHolder.plugin_version.setText(themeItem.getPluginVersion());
            } else {
                viewHolder.plugin_version.setVisibility(View.INVISIBLE);
            }
            if (themeItem.getSDKLevels() != null) {
                viewHolder.theme_apis.setText(themeItem.getSDKLevels());
            } else {
                viewHolder.theme_apis.setVisibility(View.INVISIBLE);
            }
            if (themeItem.getThemeVersion() != null) {
                viewHolder.theme_version.setText(themeItem.getThemeVersion());
            } else {
                viewHolder.theme_version.setVisibility(View.INVISIBLE);
            }

            if (prefs.getBoolean("grid_layout", true) ||
                    (themeItem.getThemeReadyVariable() == null)) {
                viewHolder.divider.setVisibility(View.GONE);
                viewHolder.tbo.setVisibility(View.GONE);
                viewHolder.two.setVisibility(View.GONE);
            } else if (THEME_READY_ALL.equals(themeItem.getThemeReadyVariable())) {
                viewHolder.divider.setVisibility(View.VISIBLE);
                viewHolder.tbo.setVisibility(View.VISIBLE);
                viewHolder.two.setVisibility(View.VISIBLE);
            } else if (THEME_READY_READY.equals(themeItem.getThemeReadyVariable())) {
                viewHolder.divider.setVisibility(View.VISIBLE);
                viewHolder.tbo.setVisibility(View.VISIBLE);
                viewHolder.two.setVisibility(View.GONE);
            } else if (THEME_READY_STOCK.equals(themeItem.getThemeReadyVariable())) {
                viewHolder.divider.setVisibility(View.VISIBLE);
                viewHolder.tbo.setVisibility(View.GONE);
                viewHolder.two.setVisibility(View.VISIBLE);
            } else {
                viewHolder.divider.setVisibility(View.GONE);
                viewHolder.tbo.setVisibility(View.GONE);
                viewHolder.two.setVisibility(View.GONE);
            }

            viewHolder.tbo.setOnClickListener(v -> this.explainTBO());
            viewHolder.two.setOnClickListener(v -> this.explainTWO());
        }

        viewHolder.cardView.setOnClickListener(
                v -> {
                    MainActivity.themeCardProgressBar = viewHolder.progressBar;
                    MainActivity.themeCardProgressBar.setVisibility(View.VISIBLE);

                    ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
                            MainActivity.mainActivity,
                            viewHolder.imageView,
                            themeItem.getThemePackage()
                    );

                    // Okay you fuck, this fucking works, fucking know this shit or fuck off
                    boolean isUsingDefaultTheme =
                            isThemeUsingDefaultTheme(themeItem.getThemePackage());
                    Intent theme_intent = themeIntent(
                            mContext,
                            themeItem.getThemePackage(),
                            null,
                            TEMPLATE_THEME_MODE,
                            (isUsingDefaultTheme ? KeyExchangeActivity.class :
                                    ThemeLaunchActivity.class));
                    try {
                        mContext.startActivity(theme_intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    launchThemeReceiver = new LaunchThemeReceiver();
                    localBroadcastManager = LocalBroadcastManager.getInstance(mContext);
                    localBroadcastManager.registerReceiver(launchThemeReceiver,
                            new IntentFilter(KEY_ACCEPTED_RECEIVER));

                    this.themeItem = themeItem;
                    this.isUsingDefaultTheme = isUsingDefaultTheme;
                    this.options = options;
                });

        viewHolder.cardView.setOnLongClickListener(view -> {
            // Vibrate the device alerting the user they are about to do something dangerous!
            if (Packages.isUserApp(this.mContext, themeItem.getThemePackage())) {
                Vibrator v = (Vibrator) this.mContext.getSystemService(Context
                        .VIBRATOR_SERVICE);
                if (v != null) {
                    v.vibrate(30L);
                }

                // About the theme
                SheetDialog sheetDialog = new SheetDialog(this.mContext);
                View sheetView =
                        View.inflate(this.mContext, R.layout.theme_long_press_sheet_dialog, null);

                TextView aboutText = sheetView.findViewById(R.id.about_text);
                TextView moreText = sheetView.findViewById(R.id.more_text);
                String boldedThemeName = "<b>" + themeItem.getThemeName() + "</b>";
                aboutText.setText(Html.fromHtml(boldedThemeName, Html.FROM_HTML_MODE_LEGACY));
                moreText.setText(String.format("%s (%s)\n%s",
                        Packages.getAppVersion(this.mContext, themeItem.getThemePackage()),
                        Packages.getAppVersionCode(this.mContext, themeItem.getThemePackage()),
                        Packages.getPackageTemplateVersion(this.mContext,
                                themeItem.getThemePackage())));

                ImageView icon = sheetView.findViewById(R.id.icon);
                icon.setImageDrawable(Packages.getAppIcon(this.mContext, themeItem
                        .getThemePackage()));

                ImageView two = sheetView.findViewById(R.id.theme_unready_indicator);
                ImageView tbo = sheetView.findViewById(R.id.theme_ready_indicator);
                tbo.setOnClickListener(v2 -> this.explainTBO());
                two.setOnClickListener(v2 -> this.explainTWO());

                try {
                    switch (themeItem.getThemeReadyVariable()) {
                        case THEME_READY_ALL:
                            tbo.setVisibility(View.VISIBLE);
                            two.setVisibility(View.VISIBLE);
                            break;
                        case THEME_READY_READY:
                            tbo.setVisibility(View.VISIBLE);
                            two.setVisibility(View.GONE);
                            break;
                        case THEME_READY_STOCK:
                            tbo.setVisibility(View.GONE);
                            two.setVisibility(View.VISIBLE);
                            break;
                        default:
                            tbo.setVisibility(View.GONE);
                            two.setVisibility(View.GONE);
                            break;
                    }
                } catch (Exception ignored) {
                    tbo.setVisibility(View.GONE);
                    two.setVisibility(View.GONE);
                }

                // Favorite
                LinearLayout favorite = sheetView.findViewById(R.id.favorite);

                Drawable favoriteImg = this.mContext.getDrawable(R.drawable.toolbar_favorite);
                Drawable notFavoriteImg =
                        this.mContext.getDrawable(R.drawable.toolbar_not_favorite);
                TextView favoriteText = sheetView.findViewById(R.id.favorite_text);
                if (prefs.getString("app_shortcut_theme", "").equals(themeItem.getThemePackage())) {
                    assert favoriteImg != null;
                    favoriteImg.setBounds(0, 0, 60, 60);
                    favoriteText.setCompoundDrawables(favoriteImg, null, null, null);
                    favoriteText.setText(this.mContext.getString(R.string.menu_unfavorite));
                } else {
                    assert notFavoriteImg != null;
                    notFavoriteImg.setBounds(0, 0, 60, 60);
                    favoriteText.setCompoundDrawables(notFavoriteImg, null, null, null);
                    favoriteText.setText(this.mContext.getString(R.string.menu_favorite));
                }

                favorite.setOnClickListener(view1 -> {
                    if (prefs.contains("app_shortcut_theme")) {
                        if (!prefs.getString("app_shortcut_theme", "").equals(
                                themeItem.getThemePackage())) {
                            prefs.edit().remove("app_shortcut_theme").apply();
                            References.clearShortcut(this.mContext);
                            prefs.edit().putString("app_shortcut_theme",
                                    themeItem.getThemePackage()).apply();
                            References.createShortcut(
                                    this.mContext,
                                    themeItem.getThemePackage(),
                                    themeItem.getThemeName());
                            assert favoriteImg != null;
                            favoriteImg.setBounds(0, 0, 60, 60);
                            favoriteText.setCompoundDrawables(favoriteImg, null, null, null);
                            favoriteText.setText(this.mContext.getString(R.string.menu_unfavorite));
                        } else {
                            prefs.edit().remove("app_shortcut_theme").apply();
                            References.clearShortcut(this.mContext);
                            assert notFavoriteImg != null;
                            notFavoriteImg.setBounds(0, 0, 60, 60);
                            favoriteText.setCompoundDrawables(notFavoriteImg, null, null, null);
                            favoriteText.setText(this.mContext.getString(R.string.menu_favorite));
                        }
                    } else {
                        prefs.edit().putString("app_shortcut_theme",
                                themeItem.getThemePackage()).apply();
                        References.createShortcut(
                                this.mContext,
                                themeItem.getThemePackage(),
                                themeItem.getThemeName());
                        assert favoriteImg != null;
                        favoriteImg.setBounds(0, 0, 60, 60);
                        favoriteText.setCompoundDrawables(favoriteImg, null, null, null);
                        favoriteText.setText(this.mContext.getString(R.string.menu_unfavorite));
                    }
                });

                // Rate Theme
                LinearLayout rate = sheetView.findViewById(R.id.rate);
                String installer =
                        Packages.getInstallerId(this.mContext, themeItem.getThemePackage());
                if ((installer != null) && installer.equals(PLAY_STORE_PACKAGE_NAME)) {
                    rate.setVisibility(View.VISIBLE);
                } else {
                    rate.setVisibility(View.GONE);
                }
                rate.setOnClickListener(view12 -> {
                    try {
                        String playURL = PLAY_URL_PREFIX + themeItem.getThemePackage();
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(playURL));
                        this.mContext.startActivity(i);
                    } catch (ActivityNotFoundException activityNotFoundException) {
                        Toast.makeText(
                                this.mContext,
                                this.mContext.getString(R.string.activity_missing_toast),
                                Toast.LENGTH_SHORT).show();
                    }
                    sheetDialog.dismiss();
                });

                // Shortcut
                LinearLayout shortcut = sheetView.findViewById(R.id.shortcut);
                shortcut.setOnClickListener(view12 -> {
                    References.createLauncherIcon(this.mContext,
                            themeItem.getThemePackage(), themeItem.getThemeName());
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                        Toast.makeText(
                                this.mContext,
                                this.mContext.getString(R.string.launcher_shortcut_toast),
                                Toast.LENGTH_SHORT).show();
                    }
                    sheetDialog.dismiss();
                });

                // Uninstalling
                LinearLayout uninstall = sheetView.findViewById(R.id.uninstall);
                uninstall.setOnClickListener(view2 -> {
                    Uri packageURI = Uri.parse("package:" + themeItem.getThemePackage());
                    Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
                    this.mContext.startActivity(uninstallIntent);
                    sheetDialog.dismiss();
                });

                sheetDialog.setContentView(sheetView);
                sheetDialog.show();
            }
            return false;
        });

        viewHolder.theme_author.setText(themeItem.getThemeAuthor());
        viewHolder.imageView.setImageDrawable(themeItem.getThemeDrawable());
        viewHolder.imageView2.setImageDrawable(themeItem.getThemeDrawable());

        if (prefs.getBoolean("advanced_ui", false)) {
            References.setRecyclerViewAnimation(
                    this.mContext,
                    viewHolder.itemView,
                    R.anim.recyclerview_anim);
        } else {
            References.setRecyclerViewAnimation(
                    this.mContext,
                    viewHolder.itemView,
                    android.R.anim.fade_in);
        }
    }

    private void launchTheme(ThemeItem themeItem,
                             Boolean isUsingDefaultTheme,
                             ActivityOptions options) {
        Boolean checkIfNull = Substratum.currentThemeSecurity == null &&
                Substratum.currentThemeSecurity.getPackageName() != null;

        MainActivity.mainActivity.startActivity(
                launchThemeActivity(
                        Substratum.getInstance(),
                        themeItem.getThemeName(),
                        themeItem.getThemeAuthor().toString(),
                        themeItem.getThemePackage(),
                        themeItem.getThemeMode(),
                        !checkIfNull ? Substratum.currentThemeSecurity.getHash() : null,
                        !checkIfNull ? Substratum.currentThemeSecurity.getLaunchType() : null,
                        !checkIfNull ? Substratum.currentThemeSecurity.getDebug() : null,
                        !checkIfNull ? Substratum.currentThemeSecurity.getPiracyCheck() : null,
                        !checkIfNull ? Substratum.currentThemeSecurity.getEncryptionKey() : null,
                        !checkIfNull ? Substratum.currentThemeSecurity.getIVEncryptKey() : null,
                        Systems.checkOMS(Substratum.getInstance())
                ), (isUsingDefaultTheme ?
                        options != null ?
                                options.toBundle() : null : null));
    }

    /**
     * Show a dialog to explain what Theme Ready Gapps are
     */
    private void explainTBO() {
        new AlertDialog.Builder(this.mContext)
                .setMessage(R.string.tbo_description)
                .setPositiveButton(R.string.tbo_dialog_proceed,
                        (dialog, which) -> {
                            try {
                                String playURL =
                                        this.mContext.getString(R.string.tbo_theme_ready_url);
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(playURL));
                                this.mContext.startActivity(intent);
                            } catch (ActivityNotFoundException anfe) {
                                // Suppress warning
                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        (dialog, which) -> dialog.cancel())
                .setCancelable(true)
                .show();
    }

    /**
     * Show a dialog to explain what stock theming is
     */
    private void explainTWO() {
        new AlertDialog.Builder(this.mContext)
                .setMessage(R.string.two_description)
                .setCancelable(true)
                .setPositiveButton(R.string.dynamic_gapps_dialog,
                        (dialog, which) -> {
                            try {
                                String playURL =
                                        this.mContext.getString(R.string.dynamic_gapps_link);
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(playURL));
                                this.mContext.startActivity(intent);
                            } catch (ActivityNotFoundException anfe) {
                                // Suppress warning
                            }
                        })
                .setNegativeButton(R.string.open_gapps_dialog,
                        (dialog, which) -> {
                            try {
                                String playURL =
                                        this.mContext.getString(R.string.open_gapps_link);
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(playURL));
                                this.mContext.startActivity(intent);
                            } catch (ActivityNotFoundException anfe) {
                                // Suppress warning
                            }
                        })
                .setNeutralButton(android.R.string.cancel,
                        (dialog, which) -> dialog.cancel())
                .show();
    }

    @Override
    public int getItemCount() {
        return this.information.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView theme_name;
        TextView theme_author;
        TextView theme_apis;
        TextView theme_version;
        TextView plugin_version;
        ImageView imageView;
        ImageView imageView2;
        RelativeLayout progressBar;
        View divider;
        ImageView tbo;
        ImageView two;

        ViewHolder(View view) {
            super(view);
            this.cardView = view.findViewById(R.id.theme_card);
            this.theme_name = view.findViewById(R.id.theme_name);
            this.theme_author = view.findViewById(R.id.theme_author);
            this.theme_apis = view.findViewById(R.id.api_levels);
            this.theme_version = view.findViewById(R.id.theme_version);
            this.plugin_version = view.findViewById(R.id.plugin_version);
            this.imageView = view.findViewById(R.id.theme_preview_image);
            this.imageView2 = view.findViewById(R.id.theme_preview_image_backup);
            this.progressBar = view.findViewById(R.id.loading_theme);
            this.divider = view.findViewById(R.id.theme_ready_divider);
            this.tbo = view.findViewById(R.id.theme_ready_indicator);
            this.two = view.findViewById(R.id.theme_unready_indicator);
        }
    }

    /**
     * Receiver to kill the activity
     */
    class LaunchThemeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            launchTheme(themeItem, isUsingDefaultTheme, options);
            themeItem = null;
            isUsingDefaultTheme = null;
            options = null;
            Substratum.currentThemeSecurity = null;
            try {
                localBroadcastManager.unregisterReceiver(launchThemeReceiver);
            } catch (Exception e) {
                // Unregistered already
            }
        }
    }
}