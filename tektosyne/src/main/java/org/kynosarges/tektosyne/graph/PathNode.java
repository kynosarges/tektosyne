package org.kynosarges.tektosyne.graph;

import java.util.*;

/**
 * Provides a node within {@link AStar} search paths.
 * Associates a {@link Graph} node with various other data
 * required by the {@link AStar} pathfinding algorithm.
 * 
 * @param <T> the type of all nodes in the {@link Graph}
 * @author Christoph Nahr
 * @version 6.0.0
 */
public final class PathNode<T> {
    /**
     * All accessible direct neighbors of the current {@link #node}
     * that were examined during the path search.
     */
    List<PathNode<T>> _children;
    /**
     * The total cost from the source to the current {@link #node}.
     */
    double _g;
    /**
     * The estimated total cost from the current {@link #node} to the target.
     */
    double _h;
    /**
     * The next {@link PathNode} in a linked list, or {@code null}
     * for the last list node or an as yet unlinked node.
     */
    PathNode<T> _next;
    /**
     * The parent of the {@link PathNode}.
     */
    PathNode<T> _parent;

    /**
     * Creates a {@link PathNode} for the specified {@link Graph} node.
     * The specified {@code childCapacity} should equal the {@link
     * Graph#connectivity} of the {@link Graph} on which the path search
     * is performed, so as to avoid costly reallocations.
     * 
     * @param node the {@link Graph} node that the {@link PathNode} represents
     * @param childCapacity the initial capacity of the {@link #children} collection
     * @throws IllegalArgumentException if {@code childCapacity} is less than zero
     * @throws NullPointerException if {@code node} is {@code null}
     */
    PathNode(T node, int childCapacity) {
        if (node == null)
            throw new NullPointerException("node");

        this.node = node;
        this._children = new ArrayList<>(childCapacity);
    }

    /**
     * Gets all accessible direct neighbors of the current {@link #node}
     * that were examined during the path search.
     * {@link AStar} adds elements to the {@link #children} collection
     * while expanding the current search path.
     * 
     * @return an unmodifiable {@link List} of all accessible direct neighbors of the
     *         current {@link #node} that were examined during the path search
     */
    public List<PathNode<T>> children() {
        return Collections.unmodifiableList(_children);
    }

    /**
     * Gets the estimated total cost of the search path.
     * Returns the estimated total cost of the path from source to target
     * that leads across the current {@link #node}.
     * 
     * @return the sum of {@link #g} and {@link #h}
     */
    public double f() {
        return _g + _h;
    }

    /**
     * Gets the total cost of the search path up to the current {@link #node}.
     * Known quantity that represents the total cost to move from the source node
     * to the current {@link #node}, along the path defined by the chain of
     * {@link parent} links. Zero if the current {@link #node} is the source node.
     * 
     * @return the total cost of the search path from source to current {@link #node}
     */
    public double g() {
        return _g;
    }

    /**
     * Gets the estimated total cost of the search path from the current {@link #node}.
     * Estimated quantity that represents the total cost to move from the current
     * {@link #node} to the target node. This estimate is usually obtained by calling
     * {@link Graph#getDistance}. Zero if the current {@link #node} is the target node.
     * 
     * @return the estimated total cost from current {@link #node} to target
     */
    public double h() {
        return _h;
    }

    /**
     * The {@link Graph} node that the {@link PathNode} represents.
     * The {@link #node} of the first {@link PathNode} in a search path is the
     * source node, and the {@link #node} of the last {@link PathNode} is the
     * target node, assuming that pathfinding was successful.
     */
    public final T node;

    /**
     * Gets the parent of the {@link PathNode}.
     * Tracing back through the {@link #parent} links of all {@link PathNode}
     * instances in a search path eventually leads back to the source node.
     * 
     * @return the preceding {@link PathNode} in a path that starts at the source node,
     *         or {@code null} for the source node itself
     */
    public PathNode<T> parent() {
        return _parent;
    }
}
