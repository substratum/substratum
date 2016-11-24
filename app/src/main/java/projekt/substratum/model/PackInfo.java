package projekt.substratum.model;

import android.content.Context;
import android.graphics.drawable.Drawable;

public class PackInfo {
    private String package_name;
    private Context mContext;
    private Drawable mDrawable;

    public PackInfo(Context mContext, String package_name) {
        this.mContext = mContext;
        this.package_name = package_name;
    }

    public String getPackageName() {
        return package_name;
    }

    public Context getContext() {
        return mContext;
    }

    public Drawable getDrawable() {
        return mDrawable;
    }

    public void setDrawable(Drawable drawable) {
        this.mDrawable = drawable;
    }
}