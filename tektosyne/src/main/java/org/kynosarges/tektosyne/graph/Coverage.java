package org.kynosarges.tektosyne.graph;

import java.util.*;

/**
 * Provides a path coverage algorithm for {@link Graph} instances.
 * Finds all {@link Graph} nodes that a {@link GraphAgent} can reach by any movement path that
 * starts on a specified node, and whose total cost does not exceed a specified maximum cost.
 * <p>
 * The total path cost is defined as the sum of all {@link GraphAgent#getStepCost} results
 * for each movement step between two adjacent path nodes. Only nodes for which {@link
 * GraphAgent#canOccupy} succeeds are considered reachable.</p>
 * <p>
 * <b>Caution:</b> Multi-step movements are problematic when the {@link GraphAgent} uses a
 * non-trivial {@link GraphAgent#canOccupy} condition. After a successful path search, all
 * found {@link #nodes} are valid end points for a single-step movement. However, attempting
 * to reach any {@link #nodes} element with multiple movement steps may prove impossible.
 * Since the algorithm does not know whether a given intermediate path node will be occupied
 * permanently or merely traversed, the path by which a {@link #nodes} element was reached may
 * contain many intermediate nodes for which {@link GraphAgent#canOccupy} fails.</p>
 * 
 * @param <T> the type of all nodes in the {@link Graph}
 * @author Christoph Nahr
 * @version 6.0.0
 */
public class Coverage<T> {

    // search arguments & results
    private GraphAgent<T> _agent;
    private double _maxCost;
    private final List<T> _nodes = new ArrayList<>(0);

    // hashtable holding minimum path costs
    private final Map<T, Double> _pathCosts = new HashMap<>(0);

    /**
     * The {@link Graph} on which all searches are performed.
     */
    public final Graph<T> graph;

    /**
     * Creates a {@link Coverage} algorithm for the specified {@link Graph}.
     * @param graph the {@link Graph} on which all searches are performed
     * @throws NullPointerException if {@code graph} is {@code null}
     */
    public Coverage(Graph<T> graph) {
        if (graph == null)
            throw new NullPointerException("graph");

        this.graph = graph;
    }

    /**
     * Finds all {@link #graph} nodes that the specified {@link GraphAgent} can reach
     * from the specified source node within the specified maximum path cost.
     * Returns {@code false} if {@link #graph} does not contain {@code source},
     * or if there are no nodes reachable from {@code source} whose total path cost
     * is equal to or less than the specified {@code maxCost}.
     * <p>
     * Otherwise, returns {@code true} and sets {@link #nodes} to the result of the
     * path search. {@link #nodes} contains only those reachable nodes for which
     * {@link GraphAgent#canOccupy} has succeeded.</p>
     * <p>
     * If {@link GraphAgent#relaxedRange} is {@code true} for {@code agent}, {@link #nodes}
     * includes those nodes whose total path cost exceeds {@code maxCost}, but which can be
     * reached from a neighbor whose total path cost is strictly less than {@code maxCost}.
     * These nodes are considered reachable regardless of their actual step costs.</p>
     * 
     * @param agent the {@link GraphAgent} that performs the movement
     * @param source the source node within {@link #graph} where the movement starts
     * @param maxCost the maximum total cost of the best path from {@code source}
     *                to any reachable {@link #graph} node
     * @return {@code true} if one or more {@link #graph} nodes could be reached
     *         from {@code source} within {@code maxCost}, else {@code false}
     * @throws IllegalArgumentException if {@code maxCost} is equal to or less than zero
     * @throws NullPointerException if {@code agent} or {@code source} is {@code null}
     */
    public boolean findReachable(GraphAgent<T> agent, T source, double maxCost) {
        if (agent == null)
            throw new NullPointerException("agent");
        if (source == null)
            throw new NullPointerException("source");
        if (maxCost <= 0)
            throw new IllegalArgumentException("maxCost <= 0");

        _agent = agent;
        _maxCost = maxCost;

        // clear previous results
        _nodes.clear();

        // fail if source node is invalid
        if (!graph.contains(source))
            return false;

        // mark source node as visited
        _pathCosts.put(source, -1.0);

        // expand coverage to maxCost
        expandArea(source, 0);

        // clear intermediate data
        _pathCosts.clear();

        // succeed if any nodes reached
        return !_nodes.isEmpty();
    }

    /**
     * Gets the {@link GraphAgent} for the last search.
     * @return the {@link GraphAgent} that was supplied to the last {@link #findReachable}
     *         call, or {@code null} if the method has not yet been called
     */
    public GraphAgent<T> agent() {
        return _agent;
    }

    /**
     * Gets all {@link #graph} nodes that were reached by the last successful search.
     * Returns an empty collection if the last {@link #findReachable} call returned
     * {@code false}, or if the method has not yet been called.
     * 
     * @return an unmodifiable {@link List} containing all {@link #graph} nodes that were reached
     *         by the last successful {@link #findReachable} call, not including the source node
     */
    public List<T> nodes() {
        return Collections.unmodifiableList(_nodes);
    }

    /**
     * Expands the current search area with all neighbors of the specified
     * {@link #graph} node that can be reached by the current {@link #agent}.
     * Recursively computes all possible movement paths for the current {@link #agent},
     * adding all valid nodes in any affordable path to {@link #nodes}, excluding the
     * source node. Never revisits nodes that were already reached by a better path.
     * 
     * @param node the {@link #graph} node whose neighbors to examine
     * @param cost the total path cost to reach {@code node}, measured as the sum of all
     *             {@link GraphAgent#getStepCost} results for the current {@link #agent}
     *             and each movement step between neighboring nodes
     * @throws NullPointerException if {@code node} is {@code null}
     */
    private void expandArea(T node, double cost) {

        // recurse into all valid neighbors of current node
        final Collection<T> neighbors = graph.getNeighbors(node);
        for (T neighbor: neighbors) {

            // skip nodes with better path
            final Double minCost = _pathCosts.get(neighbor);
            if (minCost != null && minCost <= cost)
                continue;

            // skip unreachable nodes
            if (!_agent.canMakeStep(node, neighbor))
                continue;

            // get cost for next movement step
            final double stepCost = _agent.getStepCost(node, neighbor);
            assert(stepCost > 0);

            // skip unaffordable nodes
            if (!_agent.relaxedRange() && cost + stepCost > _maxCost)
                continue;

            // skip nodes with better path
            if (minCost != null && minCost <= cost + stepCost)
                continue;

            // add newly reached neighbor if possible
            if (minCost == null && _agent.canOccupy(neighbor))
                _nodes.add(neighbor);

            // store new minimum path cost
            _pathCosts.put(neighbor, cost + stepCost);

            // visit neighbors if still affordable
            if (cost + stepCost < _maxCost)
                expandArea(neighbor, cost + stepCost);
        }
    }
}
