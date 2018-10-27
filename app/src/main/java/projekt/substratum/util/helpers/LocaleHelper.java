/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.util.helpers;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.LocaleList;
import projekt.substratum.Substratum;

import java.util.Locale;

public class LocaleHelper extends android.content.ContextWrapper {

    private LocaleHelper(Context base) {
        super(base);
    }

    public static LocaleHelper wrap(Context context) {
        Resources resources = context.getResources();
        boolean forceEnglish = Substratum.getPreferences().getBoolean("force_english_locale", false);
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
