package projekt.substratum.model;

public class SoundsInfo {
    private String title, absolute_path;

    public SoundsInfo(String title, String absolute_path) {
        this.title = title;
        this.absolute_path = absolute_path;
    }

    public String getAbsolutePath() {
        return absolute_path;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String name) {
        this.title = name;
    }
}