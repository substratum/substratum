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

package projekt.substratum.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.Lunchbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import projekt.substratum.R;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Resources;
import projekt.substratum.common.Systems;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.common.tabs.BootAnimationManager;
import projekt.substratum.common.tabs.FontManager;
import projekt.substratum.common.tabs.WallpaperManager;
import projekt.substratum.util.files.Root;
import projekt.substratum.util.tabs.SoundUtils;
import projekt.substratum.util.views.SheetDialog;

import static projekt.substratum.common.References.DATA_RESOURCE_DIR;
import static projekt.substratum.common.References.LEGACY_NEXUS_DIR;
import static projekt.substratum.common.References.PIXEL_NEXUS_DIR;
import static projekt.substratum.common.References.VENDOR_DIR;
import static projekt.substratum.common.Systems.checkAndromeda;


public class RecoveryFragment extends Fragment {

    private ProgressDialog mProgressDialog;
    private ArrayList<String> final_commands_array;
    private SharedPreferences prefs;
    private SheetDialog sheetDialog;
    private Context mContext;

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.sheetDialog != null) this.sheetDialog.dismiss();
        if (this.mProgressDialog != null) this.mProgressDialog.dismiss();
    }

    @Override
    public View onCreateView(
            final LayoutInflater inflater,
            final ViewGroup container,
            final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.mContext = this.getContext();
        final ViewGroup root = (ViewGroup) inflater.inflate(R.layout.restore_fragment, container, false);

        this.prefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);

        this.setHasOptionsMenu(true);

        final Button overlaysButton = root.findViewById(R.id.overlaysButton);
        final Button wallpaperButton = root.findViewById(R.id.wallpaperButton);
        final Button bootanimationButton = root.findViewById(R.id.bootanimationButton);
        final Button fontsButton = root.findViewById(R.id.fontsButton);
        final Button soundsButton = root.findViewById(R.id.soundsButton);

        // Overlays Dialog
        overlaysButton.setOnClickListener(v -> {
            this.sheetDialog = new SheetDialog(this.mContext);
            final View sheetView = View.inflate(
                    this.mContext,
                    R.layout.restore_overlays_sheet_dialog,
                    null);
            final LinearLayout disable_all = sheetView.findViewById(R.id.disable_all);
            final LinearLayout uninstall_all = sheetView.findViewById(R.id.uninstall_all);
            if (!Systems.checkOMS(this.mContext)) disable_all.setVisibility(View.GONE);
            disable_all.setOnClickListener(view -> {
                new RestoreFunction(this).execute(false);
                this.sheetDialog.hide();
            });
            uninstall_all.setOnClickListener(view -> {
                new RestoreFunction(this).execute(true);
                this.sheetDialog.hide();
            });
            this.sheetDialog.setContentView(sheetView);
            this.sheetDialog.show();
        });

        // Wallpaper Dialog
        wallpaperButton.setOnClickListener(v -> {
            this.sheetDialog = new SheetDialog(this.mContext);
            final View sheetView = View.inflate(this.mContext, R.layout.restore_wallpapers_sheet_dialog,
                    null);
            final LinearLayout home = sheetView.findViewById(R.id.home);
            final LinearLayout lock = sheetView.findViewById(R.id.lock);
            final LinearLayout both = sheetView.findViewById(R.id.both);
            home.setOnClickListener(view2 -> {
                try {
                    WallpaperManager.clearWallpaper(this.mContext, "home");
                    if (this.getView() != null) {
                        Lunchbar.make(this.getView(),
                                this.getString(R.string.manage_wallpaper_home_toast),
                                Lunchbar.LENGTH_LONG)
                                .show();
                    }
                } catch (final IOException e) {
                    Log.e(References.SUBSTRATUM_LOG,
                            "Failed to restore home screen wallpaper! " + e.getMessage());
                } catch (final NullPointerException e) {
                    Log.e(References.SUBSTRATUM_LOG,
                            "Cannot retrieve lock screen wallpaper! " + e.getMessage());
                }
                this.sheetDialog.hide();
            });
            lock.setOnClickListener(view2 -> {
                try {
                    WallpaperManager.clearWallpaper(this.mContext, "lock");
                    if (this.getView() != null) {
                        Lunchbar.make(this.getView(),
                                this.getString(R.string.manage_wallpaper_lock_toast),
                                Lunchbar.LENGTH_LONG)
                                .show();
                    }
                } catch (final IOException e) {
                    Log.e(References.SUBSTRATUM_LOG,
                            "Failed to restore lock screen wallpaper!" + e.getMessage());
                }
                this.sheetDialog.hide();
            });
            both.setOnClickListener(view2 -> {
                try {
                    WallpaperManager.clearWallpaper(this.mContext, "all");
                    if (this.getView() != null) {
                        Lunchbar.make(this.getView(),
                                this.getString(R.string.manage_wallpaper_all_toast),
                                Lunchbar.LENGTH_LONG)
                                .show();
                    }
                } catch (final IOException e) {
                    Log.e(References.SUBSTRATUM_LOG,
                            "Failed to restore wallpapers! " + e.getMessage());
                }
                this.sheetDialog.hide();
            });

            home.setVisibility(View.VISIBLE);
            lock.setVisibility(View.VISIBLE);
            this.sheetDialog.setContentView(sheetView);
            this.sheetDialog.show();
        });

        // Boot Animation Dialog
        bootanimationButton.setOnClickListener(v -> {
            this.sheetDialog = new SheetDialog(this.mContext);
            final View sheetView = View.inflate(this.mContext,
                    R.layout.restore_bootanimations_sheet_dialog, null);
            final LinearLayout restore = sheetView.findViewById(R.id.restore);
            restore.setOnClickListener(view2 -> {
                new BootAnimationClearer(this).execute();
                this.sheetDialog.hide();
            });
            this.sheetDialog.setContentView(sheetView);
            this.sheetDialog.show();
        });

        // Font Dialog
        fontsButton.setOnClickListener(v -> {
            this.sheetDialog = new SheetDialog(this.mContext);
            final View sheetView = View.inflate(this.mContext,
                    R.layout.restore_fonts_sheet_dialog, null);
            final LinearLayout restore = sheetView.findViewById(R.id.restore);
            restore.setOnClickListener(view2 -> {
                if (Systems.checkThemeInterfacer(this.mContext) ||
                        Settings.System.canWrite(this.mContext)) {
                    new FontsClearer(this).execute("");
                } else {
                    final Intent intent = new Intent(
                            Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.setData(Uri.parse("package:" + this.getActivity().getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    this.startActivity(intent);
                    if (this.getView() != null) {
                        Lunchbar.make(this.getView(),
                                this.getString(R.string.fonts_dialog_permissions_grant_toast),
                                Lunchbar.LENGTH_LONG).show();
                    }
                }
                this.sheetDialog.hide();
            });
            this.sheetDialog.setContentView(sheetView);
            this.sheetDialog.show();
        });

        // Sounds Dialog
        soundsButton.setOnClickListener(v -> {
            this.sheetDialog = new SheetDialog(this.mContext);
            final View sheetView = View.inflate(this.mContext,
                    R.layout.restore_sounds_sheet_dialog, null);
            final LinearLayout restore = sheetView.findViewById(R.id.restore);
            restore.setOnClickListener(view2 -> {
                if (Systems.checkThemeInterfacer(this.mContext) ||
                        Settings.System.canWrite(this.mContext)) {
                    new SoundsClearer(this).execute();
                } else {
                    final Intent intent = new Intent(
                            Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.setData(Uri.parse("package:" + this.getActivity().getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    this.startActivity(intent);
                    if (this.getView() != null) {
                        Lunchbar.make(this.getView(),
                                this.getString(R.string.sounds_dialog_permissions_grant_toast),
                                Lunchbar.LENGTH_LONG).show();
                    }
                }
                this.sheetDialog.hide();
            });
            this.sheetDialog.setContentView(sheetView);
            this.sheetDialog.show();
        });

        final View overlayCard = root.findViewById(R.id.restore_overlay_card);
        if (Systems.isSamsung(this.getContext())) {
            overlayCard.setVisibility(View.GONE);
        }

        final View bootanimationCard = root.findViewById(R.id.restore_bootanimation_card);
        if (Systems.isSamsung(this.getContext()) ||
                (checkAndromeda(this.getContext()) && !Root.checkRootAccess())) {
            bootanimationCard.setVisibility(View.GONE);
        }

        final View fontsCard = root.findViewById(R.id.restore_fonts_card);
        if (!Resources.isFontsSupported()) {
            fontsCard.setVisibility(View.GONE);
        }

        final View soundCard = root.findViewById(R.id.restore_sounds_card);
        if (Systems.isSamsung(this.getContext()) ||
                (checkAndromeda(this.getContext()) && !Root.checkRootAccess())) {
            soundCard.setVisibility(View.GONE);
        }

        if (!this.prefs.getBoolean("seen_restore_warning", false) &&
                !Systems.isSamsung(this.getContext())) {
            this.showRecoveryWarning();
        }
        return root;
    }

    private void showRecoveryWarning() {
        new AlertDialog.Builder(this.mContext)
                .setTitle(R.string.restore_info_dialog_title)
                .setMessage(R.string.restore_info_dialog_message)
                .setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
                    if (!this.prefs.getBoolean("seen_restore_warning", false))
                        this.prefs.edit().putBoolean("seen_restore_warning", true).apply();
                    dialog.dismiss();
                })
                .show();
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.restore_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();

        if (id == R.id.restore_info) {
            this.showRecoveryWarning();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private static final class FontsClearer extends AsyncTask<String, Integer, String> {

        private final WeakReference<RecoveryFragment> ref;

        private FontsClearer(final RecoveryFragment fragment) {
            super();
            this.ref = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            final RecoveryFragment fragment = this.ref.get();
            if (fragment != null) {
                final Context context = fragment.mContext;
                if (References.ENABLE_EXTRAS_DIALOG) {
                    fragment.mProgressDialog = new ProgressDialog(context, R.style.RestoreDialog);
                    fragment.mProgressDialog.setMessage(
                            context.getString(R.string.manage_dialog_performing));
                    fragment.mProgressDialog.setIndeterminate(true);
                    fragment.mProgressDialog.setCancelable(false);
                    fragment.mProgressDialog.show();
                }
            }
        }

        @Override
        protected void onPostExecute(final String result) {
            final RecoveryFragment fragment = this.ref.get();
            if (fragment != null) {
                final Context context = fragment.mContext;
                if (References.ENABLE_EXTRAS_DIALOG) {
                    fragment.mProgressDialog.dismiss();
                }
                final SharedPreferences.Editor editor = fragment.prefs.edit();
                editor.remove("fonts_applied");
                editor.apply();

                if (Systems.checkOMS(context)) {
                    final Toast toast = Toast.makeText(
                            context,
                            R.string.manage_fonts_toast,
                            Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    final Toast toast = Toast.makeText(
                            context,
                            R.string.manage_fonts_toast,
                            Toast.LENGTH_SHORT);
                    toast.show();
                    final AlertDialog.Builder alertDialogBuilder =
                            new AlertDialog.Builder(context);
                    alertDialogBuilder.setTitle(R.string.legacy_dialog_soft_reboot_title);
                    alertDialogBuilder.setMessage(R.string.legacy_dialog_soft_reboot_text);
                    alertDialogBuilder.setPositiveButton(android.R.string.ok,
                            (dialog, id) -> ElevatedCommands.reboot());
                    alertDialogBuilder.setNegativeButton(R.string.remove_dialog_later,
                            (dialog, id) -> dialog.dismiss());
                    alertDialogBuilder.setCancelable(false);
                    final AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }
            }
        }

        @Override
        protected String doInBackground(final String... sUrl) {
            final RecoveryFragment fragment = this.ref.get();
            if (fragment != null) {
                final Context context = fragment.mContext;
                FontManager.clearFonts(context);
            }
            return null;
        }
    }

    private static final class RestoreFunction extends AsyncTask<Boolean, Void, Void> {
        private boolean withUninstall;
        private final WeakReference<RecoveryFragment> ref;

        private RestoreFunction(final RecoveryFragment fragment) {
            super();
            this.ref = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            final RecoveryFragment fragment = this.ref.get();
            if (fragment != null) {
                fragment.mProgressDialog = new ProgressDialog(
                        fragment.getActivity(), R.style.RestoreDialog);
                fragment.mProgressDialog.setMessage(
                        fragment.getString(R.string.manage_dialog_performing));
                fragment.mProgressDialog.setIndeterminate(true);
                fragment.mProgressDialog.setCancelable(false);
                fragment.mProgressDialog.show();
            }
        }

        @Override
        protected void onPostExecute(final Void result) {
            super.onPostExecute(result);
            final RecoveryFragment fragment = this.ref.get();
            if (fragment != null) {
                final Context context = fragment.getContext();
                final View view = fragment.getView();

                fragment.mProgressDialog.dismiss();
                if (this.withUninstall) {
                    if (Systems.checkOMS(context)) {
                        try {
                            if (view != null) {
                                Lunchbar.make(view,
                                        context.getString(R.string
                                                .manage_system_overlay_uninstall_toast),
                                        Lunchbar.LENGTH_LONG)
                                        .show();
                            }
                            //noinspection ConstantConditions
                            if (context != null) {
                                ThemeManager.uninstallOverlay(
                                        context, fragment.final_commands_array);
                            }
                        } catch (final Exception e) {
                            // At this point the window is refreshed too many times detaching the
                            // activity
                            Log.e(References.SUBSTRATUM_LOG, "Profile window refreshed too " +
                                    "many times, restarting current activity to preserve app " +
                                    "integrity.");
                        }
                    } else {
                        if (view != null) {
                            Lunchbar.make(view,
                                    context.getString(R.string.abort_overlay_toast_success),
                                    Lunchbar.LENGTH_LONG)
                                    .show();
                        }
                        final AlertDialog.Builder alertDialogBuilder =
                                new AlertDialog.Builder(context);
                        alertDialogBuilder
                                .setTitle(context.getString(R.string
                                        .legacy_dialog_soft_reboot_title));

                        alertDialogBuilder
                                .setMessage(context.getString(R.string
                                        .legacy_dialog_soft_reboot_text));
                        alertDialogBuilder
                                .setPositiveButton(android.R.string.ok,
                                        (dialog, id) -> ElevatedCommands.reboot());
                        alertDialogBuilder.setCancelable(false);
                        final AlertDialog alertDialog = alertDialogBuilder.create();
                        alertDialog.show();
                    }
                } else {
                    if (view != null) {
                        Lunchbar.make(view,
                                context.getString(R.string.manage_system_overlay_toast),
                                Lunchbar.LENGTH_LONG)
                                .show();
                    }
                }
            }
        }

        @Override
        protected Void doInBackground(final Boolean... sUrl) {
            final RecoveryFragment fragment = this.ref.get();
            if (fragment != null) {
                final Context context = fragment.getActivity();
                this.withUninstall = sUrl[0];

                if (this.withUninstall) {
                    if (Systems.checkOMS(context)) {
                        final List<String> overlays = ThemeManager.listAllOverlays(context);

                        fragment.final_commands_array = new ArrayList<>();
                        fragment.final_commands_array.addAll(overlays.stream()
                                .filter(o -> Packages.getOverlayParent(context, o) != null)
                                .collect(Collectors.toList()));
                    } else {
                        FileOperations.mountRW();
                        FileOperations.mountRWData();
                        FileOperations.mountRWVendor();
                        FileOperations.bruteforceDelete(DATA_RESOURCE_DIR);
                        FileOperations.bruteforceDelete(LEGACY_NEXUS_DIR);
                        FileOperations.bruteforceDelete(PIXEL_NEXUS_DIR);
                        FileOperations.bruteforceDelete(VENDOR_DIR);
                        FileOperations.mountROVendor();
                        FileOperations.mountROData();
                        FileOperations.mountRO();
                    }
                } else {
                    ThemeManager.disableAllThemeOverlays(context);
                }
            }
            return null;
        }
    }

    private static final class BootAnimationClearer extends AsyncTask<Void, Void, Void> {
        private final WeakReference<RecoveryFragment> ref;

        private BootAnimationClearer(final RecoveryFragment fragment) {
            super();
            this.ref = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            final RecoveryFragment fragment = this.ref.get();
            if (fragment != null) {
                fragment.mProgressDialog = new ProgressDialog(
                        fragment.getActivity(), R.style.RestoreDialog);
                fragment.mProgressDialog.setMessage(
                        fragment.getString(R.string.manage_dialog_performing));
                fragment.mProgressDialog.setIndeterminate(true);
                fragment.mProgressDialog.setCancelable(false);
                fragment.mProgressDialog.show();
            }
        }

        @Override
        protected void onPostExecute(final Void result) {
            final RecoveryFragment fragment = this.ref.get();
            if (fragment != null) {
                fragment.mProgressDialog.dismiss();

                final SharedPreferences.Editor editor = fragment.prefs.edit();
                editor.remove("bootanimation_applied").apply();

                if (fragment.getView() != null) {
                    Lunchbar.make(fragment.getView(),
                            fragment.getString(R.string.manage_bootanimation_toast),
                            Lunchbar.LENGTH_LONG)
                            .show();
                }
            }
        }

        @Override
        protected Void doInBackground(final Void... sUrl) {
            final RecoveryFragment fragment = this.ref.get();
            if (fragment != null) {
                BootAnimationManager.clearBootAnimation(fragment.getActivity(), false);
            }
            return null;
        }
    }

    private static final class SoundsClearer extends AsyncTask<Void, Void, Void> {
        private final WeakReference<RecoveryFragment> ref;

        private SoundsClearer(final RecoveryFragment fragment) {
            super();
            this.ref = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            final RecoveryFragment fragment = this.ref.get();
            if (fragment != null) {
                fragment.mProgressDialog = new ProgressDialog(
                        fragment.getActivity(), R.style.RestoreDialog);
                fragment.mProgressDialog.setMessage(
                        fragment.getString(R.string.manage_dialog_performing));
                fragment.mProgressDialog.setIndeterminate(true);
                fragment.mProgressDialog.setCancelable(false);
                fragment.mProgressDialog.show();
            }
        }

        @Override
        protected void onPostExecute(final Void result) {
            final RecoveryFragment fragment = this.ref.get();
            if (fragment != null) {
                fragment.mProgressDialog.dismiss();

                final SharedPreferences.Editor editor = fragment.prefs.edit();
                editor.remove("sounds_applied").apply();

                if (fragment.getView() != null) {
                    Lunchbar.make(fragment.getView(),
                            fragment.getString(R.string.manage_sounds_toast),
                            Lunchbar.LENGTH_LONG)
                            .show();
                }
            }
        }

        @Override
        protected Void doInBackground(final Void... sUrl) {
            final RecoveryFragment fragment = this.ref.get();
            if (fragment != null) {
                new SoundUtils().SoundsClearer(fragment.getActivity());
            }
            return null;
        }
    }
}