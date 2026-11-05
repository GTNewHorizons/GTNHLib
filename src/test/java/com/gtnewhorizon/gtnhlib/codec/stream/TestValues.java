package com.gtnewhorizon.gtnhlib.codec.stream;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

final class TestValues {

    private TestValues() {}

    abstract static class Value {

        abstract Object[] fields();

        @Override
        public boolean equals(Object o) {
            return o != null && o.getClass() == getClass() && Arrays.equals(fields(), ((Value) o).fields());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(fields());
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + Arrays.toString(fields());
        }
    }

    static final class Pair2 extends Value {

        private final int a;
        private final String b;

        Pair2(int a, String b) {
            this.a = a;
            this.b = b;
        }

        int a() {
            return a;
        }

        String b() {
            return b;
        }

        @Override
        Object[] fields() {
            return new Object[] { a, b };
        }
    }

    abstract static class Ints extends Value {

        private final int[] values;

        Ints(int... values) {
            this.values = values;
        }

        int get(int index) {
            return values[index];
        }

        @Override
        Object[] fields() {
            return Arrays.stream(values).boxed().toArray();
        }
    }

    static final class Six extends Ints {

        Six(int a, int b, int c, int d, int e, int f) {
            super(a, b, c, d, e, f);
        }
    }

    static final class Twelve extends Ints {

        Twelve(int a, int b, int c, int d, int e, int f, int g, int h, int i, int j, int k, int l) {
            super(a, b, c, d, e, f, g, h, i, j, k, l);
        }
    }

    static final class Node extends Value {

        private final String name;
        private final List<Node> children;

        Node(String name, List<Node> children) {
            this.name = name;
            this.children = children;
        }

        String name() {
            return name;
        }

        List<Node> children() {
            return children;
        }

        @Override
        Object[] fields() {
            return new Object[] { name, children };
        }
    }

    static final class Point extends Value {

        static final Codec<Point> CODEC = RecordCodecBuilder
                .create(
                        instance -> instance
                                .group(
                                        Codec.INT.fieldOf("x").forGetter(Point::x),
                                        Codec.STRING.fieldOf("label").forGetter(Point::label))
                                .apply(instance, Point::new));

        private final int x;
        private final String label;

        Point(int x, String label) {
            this.x = x;
            this.label = label;
        }

        int x() {
            return x;
        }

        String label() {
            return label;
        }

        @Override
        Object[] fields() {
            return new Object[] { x, label };
        }
    }

    interface Message {

        String type();
    }

    static final class Ping extends Value implements Message {

        private final int id;

        Ping(int id) {
            this.id = id;
        }

        int id() {
            return id;
        }

        @Override
        public String type() {
            return "ping";
        }

        @Override
        Object[] fields() {
            return new Object[] { id };
        }
    }

    static final class Chat extends Value implements Message {

        private final String text;

        Chat(String text) {
            this.text = text;
        }

        String text() {
            return text;
        }

        @Override
        public String type() {
            return "chat";
        }

        @Override
        Object[] fields() {
            return new Object[] { text };
        }
    }

    static final class Unknown implements Message {

        @Override
        public String type() {
            return "unknown";
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Unknown;
        }

        @Override
        public int hashCode() {
            return Objects.hash("unknown");
        }
    }
}
