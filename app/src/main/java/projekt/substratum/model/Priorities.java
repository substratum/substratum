package projekt.substratum.model;

import android.graphics.drawable.Drawable;

public class Priorities implements PrioritiesItem {

    private String mName;
    private Drawable mDrawableId;

    public Priorities(final String name, Drawable drawable) {
        mName = name;
        mDrawableId = drawable;
    }

    @Override
    public PrioritiesItemType getType() {
        return PrioritiesItemType.CONTENT;
    }

    public String getName() {
        return mName;
    }

    public Drawable getDrawableId() {
        return mDrawableId;
    }
}