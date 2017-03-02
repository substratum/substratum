package projekt.substratum.model;

public class VariantInfo {
    private String variant_name;
    private String variant_hex;
    private boolean forceHidden;
    private int color = 0;

    public VariantInfo(String variant_name, String variant_hex) {
        this.variant_name = variant_name;
        if (variant_hex == null) {
            forceHidden = true;
        } else {
            forceHidden = false;
            this.variant_hex = variant_hex;
        }
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

    public int getColor() {
        return this.color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public boolean isDefaultOption() {
        return this.forceHidden;
    }
}
