/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.common;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.snackbar.Snackbar;
import projekt.substratum.R;
import projekt.substratum.Substratum;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.tabs.BootAnimationsManager;
import projekt.substratum.tabs.FontsManager;
import projekt.substratum.tabs.WallpapersManager;
import projekt.substratum.util.tabs.SoundUtils;
import projekt.substratum.util.views.Lunchbar;
import projekt.substratum.util.views.SheetDialog;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static projekt.substratum.common.Internal.ALL_WALLPAPER;
import static projekt.substratum.common.Internal.BOOT_ANIMATION_APPLIED;
import static projekt.substratum.common.Internal.HOME_WALLPAPER;
import static projekt.substratum.common.Internal.LOCK_WALLPAPER;
import static projekt.substratum.common.Internal.SOUNDS_APPLIED;
import static projekt.substratum.common.References.DATA_RESOURCE_DIR;
import static projekt.substratum.common.References.LEGACY_NEXUS_DIR;
import static projekt.substratum.common.References.PIXEL_NEXUS_DIR;
import static projekt.substratum.common.References.SUBSTRATUM_LOG;
import static projekt.substratum.common.References.VENDOR_DIR;
import static projekt.substratum.common.References.getPieDir;
import static projekt.substratum.common.Resources.isBootAnimationSupported;
import static projekt.substratum.common.Resources.isFontsSupported;
import static projekt.substratum.common.Resources.isSoundsSupported;

public class Restore {

    private static final SharedPreferences.Editor editor = Substratum.getPreferences().edit();

    public static void invoke(Context context) {
        Activity activity = (Activity) context;

        SheetDialog sheetDialog = new SheetDialog(activity);
        View sheetView = View.inflate(context, R.layout.restore_sheet_dialog, null);

        LinearLayout disableAll = sheetView.findViewById(R.id.disable_all);
        LinearLayout uninstallAll = sheetView.findViewById(R.id.uninstall_all);
        LinearLayout restoreBootanimation = sheetView.findViewById(R.id.restore_bootanimation);
        LinearLayout restoreSystemFont = sheetView.findViewById(R.id.restore_system_font);
        LinearLayout restoreSounds = sheetView.findViewById(R.id.restore_sounds);
        LinearLayout homeWallpaper = sheetView.findViewById(R.id.home_wallpaper);
        LinearLayout lockWallpaper = sheetView.findViewById(R.id.lock_wallpaper);
        LinearLayout bothWallpapers = sheetView.findViewById(R.id.both_wallpapers);

        View view = activity.findViewById(R.id.drawer_container);

        if (!Systems.checkOMS(context)) {
            disableAll.setVisibility(View.GONE);
            uninstallAll.setVisibility(View.GONE);
        }
        if (!isBootAnimationSupported(context)) restoreBootanimation.setVisibility(View.GONE);
        if (!isFontsSupported(context)) restoreSystemFont.setVisibility(View.GONE);
        if (!isSoundsSupported(context)) restoreSounds.setVisibility(View.GONE);

        // Overlays
        disableAll.setOnClickListener(vi -> {
            new RestoreFunction(activity).execute(false);
            sheetDialog.hide();
        });
        uninstallAll.setOnClickListener(vi -> {
            new RestoreFunction(activity).execute(true);
            sheetDialog.hide();
        });


        // Boot Animations
        restoreBootanimation.setOnClickListener(view2 -> {
            new BootAnimationClearer(activity).execute();
            sheetDialog.hide();
        });

        // Fonts
        restoreSystemFont.setOnClickListener(view2 -> {
            if (Systems.checkThemeInterfacer(context) ||
                    Systems.checkSubstratumService(context)) {
                new FontsClearer(activity).execute("");
            }
            sheetDialog.hide();
        });

        // Sounds
        restoreSounds.setOnClickListener(view2 -> {
            if (Systems.checkThemeInterfacer(context) ||
                    Systems.checkSubstratumService(context)) {
                new SoundsClearer(activity).execute();
            }
            sheetDialog.hide();
        });

        // Wallpapers
        homeWallpaper.setOnClickListener(view2 -> {
            try {
                WallpapersManager.clearWallpaper(context, HOME_WALLPAPER);
                if (view != null) {
                    Lunchbar.make(view,
                            activity.getString(R.string.manage_wallpaper_home_toast),
                            Snackbar.LENGTH_LONG)
                            .show();
                }
            } catch (IOException e) {
                Log.e(SUBSTRATUM_LOG,
                        "Failed to restore home screen wallpaper! " + e.getMessage());
            } catch (NullPointerException e) {
                Log.e(SUBSTRATUM_LOG,
                        "Cannot retrieve lock screen wallpaper! " + e.getMessage());
            }
            sheetDialog.hide();
        });
        lockWallpaper.setOnClickListener(view2 -> {
            try {
                WallpapersManager.clearWallpaper(context, LOCK_WALLPAPER);
                if (view != null) {
                    Lunchbar.make(view,
                            activity.getString(R.string.manage_wallpaper_lock_toast),
                            Snackbar.LENGTH_LONG)
                            .show();
                }
            } catch (IOException e) {
                Log.e(SUBSTRATUM_LOG,
                        "Failed to restore lock screen wallpaper!" + e.getMessage());
            }
            sheetDialog.hide();
        });
        bothWallpapers.setOnClickListener(view2 -> {
            try {
                WallpapersManager.clearWallpaper(context, ALL_WALLPAPER);
                if (view != null) {
                    Lunchbar.make(view,
                            activity.getString(R.string.manage_wallpaper_all_toast),
                            Snackbar.LENGTH_LONG)
                            .show();
                }
            } catch (IOException e) {
                Log.e(SUBSTRATUM_LOG,
                        "Failed to restore wallpapers! " + e.getMessage());
            }
            sheetDialog.hide();
        });

        // Show!
        sheetDialog.setContentView(sheetView);
        sheetDialog.show();
    }


