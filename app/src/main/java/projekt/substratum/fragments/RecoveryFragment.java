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
import android.support.annotation.NonNull;
import android.support.design.widget.Lunchbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import butterknife.BindView;
import butterknife.ButterKnife;
import projekt.substratum.R;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Resources;
import projekt.substratum.common.Systems;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.tabs.BootAnimationsManager;
import projekt.substratum.tabs.FontsManager;
import projekt.substratum.tabs.WallpapersManager;
import projekt.substratum.util.files.Root;
import projekt.substratum.util.tabs.SoundUtils;
import projekt.substratum.util.views.SheetDialog;

import static projekt.substratum.common.Internal.ALL_WALLPAPER;
import static projekt.substratum.common.Internal.BOOT_ANIMATION_APPLIED;
import static projekt.substratum.common.Internal.FONTS_APPLIED;
import static projekt.substratum.common.Internal.HOME_WALLPAPER;
import static projekt.substratum.common.Internal.LOCK_WALLPAPER;
import static projekt.substratum.common.Internal.SOUNDS_APPLIED;
import static projekt.substratum.common.References.DATA_RESOURCE_DIR;
import static projekt.substratum.common.References.LEGACY_NEXUS_DIR;
import static projekt.substratum.common.References.PIXEL_NEXUS_DIR;
import static projekt.substratum.common.References.VENDOR_DIR;
import static projekt.substratum.common.Systems.checkAndromeda;


public class RecoveryFragment extends Fragment {

