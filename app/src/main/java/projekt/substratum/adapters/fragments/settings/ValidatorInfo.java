/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.adapters.fragments.settings;

import android.content.Context;
import android.graphics.drawable.Drawable;

public class ValidatorInfo {
    private final String packageName;
    private final Context context;
    private final boolean verified;
    private final boolean commons;
    private Drawable drawable;
    private ValidatorError validatorError;

    public ValidatorInfo(Context context,
                         String packageName,
                         boolean verified,
                         boolean commons) {
        super();
        this.context = context;
        this.packageName = packageName;
        this.verified = verified;
        this.commons = commons;
    }

    boolean getCommons() {
        return this.commons;
    }

    ValidatorError getPackageError() {
        return this.validatorError;
    }

    public void setPackageError(ValidatorError validatorError) {
        this.validatorError = validatorError;
    }

    public String getPackageName() {
        return this.packageName;
    }

    public Context getContext() {
        return this.context;
    }

    boolean getVerification() {
        return this.verified;
    }
}