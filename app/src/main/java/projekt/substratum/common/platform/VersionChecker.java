package projekt.substratum.common.platform;

import android.os.Build;

public class VersionChecker {

    // TODO: This needs to go
    public static boolean checkOreo() {
        switch (Build.VERSION.RELEASE) {
            case "O":
            case "8.0.0":
                return true;
            default:
                break;
        }
        return false;
    }

    public static boolean checkOreoStockOMS() {
        switch (Build.ID) {
            case "OPP2.170420.017":
                return true;
            default:
                break;
        }
        return false;
    }
}
