package projekt.substratum.model;

import android.content.Context;

public class ShowcaseItem {

    private Context mContext;
    private String themeName;
    private String themeLink;
    private String themeIcon;
    private String themeBackgroundImage;
    private String themeAuthor;
    private String themePricing;
    private String themeSupport;

    public Context getContext() {
        return mContext;
    }

    public void setContext(Context mContext) {
        this.mContext = mContext;
    }

    public String getThemeAuthor() {
        return themeAuthor;
    }

    public void setThemeAuthor(String themeAuthor) {
        this.themeAuthor = themeAuthor;
    }

    public String getThemePricing() {
        return themePricing;
    }

    public void setThemePricing(String themePricing) {
        this.themePricing = themePricing;
    }

    public String getThemeName() {
        return themeName;
    }

    public void setThemeName(String themeName) {
        this.themeName = themeName;
    }

    public String getThemeLink() {
        return themeLink;
    }

    public void setThemeLink(String themeLink) {
        this.themeLink = themeLink;
    }

    public String getThemeIcon() {
        return themeIcon;
    }

    public void setThemeIcon(String themeIcon) {
        this.themeIcon = themeIcon;
    }

    public String getThemeBackgroundImage() {
        return themeBackgroundImage;
    }

    public void setThemeBackgroundImage(String themeBackgroundImage) {
        this.themeBackgroundImage = themeBackgroundImage;
    }

    public String getThemeSupport() {
        return themeSupport;
    }

    public void setThemeSupport(String themeSupport) {
        this.themeSupport = themeSupport;
    }
}