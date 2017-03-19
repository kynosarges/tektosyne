package org.kynosarges.tektosyne.graph;

import java.util.*;
import java.util.function.*;

import org.kynosarges.tektosyne.geometry.*;

/**
 * Provides a line-of-sight algorithm for {@link Graph} instances.
 * Starts on a specified starting node and recursively finds any adjacent nodes,
 * up to a specified maximum world distance, whose line of sight is not blocked by
 * any other examined nodes closer to the source. The opacity of any given node is
 * determined by a user-supplied {@link Predicate}.
 * <p>
 * For each tested node, we consider its polygonal world region as defined by {@link
 * Graph#getWorldRegion}. We draw the two tangents from the source node’s world location
 * to the extreme points of that region. An opaque node blocks visibility, across the angle
 * between its tangents, for any node whose nearest world region vertex is farther from the
 * source than that of the opaque node. A node is considered visible as long as a certain
 * fraction of its tangential arc remains unobscured, as defined by {@link #threshold}.</p>
 * <p>
 * If an opaque node obscures only the middle part of another node’s tangential arc, but
 * leaves partial arcs on both ends visible, only the greater of these partial arcs is
 * considered visible whereas the smaller is considered obscured. This simplifies visibility
 * testing, although very rarely visible nodes may be misclassified as obscured if the size
 * of world regions varies greatly among {@link Graph} nodes.</p>
 * <p>
 * Any graph node for which {@link Graph#getWorldRegion} returns {@code null} is assigned
 * a default tangential arc, spanning one degree around its world location. This allows the
 * {@link Visibility} algorithm to process graphs that do not define world regions for all
 * nodes, although the results are likely not very useful.</p>
 * 
 * @param <T> the type of all nodes in the {@link Graph}
 * @author Christoph Nahr
 * @version 6.0.0
 */
public class Visibility<T> {

    // search arguments
    private T _source;
    private PointD _sourceWorld;
    private Predicate<T> _isOpaque;
    private double _distance;

    // visibility threshold and output collections
    private double _threshold = 1.0 / 3.0;
    private final List<T> _nodes = new ArrayList<>();
    private final Map<T, NodeArc> _nodeArcs = new HashMap<>();

    // visible nodes that obscure the view on other nodes
    private final Map<T, NodeArc> _obscuringNodes = new HashMap<>();

    // nodes to remove from collection of obscuring nodes
    private final List<T> _removeNodes = new ArrayList<>();

    /**
     * The {@link Graph} on which all searches are performed.
     */
    public final Graph<T> graph;

    /**
     * Creates a {@link Visibility} algorithm for the specified {@link Graph}.
     * @param graph the {@link Graph} on which all searches are performed
     * @throws NullPointerException if {@code graph} is {@code null}
     */
    public Visibility(Graph<T> graph) {
        if (graph == null)
            throw new NullPointerException("graph");

        this.graph = graph;
    }

    /**
     * Finds all contiguous {@link #graph} nodes within the specified maximum
     * world distance that are visible from the specified source node.
     * Returns {@code false} if {@link #graph} does not contain {@code source},
     * or if there are no visible nodes. Otherwise, returns {@code true} and sets
     * {@link #nodes} and {@link #nodeArcs} to the result of the visibility search.
     * <p>
     * All nodes within the specified maximum {@code distance} are considered visible,
     * except for those that are obscured by a node for which {@code isOpaque} succeeds,
     * as described in the documentation of the {@link Visibility} class.</p>
     * <p>
     * If {@code distance} is positive, any visible node must be reachable by a path
     * that only includes other nodes within {@code distance}; otherwise, it will not
     * be found. This condition holds for any {@link PolygonGrid}, and for any
     * {@link org.kynosarges.tektosyne.subdivision.Subdivision}
     * that was created from a Delaunay triangulation.</p>
     * 
     * @param isOpaque the {@link Predicate} that determines whether a {@link #graph}
     *                 node blocks the line of sight
     * @param source the source node within {@link #graph} where the search starts
     * @param distance the maximum world distance from {@code source} to search,
     *                 or zero to search the entire {@link #graph}
     * @return {@code true} if one or more nodes are visible from {@code source}
     *         within the specified {@code distance}, else {@code false}
     * @throws IllegalArgumentException if {@code distance} is less than zero
     * @throws NullPointerException if {@code isOpaque} or {@code source} is {@code null}
     */
    public boolean findVisible(Predicate<T> isOpaque, T source, double distance) {
        if (isOpaque == null)
            throw new NullPointerException("isOpaque");
        if (source == null)
            throw new NullPointerException("source");
        if (distance < 0)
            throw new IllegalArgumentException("distance < 0");

        // clear previous results
        _nodes.clear();
        _nodeArcs.clear();

        // fail if source node is invalid
        if (!graph.contains(source))
            return false;

        _isOpaque = isOpaque;
        _source = source;
        _distance = distance;

        // compute world coordinates of source node
        _sourceWorld = graph.getWorldLocation(source);

        // expand visibility area from source
        findObscuringNodes(source);
        findVisibleNodes();

        // clear intermediate data
        _isOpaque = null;
        _source = null;
        _obscuringNodes.clear();

        // succeed if any nodes reached
        return !_nodes.isEmpty();
    }

