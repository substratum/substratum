package projekt.substratum.model;

public interface PrioritiesItem {

    MonthItemType getType();

    enum MonthItemType {
        HEADER, MONTH
    }
}
