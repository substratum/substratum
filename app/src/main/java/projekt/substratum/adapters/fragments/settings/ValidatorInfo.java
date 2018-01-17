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

package projekt.substratum.adapters.fragments.settings;

import android.content.Context;
import android.graphics.drawable.Drawable;

public class ValidatorInfo {
    private final String package_name;
    private final Context context;
    private final Boolean mVerified;
    private final Boolean mCommons;
    private Drawable mDrawable;
    private ValidatorError mValidatorError;

    public ValidatorInfo(Context context,
                         String package_name,
                         Boolean verified,
                         Boolean commons) {
        super();
        this.context = context;
        this.package_name = package_name;
        this.mVerified = verified;
        this.mCommons = commons;
    }

    public Boolean getCommons() {
        return this.mCommons;
    }

    ValidatorError getPackageError() {
        return this.mValidatorError;
    }

    public void setPackageError(ValidatorError validatorError) {
        this.mValidatorError = validatorError;
    }

    public String getPackageName() {
        return this.package_name;
    }

    public Context getContext() {
        return this.context;
    }

    public Drawable getDrawable() {
        return this.mDrawable;
    }

    public void setDrawable(Drawable drawable) {
        this.mDrawable = drawable;
    }

    Boolean getVerification() {
        return this.mVerified;
    }
}