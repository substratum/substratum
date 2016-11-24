package projekt.substratum.model;

import android.content.Context;

public class IconInfo {
    private String package_name, package_drawable, package_theme;
    private String mPackageName;
    private Context mContext;
    private byte[] mDrawable;

    public IconInfo(Context mContext, String package_name,
                    String package_drawable, String package_theme) {
        this.mContext = mContext;
        this.package_name = package_name;
        this.package_drawable = package_drawable;
        this.package_theme = package_theme;
    }

    public String getPackageName() {
        return package_name;
    }

    public Context getContext() {
        return mContext;
    }

    public String getPackageDrawable() {
        return package_drawable;
    }

    public String getThemePackage() {
        return package_theme;
    }

    public byte[] getDrawable() {
        return mDrawable;
    }

    public void setDrawable(byte[] drawable) {
        this.mDrawable = drawable;
    }

    public String getParsedName() {
        return mPackageName;
    }

    public void setParsedName(String parsedName) {
        this.mPackageName = parsedName;
    }
}