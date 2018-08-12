/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.adapters.fragments.priorities;

public interface PrioritiesInterface {

    PrioritiesItemType getType();

    enum PrioritiesItemType {
        HEADER, CONTENT
    }
}