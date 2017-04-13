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
    void installPackage(in List<String> paths);
    void uninstallPackage(in List<String> packages, boolean restartUi);
    void restartSystemUI();
    void configurationShim();
    void applyBootanimation(String name);
    void applyFonts(String pid, String fileName);
    void applyAudio(String pid, String fileName);
    void enableOverlay(in List<String> packages, boolean restartUi);
    void disableOverlay(in List<String> packages, boolean restartUi);
    void changePriority(in List<String> packages, boolean restartUi);
    void copy(String source, String destination);
    void move(String source, String destination);
    void mkdir(String destination);
    void deleteDirectory(String directory, boolean withParent);
    void applyProfile(in List<String> enable, in List<String> disable, String name,
            boolean restartUi);
}
