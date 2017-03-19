package org.kynosarges.tektosyne.graph;

import java.util.*;
import org.kynosarges.tektosyne.geometry.*;

/**
 * Provides an A* pathfinding algorithm for {@link Graph} instances.
 * Finds the best path to move a {@link GraphAgent} from one specified {@link Graph} node
 * to another. The "best path" is the one whose total cost is minimal compared to all
 * other connecting paths, where the total cost of a path is defined as the sum of all
 * {@link GraphAgent#getStepCost} results for each step between two adjacent path nodes.
 * <p>
 * This implementation of the A* algorithm is based on the CAStar class created by James
 * Matthews. The original source code supplemented his article "Basic A* Pathfinding Made
 * Simple", pages 105-113 in "AI Game Programming Wisdom", Charles River Media, 2002.</p>
 * <p>
 * <b>Caution:</b> Multi-step movements are problematic when the {@link GraphAgent} uses
 * a non-trivial {@link GraphAgent#canOccupy} condition. After a successful path search,
 * {@link AStar#bestNode} is always valid and optimal for a single-step movement. However,
 * calling {@link AStar#getLastNode} with less than the maximum path cost may yield a
 * suboptimal intermediate node, or none at all. Since the algorithm does not know whether
 * a given intermediate path node will be occupied permanently or merely traversed, the final
 * path may contain many intermediate nodes for which {@link GraphAgent#canOccupy} fails.</p>
 * 
 * @param <T> the type of all nodes in the {@link Graph}
 * @author Christoph Nahr
 * @version 6.0.0
 */
public class AStar<T> implements GraphPath<T> {

    // search arguments
    private GraphAgent<T> _agent;
    private T _source, _target;
    private double _relativeLimit;
    private PointD _targetWorld;

    // search results
    private double _absoluteLimit;
    private PathNode<T> _bestNode;
    private final List<T> _nodes = new ArrayList<>(0);

    // node lists and parent stack
    private PathNode<T> _openList;
    private final Stack<PathNode<T>> _parents = new Stack<>();
    private final Map<T, PathNode<T>> _openTable = new HashMap<>(0);
    private final Map<T, PathNode<T>> _closedTable = new HashMap<>(0);

    /**
     * The {@link Graph} on which all searches are performed.
     */
    public final Graph<T> graph;

    /**
     * Determines whether {@link AStar} should prefer path nodes with a minimal target distance.
     * {@code true} to prefer path nodes with a minimal distance from the target node,
     * in world coordinates, else {@code false}. The default is {@code false}.
     * <p>
     * When {@code true}, candidate path nodes whose path costs are identical are also
     * compared for their distances from the target node, using the world coordinates
     * returned by {@link Graph#getWorldLocation}.</p>
     * <p>
     * This eliminates zero-cost oscillations in the final path, creating a smoother and
     * more "natural" course. However, the additional calculations and comparisons may slow
     * down pathfinding. {@link #useWorldDistance} has no effect if {@link Graph#getDistance}
     * already uses world coordinates.</p>
     */
    public boolean useWorldDistance;

    /**
     * Creates an {@link AStar} algorithm for the specified {@link Graph}.
     * @param graph the {@link Graph} on which all searches are performed
     * @throws NullPointerException if {@code graph} is {@code null}
     */
    public AStar(Graph<T> graph) {
        if (graph == null)
            throw new NullPointerException("graph");

        this.graph = graph;
    }

