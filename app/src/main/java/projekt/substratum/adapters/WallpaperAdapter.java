package projekt.substratum.adapters;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import projekt.substratum.R;
import projekt.substratum.model.WallpaperEntries;

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
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout
                        .wallpaper_entry_card,
                viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int pos) {
        final int i = pos;

        mContext = information.get(i).getContext();

        Glide.with(information.get(i).getContext())
                .load(information.get(i).getWallpaperPreview())
                .centerCrop()
                .crossFade()
                .into(viewHolder.imageView);

        viewHolder.wallpaperName.setText(information.get(i).getWallpaperName());

        viewHolder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AlertDialog.Builder builder = new AlertDialog.Builder(
                        information.get(i).getContext());
                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(information.get(i)
                        .getContext(),
                        R.layout.dialog_listview);
                arrayAdapter.add(information.get(i).getContext().getString(R.string
                        .wallpaper_dialog_wallpaper));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    arrayAdapter.add(information.get(i).getContext().getString(R.string
                            .wallpaper_dialog_lockscreen));
                    arrayAdapter.add(information.get(i).getContext().getString(R.string
                            .wallpaper_dialog_wallpaper_both));
                }
                builder.setCancelable(false);
                builder.setNegativeButton(
                        android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                dialog.cancel();

                                // Find out the extension of the image
                                String extension;
                                if (information.get(i).getWallpaperLink().endsWith(".png")) {
                                    extension = ".png";
                                } else {
                                    extension = ".jpg";
                                }

                                // Download the image
                                new downloadWallpaper().execute(
                                        information.get(i).getWallpaperLink(),
                                        "homescreen_wallpaper" + extension);

                                // Crop the image, and send the request back to InformationActivity
                                CropImage.activity(Uri.fromFile(new File(
                                        mContext.getCacheDir().getAbsolutePath() +
                                                "/" + "homescreen_wallpaper" + extension)))
                                        .setGuidelines(CropImageView.Guidelines.ON)
                                        .setActivityTitle(information.get(i).getWallpaperName())
                                        .setOutputUri(Uri.fromFile(new File(
                                                mContext.getCacheDir().getAbsolutePath() +
                                                        "/" + "homescreen_wallpaper" + extension)))
                                        .start(information.get(i).getCallingActivity());
                                break;
                            case 1:
                                dialog.cancel();

                                // Find out the extension of the image
                                String extension2;
                                if (information.get(i).getWallpaperLink().endsWith(".png")) {
                                    extension2 = ".png";
                                } else {
                                    extension2 = ".jpg";
                                }

                                // Download the image
                                new downloadWallpaper().execute(
                                        information.get(i).getWallpaperLink(),
                                        "lockscreen_wallpaper" + extension2);

                                // Crop the image, and send the request back to
                                // InformationActivity
                                CropImage.activity(Uri.fromFile(new File(
                                        mContext.getCacheDir().getAbsolutePath() +
                                                "/" + "lockscreen_wallpaper" + extension2)))
                                        .setGuidelines(CropImageView.Guidelines.ON)
                                        .setActivityTitle(information.get(i).getWallpaperName())
                                        .setOutputUri(Uri.fromFile(new File(
                                                mContext.getCacheDir().getAbsolutePath() +
                                                        "/" + "lockscreen_wallpaper" +
                                                        extension2)))
                                        .start(information.get(i).getCallingActivity());
                                break;
                            case 2:
                                dialog.cancel();

                                // Find out the extension of the image
                                String extension3;
                                if (information.get(i).getWallpaperLink().endsWith(".png")) {
                                    extension3 = ".png";
                                } else {
                                    extension3 = ".jpg";
                                }

                                // Download the image
                                new downloadWallpaper().execute(
                                        information.get(i).getWallpaperLink(),
                                        "all_wallpaper" + extension3);

                                // Crop the image, and send the request back to
                                // InformationActivity
                                CropImage.activity(Uri.fromFile(new File(
                                        mContext.getCacheDir().getAbsolutePath() +
                                                "/" + "all_wallpaper" + extension3)))
                                        .setGuidelines(CropImageView.Guidelines.ON)
                                        .setActivityTitle(information.get(i).getWallpaperName())
                                        .setOutputUri(Uri.fromFile(new File(
                                                mContext.getCacheDir().getAbsolutePath() +
                                                        "/" + "all_wallpaper" +
                                                        extension3)))
                                        .start(information.get(i).getCallingActivity());
                                break;
                        }
                    }
                });
                builder.show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return information.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView wallpaperName;
        ImageView imageView;

        ViewHolder(View view) {
            super(view);
            cardView = (CardView) view.findViewById(R.id.wallpaperCard);
            imageView = (ImageView) view.findViewById(R.id.wallpaperImage);
            wallpaperName = (TextView) view.findViewById(R.id.wallpaperName);
        }
    }

    private class downloadWallpaper extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // Instantiate Progress Dialog
            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setMessage(mContext.getString(R.string.wallpaper_downloading));
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setCancelable(false);

            // Take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager)
                    mContext.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            mWakeLock.release();
            mProgressDialog.dismiss();
        }

        @Override
        protected String doInBackground(String... sUrl) {
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
                        mContext.getCacheDir().getAbsolutePath() + "/" + sUrl[1]);

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
            return null;
        }
    }
}