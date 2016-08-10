package projekt.substratum.config;

import android.util.Log;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class AreYouSure {

    public static boolean areYouReadyForTheDanceOfYourLife(
            String className, String currentPackage) {
        String[] allowed_class_name = {
                "a.a.a.a",
                "a.b.a.a",
                "android.support.a.a",
                "android.support.annotation",
                "android.support.v4",
                "android.support.v7",
                "com.a.a.a",
                "com.google.android.vending.licensing",
                "android.support.a.a",
                "android.support.annotation",
                "android.support.graphics.drawable",
                "com.github.javiersantos.piracychecker",
                "com.android.tools.fd",
                "com.android.tools.ir",
                "adrt",
                currentPackage + ".MainActivity",
                "substratum.theme.template.MainActivity",
                currentPackage + ".a",
                "substratum.theme.template.a",
                currentPackage + ".b",
                "substratum.theme.template.b",
                currentPackage + ".c",
                "substratum.theme.template.c",
                currentPackage + ".d",
                "substratum.theme.template.d",
                currentPackage + ".e",
                "substratum.theme.template.e",
                currentPackage + ".f",
                "substratum.theme.template.f",
                currentPackage + ".g",
                "substratum.theme.template.g",
                currentPackage + ".h",
                "substratum.theme.template.h",
                currentPackage + ".i",
                "substratum.theme.template.i",
                currentPackage + ".j",
                "substratum.theme.template.j",
                currentPackage + ".k",
                "substratum.theme.template.k",
                currentPackage + ".l",
                "substratum.theme.template.l",
                currentPackage + ".m",
                "substratum.theme.template.m",
                currentPackage + ".n",
                "substratum.theme.template.n",
                currentPackage + ".o",
                "substratum.theme.template.o",
                currentPackage + ".p",
                "substratum.theme.template.p",
                currentPackage + ".q",
                "substratum.theme.template.q",
                currentPackage + ".r",
                "substratum.theme.template.r",
                currentPackage + ".s",
                "substratum.theme.template.s",
                currentPackage + ".t",
                "substratum.theme.template.t",
                currentPackage + ".u",
                "substratum.theme.template.u",
                currentPackage + ".v",
                "substratum.theme.template.v",
                currentPackage + ".w",
                "substratum.theme.template.w",
                currentPackage + ".x",
                "substratum.theme.template.x",
                currentPackage + ".y",
                "substratum.theme.template.y",
                currentPackage + ".z",
                "substratum.theme.template.z",
                currentPackage + ".BuildConfig",
                "substratum.theme.template.BuildConfig",
                currentPackage + ".R$anim",
                "substratum.theme.template.R$anim",
                currentPackage + ".R$attr",
                "substratum.theme.template.R$attr",
                currentPackage + ".R$bool",
                "substratum.theme.template.R$bool",
                currentPackage + ".R$color",
                "substratum.theme.template.R$color",
                currentPackage + ".R$dimen",
                "substratum.theme.template.R$dimen",
                currentPackage + ".R$drawable",
                "substratum.theme.template.R$drawable",
                currentPackage + ".R$id",
                "substratum.theme.template.R$id",
                currentPackage + ".R$integer",
                "substratum.theme.template.R$integer",
                currentPackage + ".R$layout",
                "substratum.theme.template.R$layout",
                currentPackage + ".R$mipmap",
                "substratum.theme.template.R$mipmap",
                currentPackage + ".R$string",
                "substratum.theme.template.R$string",
                currentPackage + ".R$style",
                "substratum.theme.template.R$style",
                currentPackage + ".R$styleable",
                "substratum.theme.template.R$styleable",
                currentPackage + ".R",
                "substratum.theme.template.R",
                currentPackage + ".SubstratumLauncher$1",
                "substratum.theme.template.SubstratumLauncher$1",
                currentPackage + ".SubstratumLauncher",
                "substratum.theme.template.SubstratumLauncher",
                currentPackage + ".SubstratumLauncher$100000000",
                "substratum.theme.template.SubstratumLauncher$100000000"
        };
        for (int i = 0; i < allowed_class_name.length; i++) {
            if (allowed_class_name.length == 105) {
                if (className.startsWith(allowed_class_name[i])) {
                    return true;
                }
            } else {
                Log.e("SubstratumClassLogger", "Substratum has reached an unrecoverable error, " +
                        "terminating responsibilities...");
                return false;
            }
        }
        return false;
    }
}