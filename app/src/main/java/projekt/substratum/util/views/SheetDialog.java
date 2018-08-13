/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.util.views;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class SheetDialog extends BottomSheetDialog {

    public SheetDialog(@NonNull final Context context) {
        super(context);
    }

    /**
     * Overriden onCreate to ensure that the default BottomSheetDialog will not contain a black
     * status bar
     *
     * @param savedInstanceState Saved state
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        if (window == null) return;
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            View v = window.getDecorView();
            int flags = v.getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            v.setSystemUiVisibility(flags);
        }
    }
}