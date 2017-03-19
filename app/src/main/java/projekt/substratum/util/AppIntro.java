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

package projekt.substratum.util;

import com.stephentuso.welcome.WelcomeScreenBuilder;
import com.stephentuso.welcome.ui.WelcomeActivity;
import com.stephentuso.welcome.util.WelcomeScreenConfiguration;

import projekt.substratum.R;

public class AppIntro extends WelcomeActivity {

    @Override
    protected WelcomeScreenConfiguration configuration() {
        return new WelcomeScreenBuilder(this)
                .theme(R.style.CustomWelcomeScreenTheme)
                .defaultTitleTypefacePath("Montserrat-Bold.ttf")
                .defaultHeaderTypefacePath("Montserrat-Bold.ttf")
                .basicPage(R.drawable.appintro_slide_1, getString(R.string.slide_1_title),
                        getString(R.string.slide_1_text), R.color.slide_1)
                .basicPage(R.drawable.appintro_slide_2, getString(R.string.slide_2_title),
                        getString(R.string.slide_2_text), R.color.slide_2)
                .basicPage(R.drawable.appintro_slide_3, getString(R.string.slide_3_title),
                        getString(R.string.slide_3_text), R.color.slide_3)
                .basicPage(R.drawable.appintro_slide_4, getString(R.string.slide_4_title),
                        getString(R.string.slide_4_text), R.color.slide_4)
                .basicPage(R.drawable.appintro_slide_5, getString(R.string.slide_5_title),
                        getString(R.string.slide_5_text), R.color.slide_5)
                .basicPage(R.drawable.appintro_slide_6, getString(R.string.slide_6_title),
                        getString(R.string.slide_6_text), R.color.slide_6)
                .swipeToDismiss(true)
                .exitAnimation(android.R.anim.fade_out)
                .build();
    }
}