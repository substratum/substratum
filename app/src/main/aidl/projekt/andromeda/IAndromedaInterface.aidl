/*
 * Copyright (c) 2017 Projekt Andromeda
 * This file is part of Andromeda.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package projekt.andromeda;

interface IAndromedaInterface {

    /**
     * Install a list of specified applications
     *
     * @param overlays Filled in with a list of path names for packages to be installed from.
     */
    boolean installPackage(in List<String> overlays);

    /**
     * Uninstall a list of specified applications
     *
     * @param overlays  Filled in with a list of path names for packages to be installed from.
     */
    boolean uninstallPackage(in List<String> overlays);

    /**
     * List enabled overlays
     */
    boolean listOverlays();

    /**
     * Enable a specified list of overlays
     *
     * @param overlays  Filled in with a list of package names to be enabled.
     */
    boolean enableOverlay(in List<String> overlays);

    /**
     * Disable a specified list of overlays
     *
     * @param overlays  Filled in with a list of package names to be disabled.
     */
    boolean disableOverlay(in List<String> overlays);

    /**
     * Change the priority of a specified list of overlays
     *
     * @param overlays  Filled in with a list of package names to be reordered.
     */
    boolean changePriority(in List<String> overlays);
}