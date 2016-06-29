package projekt.substratum.model;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class SoundsInfo {
    private String title, absolute_path;

    public SoundsInfo(String title, String absolute_path) {
        this.title = title;
        this.absolute_path = absolute_path;
    }

    public String getAbsolutePath() {
        return absolute_path;
    }

    public void setAbsolutePath(String name) {
        this.absolute_path = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String name) {
        this.title = name;
    }
}