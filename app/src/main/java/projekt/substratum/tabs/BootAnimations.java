package projekt.substratum.tabs;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import projekt.substratum.InformationActivity;
import projekt.substratum.R;
import projekt.substratum.config.BootAnimationManager;
import projekt.substratum.config.FileOperations;
import projekt.substratum.config.References;
import projekt.substratum.util.BootAnimationUtils;
import projekt.substratum.util.Root;

public class BootAnimations extends Fragment {

    private static final int ANIMATION_FRAME_DURATION = 40;
    private String theme_pid;
    private AnimationDrawable animation;
    private ViewGroup root;
    private List<Bitmap> images = new ArrayList<>();
    private ImageView bootAnimationPreview;
    private MaterialProgressBar progressBar;
    private ImageButton imageButton;
    private Spinner bootAnimationSelector;
    private ColorStateList unchecked, checked;
    private TextView vm_blown;
    private RelativeLayout bootanimation_placeholder;
    private RelativeLayout defaults;
    private ProgressDialog mProgressDialog;
    private SharedPreferences prefs;
    private AsyncTask current;
    private NestedScrollView nsv;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        theme_pid = InformationActivity.getThemePID();

        root = (ViewGroup) inflater.inflate(R.layout.tab_fragment_2, container, false);
        nsv = (NestedScrollView) root.findViewById(R.id.nestedScrollView);

        animation = new AnimationDrawable();
        animation.setOneShot(false);

        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        progressBar = (MaterialProgressBar) root.findViewById(R.id.progress_bar_loader);

        vm_blown = (TextView) root.findViewById(R.id.vm_blown);
        bootanimation_placeholder = (RelativeLayout) root.findViewById(
                R.id.bootanimation_placeholder);
        defaults = (RelativeLayout) root.findViewById(R.id.restore_to_default);

