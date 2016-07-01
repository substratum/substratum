package projekt.substratum.fragments;

import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.alimuzaffar.lib.widgets.AnimatedEditText;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import projekt.substratum.R;
import projekt.substratum.util.ReadOverlaysFile;
import projekt.substratum.util.Root;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class ProfileFragment extends Fragment {

    private List<String> list;
    private ProgressBar headerProgress;
    private Spinner profile_selector;
    private AnimatedEditText aet_backup;
    private String aet_getText;
    private String to_be_run_commands;
    private ArrayAdapter<String> adapter;
    private List<String> cannot_run_overlays;
    private String dialog_message;

    public void RefreshSpinner() {
        list.clear();

        list.add(getResources().getString(R.string.spinner_default_item));

        // Now lets add all the located profiles
        File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                "/substratum/profiles/");
        File[] files = f.listFiles();
        if (files != null) {
            for (File inFile : files) {
                if (!inFile.isDirectory()) {
                    if (inFile.getName().split("\\.")[inFile.getName().split("\\.").length - 1]
                            .equals("substratum")) {
                        list.add(inFile.getName().substring(0, inFile.getName().length() - 11));
                    }
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.profile_fragment, null);

        headerProgress = (ProgressBar) root.findViewById(R.id.header_loading_bar);
        headerProgress.setVisibility(View.GONE);

        // Create a user viewable directory for profiles

        File directory = new File(Environment.getExternalStorageDirectory(),
                "/substratum/");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File directory2 = new File(Environment.getExternalStorageDirectory(),
                "/substratum/profiles");
        if (!directory2.exists()) {
            directory2.mkdirs();
        }

        // Handle Backups

        aet_backup = (AnimatedEditText) root.findViewById(R.id.edittext);

        final Button backupButton = (Button) root.findViewById(R.id.backupButton);
        backupButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (aet_backup.getText().length() > 0) {
                    aet_getText = aet_backup.getText().toString();
                    BackupFunction backupFunction = new BackupFunction();
                    backupFunction.execute();
                } else {
                    Toast toast = Toast.makeText(getContext(), getString(R.string
                                    .profile_edittext_empty_toast),
                            Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });

        // Handle Restores

        list = new ArrayList<>();
        list.add(getResources().getString(R.string.spinner_default_item));

        profile_selector = (Spinner) root.findViewById(R.id.restore_spinner);

        // Now lets add all the located profiles
        File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                "/substratum/profiles/");
        File[] files = f.listFiles();
        if (files != null) {
            for (File inFile : files) {
                if (!inFile.isDirectory()) {
                    if (inFile.getName().split("\\.")[inFile.getName().split("\\.").length - 1]
                            .equals("substratum")) {
                        list.add(inFile.getName().substring(0, inFile.getName().length() - 11));
                    }
                }
            }
        }

        adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, list);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        profile_selector.setAdapter(adapter);

        ImageButton imageButton = (ImageButton) root.findViewById(R.id.remove_profile);
        imageButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (profile_selector.getSelectedItemPosition() > 0) {
                    String formatted = String.format(getString(R.string.delete_dialog_text),
                            profile_selector.getSelectedItem());
                    new AlertDialog.Builder(getContext())
                            .setTitle(getString(R.string.delete_dialog_title))
                            .setMessage(formatted)
                            .setCancelable(false)
                            .setPositiveButton(getString(R.string.delete_dialog_okay), new
                                    DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            File f = new File(Environment
                                                    .getExternalStorageDirectory().getAbsolutePath()
                                                    + "/substratum/profiles/" + profile_selector
                                                    .getSelectedItem() + "" +
                                                    ".substratum");
                                            boolean deleted = f.delete();
                                            RefreshSpinner();
                                        }
                                    })
                            .setNegativeButton(getString(R.string.delete_dialog_cancel), new
                                    DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                        }
                                    })
                            .create().show();
                } else {
                    Toast toast = Toast.makeText(getContext(), getString(R.string
                                    .profile_delete_button_none_selected_toast),
                            Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });

        final Button restoreButton = (Button) root.findViewById(R.id.restoreButton);
        restoreButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (profile_selector.getSelectedItemPosition() > 0) {
                    RestoreFunction restoreFunction = new RestoreFunction();
                    restoreFunction.execute(profile_selector.getSelectedItem().toString());
                } else {
                    Toast toast = Toast.makeText(getContext(), getString(R.string
                                    .restore_button_none_selected_toast),
                            Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
        return root;
    }

    private class BackupFunction extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            headerProgress.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            headerProgress.setVisibility(View.GONE);
            String directory_parse = String.format(getString(R.string.toast_backup_success),
                    aet_getText + ".substratum");
            Toast toast = Toast.makeText(getContext(), directory_parse, Toast.LENGTH_LONG);
            toast.show();
            RefreshSpinner();
        }

        @Override
        protected String doInBackground(String... sUrl) {
            Root.runCommand("cp /data/system/overlays.xml " +
                    Environment
                            .getExternalStorageDirectory().getAbsolutePath() +
                    "/substratum/profiles/" + aet_getText + ".substratum");

            return null;
        }
    }

    private class RestoreFunction extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            headerProgress.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (cannot_run_overlays.size() > 0) {
                new AlertDialog.Builder(getContext())
                        .setTitle(getString(R.string.restore_dialog_title))
                        .setMessage(dialog_message)
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.restore_dialog_okay), new
                                DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        headerProgress.setVisibility(View.GONE);
                                        Root.runCommand(to_be_run_commands);
                                    }
                                })
                        .setNegativeButton(getString(R.string.restore_dialog_cancel), new
                                DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        headerProgress.setVisibility(View.GONE);
                                    }
                                })
                        .create().show();
            } else {
                headerProgress.setVisibility(View.GONE);
                Root.runCommand(to_be_run_commands);
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            String profile_name = sUrl[0];

            Root.runCommand("cp /data/system/overlays" +
                    ".xml " +
                    Environment
                            .getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml");

            String[] commandsSystem4 = {Environment
                    .getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml", "4"};

            String[] commandsSystem5 = {Environment
                    .getExternalStorageDirectory().getAbsolutePath() +
                    "/.substratum/current_overlays.xml", "5"};

            String[] commands = {Environment.getExternalStorageDirectory()
                    .getAbsolutePath() +
                    "/substratum/profiles/" + profile_name + ".substratum", "5"};

            List<String> profile = ReadOverlaysFile.main(commands);
            List<String> system = ReadOverlaysFile.main(commandsSystem4);
            system.addAll(ReadOverlaysFile.main(commandsSystem5));
            List<String> to_be_run = new ArrayList<>();

            // Disable everything enabled first

            List<String> system5 = ReadOverlaysFile.main(commandsSystem5);

            String to_be_disabled = "";
            for (int i = 0; i < system5.size(); i++) {
                if (i == 0) {
                    to_be_disabled = "om disable " + system5.get(i);
                } else {
                    if (i > 0 && to_be_disabled.length() == 0) {
                        to_be_disabled = "om disable " + system5.get(i);
                    } else {
                        to_be_disabled = to_be_disabled + " && om disable " + system5.get(i);
                    }
                }
            }

            // Now process the overlays to be enabled

            cannot_run_overlays = new ArrayList<>();
            for (int i = 0; i < profile.size(); i++) {
                if (system.contains(profile.get(i))) {
                    to_be_run.add(profile.get(i));
                } else {
                    cannot_run_overlays.add(profile.get(i));
                }
            }
            dialog_message = "";
            for (int i = 0; i < cannot_run_overlays.size(); i++) {
                String not_split = cannot_run_overlays.get(i);
                String[] split = not_split.split("\\.");
                String theme_name = split[split.length - 1];
                String package_id = not_split.substring(0, not_split.length() - theme_name.length
                        () - 1);
                String package_name = "";

                try {
                    ApplicationInfo applicationInfo = getContext().getPackageManager()
                            .getApplicationInfo
                                    (package_id, 0);
                    String packageTitle = getContext().getPackageManager().getApplicationLabel
                            (applicationInfo).toString();
                    package_name = packageTitle;
                } catch (PackageManager.NameNotFoundException nnfe) {
                    Log.e("SubstratumLogger", "Could not find explicit package identifier" +
                            " in package manager list.");
                }

                if (i == 0) {
                    dialog_message = dialog_message + "\u2022 " + theme_name + " [" +
                            package_name + "]";
                } else {
                    if (i > 0 && dialog_message.length() == 0) {
                        dialog_message = dialog_message + "\u2022 " + theme_name + " [" +
                                package_name + "]";
                    } else {
                        dialog_message = dialog_message + "\n" + "\u2022 " + theme_name + " [" +
                                package_name + "]";
                    }
                }
            }

            to_be_run_commands = "";
            for (int i = 0; i < to_be_run.size(); i++) {
                if (i == 0) {
                    to_be_run_commands = "om enable " + to_be_run.get(i);
                } else {
                    if (i > 0 && to_be_run_commands.length() == 0) {
                        to_be_run_commands = "om enable " + to_be_run.get(i);
                    } else {
                        to_be_run_commands = to_be_run_commands + " && om enable " + to_be_run
                                .get(i);
                    }
                }
            }
            if (to_be_run_commands.length() > 0) {
                to_be_run_commands = to_be_run_commands + " && cp /data/system/overlays.xml " +
                        Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/.substratum/current_overlays.xml";
            }

            if (to_be_disabled.length() > 0) {
                to_be_run_commands = to_be_disabled + " && " + to_be_run_commands;
            }

            Log.e("SubstratumRestore", to_be_run_commands);
            return null;
        }
    }
}