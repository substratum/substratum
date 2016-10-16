package projekt.substratum.easteregg;

import android.content.Context;
import android.util.AttributeSet;

import projekt.substratum.R;

public class BubbaLand extends LLand {
    public static final String TAG = "BubbaLand";

    public BubbaLand(Context context) {
        super(context, null);
    }

    public BubbaLand(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public BubbaLand(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected int getEggPlayer() {
        return R.drawable.bubba;
    }

    @Override
    protected int getEggPlayerColor() {
        return 0x00000000;
    }
}