        imageButton = (ImageButton) root.findViewById(R.id.checkBox);
        imageButton.setClickable(false);
        imageButton.setOnClickListener(v -> {
            if (References.getDeviceEncryptionStatus(getContext()) <= 1 ||
                    !References.checkOMS(getContext())) {
                if (bootAnimationSelector.getSelectedItemPosition() == 1) {
                    new BootAnimationClearer().execute("");
                } else {
                    new BootAnimationUtils().execute(nsv,
                            bootAnimationSelector.getSelectedItem().toString(),
                            getContext(), theme_pid);
                }
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(R.string.root_required_title)
                        .setMessage(R.string.root_required_boot_animation)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            dialog.dismiss();
                            if (Root.requestRootAccess()) {
                                if (bootAnimationSelector.getSelectedItemPosition() == 1) {
                                    new BootAnimationClearer().execute("");
                                } else {
                                    new BootAnimationUtils().execute(nsv,
                                            bootAnimationSelector.getSelectedItem().toString(),
                                            getContext(), theme_pid);
                                }
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, (dialog, which) ->
                                dialog.cancel())
                        .show();
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
            // Parses the list of items in the boot animation folder
            File f = new File(getContext().getCacheDir().getAbsoluteFile() +
                    "/SubstratumBuilder/" + theme_pid + "/assets/bootanimation");
            File[] fileArray = f.listFiles();
            ArrayList<String> unparsedBootAnimations = new ArrayList<>();
            for (File file : fileArray) {
                unparsedBootAnimations.add(file.getName());
            }

            // Creates the list of dropdown items
            ArrayList<String> parsedBootAnimations = new ArrayList<>();
            parsedBootAnimations.add(getString(R.string.bootanimation_default_spinner));
            parsedBootAnimations.add(getString(R.string.bootanimation_spinner_set_defaults));
            for (int i = 0; i < unparsedBootAnimations.size(); i++) {
                parsedBootAnimations.add(unparsedBootAnimations.get(i).substring(0,
                        unparsedBootAnimations.get(i).length() - 4));
            }

            ArrayAdapter<String> adapter1 = new ArrayAdapter<>(getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, parsedBootAnimations);
            bootAnimationSelector = (Spinner) root.findViewById(R.id.bootAnimationSelection);
            bootAnimationSelector.setAdapter(adapter1);
            bootAnimationSelector.setOnItemSelectedListener(
                    new AdapterView.OnItemSelectedListener() {

                        @Override
                        public void onItemSelected(AdapterView<?> arg0, View arg1,
                                                   int pos, long id) {
                            switch (pos) {
                                case 0:
                                    if (current != null)
                                        current.cancel(true);

                                    bootanimation_placeholder.setVisibility(View.VISIBLE);
                                    defaults.setVisibility(View.GONE);
                                    vm_blown.setVisibility(View.GONE);
                                    imageButton.setClickable(false);
                                    imageButton.setImageTintList(unchecked);
                                    animation = new AnimationDrawable();
                                    animation.setOneShot(false);
                                    bootAnimationPreview =
                                            (ImageView) root.findViewById(R.id
                                                    .bootAnimationPreview);
                                    bootAnimationPreview.setImageDrawable(null);
                                    images.clear();
                                    progressBar.setVisibility(View.GONE);
                                    break;

                                case 1:
                                    if (current != null)
                                        current.cancel(true);

                                    defaults.setVisibility(View.VISIBLE);
                                    bootanimation_placeholder.setVisibility(View.GONE);
                                    vm_blown.setVisibility(View.GONE);
                                    imageButton.setImageTintList(checked);
                                    imageButton.setClickable(true);
                                    progressBar.setVisibility(View.GONE);
                                    animation = new AnimationDrawable();
                                    animation.setOneShot(false);
                                    bootAnimationPreview =
                                            (ImageView) root.findViewById(R.id
                                                    .bootAnimationPreview);
                                    bootAnimationPreview.setImageDrawable(null);
                                    images.clear();
                                    progressBar.setVisibility(View.GONE);
                                    break;

                                default:
                                    if (current != null)
                                        current.cancel(true);

                                    defaults.setVisibility(View.GONE);
                                    bootanimation_placeholder.setVisibility(View.GONE);
                                    String[] commands = {arg0.getSelectedItem().toString()};
                                    current = new BootAnimationPreview().execute(commands);
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> arg0) {
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("BootAnimationUtils",
                    "There is no bootanimation.zip found within the assets of this theme!");
        }
        return root;
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
            editor.remove("bootanimation_applied");
            editor.apply();
            if (getView() != null) {
                Snackbar.make(getView(),
                        getString(R.string.manage_bootanimation_toast),
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
            if (vm_blown.getVisibility() == View.VISIBLE)
                vm_blown.setVisibility(View.GONE);
        }

        @Override
        protected void onPostExecute(String result) {
            if ("true".equals(result))
                vm_blown.setVisibility(View.VISIBLE);
            try {
                Log.d("BootAnimationUtils", "Loaded boot animation contains " + images.size() +
                        " frames.");
                if (bootAnimationSelector.getSelectedItemPosition() > 1) {
                    bootAnimationPreview.setImageDrawable(animation);
                    animation.start();
                }
                FileOperations.delete(getContext(), getContext().getCacheDir().getAbsolutePath() +
                        "/BootAnimationCache/animation_preview/");
                imageButton.setImageTintList(checked);
                imageButton.setClickable(true);
                progressBar.setVisibility(View.GONE);
            } catch (Exception e) {
                Log.e("BootAnimationUtils",
                        "Window was destroyed before AsyncTask could perform postExecute()");
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            try {
                File cacheDirectory = new File(getContext().getCacheDir(), "/BootAnimationCache/");
                if (!cacheDirectory.exists()) {
                    boolean created = cacheDirectory.mkdirs();
                    if (created)
                        Log.d("BootAnimationUtils", "Bootanimation folder created");
                }
                File cacheDirectory2 = new File(getContext().getCacheDir(),
                        "/BootAnimationCache/animation_preview/");
                if (!cacheDirectory2.exists()) {
                    boolean created = cacheDirectory2.mkdirs();
                    if (created)
                        Log.d("BootAnimationUtils", "Bootanimation work folder created");
                } else {
                    FileOperations.delete(getContext(),
                            getContext().getCacheDir().getAbsolutePath() +
                                    "/BootAnimationCache/animation_preview/");
                    boolean created = cacheDirectory2.mkdirs();
                    if (created)
                        Log.d("BootAnimationUtils", "Bootanimation folder recreated");
                }

                // Copy the bootanimation.zip from assets/bootanimation of the theme's assets
                String source = sUrl[0] + ".zip";

                try {
                    File f = new File(getContext().getCacheDir().getAbsoluteFile() +
                            "/SubstratumBuilder/" + theme_pid + "/assets/bootanimation/" + source);
                    try (InputStream inputStream = new FileInputStream(f);
                         OutputStream outputStream =
                                 new FileOutputStream(getContext().getCacheDir().getAbsolutePath() +
                                         "/BootAnimationCache/" + source)) {
                        CopyStream(inputStream, outputStream);
                    }
                } catch (Exception e) {
                    Log.e("BootAnimationUtils",
                            "There is no bootanimation.zip found within the assets of this theme!");
                }

                // Unzip the boot animation to get it prepared for the preview
                unzip(getContext().getCacheDir().getAbsolutePath() +
                                "/BootAnimationCache/" + source,
                        getContext().getCacheDir().getAbsolutePath() +
                                "/BootAnimationCache/animation_preview/");

                // Begin creating the animated drawable
                try {
                    int inSampleSize =
                            previewDeterminator(getContext().getCacheDir().getAbsolutePath() +
                                    "/BootAnimationCache/" + source);
                    Log.d("BootAnimationUtils",
                            "Resampling bootanimation for preview at scale " + inSampleSize);
                    for (int counter = 0; true; counter++) {
                        File current_directory = new File(getContext().getCacheDir(),
                                "/BootAnimationCache/" +
                                        "animation_preview/part" + counter);
                        String directory = getContext().getCacheDir().getAbsolutePath() +
                                "/BootAnimationCache/" +
                                "animation_preview/part" + counter + "/";
                        if (current_directory.exists()) {
                            String[] dirObjects = current_directory.list();
                            for (String string : dirObjects) {
                                BitmapFactory.Options options = new BitmapFactory.Options();
                                options.inSampleSize = inSampleSize;
                                Bitmap bitmap =
                                        BitmapFactory.decodeFile(directory + string, options);
                                images.add(bitmap);
                            }
                        } else {
                            break;
                        }
                    }

                    for (Bitmap image : images) {
                        BitmapDrawable frame = new BitmapDrawable(getResources(), image);
                        animation.addFrame(frame, ANIMATION_FRAME_DURATION);
                    }
                } catch (OutOfMemoryError oome) {
                    Log.e("BootAnimationUtils", "The VM has been blown up and the rendering of " +
                            "this bootanimation has been cancelled.");
                    animation = new AnimationDrawable();
                    animation.setOneShot(false);
                    images.clear();
                    return "true";
                }
            } catch (Exception e) {
                Log.e("BootAnimationUtils",
                        "Unexpectedly lost connection to the application host");
            }
            return null;
        }

        private int previewDeterminator(String file_location) {
            File checkFile = new File(file_location);
            int file_size = Integer.parseInt(String.valueOf(checkFile.length() / 1024 / 1024));
            Log.d("BootAnimationUtils", "Managing bootanimation with size: " + file_size + "MB");

            if (file_size <= 5) {
                return 1;
            } else {
                if (file_size > 5) {
                    if (file_size >= 10) {
                        return 5;
                    } else {
                        return 4;
                    }
                }
            }
            return 1;
        }

        private void unzip(String source, String destination) {
            try (ZipInputStream inputStream = new ZipInputStream(
                    new BufferedInputStream(new FileInputStream(source)))) {
                ZipEntry zipEntry;
                int count;
                byte[] buffer = new byte[8192];
                while ((zipEntry = inputStream.getNextEntry()) != null) {
                    File file = new File(destination, zipEntry.getName());
                    File dir = zipEntry.isDirectory() ? file : file.getParentFile();
                    if (!dir.isDirectory() && !dir.mkdirs())
                        throw new FileNotFoundException(
                                "Failed to ensure directory: " + dir.getAbsolutePath());
                    if (zipEntry.isDirectory())
                        continue;
                    try (FileOutputStream outputStream = new FileOutputStream(file)) {
                        while ((count = inputStream.read(buffer)) != -1)
                            outputStream.write(buffer, 0, count);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("BootAnimationUtils",
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