    /**
     * Uninstall overlays
     */
    private static class RestoreFunction extends AsyncTask<Boolean, Void, Void> {
        private final WeakReference<Activity> ref;
        ArrayList<String> finalCommandsArray;
        private boolean withUninstall;
        private ProgressDialog alertDialog;

        private RestoreFunction(Activity activity) {
            super();
            ref = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            Activity activity = ref.get();
            if (activity != null) {
                alertDialog = new ProgressDialog(activity, R.style.RestoreDialog);
                alertDialog.setMessage(activity.getString(R.string.manage_dialog_performing));
                alertDialog.setIndeterminate(true);
                alertDialog.setCancelable(false);
                alertDialog.show();
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            Activity activity = ref.get();
            if (activity != null) {
                Context context = activity.getApplicationContext();
                View view = activity.findViewById(R.id.drawer_container);
                if (alertDialog != null) alertDialog.dismiss();
                if (withUninstall) {
                    if (Systems.checkOMS(context)) {
                        try {
                            if (view != null) {
                                Lunchbar.make(view,
                                        context.getString(R.string
                                                .manage_system_overlay_uninstall_toast),
                                        Snackbar.LENGTH_LONG)
                                        .show();
                            }
                            //noinspection ConstantConditions
                            if (context != null) {
                                ThemeManager.uninstallOverlay(
                                        context, finalCommandsArray);
                            }
                        } catch (Exception e) {
                            // At this point the window is refreshed too many times detaching the
                            // activity
                            Log.e(SUBSTRATUM_LOG, "Profile window refreshed too " +
                                    "many times, restarting current activity to preserve app " +
                                    "integrity.");
                        }
                    } else {
                        if (view != null) {
                            Lunchbar.make(view,
                                    context.getString(R.string.abort_overlay_toast_success),
                                    Snackbar.LENGTH_LONG)
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
                                Snackbar.LENGTH_LONG)
                                .show();
                    }
                }
            }
        }

        @Override
        protected Void doInBackground(Boolean... sUrl) {
            Activity activity = ref.get();
            if (activity != null) {
                Context context = activity.getApplicationContext();
                withUninstall = sUrl[0];
                if (context != null && withUninstall) {
                    if (Systems.checkOMS(context)) {
                        List<String> overlays = ThemeManager.listAllOverlays(context);

                        finalCommandsArray = new ArrayList<>();
                        finalCommandsArray.addAll(overlays.stream()
                                .filter(o -> Packages.getOverlayParent(context, o) != null)
                                .collect(Collectors.toList()));
                    } else {
                        FileOperations.mountSystemRW();
                        FileOperations.mountRWData();
                        FileOperations.mountRWVendor();
                        FileOperations.bruteforceDelete(DATA_RESOURCE_DIR);
                        FileOperations.bruteforceDelete(LEGACY_NEXUS_DIR);
                        FileOperations.bruteforceDelete(PIXEL_NEXUS_DIR);
                        FileOperations.bruteforceDelete(VENDOR_DIR);
                        FileOperations.bruteforceDelete(getPieDir() + "_*.apk");
                        FileOperations.mountROVendor();
                        FileOperations.mountROData();
                        FileOperations.mountSystemRO();
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
        private final WeakReference<Activity> ref;
        private ProgressDialog alertDialog;

        private BootAnimationClearer(Activity activity) {
            super();
            ref = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            Activity activity = ref.get();
            if (activity != null) {
                alertDialog = new ProgressDialog(activity, R.style.RestoreDialog);
                alertDialog.setMessage(activity.getString(R.string.manage_dialog_performing));
                alertDialog.setIndeterminate(true);
                alertDialog.setCancelable(false);
                alertDialog.show();
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            Activity activity = ref.get();
            if (activity != null) {
                View view = activity.findViewById(R.id.drawer_container);
                if (alertDialog != null) alertDialog.dismiss();

                editor.remove(BOOT_ANIMATION_APPLIED).apply();

                if (view != null) {
                    Lunchbar.make(view,
                            activity.getString(R.string.manage_bootanimation_toast),
                            Snackbar.LENGTH_LONG)
                            .show();
                }
            }
        }

        @Override
        protected Void doInBackground(Void... sUrl) {
            Activity activity = ref.get();
            if (activity != null) {
                BootAnimationsManager.clearBootAnimation(activity, false);
            }
            return null;
        }
    }


    /**
     * Clear the applied fonts
     */
    private static class FontsClearer extends AsyncTask<String, Integer, String> {
        private final WeakReference<Activity> ref;
        private ProgressDialog alertDialog;

        private FontsClearer(Activity activity) {
            super();
            ref = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            Activity activity = ref.get();
            if (activity != null) {
                if (References.ENABLE_EXTRAS_DIALOG) {
                    alertDialog = new ProgressDialog(ref.get(), R.style.RestoreDialog);
                    alertDialog.setMessage(ref.get().getString(R.string.manage_dialog_performing));
                    alertDialog.setIndeterminate(true);
                    alertDialog.setCancelable(false);
                    alertDialog.show();
                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Activity activity = ref.get();
            if (activity != null) {
                Context context = activity.getApplicationContext();
                if (References.ENABLE_EXTRAS_DIALOG && alertDialog != null) alertDialog.dismiss();

                editor.remove(BOOT_ANIMATION_APPLIED).apply();

                if (Systems.checkSubstratumService(context) ||
                        Systems.checkThemeInterfacer(context)) {
                    Toast toast = Toast.makeText(
                            context,
                            R.string.manage_fonts_toast,
                            Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            Activity activity = ref.get();
            if (activity != null) {
                Context context = activity.getApplicationContext();
                FontsManager.clearFonts(context);
            }
            return null;
        }
    }

    /**
     * Clear all applied sounds
     */
    private static class SoundsClearer extends AsyncTask<Void, Void, Void> {
        private final WeakReference<Activity> ref;
        private ProgressDialog alertDialog;

        private SoundsClearer(Activity activity) {
            super();
            ref = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            Activity activity = ref.get();
            if (activity != null) {
                alertDialog = new ProgressDialog(activity, R.style.RestoreDialog);
                alertDialog.setMessage(activity.getString(R.string.manage_dialog_performing));
                alertDialog.setIndeterminate(true);
                alertDialog.setCancelable(false);
                alertDialog.show();
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            Activity activity = ref.get();
            if (activity != null) {
                View view = activity.findViewById(R.id.drawer_container);
                if (alertDialog != null) alertDialog.dismiss();

                editor.remove(SOUNDS_APPLIED).apply();

                if (view != null) {
                    Lunchbar.make(view,
                            activity.getString(R.string.manage_sounds_toast),
                            Snackbar.LENGTH_LONG)
                            .show();
                }
            }
        }

        @Override
        protected Void doInBackground(Void... sUrl) {
            Activity activity = ref.get();
            if (activity != null) {
                SoundUtils.SoundsClearer(activity);
            }
            return null;
        }
    }
}