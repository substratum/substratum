/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.util.helpers;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;

public class StringUtils {

    public static SpannableStringBuilder format(String toBold,
                                                String trailingElement,
                                                int typefaceType) {
        SpannableStringBuilder stringBuilder =
                new SpannableStringBuilder(toBold + " " + trailingElement);
        stringBuilder.setSpan(
                new StyleSpan(typefaceType),
                0,
                toBold.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        return stringBuilder;
    }
}
