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

package projekt.substratum.adapters.tabs.sounds;

import android.content.Context;

public class SoundsInfo {
    private final String absolute_path;
    private final Context context;
    private String title;

    public SoundsInfo(Context context,
                      String title,
                      String absolute_path) {
        super();
        this.context = context;
        this.title = title;
        this.absolute_path = absolute_path;
    }

    public String getAbsolutePath() {
        return this.absolute_path;
    }

    public Context getContext() {
        return this.context;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String name) {
        this.title = name;
    }
}