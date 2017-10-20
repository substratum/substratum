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

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import projekt.substratum.R;
import projekt.substratum.common.Broadcasts;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.common.Theming;
import projekt.substratum.util.views.SheetDialog;

import static projekt.substratum.common.References.PLAY_STORE_PACKAGE_NAME;


public class ThemeAdapter extends RecyclerView.Adapter<ThemeAdapter.ViewHolder> {
    private List<ThemeItem> information;
    private Context mContext;
    private ProgressDialog mProgressDialog;
    private ThemeItem toBeUninstalled;

    public ThemeAdapter(List<ThemeItem> information) {
        this.information = information;
    }

    @Override
    public ThemeAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(viewGroup.getContext());
        View view;
        if (prefs.getBoolean("nougat_style_cards", false)) {
            view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.theme_entry_card_n,
                    viewGroup, false);
        } else {
            view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.theme_entry_card,
                    viewGroup, false);
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int pos) {
        ThemeItem themeItem = information.get(pos);
        mContext = themeItem.getContext();

        viewHolder.theme_name.setText(themeItem.getThemeName());
        viewHolder.theme_author.setText(themeItem.getThemeAuthor());
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

        SharedPreferences pref =
                PreferenceManager.getDefaultSharedPreferences(themeItem.getContext());
        if (pref.getBoolean("grid_layout", true) || themeItem.getThemeReadyVariable() == null) {
            viewHolder.divider.setVisibility(View.GONE);
            viewHolder.tbo.setVisibility(View.GONE);
            viewHolder.two.setVisibility(View.GONE);
        } else if ("all".equals(themeItem.getThemeReadyVariable())) {
            viewHolder.divider.setVisibility(View.VISIBLE);
            viewHolder.tbo.setVisibility(View.VISIBLE);
            viewHolder.two.setVisibility(View.VISIBLE);
        } else if ("ready".equals(themeItem.getThemeReadyVariable())) {
            viewHolder.divider.setVisibility(View.VISIBLE);
            viewHolder.tbo.setVisibility(View.VISIBLE);
            viewHolder.two.setVisibility(View.GONE);
        } else if ("stock".equals(themeItem.getThemeReadyVariable())) {
            viewHolder.divider.setVisibility(View.VISIBLE);
            viewHolder.tbo.setVisibility(View.GONE);
            viewHolder.two.setVisibility(View.VISIBLE);
        } else {
            viewHolder.divider.setVisibility(View.GONE);
            viewHolder.tbo.setVisibility(View.GONE);
            viewHolder.two.setVisibility(View.GONE);
        }

        viewHolder.cardView.setOnClickListener(
                v -> Theming.launchTheme(mContext,
                        themeItem.getThemePackage(),
                        themeItem.getThemeMode()
                ));

        viewHolder.cardView.setOnLongClickListener(view -> {
            // Vibrate the device alerting the user they are about to do something dangerous!
            if (Packages.isUserApp(mContext, themeItem.getThemePackage())) {
                Vibrator v = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null) {
                    v.vibrate(30);
                }

                // About the theme
                SheetDialog sheetDialog = new SheetDialog(mContext);
                View sheetView =
                        View.inflate(mContext, R.layout.theme_long_press_sheet_dialog, null);

                TextView aboutText = sheetView.findViewById(R.id.about_text);
                TextView moreText = sheetView.findViewById(R.id.more_text);
                String boldedThemeName = "<b>" + themeItem.getThemeName() + "</b>";
                aboutText.setText(Html.fromHtml(boldedThemeName, Html.FROM_HTML_MODE_LEGACY));
                moreText.setText(String.format("%s (%s)\n%s",
                        Packages.getAppVersion(mContext, themeItem.getThemePackage()),
                        Packages.getAppVersionCode(mContext, themeItem.getThemePackage()),
                        Packages.getPackageTemplateVersion(mContext,
                                themeItem.getThemePackage())));

                ImageView icon = sheetView.findViewById(R.id.icon);
                icon.setImageDrawable(Packages.getAppIcon(mContext, themeItem.getThemePackage()));

                ImageView two = sheetView.findViewById(R.id.theme_unready_indicator);
                ImageView tbo = sheetView.findViewById(R.id.theme_ready_indicator);
                tbo.setOnClickListener(v2 -> explainTBO());
                two.setOnClickListener(v2 -> explainTWO());

                try {
                    switch (themeItem.getThemeReadyVariable()) {
                        case "all":
                            tbo.setVisibility(View.VISIBLE);
                            two.setVisibility(View.VISIBLE);
                            break;
                        case "ready":
                            tbo.setVisibility(View.VISIBLE);
                            two.setVisibility(View.GONE);
                            break;
                        case "stock":
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

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                Drawable favoriteImg = mContext.getDrawable(R.drawable.toolbar_favorite);
                Drawable notFavoriteImg = mContext.getDrawable(R.drawable.toolbar_not_favorite);
                TextView favoriteText = sheetView.findViewById(R.id.favorite_text);
                if (prefs.getString("app_shortcut_theme", "").equals(themeItem.getThemePackage())) {
                    assert favoriteImg != null;
                    favoriteImg.setBounds(0, 0, 60, 60);
                    favoriteText.setCompoundDrawables(favoriteImg, null, null, null);
                    favoriteText.setText(mContext.getString(R.string.menu_unfavorite));
                } else {
                    assert notFavoriteImg != null;
                    notFavoriteImg.setBounds(0, 0, 60, 60);
                    favoriteText.setCompoundDrawables(notFavoriteImg, null, null, null);
                    favoriteText.setText(mContext.getString(R.string.menu_favorite));
                }

                favorite.setOnClickListener(view1 -> {
                    if (prefs.contains("app_shortcut_theme")) {
                        if (!prefs.getString("app_shortcut_theme", "").equals(
                                themeItem.getThemePackage())) {
                            prefs.edit().remove("app_shortcut_theme").apply();
                            References.clearShortcut(mContext);
                            prefs.edit().putString("app_shortcut_theme",
                                    themeItem.getThemePackage()).apply();
                            References.createShortcut(
                                    mContext,
                                    themeItem.getThemePackage(),
                                    themeItem.getThemeName());
                            assert favoriteImg != null;
                            favoriteImg.setBounds(0, 0, 60, 60);
                            favoriteText.setCompoundDrawables(favoriteImg, null, null, null);
                            favoriteText.setText(mContext.getString(R.string.menu_unfavorite));
                        } else {
                            prefs.edit().remove("app_shortcut_theme").apply();
                            References.clearShortcut(mContext);
                            assert notFavoriteImg != null;
                            notFavoriteImg.setBounds(0, 0, 60, 60);
                            favoriteText.setCompoundDrawables(notFavoriteImg, null, null, null);
                            favoriteText.setText(mContext.getString(R.string.menu_favorite));
                        }
                    } else {
                        prefs.edit().putString("app_shortcut_theme",
                                themeItem.getThemePackage()).apply();
                        References.createShortcut(
                                mContext,
                                themeItem.getThemePackage(),
                                themeItem.getThemeName());
                        assert favoriteImg != null;
                        favoriteImg.setBounds(0, 0, 60, 60);
                        favoriteText.setCompoundDrawables(favoriteImg, null, null, null);
                        favoriteText.setText(mContext.getString(R.string.menu_unfavorite));
                    }
                });

                // Rate Theme
                LinearLayout rate = sheetView.findViewById(R.id.rate);
                String installer =
                        Packages.getInstallerId(mContext, themeItem.getThemePackage());
                if (installer != null && installer.equals(PLAY_STORE_PACKAGE_NAME)) {
                    rate.setVisibility(View.VISIBLE);
                } else {
                    rate.setVisibility(View.GONE);
                }
                rate.setOnClickListener(view12 -> {
                    try {
                        String playURL = "https://play.google.com/store/apps/details?id=" +
                                themeItem.getThemePackage();
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(playURL));
                        mContext.startActivity(i);
                    } catch (ActivityNotFoundException activityNotFoundException) {
                        Toast.makeText(
                                mContext,
                                mContext.getString(R.string.activity_missing_toast),
                                Toast.LENGTH_SHORT).show();
                    }
                    sheetDialog.dismiss();
                });

                // Shortcut
                LinearLayout shortcut = sheetView.findViewById(R.id.shortcut);
                shortcut.setOnClickListener(view12 -> {
                    References.createLauncherIcon(mContext,
                            themeItem.getThemePackage(), themeItem.getThemeName());
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                        Toast.makeText(
                                mContext,
                                mContext.getString(R.string.launcher_shortcut_toast),
                                Toast.LENGTH_SHORT).show();
                    }
                    sheetDialog.dismiss();
                });

                // Uninstalling
                LinearLayout uninstall = sheetView.findViewById(R.id.uninstall);
                uninstall.setOnClickListener(view2 -> {
                    if (!Systems.isSamsung(mContext) && !Systems.checkAndromeda(mContext)) {
                        toBeUninstalled = themeItem;
                        new uninstallTheme(this).execute();
                    } else {
                        Uri packageURI = Uri.parse("package:" + themeItem.getThemePackage());
                        Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
                        mContext.startActivity(uninstallIntent);
                    }
                    sheetDialog.dismiss();
                });

                sheetDialog.setContentView(sheetView);
                sheetDialog.show();
            }
            return false;
        });

        viewHolder.tbo.setOnClickListener(v -> explainTBO());
        viewHolder.two.setOnClickListener(v -> explainTWO());

        viewHolder.theme_author.setText(themeItem.getThemeAuthor());
        viewHolder.imageView.setImageDrawable(themeItem.getThemeDrawable());

        References.setRecyclerViewAnimation(mContext, viewHolder.itemView, R.anim
                .recyclerview_anim);
    }

    private void explainTBO() {
        new AlertDialog.Builder(mContext)
                .setMessage(R.string.tbo_description)
                .setPositiveButton(R.string.tbo_dialog_proceed,
                        (dialog, which) -> {
                            try {
                                String playURL =
                                        mContext.getString(R.string.tbo_theme_ready_url);
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(playURL));
                                mContext.startActivity(intent);
                            } catch (ActivityNotFoundException anfe) {
                                // Suppress warning
                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        (dialog, which) -> dialog.cancel())
                .setCancelable(true)
                .show();
    }

    private void explainTWO() {
        new AlertDialog.Builder(mContext)
                .setMessage(R.string.two_description)
                .setCancelable(true)
                .setPositiveButton(R.string.dynamic_gapps_dialog,
                        (dialog, which) -> {
                            try {
                                String playURL =
                                        mContext.getString(R.string.dynamic_gapps_link);
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(playURL));
                                mContext.startActivity(intent);
                            } catch (ActivityNotFoundException anfe) {
                                // Suppress warning
                            }
                        })
                .setNegativeButton(R.string.open_gapps_dialog,
                        (dialog, which) -> {
                            try {
                                String playURL =
                                        mContext.getString(R.string.open_gapps_link);
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(playURL));
                                mContext.startActivity(intent);
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
        return information.size();
    }

    private static class uninstallTheme extends AsyncTask<String, Integer, String> {
        private WeakReference<ThemeAdapter> ref;

        uninstallTheme(ThemeAdapter themeAdapter) {
            ref = new WeakReference<>(themeAdapter);
        }

        @Override
        protected void onPreExecute() {
            ThemeAdapter themeAdapter = ref.get();
            if (themeAdapter != null) {
                if (themeAdapter.toBeUninstalled != null) {
                    String parseMe = String.format(
                            themeAdapter.mContext.getString(R.string.adapter_uninstalling),
                            themeAdapter.toBeUninstalled.getThemeName());
                    themeAdapter.mProgressDialog = new ProgressDialog(themeAdapter.mContext);
                    themeAdapter.mProgressDialog.setMessage(parseMe);
                    themeAdapter.mProgressDialog.setIndeterminate(true);
                    themeAdapter.mProgressDialog.setCancelable(false);
                    themeAdapter.mProgressDialog.show();
                    // Clear the notification of building theme if shown
                    NotificationManager manager = (NotificationManager)
                            themeAdapter.mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                    if (manager != null) {
                        manager.cancel(References.notification_id_compiler);
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
            ThemeAdapter themeAdapter = ref.get();
            if (themeAdapter != null) {
                if (themeAdapter.toBeUninstalled != null) {
                    themeAdapter.toBeUninstalled = null;
                    Broadcasts.sendRefreshMessage(themeAdapter.mContext);
                    themeAdapter.mProgressDialog.cancel();
                }
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            ThemeAdapter themeAdapter = ref.get();
            if (themeAdapter != null) {
                if (themeAdapter.toBeUninstalled != null) {
                    // Uninstall theme
                    Packages.uninstallPackage(
                            themeAdapter.mContext,
                            themeAdapter.toBeUninstalled.getThemePackage());
                }
            }
            return null;
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView theme_name;
        TextView theme_author;
        TextView theme_apis;
        TextView theme_version;
        TextView plugin_version;
        ImageView imageView;
        View divider;
        ImageView tbo;
        ImageView two;

        ViewHolder(View view) {
            super(view);
            cardView = view.findViewById(R.id.theme_card);
            theme_name = view.findViewById(R.id.theme_name);
            theme_author = view.findViewById(R.id.theme_author);
            theme_apis = view.findViewById(R.id.api_levels);
            theme_version = view.findViewById(R.id.theme_version);
            plugin_version = view.findViewById(R.id.plugin_version);
            imageView = view.findViewById(R.id.theme_preview_image);
            divider = view.findViewById(R.id.theme_ready_divider);
            tbo = view.findViewById(R.id.theme_ready_indicator);
            two = view.findViewById(R.id.theme_unready_indicator);
        }
    }
}
