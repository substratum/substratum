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

package projekt.substratum.adapters.tabs.wallpapers;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.PowerManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import projekt.substratum.R;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;
import static com.bumptech.glide.request.RequestOptions.centerCropTransform;

public class WallpaperAdapter extends RecyclerView.Adapter<WallpaperAdapter.ViewHolder> {
    private ProgressDialog mProgressDialog;
    private ArrayList<WallpaperEntries> information;
    private Context mContext;
    private PowerManager.WakeLock mWakeLock;

    public WallpaperAdapter(ArrayList<WallpaperEntries> information) {
        this.information = information;
    }

    @Override
    public WallpaperAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(
                viewGroup.getContext()).inflate(R.layout.wallpaper_entry_card, viewGroup, false);
        return new ViewHolder(view);
    }

    private void setAnimation(Context context, View view) {
        Animation animation = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
        view.startAnimation(animation);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int pos) {
        WallpaperEntries wallpaperEntry = information.get(pos);
        mContext = wallpaperEntry.getContext();

        Glide.with(mContext)
                .load(wallpaperEntry.getWallpaperPreview())
                .apply(centerCropTransform())
                .transition(withCrossFade())
                .into(viewHolder.imageView);

        viewHolder.wallpaperName.setText(wallpaperEntry.getWallpaperName());

        viewHolder.cardView.setOnClickListener(view -> {

            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                    mContext, R.layout.wallpaper_dialog_listview);
            arrayAdapter.add(mContext.getString(R.string.wallpaper_dialog_wallpaper));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                arrayAdapter.add(mContext.getString(R.string.wallpaper_dialog_lockscreen));
                arrayAdapter.add(mContext.getString(R.string.wallpaper_dialog_wallpaper_both));
            }
            builder.setCancelable(false);
            builder.setNegativeButton(
                    android.R.string.cancel,
                    (dialog, which) -> dialog.dismiss());
            builder.setAdapter(arrayAdapter, (dialog, which) -> {
                switch (which) {
                    case 0:
                        dialog.cancel();

                        // Find out the extension of the image
                        String extension;
                        if (wallpaperEntry.getWallpaperLink().endsWith(".png")) {
                            extension = ".png";
                        } else {
                            extension = ".jpg";
                        }

                        // Download the image
                        new downloadWallpaper(this).execute(
                                wallpaperEntry.getWallpaperLink(),
                                "homescreen_wallpaper" + extension);

                        // Crop the image, and send the request back to InformationActivity
                        CropImage.activity(Uri.fromFile(new File(
                                mContext.getCacheDir().getAbsolutePath() +
                                        "/" + "homescreen_wallpaper" + extension)))
                                .setGuidelines(CropImageView.Guidelines.ON)
                                .setFixAspectRatio(false)
                                .setInitialCropWindowPaddingRatio(0)
                                .setActivityTitle(wallpaperEntry.getWallpaperName())
                                .setOutputUri(Uri.fromFile(new File(
                                        mContext.getCacheDir().getAbsolutePath() +
                                                "/" + "homescreen_wallpaper" + extension)))
                                .start(wallpaperEntry.getCallingActivity());
                        break;
                    case 1:
                        dialog.cancel();

                        // Find out the extension of the image
                        String extension2;
                        if (wallpaperEntry.getWallpaperLink().endsWith(".png")) {
                            extension2 = ".png";
                        } else {
                            extension2 = ".jpg";
                        }

                        // Download the image
                        new downloadWallpaper(this).execute(
                                wallpaperEntry.getWallpaperLink(),
                                "lockscreen_wallpaper" + extension2);

                        // Crop the image, and send the request back to
                        // InformationActivity
                        CropImage.activity(Uri.fromFile(new File(
                                mContext.getCacheDir().getAbsolutePath() +
                                        "/" + "lockscreen_wallpaper" + extension2)))
                                .setGuidelines(CropImageView.Guidelines.ON)
                                .setFixAspectRatio(false)
                                .setInitialCropWindowPaddingRatio(0)
                                .setActivityTitle(wallpaperEntry.getWallpaperName())
                                .setOutputUri(Uri.fromFile(new File(
                                        mContext.getCacheDir().getAbsolutePath() +
                                                "/" + "lockscreen_wallpaper" + extension2)))
                                .start(wallpaperEntry.getCallingActivity());
                        break;
                    case 2:
                        dialog.cancel();

                        // Find out the extension of the image
                        String extension3;
                        if (wallpaperEntry.getWallpaperLink().endsWith(".png")) {
                            extension3 = ".png";
                        } else {
                            extension3 = ".jpg";
                        }

                        // Download the image
                        new downloadWallpaper(this).execute(
                                wallpaperEntry.getWallpaperLink(),
                                "all_wallpaper" + extension3);

                        // Crop the image, and send the request back to
                        // InformationActivity
                        CropImage.activity(Uri.fromFile(new File(
                                mContext.getCacheDir().getAbsolutePath() +
                                        "/" + "all_wallpaper" + extension3)))
                                .setGuidelines(CropImageView.Guidelines.ON)
                                .setFixAspectRatio(false)
                                .setInitialCropWindowPaddingRatio(0)
                                .setActivityTitle(wallpaperEntry.getWallpaperName())
                                .setOutputUri(Uri.fromFile(new File(
                                        mContext.getCacheDir().getAbsolutePath() +
                                                "/" + "all_wallpaper" +
                                                extension3)))
                                .start(wallpaperEntry.getCallingActivity());
                        break;
                }
            });
            builder.show();
        });
        setAnimation(mContext, viewHolder.itemView);
    }

    @Override
    public int getItemCount() {
        return information.size();
    }

    private static class downloadWallpaper extends AsyncTask<String, Integer, String> {

        private WeakReference<WallpaperAdapter> ref;

        downloadWallpaper(WallpaperAdapter wallpaperAdapter) {
            ref = new WeakReference<>(wallpaperAdapter);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            WallpaperAdapter wallpaperAdapter = ref.get();
            if (wallpaperAdapter != null) {
                // Instantiate Progress Dialog
                wallpaperAdapter.mProgressDialog = new ProgressDialog(wallpaperAdapter.mContext);
                wallpaperAdapter.mProgressDialog.setMessage(
                        wallpaperAdapter.mContext.getString(R.string.wallpaper_downloading));
                wallpaperAdapter.mProgressDialog.setIndeterminate(false);
                wallpaperAdapter.mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                wallpaperAdapter.mProgressDialog.setCancelable(false);

                // Take CPU lock to prevent CPU from going off if the user
                // presses the power button during download
                PowerManager pm = (PowerManager)
                        wallpaperAdapter.mContext.getSystemService(Context.POWER_SERVICE);
                if (pm != null) {
                    wallpaperAdapter.mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                            getClass().getName());
                }
                wallpaperAdapter.mWakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
                wallpaperAdapter.mProgressDialog.show();
            }
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            WallpaperAdapter wallpaperAdapter = ref.get();
            if (wallpaperAdapter != null) {
                wallpaperAdapter.mProgressDialog.setIndeterminate(false);
                wallpaperAdapter.mProgressDialog.setMax(100);
                wallpaperAdapter.mProgressDialog.setProgress(progress[0]);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            WallpaperAdapter wallpaperAdapter = ref.get();
            if (wallpaperAdapter != null) {
                wallpaperAdapter.mWakeLock.release();
                wallpaperAdapter.mProgressDialog.dismiss();
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            WallpaperAdapter wallpaperAdapter = ref.get();
            if (wallpaperAdapter != null) {

                InputStream input = null;
                OutputStream output = null;
                HttpURLConnection connection = null;

                try {
                    URL url = new URL(sUrl[0]);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.connect();

                    // expect HTTP 200 OK, so we don't mistakenly save error report
                    // instead of the file
                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        return "Server returned HTTP " + connection.getResponseCode()
                                + " " + connection.getResponseMessage();
                    }

                    // this will be useful to display download percentage
                    // might be -1: server did not report the length
                    int fileLength = connection.getContentLength();

                    // download the file
                    input = connection.getInputStream();

                    output = new FileOutputStream(
                            wallpaperAdapter.mContext.getCacheDir().getAbsolutePath() +
                                    "/" + sUrl[1]);

                    byte data[] = new byte[4096];
                    long total = 0;
                    int count;
                    while ((count = input.read(data)) != -1) {
                        // allow canceling with back button
                        if (isCancelled()) {
                            input.close();
                            return null;
                        }
                        total += count;
                        // publishing the progress....
                        if (fileLength > 0) // only if total length is known
                            publishProgress((int) (total * 100 / fileLength));
                        output.write(data, 0, count);
                    }
                } catch (Exception e) {
                    return e.toString();
                } finally {
                    try {
                        if (output != null)
                            output.close();
                        if (input != null)
                            input.close();
                    } catch (IOException ignored) {
                    }

                    if (connection != null)
                        connection.disconnect();
                }
            }
            return null;
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView wallpaperName;
        ImageView imageView;

        ViewHolder(View view) {
            super(view);
            cardView = view.findViewById(R.id.wallpaperCard);
            imageView = view.findViewById(R.id.wallpaperImage);
            wallpaperName = view.findViewById(R.id.wallpaperName);
        }
    }
}