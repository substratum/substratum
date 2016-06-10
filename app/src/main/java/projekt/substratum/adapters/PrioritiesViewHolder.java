package projekt.substratum.adapters;

import android.animation.ValueAnimator;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.thesurix.gesturerecycler.GestureViewHolder;

import butterknife.Bind;
import butterknife.ButterKnife;
import projekt.substratum.R;


public class PrioritiesViewHolder extends GestureViewHolder {

    @Bind(R.id.month_text)
    TextView mMonthText;
    @Bind(R.id.month_drag)
    ImageView mItemDrag;
    @Bind(R.id.app_icon)
    ImageView mAppIcon;

    public PrioritiesViewHolder(final View view) {
        super(view);
        ButterKnife.bind(this, view);
    }

    @Nullable
    @Override
    public View getDraggableView() {
        return mItemDrag;
    }

    @Override
    public boolean canDrag() {
        return true;
    }

    @Override
    public boolean canSwipe() {
        return true;
    }

    private ValueAnimator.AnimatorUpdateListener getBackgroundAnimatorListener(final TextView
                                                                                       view,
                                                                               final
                                                                               ValueAnimator
                                                                                       animator) {
        return new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(final ValueAnimator valueAnimator) {
                view.setBackgroundColor((int) animator.getAnimatedValue());
            }
        };
    }

    private ValueAnimator.AnimatorUpdateListener getTextAnimatorListener(final TextView view,
                                                                         final ValueAnimator
                                                                                 animator) {
        return new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(final ValueAnimator valueAnimator) {
                view.setTextColor((int) animator.getAnimatedValue());
            }
        };
    }
}
