package projekt.substratum.fragments;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import projekt.substratum.R;
import projekt.substratum.util.ReadOverlaysFile;
import projekt.substratum.util.Root;
import projekt.substratum.util.SoundsHandler;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class ManageFragment extends Fragment {

    private ProgressDialog mProgressDialog;
    private String final_commands;
    private ArrayList<String> final_commands_array;

    private boolean isPackageInstalled(String package_name) {
        PackageManager pm = getContext().getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo packageInfo : packages) {
            if (packageInfo.packageName.equals(package_name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.manage_fragment, null);

        CardView overlaysCard = (CardView) root.findViewById(R.id.overlaysCard);
        CardView bootAnimCard = (CardView) root.findViewById(R.id.bootAnimCard);
        CardView fontsCard = (CardView) root.findViewById(R.id.fontsCard);
        CardView soundsCard = (CardView) root.findViewById(R.id.soundsCard);

        // Overlays Dialog

        overlaysCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builderSingle = new AlertDialog.Builder(getContext());
                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getContext(),
                        R.layout.dialog_listview);

                arrayAdapter.add(getString(R.string.manage_system_overlay_disable));
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
                                        Toast toast = Toast.makeText(getContext(), getString(R
                                                        .string.manage_system_overlay_toast),
                                                Toast.LENGTH_LONG);
                                        toast.show();
                                        if (isPackageInstalled("masquerade.substratum")) {
                                            Intent runCommand = new Intent();
                                            runCommand.addFlags(Intent
                                                    .FLAG_INCLUDE_STOPPED_PACKAGES);
                                            runCommand.setAction("projekt.substratum.helper" +
                                                    ".COMMANDS");
                                            runCommand.putExtra("om-commands", "om disable-all");
                                            getContext().sendBroadcast(runCommand);
                                        } else {
                                            Root.runCommand("om disable-all");
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
            if (isPackageInstalled("masquerade.substratum")) {
                Intent runCommand = new Intent();
                runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                runCommand.setAction("masquerade.substratum.COMMANDS");
                runCommand.putStringArrayListExtra("pm-uninstall", final_commands_array);
                getContext().sendBroadcast(runCommand);
            } else {
                Root.runCommand(final_commands);
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            File current_overlays = new File(Environment
                    .getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml");
            if (current_overlays.exists()) {
                Root.runCommand("rm " + Environment
                        .getExternalStorageDirectory().getAbsolutePath() +
                        "/.substratum/current_overlays.xml");
            }
            Root.runCommand("cp /data/system/overlays.xml " +
                    Environment
                            .getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml");
            String[] commands0 = {Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml", "0"};
            String[] commands1 = {Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml", "1"};
            String[] commands2 = {Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml", "2"};
            String[] commands3 = {Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml", "3"};
            String[] commands4 = {Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml", "4"};
            String[] commands5 = {Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml", "5"};

            List<String> state0 = ReadOverlaysFile.main(commands0);
            List<String> state1 = ReadOverlaysFile.main(commands1);
            List<String> state2 = ReadOverlaysFile.main(commands2);
            List<String> state3 = ReadOverlaysFile.main(commands3);
            List<String> state4 = ReadOverlaysFile.main(commands4);
            List<String> state5 = ReadOverlaysFile.main(commands5);

            final_commands_array = new ArrayList<>(state0);
            final_commands_array.addAll(state1);
            final_commands_array.addAll(state2);
            final_commands_array.addAll(state3);
            final_commands_array.addAll(state4);
            final_commands_array.addAll(state5);
            if (final_commands_array.size() > 0)
                final_commands_array.add(" && pkill com.android.systemui");
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
            Root.runCommand("rm -r /data/system/theme/bootanimation.zip");
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

            // Reflect back to Settings and updateConfiguration() run on
            // simulated locale change

            try {
                Class<?> activityManagerNative = Class.forName("android.app" +
                        ".ActivityManagerNative");
                Object am = activityManagerNative.getMethod("getDefault").invoke
                        (activityManagerNative);
                Object config = am.getClass().getMethod("getConfiguration")
                        .invoke(am);
                am.getClass().getMethod("updateConfiguration", android
                        .content.res
                        .Configuration.class).invoke(am, config);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Finally, enable/disable the SystemUI dummy overlay
            if (isPackageInstalled("masquerade.substratum")) {
                Intent runCommand = new Intent();
                runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                runCommand.setAction("masquerade.substratum.COMMANDS");
                runCommand.putExtra("om-commands", final_commands);
                getContext().sendBroadcast(runCommand);
            } else {
                Root.runCommand(final_commands);
            }
            if (isPackageInstalled("masquerade.substratum")) {
                Intent runCommand = new Intent();
                runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                runCommand.setAction("masquerade.substratum.COMMANDS");
                runCommand.putExtra("om-commands", "setprop sys.refresh_theme 1");
                getContext().sendBroadcast(runCommand);
            } else {
                Root.runCommand("setprop sys.refresh_theme 1");
            }

            Toast toast = Toast.makeText(getContext(), getString(R
                            .string.manage_fonts_toast),
                    Toast.LENGTH_SHORT);
            toast.show();
        }

        @Override
        protected String doInBackground(String... sUrl) {
            final_commands = "";

            Root.runCommand("rm -r /data/system/theme/fonts/");

            File current_overlays = new File(Environment
                    .getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml");
            if (current_overlays.exists()) {
                Root.runCommand("rm " + Environment
                        .getExternalStorageDirectory().getAbsolutePath() +
                        "/.substratum/current_overlays.xml");
            }
            Root.runCommand("cp /data/system/overlays" +
                    ".xml " +
                    Environment
                            .getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml");

            String[] commands0 = {Environment.getExternalStorageDirectory()
                    .getAbsolutePath() +
                    "/.substratum/current_overlays.xml", "4"};
            String[] commands1 = {Environment.getExternalStorageDirectory()
                    .getAbsolutePath() +
                    "/.substratum/current_overlays.xml", "5"};

            List<String> state4overlays = ReadOverlaysFile.main(commands0);
            List<String> state5overlays = ReadOverlaysFile.main(commands1);

            if (state4overlays.contains("substratum.helper")) {
                final_commands = "om enable substratum.helper";
            } else {
                if (state5overlays.contains("substratum.helper")) {
                    final_commands = "om disable substratum.helper";
                } else {
                    final_commands = "pkill com.android.systemui";
                }
            }
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
            Root.runCommand("pkill com.android.systemui");
        }

        @Override
        protected String doInBackground(String... sUrl) {
            Root.runCommand("rm -r /data/system/theme/audio/");
            new SoundsHandler().SoundsClearer(getContext());
            return null;
        }
    }
}