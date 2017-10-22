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

public class WallpaperEntries {

    private Context mContext;
    private String wallpaperName;
    private String wallpaperLink;
    private String wallpaperPreview;
    private Activity mActivity;

    Activity getCallingActivity() {
        return this.mActivity;
    }

    public void setCallingActivity(final Activity mActivity) {
        this.mActivity = mActivity;
    }

    public Context getContext() {
        return this.mContext;
    }

    public void setContext(final Context mContext) {
        this.mContext = mContext;
    }

    String getWallpaperName() {
        return this.wallpaperName;
    }

    public void setWallpaperName(final String wallpaperName) {
        this.wallpaperName = wallpaperName;
    }

    String getWallpaperLink() {
        return this.wallpaperLink;
    }

    public void setWallpaperLink(final String wallpaperLink) {
        this.wallpaperLink = wallpaperLink;
    }

    String getWallpaperPreview() {
        return this.wallpaperPreview;
    }

    public void setWallpaperPreview(final String wallpaperPreview) {
        this.wallpaperPreview = wallpaperPreview;
    }
}