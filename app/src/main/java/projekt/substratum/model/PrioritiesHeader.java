package projekt.substratum.model;

public class PrioritiesHeader implements PrioritiesItem {

    private String mName;

    public PrioritiesHeader(final String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    @Override
    public MonthItemType getType() {
        return MonthItemType.HEADER;
    }
}
