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

package projekt.substratum.util.files;

import android.support.v4.util.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class MapUtils {

    public static <S, T extends Comparable<T>> List<Pair<S, T>> sortMapByValues(HashMap<S, T> map) {
        List<Pair<S, T>> list = map.entrySet().stream().map(entry -> new Pair<>(entry.getKey(),
                entry.getValue())).collect(Collectors.toList());

        Collections.sort(list,
                (pair1, pair2) -> pair1.second.compareTo(pair2.second));
        return list;
    }
}