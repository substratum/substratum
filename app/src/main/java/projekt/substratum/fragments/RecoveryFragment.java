package projekt.substratum.fragments;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import projekt.substratum.R;
import projekt.substratum.config.BootAnimationManager;
import projekt.substratum.config.ElevatedCommands;
import projekt.substratum.config.FileOperations;
import projekt.substratum.config.FontManager;
import projekt.substratum.config.References;
import projekt.substratum.config.ThemeManager;
import projekt.substratum.config.WallpaperManager;
import projekt.substratum.util.SheetDialog;
import projekt.substratum.util.SoundUtils;

import static projekt.substratum.config.References.REFRESH_WINDOW_DELAY;

public class RecoveryFragment extends Fragment {

    private ProgressDialog mProgressDialog;
    private ArrayList<String> final_commands_array;
    private Boolean DEBUG = References.DEBUG;
    private SharedPreferences prefs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.restore_fragment, container, false);

        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        setHasOptionsMenu(true);

        Button overlaysButton = (Button) root.findViewById(R.id.overlaysButton);
        Button wallpaperButton = (Button) root.findViewById(R.id.wallpaperButton);
        Button bootanimationButton = (Button) root.findViewById(R.id.bootanimationButton);
        Button fontsButton = (Button) root.findViewById(R.id.fontsButton);
        Button soundsButton = (Button) root.findViewById(R.id.soundsButton);

        // Overlays Dialog
        overlaysButton.setOnClickListener(v -> {
            SheetDialog sheetDialog = new SheetDialog(getContext());
            View sheetView = View.inflate(getContext(), R.layout.manage_overlays_sheet_dialog,
                    null);
            LinearLayout disable_all = (LinearLayout) sheetView.findViewById(R.id.disable_all);
            LinearLayout uninstall_all = (LinearLayout) sheetView.findViewById(R.id.uninstall_all);
            if (!References.checkOMS(getContext())) disable_all.setVisibility(View.GONE);
            disable_all.setOnClickListener(view -> {
                if (References.checkOMS(getContext())) {
                    if (getView() != null) {
                        Snackbar.make(getView(),
                                getString(R.string.
                                        manage_system_overlay_toast),
                                Snackbar.LENGTH_LONG)
                                .show();
                    }
                    List<String> list = ThemeManager.listOverlays(5);
                    ThemeManager.disableAll(getContext());
                    if (References.needsRecreate(getContext(), new ArrayList<>(list))) {
                        Handler handler = new Handler();
                        handler.postDelayed(() -> {
                            try {
                                getActivity().recreate();
                            } catch (Exception e) {
                                // Consume window refresh
                            }
                        }, REFRESH_WINDOW_DELAY);
                    }
                } else {
                    File vendor_location = new File("/system/vendor/overlay/");
                    File overlay_location = new File("/system/overlay/");
                    FileOperations.mountRW();
                    if (vendor_location.exists()) {
                        FileOperations.mountRWVendor();
                        FileOperations.delete(getContext(), vendor_location
                                .getAbsolutePath());
                        FileOperations.mountROVendor();
                    }
                    if (overlay_location.exists()) {
                        FileOperations.delete(getContext(), overlay_location
                                .getAbsolutePath());
                    }
                    FileOperations.mountRO();
                    if (getView() != null) {
                        Snackbar.make(getView(),
                                getString(R.string.
                                        abort_overlay_toast_success),
                                Snackbar.LENGTH_LONG)
                                .show();
                    }
                    AlertDialog.Builder alertDialogBuilder =
                            new AlertDialog.Builder(getContext());
                    alertDialogBuilder
                            .setTitle(getString(
                                    R.string.legacy_dialog_soft_reboot_title));
                    alertDialogBuilder
                            .setMessage(getString(
                                    R.string.legacy_dialog_soft_reboot_text));
                    alertDialogBuilder
                            .setPositiveButton(
                                    android.R.string.ok,
                                    (dialog1, id) ->
                                            ElevatedCommands.softReboot());
                    alertDialogBuilder.setCancelable(false);
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }
                sheetDialog.hide();
            });
            uninstall_all.setOnClickListener(view -> {
                new AbortFunction().execute("");
                sheetDialog.hide();
            });
            sheetDialog.setContentView(sheetView);
            sheetDialog.show();
        });

        // Wallpaper Dialog
        wallpaperButton.setOnClickListener(v -> {
            SheetDialog sheetDialog = new SheetDialog(getContext());
            View sheetView = View.inflate(getContext(), R.layout.manage_wallpapers_sheet_dialog,
                    null);
            LinearLayout home = (LinearLayout) sheetView.findViewById(R.id.home);
            LinearLayout lock = (LinearLayout) sheetView.findViewById(R.id.lock);
            LinearLayout both = (LinearLayout) sheetView.findViewById(R.id.both);
            home.setOnClickListener(view2 -> {
                try {
                    WallpaperManager.clearWallpaper(getContext(), "home");
                    if (getView() != null) {
                        Snackbar.make(getView(),
                                getString(R.string.manage_wallpaper_home_toast),
                                Snackbar.LENGTH_LONG)
                                .show();
                    }
                } catch (IOException e) {
                    Log.e(References.SUBSTRATUM_LOG, "Failed to restore home " +
                            "screen wallpaper!");
                } catch (NullPointerException e) {
                    Log.e(References.SUBSTRATUM_LOG, "Cannot retrieve lock screen " +
                            "wallpaper!");
                }
                sheetDialog.hide();
            });
            lock.setOnClickListener(view2 -> {
                try {
                    WallpaperManager.clearWallpaper(getContext(), "lock");
                    if (getView() != null) {
                        Snackbar.make(getView(),
                                getString(R.string.manage_wallpaper_lock_toast),
                                Snackbar.LENGTH_LONG)
                                .show();
                    }
                } catch (IOException e) {
                    Log.e(References.SUBSTRATUM_LOG, "Failed to restore lock " +
                            "screen wallpaper!");
                }
                sheetDialog.hide();
            });
            both.setOnClickListener(view2 -> {
                try {
                    WallpaperManager.clearWallpaper(getContext(), "all");
                    if (getView() != null) {
                        Snackbar.make(getView(),
                                getString(R.string.
                                        manage_wallpaper_all_toast),
                                Snackbar.LENGTH_LONG)
                                .show();
                    }
                } catch (IOException e) {
                    Log.e(References.SUBSTRATUM_LOG, "Failed to restore wallpapers!");
                }
                sheetDialog.hide();
            });

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                home.setVisibility(View.GONE);
                lock.setVisibility(View.GONE);
            } else {
                home.setVisibility(View.VISIBLE);
                lock.setVisibility(View.VISIBLE);
            }
            sheetDialog.setContentView(sheetView);
            sheetDialog.show();
        });

        // Boot Animation Dialog
        bootanimationButton.setOnClickListener(v -> {
            SheetDialog sheetDialog = new SheetDialog(getContext());
            View sheetView = View.inflate(getContext(),
                    R.layout.manage_bootanimations_sheet_dialog, null);
            LinearLayout restore = (LinearLayout) sheetView.findViewById(R.id.restore);
            restore.setOnClickListener(view2 -> {
                new BootAnimationClearer().execute("");
                sheetDialog.hide();
            });
            sheetDialog.setContentView(sheetView);
            sheetDialog.show();
        });

        // Font Dialog
        fontsButton.setOnClickListener(v -> {
            SheetDialog sheetDialog = new SheetDialog(getContext());
            View sheetView = View.inflate(getContext(),
                    R.layout.manage_fonts_sheet_dialog, null);
            LinearLayout restore = (LinearLayout) sheetView.findViewById(R.id.restore);
            restore.setOnClickListener(view2 -> {
                if (References.checkThemeInterfacer(getContext()) ||
                        Settings.System.canWrite(getContext())) {
                    new FontsClearer().execute("");
                } else {
                    Intent intent = new Intent(
                            Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.setData(Uri.parse(
                            "package:" + getActivity().getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    if (getView() != null) {
                        Snackbar.make(getView(),
                                getString(R.string.fonts_dialog_permissions_grant_toast),
                                Snackbar.LENGTH_LONG).show();
                    }
                }
                sheetDialog.hide();
            });
            sheetDialog.setContentView(sheetView);
            sheetDialog.show();
        });

        // Sounds Dialog
        soundsButton.setOnClickListener(v -> {
            SheetDialog sheetDialog = new SheetDialog(getContext());
            View sheetView = View.inflate(getContext(),
                    R.layout.manage_sounds_sheet_dialog, null);
            LinearLayout restore = (LinearLayout) sheetView.findViewById(R.id.restore);
            restore.setOnClickListener(view2 -> {
                if (References.checkThemeInterfacer(getContext()) ||
                        Settings.System.canWrite(getContext())) {
                    new SoundsClearer().execute("");
                } else {
                    Intent intent = new Intent(
                            Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.setData(Uri.parse(
                            "package:" + getActivity().getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    if (getView() != null) {
                        Snackbar.make(getView(),
                                getString(R.string.sounds_dialog_permissions_grant_toast),
                                Snackbar.LENGTH_LONG).show();
                    }
                }
                sheetDialog.hide();
            });
            sheetDialog.setContentView(sheetView);
            sheetDialog.show();
        });

        return root;
    }

    private class AbortFunction extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(getActivity(), R.style.RestoreDialog);
            mProgressDialog.setMessage(getString(R.string.manage_dialog_performing));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        @Override
        protected void onPostExecute(String result) {
            mProgressDialog.dismiss();
            super.onPostExecute(result);
            try {
                if (getView() != null) {
                    Snackbar.make(getView(),
                            getString(R.string.
                                    manage_system_overlay_uninstall_toast),
                            Snackbar.LENGTH_LONG)
                            .show();
                }
            } catch (Exception e) {
                // At this point the window is refreshed too many times causing an unattached
                // Activity
                Log.e(References.SUBSTRATUM_LOG, "Profile window refreshed too " +
                        "many times, restarting current activity to preserve app " +
                        "integrity.");
            }
            ThemeManager.uninstallOverlay(getContext(), final_commands_array);
        }

        @Override
        protected String doInBackground(String... sUrl) {
            List<String> unapproved = ThemeManager.listOverlays(3);
            List<String> disabled = ThemeManager.listOverlays(4);
            List<String> enabled = ThemeManager.listOverlays(5);

            final_commands_array = new ArrayList<>(unapproved);
            final_commands_array.addAll(disabled);
            final_commands_array.addAll(enabled);
            return null;
        }
    }

    private class BootAnimationClearer extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(getActivity(), R.style.RestoreDialog);
            mProgressDialog.setMessage(getString(R.string.manage_dialog_performing));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        @Override
        protected void onPostExecute(String result) {
            mProgressDialog.dismiss();

            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("bootanimation_applied").apply();

            if (getView() != null) {
                Snackbar.make(getView(),
                        getString(R.string.
                                manage_bootanimation_toast),
                        Snackbar.LENGTH_LONG)
                        .show();
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            BootAnimationManager.clearBootAnimation(getContext());
            return null;
        }
    }

    private class FontsClearer extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(getActivity(), R.style.RestoreDialog);
            mProgressDialog.setMessage(getString(R.string.manage_dialog_performing));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        @Override
        protected void onPostExecute(String result) {
            mProgressDialog.dismiss();

            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("fonts_applied").apply();

            if (References.checkOMS(getContext())) {
                if (getView() != null) {
                    Snackbar.make(getView(),
                            getString(R.string.
                                    manage_fonts_toast),
                            Snackbar.LENGTH_LONG)
                            .show();
                }
            } else {
                if (getView() != null) {
                    Snackbar.make(getView(),
                            getString(R.string.
                                    manage_fonts_toast),
                            Snackbar.LENGTH_LONG)
                            .show();
                }
                final AlertDialog.Builder alertDialogBuilder =
                        new AlertDialog.Builder(getContext());
                alertDialogBuilder.setTitle(getString(R.string
                        .legacy_dialog_soft_reboot_title));
                alertDialogBuilder.setMessage(getString(R.string
                        .legacy_dialog_soft_reboot_text));
                alertDialogBuilder.setPositiveButton(android.R.string.ok, (dialog, id) ->
                        ElevatedCommands.reboot());
                alertDialogBuilder.setNegativeButton(R.string.remove_dialog_later,
                        (dialog, id) -> dialog.dismiss());
                alertDialogBuilder.setCancelable(false);
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            FontManager.clearFonts(getContext());
            return null;
        }
    }

    private class SoundsClearer extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(getActivity(), R.style.RestoreDialog);
            mProgressDialog.setMessage(getString(R.string.manage_dialog_performing));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        @Override
        protected void onPostExecute(String result) {
            mProgressDialog.dismiss();

            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("sounds_applied").apply();

            if (getView() != null) {
                Snackbar.make(getView(),
                        getString(R.string.
                                manage_sounds_toast),
                        Snackbar.LENGTH_LONG)
                        .show();
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            new SoundUtils().SoundsClearer(getContext());
            return null;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.restore_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.restore_info) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setComponent(new ComponentName("projekt.substratum","projekt.substratum.RestoreInfo"));
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}