    /**
     * Finds the best path to move the specified {@link GraphAgent}
     * from one specified {@link #graph} node to another.
     * Returns {@code false} if {@link #graph} does not contain both {@code source}
     * and {@code target}, or if no connecting path could be found. Otherwise,
     * returns {@code true} and sets {@link #bestNode}, {@link #nodes}, and
     * {@link #totalCost} to the results of the path search.
     * <p>
     * {@code findBestPath} calls {@link GraphAgent#canOccupy} and {@link
     * GraphAgent#isNearTarget} on the specified {@code agent} to determine whether a
     * given {@link #bestNode} candidate is acceptable. Depending on the implementation
     * of {@code isNearTarget}, the {@link PathNode#node} of the final {@link #bestNode}
     * may differ from the specified {@code target}, and possibly equal the specified
     * {@code source}. {@code canOccupy} is never called on the {@code source} node.</p>
     * <p>
     * {@code findBestPath} operates with a <em>restricted search radius</em> if {@link
     * #relativeLimit} is greater than zero. In this case, {@link #absoluteLimit} is set
     * to the product (rounded up) of {@link #relativeLimit} and the distance between
     * {@code source} and {@code target}. Whenever a node is considered for inclusion in
     * the search path, its distances from {@code source} and {@code target} are calculated,
     * and the node is ignored if the sum exceeds {@link #absoluteLimit}.</p>
     * 
     * @param agent the {@link GraphAgent} that performs the movement
     * @param source the source node within {@link #graph}
     * @param target the source node within {@link #graph}
     * @return {@code true} if a best path between {@code source} and {@code target}
     *         could be found, else {@code false}
     * @throws NullPointerException if any argument is {@code null}
     */
    public boolean findBestPath(GraphAgent<T> agent, T source, T target) {
        if (agent == null)
            throw new NullPointerException("agent");
        if (source == null)
            throw new NullPointerException("source");
        if (target == null)
            throw new NullPointerException("target");

        _agent = agent;
        _source = source;
        _target = target;

        // clear previous results
        _absoluteLimit = 0;
        _bestNode = null;
        _nodes.clear();
        _targetWorld = PointD.EMPTY;

        // fail if either node is invalid
        if (!graph.contains(source) || !graph.contains(target))
            return false;

        // compute absolute distance limit if desired
        final double distance = graph.getDistance(source, target);
        if (_relativeLimit > 0)
            _absoluteLimit = distance * _relativeLimit;

        // compute world distance to target if desired
        if (useWorldDistance)
            _targetWorld = graph.getWorldLocation(target);

        // initialize search list
        _openList = new PathNode<>(source, graph.connectivity());
        _openList._h = distance;

        boolean success = false;
        while (setBestNode()) {
            T node = _bestNode.node;

            // succeed if occupation target is in range
            if (_agent.isNearTarget(node, target, _bestNode._h) &&
                (Objects.equals(source, node) || _agent.canOccupy(node))) {
                success = true;
                break;
            }

            // add children to search space
            createChildren(_bestNode);
        }

        // clear intermediate data
        _source = _target = null;
        assert(_parents.isEmpty());
        _openList = null;
        _openTable.clear();
        _closedTable.clear();

        return success;
    }

    /**
     * Gets the absolute limit on the search radius for the last path search.
     * Zero indicates that there was no limit on the search radius. Otherwise,
     * nodes that exceed {@link #absoluteLimit} are not added to the search path.
     * <p>
     * Set by {@link #findBestPath} depending on the current {@link #relativeLimit}.
     * See there for further details.</p>
     * 
     * @return the maximum sum of the distances, according to {@link Graph#getDistance}, between
     *         any one {@link #graph} node in the search path and the source and target nodes
     */
    public double absoluteLimit() {
        return _absoluteLimit;
    }

    /**
     * Gets the {@link GraphAgent} for the last search.
     * @return the {@link GraphAgent} that was supplied to the last {@link #findBestPath}
     *         call, or {@code null} if the method has not yet been called
     */
    public GraphAgent<T> agent() {
        return _agent;
    }

    /**
     * Gets the final node found by the last successful path search.
     * Returns {@code null} if the last {@link #findBestPath} call returned
     * {@code false}, or if the method has not yet been called.
     * <p>
     * Otherwise, the best path ended by {@link #bestNode} is stored in {@link #nodes}
     * by backtracking through the {@link PathNode#parent} links of all connected
     * {@link PathNode} instances.</p>
     * 
     * @return the {@link PathNode} that represents the target node of the last successful
     *         {@link #findBestPath} call, ending the best path
     */
    public PathNode<T> bestNode() {
        return _bestNode;
    }

