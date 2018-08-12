/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.util.views;

import com.stephentuso.welcome.BasicPage;
import com.stephentuso.welcome.WelcomeActivity;
import com.stephentuso.welcome.WelcomeConfiguration;
import projekt.substratum.R;

public class AppIntro extends WelcomeActivity {

    @Override
    protected WelcomeConfiguration configuration() {
        WelcomeConfiguration.Builder welcomeBuilder = new WelcomeConfiguration.Builder(this);
        welcomeBuilder.page(
                new BasicPage(
                        R.drawable.appintro_slide_1,
                        getString(R.string.slide_1_title),
                        getString(R.string.slide_1_text)
                ).background(R.color.slide_1));
        welcomeBuilder.page(
                new BasicPage(
                        R.drawable.appintro_slide_2,
                        getString(R.string.slide_2_title),
                        getString(R.string.slide_2_text)
                ).background(R.color.slide_2));
        welcomeBuilder.page(
                new BasicPage(
                        R.drawable.appintro_slide_3,
                        getString(R.string.slide_3_title),
                        getString(R.string.slide_3_text)
                ).background(R.color.slide_3));
        welcomeBuilder.page(
                new BasicPage(
                        R.drawable.appintro_slide_4,
                        getString(R.string.slide_4_title),
                        getString(R.string.slide_4_text)
                ).background(R.color.slide_4));
        welcomeBuilder.page(
                new BasicPage(
                        R.drawable.appintro_slide_5,
                        getString(R.string.slide_5_title),
                        getString(R.string.slide_5_text)
                ).background(R.color.slide_5));
        welcomeBuilder.page(
                new BasicPage(
                        R.drawable.appintro_slide_6,
                        getString(R.string.slide_6_title),
                        getString(R.string.slide_6_text)
                ).background(R.color.slide_6));
        welcomeBuilder.swipeToDismiss(true);
        welcomeBuilder.exitAnimation(android.R.anim.fade_out);
        return welcomeBuilder.build();
    }
}