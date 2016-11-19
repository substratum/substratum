package projekt.substratum.model;

import android.content.Context;

public class SoundsInfo {
    private String title, absolute_path;
    private Context mContext;

    public SoundsInfo(Context mContext, String title, String absolute_path) {
        this.mContext = mContext;
        this.title = title;
        this.absolute_path = absolute_path;
    }

    public String getAbsolutePath() {
        return absolute_path;
    }

    public Context getContext() {
        return mContext;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String name) {
        this.title = name;
    }
}