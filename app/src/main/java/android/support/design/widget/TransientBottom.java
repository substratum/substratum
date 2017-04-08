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

package android.support.design.widget;

import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;

public abstract class TransientBottom<F extends TransientBottom>
        extends BaseTransientBottomBar {
    /**
     * Constructor for the transient bottom bar.
     *
     * @param parent              The parent for this transient bottom bar.
     * @param content             The content view for this transient bottom bar.
     * @param contentViewCallback The content view callback for this transient bottom bar.
     */
    TransientBottom(@NonNull ViewGroup parent,
                    @NonNull View content,
                    @NonNull ContentViewCallback contentViewCallback) {
        super(parent, content, contentViewCallback);
    }

    @Override
    boolean shouldAnimate() {
        // Force animation!
        return true;
    }
}