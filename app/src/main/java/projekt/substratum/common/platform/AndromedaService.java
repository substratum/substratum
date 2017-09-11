/*
 * Copyright (c) 2016-2017 Projekt Substratum
 * This file is part of Substratum.
 *
 * Substratum is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Substratum is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Substratum.  If not, see <http://www.gnu.org/licenses/>.
 */

package projekt.substratum.common.platform;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.Collections;
import java.util.List;

import projekt.andromeda.IAndromedaInterface;
import projekt.substratum.Substratum;
import projekt.substratum.common.References;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.services.binder.AndromedaBinderService;

public class AndromedaService {

    private static IAndromedaInterface getAndromedaInterface() {
        return AndromedaBinderService.getInstance().getAndromedaInterface();
    }

    public static boolean checkServerActivity() {
        try {
            return getAndromedaInterface().checkServerActivity();
        } catch (Exception e) {
            Substratum.getInstance().startAndromedaBinderService(true);
        }
        return false;
    }

    static boolean enableOverlays(List<String> overlays) {
        try {
            return getAndromedaInterface().enableOverlay(overlays);
        } catch (Exception e) {
            Substratum.getInstance().startAndromedaBinderService(true);
        }
        return false;
    }

    static boolean disableOverlays(List<String> overlays) {
        try {
            return getAndromedaInterface().disableOverlay(overlays);
        } catch (Exception e) {
            Substratum.getInstance().startAndromedaBinderService(true);
        }
        return false;
    }

    public static boolean listOverlays() {
        try {
            return getAndromedaInterface().listOverlays();
        } catch (Exception e) {
            Substratum.getInstance().startAndromedaBinderService(true);
        }
        return false;
    }

    static boolean installOverlays(List<String> overlays) {
        try {
            return getAndromedaInterface().installPackage(overlays);
        } catch (Exception e) {
            Substratum.getInstance().startAndromedaBinderService(true);
        }
        return false;
    }

    static boolean uninstallOverlays(List<String> overlays) {
        try {
            return getAndromedaInterface().uninstallPackage(overlays);
        } catch (Exception e) {
            Substratum.getInstance().startAndromedaBinderService(true);
        }
        return false;
    }

    static boolean setPriority(List<String> overlays) {
        try {
            return getAndromedaInterface().changePriority(overlays);
        } catch (Exception e) {
            Substratum.getInstance().startAndromedaBinderService(true);
        }
        return false;
    }

    static void restartSystemUI(Context context){
        if (References.getCrashOverlayPackage(context) == null){
            //(0) If not installed, install it!
            installCrashOverlay(context);
            return; //Return because package added receiver is called on installation finished
        }

        //(1) Enable a malformed overlay
        AndromedaService.enableOverlays(Collections.singletonList(References.CRASH_OVERLAY_PACKAGE));

        //(2) Wait one second
        new Handler().postDelayed(() ->
                        //(3) Disable it to restore SystemUI functionality
                        AndromedaService.disableOverlays(Collections.singletonList(References.CRASH_OVERLAY_PACKAGE))
                        , References.CRASH_OVERLAY_DELAY);
    }

    private static void installCrashOverlay(Context context) {
        Log.d(References.SUBSTRATUM_LOG, "Crash overlay not found, installing it");
        //(1) Check write external storage permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
            return;

        //(2) Copy apk file from assets to external cache
        String path = References.EXTERNAL_STORAGE_CACHE + References.CRASH_OVERLAY_ASSETS_FILE_NAME;
        FileOperations.copyFromAsset(context, References.CRASH_OVERLAY_ASSETS_FILE_NAME, path);

        //(3) Install it
        ThemeManager.installOverlay(context, path);
    }
}