    /**
     * Gets all {@link #graph} nodes that were reached by the last successful search.
     * Returns an empty collection if the last {@link #findVisible} call returned
     * {@code false}, or if the method has not yet been called.
     * <p>
     * Otherwise, contains those {@link #graph} nodes in {@link #nodeArcs} whose {@link
     * NodeArc#visibleFraction} equals or exceeds the current {@link #threshold}.</p>
     * 
     * @return an unmodifiable {@link List} containing all {@link #graph} nodes that were reached
     *         by the last successful {@link #findVisible} call, not including the source node
     */
    public List<T> nodes() {
        return Collections.unmodifiableList(_nodes);
    }

    /**
     * Gets the source distances, tangential arcs, and visible fractions for all
     * {@link #graph} nodes that were examined by the last search.
     * Returns an empty collection if the last {@link #findVisible} call returned
     * {@code false}, or if the method has not yet been called.
     * <p>
     * Otherwise, contains all {@link #graph} nodes that were examined by {@link
     * #findVisible}, including partly or fully obscured nodes that were not added
     * to {@link #nodes}, but excluding the source node.</p>
     * 
     * @return an immutable {@link Map} that maps all visited {@link #graph} nodes to the
     *         corresponding {@link NodeArc} instances created by {@link #findVisible}
     */
    public Map<T, NodeArc> nodeArcs() {
        return Collections.unmodifiableMap(_nodeArcs);
    }

    /**
     * Gets the visibility threshold for any {@link #graph} node,
     * as a fraction of the sweep angle of its tangential arc.
     * The default is 1/3.
     * 
     * @return the minimum {@link NodeArc#visibleFraction} for the {@link NodeArc}
     *         of any {@link #graph} node that should be considered visible
     */
    public double threshold() {
        return _threshold;
    }

    /**
     * Sets the visibility threshold for any {@link #graph} node,
     * as a fraction of the sweep angle of its tangential arc.
     * A {@code value} of zero is interpreted as {@link Double#MIN_NORMAL}, so a
     * {@link #graph} node is considered visible while even the smallest fraction
     * of its tangential arc remains unobscured.
     * <p>
     * A {@code value} of one has the opposite effect. A {@link #graph} node is
     * considered visible only if its entire tangential arc remains unobscured.
     * Values between zero and one allow greater or lesser degrees of obscuration.</p>
     * 
     * @param value the minimum {@link NodeArc#visibleFraction} for the {@link NodeArc}
     *              of any {@link #graph} node that should be considered visible
     * @throws IllegalArgumentException if {@code value} is less than zero or greater than one
     */
    public void setThreshold(double value) {
        if (value < 0 || value > 1)
            throw new IllegalArgumentException("value < 0 || value > 1");

        if (value == 0) value = Double.MIN_NORMAL;
        _threshold = value;
    }

    /**
     * Creates the {@link NodeArc} for the specified target node.
     * 
     * @param target the {@link #graph} node whose {@link NodeArc} to create
     * @return a new {@link NodeArc} for the specified {@code target} node
     * @throws NullPointerException if {@code target} is {@code null}
     */
    private NodeArc createNodeArc(T target) {

        // compute world coordinates of target node
        final PointD targetWorld = graph.getWorldLocation(target);
        final PointD[] region = graph.getWorldRegion(target);

        // alpha is angle from source to center of target node
        final LineD line = new LineD(_sourceWorld, targetWorld);
        double alpha = line.angle();

        // use central location with 1° arc if no region available
        if (region == null) {
            alpha -= Angle.DEGREES_TO_RADIANS / 2;
            if (alpha <= -Math.PI) alpha += 2 * Math.PI;
            return new NodeArc(alpha, Angle.DEGREES_TO_RADIANS, line.length());
        }

        double distance = Double.MAX_VALUE;
        double minBeta = 0, maxBeta = 0;

        for (PointD vertex: region) {
            // find extreme relative angles for any region vertex
            final double beta = _sourceWorld.angleBetween(targetWorld, vertex);
            if (beta <= minBeta)
                minBeta = beta;
            else if (beta >= maxBeta)
                maxBeta = beta;

            // find smallest distance for any region vertex
            final double vertexDistance = vertex.subtract(_sourceWorld).length();
            if (distance > vertexDistance)
                distance = vertexDistance;
        }

        return new NodeArc(alpha + minBeta, maxBeta - minBeta, distance);
    }