    /**
     * Gets the last {@link #graph} node in the best path whose
     * total path cost does not exceed the specified maximum cost.
     * Returns the {@link PathNode#node} associated with the {@link PathNode}
     * found by {@link getLastPathNode} for {@code maxCost}. See there for details.
     * 
     * @param maxCost the maximum total path cost of the {@link #graph} node
     * @return the {@link PathNode#node} of the last {@link #nodes} element
     *         whose total path cost does not exceed {@code maxCost}
     * @throws IllegalArgumentException if {@code maxCost} is equal to or less than zero
     * @throws IllegalStateException if {@link #bestNode} is {@code null}
     */
    @Override
    public T getLastNode(double maxCost) {
        return getLastPathNode(maxCost).node;
    }

    /**
     * Gets the last {@link PathNode} in the best path whose
     * total path cost does not exceed the specified maximum cost.
     * Always returns a {@link PathNode} whose {@link PathNode#node} is an element
     * of {@link #nodes}. The exact element depends on the specified {@code maxCost}.
     * <p>
     * {@code getLastPathNode} searches for the {@link PathNode} that is the last
     * {@link PathNode#parent} of {@link #bestNode} whose {@link PathNode#g} value
     * does not exceed the specified {@code maxCost}, and for which {@link
     * GraphAgent#canOccupy} succeeds with the moving {@link #agent}.</p>
     * <p>
     * If {@link GraphAgent#relaxedRange} is {@code true} for the moving {@link #agent},
     * the {@link PathNode#g} value of the returned {@link PathNode} may exceed
     * {@code maxCost} if the {@link PathNode#g} value of its {@link PathNode#parent}
     * node is strictly less than {@code maxCost}.</p>
     * <p>
     * If the specified {@code maxCost} exceeds the cost of all nodes, or if {@link
     * GraphAgent#canOccupy} fails for all affordable nodes, {@code getLastPathNode}
     * returns the {@link PathNode} that corresponds to the first {@link #nodes} element,
     * i.e. the source node of the path search.</p>
     * 
     * @param maxCost the maximum total path cost of the {@link PathNode}
     * @return the {@link PathNode} that is the last parent of {@link #bestNode}
     *         whose total path cost does not exceed {@code maxCost}
     * @throws IllegalArgumentException if {@code maxCost} is equal to or less than zero
     * @throws IllegalStateException if {@link #bestNode} is {@code null}
     */
    public PathNode<T> getLastPathNode(double maxCost) {
        if (maxCost <= 0)
            throw new IllegalArgumentException("maxCost <= 0");
        if (_bestNode == null)
            throw new IllegalStateException("bestNode == null");

        /*
         * Go backward starting at bestNode and check these conditions:
         * 
         * 1. cursor.parent is null -- we have arrived at the source node and have
         *    nowhere else to go, so we return the source node.
         * 
         * 2. cursor.g <= maxCost -- the current node is no more expensive than maxCost.
         * 
         * 3. cursor.parent.g < maxCost -- Agent.relaxedRange means we can enter any node
         *    if we have even one movement point left, so we return the current node.
         * 
         * 4. Agent.canOccupy(cursor.node) -- always check that the Agent’s movement
         *    can end on the current node.
         */
        PathNode<T> cursor = _bestNode;
        final boolean relaxed = _agent.relaxedRange();

        while (true) {
            final PathNode<T> parent = cursor._parent;
            if (parent == null) return cursor;

            if ((cursor._g <= maxCost || (relaxed && parent._g < maxCost))
                && _agent.canOccupy(cursor.node))
                return cursor;

            cursor = parent;
        }
    }

    /**
     * Gets all {@link #graph} nodes in the best path found by the last successful path search.
     * Returns an empty collection if the last {@link #findBestPath} call returned
     * {@code false}, or if the method has not yet been called.
     * <p>
     * Otherwise, first element is always the source node, and the last element is
     * always the {@link PathNode#node} represented by {@link #bestNode}.</p>
     * 
     * @return an unmodifiable {@link List} containing all {@link #graph} nodes that constitute
     *         the best movement path found by the last successful {@link #findBestPath} call,
     *         including source and target node
     */
    @Override
    public List<T> nodes() {
        // create path if necessary and possible
        if (_nodes.isEmpty() && _bestNode != null) {

            // traverse along parent relationships
            for (PathNode<T> cursor = _bestNode; cursor != null; cursor = cursor._parent)
                _nodes.add(cursor.node);

            // reverse path for standard ordering
            Collections.reverse(_nodes);
        }

        return Collections.unmodifiableList(_nodes);
    }

