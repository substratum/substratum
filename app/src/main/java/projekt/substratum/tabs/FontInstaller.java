package projekt.substratum.tabs;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import kellinwood.security.zipsigner.ZipSigner;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import projekt.substratum.InformationActivityTabs;
import projekt.substratum.R;
import projekt.substratum.util.ReadOverlaysFile;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class FontInstaller extends Fragment {

    private String theme_pid;
    private ViewGroup root;
    private MaterialProgressBar progressBar;
    private ImageButton imageButton;
    private Spinner fontSelector;
    private ColorStateList unchecked, checked;
    private ProgressDialog progress;
    private RelativeLayout font_holder;
    private String final_commands;
    private RelativeLayout font_placeholder;

    private boolean checkChangeConfigurationPermissions() {
        String permission = "android.permission.CHANGE_CONFIGURATION";
        int res = getContext().checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    private boolean isPackageInstalled(Context context, String package_name) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(package_name, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {

        theme_pid = InformationActivityTabs.getThemePID();

        root = (ViewGroup) inflater.inflate(R.layout.tab_fragment_4, container, false);

        progressBar = (MaterialProgressBar) root.findViewById(R.id.progress_bar_loader);

        font_holder = (RelativeLayout) root.findViewById(R.id.font_holder);
        font_placeholder = (RelativeLayout) root.findViewById(R.id.font_placeholder);

        imageButton = (ImageButton) root.findViewById(R.id.checkBox);
        imageButton.setClickable(false);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new FontHandler().execute(fontSelector.getSelectedItem()
                        .toString());
            }
        });

        unchecked = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{
                        getContext().getColor(R.color.font_unchecked),
                        getContext().getColor(R.color.font_unchecked)
                }
        );
        checked = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{
                        getContext().getColor(R.color.font_checked),
                        getContext().getColor(R.color.font_checked)
                }
        );

        try {
            Context otherContext = getContext().createPackageContext(theme_pid, 0);
            AssetManager am = otherContext.getAssets();
            String[] fontsToParse = am.list("fonts");
            ArrayList<String> fonts = new ArrayList<>();
            fonts.add(getString(R.string.font_default_spinner));
            for (int i = 0; i < fontsToParse.length; i++) {
                fonts.add(fontsToParse[i].substring(0,
                        fontsToParse[i].length() - 4));
            }
            ArrayAdapter<String> adapter1 = new ArrayAdapter<>(getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, fonts);
            fontSelector = (Spinner) root.findViewById(R.id.fontSelection);
            fontSelector.setAdapter(adapter1);
            fontSelector.setOnItemSelectedListener(new AdapterView
                    .OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1,
                                           int pos, long id) {
                    if (pos == 0) {
                        font_placeholder.setVisibility(View.VISIBLE);
                        imageButton.setClickable(false);
                        imageButton.setImageTintList(unchecked);
                        font_holder.setVisibility(View.GONE);
                        progressBar.setVisibility(View.GONE);
                    } else {
                        font_placeholder.setVisibility(View.GONE);
                        String[] commands = {arg0.getSelectedItem().toString()};
                        new FontPreview().execute(commands);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("FontHandler", "There is no font.zip found within the assets " +
                    "of this theme!");
        }

        return root;
    }

    private class FontHandler extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            if (!checkChangeConfigurationPermissions()) {
                Log.e("FontHandler", "Substratum was not granted " +
                        "CHANGE_CONFIGURATION permissions, allowing now...");
                eu.chainfire.libsuperuser.Shell.SU.run("pm grant projekt.substratum " +
                        "android.permission.CHANGE_CONFIGURATION");
            } else {
                Log.d("FontHandler", "Substratum was granted CHANGE_CONFIGURATION permissions!");
            }
            progress = new ProgressDialog(getContext(), android.R.style
                    .Theme_DeviceDefault_Dialog_Alert);
            progress.setMessage(getString(R.string.font_dialog_apply_text));
            progress.setIndeterminate(false);
            progress.setCancelable(false);
            progress.show();
        }

        @Override
        protected void onPostExecute(String result) {
            progress.dismiss();
            if (result == null) {
                Toast toast = Toast.makeText(getContext(),
                        getString(R.string.font_dialog_apply_success), Toast.LENGTH_LONG);
                toast.show();
            } else {
                Toast toast = Toast.makeText(getContext(),
                        getString(R.string.font_dialog_apply_failed), Toast.LENGTH_LONG);
                toast.show();
            }

            if (final_commands.length() > 0 && result == null) {

                // Reflect back to Settings and updateConfiguration() run on simulated locale change

                try {
                    Class<?> activityManagerNative = Class.forName("android.app" +
                            ".ActivityManagerNative");
                    Object am = activityManagerNative.getMethod("getDefault").invoke
                            (activityManagerNative);
                    Object config = am.getClass().getMethod("getConfiguration").invoke(am);
                    am.getClass().getMethod("updateConfiguration", android.content.res
                            .Configuration.class).invoke(am, config);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Finally, enable/disable the SystemUI dummy overlay

                eu.chainfire.libsuperuser.Shell.SU.run(final_commands);
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {

            try {
                // Move the file from assets folder to a new working area

                Log.d("FontHandler", "Copying over the selected fonts to working " +
                        "directory...");

                File cacheDirectory = new File(getContext().getCacheDir(), "/FontCache/");
                if (!cacheDirectory.exists()) {
                    boolean created = cacheDirectory.mkdirs();
                    if (created) Log.d("FontHandler", "Successfully created cache folder!");
                }
                File cacheDirectory2 = new File(getContext().getCacheDir(), "/FontCache/" +
                        "FontCreator/");
                if (!cacheDirectory2.exists()) {
                    boolean created = cacheDirectory2.mkdirs();
                    if (created) Log.d("FontHandler", "Successfully created cache folder work " +
                            "directory!");
                } else {
                    eu.chainfire.libsuperuser.Shell.SU.run(
                            "rm -r " + getContext().getCacheDir().getAbsolutePath() +
                                    "/FontCache/FontCreator/");
                    boolean created = cacheDirectory2.mkdirs();
                    if (created) Log.d("FontHandler", "Successfully recreated cache folder work " +
                            "directory!");
                }

                // Copy the font.zip from assets/fonts of the theme's assets

                String source = sUrl[0] + ".zip";

                try {
                    Context otherContext = getContext().createPackageContext(theme_pid, 0);
                    AssetManager am = otherContext.getAssets();
                    InputStream inputStream = am.open("fonts/" + source);
                    OutputStream outputStream = new FileOutputStream(getContext().getCacheDir()
                            .getAbsolutePath() + "/FontCache/" + source);
                    CopyStream(inputStream, outputStream);
                } catch (Exception e) {
                    Log.e("FontHandler", "There is no fonts.zip found within the assets " +
                            "of this theme!");
                }

                // Unzip the fonts to get it prepared for the preview
                unzip(getContext().getCacheDir().getAbsolutePath() +
                                "/FontCache/" + source,
                        getContext().getCacheDir().getAbsolutePath() +
                                "/FontCache/FontCreator/");

                // Copy all the system fonts to /data/system/theme/fonts

                File dataSystemThemeDir = new File("/data/system/theme");
                if (!dataSystemThemeDir.exists()) {
                    eu.chainfire.libsuperuser.Shell.SU.run("mount -o remount,rw /data");
                    eu.chainfire.libsuperuser.Shell.SU.run(
                            "mkdir /data/system/theme/");
                } else {
                    eu.chainfire.libsuperuser.Shell.SU.run("mount -o remount,rw /data");
                    eu.chainfire.libsuperuser.Shell.SU.run(
                            "rm -r /data/system/theme/");
                    eu.chainfire.libsuperuser.Shell.SU.run(
                            "mkdir /data/system/theme/");
                }
                File dataSystemThemeFontsDir = new File("/data/system/theme/fonts");
                if (!dataSystemThemeFontsDir.exists()) {
                    eu.chainfire.libsuperuser.Shell.SU.run("mount -o remount,rw /data");
                    eu.chainfire.libsuperuser.Shell.SU.run(
                            "mkdir /data/system/theme/fonts");
                }

                // Copy font configuration file (fonts.xml) to the working directory
                copyAssets();

                eu.chainfire.libsuperuser.Shell.SU.run(
                        "cp /system/fonts/* /data/system/theme/fonts/");

                // Copy all the files from work directory to /data/system/theme/fonts

                eu.chainfire.libsuperuser.Shell.SU.run(
                        "cp -F " + getContext().getCacheDir().getAbsolutePath() +
                                "/FontCache/FontCreator/*" + " /data/system/theme/fonts/");

                // Check if substratum_helper overlay installed, if not, create it

                if (!isPackageInstalled(getContext(), "substratum.helper")) {
                    Log.e("FontHandler", "Substratum Helper Overlay not installed, compiling and " +
                            "installing now...");

                    File helperDirectory = new File(getContext().getCacheDir(),
                            "/FontCache/helper/");

                    if (!helperDirectory.exists()) {
                        boolean created = helperDirectory.mkdirs();
                        if (created) Log.d("FontHandler", "Helper folder created");
                    }
                    File helperDirectoryRes = new File(getContext().getCacheDir(),
                            "/FontCache/helper/res/");
                    if (!helperDirectoryRes.exists()) {
                        boolean created = helperDirectoryRes.mkdirs();
                        if (created) Log.d("FontHandler", "Helper resources folder created");
                    }
                    File helperDirectoryResDrawable =
                            new File(getContext().getCacheDir(),
                                    "/FontCache/helper/res/drawable-xxhdpi/");
                    if (!helperDirectoryResDrawable.exists()) {
                        boolean created = helperDirectoryResDrawable.mkdirs();
                        if (created) Log.d("FontHandler", "Helper drawable folder created");
                    }
                    File helperDirectoryResValues =
                            new File(getContext().getCacheDir(), "/FontCache/helper/res/values/");
                    if (!helperDirectoryResValues.exists()) {
                        boolean created = helperDirectoryResValues.mkdirs();
                        if (created) Log.d("FontHandler", "Helper values folder created");
                    }

                    // Create new icon in /res/drawable-xxhdpi from Substratum's mipmap-xxhdpi
                    Resources res;
                    Drawable hero = null;
                    try {
                        res = getContext().getPackageManager().getResourcesForApplication(
                                getContext().getApplicationContext().getPackageName());
                        int resourceId = res.getIdentifier(getContext().getApplicationContext()
                                .getPackageName() + ":mipmap/main_launcher", null, null);
                        if (0 != resourceId) {
                            hero = getContext().getPackageManager().getDrawable(getContext()
                                    .getApplicationContext().getPackageName(), resourceId, null);
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }

                    try {
                        Bitmap bmp = ((BitmapDrawable) hero).getBitmap();
                        FileOutputStream out = null;
                        try {
                            out = new FileOutputStream(new File(getContext().getCacheDir(),
                                    "/FontCache/helper/res/drawable-xxhdpi/app_icon.png"));
                            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                if (out != null) {
                                    out.close();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (NullPointerException npe) {
                        Log.e("FontHandler", "Could not create bitmap drawable of app icon!");
                    }

                    // Create new EXISTING boolean file based on MM6.0+ to surpass non-dangerous
                    // overlay

                    File root = new File(getContext().getCacheDir(),
                            "/FontCache/helper/res/values/bools.xml");
                    try {
                        boolean created = root.createNewFile();
                        if (created) Log.d("FontHandler", "Allowing handler overlay take on " +
                                "resources from SystemUI");
                        FileWriter fw = new FileWriter(root);
                        BufferedWriter bw = new BufferedWriter(fw);
                        PrintWriter pw = new PrintWriter(bw);
                        String string =
                                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                        "<resources>\n" +
                                        "        <bool name=\"enable_volume_ui\">true</bool>\n" +
                                        "</resources>";
                        pw.write(string);
                        pw.close();
                        bw.close();
                        fw.close();
                    } catch (Exception e) {
                        Log.e("FontHandler", "There was an exception creating a new bools file!");
                    }

                    // Create dummy AndroidManifest

                    File root2 = new File(getContext().getCacheDir(),
                            "/FontCache/helper/AndroidManifest.xml");
                    try {
                        boolean created = root2.createNewFile();
                        if (created) Log.d("FontHandler", "AndroidManifest file created");
                        FileWriter fw2 = new FileWriter(root2);
                        BufferedWriter bw2 = new BufferedWriter(fw2);
                        PrintWriter pw2 = new PrintWriter(bw2);

                        PackageInfo pInfo = getContext().getPackageManager()
                                .getPackageInfo(getContext().getPackageName(), 0);
                        String version = pInfo.versionName;

                        String manifest =
                                "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>\n" +
                                        "<manifest xmlns:android=\"http://schemas.android" +
                                        ".com/apk/res/android\" package=\"" +
                                        "substratum.helper" + "\"\n" +
                                        "        android:versionName=\"" + version + "\"> \n" +
                                        "    <overlay android:targetPackage=\"" +
                                        "com.android.systemui" +
                                        "\"/>\n" +
                                        "    <application android:label=\"" +
                                        getString(R.string.helper_name) + "\"\n" +
                                        "         android:icon=\"@drawable/app_icon\"/>\n" +
                                        "</manifest>\n";
                        pw2.write(manifest);
                        pw2.close();
                        bw2.close();
                        fw2.close();
                    } catch (Exception e) {
                        Log.e("FontHandler", "There was an exception creating a new Manifest " +
                                "file!");
                    }

                    // Begin aapt and compile the temp file

                    try {
                        String commands = "aapt p -M " + getContext().getCacheDir()
                                .getAbsolutePath() +

                                "/FontCache/helper/AndroidManifest.xml " +
                                "-S " + getContext().getCacheDir().getAbsolutePath() +
                                "/FontCache/helper/res/ " +
                                "-I " + "/system/framework/framework-res.apk " +
                                "-F " + getContext().getCacheDir().getAbsolutePath() +
                                "/FontCache/helper/unsigned_helper.apk " +
                                "-f\n";

                        String line;
                        Process nativeApp = Runtime.getRuntime().exec(commands);

                        OutputStream stdin = nativeApp.getOutputStream();
                        InputStream stderr = nativeApp.getErrorStream();
                        InputStream stdout = nativeApp.getInputStream();
                        stdin.write(("ls\n").getBytes());
                        stdin.write("exit\n".getBytes());
                        stdin.flush();
                        stdin.close();

                        BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
                        while ((line = br.readLine()) != null) {
                            Log.d("OverlayOptimizer", line);
                        }
                        br.close();
                        br = new BufferedReader(new InputStreamReader(stderr));
                        while ((line = br.readLine()) != null) {
                            Log.e("SubstratumBuilder", line);
                        }
                        br.close();
                        nativeApp.waitFor();
                    } catch (Exception e) {
                        Log.e("FontHandler", "Unfortunately, there was an exception trying to " +
                                "create the Substratum Overlay Helper " +
                                "APK");
                    }

                    // Sign the helper overlay

                    try {
                        // Sign with the built-in test key/certificate.
                        String sign_source = getContext().getCacheDir().getAbsolutePath() +
                                "/FontCache/helper/unsigned_helper.apk";
                        String sign_destination = Environment.getExternalStorageDirectory()
                                .getAbsolutePath() +
                                "/.substratum/" + "substratum.helper" + "-signed.apk";

                        ZipSigner zipSigner = new ZipSigner();
                        zipSigner.setKeymode("testkey");
                        zipSigner.signZip(sign_source, sign_destination);

                        Log.d("FontHandler", "Substratum Helper Overlay APK successfully signed!");
                    } catch (Throwable t) {
                        t.printStackTrace();
                        Log.e("FontHandler", "Substratum Helper Overlay APK could not be signed. " +
                                t.toString());
                    }

                    // Install the Helper

                    eu.chainfire.libsuperuser.Shell.SU.run(
                            "pm install -r " + Environment.getExternalStorageDirectory()
                                    .getAbsolutePath() + "/.substratum/substratum.helper-signed" +
                                    ".apk");

                    // Bruteforce the cache folder out of here due to possible MAJOR access
                    // permissions denial

                    eu.chainfire.libsuperuser.Shell.SU.run(
                            "rm -r " + getContext().getCacheDir().getAbsolutePath() +
                                    "/FontCache/");

                    final_commands = "om enable substratum.helper";
                } else {
                    eu.chainfire.libsuperuser.Shell.SU.run("cp /data/system/overlays" +
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
                        }
                    }
                }

                // Whether the user has the helper installed, we have to check for correct
                // permissions
                // and system file context integrity.

                eu.chainfire.libsuperuser.Shell.SU.run("mount -o remount,rw /system");
                eu.chainfire.libsuperuser.Shell.SU.run("chmod 755 /data/system/theme/");

                eu.chainfire.libsuperuser.Shell.SU.run("mount -o remount,rw /system");
                eu.chainfire.libsuperuser.Shell.SU.run("chmod -R 747 " +
                        "/data/system/theme/fonts/");

                eu.chainfire.libsuperuser.Shell.SU.run("mount -o remount,rw /system");
                eu.chainfire.libsuperuser.Shell.SU.run("chmod 775 " +
                        "/data/system/theme/fonts/");

                eu.chainfire.libsuperuser.Shell.SU.run("mount -o remount,rw /data");
                eu.chainfire.libsuperuser.Shell.SU.run("chcon -R u:object_r:system_file:s0 " +
                        "/data/system/theme");
            } catch (Exception e) {
                e.printStackTrace();
                return "failed";
            }
            return null;
        }

        private void copyAssets() {
            AssetManager assetManager = getContext().getAssets();
            InputStream in;
            OutputStream out;
            String filename = "fonts.xml";
            try {
                in = assetManager.open(filename);
                out = new FileOutputStream(getContext().getCacheDir().getAbsolutePath() +
                        "/FontCache/FontCreator/" + filename);
                copyFile(in, out);
                in.close();
                in = null;
                out.flush();
                out.close();
                out = null;
            } catch (IOException e) {
                Log.e("FontHandler", "Failed to move font configuration file to working " +
                        "directory!");
            }
        }

        private void copyFile(InputStream in, OutputStream out) throws IOException {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }

        private void unzip(String source, String destination) {
            try {
                ZipInputStream inputStream = new ZipInputStream(
                        new BufferedInputStream(new FileInputStream(source)));
                ZipEntry zipEntry;
                int count;
                byte[] buffer = new byte[8192];
                while ((zipEntry = inputStream.getNextEntry()) != null) {
                    File file = new File(destination, zipEntry.getName());
                    File dir = zipEntry.isDirectory() ? file : file.getParentFile();
                    if (!dir.isDirectory() && !dir.mkdirs())
                        throw new FileNotFoundException("Failed to ensure directory: " +
                                dir.getAbsolutePath());
                    if (zipEntry.isDirectory())
                        continue;
                    FileOutputStream outputStream = new FileOutputStream(file);
                    while ((count = inputStream.read(buffer)) != -1)
                        outputStream.write(buffer, 0, count);
                    outputStream.close();
                }
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("FontHandler",
                        "An issue has occurred while attempting to decompress this archive.");
            }
        }

        private void CopyStream(InputStream Input, OutputStream Output) throws IOException {
            byte[] buffer = new byte[5120];
            int length = Input.read(buffer);
            while (length > 0) {
                Output.write(buffer, 0, length);
                length = Input.read(buffer);
            }
        }
    }


    private class FontPreview extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            imageButton.setClickable(false);
            imageButton.setImageTintList(unchecked);
            font_holder.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                Log.d("FontHandler", "Fonts have been loaded on the drawing panel.");

                String work_directory = getContext().getCacheDir().getAbsolutePath() +
                        "/FontCache/font_preview/";

                try {
                    Typeface normal_tf = Typeface.createFromFile(work_directory + "Roboto-Regular" +
                            ".ttf");
                    TextView normal = (TextView) root.findViewById(R.id.text_normal);
                    normal.setTypeface(normal_tf);
                } catch (Exception e) {
                    Log.e("FontHandler", "Could not load font from directory for normal template." +
                            " Maybe it wasn't themed?");
                }

                try {
                    Typeface bold_tf = Typeface.createFromFile(work_directory + "Roboto-Black.ttf");
                    TextView normal_bold = (TextView) root.findViewById(R.id.text_bold);
                    normal_bold.setTypeface(bold_tf);
                } catch (Exception e) {
                    Log.e("FontHandler", "Could not load font from directory for normal-bold " +
                            "template. Maybe it wasn't themed?");
                }

                try {
                    Typeface italics_tf = Typeface.createFromFile(work_directory + "Roboto-Italic" +
                            ".ttf");
                    TextView italics = (TextView) root.findViewById(R.id.text_normal_italics);
                    italics.setTypeface(italics_tf);
                } catch (Exception e) {
                    Log.e("FontHandler", "Could not load font from directory for italic template." +
                            " Maybe it wasn't themed?");
                }

                try {
                    Typeface italics_bold_tf = Typeface.createFromFile(work_directory +
                            "Roboto-BoldItalic.ttf");
                    TextView italics_bold = (TextView) root.findViewById(R.id
                            .text_normal_bold_italics);
                    italics_bold.setTypeface(italics_bold_tf);
                } catch (Exception e) {
                    Log.e("FontHandler", "Could not load font from directory for italic-bold " +
                            "template. Maybe it wasn't themed?");
                }

                eu.chainfire.libsuperuser.Shell.SU.run(
                        "rm -r " + getContext().getCacheDir().getAbsolutePath() +
                                "/FontCache/font_preview/");
                imageButton.setImageTintList(checked);
                imageButton.setClickable(true);
                font_holder.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            } catch (Exception e) {
                Log.e("FontInstaller", "Window was destroyed before AsyncTask could complete " +
                        "postExecute()");
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            try {
                File cacheDirectory = new File(getContext().getCacheDir(), "/FontCache/");
                if (!cacheDirectory.exists()) {
                    boolean created = cacheDirectory.mkdirs();
                    if (created) Log.d("FontHandler", "FontCache folder created");
                }
                File cacheDirectory2 = new File(getContext().getCacheDir(), "/FontCache/" +
                        "font_preview/");
                if (!cacheDirectory2.exists()) {
                    boolean created = cacheDirectory2.mkdirs();
                    if (created) Log.d("FontHandler", "FontCache work folder created");
                } else {
                    eu.chainfire.libsuperuser.Shell.SU.run(
                            "rm -r " + getContext().getCacheDir().getAbsolutePath() +
                                    "/FontCache/font_preview/");
                    boolean created = cacheDirectory2.mkdirs();
                    if (created) Log.d("FontHandler", "FontCache folder recreated");
                }

                // Copy the font.zip from assets/fonts of the theme's assets

                String source = sUrl[0] + ".zip";

                try {
                    Context otherContext = getContext().createPackageContext(theme_pid, 0);
                    AssetManager am = otherContext.getAssets();
                    InputStream inputStream = am.open("fonts/" + source);
                    OutputStream outputStream = new FileOutputStream(getContext().getCacheDir()
                            .getAbsolutePath() + "/FontCache/" + source);
                    CopyStream(inputStream, outputStream);
                } catch (Exception e) {
                    Log.e("FontHandler", "There is no fonts.zip found within the assets " +
                            "of this theme!");
                }

                // Unzip the fonts to get it prepared for the preview
                unzip(getContext().getCacheDir().getAbsolutePath() +
                                "/FontCache/" + source,
                        getContext().getCacheDir().getAbsolutePath() +
                                "/FontCache/font_preview/");
            } catch (Exception e) {
                Log.e("FontHandler", "Unexpectedly lost connection to the application host");
            }
            return null;
        }

        private void unzip(String source, String destination) {
            try {
                ZipInputStream inputStream = new ZipInputStream(
                        new BufferedInputStream(new FileInputStream(source)));
                ZipEntry zipEntry;
                int count;
                byte[] buffer = new byte[8192];
                while ((zipEntry = inputStream.getNextEntry()) != null) {
                    File file = new File(destination, zipEntry.getName());
                    File dir = zipEntry.isDirectory() ? file : file.getParentFile();
                    if (!dir.isDirectory() && !dir.mkdirs())
                        throw new FileNotFoundException("Failed to ensure directory: " +
                                dir.getAbsolutePath());
                    if (zipEntry.isDirectory())
                        continue;
                    FileOutputStream outputStream = new FileOutputStream(file);
                    while ((count = inputStream.read(buffer)) != -1)
                        outputStream.write(buffer, 0, count);
                    outputStream.close();
                }
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("FontHandler",
                        "An issue has occurred while attempting to decompress this archive.");
            }
        }

        private void CopyStream(InputStream Input, OutputStream Output) throws IOException {
            byte[] buffer = new byte[5120];
            int length = Input.read(buffer);
            while (length > 0) {
                Output.write(buffer, 0, length);
                length = Input.read(buffer);
            }
        }
    }
}