    /**
     * Expands the current collection of obscuring {@link #graph} nodes with all neighbors
     * of the specified node, within maximum world distance from the source node.
     * Recursively visits all directly connected nodes, and adds them to an internal
     * collection of obscuring nodes if they are opaque. Nodes which are fully obscured
     * by other obscuring nodes are removed from the collection.
     * <p>
     * Never revisits nodes that were already examined. All visited nodes are added to
     * {@link #nodeArcs} for later processing by {@link #findVisibleNodes}.</p>
     * 
     * @param node the {@link #graph} node whose neighbors to examine
     * @throws NullPointerException if {@code node} is {@code null}
     */
    private void findObscuringNodes(T node) {

        // recurse into all valid neighbors of current node
        final Collection<T> neighbors = graph.getNeighbors(node);
        for (T neighbor: neighbors) {

            // skip source and previously visited nodes
            if (Objects.equals(_source, neighbor) || _nodeArcs.containsKey(neighbor))
                continue;

            // compute tangential arc and source distance
            final NodeArc arc = createNodeArc(neighbor);

            // skip nodes beyond maximum distance
            if (_distance > 0 && arc.distance > _distance)
                continue;

            // record visited node with tangential arc
            _nodeArcs.put(neighbor, arc);

        nextNeighbor: {
                // nothing else to do for transparent nodes
                if (!_isOpaque.test(neighbor))
                    break nextNeighbor;

                /*
                 * Try adding current opaque node to list of all obscuring nodes recorded so far.
                 * 
                 * If any single recorded node completely obscures the current node, we skip it.
                 * If the current node completely obscures any recorded nodes, we delete those.
                 * 
                 * We also clear the visiblityFraction for all completely obscured nodes (current
                 * or recorded) so we won't waste time testing them again in findVisibleNodes.
                 */
                for (Map.Entry<T, NodeArc> pair: _obscuringNodes.entrySet()) { 
                    final int result = arc.isObscured(pair.getValue());

                    if (result < 0) {
                        arc._visibleFraction = 0;
                        break nextNeighbor;
                    }
                    else if (result > 0) {
                        pair.getValue()._visibleFraction = 0;
                        _removeNodes.add(pair.getKey());
                    }
                }

                // remove obscuring nodes that were themselves obscured
                for (T removeNode: _removeNodes)
                    _obscuringNodes.remove(removeNode);
                _removeNodes.clear();

                // add neighbor to obscuring nodes
                _obscuringNodes.put(neighbor, arc);
            }

            findObscuringNodes(neighbor);
        }
    }

    /**
     * Expands the current visibility area with all visible {@link #graph} nodes,
     * within maximum world distance from the source node.
     * Iterates over all {@link #nodeArcs} found by {@link #findObscuringNodes},
     * and adjusts their {@link NodeArc#visibleFraction} according to the collection
     * of obscuring nodes also created by that method.
     * <p>
     * Any node whose {@link NodeArc} remains unobscured by at least the current
     * {@link #threshold} is added to the {@link #nodes} collection.</p>
     */
    private void findVisibleNodes() {

    nextVisited:
        // iterate over all visited nodes that may be visible
        for (Map.Entry<T, NodeArc> pair: _nodeArcs.entrySet()) {
            final NodeArc arc = pair.getValue();
            if (arc._visibleFraction == 0) continue;

            // obscure copy of arc, to retain original values
            final NodeArc obscuredArc = new NodeArc(arc);

            // compare visited node to all obscuring nodes (except itself)
            for (Map.Entry<T, NodeArc> obscuringPair: _obscuringNodes.entrySet()) {
                if (Objects.equals(pair.getKey(), obscuringPair.getKey()))
                    continue;

                // skip obscuring nodes at equal or greater source distance
                final NodeArc obscuringAngle = obscuringPair.getValue();
                if (obscuringAngle.distance >= arc.distance)
                    continue;

                // obscure visibility arc of visited node
                obscuringAngle.obscure(obscuredArc);
                arc._visibleFraction = obscuredArc.sweep() / arc.sweep();

                // check if arc is sufficiently obscured
                if (arc._visibleFraction < _threshold)
                    continue nextVisited;
            }

            // add visible node to search results
            _nodes.add(pair.getKey());
        }
    }
}
