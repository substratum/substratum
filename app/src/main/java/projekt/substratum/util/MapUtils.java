package projekt.substratum.util;

import android.support.v4.util.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class MapUtils {

    public static <S, T extends Comparable<T>> List<Pair<S, T>> sortMapByValues(HashMap<S, T> map) {
        List<Pair<S, T>> list = map.entrySet().stream().map(entry -> new Pair<>(entry.getKey(), entry.getValue())).collect(Collectors.toList());

        Collections.sort(list,
                (pair1, pair2) -> pair1.second.compareTo(pair2.second));
        return list;
    }
}