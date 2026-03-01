package com.gtnewhorizon.gtnhlib.test.concurrent;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.concurrent.cas.CasList;
import com.gtnewhorizon.gtnhlib.concurrent.cas.CasLongObjectMap;
import com.gtnewhorizon.gtnhlib.concurrent.cas.CasMap;
import com.gtnewhorizon.gtnhlib.concurrent.cas.Versioned;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

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

    @Test
    void simpleCasLongObjectMap() {
        final CasLongObjectMap<String> map = new CasLongObjectMap<>();
        for (long i = 1; i <= 10; i++) {
            map.put(i, "v" + i);
        }
        Assertions.assertEquals(10, map.size());
        for (long i = 1; i <= 10; i++) {
            Assertions.assertEquals("v" + i, map.get(i));
        }
        Assertions.assertTrue(map.containsKey(5));
        Assertions.assertFalse(map.containsKey(99));
        Assertions.assertEquals("v3", map.remove(3));
        Assertions.assertEquals(9, map.size());
        Assertions.assertNull(map.get(3));
    }

    @Test
    void casLongObjectMapSnapshot() {
        final CasLongObjectMap<Integer> map = new CasLongObjectMap<>();
        map.put(1L, 100);
        map.put(2L, 200);
        Long2ObjectOpenHashMap<Integer> snap = map.snapshot();
        Assertions.assertEquals(2, snap.size());
        // Mutating the snapshot must not affect the original
        snap.put(3L, (Integer) 300);
        Assertions.assertEquals(2, map.size());
    }

    @Test
    void lockAndGetMutateVisibleDuringLock() {
        final CasLongObjectMap<String> map = new CasLongObjectMap<>();
        map.put(1L, "a");
        map.put(2L, "b");

        Long2ObjectOpenHashMap<String> mutable = map.lockAndGet();
        try {
            // Writes go directly to the locked copy
            map.put(3L, "c");
            // Reads see the locked copy with the new entry
            Assertions.assertEquals("c", map.get(3L));
            Assertions.assertEquals(3, map.size());
            // mutate() also routes to the locked copy
            map.mutate(m -> {
                m.put(4L, "d");
                return null;
            });
            Assertions.assertEquals(4, map.size());
        } finally {
            map.replaceAndUnlock(mutable);
        }

        // After unlock, published snapshot includes all changes
        Assertions.assertEquals(4, map.size());
        Assertions.assertEquals("c", map.get(3L));
        Assertions.assertEquals("d", map.get(4L));
    }

    @Test
    void lockAndGetCrossThreadSeesOldSnapshot() throws InterruptedException {
        final CasLongObjectMap<String> map = new CasLongObjectMap<>();
        map.put(1L, "a");

        Long2ObjectOpenHashMap<String> mutable = map.lockAndGet();
        try {
            // Write while locked
            map.put(2L, "b");

            // Another thread sees the pre-lock snapshot
            final boolean[] results = new boolean[2];
            Thread reader = new Thread(() -> {
                results[0] = map.containsKey(1L); // should be true
                results[1] = map.containsKey(2L); // should be false - not published yet
            });
            reader.start();
            reader.join();
            Assertions.assertTrue(results[0], "cross-thread should see pre-lock data");
            Assertions.assertFalse(results[1], "cross-thread should NOT see locked writes");
        } finally {
            map.replaceAndUnlock(mutable);
        }

        // After unlock, cross-thread sees everything
        final boolean[] results = new boolean[1];
        Thread reader = new Thread(() -> { results[0] = map.containsKey(2L); });
        reader.start();
        reader.join();
        Assertions.assertTrue(results[0], "cross-thread should see published data after unlock");
    }

    @Test
    void versionIncrements() {
        final CasLongObjectMap<String> map = new CasLongObjectMap<>();
        final long v0 = map.version();

        // mutate bumps version
        map.put(1L, "a");
        Assertions.assertEquals(v0 + 1, map.version());

        // overwrite bumps version
        map.overwrite(new Long2ObjectOpenHashMap<>());
        Assertions.assertEquals(v0 + 2, map.version());

        // lock/unlock bumps version once
        map.lock();
        map.put(1L, "b");
        map.put(2L, "c");
        map.unlock();
        Assertions.assertEquals(v0 + 3, map.version());

        // lockAndGet/replaceAndUnlock bumps version once
        Long2ObjectOpenHashMap<String> mutable = map.lockAndGet();
        mutable.put(3L, "d");
        map.replaceAndUnlock(mutable);
        Assertions.assertEquals(v0 + 4, map.version());
    }

    @Test
    void readVersionedAndValidate() {
        final CasLongObjectMap<String> map = new CasLongObjectMap<>();
        map.put(1L, "a");

        Versioned<Long2ObjectOpenHashMap<String>> snap = map.readVersioned();
        Assertions.assertEquals(1, snap.value().size());
        Assertions.assertTrue(map.validateVersion(snap.version()));

        // Write invalidates the stamp
        map.put(2L, "b");
        Assertions.assertFalse(map.validateVersion(snap.version()));

        // Fresh read gets new version
        Versioned<Long2ObjectOpenHashMap<String>> snap2 = map.readVersioned();
        Assertions.assertTrue(map.validateVersion(snap2.version()));
        Assertions.assertEquals(2, snap2.value().size());
    }

    @Test
    void versionStableDuringLock() {
        final CasLongObjectMap<String> map = new CasLongObjectMap<>();
        map.put(1L, "a");
        final long vBeforeLock = map.version();

        map.lock();
        // Mutations while locked do NOT bump version (not published yet)
        map.put(2L, "b");
        Assertions.assertEquals(vBeforeLock, map.version());
        map.mutate(m -> {
            m.put(3L, "c");
            return null;
        });
        Assertions.assertEquals(vBeforeLock, map.version());
        // Only unlock publishes and bumps
        map.unlock();
        Assertions.assertEquals(vBeforeLock + 1, map.version());
    }

    @Test
    void lockThenLockAndGetThrows() {
        final CasLongObjectMap<String> map = new CasLongObjectMap<>();
        map.lock();
        try {
            Assertions.assertThrows(IllegalStateException.class, map::lockAndGet);
        } finally {
            map.unlock();
        }
    }

    @Test
    void lockAndGetThenLockThrows() {
        final CasLongObjectMap<String> map = new CasLongObjectMap<>();
        Long2ObjectOpenHashMap<String> mutable = map.lockAndGet();
        try {
            Assertions.assertThrows(IllegalStateException.class, map::lock);
        } finally {
            map.replaceAndUnlock(mutable);
        }
    }

    @Test
    void unlockWithoutLockThrows() {
        final CasLongObjectMap<String> map = new CasLongObjectMap<>();
        Assertions.assertThrows(IllegalMonitorStateException.class, map::unlock);
    }

    @Test
    void reentrantLockPublishesOnce() {
        final CasLongObjectMap<String> map = new CasLongObjectMap<>();
        map.put(1L, "a");
        long vBefore = map.version();

        map.lock();
        map.lock(); // re-entrant
        map.put(2L, "b");
        map.put(3L, "c");
        map.unlock(); // inner — no publish
        Assertions.assertEquals(vBefore, map.version(), "inner unlock should not publish");
        map.unlock(); // outer — publishes
        Assertions.assertEquals(vBefore + 1, map.version(), "outer unlock should bump version once");
        Assertions.assertEquals("b", map.get(2L));
        Assertions.assertEquals("c", map.get(3L));
    }

    @Test
    void casMapLockAwareReads() {
        final CasMap<String, String> map = new CasMap<>();
        map.put("a", "1");

        map.lock();
        try {
            map.put("b", "2");
            Assertions.assertEquals("2", map.get("b"), "get() should see locked write");
            Assertions.assertEquals(2, map.size(), "size() should reflect locked state");
            Assertions.assertTrue(map.containsKey("b"), "containsKey() should see locked write");
            Assertions.assertTrue(map.containsValue("2"), "containsValue() should see locked write");
            Assertions.assertFalse(map.isEmpty());
        } finally {
            map.unlock();
        }
    }

    @Test
    void casListLockAwareReads() {
        final CasList<String> list = new CasList<>("a");

        list.lock();
        try {
            list.add("b");
            Assertions.assertTrue(list.contains("b"), "contains() should see locked write");
            Assertions.assertEquals(2, list.size(), "size() should reflect locked state");
            Assertions.assertFalse(list.isEmpty());
        } finally {
            list.unlock();
        }
    }

    @Test
    void casMapForEachDoesNotLock() {
        final CasMap<String, Integer> map = new CasMap<>(new String[] { "a", "b" }, new Integer[] { 1, 2 });
        long vBefore = map.version();
        HashMap<String, Integer> collected = new HashMap<>();
        map.forEach(collected::put);
        Assertions.assertEquals(2, collected.size());
        Assertions.assertEquals(1, collected.get("a"));
        Assertions.assertEquals(2, collected.get("b"));
        Assertions.assertEquals(vBefore, map.version(), "forEach should not bump version");
    }

    @Test
    void crossThreadWriterBlocks() throws InterruptedException {
        final CasMap<String, String> map = new CasMap<>();
        map.put("key", "initial");

        CountDownLatch writerStarted = new CountDownLatch(1);
        AtomicBoolean writerDone = new AtomicBoolean(false);

        map.lock();
        try {
            map.put("key", "locked");

            Thread writer = new Thread(() -> {
                writerStarted.countDown();
                map.put("key", "from-thread-b");
                writerDone.set(true);
            });
            writer.start();

            // Wait for writer thread to start, then give it time to block on the lock
            Assertions.assertTrue(writerStarted.await(1, TimeUnit.SECONDS));
            Thread.sleep(100);
            Assertions.assertFalse(writerDone.get(), "Writer should be blocked while lock is held");
        } finally {
            map.unlock();
        }

        // Writer should complete shortly after unlock
        Thread.sleep(200);
        Assertions.assertTrue(writerDone.get(), "Writer should complete after unlock");
    }

    @Test
    void nestedMutateLosesData() {
        final CasMap<String, String> map = new CasMap<>();
        map.put("key", "initial");
        map.mutate(m -> {
            map.put("key", "inner");
            return null;
        });
        Assertions.assertEquals("inner", map.get("key"));
    }

    @Test
    void spuriousUnlockDoesNotCorruptState() {
        final CasMap<String, String> map = new CasMap<>();
        try {
            map.unlock();
        } catch (IllegalMonitorStateException e) {}
        map.lock();
        try {
            map.put("a", "b");
        } finally {
            map.unlock();
        }
        Assertions.assertEquals("b", map.get("a"));
    }

    @Test
    void mutateUsesWriteLock() {
        // Verify that mutate() and overwrite() don't deadlock with each other (both use writeLock)
        final CasMap<String, String> map = new CasMap<>(new String[] { "a" }, new String[] { "1" });
        map.mutate(m -> {
            m.put("b", "2");
            return null;
        });
        Assertions.assertEquals("2", map.get("b"));
        map.overwrite(
                java.util.Collections.unmodifiableMap(
                        new it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<>(
                                new String[] { "c" },
                                new String[] { "3" })));
        Assertions.assertEquals("3", map.get("c"));
        Assertions.assertNull(map.get("b")); // overwrite replaced everything
    }
}
