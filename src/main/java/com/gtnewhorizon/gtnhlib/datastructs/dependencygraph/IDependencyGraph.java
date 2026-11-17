package com.gtnewhorizon.gtnhlib.datastructs.dependencygraph;

/// A dependency graph is something that stores a list of named entities. Each entity has a list of dependencies, and
/// these dependencies determine the order that the objects in the dependency graph will be processed.
///
/// This is typically used to control the ordering of third party integrations. If one integration needs to run after or
/// before another, it will add a `before:xyz` or `after:xyz` dependency on the other integration.
///
/// Entities can be added or removed at any point, but this is discouraged because it makes debugging very difficult.
/// Circular dependencies between entities will cause a runtime error and must be avoided.
///
/// An entity may be one of the following:
/// - An object can be any generic java object, and is the primary 'unit'.
/// - A target acts like a systemd target - see [#addTarget(String, String...)].
/// - A subgraph is a nested graph within this graph - see [#addSubgraph(String, String...)].
///
/// Names for entities can be separated by forward slashes, which inserts them into a subgraph instead of the root
/// graph.
/// The subgraph must exist, or an error will be thrown. Subgraphs cannot be made optional. When an entity is inserted
/// into a subgraph, its dependencies are against entities within that subgraph.
/// Example: `inventories/backpacks/backpack-adapter`.
///
/// A segment wrapped in square brackets is taken literally, so that names containing slashes (such as resource
/// locations) can be used: `commands/[mymod:chat/foo]` is the entity `mymod:chat/foo` within the `commands` subgraph.
/// Brackets must wrap a whole segment and cannot be nested. Dependencies always refer to an entity in the same graph as
/// the dependent, so they are a single segment and must be bracketed if they contain a slash: `after:[mymod:chat/foo]`.
public interface IDependencyGraph<T> {

    /// The current object must run after another one. These dependencies can be made optional by adding a question mark
    /// suffix: `requires:foo?`. An optional dependency will not throw an error if the dependent object is missing.
    String REQUIRES = "requires:";
    /// The current object will always run after another one. This is always optional.
    String AFTER = "after:";
    /// The opposite of [#REQUIRES]. Ths current object will run before another one. As with [#REQUIRES], this can be
    /// made optional by adding a question mark suffix: `required-by:bar?`.
    String REQUIRED_BY = "required-by:";
    /// The opposite of [#AFTER]. The current object will always run before another one. As with [#AFTER], this is
    /// always optional.
    String BEFORE = "before:";

    /// Adds a named object and assigns it zero or more dependencies.
    void addObject(String name, T value, String... deps);

    /// Checks if a named objects exists. If the given refers to a non-object this returns false.
    boolean hasObject(String name);

    /// Gets a named object, or null if the name refers to a non-object or if it does not exist.
    T getObject(String name);

    /// Adds a dependency from an object to another one. `object` will be sorted with relation to `dependsOn`.
    /// If a dependency between the two entities already exists, the given `optional` parameter overwrites the existing
    /// optional-ness.
    void addDependency(String object, String dependsOn, boolean optional);

    /// Removes a dependency. Opposite of [#addDependency(String, String, boolean)].
    /// @return True when a dependency was removed.
    boolean removeDependency(String object, String dependsOn);

    /// Adds a target. This acts like a systemd target - it can be thought of as a 'goal' instead of a discrete step.
    /// As an example, if an integration module requires all block information to be written to perform some TileEntity
    /// operation, the `configure-block` target can be used to force all block-modifying operations to run before the
    /// TileEntity operation.
    void addTarget(String targetName, String... deps);

    /// Adds a nested graph within this graph. Subgraphs can be nested within another subgraph via forward slashes.
    void addSubgraph(String graphName, String... deps);
}
