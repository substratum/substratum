package projekt.substratum.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
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
import projekt.substratum.config.References;
import projekt.substratum.util.ReadOverlays;
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
    private boolean helper_exists = true;
    private SharedPreferences prefs;

    public void RefreshSpinner() {
        list.clear();

        list.add(getResources().getString(R.string.spinner_default_item));

        // Now lets add all the located profiles
        File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                "/substratum/profiles/");
        File[] files = f.listFiles();
        if (files != null) {
            for (File inFile : files) {
                if (References.checkOMS()) {
                    if (!inFile.isDirectory()) {
                        if (inFile.getName().split("\\.")[inFile.getName().split("\\.").length - 1]
                                .equals("substratum")) {
                            list.add(inFile.getName().substring(0, inFile.getName().length() - 11));
                        }
                    }
                } else {
                    if (inFile.isDirectory()) {
                        // Because overlays.xml does not exist in RRO, there shouldn't need these
                        // files. Remove the .substratum filter.
                        list.add(inFile.getName());
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

        prefs = PreferenceManager.getDefaultSharedPreferences(
                getContext());

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
                if (References.checkOMS()) {
                    if (!inFile.isDirectory()) {
                        if (inFile.getName().split("\\.")[inFile.getName().split("\\.").length - 1]
                                .equals("substratum")) {
                            list.add(inFile.getName().substring(0, inFile.getName().length() - 11));
                        }
                    }
                } else {
                    if (inFile.isDirectory()) {
                        // Because overlays.xml does not exist in RRO, there shouldn't need these
                        // files. Remove the .substratum filter.
                        list.add(inFile.getName());
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
                                            Root.runCommand("rm -r " +
                                                    Environment.getExternalStorageDirectory()
                                                            .getAbsolutePath() +
                                                    "/substratum/profiles/" + profile_selector
                                                    .getSelectedItem());
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
            if (References.checkOMS()) {
                String directory_parse = String.format(getString(R.string.toast_backup_success),
                        aet_getText + ".substratum");
                Toast toast = Toast.makeText(getContext(), directory_parse, Toast.LENGTH_LONG);
                toast.show();
            } else {
                String directory_parse = String.format(getString(R.string.toast_backup_success),
                        aet_getText + "/");
                Toast toast = Toast.makeText(getContext(), directory_parse, Toast.LENGTH_LONG);
                toast.show();
            }
            RefreshSpinner();
        }

        @Override
        protected String doInBackground(String... sUrl) {
            if (References.checkOMS()) {
                Root.runCommand("cp /data/system/overlays.xml " +
                        Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/substratum/profiles/" + aet_getText + ".substratum");

                File makeProfileDir = new File(Environment
                        .getExternalStorageDirectory().getAbsolutePath() +
                        "/substratum/profiles/" + aet_getText + "/");
                if (makeProfileDir.exists()) {
                    Root.runCommand("rm -r " +
                            Environment.getExternalStorageDirectory().getAbsolutePath() +
                            "/substratum/profiles/" + aet_getText);
                    boolean created = makeProfileDir.mkdir();
                } else {
                    boolean created = makeProfileDir.mkdir();
                }

                // Backup the entire /data/system/theme/ folder
                Root.runCommand("cp -rf /data/system/theme/ " +
                        Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/substratum/profiles/" + aet_getText);
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
                    Root.runCommand("cp -rf " + current_directory + " " +
                            Environment.getExternalStorageDirectory().getAbsolutePath() +
                            "/substratum/profiles/");
                    File oldFolder = new File(Environment.getExternalStorageDirectory()
                            .getAbsolutePath() + "/substratum/profiles/overlay");
                    File newFolder = new File(Environment.getExternalStorageDirectory()
                            .getAbsolutePath() + "/substratum/profiles/" + aet_getText);
                    boolean success = oldFolder.renameTo(newFolder);

                    // Now begin backing up sounds
                    Root.runCommand("cp -rf /data/system/theme/audio/ " +
                            Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() + "/substratum/profiles/" + aet_getText);
                    Root.runCommand("mount -o ro,remount /system");
                } else {
                    Toast toast = Toast.makeText(getContext(), getString(R.string
                                    .backup_no_overlays),
                            Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
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
            if (References.checkOMS()) {
                if (cannot_run_overlays.size() > 0) {
                    new AlertDialog.Builder(getContext())
                            .setTitle(getString(R.string.restore_dialog_title))
                            .setMessage(dialog_message)
                            .setCancelable(false)
                            .setPositiveButton(getString(R.string.restore_dialog_okay), new
                                    DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (References.isPackageInstalled(getContext(),
                                                    "masquerade.substratum")) {
                                                Intent runCommand = new Intent();
                                                runCommand.addFlags(Intent
                                                        .FLAG_INCLUDE_STOPPED_PACKAGES);
                                                runCommand.setAction("masquerade.substratum" +
                                                        ".COMMANDS");
                                                runCommand.putExtra("om-commands",
                                                        to_be_run_commands);

                                                getContext().sendBroadcast(runCommand);
                                                if (!helper_exists) {
                                                    Toast toast = Toast.makeText(getContext(),
                                                            getString(R.string
                                                                    .profile_edittext_empty_toast_sysui),
                                                            Toast.LENGTH_SHORT);
                                                    toast.show();
                                                }
                                            } else {
                                                Root.runCommand(to_be_run_commands);
                                                if (!helper_exists) {
                                                    Toast toast = Toast.makeText(getContext(),
                                                            getString(R.string
                                                                    .profile_edittext_empty_toast_sysui),
                                                            Toast.LENGTH_SHORT);
                                                    toast.show();
                                                }
                                            }
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
                    if (References.isPackageInstalled(getContext(), "masquerade.substratum")) {
                        Intent runCommand = new Intent();
                        runCommand.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                        runCommand.setAction("masquerade.substratum.COMMANDS");
                        runCommand.putExtra("om-commands", to_be_run_commands);
                        getContext().sendBroadcast(runCommand);
                        if (!helper_exists) {
                            Toast toast = Toast.makeText(getContext(), getString(R.string
                                            .profile_edittext_empty_toast_sysui),
                                    Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    } else {
                        Root.runCommand(to_be_run_commands);
                        if (!helper_exists) {
                            Toast toast = Toast.makeText(getContext(), getString(R.string
                                            .profile_edittext_empty_toast_sysui),
                                    Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    }
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
                    // Delete destination overlays
                    Root.runCommand("mount -o rw,remount /system");
                    Root.runCommand("rm -r" + current_directory);
                    Root.runCommand("rm -r /data/system/theme/");
                    Root.runCommand("mkdir " + current_directory);
                    Root.runCommand("mkdir /data/system/theme/");
                    Root.runCommand("chmod 755 /data/system/theme/");

                    File profile_apk_files = new File(Environment.getExternalStorageDirectory()
                            .getAbsolutePath() + "/substratum/profiles/" +
                            profile_selector.getSelectedItem() + "/");
                    String[] located_files = profile_apk_files.list();
                    for (int i = 0; i < located_files.length; i++) {
                        if (!located_files[i].equals("audio")) {
                            Root.runCommand("cp -rf " + Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() +
                                    "/substratum/profiles/" + profile_selector.getSelectedItem() +
                                    "/" + located_files[i] + " " + current_directory);
                        } else {
                            Root.runCommand("cp -rf " + Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() +
                                    "/substratum/profiles/" + profile_selector.getSelectedItem() +
                                    "/" + located_files[i] + "/ " + "/data/system/theme/audio/");
                            Root.runCommand("chmod -R 644 /data/system/theme/audio/");
                            Root.runCommand("chmod 755 /data/system/theme/audio/");
                        }
                    }

                    Root.runCommand("chmod -R 644 " + current_directory);
                    Root.runCommand("chmod 755 " + current_directory);
                    Root.runCommand("chcon -R u:object_r:system_file:s0 " + current_directory);
                    Root.runCommand("mount -o ro,remount /system");
                } else {
                    String vendor_location = "/system/vendor/overlay/";
                    String vendor_partition = "/vendor/overlay/";
                    String vendor_symlink = "/system/overlay/";
                    String current_vendor =
                            ((References.inNexusFilter()) ? vendor_partition :
                                    vendor_location);

                    Root.runCommand("mount -o rw,remount /system");

                    File vendor = new File(current_vendor);
                    if (!vendor.exists()) {
                        if (current_vendor.equals(vendor_location)) {
                            Root.runCommand("mkdir " + current_vendor);
                        } else {
                            Root.runCommand("mount -o rw,remount /vendor");
                            Root.runCommand("mkdir " + vendor_symlink);
                            Root.runCommand("ln -s " + vendor_symlink + " /vendor");
                            Root.runCommand("chmod 755 " + vendor_partition);
                            Root.runCommand("mount -o ro,remount /vendor");
                        }
                    }

                    Root.runCommand("rm -r /data/system/theme/");
                    Root.runCommand("mkdir /data/system/theme/");

                    File profile_apk_files = new File(Environment.getExternalStorageDirectory()
                            .getAbsolutePath() + "/substratum/profiles/" +
                            profile_selector.getSelectedItem() + "/");
                    String[] located_files = profile_apk_files.list();
                    for (int i = 0; i < located_files.length; i++) {
                        if (!located_files[i].equals("audio")) {
                            Root.runCommand("cp -rf " + Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() +
                                    "/substratum/profiles/" + profile_selector.getSelectedItem() +
                                    "/" + located_files[i] + " " + current_directory);
                        } else {
                            Root.runCommand("chmod 755 /data/system/theme/");
                            Root.runCommand("cp -rf " + Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() +
                                    "/substratum/profiles/" + profile_selector.getSelectedItem() +
                                    "/" + located_files[i] + "/ " + "/data/system/theme/audio/");
                            Root.runCommand("chmod -R 644 /data/system/theme/audio/");
                            Root.runCommand("chmod 755 /data/system/theme/audio/");
                        }
                    }

                    Root.runCommand("chmod -R 644 " + current_directory);
                    Root.runCommand("chmod 755 " + current_directory);
                    Root.runCommand("chcon -R u:object_r:system_file:s0 " + current_directory);
                    Root.runCommand("mount -o ro,remount /system");
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
                                android.R.string.ok, new DialogInterface
                                        .OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        Root.runCommand("reboot");
                                    }
                                });
                alertDialogBuilder.setCancelable(false);
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
                headerProgress.setVisibility(View.GONE);
            }
            super.onPostExecute(result);
        }

        @Override
        protected String doInBackground(String... sUrl) {

            if (References.checkOMS()) {  // RRO doesn't need any of this
                String profile_name = sUrl[0];
                helper_exists = true;

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

                List<String> profile = ReadOverlays.main(5);
                List<String> system = ReadOverlays.main(4);
                List<String> system_active = ReadOverlays.main(5);
                system.addAll(ReadOverlays.main(5));
                List<String> to_be_run = new ArrayList<>();

                // Disable everything enabled first
                String to_be_disabled = "om disable-all";

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
                    String package_id = not_split.substring(0, not_split.length() - theme_name
                            .length() - 1);
                    String package_name = "";
                    try {
                        ApplicationInfo applicationInfo = getContext().getPackageManager()
                                .getApplicationInfo
                                        (package_id, 0);
                        String packageTitle = getContext().getPackageManager().getApplicationLabel
                                (applicationInfo).toString();
                        package_name = packageTitle;
                    } catch (Exception e) {
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
                    if (!to_be_run.get(i).equals("substratum.helper")) {
                        if (i == 0) {
                            to_be_run_commands = "om enable " + to_be_run.get(i);
                        } else {
                            if (i > 0 && to_be_run_commands.length() == 0) {
                                to_be_run_commands = "om enable " + to_be_run.get(i);
                            } else {
                                to_be_run_commands = to_be_run_commands + " " + to_be_run
                                        .get(i);
                            }
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

                File theme = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/substratum/profiles/" + profile_name + "/");
                if (theme.length() > 0) {
                    // Restore the whole backed up profile back to /data/system/theme/
                    to_be_run_commands = to_be_run_commands + " && rm -r " + "/data/system/theme";

                    // Set up work directory again

                    to_be_run_commands = to_be_run_commands + " && cp -rf " +
                            Environment.getExternalStorageDirectory().getAbsolutePath() +
                            "/substratum/profiles/" + profile_name + "/ /data/system/theme/";
                    to_be_run_commands = to_be_run_commands + " && chmod 755 /data/system/theme/";

                    // Boot Animation
                    File bootanimations = new File(Environment.getExternalStorageDirectory()
                            .getAbsolutePath() +

                            "/substratum/profiles/" + profile_name + "/bootanimation.zip");
                    if (bootanimations.exists()) {
                        to_be_run_commands = to_be_run_commands +
                                " && chmod 644 /data/system/theme/bootanimation.zip";
                    }

                    // Fonts
                    File fonts = new File(Environment.getExternalStorageDirectory()
                            .getAbsolutePath() +
                            "/substratum/profiles/" + profile_name + "/fonts/");
                    if (fonts.exists()) {
                        to_be_run_commands = to_be_run_commands + " && chmod -R 747 /data/system" +
                                "/theme/fonts/";
                        to_be_run_commands = to_be_run_commands + " && chmod 775 /data/system" +
                                "/theme" +
                                "/fonts/";
                    }

                    // Sounds
                    File sounds = new File(Environment.getExternalStorageDirectory()
                            .getAbsolutePath() +
                            "/substratum/profiles/" + profile_name + "/audio/");
                    if (sounds.exists()) {
                        File alarms = new File(Environment.getExternalStorageDirectory()
                                .getAbsolutePath() +
                                "/substratum/profiles/" + profile_name + "/audio/alarms/");
                        if (alarms.exists()) {
                            to_be_run_commands = to_be_run_commands +
                                    " && chmod -R 644 /data/system/theme/audio/alarms/";
                            to_be_run_commands = to_be_run_commands +
                                    " && chmod 755 /data/system/theme/audio/alarms/";
                        }

                        File notifications = new File(Environment.getExternalStorageDirectory()
                                .getAbsolutePath() +
                                "/substratum/profiles/" + profile_name + "/audio/notifications/");
                        if (notifications.exists()) {
                            to_be_run_commands = to_be_run_commands +
                                    " && chmod -R 644 /data/system/theme/audio/notifications/";
                            to_be_run_commands = to_be_run_commands +
                                    " && chmod 755 /data/system/theme/audio/notifications/";
                        }

                        File ringtones = new File(Environment.getExternalStorageDirectory()
                                .getAbsolutePath() +

                                "/substratum/profiles/" + profile_name + "/audio/ringtones/");
                        if (ringtones.exists()) {
                            to_be_run_commands = to_be_run_commands +
                                    " && chmod -R 644 /data/system/theme/audio/ringtones/";
                            to_be_run_commands = to_be_run_commands +
                                    " && chmod 755 /data/system/theme/audio/ringtones/";
                        }

                        File ui = new File(Environment.getExternalStorageDirectory()
                                .getAbsolutePath() +
                                "/substratum/profiles/" + profile_name + "/audio/ui/");
                        if (ui.exists()) {
                            to_be_run_commands = to_be_run_commands +
                                    " && chmod -R 644 /data/system/theme/audio/ui/";
                            to_be_run_commands = to_be_run_commands +
                                    " && chmod 755 /data/system/theme/audio/ui/";
                        }

                        to_be_run_commands = to_be_run_commands +
                                " && chmod 755 /data/system/theme/audio/";
                    }

                    // Final touch ups
                    to_be_run_commands = to_be_run_commands + " && chcon -R " +
                            "u:object_r:system_file:s0" +
                            " " +
                            "/data/system/theme";
                    to_be_run_commands = to_be_run_commands + " && setprop sys.refresh_theme 1";

                    if (fonts.exists()) {
                        if (system.contains("substratum.helper")) {
                            to_be_run_commands = to_be_run_commands + " && om enable substratum" +
                                    ".helper";
                        } else {
                            if (system_active.contains("substratum.helper")) {
                                to_be_run_commands = to_be_run_commands + " && om disable " +
                                        "substratum" +
                                        ".helper";
                            } else {
                                helper_exists = false;
                            }
                        }
                    }
                }
                if (!prefs.getBoolean("systemui_recreate", false)) {
                    to_be_run_commands = to_be_run_commands + " && pkill -f com.android.systemui";
                }
                Log.e("SubstratumRestore", to_be_run_commands);
            }
            return null;
        }
    }
}