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

import android.app.Activity;
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
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;

import projekt.substratum.R;
import projekt.substratum.common.References;
import projekt.substratum.util.files.FileDownloader;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;
import static com.bumptech.glide.request.RequestOptions.centerCropTransform;

public class WallpaperAdapter extends RecyclerView.Adapter<WallpaperAdapter.ViewHolder> {
    private List<WallpaperEntries> information;
    private ProgressDialog mProgressDialog;
    private Context mContext;
    private PowerManager.WakeLock mWakeLock;
    private AsyncTask current_download;

    public WallpaperAdapter(List<WallpaperEntries> information) {
        super();
        this.information = information;
    }

    @Override
    public WallpaperAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup,
                                                          int i) {
        View view = LayoutInflater.from(
                viewGroup.getContext()).inflate(R.layout.wallpaper_entry_card, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder,
                                 int pos) {
        WallpaperEntries wallpaperEntry = this.information.get(pos);
        this.mContext = wallpaperEntry.getContext();

        Glide.with(this.mContext)
                .load(wallpaperEntry.getWallpaperPreview())
                .apply(centerCropTransform())
                .transition(withCrossFade())
                .into(viewHolder.imageView);

        viewHolder.wallpaperName.setText(wallpaperEntry.getWallpaperName());
        viewHolder.cardView.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this.mContext);
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                    this.mContext, R.layout.wallpaper_dialog_listview);
            arrayAdapter.add(this.mContext.getString(R.string.wallpaper_dialog_wallpaper));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                arrayAdapter.add(this.mContext.getString(R.string.wallpaper_dialog_lockscreen));
                arrayAdapter.add(this.mContext.getString(R.string.wallpaper_dialog_wallpaper_both));
            }
            builder.setCancelable(false);
            builder.setNegativeButton(
                    android.R.string.cancel,
                    (dialog, which) -> dialog.dismiss());
            builder.setAdapter(arrayAdapter, (dialog, which) -> {
                String mode = "homescreen_wallpaper";
                switch (which) {
                    case 0:
                    case 1:
                        if (which == 1) mode = "lockscreen_wallpaper";
                    case 2:
                        if (which == 2) mode = "all_wallpaper";
                        dialog.cancel();

                        // Download the image
                        this.current_download = new downloadWallpaper(
                                this,
                                wallpaperEntry.getCallingActivity()
                        ).execute(
                                wallpaperEntry.getWallpaperLink(),
                                mode,
                                wallpaperEntry.getWallpaperName());
                        break;
                }
            });
            builder.show();
        });
        References.setRecyclerViewAnimation(
                this.mContext,
                viewHolder.itemView,
                android.R.anim.fade_in);
    }

    @Override
    public int getItemCount() {
        return this.information.size();
    }

    private static class downloadWallpaper extends AsyncTask<String, Integer, String> {

        private WeakReference<WallpaperAdapter> ref;
        private WeakReference<Activity> activity;
        private String wallpaperLink;
        private String extension;
        private String directory_output;
        private String wallpaperName;

        downloadWallpaper(WallpaperAdapter wallpaperAdapter,
                          Activity callingActivity) {
            super();
            this.ref = new WeakReference<>(wallpaperAdapter);
            this.activity = new WeakReference<>(callingActivity);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            WallpaperAdapter wallpaperAdapter = this.ref.get();
            if (wallpaperAdapter != null) {
                // Instantiate Progress Dialog
                wallpaperAdapter.mProgressDialog = new ProgressDialog(wallpaperAdapter.mContext);
                wallpaperAdapter.mProgressDialog.setMessage(
                        wallpaperAdapter.mContext.getString(R.string.wallpaper_downloading));
                wallpaperAdapter.mProgressDialog.setIndeterminate(false);
                wallpaperAdapter.mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

                // Take CPU lock to prevent CPU from going off if the user
                // presses the power button during download
                PowerManager pm = (PowerManager)
                        wallpaperAdapter.mContext.getSystemService(Context.POWER_SERVICE);
                if (pm != null) {
                    wallpaperAdapter.mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                            this.getClass().getName());
                }
                wallpaperAdapter.mWakeLock.acquire(10L * 60L * 1000L /*10 minutes*/);
                wallpaperAdapter.mProgressDialog.setOnCancelListener(
                        dialogInterface -> wallpaperAdapter.current_download.cancel(true));
                wallpaperAdapter.mProgressDialog.show();
            }
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            WallpaperAdapter wallpaperAdapter = this.ref.get();
            if (wallpaperAdapter != null) {
                wallpaperAdapter.mProgressDialog.setIndeterminate(false);
                wallpaperAdapter.mProgressDialog.setMax(100);
                wallpaperAdapter.mProgressDialog.setProgress(progress[0]);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            WallpaperAdapter wallpaperAdapter = this.ref.get();
            if ((wallpaperAdapter != null) && (this.activity != null)) {
                wallpaperAdapter.mWakeLock.release();
                wallpaperAdapter.mProgressDialog.dismiss();

                // Crop the image, and send the request back to
                // InformationActivity
                CropImage.activity(Uri.fromFile(new File(
                        wallpaperAdapter.mContext.getCacheDir().getAbsolutePath() +
                                '/' + this.directory_output)))
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setFixAspectRatio(false)
                        .setInitialCropWindowPaddingRatio((float) 0)
                        .setActivityTitle(this.wallpaperName)
                        .setOutputUri(Uri.fromFile(new File(
                                wallpaperAdapter.mContext.getCacheDir().getAbsolutePath() +
                                        '/' + this.directory_output)))
                        .start(this.activity.get());
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            WallpaperAdapter wallpaperAdapter = this.ref.get();
            if (wallpaperAdapter != null) {
                this.wallpaperLink = sUrl[0];
                if (this.wallpaperLink.endsWith(".png")) {
                    this.extension = ".png";
                } else {
                    this.extension = ".jpg";
                }
                this.directory_output = sUrl[1] + this.extension;
                this.wallpaperName = sUrl[2];

                FileDownloader.init(wallpaperAdapter.mContext,
                        this.wallpaperLink,
                        "",
                        this.directory_output
                );
            }
            return null;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView wallpaperName;
        ImageView imageView;

        ViewHolder(View view) {
            super(view);
            this.cardView = view.findViewById(R.id.wallpaperCard);
            this.imageView = view.findViewById(R.id.wallpaperImage);
            this.wallpaperName = view.findViewById(R.id.wallpaperName);
        }
    }
}