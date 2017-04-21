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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.design.widget.Lunchbar;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import projekt.substratum.R;
import projekt.substratum.common.References;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.util.views.SheetDialog;

import static projekt.substratum.common.References.SUBSTRATUM_BUILDER_CACHE;


public class ThemeAdapter extends RecyclerView.Adapter<ThemeAdapter.ViewHolder> {
    private ArrayList<ThemeItem> information;
    private Context mContext;
    private ProgressDialog mProgressDialog;
    private Activity mActivity;
    private ThemeItem toBeUninstalled;

    public ThemeAdapter(ArrayList<ThemeItem> information) {
        this.information = information;
    }

    public void updateInformation(ArrayList<ThemeItem> information) {
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
        mActivity = themeItem.getActivity();

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
        if (themeItem.getThemeReadyVariable() == null) {
            viewHolder.divider.setVisibility(View.GONE);
            viewHolder.tbo.setVisibility(View.GONE);
            viewHolder.two.setVisibility(View.GONE);
        } else if (themeItem.getThemeReadyVariable().equals("all")) {
            viewHolder.divider.setVisibility(View.VISIBLE);
            viewHolder.tbo.setVisibility(View.VISIBLE);
            viewHolder.two.setVisibility(View.VISIBLE);
        } else if (themeItem.getThemeReadyVariable().equals("ready")) {
            viewHolder.divider.setVisibility(View.VISIBLE);
            viewHolder.tbo.setVisibility(View.VISIBLE);
            viewHolder.two.setVisibility(View.GONE);
        } else if (themeItem.getThemeReadyVariable().equals("stock")) {
            viewHolder.divider.setVisibility(View.VISIBLE);
            viewHolder.tbo.setVisibility(View.GONE);
            viewHolder.two.setVisibility(View.VISIBLE);
        } else {
            viewHolder.divider.setVisibility(View.GONE);
            viewHolder.tbo.setVisibility(View.GONE);
            viewHolder.two.setVisibility(View.GONE);
        }

        viewHolder.cardView.setOnClickListener(
                v -> {
                    SharedPreferences prefs =
                            mContext.getSharedPreferences("substratum_state", Context.MODE_PRIVATE);
                    if (References.isCachingEnabled(mContext)) {
                        if (!prefs.contains("is_updating")) prefs.edit()
                                .putBoolean("is_updating", false).apply();
                        if (!prefs.getBoolean("is_updating", true)) {
                            // Process fail case if user uninstalls an app and goes back an activity
                            if (References.isPackageInstalled(
                                    mContext, themeItem.getThemePackage())) {
                                File checkSubstratumVerity = new File(
                                        mContext.getCacheDir().getAbsoluteFile() +
                                                SUBSTRATUM_BUILDER_CACHE +
                                                themeItem.getThemePackage() + "/substratum.xml");
                                if (checkSubstratumVerity.exists()) {
                                    References.launchTheme(mContext,
                                            themeItem.getThemePackage(),
                                            themeItem.getThemeMode(),
                                            false);
                                } else {
                                    new References.SubstratumThemeUpdate(
                                            mContext,
                                            themeItem.getThemePackage(),
                                            themeItem.getThemeName(),
                                            themeItem.getThemeMode())
                                            .execute();
                                }
                            } else {
                                Lunchbar.make(v,
                                        mContext.getString(R.string.toast_uninstalled),
                                        Lunchbar.LENGTH_LONG)
                                        .show();
                                mActivity.recreate();
                            }
                        } else {
                            if (References.isNotificationVisible(
                                    mContext, References.notification_id_upgrade)) {
                                Lunchbar.make(v,
                                        mContext.getString(R.string.background_updating_toast),
                                        Lunchbar.LENGTH_LONG)
                                        .show();
                            } else {
                                Lunchbar.make(v,
                                        mContext.getString(R.string.background_needs_invalidating),
                                        Lunchbar.LENGTH_INDEFINITE)
                                        .setAction(mContext.getString(
                                                R.string.background_needs_invalidating_button),
                                                view -> new deleteCache().execute(""))
                                        .show();
                            }
                        }
                    } else {
                        References.launchTheme(mContext,
                                themeItem.getThemePackage(),
                                themeItem.getThemeMode(),
                                false);
                    }
                });

        viewHolder.cardView.setOnLongClickListener(view -> {
            // Vibrate the device alerting the user they are about to do something dangerous!
            Vibrator v = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(30);

            SheetDialog sheetDialog = new SheetDialog(mContext);
            View sheetView = View.inflate(mContext, R.layout.uninstall_sheet_dialog, null);
            LinearLayout uninstall = (LinearLayout) sheetView.findViewById(R.id.uninstall);
            uninstall.setOnClickListener(view2 -> {
                toBeUninstalled = themeItem;
                new uninstallTheme().execute();
                sheetDialog.hide();
            });
            sheetDialog.setContentView(sheetView);
            sheetDialog.show();
            return false;
        });

        viewHolder.tbo.setOnClickListener(
                v -> new AlertDialog.Builder(mContext)
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
                        .show()
        );

        viewHolder.two.setOnClickListener(
                v -> new AlertDialog.Builder(mContext)
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
                        .show()
        );

        viewHolder.theme_author.setText(themeItem.getThemeAuthor());
        viewHolder.imageView.setImageDrawable(themeItem.getThemeDrawable());
    }