    /**
     * Gets the limit on the search radius, relative to the distance between source and target node.
     * The default is zero, indicating that there is no limit on the search radius.
     * When not zero, {@link #relativeLimit} is always equal to or greater than one.
     * <p>
     * See {@link #absoluteLimit}, {@link #findBestPath}, and {@link #setRelativeLimit}
     * for further details.</p>
     * 
     * @return the factor to multiply with the distance between source and target node, according to
     *         {@link Graph#getDistance}, to obtain the {@link absoluteLimit} on the search radius
     */
    public double relativeLimit() {
        return _relativeLimit;
    }

    /**
     * Sets the limit on the search radius, relative to the distance between source and target node.
     * Zero indicates that there is no limit on the search radius. A positive value restricts
     * the search path to an elliptical area. The source and target nodes are the focal points
     * of the ellipse, and {@link #relativeLimit} defines its inverse eccentricity.
     * <p>
     * See {@link #absoluteLimit} and {@link #findBestPath} for further details.</p>
     * 
     * @param value the factor to multiply with the distance between source and target node, according to
     *              {@link Graph#getDistance}, to obtain the {@link absoluteLimit} on the search radius
     * @throws IllegalArgumentException if {@code value} is less than zero,
     *                                  or greater than zero but smaller than one
     */    
    public void setRelativeLimit(double value) {
        if (value < 0)
            throw new IllegalArgumentException("value < 0");
        if (value > 0 && value < 1)
            throw new IllegalArgumentException("value > 0 && value < 1");

        _relativeLimit = value;
    }

    /**
     * Gets the total cost of the best path found by the last successful path search.
     * Returns -1 if {@link #bestNode} is {@code null}. This is the case if the last {@link
     * #findBestPath} call returned {@code false}, or if the method has not yet been called.
     * 
     * @return the {@link PathNode#g} value of {@link #bestNode}, which is the sum
     *         of the {@link GraphAgent#getStepCost} results for all {@link #nodes}
     */
    @Override
    public double totalCost() {
        return (_bestNode == null ? -1 : _bestNode._g);
    }

    /**
     * Adds all reachable neighbors as children to the specified parent {@link PathNode}.
     * @param parent the {@link PathNode} whose {@link #graph} neighbors to examine
     * @throws NullPointerException if {@code parent} is {@code null}
     */
    private void createChildren(PathNode<T> parent) {
        T source = parent.node;

        // link all direct neighbors that can be reached
        final Collection<T> neighbors = graph.getNeighbors(source);
        for (T neighbor: neighbors)
            if (_agent.canMakeStep(source, neighbor))
                linkChild(parent, neighbor);
    }

    /**
     * Returns the squared world distance between the specified {@link PathNode} and the target node.
     * @param node the {@link PathNode} whose distance to the target node to compute
     * @return the squared distance, in world coordinates, from {@code node} to the target node
     * @throws NullPointerException if {@code node} is {@code null}
     */
    private double getWorldDistance(PathNode<T> node) {

        final PointD nodeWorld = graph.getWorldLocation(node.node);
        final double x = nodeWorld.x - _targetWorld.x;
        final double y = nodeWorld.y - _targetWorld.y;

        return (x * x + y * y);
    }

