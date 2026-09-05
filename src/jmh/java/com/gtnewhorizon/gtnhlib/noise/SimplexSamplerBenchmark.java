package com.gtnewhorizon.gtnhlib.noise;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
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
public class SimplexSamplerBenchmark {

    private SimplexSampler sampler;
    private int counter;

    @Setup
    public void setup() {
        sampler = new SimplexSampler(new Random(42));
        counter = 0;
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
