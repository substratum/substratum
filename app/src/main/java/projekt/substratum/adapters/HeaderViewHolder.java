package projekt.substratum.adapters;

import android.view.View;
import android.widget.TextView;

import com.thesurix.gesturerecycler.GestureViewHolder;

import butterknife.Bind;
import butterknife.ButterKnife;
import projekt.substratum.R;

class HeaderViewHolder extends GestureViewHolder {

    @Bind(R.id.header_text)
    TextView mHeaderText;

    HeaderViewHolder(final View view) {
        super(view);
        ButterKnife.bind(this, view);
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
