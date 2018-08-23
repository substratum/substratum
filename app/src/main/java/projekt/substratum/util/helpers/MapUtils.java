/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.util.helpers;

import androidx.core.util.Pair;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MapUtils {

    @SuppressWarnings("FuseStreamOperations")
    public static <S, T extends Comparable<T>> List<Pair<S, T>> sortMapByValues(Map<S, T> map) {
        List<Pair<S, T>> list = map.entrySet().stream().map(entry ->
                new Pair<>(entry.getKey(),
                        entry.getValue())).collect(Collectors.toList());

        list.sort(Comparator.comparing(pair -> pair.second));
        return list;
    }
}