    @BindView(R.id.overlaysButton)
    CardView overlaysButton;
    @BindView(R.id.wallpaperButton)
    CardView wallpaperButton;
    @BindView(R.id.bootanimationButton)
    CardView bootanimationButton;
    @BindView(R.id.fontsButton)
    CardView fontsButton;
    @BindView(R.id.soundsButton)
    CardView soundsButton;
    @BindView(R.id.restore_overlay_card)
    View overlayCard;
    @BindView(R.id.restore_bootanimation_card)
    View bootanimationCard;
    @BindView(R.id.restore_fonts_card)
    View fontsCard;
    @BindView(R.id.restore_sounds_card)
    View soundCard;
    private ProgressDialog mProgressDialog;
    private ArrayList<String> final_commands_array;
    private SharedPreferences prefs;
    private SheetDialog sheetDialog;
    private Context mContext;

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sheetDialog != null) sheetDialog.dismiss();
        if (mProgressDialog != null) mProgressDialog.dismiss();
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
        View view = inflater.inflate(R.layout.restore_fragment, container, false);
        ButterKnife.bind(this, view);

        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        setHasOptionsMenu(true);

        // Overlays Dialog
        overlaysButton.setOnClickListener(v -> {
            sheetDialog = new SheetDialog(mContext);
            View sheetView = View.inflate(mContext, R.layout.restore_overlays_sheet_dialog, null);
            LinearLayout disable_all = sheetView.findViewById(R.id.disable_all);
            LinearLayout uninstall_all = sheetView.findViewById(R.id.uninstall_all);
            if (!Systems.checkOMS(mContext)) disable_all.setVisibility(View.GONE);
            disable_all.setOnClickListener(vi -> {
                new RestoreFunction(this).execute(false);
                sheetDialog.hide();
            });
            uninstall_all.setOnClickListener(vi -> {
                new RestoreFunction(this).execute(true);
                sheetDialog.hide();
            });
            sheetDialog.setContentView(sheetView);
            sheetDialog.show();
        });

        // Wallpaper Dialog
        wallpaperButton.setOnClickListener(v -> {
            sheetDialog = new SheetDialog(mContext);
            View sheetView = View.inflate(mContext, R.layout
                            .restore_wallpapers_sheet_dialog,
                    null);
            LinearLayout home = sheetView.findViewById(R.id.home);
            LinearLayout lock = sheetView.findViewById(R.id.lock);
            LinearLayout both = sheetView.findViewById(R.id.both);
            home.setOnClickListener(view2 -> {
                try {
                    WallpapersManager.clearWallpaper(mContext, HOME_WALLPAPER);
                    if (getView() != null) {
                        Lunchbar.make(getView(),
                                getString(R.string.manage_wallpaper_home_toast),
                                Lunchbar.LENGTH_LONG)
                                .show();
                    }
                } catch (IOException e) {
                    Log.e(References.SUBSTRATUM_LOG,
                            "Failed to restore home screen wallpaper! " + e.getMessage());
                } catch (NullPointerException e) {
                    Log.e(References.SUBSTRATUM_LOG,
                            "Cannot retrieve lock screen wallpaper! " + e.getMessage());
                }
                sheetDialog.hide();
            });
            lock.setOnClickListener(view2 -> {
                try {
                    WallpapersManager.clearWallpaper(mContext, LOCK_WALLPAPER);
                    if (getView() != null) {
                        Lunchbar.make(getView(),
                                getString(R.string.manage_wallpaper_lock_toast),
                                Lunchbar.LENGTH_LONG)
                                .show();
                    }
                } catch (IOException e) {
                    Log.e(References.SUBSTRATUM_LOG,
                            "Failed to restore lock screen wallpaper!" + e.getMessage());
                }
                sheetDialog.hide();
            });
            both.setOnClickListener(view2 -> {
                try {
                    WallpapersManager.clearWallpaper(mContext, ALL_WALLPAPER);
                    if (getView() != null) {
                        Lunchbar.make(getView(),
                                getString(R.string.manage_wallpaper_all_toast),
                                Lunchbar.LENGTH_LONG)
                                .show();
                    }
                } catch (IOException e) {
                    Log.e(References.SUBSTRATUM_LOG,
                            "Failed to restore wallpapers! " + e.getMessage());
                }
                sheetDialog.hide();
            });

            home.setVisibility(View.VISIBLE);
            lock.setVisibility(View.VISIBLE);
            sheetDialog.setContentView(sheetView);
            sheetDialog.show();
        });

        // Boot Animation Dialog
        bootanimationButton.setOnClickListener(v -> {
            sheetDialog = new SheetDialog(mContext);
            View sheetView = View.inflate(mContext,
                    R.layout.restore_bootanimations_sheet_dialog, null);
            LinearLayout restore = sheetView.findViewById(R.id.restore);
            restore.setOnClickListener(view2 -> {
                new BootAnimationClearer(this).execute();
                sheetDialog.hide();
            });
            sheetDialog.setContentView(sheetView);
            sheetDialog.show();
        });

        // Font Dialog
        fontsButton.setOnClickListener(v -> {
            sheetDialog = new SheetDialog(mContext);
            View sheetView = View.inflate(mContext, R.layout.restore_fonts_sheet_dialog, null);
            LinearLayout restore = sheetView.findViewById(R.id.restore);
            restore.setOnClickListener(view2 -> {
                if (Systems.checkThemeInterfacer(mContext) ||
                        Systems.checkSubstratumService(mContext) ||
                        Settings.System.canWrite(mContext)) {
                    new FontsClearer(this).execute("");
                } else {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    assert getActivity() != null;
                    intent.setData(Uri.parse("package:" + getActivity().getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    if (getView() != null) {
                        Lunchbar.make(getView(),
                                getString(R.string.fonts_dialog_permissions_grant_toast),
                                Lunchbar.LENGTH_LONG).show();
                    }
                }
                sheetDialog.hide();
            });
            sheetDialog.setContentView(sheetView);
            sheetDialog.show();
        });

        // Sounds Dialog
        soundsButton.setOnClickListener(v -> {
            sheetDialog = new SheetDialog(mContext);
            View sheetView = View.inflate(mContext, R.layout.restore_sounds_sheet_dialog, null);
            LinearLayout restore = sheetView.findViewById(R.id.restore);
            restore.setOnClickListener(view2 -> {
                if (Systems.checkThemeInterfacer(mContext) ||
                        Settings.System.canWrite(mContext)) {
                    new SoundsClearer(this).execute();
                } else {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    assert getActivity() != null;
                    intent.setData(Uri.parse("package:" + getActivity().getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    if (getView() != null) {
                        Lunchbar.make(getView(),
                                getString(R.string.sounds_dialog_permissions_grant_toast),
                                Lunchbar.LENGTH_LONG).show();
                    }
                }
                sheetDialog.hide();
            });
            sheetDialog.setContentView(sheetView);
            sheetDialog.show();
        });

        if (Systems.isSamsungDevice(mContext)) {
            overlayCard.setVisibility(View.GONE);
        }

        if (Systems.isSamsungDevice(mContext) ||
                (checkAndromeda(mContext) &&
                        !Root.checkRootAccess())) {
            bootanimationCard.setVisibility(View.GONE);
        }

        if (!Resources.isFontsSupported(mContext)) {
            fontsCard.setVisibility(View.GONE);
        }

        if (Systems.isSamsungDevice(mContext) ||
                (checkAndromeda(mContext) &&
                        !Root.checkRootAccess())) {
            soundCard.setVisibility(View.GONE);
        }

        if (!prefs.getBoolean("seen_restore_warning", false) &&
                !Systems.isSamsungDevice(mContext)) {
            showRecoveryWarning();
        }
        return view;
    }

    /**
     * Show the dialog to warn the user that restoring would take some time
     */
    private void showRecoveryWarning() {
        new AlertDialog.Builder(mContext)
                .setTitle(R.string.restore_info_dialog_title)
                .setMessage(R.string.restore_info_dialog_message)
                .setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
                    if (!prefs.getBoolean("seen_restore_warning", false))
                        prefs.edit().putBoolean("seen_restore_warning", true).apply();
                    dialog.dismiss();
                })
                .show();
    }

    /**
     * Creating the options menu (3dot overflow menu)
     *
     * @param menu Menu object
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.restore_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * Assign actions to every option when they are selected
     *
     * @param item Object of menu item
     * @return True, if something has changed.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.restore_info) {
            showRecoveryWarning();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Clear the applied fonts
     */
    private static class FontsClearer extends AsyncTask<String, Integer, String> {

        private WeakReference<RecoveryFragment> ref;

        private FontsClearer(RecoveryFragment fragment) {
            super();
            ref = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            RecoveryFragment fragment = ref.get();
            if (fragment != null) {
                Context context = fragment.mContext;
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
        protected void onPostExecute(String result) {
            RecoveryFragment fragment = ref.get();
            if (fragment != null) {
                Context context = fragment.mContext;
                if (References.ENABLE_EXTRAS_DIALOG) {
                    fragment.mProgressDialog.dismiss();
                }
                SharedPreferences.Editor editor = fragment.prefs.edit();
                editor.remove(FONTS_APPLIED);
                editor.apply();

                if (Systems.checkOMS(context)) {
                    Toast toast = Toast.makeText(
                            context,
                            R.string.manage_fonts_toast,
                            Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    Toast toast = Toast.makeText(
                            context,
                            R.string.manage_fonts_toast,
                            Toast.LENGTH_SHORT);
                    toast.show();
                    AlertDialog.Builder alertDialogBuilder =
                            new AlertDialog.Builder(context);
                    alertDialogBuilder.setTitle(R.string.legacy_dialog_soft_reboot_title);
                    alertDialogBuilder.setMessage(R.string.legacy_dialog_soft_reboot_text);
                    alertDialogBuilder.setPositiveButton(android.R.string.ok,
                            (dialog, id) -> ElevatedCommands.reboot());
                    alertDialogBuilder.setNegativeButton(R.string.remove_dialog_later,
                            (dialog, id) -> dialog.dismiss());
                    alertDialogBuilder.setCancelable(false);
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            RecoveryFragment fragment = ref.get();
            if (fragment != null) {
                Context context = fragment.mContext;
                FontsManager.clearFonts(context);
            }
            return null;
        }
    }

    /**
     * Uninstall overlays
     */
    private static class RestoreFunction extends AsyncTask<Boolean, Void, Void> {
        private WeakReference<RecoveryFragment> ref;
        private boolean withUninstall;

        private RestoreFunction(RecoveryFragment fragment) {
            super();
            ref = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            RecoveryFragment fragment = ref.get();
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
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            RecoveryFragment fragment = ref.get();
            if (fragment != null) {
                Context context = fragment.mContext;
                View view = fragment.getView();

                fragment.mProgressDialog.dismiss();
                if (withUninstall) {
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
                        } catch (Exception e) {
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
                        AlertDialog.Builder alertDialogBuilder =
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
                        AlertDialog alertDialog = alertDialogBuilder.create();
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
        protected Void doInBackground(Boolean... sUrl) {
            RecoveryFragment fragment = ref.get();
            if (fragment != null) {
                Context context = fragment.getActivity();
                withUninstall = sUrl[0];
                if (context != null && withUninstall) {
                    if (Systems.checkOMS(context)) {
                        List<String> overlays = ThemeManager.listAllOverlays(context);

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

    /**
     * Clear applied boot animation
     */
    private static class BootAnimationClearer extends AsyncTask<Void, Void, Void> {
        private WeakReference<RecoveryFragment> ref;

        private BootAnimationClearer(RecoveryFragment fragment) {
            super();
            ref = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            RecoveryFragment fragment = ref.get();
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
        protected void onPostExecute(Void result) {
            RecoveryFragment fragment = ref.get();
            if (fragment != null) {
                fragment.mProgressDialog.dismiss();

                SharedPreferences.Editor editor = fragment.prefs.edit();
                editor.remove(BOOT_ANIMATION_APPLIED).apply();

                if (fragment.getView() != null) {
                    Lunchbar.make(fragment.getView(),
                            fragment.getString(R.string.manage_bootanimation_toast),
                            Lunchbar.LENGTH_LONG)
                            .show();
                }
            }
        }

        @Override
        protected Void doInBackground(Void... sUrl) {
            RecoveryFragment fragment = ref.get();
            if (fragment != null) {
                BootAnimationsManager.clearBootAnimation(fragment.getActivity(), false);
            }
            return null;
        }
    }

    /**
     * Clear all applied sounds
     */
    private static class SoundsClearer extends AsyncTask<Void, Void, Void> {
        private WeakReference<RecoveryFragment> ref;

        private SoundsClearer(RecoveryFragment fragment) {
            super();
            ref = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            RecoveryFragment fragment = ref.get();
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
        protected void onPostExecute(Void result) {
            RecoveryFragment fragment = ref.get();
            if (fragment != null) {
                fragment.mProgressDialog.dismiss();

                SharedPreferences.Editor editor = fragment.prefs.edit();
                editor.remove(SOUNDS_APPLIED).apply();

                if (fragment.getView() != null) {
                    Lunchbar.make(fragment.getView(),
                            fragment.getString(R.string.manage_sounds_toast),
                            Lunchbar.LENGTH_LONG)
                            .show();
                }
            }
        }

        @Override
        protected Void doInBackground(Void... sUrl) {
            RecoveryFragment fragment = ref.get();
            if (fragment != null) {
                SoundUtils.SoundsClearer(fragment.getActivity());
            }
            return null;
        }
    }
}