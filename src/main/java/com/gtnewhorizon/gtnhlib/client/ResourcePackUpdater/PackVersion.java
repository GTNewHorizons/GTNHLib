package com.gtnewhorizon.gtnhlib.client.ResourcePackUpdater;

final class PackVersion implements Comparable<PackVersion> {

    final int major;
    final int minor;

    private PackVersion(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    static PackVersion parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Version is null");
        }
        String[] parts = value.trim().split("\\.");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Version must be major.minor: " + value);
        }
        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        if (major < 0 || minor < 0) {
            throw new IllegalArgumentException("Version must be non-negative: " + value);
        }
        return new PackVersion(major, minor);
    }

    @Override
    public int compareTo(PackVersion other) {
        int majorCompare = Integer.compare(this.major, other.major);
        if (majorCompare != 0) {
            return majorCompare;
        }
        return Integer.compare(this.minor, other.minor);
    }
}