    @Override
    public int getItemCount() {
        return information.size();
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
            cardView = (CardView) view.findViewById(R.id.theme_card);
            theme_name = (TextView) view.findViewById(R.id.theme_name);
            theme_author = (TextView) view.findViewById(R.id.theme_author);
            theme_apis = (TextView) view.findViewById(R.id.api_levels);
            theme_version = (TextView) view.findViewById(R.id.theme_version);
            plugin_version = (TextView) view.findViewById(R.id.plugin_version);
            imageView = (ImageView) view.findViewById(R.id.theme_preview_image);
            divider = view.findViewById(R.id.theme_ready_divider);
            tbo = (ImageView) view.findViewById(R.id.theme_ready_indicator);
            two = (ImageView) view.findViewById(R.id.theme_unready_indicator);
        }
    }

    private class uninstallTheme extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
            if (toBeUninstalled != null) {
                String parseMe = String.format(
                        mContext.getString(R.string.adapter_uninstalling),
                        toBeUninstalled.getThemeName());
                mProgressDialog = new ProgressDialog(mContext);
                mProgressDialog.setMessage(parseMe);
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();
                // Clear the notification of building theme if shown
                NotificationManager manager = (NotificationManager)
                        mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                manager.cancel(References.notification_id);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (toBeUninstalled != null) {
                toBeUninstalled = null;
                References.sendRefreshMessage(mContext);
                mProgressDialog.cancel();
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            if (toBeUninstalled != null) {
                // Uninstall theme
                References.uninstallPackage(mContext, toBeUninstalled.getThemePackage());
            }
            return null;
        }
    }

    private class deleteCache extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setMessage(
                    mContext.getString(R.string.substratum_cache_clear_initial_toast));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
            References.clearAllNotifications(mContext);
        }

        @Override
        protected void onPostExecute(String result) {
            // Since the cache is invalidated, better relaunch the app now
            mProgressDialog.cancel();
            mActivity.finish();
            mContext.startActivity(mActivity.getIntent());
        }

        @Override
        protected String doInBackground(String... sUrl) {
            // Delete the directory
            try {
                FileOperations.delete(mContext, mContext.getCacheDir().getAbsolutePath() +
                        SUBSTRATUM_BUILDER_CACHE);
            } catch (Exception e) {
                // Suppress warning
            }
            // Reset the flag for is_updating
            SharedPreferences prefsPrivate = mContext.getSharedPreferences("substratum_state",
                    Context.MODE_PRIVATE);
            prefsPrivate.edit().remove("is_updating").apply();
            return null;
        }
    }
}