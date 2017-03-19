package org.kynosarges.tektosyne.graph;

import java.util.*;

/**
 * Represents the path of a {@link GraphAgent} across a {@link Graph}.
 * Also provides the total path cost and the last node reachable with a given maximum cost.
 * 
 * @param <T> the type of all nodes in the {@link Graph}
 * @author Christoph Nahr
 * @version 6.0.0
 */
public interface GraphPath<T> {
    /**
     * Gets all {@link Graph} nodes in the {@link GraphPath}.
     * Never returns {@code null}, but may return an empty {@link List}.
     * 
     * @return a {@link List} of all {@link Graph} nodes in the {@link GraphPath}
     */
    List<T> nodes();

    /**
     * Gets the total cost of the {@link GraphPath}.
     * May return a non-positive value if {@link #nodes} is empty.
     * 
     * @return the sum of the {@link GraphAgent#getStepCost} results for all {@link #nodes}
     */
    double totalCost();

    /**
     * Gets the last {@link GraphPath} node reachable within the specified maximum cost.
     * @param maxCost the maximum total path cost
     * @return the last {@link #nodes} element whose total path cost does not exceed {@code maxCost}
     * @throws IllegalArgumentException if {@code maxCost} is equal to or less than zero
     * @throws IllegalStateException if the {@link GraphPath} contains invalid data
     */
    T getLastNode(double maxCost);
}
