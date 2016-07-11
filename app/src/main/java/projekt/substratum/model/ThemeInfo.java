package projekt.substratum.model;

import android.graphics.drawable.Drawable;

/**
 * @author Nicholas Chum (nicholaschum)
 */
public class ThemeInfo {

    private String themeName;
    private String themeAuthor;
    private String themePackage;
    private Drawable themeDrawable;

    public String getThemeName() {
        return themeName;
    }

    public void setThemeName(String themeName) {
        this.themeName = themeName;
    }

    public String getThemeAuthor() {
        return themeAuthor;
    }

    public void setThemeAuthor(String themeAuthor) {
        this.themeAuthor = themeAuthor;
    }

    public String getThemePackage() {
        return themePackage;
    }

    public void setThemePackage(String themePackage) {
        this.themePackage = themePackage;
    }

    public Drawable getThemeDrawable() {
        return this.themeDrawable;
    }

    public void setThemeDrawable(Drawable drawable) {
        this.themeDrawable = drawable;
    }
}