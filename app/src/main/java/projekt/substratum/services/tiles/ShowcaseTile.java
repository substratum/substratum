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

package projekt.substratum.services.tiles;

import android.content.Intent;
import android.service.quicksettings.TileService;

import projekt.substratum.activities.showcase.ShowcaseActivity;


public class ShowcaseTile extends TileService {
    @Override
    public void onClick() {
        Intent collapseIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        Intent intent = new Intent(this, ShowcaseActivity.class);
        sendBroadcast(collapseIntent);
        startActivity(intent);
    }
}