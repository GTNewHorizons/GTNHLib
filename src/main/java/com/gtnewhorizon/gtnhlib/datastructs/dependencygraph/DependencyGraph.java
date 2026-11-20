package com.gtnewhorizon.gtnhlib.datastructs.dependencygraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jetbrains.annotations.UnknownNullability;

import com.github.bsideup.jabel.Desugar;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;

public class DependencyGraph<T> implements IDependencyGraph<T> {

    interface Entity {
    }

    @Desugar
    private record ObjectEntity<T> (T value) implements Entity {}

    @Desugar
    private record TargetEntity() implements Entity {}

    @Desugar
    private record SubgraphEntity<T> (DependencyGraph<T> graph) implements Entity {}

    @Desugar
    private record DepInfo(String dependency, boolean optional) {

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof DepInfo depInfo)) return false;

            return Objects.equals(dependency, depInfo.dependency);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(dependency);
        }
    }

    // linked so that entities without dependencies keep their insertion order
    private final Object2ObjectLinkedOpenHashMap<String, Entity> entities = new Object2ObjectLinkedOpenHashMap<>();

    private final Multimap<String, DepInfo> dependencies = MultimapBuilder.hashKeys().hashSetValues().build();

    private final T[] zeroSized;
    private T[] cachedSorted;

    public DependencyGraph(T[] zeroSized) {
        this.zeroSized = zeroSized;
        entities.defaultReturnValue(null);
    }

    @Desugar
    private record SubgraphLocation<T> (DependencyGraph<T> graph, String name) {}

    @UnknownNullability
    private SubgraphLocation<T> getSubgraphLocation(String name, boolean invalidate, boolean failOnInvalid) {
        String[] path = parsePath(name);

        DependencyGraph<T> graph = this;

        for (int i = 0; i < path.length - 1; i++) {
            if (invalidate) {
                // Hacky way to invalidate the cache without needing two iterations.
                // This doesn't really belong here, but it's where I'm shoving it.
                graph.cachedSorted = null;
            }

            String chunk = path[i];

            Entity e = graph.entities.get(chunk);

            if (e == null) {
                if (failOnInvalid) {
                    throw new MissingEntityException("Could not find subgraph: " + formatPath(path, i + 1));
                } else {
                    return null;
                }
            }

            if (!(e instanceof DependencyGraph.SubgraphEntity<?>subgraph)) {
                if (failOnInvalid) {
                    throw new MissingEntityException(
                            "Entity was not subgraph: " + formatPath(path, i + 1) + " (was " + e + ")");
                } else {
                    return null;
                }
            }

            // noinspection unchecked
            graph = (DependencyGraph<T>) subgraph.graph;
        }

        if (invalidate) {
            graph.cachedSorted = null;
        }

        return new SubgraphLocation<>(graph, path[path.length - 1]);
    }

    /// Splits a path into its segments. Segments are separated by forward slashes, and a segment wrapped in square
    /// brackets is taken literally so that it can contain slashes: `commands/[mymod:chat/foo]` is the entity
    /// `mymod:chat/foo` within the `commands` subgraph.
    static String[] parsePath(String path) {
        List<String> segments = new ArrayList<>();
        StringBuilder segment = new StringBuilder();
        boolean bracketed = false, closed = false;

        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);

            if (bracketed) {
                if (c == ']') {
                    bracketed = false;
                    closed = true;
                } else if (c == '[') {
                    throw new IllegalArgumentException("Nested '[' at index " + i + " in name: '" + path + "'");
                } else {
                    segment.append(c);
                }
            } else if (c == '/') {
                addSegment(segments, segment, path);
                closed = false;
            } else if (closed) {
                throw new IllegalArgumentException("Expected '/' after ']' at index " + i + " in name: '" + path + "'");
            } else if (c == '[') {
                if (segment.length() > 0) {
                    throw new IllegalArgumentException(
                            "'[' must start a segment, at index " + i + " in name: '" + path + "'");
                }

                bracketed = true;
            } else if (c == ']') {
                throw new IllegalArgumentException("Unopened ']' at index " + i + " in name: '" + path + "'");
            } else {
                segment.append(c);
            }
        }

        if (bracketed) {
            throw new IllegalArgumentException("Unclosed '[' in name: '" + path + "'");
        }

        addSegment(segments, segment, path);

        return segments.toArray(new String[0]);
    }

    private static void addSegment(List<String> segments, StringBuilder segment, String path) {
        if (segment.length() == 0) {
            throw new IllegalArgumentException("Empty segment in name: '" + path + "'");
        }

        segments.add(segment.toString());
        segment.setLength(0);
    }

    /// Parses a dependency target. Dependencies always refer to entities within the same graph, so they must be a
    /// single segment.
    static String parseLocalName(String name) {
        String[] path = parsePath(name);

        if (path.length != 1) {
            throw new IllegalArgumentException(
                    "Dependencies must refer to an entity in the same graph, wrap names that contain '/' in brackets: '"
                            + name
                            + "'");
        }

        return path[0];
    }

    /// The inverse of [#parsePath(String)], for the first `end` segments.
    private static String formatPath(String[] path, int end) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < end; i++) {
            if (i > 0) sb.append('/');

            if (path[i].indexOf('/') != -1) {
                sb.append('[').append(path[i]).append(']');
            } else {
                sb.append(path[i]);
            }
        }

        return sb.toString();
    }

    @Override
    public void addObject(String name, T value, String... deps) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(value, "value must not be null");

        var loc = getSubgraphLocation(name, true, true);

        loc.graph.entities.put(loc.name, new ObjectEntity<>(value));

        for (String dep : deps) {
            loc.graph.addUnparsedDependency(loc.name, dep);
        }
    }

    @Override
    public boolean hasObject(String name) {
        var loc = getSubgraphLocation(name, false, false);

        if (loc == null) return false;

        Entity e = loc.graph.entities.get(loc.name);

        return e instanceof ObjectEntity;
    }

    @Override
    public T getObject(String name) {
        var loc = getSubgraphLocation(name, false, false);

        if (loc == null) return null;

        Entity e = loc.graph.entities.get(loc.name);

        // noinspection rawtypes
        if (!(e instanceof ObjectEntity object)) return null;

        // noinspection unchecked
        return (T) object.value;
    }

    public void addUnparsedDependency(String object, String dep) {
        Objects.requireNonNull(object, "object must not be null");
        Objects.requireNonNull(dep, "dep must not be null");

        boolean optional = dep.endsWith("?");

        if (optional) {
            dep = dep.substring(0, dep.length() - 1);
        }

        if (dep.startsWith(REQUIRES)) {
            dep = parseLocalName(dep.substring(REQUIRES.length()).trim());
            dependencies.put(object, new DepInfo(dep, optional));
        } else if (dep.startsWith(REQUIRED_BY)) {
            dep = parseLocalName(dep.substring(REQUIRED_BY.length()).trim());
            dependencies.put(dep, new DepInfo(object, optional));
        } else if (dep.startsWith(AFTER)) {
            dep = parseLocalName(dep.substring(AFTER.length()).trim());
            dependencies.put(object, new DepInfo(dep, true));
        } else if (dep.startsWith(BEFORE)) {
            dep = parseLocalName(dep.substring(BEFORE.length()).trim());
            dependencies.put(dep, new DepInfo(object, true));
        } else {
            throw new IllegalArgumentException(
                    "Invalid dependency specification for object '" + object + "': '" + dep + "'");
        }

        cachedSorted = null;
    }

    @Override
    public void addDependency(String object, String dependsOn, boolean optional) {
        Objects.requireNonNull(object, "object must not be null");
        Objects.requireNonNull(dependsOn, "dependsOn must not be null");

        var loc = getSubgraphLocation(object, true, true);
        dependsOn = parseLocalName(dependsOn);

        // DepInfo equality ignores optional-ness, so the old dependency must be removed for the new one to replace it
        loc.graph.dependencies.remove(loc.name, new DepInfo(dependsOn, optional));
        loc.graph.dependencies.put(loc.name, new DepInfo(dependsOn, optional));
    }

    @Override
    public boolean removeDependency(String object, String dependsOn) {
        Objects.requireNonNull(object, "object must not be null");
        Objects.requireNonNull(dependsOn, "dependsOn must not be null");

        var loc = getSubgraphLocation(object, true, true);

        return loc.graph.dependencies.remove(loc.name, new DepInfo(parseLocalName(dependsOn), false));
    }

    @Override
    public void addTarget(String targetName, String... deps) {
        Objects.requireNonNull(targetName, "targetName must not be null");

        var loc = getSubgraphLocation(targetName, true, true);

        loc.graph.entities.put(loc.name, new TargetEntity());

        for (String dep : deps) {
            loc.graph.addUnparsedDependency(loc.name, dep);
        }
    }

    @Override
    public void addSubgraph(String graphName, String... deps) {
        var loc = getSubgraphLocation(graphName, true, true);

        loc.graph.entities.put(loc.name, new SubgraphEntity<>(new DependencyGraph<>(zeroSized)));

        for (String dep : deps) {
            loc.graph.addUnparsedDependency(loc.name, dep);
        }
    }

    public T[] sorted() {
        if (cachedSorted != null) return cachedSorted;

        ObjectLinkedOpenHashSet<String> path = new ObjectLinkedOpenHashSet<>();

        for (var e : dependencies.entries()) {
            preventCyclicDeps(e.getKey(), e.getValue().optional, path);
        }

        @SuppressWarnings("rawtypes")
        List out = new ArrayList<>();
        ObjectLinkedOpenHashSet<String> added = new ObjectLinkedOpenHashSet<>();

        ObjectLinkedOpenHashSet<String> remaining = new ObjectLinkedOpenHashSet<>(entities.keySet());
        while (!remaining.isEmpty()) {
            Iterator<String> iter = remaining.iterator();
            boolean progressed = false;

            iterdeps: while (iter.hasNext()) {
                String curr = iter.next();

                for (DepInfo dep : dependencies.get(curr)) {
                    // missing required deps were already rejected by preventCyclicDeps, so a missing dep is optional
                    if (!entities.containsKey(dep.dependency)) continue;

                    if (!added.contains(dep.dependency)) {
                        continue iterdeps;
                    }
                }

                progressed = true;

                iter.remove();

                added.add(curr);

                Entity value = entities.get(curr);

                if (value == null) {
                    throw new MissingEntityException("Could not find entity: " + curr);
                }

                if (value instanceof DependencyGraph.ObjectEntity<?>obj) {
                    // noinspection unchecked
                    out.add(obj.value);
                } else if (value instanceof DependencyGraph.SubgraphEntity<?>subgraph) {
                    // noinspection unchecked
                    out.addAll(Arrays.asList(subgraph.graph.sorted()));
                }
            }

            if (!progressed) {
                throw new IllegalStateException("Could not resolve the dependencies of: " + remaining);
            }
        }

        // noinspection unchecked
        cachedSorted = ((List<T>) out).toArray(zeroSized);

        return cachedSorted;
    }

    private void preventCyclicDeps(String node, boolean optional, Set<String> path) {
        if (path.contains(node)) {
            throw new IllegalStateException(
                    node + " has a cyclic dependency with itself. The path is: "
                            + path.stream().reduce("", (s, s2) -> s + ", " + s2));
        }

        if (!optional && !entities.containsKey(node)) {
            throw new IllegalStateException(
                    node + " is present in the dependency graph but does not have a matching object");
        }

        path.add(node);

        for (DepInfo dep : dependencies.get(node)) {
            preventCyclicDeps(dep.dependency, dep.optional, path);
        }

        path.remove(node);
    }
}
