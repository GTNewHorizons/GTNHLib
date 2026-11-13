package com.gtnewhorizon.gtnhlib.test.block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import it.unimi.dsi.fastutil.longs.LongArrayList;

final class BlackHoleProfile {

    private final int seconds;
    private final long end;
    private final boolean capturing;
    private long lastFrame;
    private final LongArrayList frames = new LongArrayList();
    private int draws;
    private long copyNanos;
    private long lensNanos;
    private double nearest = Double.MAX_VALUE;
    private double farthest;
    private int captures;
    private long captureNanos;
    private int timedCaptures;
    private long captureGpuNanos;

    BlackHoleProfile(int seconds, boolean capturing) {
        this.seconds = seconds;
        this.end = System.nanoTime() + seconds * 1_000_000_000L;
        this.capturing = capturing;
    }

    boolean frame(long now) {
        if (lastFrame != 0) frames.add(now - lastFrame);
        lastFrame = now;
        return now >= end;
    }

    void draw(double distance, long copy, long lens) {
        draws++;
        copyNanos += copy;
        lensNanos += lens;
        nearest = Math.min(nearest, distance);
        farthest = Math.max(farthest, distance);
    }

    void capture(long cpu) {
        captures++;
        captureNanos += cpu;
    }

    void captureGpu(long gpu) {
        timedCaptures++;
        captureGpuNanos += gpu;
    }

    List<String> summary() {
        final List<String> lines = new ArrayList<>();
        lines.add(String.format("Black hole profile, %d s, capture %s:", seconds, capturing ? "on" : "off"));
        final long[] sorted = frames.toLongArray();
        Arrays.sort(sorted);
        long total = 0;
        for (long frame : sorted) total += frame;
        final int count = Math.max(sorted.length, 1);
        if (sorted.length > 0) {
            lines.add(
                    String.format(
                            "Frames: %d, %.2f ms avg, 99th percentile %.2f ms, worst %.2f ms.",
                            sorted.length,
                            total / 1e6 / count,
                            sorted[(int) (sorted.length * 0.99)] / 1e6,
                            sorted[sorted.length - 1] / 1e6));
        }
        if (draws > 0) {
            lines.add(
                    String.format(
                            "Lensing GPU: %.2f ms per draw, %.2f per frame, over %d draws %.1f to %.1f blocks away. Screen copy %.2f ms.",
                            lensNanos / 1e6 / draws,
                            lensNanos / 1e6 / count,
                            draws,
                            nearest,
                            farthest,
                            copyNanos / 1e6 / draws));
        } else {
            lines.add("The black hole wasn't drawn.");
        }
        if (captures > 0) {
            lines.add(
                    String.format(
                            "Capture: %d faces, %.2f ms CPU and %.2f GPU each, %.2f and %.2f per frame.",
                            captures,
                            captureNanos / 1e6 / captures,
                            captureGpuNanos / 1e6 / Math.max(timedCaptures, 1),
                            captureNanos / 1e6 / count,
                            captureGpuNanos / 1e6 / count));
        }
        return lines;
    }
}
