package projekt.substratum.tabs;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.zeroturnaround.zip.FileSource;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import projekt.substratum.InformationActivityTabs;
import projekt.substratum.R;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class BootAnimation extends Fragment {

    private String theme_pid;
    private AnimationDrawable animation;
    private ViewGroup root;
    private List<Bitmap> images = new ArrayList<>();
    private ImageView bootAnimationPreview;
    private MaterialProgressBar progressBar;
    private ImageButton imageButton;
    private Spinner bootAnimationSelector;
    private ColorStateList unchecked, checked;
    private ProgressDialog progress;
    private boolean has_failed;
    private TextView vm_blown;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {

        theme_pid = InformationActivityTabs.getThemePID();

        root = (ViewGroup) inflater.inflate(R.layout.tab_fragment_3, container, false);

        animation = new AnimationDrawable();
        animation.setOneShot(false);

        progressBar = (MaterialProgressBar) root.findViewById(R.id.progress_bar_loader);

        vm_blown = (TextView) root.findViewById(R.id.vm_blown);

        imageButton = (ImageButton) root.findViewById(R.id.checkBox);
        imageButton.setClickable(false);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new BootAnimationHandler().execute(bootAnimationSelector.getSelectedItem()
                        .toString());
            }
        });

        unchecked = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{
                        getContext().getColor(R.color.bootanimation_unchecked),
                        getContext().getColor(R.color.bootanimation_unchecked)
                }
        );
        checked = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{
                        getContext().getColor(R.color.bootanimation_checked),
                        getContext().getColor(R.color.bootanimation_checked)
                }
        );

        try {
            Context otherContext = getContext().createPackageContext(theme_pid, 0);
            AssetManager am = otherContext.getAssets();
            String[] unparsedBootAnimations = am.list("bootanimation");
            ArrayList<String> parsedBootAnimations = new ArrayList<>();
            for (int i = 0; i < unparsedBootAnimations.length; i++) {
                parsedBootAnimations.add(unparsedBootAnimations[i].substring(0,
                        unparsedBootAnimations[i].length() - 4));
            }
            ArrayAdapter<String> adapter1 = new ArrayAdapter<String>(getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, parsedBootAnimations);
            bootAnimationSelector = (Spinner) root.findViewById(R.id.bootAnimationSelection);
            bootAnimationSelector.setAdapter(adapter1);
            bootAnimationSelector.setOnItemSelectedListener(new AdapterView
                    .OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1,
                                           int pos, long id) {
                    String[] commands = {arg0.getSelectedItem().toString()};
                    new BootAnimationPreview().execute(commands);
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                    String[] commands = {arg0.getSelectedItem().toString()};
                    new BootAnimationPreview().execute(commands);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("BootAnimationHandler", "There is no bootanimation.zip found within the assets " +
                    "of this theme!");
        }

        return root;
    }

    private class BootAnimationHandler extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(getContext(), android.R.style
                    .Theme_DeviceDefault_Dialog_Alert);
            progress.setMessage(getString(R.string.bootanimation_dialog_apply_text));
            progress.setIndeterminate(false);
            progress.show();
        }

        @Override
        protected void onPostExecute(String result) {
            progress.dismiss();
            if (!has_failed) {
                Toast toast = Toast.makeText(getContext(),
                        getString(R.string.bootanimation_dialog_apply_success), Toast.LENGTH_LONG);
                toast.show();
            } else {
                Toast toast = Toast.makeText(getContext(),
                        getString(R.string.bootanimation_dialog_apply_failed), Toast.LENGTH_LONG);
                toast.show();
            }
            eu.chainfire.libsuperuser.Shell.SU.run("mount -o remount,ro /");
            eu.chainfire.libsuperuser.Shell.SU.run("mount -o remount,ro /data");
            eu.chainfire.libsuperuser.Shell.SU.run("mount -o remount,ro /system");
        }

        @Override
        protected String doInBackground(String... sUrl) {

            has_failed = false;

            // Move the file from assets folder to a new working area

            Log.d("BootAnimationHandler", "Copying over the selected boot animation to working " +
                    "directory...");

            File cacheDirectory = new File(getContext().getCacheDir(), "/BootAnimationCache/");
            if (!cacheDirectory.exists()) {
                boolean created = cacheDirectory.mkdirs();
                if (created) Log.d("BootAnimationHandler", "Bootanimation folder created");
            }
            File cacheDirectory2 = new File(getContext().getCacheDir(), "/BootAnimationCache/" +
                    "AnimationCreator/");
            if (!cacheDirectory2.exists()) {
                boolean created = cacheDirectory2.mkdirs();
                if (created) Log.d("BootAnimationHandler", "Bootanimation work folder created");
            } else {
                eu.chainfire.libsuperuser.Shell.SU.run(
                        "rm -r " + getContext().getCacheDir().getAbsolutePath() +
                                "/BootAnimationCache/AnimationCreator/");
                boolean created = cacheDirectory2.mkdirs();
                if (created) Log.d("BootAnimationHandler", "Bootanimation folder recreated");
            }

            String bootanimation = sUrl[0];

            // Now let's take out desc.txt from the theme's assets (bootanimation.zip) and parse it

            if (!has_failed) {
                Log.d("BootAnimationHandler", "Analyzing integrity of boot animation descriptor " +
                        "file...");
                try {
                    Context otherContext = getContext().createPackageContext(theme_pid, 0);
                    AssetManager am = otherContext.getAssets();
                    InputStream inputStream = am.open("bootanimation/" + bootanimation + ".zip");
                    OutputStream outputStream = new FileOutputStream(getContext().getCacheDir()
                            .getAbsolutePath() + "/BootAnimationCache/AnimationCreator/" +
                            bootanimation + ".zip");

                    CopyStream(inputStream, outputStream);
                } catch (Exception e) {
                    Log.e("BootAnimationHandler", "There is no bootanimation.zip found within the" +
                            " " +
                            "assets " +
                            "of this theme!");
                    has_failed = true;
                }

                // Rename the file

                File workingDirectory = new File(getContext().getCacheDir()
                        .getAbsolutePath() + "/BootAnimationCache/AnimationCreator/");
                File from = new File(workingDirectory, bootanimation + ".zip");
                bootanimation = bootanimation.replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9]+",
                        "");
                File to = new File(workingDirectory, bootanimation + ".zip");
                boolean rename = from.renameTo(to);
                if (rename)
                    Log.d("BootAnimationHandler", "Boot Animation successfully moved to new " +
                            "directory");
            }

            if (!has_failed) {
                boolean exists = ZipUtil.containsEntry(new File(getContext().getCacheDir()
                        .getAbsolutePath() + "/BootAnimationCache/AnimationCreator/" +
                        bootanimation + ".zip"), "desc.txt");

                if (exists) {
                    ZipUtil.unpackEntry(new File(getContext().getCacheDir()
                            .getAbsolutePath() + "/BootAnimationCache/AnimationCreator/" +
                            bootanimation + ".zip"), "desc.txt", new File(getContext().getCacheDir()
                            .getAbsolutePath() + "/BootAnimationCache/AnimationCreator/desc.txt"));
                } else {
                    Log.e("BootAnimationHandler", "Could not find specified boot animation " +
                            "descriptor file (desc.txt)!");
                    has_failed = true;
                }
            }

            // Begin parsing of the file (desc.txt) and parse the first line

            if (!has_failed) {
                Log.d("BootAnimationHandler", "Calculating hardware display density metrics and " +
                        "resizing the bootanimation...");
                try {
                    final OutputStream os = new FileOutputStream(getContext().getCacheDir()
                            .getAbsolutePath() + "/BootAnimationCache/AnimationCreator/" +
                            "scaled-" + bootanimation + ".zip");
                    final ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(os));
                    final ZipInputStream bootAni = new ZipInputStream(new BufferedInputStream(new
                            FileInputStream(getContext().getCacheDir()
                            .getAbsolutePath() + "/BootAnimationCache/AnimationCreator/" +
                            bootanimation + ".zip")));
                    ZipEntry ze;

                    zos.setMethod(ZipOutputStream.STORED);
                    final byte[] bytes = new byte[4096];
                    int len;
                    while ((ze = bootAni.getNextEntry()) != null) {
                        ZipEntry entry = new ZipEntry(ze.getName());
                        entry.setMethod(ZipEntry.STORED);
                        entry.setCrc(ze.getCrc());
                        entry.setSize(ze.getSize());
                        entry.setCompressedSize(ze.getSize());
                        if (!ze.getName().equals("desc.txt")) {
                            // just copy this entry straight over into the output zip
                            zos.putNextEntry(entry);
                            while ((len = bootAni.read(bytes)) > 0) {
                                zos.write(bytes, 0, len);
                            }
                        } else {
                            String line;
                            BufferedReader reader = new BufferedReader(new InputStreamReader
                                    (bootAni));
                            final String[] info = reader.readLine().split(" ");

                            int scaledWidth;
                            int scaledHeight;
                            WindowManager wm = (WindowManager) getContext().getSystemService
                                    (Context.WINDOW_SERVICE);
                            DisplayMetrics dm = new DisplayMetrics();
                            wm.getDefaultDisplay().getRealMetrics(dm);
                            // just in case the device is in landscape orientation we will
                            // swap the values since most (if not all) animations are portrait
                            if (dm.widthPixels > dm.heightPixels) {
                                scaledWidth = dm.heightPixels;
                                scaledHeight = dm.widthPixels;
                            } else {
                                scaledWidth = dm.widthPixels;
                                scaledHeight = dm.heightPixels;
                            }

                            int width = Integer.parseInt(info[0]);
                            int height = Integer.parseInt(info[1]);

                            if (width == height)
                                scaledHeight = scaledWidth;
                            else {
                                // adjust scaledHeight to retain original aspect ratio
                                float scale = (float) scaledWidth / (float) width;
                                int newHeight = (int) ((float) height * scale);
                                if (newHeight < scaledHeight)
                                    scaledHeight = newHeight;
                            }

                            CRC32 crc32 = new CRC32();
                            int size = 0;
                            ByteBuffer buffer = ByteBuffer.wrap(bytes);
                            line = String.format("%d %d %s\n", scaledWidth, scaledHeight, info[2]);
                            buffer.put(line.getBytes());
                            size += line.getBytes().length;
                            crc32.update(line.getBytes());
                            while ((line = reader.readLine()) != null) {
                                line = String.format("%s\n", line);
                                buffer.put(line.getBytes());
                                size += line.getBytes().length;
                                crc32.update(line.getBytes());
                            }
                            entry.setCrc(crc32.getValue());
                            entry.setSize(size);
                            entry.setCompressedSize(size);
                            zos.putNextEntry(entry);
                            zos.write(buffer.array(), 0, size);
                        }
                        zos.closeEntry();
                    }
                    zos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("BootAnimationHandler", "The boot animation descriptor file (desc.txt) " +
                            "could not be parsed properly!");
                    has_failed = true;
                }
            }

            if (!has_failed) {
                Log.d("BootAnimationHandler", "Finalizing the boot animation descriptor file and " +
                        "committing changes to the archive...");

                ZipEntrySource[] addedEntries = new ZipEntrySource[]{
                        new FileSource("desc.txt", new File(getContext().getCacheDir()
                                .getAbsolutePath() + "/BootAnimationCache/AnimationCreator/desc" +
                                ".txt"))
                };
                ZipUtil.addOrReplaceEntries(new File(getContext().getCacheDir()
                        .getAbsolutePath() + "/BootAnimationCache/AnimationCreator/" +
                        bootanimation + ".zip"), addedEntries);
            }

            if (!has_failed) {
                Log.d("BootAnimationHandler", "Moving boot animation to theme directory " +
                        "and setting correct contextual parameters...");

                File themeDirectory = new File("/data/system/theme/");
                if (!themeDirectory.exists()) {
                    eu.chainfire.libsuperuser.Shell.SU.run("mount -o remount,rw /data");
                    eu.chainfire.libsuperuser.Shell.SU.run("mkdir /data/system/theme/");
                }

                File scaledBootAnimCheck = new File(getContext().getCacheDir()
                        .getAbsolutePath() + "/BootAnimationCache/AnimationCreator/" + "scaled-"
                        + bootanimation + ".zip");
                if (scaledBootAnimCheck.exists()) {
                    Log.d("BootAnimationHandler", "Scaled boot animation created by Substratum " +
                            "verified!");
                } else {
                    Log.e("BootAnimationHandler", "Scaled boot animation created by Substratum " +
                            "NOT verified!");
                    has_failed = true;
                }

                if (!has_failed) {
                    eu.chainfire.libsuperuser.Shell.SU.run("mount -o remount,rw /system");
                    eu.chainfire.libsuperuser.Shell.SU.run("chmod 755 /data/system/theme/");

                    eu.chainfire.libsuperuser.Shell.SU.run("mount -o remount,rw /system");
                    eu.chainfire.libsuperuser.Shell.SU.run(
                            "mv -f " + getContext().getCacheDir()
                                    .getAbsolutePath() + "/BootAnimationCache/AnimationCreator/"
                                    + "scaled-" + bootanimation + ".zip " +

                                    "/data/system/theme/bootanimation.zip");

                    eu.chainfire.libsuperuser.Shell.SU.run("mount -o remount,rw /system");
                    eu.chainfire.libsuperuser.Shell.SU.run("chmod 644 " +
                            "/data/system/theme/bootanimation.zip");

                    eu.chainfire.libsuperuser.Shell.SU.run("mount -o remount,rw /data");
                    eu.chainfire.libsuperuser.Shell.SU.run("chcon -R u:object_r:system_file:s0 " +
                            "/data/system/theme");
                }
            }

            if (!has_failed) {
                Log.d("BootAnimationHandler", "Boot animation installed!");
                eu.chainfire.libsuperuser.Shell.SU.run(
                        "rm -r " + getContext().getCacheDir().getAbsolutePath() +
                                "/BootAnimationCache/AnimationCreator/");
            } else {
                Log.e("BootAnimationHandler", "Boot animation installation aborted!");
                eu.chainfire.libsuperuser.Shell.SU.run(
                        "rm -r " + getContext().getCacheDir().getAbsolutePath() +
                                "/BootAnimationCache/AnimationCreator/");
            }
            return null;
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


    private class BootAnimationPreview extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            imageButton.setClickable(false);
            imageButton.setImageTintList(unchecked);
            animation = new AnimationDrawable();
            animation.setOneShot(false);
            bootAnimationPreview = (ImageView) root.findViewById(R.id.bootAnimationPreview);
            bootAnimationPreview.setImageDrawable(null);
            images.clear();
            progressBar.setVisibility(View.VISIBLE);
            if (vm_blown.getVisibility() == View.VISIBLE) vm_blown.setVisibility(View.GONE);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == "true") vm_blown.setVisibility(View.VISIBLE);
            try {
                Log.d("BootAnimationHandler", "Loaded boot animation contains " + images.size() +
                        " " +
                        "frames.");
                bootAnimationPreview.setImageDrawable(animation);
                animation.start();
                eu.chainfire.libsuperuser.Shell.SU.run(
                        "rm -r " + getContext().getCacheDir().getAbsolutePath() +
                                "/BootAnimationCache/animation_preview/");
                imageButton.setImageTintList(checked);
                imageButton.setClickable(true);
                progressBar.setVisibility(View.GONE);
            } catch (Exception e) {
                Log.e("BootAnimationHandler", "Window was destroyed before AsyncTask could " +
                        "perform " +
                        "postExecute()");
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            try {
                File cacheDirectory = new File(getContext().getCacheDir(), "/BootAnimationCache/");
                if (!cacheDirectory.exists()) {
                    boolean created = cacheDirectory.mkdirs();
                    if (created) Log.d("BootAnimationHandler", "Bootanimation folder created");
                }
                File cacheDirectory2 = new File(getContext().getCacheDir(), "/BootAnimationCache/" +
                        "animation_preview/");
                if (!cacheDirectory2.exists()) {
                    boolean created = cacheDirectory2.mkdirs();
                    if (created) Log.d("BootAnimationHandler", "Bootanimation work folder created");
                } else {
                    eu.chainfire.libsuperuser.Shell.SU.run(
                            "rm -r " + getContext().getCacheDir().getAbsolutePath() +
                                    "/BootAnimationCache/animation_preview/");
                    boolean created = cacheDirectory2.mkdirs();
                    if (created) Log.d("BootAnimationHandler", "Bootanimation folder recreated");
                }

                // Copy the bootanimation.zip from assets/bootanimation of the theme's assets

                String source = sUrl[0] + ".zip";

                try {
                    Context otherContext = getContext().createPackageContext(theme_pid, 0);
                    AssetManager am = otherContext.getAssets();
                    InputStream inputStream = am.open("bootanimation/" + source);
                    OutputStream outputStream = new FileOutputStream(getContext().getCacheDir()
                            .getAbsolutePath() + "/BootAnimationCache/" + source);
                    CopyStream(inputStream, outputStream);
                } catch (Exception e) {
                    Log.e("BootAnimationHandler", "There is no bootanimation.zip found within the" +
                            " " +
                            "assets " +

                            "of this theme!");
                }

                // Unzip the boot animation to get it prepared for the preview
                unzip(getContext().getCacheDir().getAbsolutePath() +
                                "/BootAnimationCache/" + source,
                        getContext().getCacheDir().getAbsolutePath() +
                                "/BootAnimationCache/animation_preview/");

                // Begin creating the animated drawable
                try {
                    int counter = 0;
                    boolean has_stopped = false;
                    while (!has_stopped) {
                        File current_directory = new File(getContext().getCacheDir(),
                                "/BootAnimationCache/" +
                                        "animation_preview/part" + counter);
                        String directory = getContext().getCacheDir().getAbsolutePath() +
                                "/BootAnimationCache/" +
                                "animation_preview/part" + counter + "/";
                        if (current_directory.exists()) {
                            String[] dirObjects = current_directory.list();
                            for (int j = 0; j < dirObjects.length && images.size() <= 200; j++) {
                                Bitmap bitmap = BitmapFactory.decodeFile(directory + dirObjects[j]);
                                images.add(bitmap);
                            }
                        } else {
                            has_stopped = true;
                        }
                        counter += 1;
                    }

                    int duration = 40;

                    for (Bitmap image : images) {
                        BitmapDrawable frame = new BitmapDrawable(image);
                        animation.addFrame(frame, duration);
                    }
                } catch (OutOfMemoryError oome) {
                    Log.e("BootAnimationHandler", "The VM has been blown up and the rendering of " +
                            "this bootanimation has been cancelled.");
                    animation = new AnimationDrawable();
                    animation.setOneShot(false);
                    images.clear();
                    return "true";
                }
            } catch (Exception e) {
                Log.e("BootAnimationHandler", "Unexpectedly lost connection to the application " +
                        "host");
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
                Log.e("BootAnimationHandler",
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