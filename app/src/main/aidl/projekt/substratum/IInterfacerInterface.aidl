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

package projekt.substratum;

interface IInterfacerInterface {

    /**
     * Install a list of specified applications
     *
     * @param paths Filled in with a list of path names for packages to be installed from.
     */
    void installPackage(in List<String> paths);

    /**
     * Uninstall a list of specified applications
     *
     * @param packages  Filled in with a list of path names for packages to be installed from.
     * @param restartUi Flag to automatically restart the SystemUI.
     */
    void uninstallPackage(in List<String> packages, boolean restartUi);

    /**
     * Restart SystemUI
     */
    void restartSystemUI();

    /**
     * Perform a low-level configuration change
     */
    void configurationShim();

    /**
     * Apply a specified bootanimation
     *
     * @param name  Path to extract the bootanimation archive from.
     */
    void applyBootanimation(String name);

    /**
     * Apply a specified font pack
     *
     * @param name  Path to extract the font archive from.
     */
    void applyFonts(String pid, String fileName);

    /**
     * Apply a specified sound pack
     *
     * @param name  Path to extract the font archive from.
     */
    void applyAudio(String pid, String fileName);

    /**
     * Enable a specified list of overlays
     *
     * @param packages  Filled in with a list of package names to be enabled.
     * @param restartUi Flag to automatically restart the SystemUI.
     */
    void enableOverlay(in List<String> packages, boolean restartUi);

    /**
     * Disable a specified list of overlays
     *
     * @param packages  Filled in with a list of package names to be disabled.
     * @param restartUi Flag to automatically restart the SystemUI.
     */
    void disableOverlay(in List<String> packages, boolean restartUi);

    /**
     * Change the priority of a specified list of overlays
     *
     * @param packages  Filled in with a list of package names to be reordered.
     * @param restartUi Flag to automatically restart the SystemUI.
     */
    void changePriority(in List<String> packages, boolean restartUi);

    /**
     * Copy Method
     *
     * @param source        Path of the source file.
     * @param destination   Path of the source file to be copied to.
     */
    void copy(String source, String destination);

    /**
     * Move Method
     *
     * @param source        Path of the source file.
     * @param destination   Path of the source file to be moved to.
     */
    void move(String source, String destination);

    /**
     * Create Directory Method
     *
     * @param destination   Path of the created destination folder.
     */
    void mkdir(String destination);

    /**
     * Delete Directory Method
     *
     * @param destination   Path of the directory to be deleted.
     * @param withParent    Flag to automatically delete the folder encompassing the folder.
     */
    void deleteDirectory(String directory, boolean withParent);

    /**
     * Profile Applicator
     *
     * @param enable     Filled in with a list of package names to be enabled.
     * @param disable    Filled in with a list of package names to be disabled.
     * @param name       Name of the profile to be applied.
     * @param restartUi  Flag to automatically restart the SystemUI.
     */
    void applyProfile(in List<String> enable, in List<String> disable, String name,
            boolean restartUi);
}