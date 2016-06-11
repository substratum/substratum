package projekt.substratum.model;

public interface PrioritiesItem {

    PrioritiesItemType getType();

    enum PrioritiesItemType {
        HEADER, CONTENT
    }
}