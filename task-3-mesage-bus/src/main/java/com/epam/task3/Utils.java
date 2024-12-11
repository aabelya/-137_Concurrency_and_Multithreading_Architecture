package com.epam.task3;

import java.util.*;

public class Utils {

    public static String[] getNRandom(String[] ar, int n) {
        List<String> list = new ArrayList<>(Arrays.asList(ar));
        Collections.shuffle(list);
        return list.subList(0, n).toArray(new String[n]);
    }

    public static String[] getUpToNRandom(Random rnd, String[] ar, int n) {
        return getNRandom(ar, rnd.nextInt(n - 1) + 1);
    }
}
