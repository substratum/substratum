package projekt.substratum.adapters;

import android.view.View;
import android.widget.TextView;

import com.thesurix.gesturerecycler.GestureViewHolder;

import projekt.substratum.R;

class HeaderViewHolder extends GestureViewHolder {

    TextView mHeaderText;

    HeaderViewHolder(final View view) {
        super(view);
        mHeaderText = (TextView) view.findViewById(R.id.header_text);
    }

    @Override
    public boolean canDrag() {
        return false;
    }

    @Override
    public boolean canSwipe() {
        return false;
    }
}
