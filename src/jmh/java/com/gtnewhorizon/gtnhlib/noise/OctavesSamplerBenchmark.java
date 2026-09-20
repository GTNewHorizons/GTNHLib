package com.gtnewhorizon.gtnhlib.noise;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class OctavesSamplerBenchmark {

    static final int GRID = 256;

    @Param({ "4", "8" })
    public int octaveCount;

    private OctavesSampler sampler;
    private int counter;
    private double[] xs, ys, zs, out;

    @Setup
    public void setup() {
        sampler = new OctavesSampler(new Random(42), octaveCount);
        counter = 0;
        xs = new double[GRID];
        ys = new double[GRID];
        zs = new double[GRID];
        out = new double[GRID];
        for (int i = 0; i < GRID; i++) {
            xs[i] = (i & 15) * 0.5;
            ys[i] = (i >> 4) * 0.5;
            zs[i] = (i & 31) * 0.5;
        }
    }

    @Benchmark
    public void sample2D(Blackhole bh) {
        bh.consume(sampler.sample(1.5, 2.7));
    }

    @Benchmark
    public void sample3D(Blackhole bh) {
        bh.consume(sampler.sample(1.5, 2.7, 3.9));
    }

    @Benchmark
    public void sample2DGrid(Blackhole bh) {
        int c = counter++;
        bh.consume(sampler.sample((c & 255) * 0.5, ((c >> 8) & 255) * 0.5));
    }

    @Benchmark
    public void sample3DGrid(Blackhole bh) {
        int c = counter++;
        bh.consume(sampler.sample((c & 63) * 0.5, ((c >> 6) & 63) * 0.5, ((c >> 12) & 63) * 0.5));
    }
}
