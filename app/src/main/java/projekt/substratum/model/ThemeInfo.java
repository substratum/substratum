package projekt.substratum.model;

import android.content.Context;
import android.graphics.drawable.Drawable;

/**
 * @author Nicholas Chum (nicholaschum)
 */
public class ThemeInfo {

    private String themeName;
    private String themeAuthor;
    private String themePackage;
    private String themeVersion;
    private String sdkLevels;
    private String pluginVersion;
    private Drawable themeDrawable;
    private Context themeContext;

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

    public String getPluginVersion() {
        return pluginVersion;
    }

    public void setPluginVersion(String pluginVersion) {
        this.pluginVersion = pluginVersion;
    }

    public Drawable getThemeDrawable() {
        return this.themeDrawable;
    }

    public void setThemeDrawable(Drawable drawable) {
        this.themeDrawable = drawable;
    }

    public Context getContext() {
        return themeContext;
    }

    public void setContext(Context context) {
        this.themeContext = context;
    }

    public String getSDKLevels() {
        return sdkLevels;
    }

    public void setSDKLevels(String sdkLevels) {
        this.sdkLevels = sdkLevels;
    }

    public String getThemeVersion() {
        return themeVersion;
    }

    public void setThemeVersion(String themeVersion) {
        this.themeVersion = themeVersion;
    }
}