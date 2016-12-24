package projekt.substratum.adapters;

import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.thesurix.gesturerecycler.GestureViewHolder;

import projekt.substratum.R;


class PrioritiesViewHolder extends GestureViewHolder {

    TextView mCardText;
    ImageView mItemDrag;
    ImageView mAppIcon;

    PrioritiesViewHolder(final View view) {
        super(view);
        mCardText = (TextView) view.findViewById(R.id.card_text);
        mItemDrag = (ImageView) view.findViewById(R.id.card_drag);
        mAppIcon = (ImageView) view.findViewById(R.id.app_icon);
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