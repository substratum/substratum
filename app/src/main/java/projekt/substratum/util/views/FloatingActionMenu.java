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

package projekt.substratum.util.views;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.ScaleAnimation;

import com.gordonwong.materialsheetfab.AnimatedFab;

import projekt.substratum.R;

public class FloatingActionMenu extends FloatingActionButton implements AnimatedFab {

    private static final int FAB_ANIM_DURATION = 200;

    public FloatingActionMenu(final Context context) {
        super(context);
    }

    public FloatingActionMenu(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public FloatingActionMenu(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void show() {
        this.show(0, 0);
    }

    @Override
    public void show(final float translationX, final float translationY) {
        // Set FAB's translation
        this.setTranslation(translationX, translationY);

        // Only use scale animation if FAB is hidden
        if (this.getVisibility() != View.VISIBLE) {
            // Pivots indicate where the animation begins from
            final float pivotX = this.getPivotX() + translationX;
            final float pivotY = this.getPivotY() + translationY;

            final ScaleAnimation anim;
            // If pivots are 0, that means the FAB hasn't been drawn yet so just use the
            // center of the FAB
            if ((pivotX == 0) || (pivotY == 0)) {
                anim = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f);
            } else {
                anim = new ScaleAnimation(0, 1, 0, 1, pivotX, pivotY);
            }

            // Animate FAB expanding
            anim.setDuration(FAB_ANIM_DURATION);
            anim.setInterpolator(this.getInterpolator());
            this.startAnimation(anim);
        }
        this.setVisibility(View.VISIBLE);
    }

    @Override
    public void hide() {
        // Only use scale animation if FAB is visible
        if (this.getVisibility() == View.VISIBLE) {
            // Pivots indicate where the animation begins from
            final float pivotX = this.getPivotX() + this.getTranslationX();
            final float pivotY = this.getPivotY() + this.getTranslationY();

            // Animate FAB shrinking
            final ScaleAnimation anim = new ScaleAnimation(1, 0, 1, 0, pivotX, pivotY);
            anim.setDuration(FAB_ANIM_DURATION);
            anim.setInterpolator(this.getInterpolator());
            this.startAnimation(anim);
        }
        this.setVisibility(View.INVISIBLE);
    }

    private void setTranslation(final float translationX, final float translationY) {
        this.animate().setInterpolator(this.getInterpolator()).setDuration(FAB_ANIM_DURATION)
                .translationX(translationX).translationY(translationY);
    }

    private Interpolator getInterpolator() {
        return AnimationUtils.loadInterpolator(this.getContext(), R.interpolator.msf_interpolator);
    }
}