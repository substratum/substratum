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

package projekt.substratum.activities.base;

import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.io.File;

public class SubstratumActivity extends AppCompatActivity {

    public static final String CACHE_BUILD_DIR = "SubstratumBuilder";

    public File getBuildDir() {
        return new File(getCacheDir() + "/" + CACHE_BUILD_DIR);
    }

    public String getBuildDirPath() {
        return getBuildDir().getAbsolutePath() + "/";
    }

    public void createToast(String message, int length) {
        Toast.makeText(this, message, length).show();
    }
}
