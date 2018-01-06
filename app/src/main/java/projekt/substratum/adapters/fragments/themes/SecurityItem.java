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

package projekt.substratum.adapters.fragments.themes;

public class SecurityItem {

    private String package_name;
    private Integer hash;
    private Boolean launch_type;
    private Boolean debug;
    private Boolean piracy_check;
    private byte[] encryption_key;
    private byte[] iv_encrypt_key;

    public SecurityItem(String package_name) {
        this.package_name = package_name;
    }

    public String getPackageName() {
        return this.package_name;
    }

    Integer getHash() {
        return this.hash;
    }

    public void setHash(Integer hash) {
        this.hash = hash;
    }

    Boolean getLaunchType() {
        return this.launch_type;
    }

    public void setLaunchType(Boolean launch_type) {
        this.launch_type = launch_type;
    }

    public Boolean getDebug() {
        return this.debug;
    }

    public void setDebug(Boolean debug) {
        this.debug = debug;
    }

    Boolean getPiracyCheck() {
        return this.piracy_check;
    }

    public void setPiracyCheck(Boolean piracy_check) {
        this.piracy_check = piracy_check;
    }

    byte[] getEncryptionKey() {
        return this.encryption_key;
    }

    public void setEncryptionKey(byte[] encryption_key) {
        this.encryption_key = encryption_key;
    }

    byte[] getIVEncryptKey() {
        return this.iv_encrypt_key;
    }

    public void setIVEncryptKey(byte[] iv_encrypt_key) {
        this.iv_encrypt_key = iv_encrypt_key;
    }
}