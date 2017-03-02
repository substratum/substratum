package projekt.substratum.model;

public class VariantInfo {
    private String variant_name;
    private String variant_hex;

    public VariantInfo(String variant_name, String variant_hex) {
        this.variant_name = variant_name;
        this.variant_hex = variant_hex;
    }

    public String getVariantName() {
        return variant_name;
    }

    public String getVariantHex() {
        return variant_hex;
    }

    @Override
    public String toString() {
        return getVariantName();
    }
}
