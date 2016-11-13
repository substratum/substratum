package projekt.substratum.util;

import android.support.v4.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapUtils {

    public static <S, T extends Comparable<T>> List<Pair<S, T>> sortMapByValues(HashMap<S, T> map) {
        List<Pair<S, T>> list = new ArrayList<>();
        for (Map.Entry<S, T> entry : map.entrySet()) {
            list.add(new Pair<>(entry.getKey(), entry.getValue()));
        }

        Collections.sort(list,
                new Comparator<Pair<S, T>>() {
                    @Override
                    public int compare(Pair<S, T> pair1, Pair<S, T> pair2) {
                        return pair1.second.compareTo(pair2.second);
                    }
                });
        return list;
    }

}
