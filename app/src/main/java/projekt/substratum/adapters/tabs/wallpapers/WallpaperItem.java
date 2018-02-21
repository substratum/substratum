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