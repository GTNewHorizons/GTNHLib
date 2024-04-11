package com.gtnewhorizon.gtnhlib.test.concurrent;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.concurrent.cas.CasList;
import com.gtnewhorizon.gtnhlib.concurrent.cas.CasMap;

// A few sanity checks for the CAS structures.
public class CasTest {

    @Test
    void simpleCasList() {
        final CasList<Integer> intList = new CasList<>(1, 2, 3);
        for (int i = 4; i <= 10; i++) {
            intList.add(i);
        }
        Assertions.assertEquals(10, intList.read().size());
        Assertions.assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }, intList.toArray(new Integer[0]));
    }

    @Test
    void simpleCasMap() {
        final CasMap<Integer, Integer> intMap = new CasMap<>(new Integer[] { 1, 2, 3 }, new Integer[] { 2, 4, 6 });
        for (int i = 4; i <= 10; i++) {
            intMap.put(i, i * 2);
        }
        Assertions.assertEquals(10, intMap.read().size());
        for (int i = 1; i <= 10; i++) {
            Assertions.assertEquals(i * 2, intMap.get(i));
        }
    }
}
