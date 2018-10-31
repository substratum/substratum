/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.adapters.fragments.themes;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;
import projekt.substratum.R;
import projekt.substratum.Substratum;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Theming;
import projekt.substratum.databinding.ThemeEntryCardBinding;
import projekt.substratum.databinding.ThemeEntryLongPressSheetDialogBinding;
import projekt.substratum.util.views.SheetDialog;

import java.util.List;

import static projekt.substratum.common.Internal.PLAY_URL_PREFIX;
import static projekt.substratum.common.References.PLAY_STORE_PACKAGE_NAME;
import static projekt.substratum.common.References.setRecyclerViewAnimations;


public class ThemeAdapter extends RecyclerView.Adapter<ThemeAdapter.ViewHolder> {
    private final List<ThemeItem> information;
    private Context context;

    public ThemeAdapter(List<ThemeItem> information) {
        super();
        this.information = information;
    }

    @NonNull
    @Override
    public ThemeAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup,
                                                      int i) {
        View view = LayoutInflater.from(
                viewGroup.getContext()).inflate(
                R.layout.theme_entry_card, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder,
                                 int pos) {
        final ThemeItem themeItem = this.information.get(pos);
        this.context = themeItem.getContext();
        final ThemeEntryCardBinding viewHolderBinding = viewHolder.getBinding();
        final SharedPreferences prefs = Substratum.getPreferences();

        viewHolderBinding.themeCard.setOnClickListener(
                v -> Theming.launchTheme(this.context, themeItem.getThemePackage()));

        viewHolderBinding.themeCard.setOnLongClickListener(view -> {
            // Vibrate the device alerting the user they are about to do something dangerous!
            Vibrator v = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) {
                v.vibrate(30L);
            }

            // About the theme
            SheetDialog sheetDialog = new SheetDialog(this.context);
            View sheetView =
                    View.inflate(this.context,
                            R.layout.theme_entry_long_press_sheet_dialog, null);
            ThemeEntryLongPressSheetDialogBinding sheetDialogBinding = DataBindingUtil.bind(sheetView);

            assert sheetDialogBinding != null;
            TextView aboutText = sheetDialogBinding.aboutText;
            aboutText.setText(themeItem.getThemeName());
            aboutText.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
            sheetDialogBinding.moreText.setText(String.format("%s (%s)\n%s",
                    Packages.getAppVersion(this.context, themeItem.getThemePackage()),
                    Packages.getAppVersionCode(this.context, themeItem.getThemePackage()),
                    Packages.getPackageTemplateVersion(this.context,
                            themeItem.getThemePackage())));

            sheetDialogBinding.icon
                    .setImageDrawable(Packages.getAppIcon(this.context, themeItem
                            .getThemePackage()));

            // Favorite
            LinearLayout favorite = sheetDialogBinding.favorite;

            Drawable favoriteImg = this.context.getDrawable(R.drawable.toolbar_favorite);
            Drawable notFavoriteImg =
                    this.context.getDrawable(R.drawable.toolbar_not_favorite);
            TextView favoriteText = sheetView.findViewById(R.id.favorite_text);
            if (prefs.getString("app_shortcut_theme", "").equals(themeItem.getThemePackage())) {
                assert favoriteImg != null;
                favoriteImg.setBounds(0, 0, 60, 60);
                favoriteText.setCompoundDrawables(favoriteImg, null, null, null);
                favoriteText.setText(this.context.getString(R.string.menu_unfavorite));
            } else {
                assert notFavoriteImg != null;
                notFavoriteImg.setBounds(0, 0, 60, 60);
                favoriteText.setCompoundDrawables(notFavoriteImg, null, null, null);
                favoriteText.setText(this.context.getString(R.string.menu_favorite));
            }

            favorite.setOnClickListener(view1 -> {
                if (prefs.contains("app_shortcut_theme")) {
                    if (!prefs.getString("app_shortcut_theme", "").equals(
                            themeItem.getThemePackage())) {
                        prefs.edit().remove("app_shortcut_theme").apply();
                        References.clearShortcut(this.context);
                        prefs.edit().putString("app_shortcut_theme",
                                themeItem.getThemePackage()).apply();
                        References.createShortcut(
                                this.context,
                                themeItem.getThemePackage(),
                                themeItem.getThemeName());
                        assert favoriteImg != null;
                        favoriteImg.setBounds(0, 0, 60, 60);
                        favoriteText.setCompoundDrawables(favoriteImg, null, null, null);
                        favoriteText.setText(this.context.getString(R.string.menu_unfavorite));
                    } else {
                        prefs.edit().remove("app_shortcut_theme").apply();
                        References.clearShortcut(this.context);
                        assert notFavoriteImg != null;
                        notFavoriteImg.setBounds(0, 0, 60, 60);
                        favoriteText.setCompoundDrawables(notFavoriteImg, null, null, null);
                        favoriteText.setText(this.context.getString(R.string.menu_favorite));
                    }
                } else {
                    prefs.edit().putString("app_shortcut_theme",
                            themeItem.getThemePackage()).apply();
                    References.createShortcut(
                            this.context,
                            themeItem.getThemePackage(),
                            themeItem.getThemeName());
                    assert favoriteImg != null;
                    favoriteImg.setBounds(0, 0, 60, 60);
                    favoriteText.setCompoundDrawables(favoriteImg, null, null, null);
                    favoriteText.setText(this.context.getString(R.string.menu_unfavorite));
                }
            });

            // Rate Theme
            LinearLayout rate = sheetDialogBinding.rate;
            String installer =
                    Packages.getInstallerId(this.context, themeItem.getThemePackage());
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
                    this.context.startActivity(i);
                } catch (ActivityNotFoundException activityNotFoundException) {
                    Toast.makeText(
                            this.context,
                            this.context.getString(R.string.activity_missing_toast),
                            Toast.LENGTH_SHORT).show();
                }
                sheetDialog.dismiss();
            });

            // Shortcut
            LinearLayout shortcut = sheetDialogBinding.shortcut;
            shortcut.setOnClickListener(view12 -> {
                References.createLauncherIcon(this.context,
                        themeItem.getThemePackage(), themeItem.getThemeName());
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    Toast.makeText(
                            this.context,
                            this.context.getString(R.string.launcher_shortcut_toast),
                            Toast.LENGTH_SHORT).show();
                }
                sheetDialog.dismiss();
            });

            // Uninstalling
            LinearLayout uninstall = sheetDialogBinding.uninstall;
            if (!Packages.isUserApp(this.context, themeItem.getThemePackage())) {
                uninstall.setVisibility(View.GONE);
            } else {
                uninstall.setOnClickListener(view2 -> {
                    Uri packageURI = Uri.parse("package:" + themeItem.getThemePackage());
                    Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageURI);
                    this.context.startActivity(uninstallIntent);
                    sheetDialog.dismiss();
                });
            }

            sheetDialog.setContentView(sheetView);
            sheetDialog.show();
            return false;
        });
        viewHolderBinding.setThemeItem(themeItem);
        viewHolderBinding.executePendingBindings();
        if (!prefs.getBoolean("lite_mode", false)) {
            setRecyclerViewAnimations(viewHolderBinding.themePreviewImage);
        }
    }

    @Override
    public int getItemCount() {
        return this.information.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ThemeEntryCardBinding binding;

        ViewHolder(View view) {
            super(view);
            binding = DataBindingUtil.bind(view);
        }

        ThemeEntryCardBinding getBinding() {
            return binding;
        }
    }
}