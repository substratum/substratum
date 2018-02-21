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
    private String packageName;
    private Context context;
    private boolean verified;
    private boolean commons;
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

    public Drawable getDrawable() {
        return this.drawable;
    }

    public void setDrawable(Drawable drawable) {
        this.drawable = drawable;
    }

    boolean getVerification() {
        return this.verified;
    }
}