/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.adapters.tabs.wallpapers;

import android.app.Activity;
import android.content.Context;

public class WallpaperItem {

    private Context context;
    private String wallpaperName;
    private String wallpaperLink;
    private String wallpaperPreview;
    private Activity activity;

    Activity getCallingActivity() {
        return this.activity;
    }

    public void setCallingActivity(Activity mActivity) {
        this.activity = mActivity;
    }

    public Context getContext() {
        return this.context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public String getWallpaperName() {
        return this.wallpaperName;
    }

    public void setWallpaperName(String wallpaperName) {
        this.wallpaperName = wallpaperName;
    }

    String getWallpaperLink() {
        return this.wallpaperLink;
    }

    public void setWallpaperLink(String wallpaperLink) {
        this.wallpaperLink = wallpaperLink;
    }

    public String getWallpaperPreview() {
        return this.wallpaperPreview;
    }

    public void setWallpaperPreview(String wallpaperPreview) {
        this.wallpaperPreview = wallpaperPreview;
    }
}