    /**
     * Links the specified child {@link #graph} node to the specified parent {@link PathNode}.
     * Searches the open and closed tables for the specified {@code graphChild},
     * and creates a new open list node if no matching node was found.
     * 
     * @param parent the parent {@link PathNode} to link with {@code graphChild}
     * @param graphChild he child {@link #graph} node to link with {@code parent}
     * @throws NullPointerException if {@code parent} or {@code graphChild} is {@code null}
     */
    private void linkChild(PathNode<T> parent, T graphChild) {

        // total cost to reach child via parent
        double g = parent._g + _agent.getStepCost(parent.node, graphChild);
        assert(g > parent._g);

        // look for child node in open table
        PathNode<T> child = _openTable.get(graphChild);
        if (child != null) {
            parent._children.add(child);

            // switch to better route
            if (child._g > g) {
                child._g = g;
                child._parent = parent;
            }
            return;
        }

        // look for child node in closed table
        child = _closedTable.get(graphChild);
        if (child != null) {
            parent._children.add(child);

            // switch to better route
            if (child._g > g) {
                child._g = g;
                child._parent = parent;

                // also update parents
                updateParents(child);
            }
            return;
        }

        // compute distance from target
        final double fromTarget = graph.getDistance(graphChild, _target);

        // apply distance limit if desired
        if (_absoluteLimit > 0) {

            // compute distance from source
            final double fromSource = graph.getDistance(_source, graphChild);

            // ignore child if sum exceeds limit
            if (fromSource + fromTarget > _absoluteLimit)
                return;
        }

        // create new path node for child
        child = new PathNode<>(graphChild, graph.connectivity());
        parent._children.add(child);

        child._g = g;
        child._h = fromTarget;
        child._parent = parent;

        // prepend child to open list & table
        child._next = _openList;
        _openList = child;
        _openTable.put(graphChild, child);
    }

    /**
     * Sets {@link #bestNode} to the open list node with the lowest {@link PathNode#f} value.
     * {@link #bestNode} is {@code null} exactly if {@link #setBestNode} returns {@code false}.
     * 
     * @return {@code true} if the open list contained another node that was removed
     *         and stored as the new {@link #bestNode}, else {@code false}
     */
    private boolean setBestNode() {

        // no nodes left to search
        if (_openList == null) {
            _bestNode = null;
            return false;
        }

        // get first open list node
        _bestNode = _openList;
        double bestF = _bestNode.f();
        PathNode<T> previous = null;

        if (useWorldDistance) {
            double bestDistance = getWorldDistance(_bestNode);

            // walk remaining list to find better node
            for (PathNode<T> preCursor = _openList, cursor = preCursor._next;
                cursor != null; preCursor = cursor, cursor = cursor._next) {

                final double cursorDistance = getWorldDistance(cursor);

                // update to new best node
                if (cursor.f() < bestF ||
                    (cursor.f() == bestF && cursorDistance < bestDistance)) {

                    previous = preCursor;
                    _bestNode = cursor;
                    bestF = cursor.f();
                    bestDistance = cursorDistance;
                }
            }
        } else {
            // walk remaining list to find better node
            for (PathNode<T> preCursor = _openList, cursor = preCursor._next;
                cursor != null; preCursor = cursor, cursor = cursor._next) {

                // update to new best node
                if (cursor.f() < bestF) {
                    previous = preCursor;
                    _bestNode = cursor;
                    bestF = cursor.f();
                }
            }
        }

        // remove best node from open list
        if (previous == null)
            _openList = _openList._next;
        else
            previous._next = _bestNode._next;

        // move best node to closed table
        T graphNode = _bestNode.node;
        _openTable.remove(graphNode);
        _closedTable.put(graphNode, _bestNode);

        return true;
    }

    /**
     * Updates all parents of all children of the specified {@link PathNode}
     * to reflect lowered path costs.
     * All possible paths are represented by {@link PathNode#parent} links through the current
     * {@link PathNode} collection. If an existing node has been reached by a new and cheaper
     * path, the cost for all paths that involve this node must be updated accordingly.
     * 
     * @param node a closed list {@link PathNode} that has been reached by a new path
     * @throws NullPointerException if {@code node} is {@code null}
     */
    private void updateParents(PathNode<T> node) {
        assert(_parents.isEmpty());

        // specified node is first parent
        _parents.push(node);

        // continue while we have parents
        while (_parents.size() > 0) {
            final PathNode<T> parent = _parents.pop();

            // check total costs of all children
            final List<PathNode<T>> children = parent._children;
            for (int i = 0; i < children.size(); i++) {
                final PathNode<T> child = children.get(i);

                // skip children with better costs
                if (child._g <= parent._g) continue;

                // total cost to reach child via parent
                double g = parent._g + _agent.getStepCost(parent.node, child.node);
                assert(g > parent._g);

                // switch to better route
                if (child._g > g) {
                    child._g = g;
                    child._parent = parent;

                    // child is next parent
                    _parents.push(child);
                }
            }
        }
    }
}
