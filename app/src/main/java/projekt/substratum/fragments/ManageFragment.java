package projekt.substratum.fragments;

import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import projekt.substratum.R;
import projekt.substratum.config.References;
import projekt.substratum.util.ReadOverlays;
import projekt.substratum.util.Root;
import projekt.substratum.util.SoundsHandler;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class ManageFragment extends Fragment {

    private ProgressDialog mProgressDialog;
    private String final_commands;
    private ArrayList<String> final_commands_array;
    private Boolean DEBUG = References.DEBUG;
    private SharedPreferences prefs;

    private int getDeviceEncryptionStatus() {
        // 0: ENCRYPTION_STATUS_UNSUPPORTED
        // 1: ENCRYPTION_STATUS_INACTIVE
        // 2: ENCRYPTION_STATUS_ACTIVATING
        // 3: ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY
        // 4: ENCRYPTION_STATUS_ACTIVE
        // 5: ENCRYPTION_STATUS_ACTIVE_PER_USER
        int status = DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED;
        final DevicePolicyManager dpm = (DevicePolicyManager)
                getContext().getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm != null) {
            status = dpm.getStorageEncryptionStatus();
        }
        return status;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.manage_fragment, null);

        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        CardView overlaysCard = (CardView) root.findViewById(R.id.overlaysCard);
        CardView bootAnimCard = (CardView) root.findViewById(R.id.bootAnimCard);
        CardView fontsCard = (CardView) root.findViewById(R.id.fontsCard);
        CardView soundsCard = (CardView) root.findViewById(R.id.soundsCard);

        TextView bootAnimTitle = (TextView) root.findViewById(R.id.bootAnimTitle);
        TextView fontsCardTitle = (TextView) root.findViewById(R.id.fontsTitle);

        if (!References.checkOMS()) {
            bootAnimCard.setVisibility(View.GONE);
            bootAnimTitle.setVisibility(View.GONE);
            fontsCard.setVisibility(View.GONE);
            fontsCardTitle.setVisibility(View.GONE);
        }

        // Overlays Dialog

        overlaysCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builderSingle = new AlertDialog.Builder(getContext());
                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getContext(),
                        R.layout.dialog_listview);

                if (References.checkOMS()) arrayAdapter.add(getString(
                        R.string.manage_system_overlay_disable));
                arrayAdapter.add(getString(R.string.manage_system_overlay_uninstall));

                builderSingle.setNegativeButton(
                        android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                builderSingle.setAdapter(
                        arrayAdapter,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        dialog.dismiss();
                                        if (References.checkOMS()) {
                                            Toast toast = Toast.makeText(getContext(), getString(R
                                                            .string.manage_system_overlay_toast),
                                                    Toast.LENGTH_LONG);
                                            toast.show();
                                            final SharedPreferences prefs =
                                                    PreferenceManager.getDefaultSharedPreferences(
                                                            getContext());
                                            String commands = "om disable-all";
                                            if (!prefs.getBoolean("systemui_recreate", false)) {
                                                commands = commands +
                                                        " && pkill -f com.android.systemui";
                                            }
                                            if (References.isPackageInstalled(getContext(),
                                                    "masquerade.substratum")) {
                                                if (DEBUG)
                                                    Log.e("SubstratumLogger", "Initializing the " +
                                                            "Masquerade theme provider...");
                                                Intent runCommand = new Intent();
                                                runCommand.addFlags(Intent
                                                        .FLAG_INCLUDE_STOPPED_PACKAGES);
                                                runCommand.setAction("masquerade.substratum" +
                                                        ".COMMANDS");
                                                runCommand.putExtra("om-commands", commands);

                                                getContext().sendBroadcast(runCommand);
                                            } else {
                                                if (DEBUG)
                                                    Log.e("SubstratumLogger", "Masquerade was not" +
                                                            " " +
                                                            "found, falling back to Substratum " +
                                                            "theme " +
                                                            "provider...");
                                                Root.runCommand(commands);
                                            }
                                        } else {
                                            String current_directory;
                                            if (References.inNexusFilter()) {
                                                current_directory = "/system/overlay/";
                                            } else {
                                                current_directory = "/system/vendor/overlay/";
                                            }
                                            File file = new File(current_directory);
                                            if (file.exists()) {
                                                Root.runCommand("mount -o rw,remount /system");
                                                Root.runCommand("rm -r " + current_directory);
                                            }
                                            Toast toast2 = Toast.makeText(getContext(), getString(R
                                                            .string.abort_overlay_toast_success),
                                                    Toast.LENGTH_SHORT);
                                            toast2.show();
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
                                                            android.R.string.ok, new DialogInterface
                                                                    .OnClickListener() {
                                                                public void onClick
                                                                        (DialogInterface dialog,
                                                                         int id) {
                                                                    Root.runCommand("killall " +
                                                                            "zygote");
                                                                }
                                                            });
                                            alertDialogBuilder.setCancelable(false);
                                            AlertDialog alertDialog = alertDialogBuilder.create();
                                            alertDialog.show();
                                        }
                                        break;
                                    case 1:
                                        dialog.dismiss();
                                        new AbortFunction().execute("");
                                        break;
                                }
                            }
                        });
                builderSingle.show();
            }
        });

        // Boot Animation Dialog

        bootAnimCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
                alertDialogBuilder.setTitle(getString(R.string.manage_bootanimation_text));
                alertDialogBuilder.setMessage(getString(R.string.manage_dialog_text));
                alertDialogBuilder
                        .setPositiveButton(android.R.string.ok, new DialogInterface
                                .OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                new BootAnimationClearer().execute("");
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface
                                .OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        });

        // Font Dialog

        fontsCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
                alertDialogBuilder.setTitle(getString(R.string.manage_fonts_title));
                alertDialogBuilder.setMessage(getString(R.string.manage_dialog_text));
                alertDialogBuilder
                        .setPositiveButton(android.R.string.ok, new DialogInterface
                                .OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                new FontsClearer().execute("");
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface
                                .OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        });

        // Sounds Dialog

        soundsCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
                alertDialogBuilder.setTitle(getString(R.string.manage_sounds_title));
                alertDialogBuilder.setMessage(getString(R.string.manage_dialog_text));
                alertDialogBuilder
                        .setPositiveButton(android.R.string.ok, new DialogInterface
                                .OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                new SoundsClearer().execute("");
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface
                                .OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
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
                Toast toast = Toast.makeText(getContext(), getString(R
                                .string.manage_system_overlay_uninstall_toast),
                        Toast.LENGTH_SHORT);
                toast.show();
            } catch (Exception e) {
                // At this point the window is refreshed too many times causing an unattached
                // Activity
                Log.e("SubstratumLogger", "Profile window refreshed too " +
                        "many times, restarting current activity to preserve app " +
                        "integrity.");
            }
            if (References.isPackageInstalled(getContext(), "masquerade.substratum")) {
                if (DEBUG)
                    Log.e("SubstratumLogger", "Initializing the Masquerade theme provider...");
                Intent runCommand = new Intent();
                runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                runCommand.setAction("masquerade.substratum.COMMANDS");
                runCommand.putStringArrayListExtra("pm-uninstall", final_commands_array);
                getContext().sendBroadcast(runCommand);
            } else {
                if (DEBUG)
                    Log.e("SubstratumLogger", "Masquerade was not found, falling back to " +
                            "Substratum theme provider...");
                Root.runCommand(final_commands);
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            List<String> state0 = ReadOverlays.main(0);
            List<String> state1 = ReadOverlays.main(1);
            List<String> state2 = ReadOverlays.main(2);
            List<String> state3 = ReadOverlays.main(3);
            List<String> state4 = ReadOverlays.main(4);
            List<String> state5 = ReadOverlays.main(5);

            final_commands_array = new ArrayList<>(state0);
            final_commands_array.addAll(state1);
            final_commands_array.addAll(state2);
            final_commands_array.addAll(state3);
            final_commands_array.addAll(state4);
            final_commands_array.addAll(state5);
            if (final_commands_array.size() > 0)
                final_commands_array.add(" && pkill -f com.android.systemui");
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
            Toast toast = Toast.makeText(getContext(), getString(R
                            .string.manage_bootanimation_toast),
                    Toast.LENGTH_SHORT);
            toast.show();
        }

        @Override
        protected String doInBackground(String... sUrl) {
            if (getDeviceEncryptionStatus() <= 1) {
                Root.runCommand("rm -r /data/system/theme/bootanimation.zip");
            } else {
                Root.runCommand("rm -r /system/media/bootanimation-encrypted.zip");
            }
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

            // Finally, perform a window refresh
            if (References.isPackageInstalled(getContext(), "masquerade.substratum")) {
                if (DEBUG)
                    Log.e("SubstratumLogger", "Initializing the Masquerade theme provider...");
                Intent runCommand = new Intent();
                runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                runCommand.setAction("masquerade.substratum.COMMANDS");
                runCommand.putExtra("om-commands", final_commands);
                getContext().sendBroadcast(runCommand);
            } else {
                if (DEBUG)
                    Log.e("SubstratumLogger", "Masquerade was not found, falling back to " +
                            "Substratum theme provider...");
                Root.runCommand(final_commands);
            }
            if (References.isPackageInstalled(getContext(), "masquerade.substratum")) {
                if (DEBUG)
                    Log.e("SubstratumLogger", "Initializing the Masquerade theme provider...");
                Intent runCommand = new Intent();
                runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                runCommand.setAction("masquerade.substratum.COMMANDS");
                runCommand.putExtra("om-commands", "setprop sys.refresh_theme 1");
                getContext().sendBroadcast(runCommand);
            } else {
                if (DEBUG)
                    Log.e("SubstratumLogger", "Masquerade was not found, falling back to " +
                            "Substratum theme provider...");
                Root.runCommand("setprop sys.refresh_theme 1");
            }

            if (!prefs.getBoolean("systemui_recreate", false)) {
                Root.runCommand("pkill -f com.android.systemui");
            }

            Toast toast = Toast.makeText(getContext(), getString(R
                            .string.manage_fonts_toast),
                    Toast.LENGTH_SHORT);
            toast.show();
        }

        @Override
        protected String doInBackground(String... sUrl) {
            Root.runCommand("rm -r /data/system/theme/fonts/");
            final_commands = "om refresh";
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
            Toast toast = Toast.makeText(getContext(), getString(R
                            .string.manage_sounds_toast),
                    Toast.LENGTH_SHORT);
            toast.show();
            Root.runCommand("pkill -f com.android.systemui");
        }

        @Override
        protected String doInBackground(String... sUrl) {
            Root.runCommand("rm -r /data/system/theme/audio/");
            new SoundsHandler().SoundsClearer(getContext());
            return null;
        }
    }
}