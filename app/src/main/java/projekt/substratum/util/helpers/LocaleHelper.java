/*
 * Copyright (c) 2018 Projekt Substratum
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

package projekt.substratum.util.helpers;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.LocaleList;
import android.preference.PreferenceManager;

import java.util.Locale;

public class LocaleHelper extends android.content.ContextWrapper {

    public LocaleHelper(Context base) {
        super(base);
    }

    public static LocaleHelper wrap(Context context) {
        Resources resources = context.getResources();
        boolean forceEnglish =
                PreferenceManager.getDefaultSharedPreferences(context)
                        .getBoolean("force_english_locale", false);
        Configuration configuration = resources.getConfiguration();

        Locale chosenLocale = forceEnglish ? Locale.US : Locale.getDefault();

        configuration.setLocale(chosenLocale);
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

        LocaleList localeList = new LocaleList(chosenLocale);
        LocaleList.setDefault(localeList);
        configuration.setLocales(localeList);

        context = context.createConfigurationContext(configuration);

        return new LocaleHelper(context);
    }
}
