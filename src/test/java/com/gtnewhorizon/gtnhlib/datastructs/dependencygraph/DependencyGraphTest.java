package com.gtnewhorizon.gtnhlib.datastructs.dependencygraph;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

public class DependencyGraphTest {

    private static DependencyGraph<String> graph() {
        return new DependencyGraph<>(new String[0]);
    }

    @Test
    void keepsInsertionOrderWithoutDependencies() {
        var g = graph();
        for (String s : new String[] { "zeta", "alpha", "mu", "beta", "omega" }) {
            g.addObject(s, s);
        }

        assertArrayEquals(new String[] { "zeta", "alpha", "mu", "beta", "omega" }, g.sorted());
    }

    @Test
    void ordersByDependencies() {
        var g = graph();
        g.addObject("a", "a", "after:b");
        g.addObject("b", "b");
        g.addObject("c", "c", "before:b");

        assertArrayEquals(new String[] { "c", "b", "a" }, g.sorted());
    }

    @Test
    void missingOptionalDependencyIsIgnored() {
        var g = graph();
        g.addObject("a", "a", "after:missing");
        g.addObject("b", "b", "requires:missing?");

        String[] sorted = assertTimeoutPreemptively(Duration.ofSeconds(5), g::sorted);

        assertArrayEquals(new String[] { "a", "b" }, sorted);
    }

    @Test
    void missingRequiredDependencyThrows() {
        var g = graph();
        g.addObject("a", "a", "requires:missing");

        assertThrows(IllegalStateException.class, g::sorted);
    }

    @Test
    void addDependencyOverwritesOptionality() {
        var g = graph();
        g.addObject("a", "a");
        g.addDependency("a", "missing", true);
        g.addDependency("a", "missing", false);

        assertThrows(IllegalStateException.class, g::sorted);
    }

    @Test
    void parsesBracketedSegments() {
        assertArrayEquals(new String[] { "a", "b" }, DependencyGraph.parsePath("a/b"));
        assertArrayEquals(
                new String[] { "commands", "mymod:chat/foo" },
                DependencyGraph.parsePath("commands/[mymod:chat/foo]"));
        assertArrayEquals(new String[] { "a/b", "c" }, DependencyGraph.parsePath("[a/b]/c"));
        assertArrayEquals(new String[] { "plain" }, DependencyGraph.parsePath("[plain]"));
    }

    @Test
    void rejectsMalformedPaths() {
        for (String bad : new String[] { "[a/b", "a]", "x[a]", "[a]x", "[a[b]]", "a//b", "a/", "/a", "", "[]" }) {
            assertThrows(IllegalArgumentException.class, () -> DependencyGraph.parsePath(bad), bad);
        }
    }

    @Test
    void bracketedNamesInSubgraphs() {
        var g = graph();
        g.addSubgraph("commands");
        g.addObject("commands/[mymod:chat/foo]", "foo");
        g.addObject("commands/[mymod:chat/bar]", "bar", "before:[mymod:chat/foo]");

        assertTrue(g.hasObject("commands/[mymod:chat/foo]"));
        assertFalse(g.hasObject("commands/mymod:chat/foo"));
        assertEquals("foo", g.getObject("commands/[mymod:chat/foo]"));
        assertArrayEquals(new String[] { "bar", "foo" }, g.sorted());
    }

    @Test
    void dependenciesMustBeLocal() {
        var g = graph();

        assertThrows(IllegalArgumentException.class, () -> g.addObject("a", "a", "after:other/thing"));
    }

    @Test
    void missingSubgraphNamesFailingSegment() {
        var g = graph();
        g.addSubgraph("commands");

        var e = assertThrows(MissingEntityException.class, () -> g.addObject("commands/[x/y]/z", "z"));
        assertEquals("Could not find subgraph: commands/[x/y]", e.getMessage());
    }
}
