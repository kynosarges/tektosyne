package org.kynosarges.tektosyne.graph;

import java.util.*;
import java.util.function.*;

/**
 * Provides a flood fill algorithm for {@link Graph} instances.
 * Starts on a specified starting node, and recursively finds any adjacent
 * nodes that match the conditions defined by a specified {@link Predicate}.
 * 
 * @param <T> the type of all nodes in the {@link Graph}
 * @author Christoph Nahr
 * @version 6.0.0
 */
public class FloodFill<T> {

    // search arguments & results
    private Predicate<T> _match;
    private final List<T> _nodes = new ArrayList<>(0);

    // hash set holding visited nodes
    private final HashSet<T> _visited = new HashSet<>();

    /**
     * The {@link Graph} on which all searches are performed.
     */
    public final Graph<T> graph;

    /**
     * Creates a {@link FloodFill} algorithm for the specified {@link Graph}.
     * @param graph the {@link Graph} on which all searches are performed
     * @throws NullPointerException if {@code graph} is {@code null}
     */
    public FloodFill(Graph<T> graph) {
        if (graph == null)
            throw new NullPointerException("graph");

        this.graph = graph;
    }

    /**
     * Finds all contiguous {@link #graph} nodes that match the specified conditions,
     * starting from the specified source node.
     * Returns {@code false} if {@link #graph} does not contain {@code source}, or
     * if there are no contiguous nodes for which {@code match} succeeds. Otherwise,
     * returns {@code true} and sets {@link #nodes} to the result of the flood fill.
     * 
     * @param match the {@link Predicate} to test against each {@link #graph} node
     * @param source the source node within {@link #graph} where the search starts
     * @return {@code true} if one or more {@link #graph} nodes that pass {@code match}
     *         could be reached from the specified {@code source}, else {@code false}
     * @throws NullPointerException if {@code match} or {@code source} is {@code null}
     */
    public boolean findMatching(Predicate<T> match, T source) {
        if (match == null)
            throw new NullPointerException("match");
        if (source == null)
            throw new NullPointerException("source");

        _match = match;

        // clear previous results
        _nodes.clear();

        // fail if source node is invalid
        if (!graph.contains(source))
            return false;

        // mark source node as visited
        _visited.add(source);

        // expand area around source
        expandArea(source);

        // clear intermediate data
        _match = null;
        _visited.clear();

        // succeed if any nodes reached
        return !_nodes.isEmpty();
    }

    /**
     * Gets all {@link #graph} nodes that were reached by the last successful search.
     * Returns an empty collection if the last {@link #findMatching} call returned
     * {@code false}, or if the method has not yet been called.
     * 
     * @return an unmodifiable {@link List} containing all {@link #graph} nodes that were reached
     *         by the last successful {@link #findMatching} call, not including the source node
     */
    public List<T> nodes() {
        return Collections.unmodifiableList(_nodes);
    }

    /**
     * Expands the current search area with all neighbors of the specified
     * {@link #graph} node for which the matching predicate succeeds.
     * Recursively visits all contiguous nodes for which the matching predicate succeeds,
     * and adds them to {@link #nodes}, excluding the source node. Never revisits nodes
     * that were already added or rejected.
     * 
     * @param node the {@link #graph} node whose neighbors to examine
     * @throws NullPointerException if {@code node} is {@code null}
     */
    private void expandArea(T node) {

        // recurse into all valid neighbors of current node
        final Collection<T> neighbors = graph.getNeighbors(node);
        for (T neighbor: neighbors) {

            // skip visited nodes
            if (_visited.contains(neighbor)) continue;
            _visited.add(neighbor);

            // add match and visit neighbors
            if (_match.test(neighbor)) {
                _nodes.add(neighbor);
                expandArea(neighbor);
            }
        }
    }
}
