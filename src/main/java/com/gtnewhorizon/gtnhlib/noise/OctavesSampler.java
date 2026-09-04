package com.gtnewhorizon.gtnhlib.noise;

import java.util.Random;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// Layers several samplers on top of each other.
/// More octaves increase the CPU cost linearly, but increase the complexity and detail of the returned noise.
/// Each octave has an increasing scale (smaller features) and a decreasing amplitude (smaller effect).
public class OctavesSampler implements NoiseSampler {

    private final @NotNull NoiseSampler @NotNull [] octaves;
    private final double[] normalizedAmplitudes, scales;

    public OctavesSampler(@Nullable NoiseSampler @NotNull [] samplers, double[] amplitudes, double[] scales) {
        int samplerCount = 0;

        for (NoiseSampler s : samplers) {
            if (s != null) samplerCount++;
        }

        this.octaves = new NoiseSampler[samplerCount];
        this.normalizedAmplitudes = new double[samplerCount];
        this.scales = new double[samplerCount];

        double sum = 0;

        for (double amp : amplitudes) {
            sum += amp;
        }

        double norm = 1d / sum;

        int samplerCursor = 0;
        for (int i = 0; i < samplers.length; i++) {
            if (samplers[i] != null) {
                this.octaves[samplerCursor] = samplers[i];
                this.normalizedAmplitudes[samplerCursor] = amplitudes[i] * norm;
                this.scales[samplerCursor] = scales[i];
                samplerCursor++;
            }
        }
    }

    public OctavesSampler(Supplier<NoiseSampler> samplers, int octaves) {
        this.octaves = new NoiseSampler[octaves];
        this.normalizedAmplitudes = new double[octaves];
        this.scales = new double[octaves];

        double sum = 0;

        for (int i = 0; i < octaves; i++) {
            this.octaves[i] = samplers.get();
            double amp = 1d / Math.pow(2d, i);
            this.normalizedAmplitudes[i] = amp;
            this.scales[i] = Math.pow(2d, i);
            sum += amp;
        }

        double norm = 1d / sum;

        for (int i = 0; i < octaves; i++) {
            this.normalizedAmplitudes[i] *= norm;
        }
    }

    public OctavesSampler(Random rng, int octaves) {
        this(() -> new SimplexSampler(rng), octaves);
    }

    @Override
    public double sample(double x, double y) {
        double value = 0;

        for (int i = 0, len = octaves.length; i < len; i++) {
            double scale = scales[i];
            value += octaves[i].sample(x * scale, y * scale) * normalizedAmplitudes[i];
        }

        return value;
    }

    @Override
    public double sample(double x, double y, double z) {
        double value = 0;

        for (int i = 0, len = octaves.length; i < len; i++) {
            double scale = scales[i];
            value += octaves[i].sample(x * scale, y * scale, z * scale) * normalizedAmplitudes[i];
        }

        return value;
    }
}
