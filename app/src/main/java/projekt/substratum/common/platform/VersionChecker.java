package projekt.substratum.common.platform;

import android.os.Build;

public class VersionChecker {

    public static boolean checkOreo() {
        switch (Build.VERSION.RELEASE) {
            case "O":
                return true;
            default:
                break;
        }
        return false;
    }

    public static boolean checkOreoOMS() {
        switch (Build.ID) {
            case "OPP2.170420.017":
                return true;
            default:
                break;
        }
        return false;
    }
}
