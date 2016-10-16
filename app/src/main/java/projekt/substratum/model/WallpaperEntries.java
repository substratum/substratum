package projekt.substratum.model;

import android.app.Activity;
import android.content.Context;

/**
 * @author Nicholas Chum (nicholaschum)
 */
public class WallpaperEntries {

    private Context mContext;
    private String wallpaperName;
    private String wallpaperLink;
    private String wallpaperPreview;
    private Activity mActivity;

    public Activity getCallingActivity() {
        return mActivity;
    }

    public void setCallingActivity(Activity mActivity) {
        this.mActivity = mActivity;
    }

    public Context getContext() {
        return mContext;
    }

    public void setContext(Context mContext) {
        this.mContext = mContext;
    }

    public String getWallpaperName() {
        return wallpaperName;
    }

    public void setWallpaperName(String wallpaperName) {
        this.wallpaperName = wallpaperName;
    }

    public String getWallpaperLink() {
        return wallpaperLink;
    }

    public void setWallpaperLink(String wallpaperLink) {
        this.wallpaperLink = wallpaperLink;
    }

    public String getWallpaperPreview() {
        return wallpaperPreview;
    }

    public void setWallpaperPreview(String wallpaperPreview) {
        this.wallpaperPreview = wallpaperPreview;
    }
}