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

import android.content.om.OM;
import android.content.om.OverlayInfo;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;
import java.util.Map;

import static projekt.substratum.common.References.SUBSTRATUM_LOG;

public class OverlayManagerService {

    /**
     * Api for obtaining information about overlay packages.
     */

    // Until we have a better implementation of obtaining the current user ID from the device from
    // an app, we will have to statically assign the current user as the only user
    private static final int CURRENT_USER = 0;

    /**
     * Returns information about all installed overlay packages for the
     * specified user. If there are no installed overlay packages for this user,
     * an empty map is returned (i.e. null is never returned). The returned map is a
     * mapping of target package names to lists of overlays. Each list for a
     * given target package is sorted in priority order, with the overlay with
     * the highest priority at the end of the list.
     *
     * @return A Map<String, List<OverlayInfo>> with target package names
     * mapped to lists of overlays; if no overlays exist for the
     * requested user, an empty map is returned.
     */
    static Map getAllOverlays() {
        try {
            return OM.get().getAllOverlays(CURRENT_USER);
        } catch (RemoteException re) {
            Log.e(SUBSTRATUM_LOG,
                    "Unable to obtain the map of current overlays on the device.");
        }
        return null;
    }

    /**
     * Returns information about all overlays for the given target package for
     * the specified user. The returned list is ordered according to the
     * overlay priority with the highest priority at the end of the list.
     *
     * @param targetPackageName The name of the target package.
     * @return A list of OverlayInfo objects; if no overlays exist for the
     * requested package, an empty list is returned.
     */
    public static List getOverlayInfosForTarget(String targetPackageName) {
        try {
            return OM.get().getOverlayInfosForTarget(targetPackageName, CURRENT_USER);
        } catch (RemoteException re) {
            Log.e(SUBSTRATUM_LOG,
                    "Unable to obtain the list of current overlays on the device.");
        }
        return null;
    }

    /**
     * Returns information about the overlay with the given package name for the
     * specified user.
     *
     * @param packageName The name of the overlay package.
     * @return The OverlayInfo for the overlay package; or null if no such
     * overlay package exists.
     */
    public static OverlayInfo getOverlayInfo(String packageName) {
        try {
            return OM.get().getOverlayInfo(packageName, CURRENT_USER);
        } catch (RemoteException re) {
            Log.e(SUBSTRATUM_LOG,
                    "Unable to obtain the overlay information for \"" + packageName + "\".");
        }
        return null;
    }

    /**
     * Request that an overlay package be enabled or disabled when possible to
     * do so.
     * <p>
     * It is always possible to disable an overlay, but due to technical and
     * security reasons it may not always be possible to enable an overlay. An
     * example of the latter is when the related target package is not
     * installed. If the technical obstacle is later overcome, the overlay is
     * automatically enabled at that point in time.
     * <p>
     * An enabled overlay is a part of target package's resources, i.e. it will
     * be part of any lookups performed via {@link android.content.res.Resources}
     * and {@link android.content.res.AssetManager}. A disabled overlay will no
     * longer affect the resources of the target package. If the target is
     * currently running, its outdated resources will be replaced by new ones.
     * This happens the same way as when an application enters or exits split
     * window mode.
     *
     * @param packageName The name of the overlay package.
     * @param shouldWait  true to wait to reload resources until refresh is called
     * @return true if the system successfully registered the request, false
     * otherwise.
     */
    public static boolean enable(String packageName, Boolean shouldWait) {
        try {
            boolean success = OM.get().setEnabled(packageName, true, CURRENT_USER, shouldWait);
            if (success) {
                Log.e(SUBSTRATUM_LOG, "Enabled overlay -> " + packageName + "!");
                return true;
            }
        } catch (RemoteException re) {
            Log.e(SUBSTRATUM_LOG, "Unable to enable overlay -> " + packageName);
        }
        return false;
    }

    public static boolean disable(String packageName, Boolean shouldWait) {
        try {
            boolean success = OM.get().setEnabled(packageName, false, CURRENT_USER, shouldWait);
            if (success) {
                Log.e(SUBSTRATUM_LOG, "Disabled overlay -> " + packageName + "!");
                return true;
            }
        } catch (RemoteException re) {
            Log.e(SUBSTRATUM_LOG, "Unable to disable overlay -> " + packageName);
        }
        return false;
    }

    /**
     * Change the priority of the given overlay to be just higher than the
     * overlay with package name newParentPackage. Both overlay packages
     * must have the same target and user.
     *
     * @param currentBoundPackage The name of the overlay package whose priority should
     *                            be adjusted.
     * @param newParentPackage    The name of the overlay package the newly
     *                            adjusted overlay package should just outrank.
     */
    public static boolean setPriority(String currentBoundPackage, String newParentPackage) {
        try {
            return OM.get().setPriority(currentBoundPackage, newParentPackage, CURRENT_USER);
        } catch (RemoteException re) {
            Log.e(SUBSTRATUM_LOG,
                    "Unable to set priority for \"" +
                            currentBoundPackage + "\" with parent \"" + newParentPackage + "\".");
        }
        return false;
    }

    /**
     * Change the priority of the given overlay to the highest priority relative to
     * the other overlays with the same target and user.
     *
     * @param packageName The name of the overlay package whose priority should
     *                    be adjusted.
     */
    public static boolean setHighestPriority(String packageName) {
        try {
            return OM.get().setHighestPriority(packageName, CURRENT_USER);
        } catch (RemoteException re) {
            Log.e(SUBSTRATUM_LOG,
                    "Unable to set highest priority for \"" + packageName + "\".");
        }
        return false;
    }

    /**
     * Change the priority of the overlay to the lowest priority relative to
     * the other overlays for the same target and user.
     *
     * @param packageName The name of the overlay package whose priority should
     *                    be adjusted.
     */
    public static boolean setLowestPriority(String packageName) {
        try {
            return OM.get().setLowestPriority(packageName, CURRENT_USER);
        } catch (RemoteException re) {
            Log.e(SUBSTRATUM_LOG,
                    "Unable to set lowest priority for \"" + packageName + "\".");
        }
        return false;
    }

    /**
     * Force refresh assets after job completion
     */
    public static void refresh() {
        try {
            OM.get().refresh(CURRENT_USER);
        } catch (RemoteException re) {
            Log.e(SUBSTRATUM_LOG, "Unable to refresh windows!");
        }
    }
}