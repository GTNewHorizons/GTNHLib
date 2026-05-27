package com.gtnewhorizon.gtnhlib.client.ResourcePackUpdater;

final class PackVersion implements Comparable<PackVersion> {

    final int major;
    final int minor;
    final int patch;

    private PackVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    static PackVersion parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Version is null");
        }
        String[] parts = value.trim().split("\\.");
        if (parts.length < 2 || parts.length > 3) {
            throw new IllegalArgumentException("Version must be major.minor or major.minor.patch: " + value);
        }
        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        int patch = parts.length == 3 ? Integer.parseInt(parts[2]) : 0;
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("Version must be non-negative: " + value);
        }
        return new PackVersion(major, minor, patch);
    }

    @Override
    public int compareTo(PackVersion other) {
        int majorCompare = Integer.compare(this.major, other.major);
        if (majorCompare != 0) {
            return majorCompare;
        }
        int minorCompare = Integer.compare(this.minor, other.minor);
        if (minorCompare != 0) {
            return minorCompare;
        }
        return Integer.compare(this.patch, other.patch);
    }
}
