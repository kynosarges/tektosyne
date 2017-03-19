package org.kynosarges.tektosyne.graph;

import java.util.*;
import org.kynosarges.tektosyne.geometry.*;

/**
 * Provides a navigable graph whose nodes map to regions in two-dimensional space.
 * Suitable for pathfinding and other graph algorithms. Makes no assumptions about topology,
 * except that nodes must map to two-dimensional space. Nodes cannot be {@code null} and must
 * provide meaningful {@link Object#equals} and {@link Object#hashCode} implementations.
 * 
 * @param <T> the type of all nodes in the {@link Graph}
 * @author Christoph Nahr
 * @version 6.0.0
 */
public interface Graph<T> {
    /**
     * Gets the maximum number of direct neighbors for any {@link Graph} node.
     * Always greater than zero. Direct neighbors of a given {@link Graph} node
     * are those that are directly connected, without any intermediate nodes.
     * 
     * @return the maximum number of direct neighbors for any {@link Graph} node
     */
    int connectivity();

    /**
     * Gets the total number of {@link #nodes} in the {@link Graph}.
     * Never less than zero.
     * 
     * @return the total number of {@link #nodes} in the {@link Graph}
     */
    int nodeCount();

    /**
     * Gets all nodes in the {@link Graph}.
     * Always contains {@link #nodeCount} elements. The element order depends
     * on the concrete implementation of {@link Graph}.
     * 
     * @return a {@link Collection} of all nodes in the {@link Graph}
     */
    Collection<T> nodes();

    /**
     * Determines whether the {@link Graph} contains the specified node.
     * @param node the {@link Graph} node to examine
     * @return {@code true} if the {@link Graph} contains {@code node}, else {@code false}
     * @throws NullPointerException if {@code node} is {@code null}
     */
    boolean contains(T node);

    /**
     * Finds the {@link Graph} node nearest to the specified {@link PointD} world location.
     * Always returns a valid node, even if the distance between its {@link #getWorldLocation}
     * result and {@code location} is very large. The returned node is not necessarily the one
     * whose {@link #getWorldRegion} result contains {@code location}.
     * 
     * @param location the {@link PointD} location, in world coordinates, to examine
     * @return the {@link Graph} node whose {@link #getWorldLocation} result
     *         is nearest to the specified {@code location}
     * @throws NullPointerException if {@code location} is {@code null}
     */
    T findNearestNode(PointD location);

    /**
     * Gets the distance between two specified {@link Graph} nodes.
     * Uses an arbitrary distance measurement, as documented by the implementation.
     * However, the result must always conform to the following invariants:
     * <ul>
     * <li>The distance between identical nodes is always zero.</li>
     * <li>The distance between different nodes is always positive.</li>
     * <li>The sum of the distances between all successive nodes within a sequence is
     * never less than the distance between any two nodes from the same sequence.</li>
     * <li>The distance between two nodes is always equal to or less than the result of
     * {@link GraphAgent#getStepCost} for the same two nodes.</li>
     * <li>The distance between two nodes remains unchanged if the arguments are reversed.</li>
     * <li>The distance is undefined if one or both nodes are invalid.
     * In this case only, the result may be negative.</li>
     * </ul>
     * 
     * @param source the source node in the {@link Graph}
     * @param target the target node in the {@link Graph}
     * @return the non-negative distance between {@code source} and {@code target}
     * @throws NullPointerException if {@code source} or {@code target} is {@code null}
     */
    double getDistance(T source, T target);

    /**
     * Gets all direct neighbors of the specified {@link Graph} node.
     * Returns an empty {@link Collection} if {@code node} or all its direct neighbors are
     * not part of the {@link Graph}. The returned {@link Collection} is the complete set
     * of target nodes for which {@link GraphAgent#canMakeStep} could possibly succeed,
     * given the specified {@code node} as the source node.
     * 
     * @param node the {@link Graph} node whose direct neighbors to collect
     * @return a {@link Collection} of all {@link Graph} nodes that are directly
     *         connected with {@code node}, numbering from zero to {@link #connectivity}
     * @throws NullPointerException if {@code node} is {@code null}
     */
    Collection<T> getNeighbors(T node);

    /**
     * Gets all neighbors of the specified {@link Graph} node within the specified step distance.
     * Returns an empty {@link Collection} if {@code node} or all its direct neighbors are
     * not part of the {@link Graph}. Otherwise, returns a {@link Collection} containing all
     * direct neighbors, direct neighbors of those neighbors, etc. repeated for {@code steps}.
     * <p>
     * The default implementation uses two lists for gathering direct neighbors to inspect,
     * and returns a {@link HashSet} with all valid neighbors found within {@code steps}. This
     * requires the node type <b>T</b> to offer a useful {@link Object#hashCode} function.
     * Specific {@link Graph} implementations may be able to use more efficient algorithms.</p>
     * 
     * @param node the {@link Graph} node whose neighbors within {@code steps} to collect
     * @param steps the distance around {@code node}, in movement steps,
     *              in which other nodes are considered neighbors of {@code node}
     * @return a {@link Collection} of all {@link Graph} nodes whose step distance from
     *         {@code node} is greater than zero, but equal to or less than {@code steps}
     * @throws IllegalArgumentException if {@code steps} is equal to or less than zero
     * @throws NullPointerException if {@code node} is {@code null}
     */
    default Collection<T> getNeighbors(T node, int steps) {
        if (steps <= 0)
            throw new IllegalArgumentException("steps <= 0");
        if (!contains(node))
            return Collections.emptySet();

        // return direct neighbors for single step
        if (steps == 1) return getNeighbors(node);

        final HashSet<T> neighbors = new HashSet<>();
        List<T> queue = new ArrayList<>(), nextQueue = new ArrayList<>(), swap;

        // start with source node, repeat for specified steps
        queue.add(node);
        for (int step = 0; step < steps; step++) {
            for (T queueNode: queue) {
                neighbors.add(queueNode);

                // add direct neighbors for further inspection
                for (T neighbor: getNeighbors(queueNode))
                    if (!neighbors.contains(neighbor))
                        nextQueue.add(neighbor);
            }

            // empty old queue, swap in filled nextQueue
            queue.clear();
            swap = queue; queue = nextQueue; nextQueue = swap;
        }

        // add final neighbors, remove source node
        neighbors.addAll(queue);
        neighbors.remove(node);

        return neighbors;
    }

    /**
     * Gets the world location of the specified {@link Graph} node.
     * Uses an arbitrary “world” coordinate system, as documented by the implementation.
     * The world location always resides within the result of {@link #getWorldRegion} for
     * the same {@code node}. The result of {@link #getDistance} for any two nodes is usually
     * the Euclidean distance between their world locations, but this is not required.
     * 
     * @param node the {@link Graph} node whose world location to find
     * @return the {@link PointD} location of {@code node}, in world coordinates
     * @throws NullPointerException if {@code node} is {@code null}
     */
    PointD getWorldLocation(T node);

    /**
     * Gets the world region covered by the specified {@link Graph} node.
     * Uses an arbitrary “world” coordinate system, as documented by the implementation.
     * The returned polygon is implicitly assumed to be closed, with an edge connecting its
     * last and first vertex. The polygon should be simple and enclose a positive area that
     * contains the result of {@link #getWorldLocation} for {@code node}.
     * 
     * @param node the {@link Graph} node whose world region to find
     * @return an array of {@link PointD} vertices defining the polygonal region
     *         covered by {@code node}, in world coordinates
     * @throws NullPointerException if {@code node} is {@code null}
     */
    PointD[] getWorldRegion(T node);
}
