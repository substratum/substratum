package projekt.substratum.adapters;

import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.thesurix.gesturerecycler.GestureViewHolder;

import butterknife.Bind;
import butterknife.ButterKnife;
import projekt.substratum.R;


class PrioritiesViewHolder extends GestureViewHolder {

    @Bind(R.id.card_text)
    TextView mCardText;
    @Bind(R.id.card_drag)
    ImageView mItemDrag;
    @Bind(R.id.app_icon)
    ImageView mAppIcon;

    PrioritiesViewHolder(final View view) {
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
}